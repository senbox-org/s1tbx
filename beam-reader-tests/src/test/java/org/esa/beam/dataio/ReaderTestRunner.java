package org.esa.beam.dataio;

import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

public class ReaderTestRunner extends BlockJUnit4ClassRunner {

    private static final String PROPERTYNAME_EXECUTE_READER_TESTS = "beam.reader.tests.execute";
    private boolean runAcceptanceTests;
    private Class<?> clazz;

    public ReaderTestRunner(Class<?> clazz) throws InitializationError {
        super(clazz);

        this.clazz = clazz;

        runAcceptanceTests = Boolean.getBoolean(PROPERTYNAME_EXECUTE_READER_TESTS);
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
                                                                                     "Set VM param -D" + PROPERTYNAME_EXECUTE_READER_TESTS + "=true to enable.");
            runNotifier.fireTestIgnored(description);
        }
    }
}
