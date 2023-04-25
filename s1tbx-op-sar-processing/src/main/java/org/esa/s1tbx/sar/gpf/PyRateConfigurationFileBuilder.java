package org.esa.s1tbx.sar.gpf;

import java.time.LocalDateTime;

/*
    Helper class to create configuration and auxillary files for PyRATE.
    See https://github.com/GeoscienceAustralia/PyRate/blob/master/input_parameters.conf for more details.

    Created April 2023 by Alex McVittie
 */
public class PyRateConfigurationFileBuilder {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /******************************
     PYRATE Input parameters

     The following variables are all for writing out to the PyRATE input_parameters.conf file.
     They are all set to defaults. Once you declare your own PyRateConfigurationFileBuilder, you can easily change them.

     e.g. running on a large computer with >=16 cores, you can modify it like so:
     PyRateConfigurationFileBuilder configBuilder = new PyRateConfigurationFileBuilder();
     configBuilder.parallel = true;
     configBuilder.numProcesses = 16;

     ******************************/
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /*
        Optional correction on off switches. Config file stores as true/ON = 1, false/OFF = 0
     */
    public boolean coherenceMasking = true; // For coherence masking during prepifg step.
    public boolean orbitalErrorCorrect = true; // For orbital error correction
    public boolean demCorrect = true; // DEM error correction for residual topography correction
    public boolean phaseClosureCorrect = true; // Phase closure correction
    public boolean apsCorrection = true; // APS correction using spatio-temporal filter
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /*
        PyRATE Integer parameters
     */

    // 0 = LOS, no conversion, 1 = pseudo-vertical, 2 = pseudo-horizontal.
    public int losProjection = 0;

    // 1 = retain sign convention of input interferograms. -1 = reverse sign convention of input interferograms
    public int signalPolarity = -1;

    // Number of sigma to report velocity error.
    public int velocitytError = 2;

    // Optional save of numpy files for output products.
    public int savenpy = 0;

    // Optional save of incremental time series products
    public int savetsincr = 0;
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /*
        For running with a parallel, multi-threaded CPU workflow.
     */
    public boolean parallel = true;
    public int numProcesses = 4; // SNAPHU unwrapping uses 4 cores for default. Keep in line with that.

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /*
        Definitions for input/output file locations, and files containing lists of input files.
     */

    public String interferogramFileList = "ifgList.txt";

    public String demFile = "DEM.tif";

    public String coherenceFileList = "coherenceList.txt";

    public String headerFileList = "headers.txt";

    public String demHeaderFile = "DEM.par";

    public String outputDirectory = "pyrateOutput/";

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /*
        PREPIFG input parameters.
     */

    public int processor = 1;

    public double coherenceThreshold = 0.3;

    public int ifglksx = 1;
    public int ifglksy = 1;

    // Cropping. 1 = min extent, 2 = max extent 3 = crop 4 = no crop, use full input scene.
    public int ifgcropopt = 4;

    // (x,y) min and max extents supplied in geographic, WGS84 lat-lon coordinate format. North-west and south-east corners.
    public double ifgxfirst = 150.92;
    public double ifgyfirst = -34.18;
    public double ifgxlast = 150.94;
    public double ifgylast = -34.22;


    public double noDataAveragingThreshold = 0.5;

    public double noDataValue = 0.0;

    // Convert missing no-data values to NaN if set to true
    public boolean nan_conversion = true;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /*
        CORRECT parameters
     */

    public double refx = -99999;
    public double refy = -99999;
    public int refnx = 5;
    public int refny = 5;
    public int refchipsize = 7;
    public double refminfrac = 0.01;

    public int refest = 2;

    public int orbfitmethod = 1;
    public int orbfitdegrees = 1;
    public int orbfitlksx = 5;
    public int orbfitlksy = 5;

    public double closureThr = 0.5;
    public double ifgDropThr = 0.5;
    public int minLoopsPerIfg = 2;
    public int maxLoopLength = 4;
    public int maxLoopRedundancy = 2;


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /*
        APS filter parameters
     */
    public int tlpfcutoff = 30;
    public int tlpfpthr = 1;
    public int slpfcutoff = 1;

    // DEM error correction params
    public int de_pthr = 20;


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /*
        TIMESERIES parameters
     */

