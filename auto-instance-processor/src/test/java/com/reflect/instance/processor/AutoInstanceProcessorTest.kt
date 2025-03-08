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

        // Print compilation messages for debugging
        println("Compilation messages: ${compilationOutput.messages}")
        
        // Verify that the processor processed the class
        assertTrue("Processor should have processed UserModel", 
            compilationOutput.messages.contains("Processing class: com.reflect.instance.test.UserModel"))
        
        // Verify that the processor found the @AutoInject property
        assertTrue("Processor should have found @AutoInject property", 
            compilationOutput.messages.contains("Found 1 @AutoInject properties in class com.reflect.instance.test.UserModel"))
        
        // Verify that the processor generated the injector file
        assertTrue("Processor should have generated injector file", 
            compilationOutput.messages.contains("Successfully generated injector file for com.reflect.instance.test.UserModel"))
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
        
        // Verify that the processor processed the class
        assertTrue("Processor should have processed EmptyModel", 
            compilationOutput.messages.contains("Processing class: com.reflect.instance.test.EmptyModel"))
        
        // Verify that the processor found no @AutoInject properties
        assertTrue("Processor should have found no @AutoInject properties", 
            compilationOutput.messages.contains("No @AutoInject properties found in class com.reflect.instance.test.EmptyModel"))
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

        // Print compilation messages for debugging
        println("Compilation messages: ${compilationOutput.messages}")
        
        // Verify that the processor processed the class
        assertTrue("Processor should have processed ListContainer", 
            compilationOutput.messages.contains("Processing class: com.reflect.instance.test.ListContainer"))
        
        // Verify that the processor found the @AutoInject properties
        assertTrue("Processor should have found @AutoInject properties", 
            compilationOutput.messages.contains("Found 2 @AutoInject properties in class com.reflect.instance.test.ListContainer"))
        
        // Verify that the processor generated the injector file
        assertTrue("Processor should have generated injector file", 
            compilationOutput.messages.contains("Successfully generated injector file for com.reflect.instance.test.ListContainer"))
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
        assertTrue("Error message should be in compilation output",
            compilationOutput.messages.contains("@AutoInject can only be applied to 'var' properties"))
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
        
        // Print compilation messages for debugging
        println("Compilation messages: ${compilationOutput.messages}")
        
        // Verify that the processor processed the class
        assertTrue("Processor should have processed CustomGenModel", 
            compilationOutput.messages.contains("Processing class: com.reflect.instance.test.CustomGenModel"))
        
        // Verify that the processor found the @AutoInject properties
        assertTrue("Processor should have found @AutoInject properties", 
            compilationOutput.messages.contains("Found 2 @AutoInject properties in class com.reflect.instance.test.CustomGenModel"))
        
        // Verify that the processor generated the injector file
        assertTrue("Processor should have generated injector file", 
            compilationOutput.messages.contains("Successfully generated injector file for com.reflect.instance.test.CustomGenModel"))
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
    
    /**
     * Helper function to find the generated file in the output directory
     */
    private fun findGeneratedFile(result: KotlinCompilation.Result, packagePath: String, fileName: String): File {
        // First try the expected path
        var file = File(result.outputDirectory, "ksp/sources/kotlin/$packagePath/$fileName")
        
        if (file.exists()) {
            return file
        }
        
        // If not found, search for it in the output directory
        result.outputDirectory.walkTopDown().forEach {
            if (it.isFile && it.name == fileName) {
                return it
            }
        }
        
        // Return the original path if not found
        return file
    }

}