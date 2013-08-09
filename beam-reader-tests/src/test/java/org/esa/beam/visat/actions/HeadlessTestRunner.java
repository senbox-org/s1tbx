package org.esa.beam.visat.actions;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import java.awt.GraphicsEnvironment;

public class HeadlessTestRunner extends BlockJUnit4ClassRunner {

    public HeadlessTestRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    public void run(RunNotifier notifier) {
        if (GraphicsEnvironment.isHeadless()) {
            notifier.fireTestIgnored(getDescription());
            return;
        }
        super.run(notifier);
    }

}