    public int tsmethod = 2;
    public int smorder = 2;
    public double smfactor = -0.25;
    public int tsPthr = 10;

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /*
        STACK parameters
     */
    public int pthr = 5;
    public int nsig = 3;
    public int maxsig = 1000;

    /*
        End of PyRATE configuration variable declarations.
     */
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public PyRateConfigurationFileBuilder(){

    }


    public String createMainConfigFileContents(){
        String contents = "# PyRate configuration file.\n" +
                "#\n" +
                "# Generated on " + LocalDateTime.now() + " by ESA SNAP\n" +
                "#%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n" +
                "# Optional Correction ON/OFF switches - ON = 1; OFF = 0\n" +
                "# Coherence masking (PREPIFG)\n";

        contents += PyRateCommons.createTabbedVariableLine("cohmask", coherenceMasking ? 1 : 0);
        contents += "\n# Orbital error correction (CORRECT)\n";
        contents += PyRateCommons.createTabbedVariableLine("orbfit",orbitalErrorCorrect ? 1 : 0);
        contents += "\n# DEM error (residual topography) correction (CORRECT)\n";
        contents += PyRateCommons.createTabbedVariableLine("demerror", demCorrect ? 1 : 0);
        contents += "\n# Phase Closure correction (CORRECT)\n";
        contents += PyRateCommons.createTabbedVariableLine("phase_closure", phaseClosureCorrect ? 1 : 0);
        contents += "\n# APS correction using spatio-temporal filter (CORRECT)\n";
        contents += PyRateCommons.createTabbedVariableLine("apsest", (apsCorrection ? 1 : 0));

        contents += "\n#%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n" +
                "# Integer parameters\n" +
                "\n" +
                "# LOS Projection of output products (MERGE)\n" +
                "# Converts slanted (native) LOS signals to either \"pseudo-vertical\" or \"pseudo-horizontal\", \n" +
                "# by dividing by the cosine or sine of the incidence angle for each pixel, respectively.\n" +
                "# los_projection: 0 = LOS (no conversion); 1 = pseudo-vertical; 2 = pseudo-horizontal.\n";
        contents += PyRateCommons.createTabbedVariableLine("los_projection", losProjection);

        contents += "\n# Sign convention for phase data (MERGE)\n" +
                "# signal_polarity:  1 = retain sign convention of input interferograms\n" +
                "# signal_polarity: -1 = reverse sign convention of input interferograms (default)\n";
        contents += PyRateCommons.createTabbedVariableLine("signal_polarity", signalPolarity);

        contents += "\n# Number of sigma to report velocity error. Positive integer. Default: 2 (TIMESERIES/STACK)\n";
        contents += PyRateCommons.createTabbedVariableLine("velerror_nsig", velocitytError);

        contents += "\n# Optional save of numpy array files for output products (MERGE)\n";
        contents += PyRateCommons.createTabbedVariableLine("savenpy", savenpy);

        contents += "# Optional save of incremental time series products (TIMESERIES/MERGE)\n";
        contents += PyRateCommons.createTabbedVariableLine("savetsincr", savetsincr);

        contents += "\n#%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n" +
                "# Multi-threading parameters (CORRECT/TIMESERIES/STACK)\n" +
                "# parallel: 1 = parallel, 0 = serial\n";
        contents += PyRateCommons.createTabbedVariableLine("parallel", parallel ? 1 : 0);
        contents += "\n# number of processes\n";
        contents += PyRateCommons.createTabbedVariableLine("processes", numProcesses);

        contents += "\n#%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n" +
                "# Input/Output file locations\n" +
                "#\n" +
                "# File containing the list of interferograms to use.\n";
        contents += PyRateCommons.createTabbedVariableLine("ifgfilelist", interferogramFileList);
        contents += "\n# The DEM file used in the InSAR processing\n";
        contents += PyRateCommons.createTabbedVariableLine("demfile", demFile);
        contents += "\n# File listing the pool of available coherence files.\n";
        contents += PyRateCommons.createTabbedVariableLine("cohfilelist", coherenceFileList);
        contents += "\n# File listing the pool of available header files (GAMMA: *mli.par, ROI_PAC: *.rsc)\n";
        contents += PyRateCommons.createTabbedVariableLine("hdrfilelist", headerFileList);

        contents += "\n# The DEM header file from GAMMA (*.par) or ROI_PAC (*.rsc).\n";
        contents += PyRateCommons.createTabbedVariableLine("demHeaderFile", demHeaderFile);

        contents += "\n# Directory to write the outputs to\n";
        contents += PyRateCommons.createTabbedVariableLine("outdir", outputDirectory);

        contents += "\n#%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n" +
                "# PREPIFG parameters\n" +
                "#------------------------------------\n" +
                "# Input data format: ROI_PAC = 0, GAMMA = 1\n";
        contents += PyRateCommons.createTabbedVariableLine("processor", String.valueOf(processor));
        contents += "# Coherence threshold value for masking, between 0 and 1\n";
        contents += PyRateCommons.createTabbedVariableLine("cohthresh", coherenceThreshold);
        contents += "# Multi-look/subsampling factor in east (x) and north (y) dimension\n";
        contents += PyRateCommons.createTabbedVariableLine("ifglksx", ifglksx);
        contents += PyRateCommons.createTabbedVariableLine("ifglksy", ifglksy);

        contents += "\n# Cropping options\n" +
                "# ifgcropopt: 1 = minimum extent 2 = maximum extent 3 = crop 4 = no cropping\n" +
                "# ifgxfirst,ifgyfirst: longitude (x) and latitude (y) of north-west corner\n" +
                "# ifgxlast,ifgylast: longitude (x) and latitude (y) of south-east corner\n";
        contents += PyRateCommons.createTabbedVariableLine("ifgcropopt", ifgcropopt);
        contents += PyRateCommons.createTabbedVariableLine("ifgxfirst", ifgxfirst);
        contents += PyRateCommons.createTabbedVariableLine("ifgyfirst", ifgyfirst);
        contents += PyRateCommons.createTabbedVariableLine("ifgxlast", ifgxlast);
        contents += PyRateCommons.createTabbedVariableLine("ifgylast", ifgylast);

        contents += "\n# No-data averaging threshold (0 = 0%; 1 = 100%)\n";
        contents += PyRateCommons.createTabbedVariableLine("noDataAveragingThreshold", noDataAveragingThreshold);

        contents += "\n# The No-data value used in the interferogram files\n";
        contents += PyRateCommons.createTabbedVariableLine("noDataValue", noDataValue);

        contents += "# Nan conversion flag. Set to 1 if missing No-data values are to be converted to NaN\n";
        contents += PyRateCommons.createTabbedVariableLine("nan_conversion", nan_conversion ? 1 : 0);
        contents += "\n#%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n" +
                "# CORRECT parameters\n" +
                "#------------------------------------\n" +
                "# Reference pixel search parameters\n" +
                "\n" +
                "# refx/y: Lon/Lat coordinate of reference pixel. If left blank then search for best pixel will be performed\n" +
                "# refnx/y: number of search grid points in x/y image dimensions\n" +
                "# refchipsize: size of the data window at each search grid point\n" +
                "# refminfrac: minimum fraction of valid (non-NaN) pixels in the data window\n";
        if(refx == -99999){
            // reference pixels are not initialized from default value. Have PyRATE calculate best pixel.
            contents += "refx:\t\nrefy:\t\n";
        }else{
            contents += PyRateCommons.createTabbedVariableLine("refx", refx);
            contents += PyRateCommons.createTabbedVariableLine("refy", refy);
        }
        contents += PyRateCommons.createTabbedVariableLine("refnx", refnx);
        contents += PyRateCommons.createTabbedVariableLine("refny", refny);
        contents += PyRateCommons.createTabbedVariableLine("refchipsize", refchipsize);
        contents += PyRateCommons.createTabbedVariableLine("refminfrac", refminfrac);

        contents += "\n#------------------------------------\n" +
                "# Reference phase correction method\n" +
                "\n" +
                "# refest: 1 = median of the whole interferogram\n" +
                "# refest: 2 = median within the window surrounding the chosen reference pixel\n";
        contents += PyRateCommons.createTabbedVariableLine("refest", refest);

        contents += "\n#------------------------------------\n" +
                "# Orbital error correction parameters\n" +
                "\n" +
                "# orbfitmethod = 1: interferograms corrected independently; 2: network method\n" +
                "# orbfitdegrees: Degree of polynomial surface to fit (1 = planar; 2 = quadratic; 3 = part-cubic)\n" +
                "# orbfitlksx/y: additional multi-look factor for network orbital correction\n";
        contents += PyRateCommons.createTabbedVariableLine("orbfitmethod", orbfitmethod);
        contents += PyRateCommons.createTabbedVariableLine("orbfitdegrees", orbfitdegrees);
        contents += PyRateCommons.createTabbedVariableLine("orbfitlksx", orbfitlksx);
        contents += PyRateCommons.createTabbedVariableLine("orbfitlksy", orbfitlksy);

        contents += "\n#------------------------------------\n" +
                "# Phase closure correction parameters\n" +
                "\n" +
                "# closure_thr:         Closure threshold for each pixel in multiples of pi, e.g. 0.5 = pi/2, 1 = pi.\n" +
                "# ifg_drop_thr:        Ifgs with more than this fraction of pixels above the closure threshold in all\n" +
                "#                      loops it participates in, will be dropped entirely.\n" +
                "# min_loops_per_ifg:   Ifgs are dropped entirely if they do not participate in at least this many closure loops.\n" +
                "# max_loop_length:     Closure loops with up to this many edges will be used.\n" +
                "# max_loop_redundancy: A closure loop will be discarded if all constituent ifgs in that loop have\n" +
                "#                      already contributed to a number of loops equal to this parameter.\n";
        contents += PyRateCommons.createTabbedVariableLine("closure_thr", closureThr);
        contents += PyRateCommons.createTabbedVariableLine("ifg_drop_thr", ifgDropThr);
        contents += PyRateCommons.createTabbedVariableLine("min_loops_per_ifg", minLoopsPerIfg);
        contents += PyRateCommons.createTabbedVariableLine("max_loop_length", maxLoopLength);
        contents += PyRateCommons.createTabbedVariableLine("max_loop_redundancy", maxLoopRedundancy);

        contents += "\n#------------------------------------\n" +
                "# APS filter parameters\n" +
                "\n" +
                "# tlpfcutoff: cutoff t0 for temporal high-pass Gaussian filter in days (int);\n" +
                "# tlpfpthr: valid pixel threshold;\n" +
                "# slpfcutoff: spatial low-pass Gaussian filter cutoff in km (greater than zero).\n" +
                "# slpfcutoff=0 triggers cutoff estimation from exponential covariance function\n";

        contents += PyRateCommons.createTabbedVariableLine("tlpfcutoff", tlpfcutoff);
        contents += PyRateCommons.createTabbedVariableLine("tlpfpthr", tlpfpthr);
        contents += PyRateCommons.createTabbedVariableLine("slpfcutoff", slpfcutoff);

        contents += "\n#------------------------------------\n" +
                "# DEM error (residual topography) correction parameters\n" +
                "\n" +
                "# de_pthr: valid observations threshold;\n";
        contents += PyRateCommons.createTabbedVariableLine("de_pthr", de_pthr);

        contents += "\n#%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n" +
                "# TIMESERIES parameters\n" +
                "#------------------------------------\n" +
                "\n" +
                "# tsmethod: Method for time series inversion (1 = Laplacian Smoothing; 2 = SVD)\n" +
                "# smorder: order of Laplacian smoothing operator (1 = first-order difference; 2 = second-order difference)\n" +
                "# smfactor: smoothing factor for Laplacian smoothing (value provided is converted as 10**smfactor)\n" +
                "# ts_pthr: valid observations threshold for time series inversion\n";
        contents += PyRateCommons.createTabbedVariableLine("tsmethod", tsmethod);
        contents += PyRateCommons.createTabbedVariableLine("smorder", smorder);
        contents += PyRateCommons.createTabbedVariableLine("smfactor", smfactor);
        contents += PyRateCommons.createTabbedVariableLine("ts_pthr", tsPthr);

        contents += "\n#%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n" +
                "# STACK parameters\n" +
                "#------------------------------------\n" +
                "\n" +
                "# pthr: threshold for minimum number of ifg observations for each pixel\n" +
                "# nsig: threshold for iterative removal of observations\n" +
                "# maxsig: maximum sigma (std dev; millimetres) used as an output masking threshold applied in Merge step. 0 = OFF.\n";
        contents += PyRateCommons.createTabbedVariableLine("pthr", pthr);
        contents += PyRateCommons.createTabbedVariableLine("nsig", nsig);
        contents += PyRateCommons.createTabbedVariableLine("maxsig", maxsig);

        return contents;
    }
}
