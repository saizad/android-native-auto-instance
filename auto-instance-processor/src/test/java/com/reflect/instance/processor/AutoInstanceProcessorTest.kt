package com.reflect.instance.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AutoInstanceProcessorTest {
    @Rule
    @JvmField
    val temporaryFolder = TemporaryFolder()

    private fun findGeneratedFiles(compilation: KotlinCompilation): List<File> {
        return compilation.kspSourcesDir
            .walkTopDown()
            .filter { it.isFile }
            .toList()
    }

    private fun processAndExtractGeneratedFiles(
        injectorFileNames: List<String> = emptyList(),
        vararg sourceFiles: SourceFile,
    ): Pair<KotlinCompilation.Result, List<File>> {
        val compilation = KotlinCompilation().apply {
            sources = sourceFiles.toList()
            symbolProcessorProviders = listOf(AutoInstanceProcessorProvider())
            inheritClassPath = true
            messageOutputStream = System.out
            workingDir = temporaryFolder.root
        }


        val result = compilation.compile()
        val generatedFiles = findGeneratedFiles(compilation)
            .filter {
                injectorFileNames.contains(it.name)
            }

        return result to generatedFiles
    }

    @Test
    fun testProcessorGeneratesInjectorForClassWithValidProperties() {
        val injectionInstanceClassName = "TestClass"
        val injectionInstanceClassNameProcessed = injectionInstanceClassName.plus("Injector")
        val injectorFileNames = listOf(
            injectionInstanceClassNameProcessed.plus(".kt")
        )
        val pkgName = "com.example.test"
        val source = SourceFile.kotlin(
            "TestClass.kt", """
            package $pkgName
            
            import com.reflect.instance.annotations.AutoInject
            import com.reflect.instance.annotations.InjectInstance
            
            @InjectInstance
            class $injectionInstanceClassName {
                @AutoInject
                lateinit var name: String
                
                @AutoInject(count = 3)
                lateinit var tags: List<String>
            }
            """
        )


        val (result, generatedFiles) = processAndExtractGeneratedFiles(
            injectorFileNames = injectorFileNames,
            source,
        )

        Assert.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val injectionInstanceClassNameProcessedFile =
            generatedFiles.firstOrNull { it.name == injectionInstanceClassNameProcessed.plus(".kt")}

        Assert.assertNotNull(
            "Generated file($injectionInstanceClassNameProcessed) should exist",
            injectionInstanceClassNameProcessedFile
        )

        val fileContent = injectionInstanceClassNameProcessedFile!!.readText()
        Assert.assertTrue(fileContent.contains("package $pkgName"))
        Assert.assertTrue(fileContent.contains("object $injectionInstanceClassName"))
        Assert.assertTrue(fileContent.contains("fun inject(target: $injectionInstanceClassName)"))
        Assert.assertTrue(fileContent.contains("target.name = Any() as kotlin.String"))
        Assert.assertTrue(fileContent.contains("target.tags = List(3) { Any() as kotlin.String }"))
    }

    @Test
    fun testProcessorGeneratesInjectorForClassWithValidPropertiesForMultiple() {
        val injectionInstanceClassName1 = "TestClass1"
        val injectionInstanceClassName2 = "TestClass2"
        val injectionInstanceClassName1Processed = injectionInstanceClassName1.plus("Injector")
        val injectionInstanceClassName2Processed = injectionInstanceClassName2.plus("Injector")
        val injectorFileNames = listOf(
            injectionInstanceClassName1Processed.plus(".kt"),
            injectionInstanceClassName2Processed.plus(".kt")
        )
        val pkgName = "com.example.test"
        val source = SourceFile.kotlin(
            "TestClass.kt", """
            package $pkgName
            
            import com.reflect.instance.annotations.AutoInject
            import com.reflect.instance.annotations.InjectInstance
            
            @InjectInstance
            class $injectionInstanceClassName1 {
                @AutoInject
                lateinit var name: String
                
                @AutoInject(count = 3)
                lateinit var tags: List<String>
            }

            @InjectInstance
            class $injectionInstanceClassName2 {
                @AutoInject
                lateinit var name: String
                
                @AutoInject(count = 3)
                lateinit var tags: List<String>
            }
            """
        )


        val (result, generatedFiles) = processAndExtractGeneratedFiles(
            injectorFileNames = injectorFileNames,
            source,
        )

        Assert.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val injectionInstanceClassName1ProcessedFile =
            generatedFiles.firstOrNull { it.name == injectionInstanceClassName1Processed.plus(".kt") }

        Assert.assertNotNull(
            "Generated file($injectionInstanceClassName1Processed) should exist",
            injectionInstanceClassName1ProcessedFile
        )

        val injectionInstanceClassName2ProcessedFile =
            generatedFiles.firstOrNull { it.name == injectionInstanceClassName2Processed.plus(".kt") }

        Assert.assertNotNull(
            "Generated file($injectionInstanceClassName2Processed) should exist",
            injectionInstanceClassName2ProcessedFile
        )


        val className1Content = injectionInstanceClassName1ProcessedFile!!.readText()
        Assert.assertTrue(className1Content.contains("package $pkgName"))
        Assert.assertTrue(className1Content.contains("object $injectionInstanceClassName1"))
        Assert.assertTrue(className1Content.contains("fun inject(target: $injectionInstanceClassName1)"))
        Assert.assertTrue(className1Content.contains("target.name = Any() as kotlin.String"))
        Assert.assertTrue(className1Content.contains("target.tags = List(3) { Any() as kotlin.String }"))


        val className2Content = injectionInstanceClassName2ProcessedFile!!.readText()
        Assert.assertTrue(className2Content.contains("package $pkgName"))
        Assert.assertTrue(className2Content.contains("object $injectionInstanceClassName2"))
        Assert.assertTrue(className2Content.contains("fun inject(target: $injectionInstanceClassName2)"))
        Assert.assertTrue(className2Content.contains("target.name = Any() as kotlin.String"))
        Assert.assertTrue(className2Content.contains("target.tags = List(3) { Any() as kotlin.String }"))

    }

    @Test
    fun testProcessorHandlesImmutablePropertiesWithWarning() {
        val injectionInstanceClassName = "TestClass"
        val injectionInstanceClassNameProcessed = injectionInstanceClassName.plus("Injector")
        val injectorFileNames = listOf(
            injectionInstanceClassNameProcessed.plus(".kt")
        )
        val pkgName = "com.example.test"
        val source = SourceFile.kotlin(
            "TestClass.kt", """
            package $pkgName
            
            import com.reflect.instance.annotations.AutoInject
            import com.reflect.instance.annotations.InjectInstance
            
            @InjectInstance
            class $injectionInstanceClassName {
                @AutoInject
                lateinit var mutableProp: String
                
                @AutoInject
                val immutableProp: String = "Cannot be injected"
            }
            """
        )

        val (result, generatedFiles) = processAndExtractGeneratedFiles(
            injectorFileNames = injectorFileNames,
            source,
        )

        Assert.assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)

    }

    @Test
    fun testProcessorHandlesCustomDataGeneratorParameter() {
        val injectionInstanceClassName = "TestClass"
        val injectionInstanceClassNameProcessed = injectionInstanceClassName.plus("Injector")
        val injectorFileNames = listOf(
            injectionInstanceClassNameProcessed.plus(".kt")
        )
        val pkgName = "com.example.test"
        val dataGenerator = "generateFakeNames()"
        val source = SourceFile.kotlin(
            "TestClass.kt", """
            package $pkgName
            
            import com.reflect.instance.annotations.AutoInject
            import com.reflect.instance.annotations.InjectInstance
            
            @InjectInstance
            class $injectionInstanceClassName {
                @AutoInject(dataGenerator = "$dataGenerator")
                lateinit var name: String
                
                @AutoInject(count = 3, dataGenerator = "$dataGenerator")
                lateinit var tags: List<String>
            }
            """
        )

        val (result, generatedFiles) = processAndExtractGeneratedFiles(
            injectorFileNames = injectorFileNames,
            source,
        )

        Assert.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val injectionInstanceClassNameProcessedFile =
            generatedFiles.firstOrNull { it.name == injectionInstanceClassNameProcessed.plus(".kt")}

        Assert.assertNotNull(
            "Generated file($injectionInstanceClassNameProcessed) should exist",
            injectionInstanceClassNameProcessedFile
        )

        val fileContent = injectionInstanceClassNameProcessedFile!!.readText()
        Assert.assertTrue(fileContent.contains("package $pkgName"))
        Assert.assertTrue(fileContent.contains("/** $dataGenerator **/"))
        Assert.assertTrue(fileContent.contains("target.name = /** $dataGenerator **/ Any() as kotlin.String"))
        Assert.assertTrue(fileContent.contains("target.tags = /** $dataGenerator **/ List(3) { Any() as kotlin.String }"))
    }

    @Test
    fun testProcessorHandlesComplexPropertyTypes() {
        val injectionInstanceClassName = "TestClass"
        val injectionInstanceClassNameProcessed = injectionInstanceClassName.plus("Injector")
        val injectorFileNames = listOf(
            injectionInstanceClassNameProcessed.plus(".kt")
        )
        val pkgName = "com.example.test"
        val source = SourceFile.kotlin(
            "TestClass.kt", """
            package $pkgName
            
            import com.reflect.instance.annotations.AutoInject
            import com.reflect.instance.annotations.InjectInstance
            
            data class ComplexType(val value: String)
            
            @InjectInstance
            class $injectionInstanceClassName {
                @AutoInject
                lateinit var simpleType: String
                
                @AutoInject
                lateinit var complexType: ComplexType
                
                @AutoInject(count = 2)
                lateinit var complexList: List<ComplexType>
                
                @AutoInject
                lateinit var nestedList: List<List<String>>
            }
            """
        )

        val (result, generatedFiles) = processAndExtractGeneratedFiles(
            injectorFileNames = injectorFileNames,
            source,
        )

        Assert.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val injectionInstanceClassNameProcessedFile =
            generatedFiles.firstOrNull { it.name == injectionInstanceClassNameProcessed.plus(".kt")}

        Assert.assertNotNull(
            "Generated file($injectionInstanceClassNameProcessed) should exist",
            injectionInstanceClassNameProcessedFile
        )

        val fileContent = injectionInstanceClassNameProcessedFile!!.readText()
        println(fileContent)
        Assert.assertTrue(fileContent.contains("target.simpleType = Any() as kotlin.String"))
        Assert.assertTrue(fileContent.contains("target.complexType = Any() as $pkgName.ComplexType"))
        Assert.assertTrue(fileContent.contains("target.complexList = List(2) { Any() as $pkgName.ComplexType }"))
        // The nested list is treated as a simple List<List<String>> type
        Assert.assertTrue(fileContent.contains("target.nestedList = Any() as kotlin.collections.List"))
    }

    @Test
    fun testProcessorIgnoresClassesWithoutInjectInstanceAnnotation() {
        val annotatedClassName = "AnnotatedClass"
        val annotatedClassNameProcessed = annotatedClassName.plus("Injector")
        val nonAnnotatedClassName = "NonAnnotatedClass"
        val nonAnnotatedClassNameProcessed = nonAnnotatedClassName.plus("Injector")
        val injectorFileNames = listOf(
            annotatedClassNameProcessed.plus(".kt"),
            nonAnnotatedClassNameProcessed.plus(".kt")
        )
        val pkgName = "com.example.test"
        val source = SourceFile.kotlin(
            "TestClasses.kt", """
            package $pkgName
            
            import com.reflect.instance.annotations.AutoInject
            import com.reflect.instance.annotations.InjectInstance
            
            @InjectInstance
            class $annotatedClassName {
                @AutoInject
                lateinit var name: String
            }
            
            class $nonAnnotatedClassName {
                @AutoInject
                lateinit var name: String
            }
            """
        )

        val (result, generatedFiles) = processAndExtractGeneratedFiles(
            injectorFileNames = injectorFileNames,
            source,
        )

        Assert.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        // Should find the annotated class injector
        val annotatedClassInjectorFile =
            generatedFiles.firstOrNull { it.name == annotatedClassNameProcessed.plus(".kt")}
        Assert.assertNotNull(
            "Generated file for annotated class should exist",
            annotatedClassInjectorFile
        )

        // Should not find the non-annotated class injector
        val nonAnnotatedClassInjectorFile =
            generatedFiles.firstOrNull { it.name == nonAnnotatedClassNameProcessed.plus(".kt")}
        Assert.assertNull(
            "Generated file for non-annotated class should not exist",
            nonAnnotatedClassInjectorFile
        )
    }

    @Test
    fun testProcessorHandlesMultipleAnnotatedClasses() {
        val className1 = "TestClass1"
        val className2 = "TestClass2"
        val className3 = "TestClass3"
        val className1Processed = className1.plus("Injector")
        val className2Processed = className2.plus("Injector")
        val className3Processed = className3.plus("Injector")
        val injectorFileNames = listOf(
            className1Processed.plus(".kt"),
            className2Processed.plus(".kt"),
            className3Processed.plus(".kt")
        )
        val pkgName = "com.example.test"
        val source = SourceFile.kotlin(
            "TestClasses.kt", """
            package $pkgName
            
            import com.reflect.instance.annotations.AutoInject
            import com.reflect.instance.annotations.InjectInstance
            
            @InjectInstance
            class $className1 {
                @AutoInject
                lateinit var prop1: String
            }
            
            @InjectInstance
            class $className2 {
                @AutoInject
                lateinit var prop2: Int
            }
            
            @InjectInstance
            class $className3 {
                @AutoInject(count = 5)
                lateinit var prop3: List<Double>
            }
            """
        )

        val (result, generatedFiles) = processAndExtractGeneratedFiles(
            injectorFileNames = injectorFileNames,
            source,
        )

        Assert.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        // Check that all three injector files were generated
        val file1 = generatedFiles.firstOrNull { it.name == className1Processed.plus(".kt")}
        val file2 = generatedFiles.firstOrNull { it.name == className2Processed.plus(".kt")}
        val file3 = generatedFiles.firstOrNull { it.name == className3Processed.plus(".kt")}

        Assert.assertNotNull("Generated file for class1 should exist", file1)
        Assert.assertNotNull("Generated file for class2 should exist", file2)
        Assert.assertNotNull("Generated file for class3 should exist", file3)

        // Verify content of each file
        val content1 = file1!!.readText()
        val content2 = file2!!.readText()
        val content3 = file3!!.readText()

        Assert.assertTrue(content1.contains("target.prop1 = Any() as kotlin.String"))
        Assert.assertTrue(content2.contains("target.prop2 = Any() as kotlin.Int"))
        Assert.assertTrue(content3.contains("target.prop3 = List(5) { Any() as kotlin.Double }"))
    }

    @Test
    fun testProcessorHandlesClassWithNoAutoInjectProperties() {
        val className = "EmptyClass"
        val classNameProcessed = className.plus("Injector")
        val injectorFileNames = listOf(
            classNameProcessed.plus(".kt")
        )
        val pkgName = "com.example.test"
        val source = SourceFile.kotlin(
            "EmptyClass.kt", """
            package $pkgName
            
            import com.reflect.instance.annotations.AutoInject
            import com.reflect.instance.annotations.InjectInstance
            
            @InjectInstance
            class $className {
                // No @AutoInject properties
                lateinit var name: String
                var age: Int = 0
            }
            """
        )

        val (result, generatedFiles) = processAndExtractGeneratedFiles(
            injectorFileNames = injectorFileNames,
            source,
        )

        Assert.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        // Should not find any generated injector file
        val injectorFile = generatedFiles.firstOrNull { it.name == classNameProcessed.plus(".kt")}
        Assert.assertNull(
            "No file should be generated for class with no @AutoInject properties",
            injectorFile
        )
    }

    // Additional test case to cover private properties
    @Test
    fun testProcessorHandlesPrivateProperties() {
        val className = "PrivatePropsClass"
        val classNameProcessed = className.plus("Injector")
        val injectorFileNames = listOf(
            classNameProcessed.plus(".kt")
        )
        val pkgName = "com.example.test"
        val source = SourceFile.kotlin(
            "PrivatePropsClass.kt", """
            package $pkgName
            
            import com.reflect.instance.annotations.AutoInject
            import com.reflect.instance.annotations.InjectInstance
            
            @InjectInstance
            class $className {
                @AutoInject
                lateinit var publicProp: String
                
                @AutoInject
                private lateinit var privateProp: Int
            }
            """
        )

        val (result, generatedFiles) = processAndExtractGeneratedFiles(
            injectorFileNames = injectorFileNames,
            source,
        )

        Assert.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val injectorFile = generatedFiles.firstOrNull { it.name == classNameProcessed.plus(".kt")}
        Assert.assertNotNull(
            "Generated file should exist",
            injectorFile
        )

        val fileContent = injectorFile!!.readText()
        Assert.assertTrue(fileContent.contains("target.publicProp = Any() as kotlin.String"))
        Assert.assertTrue(fileContent.contains("target.privateProp = Any() as kotlin.Int"))
    }
}