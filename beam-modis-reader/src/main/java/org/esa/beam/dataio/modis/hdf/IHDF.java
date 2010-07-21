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

import ncsa.hdf.hdflib.HDFException;

public interface IHDF {

    boolean Hishdf(String path) throws HDFException;

    int Hopen(String path, int dfacc_roule) throws HDFException;

    boolean Hclose(int fileId) throws HDFException;

    int DFKNTsize(int i) throws HDFException;

    int SDstart(String path, int dfacc_roule) throws HDFException;

    boolean SDend(int sdsId) throws HDFException;

    boolean SDendaccess(int sdsId) throws HDFException;

    int SDfindattr(int sdsId, String s) throws HDFException;

    boolean SDattrinfo(int sdsId, int i, String[] strings, int[] ints) throws HDFException;

    boolean SDreadattr(int sdsId, int i, byte[] buf) throws HDFException;

    boolean SDgetinfo(int sdsId, String[] strings, int[] ints1, int[] ints2) throws HDFException;

    int SDgetdimid(int sdsId, int i) throws HDFException;

    boolean SDdiminfo(int dimId, String[] dimName, int[] dimInfo) throws HDFException;

    void SDreaddata(int sdsId, int[] start, int[] stride, int[] count, Object buffer) throws HDFException;

    int SDnametoindex(int sdId, String name) throws HDFException;

    int SDselect(int sdId, int sdsIdx) throws HDFException;

    boolean SDfileinfo(int sdId, int[] ints) throws HDFException;

    boolean Vstart(int fileId) throws HDFException;

    void Vend(int fileId) throws HDFException;

    int Vattach(int fileId, int i, String s) throws HDFException;

    void Vdetach(int groupId) throws HDFException;

    int Vlone(int fileId, int[] ints, int i) throws HDFException;

    void Vgetname(int groupId, String[] s) throws HDFException;

    void Vgetclass(int groupId, String[] s) throws HDFException;
}
