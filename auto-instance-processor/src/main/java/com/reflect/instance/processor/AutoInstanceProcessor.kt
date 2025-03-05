package com.reflect.instance.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.reflect.instance.annotations.AutoInject
import com.reflect.instance.annotations.InjectInstance
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import java.io.OutputStream

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

        // Generate the injector class
        val fileSpec = FileSpec.builder(packageName, generatedClassName)
            .addType(
                TypeSpec.objectBuilder(generatedClassName)
                    .addFunction(
                        FunSpec.builder("inject")
                            .addParameter("target", classDeclaration.toClassName())
                            .addStatement("val fakeHelper = %T()", ClassName("com.reflect.instance", "FakeHelper"))
                            .apply {
                                autoInjectProperties.forEach { property ->
                                    val propertyName = property.simpleName.asString()
                                    val propertyType = property.type.resolve()
                                    val propertyTypeClassName = propertyType.declaration.qualifiedName?.asString() ?: ""
                                    
                                    addStatement(
                                        "target.%L = fakeHelper.fake(%L::class, 1)[0] as %L",
                                        propertyName,
                                        propertyTypeClassName,
                                        propertyTypeClassName
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