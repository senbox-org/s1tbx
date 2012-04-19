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

package org.esa.beam.dataio.modis.hdf.lib;

import ncsa.hdf.hdflib.HDFException;
import ncsa.hdf.hdflib.HDFLibrary;
import org.esa.beam.dataio.modis.hdf.IHDF;

import java.io.IOException;

public class HDF implements IHDF {

    private static IHDF wrap = new HDF();

    final public static IHDF getWrap() {
        return wrap;
    }

    /**
     * By default this method should not be invoked expect for test cases
     * Can only be invoked in the same package
     *
     * @param wrap the mock for test case
     */
    final static void setWrap(IHDF wrap) {
        HDF.wrap = wrap;
    }

    public boolean Hishdf(String path) throws IOException {
        final boolean hishdf;
        try {
            hishdf = HDFLibrary.Hishdf(path);
        } catch (HDFException e) {
            throw new IOException(e.getMessage());
        }
        return hishdf;
    }

    // @todo 2 tb/** remove HDF constants fromInterface, deign enum and map internally
    public int Hopen(String path, int dfacc_roule) throws IOException {
        final int hopen;
        try {
            hopen = HDFLibrary.Hopen(path, dfacc_roule);
        } catch (HDFException e) {
            throw new IOException(e.getMessage());
        }
        return hopen;
    }

    public boolean Hclose(int fileId) throws IOException {
        final boolean hclose;
        try {
            hclose = HDFLibrary.Hclose(fileId);
        } catch (HDFException e) {
            throw new IOException(e.getMessage());
        }
        return hclose;
    }

    public int DFKNTsize(int i) throws IOException {
        final int size;
        try {
            size = HDFLibrary.DFKNTsize(i);
        } catch (HDFException e) {
            throw new IOException(e.getMessage());
        }
        return size;
    }

    public int SDstart(String path, int dfacc_roule) throws IOException {
        final int sdStart;
        try {
            sdStart = HDFLibrary.SDstart(path, dfacc_roule);
        } catch (HDFException e) {
            throw new IOException(e.getMessage());
        }
        return sdStart;
    }

    public boolean SDend(int sdsId) throws IOException {
        final boolean sdEnd;
        try {
            sdEnd = HDFLibrary.SDend(sdsId);
        } catch (HDFException e) {
            throw new IOException(e.getMessage());
        }
        return sdEnd;
    }

    public boolean SDendaccess(int sdsId) throws IOException {
        final boolean sdEndAccess;
        try {
            sdEndAccess = HDFLibrary.SDendaccess(sdsId);
        } catch (HDFException e) {
            throw new IOException(e.getMessage());
        }
        return sdEndAccess;
    }

    public int SDfindattr(int sdsId, String s) throws IOException {
        final int id;
        try {
            id = HDFLibrary.SDfindattr(sdsId, s);
        } catch (HDFException e) {
            throw new IOException(e.getMessage());
        }
        return id;
    }

    public boolean SDattrinfo(int sdsId, int i, String[] strings, int[] ints) throws IOException {
        final boolean success;
        try {
            success = HDFLibrary.SDattrinfo(sdsId, i, strings, ints);
        } catch (HDFException e) {
            throw new IOException(e.getMessage());
        }
        return success;
    }

    public boolean SDreadattr(int sdsId, int i, byte[] buf) throws IOException {
        final boolean success;
        try {
            success = HDFLibrary.SDreadattr(sdsId, i, buf);
        } catch (HDFException e) {
            throw new IOException(e.getMessage());
        }
        return success;
    }

    public boolean SDgetinfo(int sdsId, String[] strings, int[] ints1, int[] ints2) throws IOException {
        final boolean success;
        try {
            success = HDFLibrary.SDgetinfo(sdsId, strings, ints1, ints2);
        } catch (HDFException e) {
            throw new IOException(e.getMessage());
        }
        return success;
    }

    public int SDgetdimid(int sdsId, int i) throws IOException {
        final int id;
        try {
            id = HDFLibrary.SDgetdimid(sdsId, i);
        } catch (HDFException e) {
            throw new IOException(e.getMessage());
        }
        return id;
    }

    public boolean SDdiminfo(int dimId, String[] dimName, int[] dimInfo) throws IOException {
        final boolean success;
        try {
            success = HDFLibrary.SDdiminfo(dimId, dimName, dimInfo);
        } catch (HDFException e) {
            throw new IOException(e.getMessage());
        }
        return success;
    }

    public void SDreaddata(int sdsId, int[] start, int[] stride, int[] count, Object buffer) throws IOException {
        try {
            HDFLibrary.SDreaddata(sdsId, start, stride, count, buffer);
        } catch (HDFException e) {
            throw new IOException(e.getMessage());
        }
    }

    public int SDnametoindex(int sdId, String name) throws IOException {
        final int index;
        try {
            index = HDFLibrary.SDnametoindex(sdId, name);
        } catch (HDFException e) {
            throw new IOException(e.getMessage());
        }
        return index;
    }

    public int SDselect(int sdId, int sdsIdx) throws IOException {
        final int select;
        try {
            select = HDFLibrary.SDselect(sdId, sdsIdx);
        } catch (HDFException e) {
            throw new IOException(e.getMessage());
        }
        return select;
    }

    public boolean SDfileinfo(int sdId, int[] ints) throws IOException {
        final boolean success;
        try {
            success = HDFLibrary.SDfileinfo(sdId, ints);
        } catch (HDFException e) {
            throw new IOException(e.getMessage());
        }
        return success;
    }

    public boolean Vstart(int fileId) throws IOException {
        final boolean success;
        try {
            success = HDFLibrary.Vstart(fileId);
        } catch (HDFException e) {
            throw new IOException(e.getMessage());
        }
        return success;
    }

    public void Vend(int fileId) throws IOException {
        try {
            HDFLibrary.Vend(fileId);
        } catch (HDFException e) {
            throw new IOException(e.getMessage());
        }
    }

    public int Vattach(int fileId, int i, String s) throws IOException {
        final int id;
        try {
            id = HDFLibrary.Vattach(fileId, i, s);
        } catch (HDFException e) {
            throw new IOException(e.getMessage());
        }
        return id;
    }

    public void Vdetach(int groupId) throws IOException {
        try {
            HDFLibrary.Vdetach(groupId);
        } catch (HDFException e) {
            throw new IOException(e.getMessage());
        }
    }

    public int Vlone(int fileId, int[] ints, int i) throws IOException {
        final int id;
        try {
            id = HDFLibrary.Vlone(fileId, ints, i);
        } catch (HDFException e) {
            throw new IOException(e.getMessage());
        }
        return id;
    }

    public void Vgetname(int groupId, String[] s) throws IOException {
        try {
            HDFLibrary.Vgetname(groupId, s);
        } catch (HDFException e) {
            throw new IOException(e.getMessage());
        }
    }

    public void Vgetclass(int groupId, String[] s) throws IOException {
        try {
            HDFLibrary.Vgetclass(groupId, s);
        } catch (HDFException e) {
            throw new IOException(e.getMessage());
        }
    }
}
