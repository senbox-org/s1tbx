package org.esa.beam.dataio.obpg.hdf.lib;

import ncsa.hdf.hdflib.HDFException;
import ncsa.hdf.hdflib.HDFLibrary;
import org.esa.beam.dataio.obpg.hdf.IHDF;

public class HDF implements IHDF {

    private static IHDF instance = new HDF();

    public static IHDF getInstance() {
        return instance;
    }

    /**
     * By default this method should not be invoked expect for test cases
     * Can only be invoked in the same package
     *
     * @param instance the mock for test case
     */
    static void setInstance(IHDF instance) {
        HDF.instance = instance;
    }

    public synchronized boolean Hishdf(String path) throws HDFException {
        return HDFLibrary.Hishdf(path);
    }

    public synchronized int Hopen(String path, int dfacc_roule) throws HDFException {
        return HDFLibrary.Hopen(path, dfacc_roule);
    }

    public synchronized boolean Hclose(int fileId) throws HDFException {
        return HDFLibrary.Hclose(fileId);
    }

    public synchronized int DFKNTsize(int i) throws HDFException {
        return HDFLibrary.DFKNTsize(i);
    }

    public synchronized int SDstart(String path, int dfacc_roule) throws HDFException {
        return HDFLibrary.SDstart(path, dfacc_roule);
    }

    public synchronized boolean SDend(int sdsId) throws HDFException {
        return HDFLibrary.SDend(sdsId);
    }

    public synchronized boolean SDendaccess(int sdsId) throws HDFException {
        return HDFLibrary.SDendaccess(sdsId);
    }

    public synchronized int SDfindattr(int sdsId, String s) throws HDFException {
        return HDFLibrary.SDfindattr(sdsId, s);
    }

    public synchronized boolean SDattrinfo(int sdsId, int i, String[] strings, int[] ints) throws HDFException {
        return HDFLibrary.SDattrinfo(sdsId, i, strings, ints);
    }

    public synchronized boolean SDreadattr(int sdsId, int i, byte[] buf) throws HDFException {
        return HDFLibrary.SDreadattr(sdsId, i, buf);
    }

    public synchronized boolean SDgetinfo(int sdsId, String[] strings, int[] ints1, int[] ints2) throws HDFException {
        return HDFLibrary.SDgetinfo(sdsId, strings, ints1, ints2);
    }

    public synchronized int SDgetdimid(int sdsId, int i) throws HDFException {
        return HDFLibrary.SDgetdimid(sdsId, i);
    }

    public synchronized boolean SDdiminfo(int dimId, String[] dimName, int[] dimInfo) throws HDFException {
        return HDFLibrary.SDdiminfo(dimId, dimName, dimInfo);
    }

    public synchronized void SDreaddata(int sdsId, int[] start, int[] stride, int[] count, Object buffer) throws HDFException {
        HDFLibrary.SDreaddata(sdsId, start, stride, count, buffer);
    }

    public synchronized int SDnametoindex(int sdId, String name) throws HDFException {
        return HDFLibrary.SDnametoindex(sdId, name);
    }

    public synchronized int SDselect(int sdStart, int sdsIdx) throws HDFException {
        return HDFLibrary.SDselect(sdStart, sdsIdx);
    }

    public synchronized boolean SDfileinfo(int sdId, int[] ints) throws HDFException {
        return HDFLibrary.SDfileinfo(sdId, ints);
    }

    public synchronized boolean Vstart(int fileId) throws HDFException {
        return HDFLibrary.Vstart(fileId);
    }

    public synchronized void Vend(int fileId) throws HDFException {
        HDFLibrary.Vend(fileId);
    }

    public synchronized int Vattach(int fileId, int i, String s) throws HDFException {
        return HDFLibrary.Vattach(fileId, i, s);
    }

    public synchronized void Vdetach(int groupId) throws HDFException {
        HDFLibrary.Vdetach(groupId);
    }

    public synchronized int Vlone(int fileId, int[] ints, int i) throws HDFException {
        return HDFLibrary.Vlone(fileId, ints, i);
    }

    public synchronized void Vgetname(int groupId, String[] s) throws HDFException {
        HDFLibrary.Vgetname(groupId, s);
    }

    public synchronized void Vgetclass(int groupId, String[] s) throws HDFException {
        HDFLibrary.Vgetclass(groupId, s);
    }
}