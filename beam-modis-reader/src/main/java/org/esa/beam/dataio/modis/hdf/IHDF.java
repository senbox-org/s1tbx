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

public interface IHDF {

    boolean Hishdf(String path) throws IOException;

    int Hopen(String path, int dfacc_roule) throws IOException;

    boolean Hclose(int fileId) throws IOException;

    int DFKNTsize(int i) throws IOException;

    int SDstart(String path, int dfacc_roule) throws IOException;

    boolean SDend(int sdsId) throws IOException;

    boolean SDendaccess(int sdsId) throws IOException;

    int SDfindattr(int sdsId, String s) throws IOException;

    boolean SDattrinfo(int sdsId, int i, String[] strings, int[] ints) throws IOException;

    boolean SDreadattr(int sdsId, int i, byte[] buf) throws IOException;

    boolean SDgetinfo(int sdsId, String[] strings, int[] ints1, int[] ints2) throws IOException;

    int SDgetdimid(int sdsId, int i) throws IOException;

    boolean SDdiminfo(int dimId, String[] dimName, int[] dimInfo) throws IOException;

    void SDreaddata(int sdsId, int[] start, int[] stride, int[] count, Object buffer) throws IOException;

    int SDnametoindex(int sdId, String name) throws IOException;

    int SDselect(int sdId, int sdsIdx) throws IOException;

    boolean SDfileinfo(int sdId, int[] ints) throws IOException;

    boolean Vstart(int fileId) throws IOException;

    void Vend(int fileId) throws IOException;

    int Vattach(int fileId, int i, String s) throws IOException;

    void Vdetach(int groupId) throws IOException;

    int Vlone(int fileId, int[] ints, int i) throws IOException;

    void Vgetname(int groupId, String[] s) throws IOException;

    void Vgetclass(int groupId, String[] s) throws IOException;
}
