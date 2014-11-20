package org.jlinda.core.unwrapping.snaphu;

public class SnaphuParameters {

    public String outFileName;       // output filename
//    public String outFileFormat;   // output format [hgt] real4
    public String unwrapMode;        // snaphu TOPO DEFO SMOOTH NOSTATCOSTS
    public String logFileName;       // log filename for snaphu
    public String coherenceFileName; // coherence filename for snaphu opt
    public String phaseFileName;     // phase filename for snaphu opt
    public String verbosityFlag;     // snaphu TRUE or FALSE
    public String snaphuInit;        // snaphu MST or MCF

/*
    Note: I decided to remove these control flags from NEST UI. Setting them properly require some level of
          understanding of the algos implemented in Snaphu. Still they are in the snaphu.conf file, so more
          experienced user can declare/change them manually.

    // for parallel processing control
    public int nTileRow;
    public int nTileCol;
    public int rowOverlap;
    public int columnOverlap;
    public int numProcessors;
    public int tileCostThreshold;
*/

    public SnaphuParameters() {
    }

    public SnaphuParameters(String outFileName, String unwrapMode,
                            String logFileName,
                            String phaseFileName, String coherenceFileName,
                            String verbosityFlag, String snaphuInit) {
        this.outFileName = outFileName;
//        this.outFileFormat = outFileFormat; // always work with FLOAT
        this.unwrapMode = unwrapMode;
        this.logFileName = logFileName;
        this.coherenceFileName = coherenceFileName;
        this.phaseFileName = phaseFileName;
        this.verbosityFlag = verbosityFlag;
        this.snaphuInit = snaphuInit;
    }

    public void setOutFileName(String outFileName) {
        this.outFileName = outFileName;
    }

//    public void setOutFileFormat(String outFileFormat) {
//        this.outFileFormat = outFileFormat;
//    }

    public void setUnwrapMode(String unwrapMode) {
        this.unwrapMode = unwrapMode;
    }

    public void setLogFileName(String logFileName) {
        this.logFileName = logFileName;
    }

    public void setPhaseFileName(String phaseFileName) {
        this.phaseFileName = phaseFileName;
    }

    public void setCoherenceFileName(String coherenceFileName) {
        this.coherenceFileName = coherenceFileName;
    }

    public void setVerbosityFlag(String verbosityFlag) {
        this.verbosityFlag = verbosityFlag;
    }

    public void setSnaphuInit(String snaphuInit) {
        this.snaphuInit = snaphuInit;
    }
/*
    Removed for above mentioned reasons - simplicity in favor of complexity....

    public void setnTileRow(int nTileRow) {
        this.nTileRow = nTileRow;
    }

    public void setnTileCol(int nTileCol) {
        this.nTileCol = nTileCol;
    }

    public void setRowOverlap(int rowOverlap) {
        this.rowOverlap = rowOverlap;
    }

    public void setColumnOverlap(int columnOverlap) {
        this.columnOverlap = columnOverlap;
    }

    public void setNumProcessors(int numProcessors) {
        this.numProcessors = numProcessors;
    }

    public void setTileCostThreshold(int tileCostThreshold) {
        this.tileCostThreshold = tileCostThreshold;
    }
*/
}
