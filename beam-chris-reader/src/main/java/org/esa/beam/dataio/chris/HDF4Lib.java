package org.esa.beam.dataio.chris;

import ncsa.hdf.hdflib.HDFConstants;
import ncsa.hdf.hdflib.HDFException;
import ncsa.hdf.hdflib.HDFLibrary;

class HDF4Lib {

    public static int Hopen(String path, int openMode) throws HDFException {
        int fileId = HDFLibrary.Hopen(path, openMode);
        if (fileId == HDFConstants.FAIL) {
            throw new HDFException("Hopen");
        }
        return fileId;
    }

    public static void Hclose(int fileId) throws HDFException {
        HDFLibrary.Hclose(fileId);
    }

    public static void Vstart(int fileId) throws HDFException {
        if (!HDFLibrary.Vstart(fileId)) {
            throw new HDFException("Vstart");
        }
    }

    public static void Vend(int fileId) throws HDFException {
        HDFLibrary.Vend(fileId);
    }

    public static int VSfind(int fileId, String vdataName) throws HDFException {
        int refNo = HDFLibrary.VSfind(fileId, vdataName);
        return refNo;
    }

    public static int VSattach(int fileId, int refNo, String openMode) throws HDFException {
        int vdataId = HDFLibrary.VSattach(fileId, refNo, openMode);
        if (vdataId == HDFConstants.FAIL) {
            throw new HDFException("VSattach");
        }
        return vdataId;
    }

    public static void VSQuerycount(int vdataId, int[] numRecordsBuffer) throws HDFException {
        if (!HDFLibrary.VSQuerycount(vdataId, numRecordsBuffer)) {
            throw new HDFException("VSQuerycount");
        }
    }

    public static void VSread(int vdataId, float[] record) throws HDFException {
        int numRecs = HDFLibrary.VSread(vdataId, record, 1, HDFConstants.FULL_INTERLACE);
        if (numRecs != 1) {
            throw new HDFException("VSread");
        }
    }

    public static void VSseek(int vdataId, int recordIndex) throws HDFException {
        if (HDFLibrary.VSseek(vdataId, recordIndex) != recordIndex) {
            throw new HDFException("VSseek");
        }
    }

    public static void VSsetfields(int vdataId, String fields) throws HDFException {
        if (!HDFLibrary.VSsetfields(vdataId, fields)) {
            throw new HDFException("VSsetfields");
        }
    }

    public static int SDstart(String path, int openMode) throws HDFException {
        int sdId = HDFLibrary.SDstart(path, openMode);
        if (sdId == HDFConstants.FAIL) {
            throw new HDFException("SDstart");
        }
        return sdId;
    }

    public static void SDend(int sdId) throws HDFException {
        if (!HDFLibrary.SDend(sdId)) {
            throw new HDFException("SDend");
        }
    }

    public static void SDendaccess(int sdsId) throws HDFException {
        if (!HDFLibrary.SDendaccess(sdsId)) {
            throw new HDFException("SDendaccess");
        }
    }

    public static void SDreaddata(int sdsId, int[] start, int[] stride, int[] count, int[] data) throws HDFException {
        if (!HDFLibrary.SDreaddata_int(sdsId,
                                       start,
                                       stride,
                                       count,
                                       data)) {
            throw new HDFException("SDreaddata_int");
        }
    }

    public static void SDreaddata(int sdsId, int[] start, int[] stride, int[] count, short[] data) throws HDFException {
        if (!HDFLibrary.SDreaddata_short(sdsId,
                                         start,
                                         stride,
                                         count,
                                         data)) {
            throw new HDFException("SDreaddata_short");
        }
    }


    public static void SDfileinfo(int sdId, int[] fileInfo) throws HDFException {
        if (!HDFLibrary.SDfileinfo(sdId, fileInfo)) {
            throw new HDFException("SDfileinfo");
        }
    }

    public static void SDattrinfo(int sdId, int index, String[] nameBuffer, int[] attributeInfo) throws HDFException {
        if (!HDFLibrary.SDattrinfo(sdId, index, nameBuffer, attributeInfo)) {
            throw new HDFException("SDattrinfo");
        }
    }

    public static void SDreadattr(int sdId, int index, byte[] data) throws HDFException {
        if (!HDFLibrary.SDreadattr(sdId, index, data)) {
            throw new HDFException("SDreadattr");
        }
    }

    public static void SDgetinfo(int sdsId, String[] nameBuffer, int[] dimSizes, int[] sdsInfo) throws HDFException {
        if (!HDFLibrary.SDgetinfo(sdsId, nameBuffer, dimSizes, sdsInfo)) {
            throw new HDFException("SDgetinfo");
        }
    }

    public static int SDnametoindex(int sdId, String sdsName) throws HDFException {
        int sdsIndex = HDFLibrary.SDnametoindex(sdId, sdsName);
        return sdsIndex;
    }

    public static int SDselect(int sdId, int sdsIndex) throws HDFException {
        int sdsId = HDFLibrary.SDselect(sdId, sdsIndex);
        if (sdsId == HDFConstants.FAIL) {
            throw new HDFException("SDselect");
        }
        return sdsId;
    }
}
