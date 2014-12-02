package org.jlinda.core.todo_classes;

import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jblas.FloatMatrix;
import org.jlinda.core.Window;

import java.io.File;

/**
 * User: pmar@ppolabs.com
 * Date: 4/8/11
 * Time: 5:45 PM
 */
public class todo_classes {

    private File filename;

    public class productinfo {

        public File file;
        public Window win;
        public int multilookL;
        public int multilookP;

        // file format flat
        public int formatflag;

        public productinfo() {
            formatflag = -1;// undefined
            multilookL = 1;
            multilookP = 1;
        }

//        // ______ fill it from info in resultfiles ______
//        public void fillproductinfo(const file) {
//
//        }

        /*  // ______ assignment operator ______
          productinfo& operator = (productinfo X)
            {
            if (this != &X)
              {
              strcpy(file,X.file);
              win        = X.win;
              multilookL = X.multilookL;
              multilookP = X.multilookP;
              formatflag = X.formatflag;
              }
            return *this;
            };*/

        // ______ read data from file ______
        public DoubleMatrix readphase(Window win) {
            return null;
        }

        public ComplexDoubleMatrix readdata(Window win) {
            return null;
        }

        public FloatMatrix readdatar4(Window win) {
            return null;
        }

        // ______ show content ______
        public void showdata() {
            System.out.println("current file: \t" + file.getName()
                    + "formatflag:   \t" + formatflag
                    + "multilook:    \t" + multilookL + " " + multilookP
                    + "window:       \t" + win.linelo + " " + win.linehi
                    + " " + win.pixlo + " " + win.pixhi);


        }


    }


    public class inputgeneral {
        String logfile;
        String m_resfile;
        String s_resfile;
        String i_resfile;
        long memory;                 // available mem. in Bytes
        //        boolean process[NUMPROCESSES];  // if .[i] != 0 => process step_(i+1)
        boolean process;  // if .[i] != 0 => process step_(i+1)
        boolean interactive;            // if true, pause
        boolean overwrit;               // 0: don't overwrite existing data output files
        long orb_interp;             // method for orbit interpolation
        long dumpbaselineL;          // #lines to dump baseline param.
        long dumpbaselineP;          // #lines to dump baseline param.
        long preview;                // generate sunraster preview file
        // 0: no; 1: sh files; 2: sh sh_files.
        float terrain_height;         // mean terrain height, or of a point.
    }


    public class input_filtphase {
        String method;                 // method selector
        File fofiltphase;   // output filename
        File fifiltphase;   // input filename
        int finumlines;             // number of lines input
        // ______ method goldstein ______
        double alpha;                  // weighting
        long blocksize;              // blockSize filtered blocks
        long overlap;                // half overlap
        // ______ method goldstein, spatial conv. and spectral ______
        //        matrix<real4> kernel;                 // e.g. [1 1 1]
        FloatMatrix kernel; // e.g. [1 1 1]
        // ______ method spatial conv. and spectral ______
        File fikernel2d; // input filename
    }

    public class input_filtazi {
        String method;                 // method selector
        long fftLength;                // length per buffer
        long overlap;                  // 0.5overlap each buffer
        double hammingAlpha;           // alpha for hamming, 1 is no

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public long getFftLength() {
            return fftLength;
        }

        public void setFftLength(long fftLength) {
            this.fftLength = fftLength;
        }

        public long getOverlap() {
            return overlap;
        }

        public void setOverlap(long overlap) {
            this.overlap = overlap;
        }

        public double getHammingAlpha() {
            return hammingAlpha;
        }

        public void setHammingAlpha(double hammingAlpha) {
            this.hammingAlpha = hammingAlpha;
        }


        //        File foname;        // output filename passed to routine
//        File fomaster;      // output filename
//        File foslave;       // output filename
//        long oformatflag;            // output format [cr4] ci16
    }

    public class input_filtrange {
        int method;                 // method selector
        long oversample;            // factor
        boolean doweightcorrel;     // weighting of correlation values
        long nlmean;                // number of lines to take mean of
        long fftlength;             // length for adaptive
        long overlap;               // half overlap between blocks of fftLength
        double hammingalpha;        // alpha for hamming
        double SNRthreshold;        // spectral peak estimation
        double terrainslope;        // [rad] porbits method only
        File fomaster;              // output filename
        File foslave;               // output filename
        int oformatflag;            // output format [cr4] ci16
    }

    public class input_comprefpha {                // arguments for flatearth correction.
        /*
          //char                idflatearth[EIGHTY];
          char          ifpositions[4*ONE27];   // input file name for positions
          int16         method;                 // method selector
          int32         degree;                 // degree of polynomial
          int32         Npoints;                // number of observations
        */
    }

