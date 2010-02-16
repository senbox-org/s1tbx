package com.bc.ceres.core.runtime;

/**
 * An dependency declared in a module.
 * <p/>
 * <p>If {@link #getDeclaringModule() declared} in a module manifest (module.xml), a dependency has the following syntax:
 * <pre>
 *    &lt;dependency&gt;
 *        &lt;module&gt;{@link #getModuleSymbolicName() moduleId}&lt;/module&gt;
 *        &lt;version&gt;{@link #getVersion() version}&lt;/version&gt;
 *    &lt;/dependency&gt;
 * </pre>
 * </p>
 * <p/>
 * Or for libraries:
 * <pre>
 *    &lt;dependency&gt;
 *        &lt;lib&gt;{@link #getLibName() libName}&lt;/lib&gt;
 *        &lt;version&gt;{@link #getVersion() version}&lt;/version&gt;
 *    &lt;/dependency&gt;
 * </pre>
 * <p/>
 * This interface is not intended to be implemented by clients.</p>
 */
public interface Dependency {

    Module getDeclaringModule();

    String getLibName();

    String getModuleSymbolicName();

    String getVersion();

    boolean isOptional();
}
