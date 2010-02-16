package org.esa.beam.dataio.modis.hdf.lib;

import ncsa.hdf.hdflib.HDFException;
import ncsa.hdf.hdflib.HDFLibrary;
import org.esa.beam.dataio.modis.hdf.IHDF;

public class HDF implements IHDF {

    private static IHDF wrap = new HDF();

    final public static IHDF getWrap() {
        return wrap;
    }

    /**
     * By default this method should not be invoked expect for test cases
     * Can only be invoked in the same package
     * @param wrap the mock for test case
     */
    final static void setWrap(IHDF wrap) {
        HDF.wrap = wrap;
    }

    public boolean Hishdf(String path) throws HDFException {
        return HDFLibrary.Hishdf(path);
    }

    public int Hopen(String path, int dfacc_roule) throws HDFException {
        return HDFLibrary.Hopen(path, dfacc_roule);
    }

    public boolean Hclose(int fileId) throws HDFException {
        return HDFLibrary.Hclose(fileId);
    }

    public int DFKNTsize(int i) throws HDFException {
        return HDFLibrary.DFKNTsize(i);
    }

    public int SDstart(String path, int dfacc_roule) throws HDFException {
        return HDFLibrary.SDstart(path, dfacc_roule);
    }

    public boolean SDend(int sdsId) throws HDFException {
        return HDFLibrary.SDend(sdsId);
    }

    public boolean SDendaccess(int sdsId) throws HDFException {
        return HDFLibrary.SDendaccess(sdsId);
    }

    public int SDfindattr(int sdsId, String s) throws HDFException {
        return HDFLibrary.SDfindattr(sdsId, s);
    }

    public boolean SDattrinfo(int sdsId, int i, String[] strings, int[] ints) throws HDFException {
        return HDFLibrary.SDattrinfo(sdsId, i, strings, ints);
    }

    public boolean SDreadattr(int sdsId, int i, byte[] buf) throws HDFException {
        return HDFLibrary.SDreadattr(sdsId, i, buf);
    }

    public boolean SDgetinfo(int sdsId, String[] strings, int[] ints1, int[] ints2) throws HDFException {
        return HDFLibrary.SDgetinfo(sdsId, strings, ints1, ints2);
    }

    public int SDgetdimid(int sdsId, int i) throws HDFException {
        return HDFLibrary.SDgetdimid(sdsId, i);
    }

    public boolean SDdiminfo(int dimId, String[] dimName, int[] dimInfo) throws HDFException {
        return HDFLibrary.SDdiminfo(dimId, dimName, dimInfo);
    }

    public void SDreaddata(int sdsId, int[] start, int[] stride, int[] count, Object buffer) throws HDFException {
        HDFLibrary.SDreaddata(sdsId, start, stride, count, buffer);
    }

    public int SDnametoindex(int sdId, String name) throws HDFException {
        return HDFLibrary.SDnametoindex(sdId, name);
    }

    public int SDselect(int sdId, int sdsIdx) throws HDFException {
        return HDFLibrary.SDselect(sdId, sdsIdx);
    }

    public boolean SDfileinfo(int sdId, int[] ints) throws HDFException {
        return HDFLibrary.SDfileinfo(sdId, ints);
    }

    public boolean Vstart(int fileId) throws HDFException {
        return HDFLibrary.Vstart(fileId);
    }

    public void Vend(int fileId) throws HDFException {
        HDFLibrary.Vend(fileId);
    }

    public int Vattach(int fileId, int i, String s) throws HDFException {
        return HDFLibrary.Vattach(fileId, i, s);
    }

    public void Vdetach(int groupId) throws HDFException {
        HDFLibrary.Vdetach(groupId);
    }

    public int Vlone(int fileId, int[] ints, int i) throws HDFException {
        return HDFLibrary.Vlone(fileId, ints, i);
    }

    public void Vgetname(int groupId, String[] s) throws HDFException {
        HDFLibrary.Vgetname(groupId, s);
    }

    public void Vgetclass(int groupId, String[] s) throws HDFException {
        HDFLibrary.Vgetclass(groupId, s);
    }
}
