/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning.operator;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;

/**
 * Note: this class is unused so far because it is not yet fit for purpose.
 *
 * @author Thomas Storm
 */
class MemoryMappedFileCleaner {

    static void cleanup(RandomAccessFile raf, MappedByteBuffer buffer) throws IOException {
        if(raf != null) {
            raf.close();
        }
        // due to Java bug: workaround needed in order to be able to delete temporary file
        // see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4715154
        // See org.esa.beam.binning.operator.MappedByteBufferTest for valid use cases.
        unmap(buffer);
    }

    private static void unmap(final MappedByteBuffer buffer) {
        if(buffer == null) {
            return;
        }
        if(buffer.isDirect()) {
            ((sun.nio.ch.DirectBuffer) buffer).cleaner().clean();
        }
    }
}
