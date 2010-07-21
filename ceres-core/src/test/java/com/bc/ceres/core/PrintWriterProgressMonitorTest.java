/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package com.bc.ceres.core;

import junit.framework.TestCase;

import java.io.*;

public class PrintWriterProgressMonitorTest extends TestCase {


    public void testSimpleProgress() throws IOException {
        StringWriter sw = new StringWriter();
        PrintWriterProgressMonitor spm = new PrintWriterProgressMonitor(new PrintWriter(sw));
        spm.beginTask("Making a Cluxi", 100);
        try {
            for (int i = 0; i < 100; i++) {
                spm.worked(1);
            }
        } finally {
            spm.done();
        }

        BufferedReader br = new BufferedReader(new StringReader(sw.toString()));
        assertEquals("Making a Cluxi, started", br.readLine());
        for (int i = 1; i <= 10; i++) {
            assertEquals("Making a Cluxi, "+ (i * 10) +"% worked", br.readLine());
        }
        assertEquals("Making a Cluxi, done", br.readLine());
        assertEquals(-1, br.read());
        br.close();
    }

}