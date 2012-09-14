package org.jdoris.core.todo_classes;

import org.jdoris.core.Window;
import java.io.File;

public class Input {

    public class General {
        String logfile;
        String m_resfile;
        String s_resfile;
        String i_resfile;
        long memory;                  // available mem. in Bytes
        boolean process;              // if .[i] != 0 => process step_(i+1)
        boolean interactive;          // if true, pause
        boolean overwrite;             // 0: don't overwrite existing data output files
        long orbitInterpMethod;              // method for orbit interpolation
        long dumpBaselineL;           // #lines to dump baseline param.
        long dumpBaselineP;           // #lines to dump baseline param.
        long preview;                 // generate sunraster preview file
        float terrainHeight;         // mean terrain height, or of a point.
    }


    public class Orbits {
        long timeInterval;     // time in sec.
        long timeBefore;       // sec before first line.
        float dumpMasterOrbit; // dtime in sec.
        float dumpSlaveOrbit;  // dtime in sec.
    }

    public class FilterAzimuth {

        String method;                 // method selector
        long fftLength;                // length per buffer
        long overlap;                  // 0.5overlap each buffer
        double hammingAlpha;           // alpha for hamming, 1 is no

    }

    public class FiltRange {
        String method;              // method selector
        long ovsmpFactor;           // factor
        boolean doWeightCorrel;     // weighting of correlation values
        long nlMean;                // number of lines to take mean of
        long fftLength;             // length for adaptive
        long overlap;               // half overlap between blocks of fftLength
        double hammingAlpha;        // alpha for hamming
        double snrThreshold;        // spectral peak estimation
        double terrainSlope;        // [rad] porbits method only
    }

    public class Resample {

        // arguments for resampling slave
        String method;                 // method selector (interpolator)
        Window dbow_geo;               // cut out of original master.geo
        Window dbow;                   // cut out of original master.radar
        boolean shiftAziSpectra;        // [true] shift spectrum to 0

    }

    public class Interferogram {
        // arguments for computation interferogram
        String method;              // method selector
        int multiLookL;             // multilookfactor in line dir.
        int multiLookP;             // multilookfactor in pixel dir.
    }


    public class FiltPhase {
        String method;                 // method selector
        int finumlines;                // number of lines input
        double alpha;                  // weighting
        long blockSize;                // blockSize filtered blocks
        long overlap;                  // half overlap

        // kernel file and/or array
        File fikernel2d; // input filename
        float[] kernel; // e.g. [1 1 1]
    }


    public class CompRefPhase {
        // arguments for flatearth correction.
        String method;               // method selector
        int degree;                  // degree of polynomial
        int Npoints;                 // number of observations
        File inputFilePositions;     // input file name for positions
    }

    public class SubRefPhase {               // arguments for subtract 'flat earth'

        String method;              // method selector
        int multiLookL;             // multilookfactor in line dir.
        int multiLookP;             // multilookfactor in pixel dir.
        boolean dumpOnlyRefpha;     // do nothing except dump refpha
    }


    public class Coherence {
        // arguments for computation coherence
        String method;
        int multilookL;             // multilookfactor in line dir.
        int multilookP;             // multilookfactor in pixel dir.
        int cohsizeL;               // size of estimation window coherence
        int cohsizeP;               // size of estimation window coherence
    }


    public class CompRefDem {
        // arguments for reference phase from DEM

        String method;                 // method selector
        int demRows;                   // number of
        int demCols;                   // number of
        double demDeltaLat;            // radians
        double demDeltaLon;            // radians
        double demLatLeftUpper;        // radians
        double demLonLeftUpper;        // radians
        double demNodata;              // identifier/flag
        boolean includeRefPha;         // flag to include_flatearth correction
    }

    public class SubRefDem {
        // arguments for subtract reference DEM
        String method;                 // method selector
        long offsetL;                  // offset applied before subtraction
        long offsetP;                  // offset applied before subtraction
    }

}
