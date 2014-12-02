package org.jlinda.core.unwrapping.snaphu;

import org.jlinda.core.*;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;

public class SnaphuConfigFile {

    DecimalFormat format3 = new DecimalFormat("#.###");
    DecimalFormat format7 = new DecimalFormat("#.#######");

    static final float N_CORR_LOOKS = 23.8f;
//    static final String TAB_STRING = "\t\t";

    private SLCImage masterSLC;
    private SLCImage slaveSLC;
    private Orbit masterOrbit;
    private Orbit slaveOrbit;
    private Window dataWindow;

    private SnaphuParameters parameters;

    private StringBuffer configFileBuffer = new StringBuffer();
    private Formatter formattedConfig = new Formatter(configFileBuffer, Locale.US);

    public SnaphuConfigFile() {
    }

    public SnaphuConfigFile(SLCImage masterSLC, SLCImage slaveSLC,
                            Orbit masterOrbit, Orbit slaveOrbit,
                            Window dataWindow,
                            SnaphuParameters parameters) {
        this.masterSLC = masterSLC;
        this.slaveSLC = slaveSLC;
        this.masterOrbit = masterOrbit;
        this.slaveOrbit = slaveOrbit;
        this.dataWindow = dataWindow;
        this.parameters = parameters;
    }

    public void setMasterSLC(SLCImage masterSLC) {
        this.masterSLC = masterSLC;
    }

    public void setSlaveSLC(SLCImage slaveSLC) {
        this.slaveSLC = slaveSLC;
    }

    public void setMasterOrbit(Orbit masterOrbit) {
        this.masterOrbit = masterOrbit;
    }

    public void setSlaveOrbit(Orbit slaveOrbit) {
        this.slaveOrbit = slaveOrbit;
    }

    public void setDataWindow(Window dataWindow) {
        this.dataWindow = dataWindow;
    }


    public StringBuffer getConfigFileBuffer() {
        return configFileBuffer;
    }

