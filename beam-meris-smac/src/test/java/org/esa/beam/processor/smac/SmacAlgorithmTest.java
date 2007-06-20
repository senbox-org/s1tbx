/*
 * $Id: SmacAlgorithmTest.java,v 1.2 2007/04/19 10:41:39 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.esa.beam.processor.smac;

import java.io.FileNotFoundException;
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SmacAlgorithmTest extends TestCase {

    /*
    These are the reference results calculated with the original c-sources from
    Gerard DEDIEU CESBIO - Unite mixte CNES-CNRS-UPS.
    The coefficients for the pixel are in each case:
        sunZenith = 30.0
        sunAzimuth = 30.0
        viewZenith = 15.0
        viewAzimuth = 15.0
        taup550 = 0.5
        uh20 = 0.7
        uo3 = 0.3
        airPressure = 900.0
        toa_refl = 0.4
    and then looped over the different sensor coefficient sets
    */
    static double[] _reference = {0.468094529069023, // coef_ASTER1_CONT.dat
                                  0.430917490955927, // coef_ASTER1_DES.dat
                                  0.461342029523011, // coef_ASTER2_CONT.dat
                                  0.428992529994392, // coef_ASTER2_DES.dat
                                  0.470408616548691, // coef_ASTER3B_CONT.dat
                                  0.434972728967646, // coef_ASTER3B_DES.dat
                                  0.472640579528882, // coef_ASTER3N_CONT.dat
                                  0.437091917222285, // coef_ASTER3N_DES.dat
                                  0.441182451764032, // coef_ASTER4_CONT.dat
                                  0.415201280606522, // coef_ASTER4_DES.dat
                                  0.438643542240904, // coef_ASTER5_CONT.dat
                                  0.423308229318255, // coef_ASTER5_DES.dat
                                  0.443541240669732, // coef_ASTER6_CONT.dat
                                  0.428933562315611, // coef_ASTER6_DES.dat
                                  0.459657518954947, // coef_ASTER7_CONT.dat
                                  0.445700178500567, // coef_ASTER7_DES.dat
                                  0.521567170975127, // coef_ASTER8_CONT.dat
                                  0.506842985725600, // coef_ASTER8_DES.dat
                                  0.590143303835105, // coef_ASTER9_CONT.dat
                                  0.573935119748640, // coef_ASTER9_DES.dat
                                  0.433650603374593, // coef_MISR1_CONT.dat
                                  0.390897887585071, // coef_MISR1_DES.dat
                                  0.469412585976696, // coef_MISR2_CONT.dat
                                  0.431638285127395, // coef_MISR2_DES.dat
                                  0.449828137693103, // coef_MISR3_CONT.dat
                                  0.417712154021377, // coef_MISR3_DES.dat
                                  0.441591119997386, // coef_MISR4_CONT.dat
                                  0.406790518776699, // coef_MISR4_DES.dat
                                  0.461327947925237, // coef_MODIS1_CONT.dat
                                  0.428871402347875, // coef_MODIS1_DES.dat
                                  0.442949538024049, // coef_MODIS2_CONT.dat
                                  0.408158035789076, // coef_MODIS2_DES.dat
                                  0.436654270723612, // coef_MODIS3_CONT.dat
                                  0.395951007772092, // coef_MODIS3_DES.dat
                                  0.467610577010676, // coef_MODIS4_CONT.dat
                                  0.429787435263710, // coef_MODIS4_DES.dat
                                  0.439456234527464, // coef_MODIS5_CONT.dat
                                  0.407051175401646, // coef_MODIS5_DES.dat
                                  0.437512164557338, // coef_MODIS6_CONT.dat
                                  0.411143675694166, // coef_MODIS6_DES.dat
                                  0.439676745250465, // coef_MODIS7_CONT.dat
                                  0.423254243772214, // coef_MODIS7_DES.dat
                                  0.419793586189782, // coef_MODIS8_CONT.dat
                                  0.375225560448194, // coef_MODIS8_DES.dat
                                  0.432913560839322, // coef_MODIS9_CONT.dat
                                  0.389635748416660, // coef_MODIS9_DES.dat
                                  0.438892320591905, // coef_MODIS10_CONT.dat
                                  0.401002795199381, // coef_MODIS10_DES.dat
                                  0.456435084955153, // coef_MODIS11_CONT.dat
                                  0.419234164210766, // coef_MODIS11_DES.dat
                                  0.464087675812192, // coef_MODIS12_CONT.dat
                                  0.426257864134451, // coef_MODIS12_DES.dat
                                  0.451150574435740, // coef_MODIS13_CONT.dat
                                  0.419103304428866, // coef_MODIS13_DES.dat
                                  0.447845970070957, // coef_MODIS14_CONT.dat
                                  0.415979369662203, // coef_MODIS14_DES.dat
                                  0.442845802617623, // coef_MODIS15_CONT.dat
                                  0.409148050381570, // coef_MODIS15_DES.dat
                                  0.439613321935921, // coef_MODIS16_CONT.dat
                                  0.404940921879262, // coef_MODIS16_DES.dat
                                  0.517435198696615, // coef_MODIS17_CONT.dat
                                  0.479460622159371, // coef_MODIS17_DES.dat
                                  0.883303965959083, // coef_MODIS18_CONT.dat
                                  0.824207626660916, // coef_MODIS18_DES.dat
    };

    static String _referenceCoeffs[] = {"coef_ASTER1_CONT.dat",
                                        "coef_ASTER1_DES.dat",
                                        "coef_ASTER2_CONT.dat",
                                        "coef_ASTER2_DES.dat",
                                        "coef_ASTER3B_CONT.dat",
                                        "coef_ASTER3B_DES.dat",
                                        "coef_ASTER3N_CONT.dat",
                                        "coef_ASTER3N_DES.dat",
                                        "coef_ASTER4_CONT.dat",
                                        "coef_ASTER4_DES.dat",
                                        "coef_ASTER5_CONT.dat",
                                        "coef_ASTER5_DES.dat",
                                        "coef_ASTER6_CONT.dat",
                                        "coef_ASTER6_DES.dat",
                                        "coef_ASTER7_CONT.dat",
                                        "coef_ASTER7_DES.dat",
                                        "coef_ASTER8_CONT.dat",
                                        "coef_ASTER8_DES.dat",
                                        "coef_ASTER9_CONT.dat",
                                        "coef_ASTER9_DES.dat",
                                        "coef_MISR1_CONT.dat",
                                        "coef_MISR1_DES.dat",
                                        "coef_MISR2_CONT.dat",
                                        "coef_MISR2_DES.dat",
                                        "coef_MISR3_CONT.dat",
                                        "coef_MISR3_DES.dat",
                                        "coef_MISR4_CONT.dat",
                                        "coef_MISR4_DES.dat",
                                        "coef_MODIS1_CONT.dat",
                                        "coef_MODIS1_DES.dat",
                                        "coef_MODIS2_CONT.dat",
                                        "coef_MODIS2_DES.dat",
                                        "coef_MODIS3_CONT.dat",
                                        "coef_MODIS3_DES.dat",
                                        "coef_MODIS4_CONT.dat",
                                        "coef_MODIS4_DES.dat",
                                        "coef_MODIS5_CONT.dat",
                                        "coef_MODIS5_DES.dat",
                                        "coef_MODIS6_CONT.dat",
                                        "coef_MODIS6_DES.dat",
                                        "coef_MODIS7_CONT.dat",
                                        "coef_MODIS7_DES.dat",
                                        "coef_MODIS8_CONT.dat",
                                        "coef_MODIS8_DES.dat",
                                        "coef_MODIS9_CONT.dat",
                                        "coef_MODIS9_DES.dat",
                                        "coef_MODIS10_CONT.dat",
                                        "coef_MODIS10_DES.dat",
                                        "coef_MODIS11_CONT.dat",
                                        "coef_MODIS11_DES.dat",
                                        "coef_MODIS12_CONT.dat",
                                        "coef_MODIS12_DES.dat",
                                        "coef_MODIS13_CONT.dat",
                                        "coef_MODIS13_DES.dat",
                                        "coef_MODIS14_CONT.dat",
                                        "coef_MODIS14_DES.dat",
                                        "coef_MODIS15_CONT.dat",
                                        "coef_MODIS15_DES.dat",
                                        "coef_MODIS16_CONT.dat",
                                        "coef_MODIS16_DES.dat",
                                        "coef_MODIS17_CONT.dat",
                                        "coef_MODIS17_DES.dat",
                                        "coef_MODIS18_CONT.dat",
                                        "coef_MODIS18_DES.dat"};

    static int _vectorSize = 1;
    static float _defSza = 30.0f;
    static float _defSaa = 30.0f;
    static float _defVza = 15.0f;
    static float _defVaa = 15.0f;
    static float _defTaup550 = 0.5f;
    static float _defUh2o = 0.7f;
    static float _defUo3 = 0.3f;
    static float _defPressure = 900.0f;
    static float _defToa = 0.4f;

    public SmacAlgorithmTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(SmacAlgorithmTest.class);
    }

    public void testRun() {
        int n;
        SmacAlgorithm smac = new SmacAlgorithm();

        // generate test arrays
        // --------------------
        float[] sza = new float[_vectorSize];
        float[] saa = new float[_vectorSize];
        float[] vza = new float[_vectorSize];
        float[] vaa = new float[_vectorSize];
        float[] taup550 = new float[_vectorSize];
        float[] uh2o = new float[_vectorSize];
        float[] uo3 = new float[_vectorSize];
        float[] pressure = new float[_vectorSize];
        float[] toa = new float[_vectorSize];
        float[] t_surf = new float[_vectorSize];
        boolean[] process = new boolean[_vectorSize];
        float invalid = 0.f;

        for (n = 0; n < _vectorSize; n++) {
            sza[n] = _defSza;
            saa[n] = _defSaa;
            vza[n] = _defVza;
            vaa[n] = _defVaa;
            taup550[n] = _defTaup550;
            uh2o[n] = _defUh2o;
            uo3[n] = _defUo3;
            pressure[n] = _defPressure;
            toa[n] = _defToa;
            process[n] = true;
        }

        try {
            SensorCoefficientFile file = new SensorCoefficientFile();
            String filePath = new String("../../src/org/esa/beam/toolviews/smac/coefficients/");

            // loop over all sensor coefficient files and perform smac
            for (n = 0; n < _referenceCoeffs.length; n++) {
                file.readFile(filePath + _referenceCoeffs[n]);

                smac.setSensorCoefficients(file);
                t_surf = smac.run(sza, saa, vza, vaa, taup550, uh2o, uo3, pressure, process, invalid, toa, t_surf);

                assertEquals(_referenceCoeffs[n], _reference[n], t_surf[0], 1e-7f);
            }

            // check whether the process boolean array is working properly
            for (n = 0; n < _vectorSize; n++) {
                process[n] = false;
            }

            file.readFile(filePath + _referenceCoeffs[0]);

            smac.setSensorCoefficients(file);
            t_surf = smac.run(sza, saa, vza, vaa, taup550, uh2o, uo3, pressure, process, invalid, toa, t_surf);
            assertEquals(invalid, t_surf[0], 1e-7f);
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
    }

}