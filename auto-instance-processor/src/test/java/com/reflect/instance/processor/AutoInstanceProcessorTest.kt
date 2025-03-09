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

    private fun normalizeWhitespace(text: String): String {
        return text.replace(Regex("\\s+"), " ").trim()
    }

    private fun findGeneratedFiles(compilation: KotlinCompilation): List<File> {
        return compilation.kspSourcesDir
            .walkTopDown()
            .filter { it.isFile }
            .toList()
    }

    private fun processAndExtractGeneratedFiles(
        injectorFileNames: List<String>,
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
            
            data class CustomType(val value: String)
            
            @InjectInstance
            class $injectionInstanceClassName {
                @AutoInject
                lateinit var customType: CustomType
                
                @AutoInject(count = 3)
                lateinit var customTypeList: List<CustomType>
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
        Assert.assertTrue(fileContent.contains("target.customType = Any() as $pkgName.CustomType"))
        Assert.assertTrue(fileContent.contains("target.customTypeList = List(3) { Any() as $pkgName.CustomType }"))
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
            
            data class CustomType1(val value: String)
            data class CustomType2(val value: Int)
            
            @InjectInstance
            class $injectionInstanceClassName1 {
                @AutoInject
                lateinit var customType: CustomType1
                
                @AutoInject(count = 3)
                lateinit var customTypeList: List<CustomType1>
            }

            @InjectInstance
            class $injectionInstanceClassName2 {
                @AutoInject
                lateinit var customType: CustomType2
                
                @AutoInject(count = 3)
                lateinit var customTypeList: List<CustomType2>
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
        Assert.assertTrue(className1Content.contains("target.customType = Any() as $pkgName.CustomType1"))
        Assert.assertTrue(className1Content.contains("target.customTypeList = List(3) { Any() as $pkgName.CustomType1 }"))


        val className2Content = injectionInstanceClassName2ProcessedFile!!.readText()
        Assert.assertTrue(className2Content.contains("package $pkgName"))
        Assert.assertTrue(className2Content.contains("object $injectionInstanceClassName2"))
        Assert.assertTrue(className2Content.contains("fun inject(target: $injectionInstanceClassName2)"))
        Assert.assertTrue(className2Content.contains("target.customType = Any() as $pkgName.CustomType2"))
        Assert.assertTrue(className2Content.contains("target.customTypeList = List(3) { Any() as $pkgName.CustomType2 }"))
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
            
            data class CustomType(val value: String)
            
            @InjectInstance
            class $injectionInstanceClassName {
                @AutoInject(dataGenerator = "$dataGenerator")
                lateinit var customType: CustomType
                
                @AutoInject(count = 3, dataGenerator = "$dataGenerator")
                lateinit var customTypeList: List<CustomType>
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
        Assert.assertTrue(fileContent.contains("target.customType = /** $dataGenerator **/ Any() as $pkgName.CustomType"))

        val expectedContent = "target.customTypeList = /** $dataGenerator **/ List(3) { Any() as $pkgName.CustomType }"

        Assert.assertTrue(
            normalizeWhitespace(fileContent).contains(normalizeWhitespace(expectedContent))
        )
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
                lateinit var complexType: ComplexType
                
                @AutoInject(count = 2)
                lateinit var complexList: List<ComplexType>
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
        Assert.assertTrue(fileContent.contains("target.complexType = Any() as $pkgName.ComplexType"))
        Assert.assertTrue(fileContent.contains("target.complexList = List(2) { Any() as $pkgName.ComplexType }"))
    }

    @Test
    fun testProcessorRejectsNestedLists() {
        val injectionInstanceClassName = "TestClass"
        val pkgName = "com.example.test"
        val source = SourceFile.kotlin(
            "TestClass.kt", """
            package $pkgName
            
            import com.reflect.instance.annotations.AutoInject
            import com.reflect.instance.annotations.InjectInstance
            
            @InjectInstance
            class $injectionInstanceClassName {
                @AutoInject
                lateinit var nestedList: List<List<String>>
            }
            """
        )

        val (result, _) = processAndExtractGeneratedFiles(emptyList(), source)

        Assert.assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        Assert.assertTrue(result.messages.contains("@AutoInject cannot be applied to nested list"))
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

        Assert.assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        Assert.assertTrue(result.messages.contains("@AutoInject cannot be applied to primitive types!"))
    }

    @Test
    fun testProcessorHandlesMultipleAnnotatedClasses() {
        val className1 = "TestClass1"
        val className2 = "TestClass2"
        val className1Processed = className1.plus("Injector")
        val className2Processed = className2.plus("Injector")
        val injectorFileNames = listOf(
            className1Processed.plus(".kt"),
            className2Processed.plus(".kt")
        )
        val pkgName = "com.example.test"
        val source = SourceFile.kotlin(
            "TestClasses.kt", """
            package $pkgName
            
            import com.reflect.instance.annotations.AutoInject
            import com.reflect.instance.annotations.InjectInstance
            
            data class CustomType1(val value: String)
            data class CustomType2(val value: Int)
            
            @InjectInstance
            class $className1 {
                @AutoInject
                lateinit var prop1: CustomType1
            }
            
            @InjectInstance
            class $className2 {
                @AutoInject
                lateinit var prop2: CustomType2
                
                @AutoInject(count = 5)
                lateinit var propList: List<CustomType1>
            }
            """
        )

        val (result, generatedFiles) = processAndExtractGeneratedFiles(
            injectorFileNames = injectorFileNames,
            source,
        )

        Assert.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        // Check that both injector files were generated
        val file1 = generatedFiles.firstOrNull { it.name == className1Processed.plus(".kt")}
        val file2 = generatedFiles.firstOrNull { it.name == className2Processed.plus(".kt")}

        Assert.assertNotNull("Generated file for class1 should exist", file1)
        Assert.assertNotNull("Generated file for class2 should exist", file2)

        // Verify content of each file
        val content1 = file1!!.readText()
        val content2 = file2!!.readText()

        Assert.assertTrue(content1.contains("target.prop1 = Any() as $pkgName.CustomType1"))
        Assert.assertTrue(content2.contains("target.prop2 = Any() as $pkgName.CustomType2"))
        Assert.assertTrue(content2.contains("target.propList = List(5) { Any() as $pkgName.CustomType1 }"))
    }

    @Test
    fun testProcessorRejectsPrimitiveTypes() {
        val className = "TestClass"
        val pkgName = "com.example.test"
        val source = SourceFile.kotlin(
            "TestClass.kt", """
            package $pkgName
            
            import com.reflect.instance.annotations.AutoInject
            import com.reflect.instance.annotations.InjectInstance
            
            @InjectInstance
            class $className {
                @AutoInject
                lateinit var prop1: String
            }
            """
        )

        val (result, _) = processAndExtractGeneratedFiles(emptyList(), source)

        Assert.assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        Assert.assertTrue(result.messages.contains("@AutoInject cannot be applied to primitive type"))
    }
    
    @Test
    fun testProcessorRejectsListsOfPrimitives() {
        val className = "TestClass"
        val pkgName = "com.example.test"
        val source = SourceFile.kotlin(
            "TestClass.kt", """
            package $pkgName
            
            import com.reflect.instance.annotations.AutoInject
            import com.reflect.instance.annotations.InjectInstance
            
            @InjectInstance
            class $className {
                @AutoInject(count = 5)
                lateinit var propList: List<Double>
            }
            """
        )

        val (result, _) = processAndExtractGeneratedFiles(emptyList(), source)

        Assert.assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        Assert.assertTrue(result.messages.contains("@AutoInject cannot be applied to lists of primitive types"))
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
            
            data class CustomType1(val value: String)
            data class CustomType2(val value: Int)
            
            @InjectInstance
            class $className {
                @AutoInject
                lateinit var publicProp: CustomType1
                
                @AutoInject
                private lateinit var privateProp: CustomType2
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
        Assert.assertTrue(fileContent.contains("target.publicProp = Any() as $pkgName.CustomType1"))
        Assert.assertTrue(fileContent.contains("target.privateProp = Any() as $pkgName.CustomType2"))
    }

    @Test
    fun testProcessorRejectsAllPrimitiveTypes() {
        val primitiveTypes = listOf(
            "Int" to "0",
            "Long" to "0L",
            "Short" to "0.toShort()",
            "Byte" to "0.toByte()",
            "Float" to "0f",
            "Double" to "0.0",
            "Boolean" to "false",
            "Char" to "'a'",
            "String" to "\"test\"",
            "Any" to "Any()"
        )
        
        for ((type, defaultValue) in primitiveTypes) {
            val className = "TestClass${type}"
            val pkgName = "com.example.test"
            val source = SourceFile.kotlin(
                "${className}.kt", """
                package $pkgName
                
                import com.reflect.instance.annotations.AutoInject
                import com.reflect.instance.annotations.InjectInstance
                
                @InjectInstance
                class $className {
                    @AutoInject
                    var primitiveProperty: kotlin.$type = $defaultValue
                }
                """
            )

            val (result, _) = processAndExtractGeneratedFiles(emptyList(), source)

            Assert.assertEquals(
                "Expected compilation error for primitive type $type",
                KotlinCompilation.ExitCode.COMPILATION_ERROR, 
                result.exitCode
            )
            Assert.assertTrue(
                "Error message should mention primitive type for $type",
                result.messages.contains("@AutoInject cannot be applied to primitive type")
            )
        }
    }
    
    @Test
    fun testProcessorRejectsAllPrimitiveTypesInLists() {
        val primitiveTypes = listOf(
            "Int", "Long", "Short", "Byte", "Float", "Double", "Boolean", "Char", "String", "Any"
        )
        
        for (type in primitiveTypes) {
            val className = "TestListOf${type}"
            val pkgName = "com.example.test"
            val source = SourceFile.kotlin(
                "${className}.kt", """
                package $pkgName
                
                import com.reflect.instance.annotations.AutoInject
                import com.reflect.instance.annotations.InjectInstance
                
                @InjectInstance
                class $className {
                    @AutoInject(count = 3)
                    lateinit var listOfPrimitives: List<kotlin.$type>
                }
                """
            )

            val (result, _) = processAndExtractGeneratedFiles(emptyList(), source)

            Assert.assertEquals(
                "Expected compilation error for List<$type>",
                KotlinCompilation.ExitCode.COMPILATION_ERROR, 
                result.exitCode
            )
            Assert.assertTrue(
                "Error message should mention lists of primitive types for List<$type>",
                result.messages.contains("@AutoInject cannot be applied to lists of primitive types")
            )
        }
    }
    
    @Test
    fun testProcessorRejectsVariousNestedListTypes() {
        val nestedListTypes = listOf(
            "List<List<String>>",
            "List<List<Int>>",
            "List<List<CustomType>>"
        )
        
        for (listType in nestedListTypes) {
            val className = "TestNestedList${listType.replace("<", "").replace(">", "").replace(", ", "")}"
            val pkgName = "com.example.test"
            val source = SourceFile.kotlin(
                "${className}.kt", """
                package $pkgName
                
                import com.reflect.instance.annotations.AutoInject
                import com.reflect.instance.annotations.InjectInstance
                
                data class CustomType(val value: String)
                
                @InjectInstance
                class $className {
                    @AutoInject
                    lateinit var nestedList: $listType
                }
                """
            )

            val (result, _) = processAndExtractGeneratedFiles(emptyList(), source)

            Assert.assertEquals(
                "Expected compilation error for nested list type $listType",
                KotlinCompilation.ExitCode.COMPILATION_ERROR, 
                result.exitCode
            )
            Assert.assertTrue(
                "Error message should mention nested lists for $listType",
                result.messages.contains("@AutoInject cannot be applied to nested list")
            )
        }
    }
    
    @Test
    fun testProcessorHandlesMultipleValidationErrors() {
        val className = "TestMultipleErrors"
        val pkgName = "com.example.test"
        val source = SourceFile.kotlin(
            "${className}.kt", """
            package $pkgName
            
            import com.reflect.instance.annotations.AutoInject
            import com.reflect.instance.annotations.InjectInstance
            
            data class CustomType(val value: String)
            
            @InjectInstance
            class $className {
                @AutoInject
                lateinit var primitiveProperty: String
                
                @AutoInject
                lateinit var nestedList: List<List<CustomType>>
                
                @AutoInject(count = 3)
                lateinit var listOfPrimitives: List<Int>
            }
            """
        )

        val (result, _) = processAndExtractGeneratedFiles(emptyList(), source)

        Assert.assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        // The first error encountered will cause compilation to fail
        Assert.assertTrue(result.messages.contains("@AutoInject cannot be applied to primitive type"))
    }
    
    @Test
    fun testProcessorHandlesValidCustomTypes() {
        val className = "TestValidCustomTypes"
        val classNameProcessed = className.plus("Injector")
        val injectorFileNames = listOf(
            classNameProcessed.plus(".kt")
        )
        val pkgName = "com.example.test"
        val source = SourceFile.kotlin(
            "${className}.kt", """
            package $pkgName
            
            import com.reflect.instance.annotations.AutoInject
            import com.reflect.instance.annotations.InjectInstance
            
            interface MyInterface
            abstract class MyAbstractClass
            data class MyDataClass(val value: String)
            class MyRegularClass
            enum class MyEnum { ONE, TWO }
            
            @InjectInstance
            class $className {
                @AutoInject
                lateinit var interfaceType: MyInterface
                
                @AutoInject
                lateinit var abstractClassType: MyAbstractClass
                
                @AutoInject
                lateinit var dataClassType: MyDataClass
                
                @AutoInject
                lateinit var regularClassType: MyRegularClass
                
                @AutoInject
                lateinit var enumType: MyEnum
                
                @AutoInject(count = 2)
                lateinit var listOfCustomType: List<MyDataClass>
            }
            """
        )

        val (result, generatedFiles) = processAndExtractGeneratedFiles(
            injectorFileNames = injectorFileNames,
            source,
        )

        Assert.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val injectorFile = generatedFiles.firstOrNull { it.name == classNameProcessed.plus(".kt")}
        Assert.assertNotNull("Generated file should exist", injectorFile)

        val fileContent = injectorFile!!.readText()
        Assert.assertTrue(fileContent.contains("target.interfaceType = Any() as $pkgName.MyInterface"))
        Assert.assertTrue(fileContent.contains("target.abstractClassType = Any() as $pkgName.MyAbstractClass"))
        Assert.assertTrue(fileContent.contains("target.dataClassType = Any() as $pkgName.MyDataClass"))
        Assert.assertTrue(fileContent.contains("target.regularClassType = Any() as $pkgName.MyRegularClass"))
        Assert.assertTrue(fileContent.contains("target.enumType = Any() as $pkgName.MyEnum"))
        Assert.assertTrue(fileContent.contains("target.listOfCustomType = List(2) { Any() as $pkgName.MyDataClass }"))
    }
    
    @Test
    fun testProcessorHandlesGenericTypes() {
        val className = "TestGenericTypes"
        val classNameProcessed = className.plus("Injector")
        val injectorFileNames = listOf(
            classNameProcessed.plus(".kt")
        )
        val pkgName = "com.example.test"
        val source = SourceFile.kotlin(
            "${className}.kt", """
            package $pkgName
            
            import com.reflect.instance.annotations.AutoInject
            import com.reflect.instance.annotations.InjectInstance
            
            class GenericClass<T>
            class BoundedGenericClass<T : Any>
            
            @InjectInstance
            class $className {
                @AutoInject
                lateinit var genericType: GenericClass<String>
                
                @AutoInject
                lateinit var boundedGenericType: BoundedGenericClass<Int>
                
                @AutoInject(count = 2)
                lateinit var listOfGenericType: List<GenericClass<Double>>
            }
            """
        )

        val (result, generatedFiles) = processAndExtractGeneratedFiles(
            injectorFileNames = injectorFileNames,
            source,
        )

        // The processor should handle generic types as non-primitive custom types
        Assert.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val injectorFile = generatedFiles.firstOrNull { it.name == classNameProcessed.plus(".kt")}
        Assert.assertNotNull("Generated file should exist", injectorFile)

        val fileContent = injectorFile!!.readText()
        Assert.assertTrue(fileContent.contains("target.genericType = Any() as $pkgName.GenericClass"))
        Assert.assertTrue(fileContent.contains("target.boundedGenericType = Any() as $pkgName.BoundedGenericClass"))
        Assert.assertTrue(fileContent.contains("target.listOfGenericType = List(2) { Any() as $pkgName.GenericClass }"))
    }

    @Test
    fun testProcessorHandlesMixedValidAndInvalidProperties() {
        val className = "TestMixedProperties"
        val pkgName = "com.example.test"
        val source = SourceFile.kotlin(
            "${className}.kt", """
            package $pkgName
            
            import com.reflect.instance.annotations.AutoInject
            import com.reflect.instance.annotations.InjectInstance
            
            data class ValidType(val value: String)
            
            @InjectInstance
            class $className {
                // Valid property
                @AutoInject
                lateinit var validProperty: ValidType
                
                // Invalid property (primitive)
                @AutoInject
                lateinit var invalidPrimitive: String
                
                // Valid property
                @AutoInject(count = 2)
                lateinit var validList: List<ValidType>
                
                // Invalid property (list of primitives)
                @AutoInject(count = 3)
                lateinit var invalidList: List<Int>
            }
            """
        )

        val (result, _) = processAndExtractGeneratedFiles(emptyList(), source)

        // The processor should fail on the first invalid property
        Assert.assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        Assert.assertTrue(result.messages.contains("@AutoInject cannot be applied to primitive type"))
    }
    
    @Test
    fun testProcessorHandlesNullableTypes() {
        val className = "TestNullableTypes"
        val classNameProcessed = className.plus("Injector")
        val injectorFileNames = listOf(
            classNameProcessed.plus(".kt")
        )
        val pkgName = "com.example.test"
        val source = SourceFile.kotlin(
            "${className}.kt", """
            package $pkgName
            
            import com.reflect.instance.annotations.AutoInject
            import com.reflect.instance.annotations.InjectInstance
            
            data class CustomType(val value: String)
            
            @InjectInstance
            class $className {
                @AutoInject
                lateinit var nonNullableType: CustomType
                
                @AutoInject
                var nullableType: CustomType? = null
                
                @AutoInject(count = 2)
                lateinit var nonNullableList: List<CustomType>
                
                @AutoInject(count = 2)
                var nullableList: List<CustomType>? = null
            }
            """
        )

        val (result, generatedFiles) = processAndExtractGeneratedFiles(
            injectorFileNames = injectorFileNames,
            source,
        )

        // The processor should handle nullable types as non-primitive custom types
        Assert.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val injectorFile = generatedFiles.firstOrNull { it.name == classNameProcessed.plus(".kt")}
        Assert.assertNotNull("Generated file should exist", injectorFile)

        val fileContent = injectorFile!!.readText()
        Assert.assertTrue(fileContent.contains("target.nonNullableType = Any() as $pkgName.CustomType"))
        Assert.assertTrue(fileContent.contains("target.nullableType = Any() as $pkgName.CustomType"))
        Assert.assertTrue(fileContent.contains("target.nonNullableList = List(2) { Any() as $pkgName.CustomType }"))
        // For nullable lists, the processor should still generate a non-null list
        Assert.assertTrue(fileContent.contains("target.nullableList = List(2) { Any() as $pkgName.CustomType }"))
    }
}