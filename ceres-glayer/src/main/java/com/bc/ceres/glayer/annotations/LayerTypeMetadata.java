package com.bc.ceres.glayer.annotations;

import java.lang.annotation.*;

/**
 * An annotation providing metadata for {@link com.bc.ceres.glayer.LayerType}s.
 * This annotation is not inherited.
 *
 * @author Norman Fomferra
 * @since Ceres 0.13
 */
@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
public @interface LayerTypeMetadata {
    /**
     * @return The name of the layer type.
     */
    String name() default "";

    /**
     * @return An array of alias names for the layer type. The primary usage of alias names is providing backward
     *         compatibility with layer types that have been renamed.
     */
    String[] aliasNames() default {};
}
