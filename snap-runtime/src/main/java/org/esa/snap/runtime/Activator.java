package org.esa.snap.runtime;

/**
 * A service allowing a module to execute code when the SNAP engine is started and stopped. Typical use cases
 * include installation/deinstallation of a module's auxiliary data or registration/deregistration of secondary
 * services.
 * <p/>
 * Engine module activators are registered using the standard JAR service provider interface, i.e. a module must
 * provide a resource file {@code META-INF/services/org.esa.snap.runtime.Activator} whose lines are fully qualified
 * names of module classes that implement the {@code Activator} interface.
 *
 * @author Norman Fomferra
 */
public interface Activator {
    /**
     * Called when the SNAP engine is started.
     */
    void start();

    /**
     * Called when the SNAP engine has stopped.
     */
    void stop();

    /**
     * The start level of the activator which defaults to zero.
     * Activators with a lower level are started before and stopped after those with a higher level.
     *
     * @return The start level of the activator.
     */
    default int getStartLevel() {
        return 0;
    }
}
