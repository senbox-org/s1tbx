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

package org.esa.snap.core.util.io;

import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.*;

public class CsvWriterTest {

    @Test
    public void testWriteRecord() throws IOException {
        StringWriter out = new StringWriter();

        CsvWriter csvWriter = new CsvWriter(out, "\t");
        csvWriter.writeRecord("a", "b", "c");
        csvWriter.writeRecord("b", "c", "d");
        csvWriter.close();

        assertEquals("a\tb\tc\n" +
                             "b\tc\td\n", out.toString());
    }

    @Test
    public void testWriteDoubleRecord() throws IOException {
        StringWriter out = new StringWriter();

        CsvWriter csvWriter = new CsvWriter(out, "\t");
        csvWriter.writeRecord(1.2, 2.3, 3.4);
        csvWriter.writeRecord(4.5, 5.6, 6.7);
        csvWriter.close();

        assertEquals("1.2\t2.3\t3.4\n" +
                             "4.5\t5.6\t6.7\n", out.toString());
    }

}
