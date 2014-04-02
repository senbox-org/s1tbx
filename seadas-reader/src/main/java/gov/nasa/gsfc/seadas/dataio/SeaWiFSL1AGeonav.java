
/**
 *
 * SeaWiFSL1AGeonav - navigational functions for SeaWiFS (needed for SeaDAS to
 *    work with SeaWiFS Level1 files).  This code was converted from 
 *    Fortran code in the OCSSW library (specifically the interpnav_seawifs
 *    program), mostly geonav.f.  For explanation, see the journal article:
 *       Exact closed-form geolocation algorithm for Earth survey sensors
 *       by Patt, F.S. and W.W. Gregg
 *       International Journal of Remote Sensing
 *       1994, Volume 15, No. 18, pp. 3719-3734
 *    Within OBPG, hardcopies might be available locally.  It was found online at:
 *       http://www.informaworld.com/smpp/content~db=all~content=a778242783~frm=titlelink
 *
 * History:
 * When:                 Who:            What:
 * June - August, 2011   Matt Elliott    Original conversion from Fortran
 * October, 2011         Matt Elliott    Replaced constructors with one which takes
 *                                       only a NetCDF file as an argument and
 *                                       determines other needed info from it.
 *                                       Cleaned up code.
 *
 */

package gov.nasa.gsfc.seadas.dataio;

import ucar.ma2.ArrayFloat;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;

public class SeaWiFSL1AGeonav {
    // Constants - the Fortran version defines pi, but Java has Math.PI.
    public static final float DEGREES_PER_RADIAN = (float) (180.0 / Math.PI);
    public static final double EARTH_RADIUS = 6378.137;                // often re
    public static final double EARTH_FLATTENING_FACTOR = 1.0/298.257;  // often f
    public static final double EARTH_MEAN_RADIUS = 6371.0;             // often rem
    public static final double EARTH_ROTATION_RATE = 7.29211585494E-5; //often omegae
    public static final double OMF2 = Math.pow((1.0 - EARTH_FLATTENING_FACTOR),
                                               2.0);          // Convenience constant

    private static final int   GAC_START_SCAN_PIXEL = 147;
    private static final int   LAC_START_SCAN_PIXEL = 1;
    private static final int   GAC_PIXELS_PER_SCAN = 248;
    private static final int   LAC_PIXELS_PER_SCAN = 1285;
    private static final int   GAC_PIXEL_INCREMENT = 4;
    private static final int   LAC_PIXEL_INCREMENT = 1;

    private static final int   MAX_SEAWIFS_PIXELS = (LAC_PIXELS_PER_SCAN > GAC_PIXELS_PER_SCAN) ?
                                                    LAC_PIXELS_PER_SCAN : GAC_PIXELS_PER_SCAN;

    private double SINC = 0.0015897; //0.0015911;

    /* The sensorOffsetMatrix corresponds to navctl%msensoff and tiltCosVector
     * corresponds to navctl%tiltcos in the Fortran version.  Both variables are
     * part of the navctl structure, which is read from the navctl.dat file in
     * the Fortran.
     */
    private float[][] sensorOffsetMatrix = new float[3][3];
    private float[] tiltCosVector = new float[3];

    enum DataType { GAC, LAC }

    // The following are input parameters in the Fortran geonav.f function:
    private float[]   scanPathCoef = new float[6];           // coef in Fortran version
    private int       pixIncr = LAC_PIXEL_INCREMENT;         // ninc in Fortran version
    private int       pixPerScanLine = LAC_PIXELS_PER_SCAN;  // npix in Fortran version
    private int       scanStartPix = LAC_START_SCAN_PIXEL;   // nsta in Fortran version
    private float[]   orbPos = new float[3];                 // pos in Fortran version
    private float[][] sensorOrientation = new float[3][3];   // rm in Fortran version
    private float[]   sunUnitVec = new float[3];             // sun in Fortran version

