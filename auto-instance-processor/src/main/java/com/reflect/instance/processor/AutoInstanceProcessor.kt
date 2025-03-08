package com.reflect.instance.processor

import com.google.devtools.ksp.isPrivate
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.reflect.instance.annotations.AutoInject
import com.reflect.instance.annotations.InjectInstance
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.*

class AutoInstanceProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {

        val injectInstanceSymbols = resolver
            .getSymbolsWithAnnotation(InjectInstance::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.validate() }
            .toList()

        if (injectInstanceSymbols.isEmpty()) {
            return emptyList()
        }

        injectInstanceSymbols.forEach { classDeclaration ->
            processClass(classDeclaration)
        }

        return injectInstanceSymbols.filterNot { it.validate() }.toList()
    }

    private fun processClass(classDeclaration: KSClassDeclaration) {
        val className = classDeclaration.simpleName.asString()
        val packageName = classDeclaration.packageName.asString()
        val generatedClassName = "${className}Injector"

        // Get all properties with @AutoInject annotation
        val autoInjectProperties = classDeclaration.getAllProperties()
            .filter { property ->
                property.annotations.any {
                    it.shortName.asString() == "AutoInject" &&
                            it.annotationType.resolve().declaration.qualifiedName?.asString() == AutoInject::class.qualifiedName
                }
            }
            .toList()

        if (autoInjectProperties.isEmpty()) {
            logger.warn("No @AutoInject properties found in class ${classDeclaration.qualifiedName?.asString()}")
            return
        }

        // Validate that @AutoInject is only used on mutable (var) properties
        val invalidProperties = autoInjectProperties.filter { !it.isMutable }
        if (invalidProperties.isNotEmpty()) {
            val invalidPropertyNames = invalidProperties.joinToString { it.simpleName.asString() }
            throw IllegalStateException("❌ @AutoInject can only be applied to 'var' properties! Found on: $invalidPropertyNames in class $className")
        }

        // Proceed with valid properties only
        val validAutoInjectProperties = autoInjectProperties.filter { it.isMutable }

        if (validAutoInjectProperties.isEmpty()) {
            return
        }

        // Generate the injector class
        val fileSpec = FileSpec.builder(packageName, generatedClassName)
            .addType(
                TypeSpec.objectBuilder(generatedClassName)
                    .addKdoc("Generated on: ${java.time.LocalDateTime.now()}")
                    .addFunction(
                        FunSpec.builder("inject")
                            .addParameter("target", classDeclaration.toClassName())
                            .apply {
                                validAutoInjectProperties.forEach { property ->
                                    val propertyName = property.simpleName.asString()
                                    val propertyType = property.type.resolve()

                                    // Get annotation parameters
                                    val annotation = property.annotations.first {
                                        it.shortName.asString() == "AutoInject" &&
                                                it.annotationType.resolve().declaration.qualifiedName?.asString() == AutoInject::class.qualifiedName
                                    }

                                    val count = annotation.arguments
                                        .find { it.name?.asString() == "count" }
                                        ?.value as? Int ?: 1

                                    val dataGenerator = annotation.arguments
                                        .find { it.name?.asString() == "dataGenerator" }
                                        ?.value as? String ?: ""

                                    val generator = if (dataGenerator.isNullOrEmpty()) {
                                        ""
                                    } else {
                                        "/** $dataGenerator **/"
                                    }

                                    // Check if the property type is a List
                                    val isList = propertyType.declaration.qualifiedName?.asString() == "kotlin.collections.List"

                                    if (isList) {
                                        // Handle List type
                                        val typeArg = propertyType.arguments.firstOrNull()?.type?.resolve()
                                        if (typeArg != null) {
                                            val argShortName = typeArg.declaration.simpleName.asString()

                                            addStatement(
                                                "target.%L = %L List(%L) { Any() as %L }",
                                                propertyName,
                                                generator,
                                                count,
                                                ClassName(typeArg.declaration.qualifiedName?.getQualifier().toString(), argShortName)
                                            )
                                        }
                                    } else {
                                        val type = propertyType.declaration.simpleName.getShortName()

//                                        addStatement(
//                                            "val %L: %T = Any() as %L",
//                                            propertyName,
//                                            ClassName(propertyType.declaration.qualifiedName?.getQualifier().toString(), type),
//                                            type
//                                        )

                                        addStatement(
                                            "target.%L = %L Any() as %L",
                                            propertyName,
                                            generator,
                                            ClassName(propertyType.declaration.qualifiedName?.getQualifier().toString(), type)
                                        )
                                    }
                                }
                            }
                            .build()
                    )
                    .build()
            )
            .build()

        fileSpec.writeTo(codeGenerator, Dependencies(true, classDeclaration.containingFile!!))
    }
}

class AutoInstanceProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return AutoInstanceProcessor(
            environment.codeGenerator,
            environment.logger,
            environment.options
        )
    }
}