    public class input_resample {                   // arguments for resampling slave
        /*
          int16         method;                 // method selector (interpolator) (%100 == Npoints)
          char          fileout[4*ONE27];
          int16         oformatflag;            // output format [cr4] ci16
          window        dbow_geo;               // cut out of original master.geo
          window        dbow;                   // cut out of original master.radar
          bool          shiftazi;               // [true] shift spectrum to 0
        */
    }

    public class input_interfero {                  // arguments for computation interferogram
        /*
        int16         method;                 // method selector
        char          focint[4*ONE27];                // optional output filename complex interferogram.
        char          foint[4*ONE27];         //  ~ of interferogram (phase).
      //  char        foflatearth[EIGHTY];    //  ~ of correction (flatearth) model (phase)
                                              //  these are flags as well as arguments.
                                              //  one is man (else no output)
        uint          multilookL;             // multilookfactor in line dir.
        uint          multilookP;             // multilookfactor in pixel dir.
        */
    }

    public class input_coherence {                  // arguments for computation coherence
        /*
          int16         method;                 // method selector
          char          focoh[4*ONE27];         // opt output filename of real coherence image.
          char          foccoh[4*ONE27];                //  ~ of complex coherence image.
                                                //  these are flags as well as arguments.
          uint          multilookL;             // multilookfactor in line dir.
          uint          multilookP;             // multilookfactor in pixel dir.
          uint          cohsizeL;               // size of estimation window coherence
          uint          cohsizeP;               // size of estimation window coherence
        */
    }

    public class input_subtrrefpha {               // arguments for subtract 'flat earth'
        /*
          int16         method;                 // method selector
          uint          multilookL;             // multilookfactor in line dir.
          uint          multilookP;             // multilookfactor in pixel dir.
          char          focint[4*ONE27];                // output filename complex interferogram
          char          forefpha[4*ONE27];      // output filename complex refpha
          char          foh2ph[4*ONE27];                // output filename h2ph, added by FvL
          bool          dumponlyrefpha;         // do nothing except dump refpha
        */
    }

    public class input_comprefdem {                // arguments for reference phase from DEM

        /*
        //  int16       method;                 // method selector
          char          firefdem[4*ONE27];      // input filename reference dem
          int16         iformatflag;            // input format [signed short]
          uint          demRows;                // number of
          uint          demCols;                // number of
          real8         demDeltaLat;            // radians
          real8         demDeltaLon;            // radians
          real8         demLatLeftUpper;        // radians
          real8         demLonLeftUpper;        // radians
          real8         demNodata;              // identifier/flag
        //  real8               extradense;             // extra interpolation factor (4)
          char          forefdem[4*ONE27];      // output filename reference phase
          char          foh2ph[4*ONE27];                // output perp. baseline, added by FvL
          char          forefdemhei[4*ONE27];   // output filename DEM in radarcoord.
          bool          includeRefPha;          // flag to include_flatearth correction
          char          fodem[4*ONE27];         // flag+name output of cropped dem
          char          fodemi[4*ONE27];                // flag+name output of interpolated dem
        */
    }

    public class input_subtrrefdem {               // arguments for subtract reference DEM
        /*
          int16         method;                 // method selector
          int32         offsetL;                // offset applied before subtraction
          int32         offsetP;                // offset applied before subtraction
          char          focint[4*ONE27];                // output filename complex interferogram
        */
    }

    public class input_pr_orbits {
        /*
            char          m_orbdir[4*ONE27];
            char          s_orbdir[4*ONE27];
            int32         timeinterval;               // time in sec.
            int32         timebefore;                   // sec before first line.
            real8         dumpmasterorbit;          // dtime in sec.
            real8         dumpslaveorbit;           // dtime in sec.
        */
    }

    public class input_dinsar {
        /*
       	    //int16       method;                 // method selector
    	    char fodinsar[4 * ONE27]; // output filename complex interferogram
	        char foscaleduint[4 * ONE27]; // output filename scaled uint
	        char topomasterresfile[4 * ONE27];// input filename
	        char toposlaveresfile[4 * ONE27];// input filename
	        char topointresfile[4 * ONE27]; // input filename
        */
    }

    public class input_slant2h {
        String method; // method selector
        //       	char fohei[4 * ONE27]; // output filename height
//       	char folam[4 * ONE27]; // output filename lambda
//       	char fophi[4 * ONE27]; // output filename phi
        public int Npoints; //
        public int degree1d; // only {1,2} possible now due to solve33
        public int degree2d; //
        public int Nheights; //
    }
}