    // Output parameters in Fortran version, used here to hold single arrays
    // which get loaded into 2D matrices:
    private float[] sena = new float[MAX_SEAWIFS_PIXELS];
    private float[] senz = new float[MAX_SEAWIFS_PIXELS];
    private float[] sola = new float[MAX_SEAWIFS_PIXELS];
    private float[] solz = new float[MAX_SEAWIFS_PIXELS];
    private float[] xlat = new float[MAX_SEAWIFS_PIXELS];
    private float[] xlon = new float[MAX_SEAWIFS_PIXELS];

    // Intermediate values for calculations:
    private float[]   attAngle = new float[3];
    private double    cosa[] = new double[1285];
    private double    cosl;
    private DataType  dataType;
    private double    elev;
    private int       numScanLines;
    private double    sina[] = new double[MAX_SEAWIFS_PIXELS];
    private double    sinl;

    // Final values for "output":
    private float[][] latitudes;
    private float[][] longitudes;
    private float[][] sensorAzimuths;
    private float[][] sensorZeniths;
    private float[][] solarAzimuths;
    private float[][] solarZeniths;

    private NetcdfFile ncFile;

    public SeaWiFSL1AGeonav(NetcdfFile netcdfFile) {
        ncFile = netcdfFile;

        dataType = determineSeawifsDataType(ncFile);

        pixPerScanLine = determinePixelsPerScanLine();
        scanStartPix  = determineStartPixel();
        pixIncr = determinePixelIncrement();

        numScanLines = determineNumberScanLines(ncFile);

        latitudes = new float[numScanLines][pixPerScanLine];
        longitudes = new float[numScanLines][pixPerScanLine];
        sensorAzimuths = new float[numScanLines][pixPerScanLine];
        sensorZeniths = new float[numScanLines][pixPerScanLine];
        solarAzimuths = new float[numScanLines][pixPerScanLine];
        solarZeniths = new float[numScanLines][pixPerScanLine];

        /* The sensorOffsetMatrix values were copied from the navctl.dat file.
         * According to email from F. Patt, the values never changed during the
         * SeaWiFS mission, thus they are hard-coded here.
         */
        sensorOffsetMatrix[0][0] = 1.0f;
        sensorOffsetMatrix[0][1] = 0.0f;
        sensorOffsetMatrix[0][2] = 0.0f;
        sensorOffsetMatrix[1][0] = 0.0f;
        sensorOffsetMatrix[1][1] = 0.99999905f;
        sensorOffsetMatrix[1][2] = -0.00139626f;
        sensorOffsetMatrix[2][0] = 0.0f;
        sensorOffsetMatrix[2][1] = 0.00139626f;
        sensorOffsetMatrix[2][2] = 0.99999905f;

        // The tiltCosVector values were copied from the navctl.dat file.
        tiltCosVector[0] = 0.0f;
        tiltCosVector[1] = 0.0f;
        tiltCosVector[2] = 1.0f;

        //Group rootGroup = ncFile.getRootGroup();
        Group navGroup = ncFile.findGroup("Navigation");
        Group scanLineAttrGroup = ncFile.findGroup("Scan-Line_Attributes");

        ArrayFloat orbitData = readNetcdfDataArray("orb_vec", navGroup);
        ArrayFloat sensorData = readNetcdfDataArray("sen_mat", navGroup);
        ArrayFloat sunData = readNetcdfDataArray("sun_ref", navGroup);
        ArrayFloat attAngleData = readNetcdfDataArray("att_ang", navGroup);
        ArrayFloat scanTrackEllipseCoefData = readNetcdfDataArray("scan_ell", navGroup);
        ArrayFloat tiltData = readNetcdfDataArray("tilt", scanLineAttrGroup);

        //  Compute elevation (out-of-plane) angle
        elev = SINC * 1.2;
        sinl = Math.sin(elev);
        cosl = Math.cos(elev);
        for (int i = 0; i < MAX_SEAWIFS_PIXELS; i ++) {
            sina[i] = Math.sin((i - 642) * SINC) * cosl;
            cosa[i] = Math.cos((i - 642) * SINC) * cosl;
        }

        for (int line = 0; line < numScanLines; line ++) {
            attAngle = populateVector(attAngleData, 3, line);

            orbPos = populateVector(orbitData, 3, line);
            sensorOrientation[0][0] = sensorData.getFloat(line * 9);
            sensorOrientation[0][1] = sensorData.getFloat(line * 9 + 1);
            sensorOrientation[0][2] = sensorData.getFloat(line * 9 + 2);
            sensorOrientation[1][0] = sensorData.getFloat(line * 9 + 3);
            sensorOrientation[1][1] = sensorData.getFloat(line * 9 + 4);
            sensorOrientation[1][2] = sensorData.getFloat(line * 9 + 5);
            sensorOrientation[2][0] = sensorData.getFloat(line * 9 + 6);
            sensorOrientation[2][1] = sensorData.getFloat(line * 9 + 7);
            sensorOrientation[2][2] = sensorData.getFloat(line * 9 + 8);
            float tilt = tiltData.getFloat(line);
//            float[][] attXfm = computeTransformMatrix(tilt);
            scanPathCoef = populateVector(scanTrackEllipseCoefData, 6, line);
            sunUnitVec = populateVector(sunData, 3, line);

            doComputations();
            System.arraycopy(xlat, 0, latitudes[line], 0, pixPerScanLine);
            System.arraycopy(xlon, 0, longitudes[line], 0, pixPerScanLine);
            System.arraycopy(sena, 0, sensorAzimuths[line], 0, pixPerScanLine);
            System.arraycopy(senz, 0, sensorZeniths[line], 0, pixPerScanLine);
            System.arraycopy(sola, 0, solarAzimuths[line], 0, pixPerScanLine);
            System.arraycopy(solz, 0, solarZeniths[line], 0, pixPerScanLine);
        }
    }

