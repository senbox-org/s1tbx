package org.esa.beam.dataio;

import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

public class ReaderTestRunner extends BlockJUnit4ClassRunner {

    private boolean runAcceptanceTests;
    private Class<?> clazz;

    public ReaderTestRunner(Class<?> clazz) throws InitializationError {
        super(clazz);

        this.clazz = clazz;

        final String property = System.getProperty("run.reader.tests");
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
            final Description description = Description.createTestDescription(clazz, "allMethods. Reader acceptance tests disabled. Set VM param -Drun.reader.tests=true to enable.");
            runNotifier.fireTestIgnored(description);
        }
    }
}
