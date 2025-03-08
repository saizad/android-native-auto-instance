package com.reflect.instance.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AutoInstanceProcessorTest {
    
    @Rule
    @JvmField
    val temporaryFolder = TemporaryFolder()
    
    private lateinit var compilationOutput: KotlinCompilation.Result
    
    @Before
    fun setup() {
        // Set up will be implemented in specific tests
    }

    @Test
    fun `test processor finds classes with InjectInstance annotation`() {
        // Create test source file
        val source = SourceFile.kotlin("TestClass.kt", """
        package com.reflect.instance.test
        
        import com.reflect.instance.annotations.InjectInstance
        import com.reflect.instance.annotations.AutoInject

        data class Profile(val firstName: String, val lastName: String)

        @InjectInstance
        class UserModel {
            @AutoInject
            lateinit var profile: Profile
            
        }
        
        
    """.trimIndent())

        // Compile with our processor
        compilationOutput = compile(source)

        // Verify compilation succeeded
        assertEquals(KotlinCompilation.ExitCode.OK, compilationOutput.exitCode)

        // Check if injector was generated for UserModel
        val generatedFile = File(compilationOutput.outputDirectory,
            "ksp/sources/kotlin/com/reflect/instance/test/UserModelInjector.kt")
        assertTrue("Injector file should be generated", generatedFile.exists())

        // Check file content
        val fileContent = generatedFile.readText()

        assert(fileContent.contains("object UserModelInjector"))
        assert(fileContent.contains("fun inject(target: UserModel)"))
        assert(fileContent.contains("target.profile =")) // Ensure profile is injected
        assert(fileContent.contains("target.age ="))
    }


    @Test
    fun `test processor skips classes without AutoInject properties`() {
        val source = SourceFile.kotlin("EmptyTest.kt", """
            package com.reflect.instance.test
            
            import com.reflect.instance.annotations.InjectInstance
            
            @InjectInstance
            class EmptyModel {
                // No @AutoInject properties
                var name: String = ""
            }
        """.trimIndent())
        
        compilationOutput = compile(source)
        
        // Verify compilation succeeded
        assertEquals(KotlinCompilation.ExitCode.OK, compilationOutput.exitCode)
        
        // Check that no injector was generated
        val generatedFile = File(compilationOutput.outputDirectory, 
            "ksp/sources/kotlin/com/reflect/instance/test/EmptyModelInjector.kt")
        assertFalse("No injector file should be generated", generatedFile.exists())
    }
    
    @Test
    fun `test processor handles list properties correctly`() {
        val source = SourceFile.kotlin("ListTest.kt", """
            package com.reflect.instance.test
            
            import com.reflect.instance.annotations.InjectInstance
            import com.reflect.instance.annotations.AutoInject
            
            @InjectInstance
            class ListContainer {
                @AutoInject(count = 5)
                lateinit var items: List<String>
                
                @AutoInject
                lateinit var singleCountList: List<Int>
            }
        """.trimIndent())
        
        compilationOutput = compile(source)
        
        // Verify compilation succeeded

        assertEquals(KotlinCompilation.ExitCode.OK, compilationOutput.exitCode)


        // Check injector content
        val generatedFile = File(compilationOutput.outputDirectory, 
            "ksp/sources/kotlin/com/reflect/instance/test/ListContainerInjector.kt")
        assertTrue("Injector file should be generated", generatedFile.exists())
        
        val fileContent = generatedFile.readText()
        assert(fileContent.contains( "target.items =  List(5) { Any() as kotlin.String }"))
        assert(fileContent.contains( "target.singleCountList =  List(1) { Any() as kotlin.Int }"))
    }

    @Test
    fun `test processor reports error for val properties with AutoInject`() {
        val source = SourceFile.kotlin("InvalidTest.kt", """
        package com.reflect.instance.test
        
        import com.reflect.instance.annotations.InjectInstance
        import com.reflect.instance.annotations.AutoInject
        
        @InjectInstance
        class InvalidModel {
            @AutoInject
            val immutableProperty: String = ""  // This should cause an error
            
            @AutoInject
            var validProperty: Int = 0
        }
    """.trimIndent())

        compilationOutput = compile(source)

        // Expect compilation to fail with COMPILATION_ERROR exit code
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, compilationOutput.exitCode)

        // Verify the error message appears in the compilation messages
        assert(compilationOutput.messages.contains( "@AutoInject can only be applied to 'var' properties"))

        // Since compilation failed, no injector file should be generated
        val generatedFile = File(compilationOutput.outputDirectory,
            "ksp/sources/kotlin/com/reflect/instance/test/InvalidModelInjector.kt")
        assertFalse("No injector file should be generated", generatedFile.exists())
    }
    
    @Test
    fun `test processor handles custom data generator parameter`() {
        val source = SourceFile.kotlin("CustomGenTest.kt", """
            package com.reflect.instance.test
            
            import com.reflect.instance.annotations.InjectInstance
            import com.reflect.instance.annotations.AutoInject
            
            @InjectInstance
            class CustomGenModel {
                @AutoInject(dataGenerator = "RandomStringGenerator")
                lateinit var customString: String
                
                @AutoInject(dataGenerator = "UserFactory", count = 3)
                lateinit var users: List<String>
            }
        """.trimIndent())
        
        compilationOutput = compile(source)
        
        // Verify compilation succeeded
        assertEquals(KotlinCompilation.ExitCode.OK, compilationOutput.exitCode)
        
        // Check the custom generator is included in the comment
        val generatedFile = File(compilationOutput.outputDirectory, 
            "ksp/sources/kotlin/com/reflect/instance/test/CustomGenModelInjector.kt")
        assertTrue("Injector file should be generated", generatedFile.exists())
        
        val fileContent = generatedFile.readText()
        assert(fileContent.contains( "target.customString = /** RandomStringGenerator **/ Any() as kotlin.String"))
        assert(fileContent.contains( "target.users = /** UserFactory **/ List(3) { Any() as kotlin.String }"))
    }
    
    /**
     * Helper function to compile test sources with the processor
     */
    private fun compile(vararg sourceFiles: SourceFile): KotlinCompilation.Result {
        return KotlinCompilation().apply {
            sources = sourceFiles.toList()
            symbolProcessorProviders = listOf(AutoInstanceProcessorProvider())
            inheritClassPath = true
            messageOutputStream = System.out
            workingDir = temporaryFolder.root
        }.compile()
    }

}