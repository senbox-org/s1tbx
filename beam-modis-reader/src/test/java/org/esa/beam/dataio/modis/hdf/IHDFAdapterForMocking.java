package org.esa.beam.dataio.modis.hdf;

import ncsa.hdf.hdflib.HDFException;

public class IHDFAdapterForMocking implements IHDF {

    public boolean Hishdf(String path) throws HDFException {
        return false;
    }

    public int Hopen(String path, int dfacc_roule) throws HDFException {
        return 0;
    }

    public boolean Hclose(int fileId) throws HDFException {
        return false;
    }

    public int DFKNTsize(int i) throws HDFException {
        return 0;
    }

    public int SDstart(String path, int dfacc_roule) throws HDFException {
        return 0;
    }

    public boolean SDend(int sdsId) throws HDFException {
        return false;
    }

    public boolean SDendaccess(int sdsId) throws HDFException {
        return false;
    }

    public int SDfindattr(int sdsId, String s) throws HDFException {
        return 0;
    }

    public boolean SDattrinfo(int sdsId, int i, String[] strings, int[] ints) throws HDFException {
        return false;
    }

    public boolean SDreadattr(int sdsId, int i, byte[] buf) throws HDFException {
        return false;
    }

    public boolean SDgetinfo(int sdsId, String[] strings, int[] ints1, int[] ints2) throws HDFException {
        return false;
    }

    public int SDgetdimid(int sdsId, int i) throws HDFException {
        return 0;
    }

    public boolean SDdiminfo(int dimId, String[] dimName, int[] dimInfo) throws HDFException {
        return false;
    }

    public void SDreaddata(int sdsId, int[] start, int[] stride, int[] count, Object buffer) throws HDFException {
    }

    public int SDnametoindex(int sdId, String name) throws HDFException {
        return 0;
    }

    public int SDselect(int sdId, int sdsIdx) throws HDFException {
        return 0;
    }

    public boolean SDfileinfo(int sdId, int[] ints) throws HDFException {
        return false;
    }

    public boolean Vstart(int fileId) throws HDFException {
        return false;
    }

    public void Vend(int fileId) throws HDFException {
    }

    public int Vattach(int fileId, int i, String s) throws HDFException {
        return 0;
    }

    public void Vdetach(int groupId) throws HDFException {
    }

    public int Vlone(int fileId, int[] ints, int i) throws HDFException {
        return 0;
    }

    public void Vgetname(int groupId, String[] s) throws HDFException {
    }

    public void Vgetclass(int groupId, String[] s) throws HDFException {
    }
}