    private float [] computeEastVector(float[] up, float upxy) {
        float[] eastVector = new float[3];
        eastVector[0] = -up[1] / upxy;
        eastVector[1] = up[0] / upxy;
        return eastVector;
    }

    private float[] computeInterEllCoefs(float[][] attTransform,
                                         float[] attAngle, float tilt,
                                         float[]p) {
        /**
         * Compute coefficients of the intersection ellipse in the scan plane;
         * partial adaptation of the ellxfm function in ellxfm.f
         */

        double rd = 1.0f / OMF2;
        float[] coef = new float[6];

        float[][] sm1 = computeEulerTransformMatrix(attAngle);
        float[][] sm2 = multiplyMatrices(sm1, attTransform);

        sm1 = multiplyMatrices(sensorOffsetMatrix, sm2);
        sm2 = computeEulerAxisMatrix(tiltCosVector, tilt);
        float[][] sm3 = multiplyMatrices(sm2, sm1);

        coef[0] = (float) (1.0f + (rd - 1.0f) * sm3[0][2] * sm3[0][2]);
        coef[1] = (float) ((rd - 1.0f) * sm3[0][2] * sm3[2][2] * 2.0f);
        coef[2] = (float) (1.0f + (rd - 1.0f) * sm3[2][2] * sm3[2][2]);
        coef[3] = (float) ((sm3[0][0] * p[0] + sm3[0][1] * p[1] + sm3[0][2] * p[2] * rd) * 2.0f);
        coef[4] = (float) ((sm3[2][0] * p[0] + sm3[2][1] * p[1] + sm3[2][2] * p[2] * rd) * 2.0f);
        coef[5] = (float) (p[0] * p[0] + p[1] * p[1] + p[2] * p[2] * rd - EARTH_RADIUS * EARTH_RADIUS);
        return coef;
    }

