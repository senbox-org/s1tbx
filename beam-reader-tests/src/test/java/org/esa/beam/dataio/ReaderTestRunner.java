package org.esa.beam.dataio;

import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

public class ReaderTestRunner extends BlockJUnit4ClassRunner {

    private static final String EXECUTE_READER_TESTS_PROPERTYNAME = "beam.reader.tests.execute";
    private boolean runAcceptanceTests;
    private Class<?> clazz;

    public ReaderTestRunner(Class<?> clazz) throws InitializationError {
        super(clazz);

        this.clazz = clazz;

        final String property = System.getProperty(EXECUTE_READER_TESTS_PROPERTYNAME);
        runAcceptanceTests = "true".equalsIgnoreCase(property);
    }

    @Override
    public Description getDescription() {
        return Description.createSuiteDescription("Dataio Reader Test Runner");
    }

    @Override
    public void run(RunNotifier runNotifier) {
        if (runAcceptanceTests) {
            super.run(runNotifier);
        } else {
            final Description description = Description.createTestDescription(clazz, "allMethods. Reader acceptance tests disabled. " +
                                                                                     "Set VM param -D" + EXECUTE_READER_TESTS_PROPERTYNAME + "=true to enable.");
            runNotifier.fireTestIgnored(description);
        }
    }
}
