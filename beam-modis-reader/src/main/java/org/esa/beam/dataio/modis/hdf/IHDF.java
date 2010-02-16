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