    public void buildConfFile() throws Exception {

        // Mid point
        final double lineMid = 0.5d * dataWindow.lines() + 0.5;
        final double pixelMid = 0.5d * dataWindow.pixels() + 0.5;

        Point pointSAR = new Point(pixelMid, lineMid, 0);

        final double earthRadius = masterOrbit.computeEarthRadius(pointSAR, masterSLC);
        final double orbitRadius = masterOrbit.computeOrbitRadius(pointSAR, masterSLC);
        final double rangeNear = masterSLC.pix2range(dataWindow.pixlo);
        final double rangeDelta = masterSLC.computeDeltaRange(pointSAR);
        final double rangeResolution = masterSLC.computeRangeResolution(pointSAR);
        final double azimuthDelta = masterOrbit.computeAzimuthDelta(pointSAR, masterSLC);
        final double azimuthResolution = masterOrbit.computeAzimuthResolution(pointSAR, masterSLC);

        //// baseline parametrization
        final Baseline baseline = new Baseline();
        baseline.model(masterSLC, slaveSLC, masterOrbit, slaveOrbit);
        final double baselineTotal = baseline.getB(pointSAR);
        final double baselineAlpha = baseline.getAlpha(pointSAR);

        String DIMENSIONS = Long.toString(dataWindow.pixels() - 1); // account for zeros
        String IN_FILE_NAME = parameters.phaseFileName;

        formattedConfig.format("# CONFIG FOR SNAPHU\n");
        formattedConfig.format("# ---------------------------------------------------------------- \n");
        formattedConfig.format("# Created by NEST software on: " + printCurrentTimeDate() + "\n");
        formattedConfig.format("#\n");
        formattedConfig.format("# Command to call snaphu:\n");
        formattedConfig.format("# \n");
        formattedConfig.format("#       snaphu -f snaphu.conf " + IN_FILE_NAME + " " + DIMENSIONS + "\n");
        formattedConfig.format("\n");

        formattedConfig.format("#########################\n");
        formattedConfig.format("# Unwrapping parameters #\n");
        formattedConfig.format("#########################\n");
        formattedConfig.format("\n");
        formattedConfig.format("STATCOSTMODE \t %s %n", parameters.unwrapMode.toUpperCase());
        formattedConfig.format("INITMETHOD  \t %s %n", parameters.snaphuInit.toUpperCase());
        formattedConfig.format("VERBOSE \t %s %n", parameters.verbosityFlag.toUpperCase());
        formattedConfig.format("\n");

        formattedConfig.format("###############\n");
        formattedConfig.format("# Input files #\n");
        formattedConfig.format("###############\n");
        formattedConfig.format("\n");
        formattedConfig.format("CORRFILE \t\t" + parameters.coherenceFileName + "\n");
        formattedConfig.format("\n");

        formattedConfig.format("################\n");
        formattedConfig.format("# Output files #\n");
        formattedConfig.format("################\n");
        formattedConfig.format("\n");
        formattedConfig.format("OUTFILE \t\t" + parameters.outFileName + "\n");
        formattedConfig.format("LOGFILE \t\t" + parameters.logFileName + "\n");
        formattedConfig.format("\n");

        formattedConfig.format("################\n");
        formattedConfig.format("# File formats #\n");
        formattedConfig.format("################\n");
        formattedConfig.format("\n");
//        formattedConfig.format("INFILEFORMAT \t" + "COMPLEX_DATA\n"); // Eventually converged to export/work with FLOAT
        formattedConfig.format("INFILEFORMAT \t" + "FLOAT_DATA\n");
        formattedConfig.format("CORRFILEFORMAT \t" + "FLOAT_DATA\n");
        formattedConfig.format("OUTFILEFORMAT \t" + "FLOAT_DATA\n");
        formattedConfig.format("\n");

        formattedConfig.format("###############################\n");
        formattedConfig.format("# SAR and geometry parameters #\n");
        formattedConfig.format("###############################\n");
        formattedConfig.format("\n");
        formattedConfig.format("TRANSMITMODE \t" + "REPEATPASS\n");
        formattedConfig.format("\n");
        formattedConfig.format("ORBITRADIUS \t" + doubleToString(orbitRadius, format3) + "\n");
        formattedConfig.format("EARTHRADIUS \t" + doubleToString(earthRadius, format3) + "\n");
        formattedConfig.format("\n");
        formattedConfig.format("LAMBDA \t\t\t" + doubleToString(masterSLC.getRadarWavelength(), format7) + "\n");
        formattedConfig.format("\n");
        formattedConfig.format("BASELINE \t\t" + doubleToString(baselineTotal, format3) + "\n");
        formattedConfig.format("BASELINEANGLE_RAD \t" + doubleToString(baselineAlpha, format3) + "\n");
        formattedConfig.format("\n");
        formattedConfig.format("NEARRANGE \t\t" + doubleToString(rangeNear, format7) + "\n");
        formattedConfig.format("\n");
        formattedConfig.format("# Slant range and azimuth pixel spacings\n");
        formattedConfig.format("DR \t\t\t\t" + doubleToString(rangeDelta, format7) + "\n");
        formattedConfig.format("DA \t\t\t\t" + doubleToString(azimuthDelta, format7) + "\n");
        formattedConfig.format("\n");
        formattedConfig.format("# Single-look slant range and azimuth resolutions.\n");
        formattedConfig.format("RANGERES \t\t" + doubleToString(rangeResolution, format7) + "\n");
        formattedConfig.format("AZRES \t\t\t" + doubleToString(azimuthResolution, format7) + "\n");
        formattedConfig.format("\n");
        formattedConfig.format("# The number of independent looks: approximately equal to the\n"
                + "#     real number of looks divided by the product of range and\n"
                + "#     azimuth resolutions, and multiplied by the product of the\n"
                + "#     single-look range and azimuth spacings. It is about 0.53\n"
                + "#     times the number of real looks for ERS data processed\n"
                + "#     without windowing.\n");
        formattedConfig.format("NCORRLOOKS \t\t" + Float.toString(N_CORR_LOOKS) + "\n");

        tileControlFlags();

        formattedConfig.format("# End of snaphu configuration file");

    }

    // these are flags for controlling parallel processing with Snaphu -> be careful with COST Threshold for TILES
    private void tileControlFlags() {
        configFileBuffer.append("\n"
                + "################\n"
                + "# Tile control #\n"
                + "################\n"
                + "\n"
                + "NTILEROW           1\n"
                + "NTILECOL           1\n"
                + "\n"
                + "ROWOVRLP           0\n"
                + "COLOVRLP           0 \n"
                + "\n"
                + "NPROC              1\n"
                + "\n"
                + "TILECOSTTHRESH   500\n"
                + "\n");
    }

    private String printCurrentTimeDate() {
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
        Date date = new Date();
        return dateFormat.format(date);
    }

    private String doubleToString(final double value, final DecimalFormat format) {
        return Double.valueOf(format.format(value)).toString();
    }

}