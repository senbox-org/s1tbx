package org.esa.snap.runtime;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This class is used to launch a stand-alone SNAP Engine application.
 * It requires a system property {@code snap.mainClass} ({@link #PROPERTY_MAIN_CLASS_NAME}) to be set to the fully qualified name of a class providing the Java
 * main method: {@code public static void main(String[] args)}.
 *
 * @author Norman Fomferra
 * @see Config
 * @see Engine
 * @since SNAP 2.0
 */
public class Launcher {

    /**
     * The name of the property providing the main class name.
     */
    public static final String PROPERTY_MAIN_CLASS_NAME = "snap.mainClass";

    /**
     * Creates an instance of this launcher and calls {@link #run}.
     *
     * @param args The command-line arguments passed unchanged to {@link #run}.
     */
    public static void main(String[] args) {
        try {
            Launcher launcher = new Launcher();
            launcher.run(args);
        } catch (Throwable e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    /**
     * Loads the SNAP Engine {@link Config#load() configuration} and invokes the {@code main} method of the class given by the
     * system property {@link #PROPERTY_MAIN_CLASS_NAME} by using a context class loader derived from the loaded configuration.
     *
     * @param args The command-line arguments passed to the {@code main} method of the class given by the
     *             system property {@link #PROPERTY_MAIN_CLASS_NAME}.
     */
    public void run(String[] args) {
        String mainClassName = System.getProperty(PROPERTY_MAIN_CLASS_NAME);
        if (mainClassName == null) {
            throw new RuntimeException(String.format("Missing system property '%s'", PROPERTY_MAIN_CLASS_NAME));
        }
        Engine.start().runClientCode(() -> {
            try {
                Class<?> mainClass = Engine.getInstance().getClientClassLoader().loadClass(mainClassName);
                Method mainMethod = mainClass.getMethod("main", String[].class);
                mainMethod.invoke(null, new Object[]{args});
            } catch (ClassNotFoundException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }).stop();
    }
}
