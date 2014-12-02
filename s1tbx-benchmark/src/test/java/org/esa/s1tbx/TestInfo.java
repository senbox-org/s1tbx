/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx;

import java.io.File;

/**
 * Test Info
 */
public class TestInfo {

    public final int num;
    public final File graphFile;
    public final File inputFolder;
    public final File expectedFolder;
    public final File outputFolder;

    public TestInfo(final int num, final String graph, final String input_products,
                    final String expected_results, final String output_products) {
        this.num = num;
        this.graphFile = new File(graph);
        this.inputFolder = new File(input_products);
        if (expected_results != null)
            this.expectedFolder = new File(expected_results);
        else
            this.expectedFolder = null;
        this.outputFolder = new File(output_products);
    }
}
