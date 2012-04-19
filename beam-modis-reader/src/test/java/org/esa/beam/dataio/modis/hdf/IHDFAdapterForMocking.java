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

package org.esa.beam.dataio.modis.hdf;

import java.io.IOException;

public class IHDFAdapterForMocking implements IHDF {

    public boolean Hishdf(String path) throws IOException {
        return false;
    }

    public int Hopen(String path, int dfacc_roule) throws IOException {
        return 0;
    }

    public boolean Hclose(int fileId) throws IOException {
        return false;
    }

    public int DFKNTsize(int i) throws IOException {
        return 0;
    }

    public int SDstart(String path, int dfacc_roule) throws IOException {
        return 0;
    }

    public boolean SDend(int sdsId) throws IOException {
        return false;
    }

    public boolean SDendaccess(int sdsId) throws IOException {
        return false;
    }

    public int SDfindattr(int sdsId, String s) throws IOException {
        return 0;
    }

    public boolean SDattrinfo(int sdsId, int i, String[] strings, int[] ints) throws IOException {
        return false;
    }

    public boolean SDreadattr(int sdsId, int i, byte[] buf) throws IOException {
        return false;
    }

    public boolean SDgetinfo(int sdsId, String[] strings, int[] ints1, int[] ints2) throws IOException {
        return false;
    }

    public int SDgetdimid(int sdsId, int i) throws IOException {
        return 0;
    }

    public boolean SDdiminfo(int dimId, String[] dimName, int[] dimInfo) throws IOException {
        return false;
    }

    public void SDreaddata(int sdsId, int[] start, int[] stride, int[] count, Object buffer) throws IOException {
    }

    public int SDnametoindex(int sdId, String name) throws IOException {
        return 0;
    }

    public int SDselect(int sdId, int sdsIdx) throws IOException {
        return 0;
    }

    public boolean SDfileinfo(int sdId, int[] ints) throws IOException {
        return false;
    }

    public boolean Vstart(int fileId) throws IOException {
        return false;
    }

    public void Vend(int fileId) throws IOException {
    }

    public int Vattach(int fileId, int i, String s) throws IOException {
        return 0;
    }

    public void Vdetach(int groupId) throws IOException {
    }

    public int Vlone(int fileId, int[] ints, int i) throws IOException {
        return 0;
    }

    public void Vgetname(int groupId, String[] s) throws IOException {
    }

    public void Vgetclass(int groupId, String[] s) throws IOException {
    }
}