    private float[][] computeEulerAxisMatrix(float[] eulerAxisUnitVector, float phi) {
        /**
         * An adaptation of the eaxis function in eaxis.f
         */
        float cp = (float) Math.cos(phi / DEGREES_PER_RADIAN);
        float sp = (float) Math.sin(phi / DEGREES_PER_RADIAN);
        float omcp = 1.0f - cp;

        float[][] xm = new float[3][3];

        xm[0][0] = cp + eulerAxisUnitVector[0] * eulerAxisUnitVector[0] * omcp;
        xm[0][1] = eulerAxisUnitVector[0] * eulerAxisUnitVector[1] * omcp + eulerAxisUnitVector[2] * sp;
        xm[0][2] = eulerAxisUnitVector[0] * eulerAxisUnitVector[2] * omcp - eulerAxisUnitVector[1] * sp;
        xm[1][0] = eulerAxisUnitVector[0] * eulerAxisUnitVector[1] * omcp - eulerAxisUnitVector[2] * sp;
        xm[1][1] = cp + eulerAxisUnitVector[1] * eulerAxisUnitVector[1] * omcp;
        xm[1][2] = eulerAxisUnitVector[1] * eulerAxisUnitVector[2] * omcp + eulerAxisUnitVector[0] * sp;
        xm[2][0] = eulerAxisUnitVector[0] * eulerAxisUnitVector[2] * omcp + eulerAxisUnitVector[1] * sp;
        xm[2][1] = eulerAxisUnitVector[1] * eulerAxisUnitVector[2] * omcp - eulerAxisUnitVector[0] * sp;
        xm[2][2] = cp + eulerAxisUnitVector[2] * eulerAxisUnitVector[2] * omcp;
        return xm;
    }


