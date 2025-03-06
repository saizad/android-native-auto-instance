package com.reflect.instance.processor

import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.net.URLClassLoader
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.functions
import kotlin.reflect.full.memberProperties

class AutoInstanceProcessorTest {

    @Rule
    @JvmField
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `test simple class with single property`() {
        val source = SourceFile.kotlin(
            "SimpleClass.kt",
            """
            package com.reflect.test
            
            import com.reflect.instance.annotations.AutoInject
            import com.reflect.instance.annotations.InjectInstance
            
            @InjectInstance
            class SimpleClass {
                @AutoInject
                lateinit var simpleProperty: String
            }
            """
        )

        val result = compile(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        // Verify the generated file exists
        val generatedFile = result.outputDirectory.resolve(
            "ksp/sources/kotlin/com/reflect/test/SimpleClassInjector.kt"
        )
        assertTrue("Generated file doesn't exist", generatedFile.exists())

        // Check file content
        val fileContent = generatedFile.readText()
        assertTrue(fileContent.contains("fun inject(target: SimpleClass)"))
        assertTrue(fileContent.contains("val simpleProperty: String ="))
        assertTrue(fileContent.contains("target.simpleProperty = simpleProperty"))

        // Load and test the generated class
        val injectorClass = loadClass(result, "com.reflect.test.SimpleClassInjector")
        val simpleClass = loadClass(result, "com.reflect.test.SimpleClass").getDeclaredConstructor().newInstance()

        // Invoke the inject method
        val injectMethod = injectorClass.kotlin.functions.first { it.name == "inject" }
        injectMethod.call(injectorClass.kotlin.objectInstance, simpleClass)

        // Verify the property was set
        val property = simpleClass::class.memberProperties.first { it.name == "simpleProperty" }
        assertNotNull(property.getter.call(simpleClass))
    }

    @Test
    fun `test with multiple properties`() {
        val source = SourceFile.kotlin(
            "MultiPropertyClass.kt",
            """
            package com.reflect.test
            
            import com.reflect.instance.annotations.AutoInject
            import com.reflect.instance.annotations.InjectInstance
            
            @InjectInstance
            class MultiPropertyClass {
                @AutoInject
                lateinit var stringProperty: String
                
                @AutoInject
                var intProperty: Int = 0
                
                @AutoInject
                lateinit var customProperty: CustomClass
                
                var nonAnnotatedProperty: String = "default"
            }
            
            class CustomClass
            """
        )

        val result = compile(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val generatedFile = result.outputDirectory.resolve(
            "ksp/sources/kotlin/com/reflect/test/MultiPropertyClassInjector.kt"
        )
        assertTrue(generatedFile.exists())

        val fileContent = generatedFile.readText()
        assertTrue(fileContent.contains("val stringProperty: String ="))
        assertTrue(fileContent.contains("val intProperty: Int ="))
        assertTrue(fileContent.contains("val customProperty: CustomClass ="))
        assertFalse(fileContent.contains("nonAnnotatedProperty"))
    }

    @Test
    fun `test with list property and count parameter`() {
        val source = SourceFile.kotlin(
            "ListPropertyClass.kt",
            """
            package com.reflect.test
            
            import com.reflect.instance.annotations.AutoInject
            import com.reflect.instance.annotations.InjectInstance
            
            @InjectInstance
            class ListPropertyClass {
                @AutoInject(count = 5)
                lateinit var items: List<Item>
            }
            
            class Item
            """
        )

        val result = compile(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val generatedFile = result.outputDirectory.resolve(
            "ksp/sources/kotlin/com/reflect/test/ListPropertyClassInjector.kt"
        )
        assertTrue(generatedFile.exists())

        val fileContent = generatedFile.readText()
        assertTrue(fileContent.contains("val items: List<Item> = List(5) { Any() as Item }"))
    }

    @Test
    fun `test class without InjectInstance annotation`() {
        val source = SourceFile.kotlin(
            "NonAnnotatedClass.kt",
            """
            package com.reflect.test
            
            import com.reflect.instance.annotations.AutoInject
            
            class NonAnnotatedClass {
                @AutoInject
                lateinit var property: String
            }
            """
        )

        val result = compile(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        // Verify no file was generated
        val generatedFile = result.outputDirectory.resolve(
            "ksp/sources/kotlin/com/reflect/test/NonAnnotatedClassInjector.kt"
        )
        assertFalse(generatedFile.exists())
    }

    @Test
    fun `test class with InjectInstance but no AutoInject properties`() {
        val source = SourceFile.kotlin(
            "NoAutoInjectClass.kt",
            """
            package com.reflect.test
            
            import com.reflect.instance.annotations.InjectInstance
            
            @InjectInstance
            class NoAutoInjectClass {
                var property: String = ""
            }
            """
        )

        val result = compile(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        // Verify no file was generated
        val generatedFile = result.outputDirectory.resolve(
            "ksp/sources/kotlin/com/reflect/test/NoAutoInjectClassInjector.kt"
        )
        assertFalse(generatedFile.exists())
    }

    @Test
    fun `test source parameter`() {
        val source = SourceFile.kotlin(
            "SourceParamClass.kt",
            """
            package com.reflect.test
            
            import com.reflect.instance.annotations.AutoInject
            import com.reflect.instance.annotations.InjectInstance
            
            @InjectInstance
            class SourceParamClass {
                @AutoInject(source = "custom-source")
                lateinit var property: String
            }
            """
        )

        val result = compile(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val generatedFile = result.outputDirectory.resolve(
            "ksp/sources/kotlin/com/reflect/test/SourceParamClassInjector.kt"
        )
        assertTrue(generatedFile.exists())

        // The source parameter is retrieved but not currently used in the generated code
        // This test just verifies the compilation works with this parameter
        val fileContent = generatedFile.readText()
        assertTrue(fileContent.contains("val property: String ="))
    }

    private fun compile(vararg sourceFiles: SourceFile): KotlinCompilation.Result {
        // Create annotation source files
        val autoInjectAnnotation = SourceFile.kotlin(
            "AutoInject.kt",
            """
            package com.reflect.instance.annotations
            
            @Target(AnnotationTarget.PROPERTY)
            @Retention(AnnotationRetention.SOURCE)
            annotation class AutoInject(
                val count: Int = 1,
                val source: String = ""
            )
            """
        )

        val injectInstanceAnnotation = SourceFile.kotlin(
            "InjectInstance.kt",
            """
            package com.reflect.instance.annotations
            
            @Target(AnnotationTarget.CLASS)
            @Retention(AnnotationRetention.SOURCE)
            annotation class InjectInstance
            """
        )

        return KotlinCompilation().apply {
            sources = listOf(autoInjectAnnotation, injectInstanceAnnotation) + sourceFiles
            symbolProcessorProviders = listOf(AutoInstanceProcessorProvider())
            inheritClassPath = true
            messageOutputStream = System.out
            workingDir = temporaryFolder.root
        }.compile()
    }

    private fun loadClass(result: KotlinCompilation.Result, className: String): Class<*> {
        val classLoader = URLClassLoader(arrayOf(result.outputDirectory.toURI().toURL()))
        return classLoader.loadClass(className)
    }
}