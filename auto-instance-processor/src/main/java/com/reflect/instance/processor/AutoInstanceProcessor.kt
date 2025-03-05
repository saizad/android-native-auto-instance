package com.reflect.instance.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.reflect.instance.annotations.AutoInject
import com.reflect.instance.annotations.InjectInstance
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

class AutoInstanceProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.logging("resolver: $resolver")

        val injectInstanceSymbols = resolver
            .getSymbolsWithAnnotation(InjectInstance::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.validate() }
            .toList()

        println("injectInstanceSymbols: $injectInstanceSymbols")
        if (injectInstanceSymbols.isEmpty()) {
            return emptyList()
        }

        injectInstanceSymbols.forEach { classDeclaration ->
            println("classDeclaration: $classDeclaration")
            processClass(classDeclaration, resolver)
        }

        return injectInstanceSymbols.filterNot { it.validate() }.toList()
    }

    private fun processClass(classDeclaration: KSClassDeclaration, resolver: Resolver) {
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

        // Generate the injector class
        val fileSpec = FileSpec.builder(packageName, generatedClassName)
            .addType(
                TypeSpec.objectBuilder(generatedClassName)
                    .addKdoc("Generated on: ${java.time.LocalDateTime.now()}")
                    .addFunction(
                        FunSpec.builder("inject")
                            .addParameter("target", classDeclaration.toClassName())
                            .apply {
                                autoInjectProperties.forEach { property ->
                                    val propertyName = property.simpleName.asString()
                                    val propertyType = property.type.resolve()
                                    val type = propertyType.declaration.simpleName.getShortName()
                                    addStatement(
                                        "val %L: %T = Any() as %L",
                                        propertyName,
                                        ClassName(propertyType.declaration.qualifiedName?.getQualifier().toString(), type),
                                        type
                                    )
                                    addStatement(
                                        "target.%L = %L",
                                        propertyName,
                                        propertyName,
                                    )
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