    private float[][] computeEulerTransformMatrix(float angles[]) {
        /**
         * An adaptation of the euler function in euler.f
         */
        float[][] xm1 = new float[3][3];
        float[][] xm2 = new float[3][3];
        float[][] xm3 = new float[3][3];
        float[][] transformationMatrix = new float[3][3];

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                xm1[i][j] = 0;
                xm2[i][j] = 0;
                xm3[i][j] = 0;
            }
        }

        float c1 = (float) Math.cos(angles[0] / DEGREES_PER_RADIAN);
        float s1 = (float) Math.sin(angles[0] / DEGREES_PER_RADIAN);
        float c2 = (float) Math.cos(angles[1] / DEGREES_PER_RADIAN);
        float s2 = (float) -Math.sin(angles[1] / DEGREES_PER_RADIAN);
        float c3 = (float) Math.cos(angles[2] / DEGREES_PER_RADIAN);
        float s3 = (float) -Math.sin(angles[2] / DEGREES_PER_RADIAN);

        xm1[0][0] = 1.0f;
        xm1[1][1] = c1;
        xm1[2][2] = c1;
        xm1[1][2] = s1;
        xm1[2][1] = -s1;
        xm2[1][1] = 1.0f;
        xm2[0][0] = c2;
        xm2[2][2] = c2;
        xm2[2][0] = s2;
        xm2[0][2] = -s2;
        xm3[2][2] = 1.0f;
        xm3[1][1] = c3;
        xm3[0][0] = c3;
        xm3[0][1] = s3;

        float[][] xmm = multiplyMatrices(xm2, xm3);
        transformationMatrix = multiplyMatrices(xm1, xmm);
        return transformationMatrix;
    }

    private float computeLatitude(float[] geovec) {
        float tmp = (float) (Math.sqrt(geovec[0] * geovec[0] +
                    geovec[1] * geovec[1]) * OMF2);
        return DEGREES_PER_RADIAN * (float) Math.atan2(geovec[2], tmp);
    }

    private double computeQ(double a, double b, double h, double r, double sinl) {
        //  Solve for magnitude of sensor-to-pixel vector and compute components
        double q = (-b - Math.sqrt(r)) / (2.0 * a);
        //  Add out-of-plane correction
        q = q * (1.0 + sinl * h / Math.sqrt(r));
        return q;
    }

    float computeSensorAzimuth(float senz, double sn, double se) {
        float sena;
        // Check for zenith close to zero
        if (senz > 0.05f) {
            sena = (float) (DEGREES_PER_RADIAN * Math.atan2(se,sn));
        } else {
            sena = 0.0f;
        }
        if (sena < 0.0f) {
            sena = sena + 360.0f;
        }
        return sena;
    }

    float computeSensorZenith(double sn, double se, double sv) {
        return (float) (DEGREES_PER_RADIAN * Math.atan2(Math.sqrt(sn*sn+se*se),sv));
    }

    float computeSolarAzimuth(float solz, double sunn, double sune) {
        float sola;
        // Check for zenith close to zero
        if (solz > 0.05f) {
            sola = (float) (DEGREES_PER_RADIAN * Math.atan2(sune, sunn));
        } else {
            sola = 0.0f;
        }
        if (sola < 0.0f) {
            sola = sola + 360.0f;
        }
        return sola;
    }

    float computeSolarZenith(double sunn, double sune, double sunv) {
        return (float) (DEGREES_PER_RADIAN * Math.atan2(Math.sqrt(sunn * sunn + sune * sune), sunv));
    }

    private float[][] computeTransformMatrix(float tilt) {
        /**
         * Compute the ECEF-to-orbital tranformation matrix using the
         * sensor transformation matrix.  Corresponds to the get_xfm
         * function in the original Fortran.
         */
        float[][] xfmMatrix = new float[3][3];
        xfmMatrix[0][0] = 0.0f;
        xfmMatrix[0][1] = 0.0f;
        xfmMatrix[0][2] = 0.0f;
        xfmMatrix[1][0] = 0.0f;
        xfmMatrix[1][1] = 0.0f;
        xfmMatrix[1][2] = 0.0f;
        xfmMatrix[2][0] = 0.0f;
        xfmMatrix[2][1] = 0.0f;
        xfmMatrix[2][2] = 0.0f;

        float[][] sm1 = computeEulerAxisMatrix(tiltCosVector, tilt);
        float[][] sm2 = transposeMatrix(sm1);
        float[][] sm3 = multiplyMatrices(sm2, sensorOrientation);
        sm1 = transposeMatrix(sensorOffsetMatrix);
        sm2 = multiplyMatrices(sm1, sm3);
        sm3 = computeEulerTransformMatrix(attAngle);
        sm1 = transposeMatrix(sm3);
        xfmMatrix = multiplyMatrices(sm1, sm2);
        return xfmMatrix;
    }

    private float [] computeVerticalUnitVector(float[] geovec) {
        float [] up = new float[3];
        float uxy = geovec[0] * geovec[0] + geovec[1] * geovec[1];
        float temp = (float) Math.sqrt(geovec[2] * geovec[2] + OMF2 * OMF2 * uxy);
        up[0] = (float) (OMF2 * geovec[0] / temp);
        up[1] = (float) (OMF2 * geovec[1] / temp);
        up[2] = geovec[2] / temp;
        return up;
    }

    public static float[] crossProduct(float[] v1, float[] v2) {
        /**
         * Compute cross product of two (length 3) vectors (adapted from
         * crossp.f; also see:
         *     http://en.wikipedia.org/wiki/Cross_product
         * or a linear algebra text).  The array subscripts differ from the
         * definitional/Fortran subscripts due to Java using 0-based arrays, vs.
         * the definition/Fortran using 1-based arrays.
         */
        float[] v3;
        v3 = new float[3];
        /* */
        v3[0] = v1[1] * v2[2] - v1[2] * v2[1];
        v3[1] = v1[2] * v2[0] - v1[0] * v2[2];
        v3[2] = v1[0] * v2[1] - v1[1] * v2[0];
        return v3;
    }

    public static int determineNumberScanLines(NetcdfFile ncFile) {
        Attribute numScanLinesAttr = ncFile.findGlobalAttribute("Number_of_Scan_Lines");
        return numScanLinesAttr.getNumericValue().intValue();
    }

    private int determinePixelIncrement() {
        return ncFile.findGlobalAttribute("LAC_Pixel_Subsampling").getNumericValue().intValue();
    }

    private int determinePixelsPerScanLine() {
        return ncFile.findGlobalAttribute("Pixels_per_Scan_Line").getNumericValue().intValue();
    }

    public static SeaWiFSL1AGeonav.DataType determineSeawifsDataType(NetcdfFile ncFile) {
        SeaWiFSL1AGeonav.DataType dataType = SeaWiFSL1AGeonav.DataType.LAC;
        Attribute dataTypeAttr = ncFile.findGlobalAttribute("Data_Type");
        Attribute numScanLinesAttr = ncFile.findGlobalAttribute("Number_of_Scan_Lines");
        if (dataTypeAttr.getStringValue().equals("GAC")) {
            dataType = SeaWiFSL1AGeonav.DataType.GAC;
        }
        return dataType;
    }

    private int determineStartPixel() {
        return ncFile.findGlobalAttribute("LAC_Pixel_Start_Number").getNumericValue().intValue();
    }

    public void doComputations() {
        float[]  ea = new float[3];
        float[]  geovec = new float[3];
        float[]  no = new float[3];
        float[]  rmtq = new float[3];
        double   se;
        double   sn;
        double   sune = 0.0;
        double   sunn = 0.0;
        double   sunv = 0.0;
        double   sv;
        float[]  up = new float[3];

        //  Compute correction factor for out-of-plane angle
        double h = (sensorOrientation[0][1] * orbPos[0]
                    + sensorOrientation[1][1] * orbPos[1]
                    + sensorOrientation[2][1] * orbPos[2] / OMF2) * 2.0;

        //  Compute sensor-to-surface vectors for all scan angles
        for (int i = 0; i < pixPerScanLine; i ++) {
            int in = pixIncr * (i) + scanStartPix - 1;
	        double a = scanPathCoef[0] * cosa[in] * cosa[in]  +
                       scanPathCoef[1] * cosa[in] * sina[in] +
                       scanPathCoef[2] * sina[in] * sina[in];
            double b = scanPathCoef[3] * cosa[in]
                       + scanPathCoef[4] * sina[in];
            double c = scanPathCoef[5];
            double r = b * b - 4.0 * c * a;  // begin solve quadratic equation

            //  Check for scan past edge of Earth
            if (r < 0.0) {
                xlat[i] = 999.0f;
                xlon[i] = 999.0f;
                solz[i] = 999.0f;
                sola[i] = 999.0f;
                senz[i] = 999.0f;
                sena[i] = 999.0f;
            } else {
                double q = computeQ(a, b, h, r, sinl);
                double Qx = q * cosa[in];
                double Qy = q * sinl;
                double Qz = q * sina[in];

                //  Transform vector from sensor to geocentric frame
                for (int j = 0; j < 3; j++) {
                    rmtq[j] = (float) (Qx * sensorOrientation[j][0]
                              + Qy * sensorOrientation[j][1]
                              + Qz * sensorOrientation[j][2]);
                    geovec[j] = rmtq[j] + orbPos[j];
                }

                // Compute geodetic latitude and longitude
                xlat[i] = computeLatitude(geovec);
                xlon[i] = DEGREES_PER_RADIAN * (float) Math.atan2(geovec[1], geovec[0]);

                // Compute the local vertical, East and North unit vectors
                up = computeVerticalUnitVector(geovec);
                float upxy = (float) (Math.sqrt(up[0] * up[0] + up[1] * up[1]));
                ea = computeEastVector(up, upxy);
                no = crossProduct(up,ea);

                // Compute components of spacecraft and sun vector in the
                // vertical (up), North (no), and East (ea) vectors frame
                sv = 0.0;
                sn = 0.0;
                se = 0.0;
                sunv = 0.0;
                sunn = 0.0;
                sune = 0.0;
                for (int j = 0; j < 3; j++) {
                    double s = -rmtq[j];
                    sv = sv + s * up[j];
                    sn = sn + s * no[j];
                    se = se + s * ea[j];
                    sunv = sunv + sunUnitVec[j] * up[j];
                    sunn = sunn + sunUnitVec[j] * no[j];
                    sune = sune + sunUnitVec[j] * ea[j];
                }

                // Compute the sensor zenith and azimuth
                senz[i] = computeSensorZenith(sn, se, sv);
                sena[i] = computeSensorAzimuth(senz[i], sn, se);
            }  // close (else part of) if (r < 0.0)

            // Compute the solar zenith and azimuth
            solz[i] = computeSolarZenith(sunn, sune, sunv);
            sola[i] = computeSolarAzimuth(solz[i], sunn, sune);
        } // close for (int i = 0; i < npix; i ++)
    } // close doComputations()

    public int getFirstPixel() {
        return scanStartPix;
    }

    public float[][] getLatitudes() {
            return latitudes;
    }

    public float[][] getLongitudes() {
        return longitudes;
    }

    public int getPixelIncrement() {
        return pixIncr;
    }

    public float[][] getSensorAzimuths() {
        return sensorAzimuths;
    }

    public float[][] getSensorZeniths() {
        return sensorZeniths;
    }

    public float[][] getSolarAzimuths() {
        return solarAzimuths;
    }

    public float[][] getSolarZeniths() {
        return solarZeniths;
    }

    public int getNumberPixels() {
        return pixPerScanLine;
    }

    public int getNumberScanLines() {
        return numScanLines;
    }

    public static float[][] multiplyMatrices(float[][] m1, float[][] m2) {
        /**
         * Multiply two 3 x 3 matrices, adapted from matmpy.f.
         */
        float[][] p = new float[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                p[i][j] = 0.0f;
                for (int k = 0; k < 3; k++) {
                    p[i][j] = p[i][j] + m1[i][k] * m2[k][j];
                }
            }
        }
        return p;
    }

    private float[] populateVector(ArrayFloat sourceData, int size, int lineNum) {
        float[] newVect = new float[size];
        for(int i = 0; i < size; i ++) {
            newVect[i] = sourceData.getFloat(size * lineNum + i);
        }
        return newVect;
    }

    private ArrayFloat readNetcdfDataArray(String varName, Group group) {
        ArrayFloat dataArray = null;
        int[] startPts;
        Variable varToRead = group.findVariable(varName);
        if (varToRead.getRank() == 1) {
            try {
                dataArray = (ArrayFloat) varToRead.read();
            } catch(IOException ioe) {
                System.out.println("Encountered IOException reading the data array: " + varToRead.getShortName());
                System.out.println(ioe.getMessage());
                ioe.printStackTrace();
                System.out.println();
                System.exit(-43);
            }
        } else {
            if (varToRead.getRank() == 2) {
                startPts = new int[2];
                startPts[0] = 0;
                startPts[1] = 0;
            } else {
                // Assuming nothing with more than rank 3.
                startPts = new int[3];
                startPts[0] = 0;
                startPts[1] = 0;
                startPts[2] = 0;
            }
            try {
                dataArray = (ArrayFloat) varToRead.read(startPts, varToRead.getShape());
                return dataArray;
            } catch(IOException ioe) {
                System.out.println("Encountered IOException reading the data array: " + varToRead.getShortName());
                System.out.println(ioe.getMessage());
                ioe.printStackTrace();
                System.out.println();
                System.exit(-44);
            } catch(InvalidRangeException ire) {
                System.out.println("Encountered InvalidRangeException reading the data array: " + varToRead.getShortName());
                System.out.println(ire.getMessage());
                ire.printStackTrace();
                System.out.println();
                System.exit(-45);
            }
        }
        return dataArray;
    }

    public static float[][] transposeMatrix(float[][] matrix) {
        /**
         * Create the transpose of a 3 x 3 matrix, adapted from
         * xpose.f.
         */
        float[][] transpose = new float[3][3];
        for (int i = 0; i < 3; i ++ ) {
            for (int j = 0; i < 3; i ++) {
                transpose[j][i] = matrix[i][j];
            }
        }
        return transpose;
    }

}
