/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

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
