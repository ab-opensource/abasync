package com.adbrite.jmx.annotations;
import java.lang.annotation.*;

/**
 * Optional annotation that exposes all public methods in the class 
 * hierarchy (excluding Object) as MBean operations. All methods 
 * are exposed if and only if exposeAll attribute is true.  
 * <p>
 * 
 * If a more fine grained MBean attribute and operation exposure is needed 
 * do not use @MBean annotation but annotate fields and public methods directly 
 * using @ManagedOperation and @ManagedAttribute annotations.
 */

@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.TYPE })
@Inherited
public @interface MBean {
    String objectName() default "";
    boolean exposeAll() default false;
    String description() default "";
}