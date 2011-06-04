package org.esa.beam.dataio.spot;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

/**
 * todo - add api doc
 *
 * @author Norman Fomferra
 */
public class TestDirRunner extends BlockJUnit4ClassRunner {

    public TestDirRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    public void run(RunNotifier notifier) {
        if (!TestDataDir.available()) {
            notifier.fireTestIgnored(getDescription());
            return;
        }
        super.run(notifier);
    }

}
