package com.bc.ceres.core;
/*
 * $Id: PrintWriterProgressMonitorTest.java,v 1.1 2007/03/28 10:43:29 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import junit.framework.TestCase;

import java.io.*;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
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