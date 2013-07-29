package org.esa.beam.dataio;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

public class ReaderTestRunner extends BlockJUnit4ClassRunner {

    private Class<?> clazz;

    public ReaderTestRunner(Class<?> clazz) throws InitializationError {
        super(clazz);

        this.clazz = clazz;
    }


}
