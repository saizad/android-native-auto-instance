package com.reflect.instance.annotations

/**
 * Annotation to mark a class for automatic instance injection.
 * All properties marked with @AutoInject in this class will be automatically injected.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class InjectInstance

/**
 * Annotation to mark a property for automatic injection.
 * The property must be in a class marked with @InjectInstance.
 * 
 * @param count Number of instances to inject (default is 1)
 * @param dataGenerator Optional source object to use for injection
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class AutoInject(
    val count: Int = 1,
    val dataGenerator: String = ""
) 