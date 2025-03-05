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
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class AutoInject 