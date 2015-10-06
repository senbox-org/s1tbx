package org.esa.snap.binning.support;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.*;


/**
 * See http://oceancolor.gsfc.nasa.gov/DOCS/OCSSW/l2bin_8c_source.html
 *
 * @author Norman Fomferra
 * @author Thomas Storm
 */
public class TemporalDataPeriodTest {

    public static class instr {

        int sday;
        int eday;
        boolean night;
    }

    public static class l2_prod {

        int nrec;
        int syear;
        int sday;
        int smsec;
        int nsamp;
        // length=nsamp, filled per scan line?
        float[] longitude;
        float[] latitude;
    }

    public static final String[] INPUT_FILES = {
            "MER_RR__2PNUPA20051202_102336_000026292043_00051_19644_7303.N1",
            "MER_RR__2PNUPA20051202_120412_000026292043_00052_19645_7304.N1",
            "MER_RR__2PNUPA20051202_134448_000026292043_00053_19646_1187.N1",
            "MER_RR__2PNUPA20051202_152524_000026292043_00054_19647_7305.N1",
            "MER_RR__2PNUPA20051202_170600_000026322043_00055_19648_7306.N1",
            "MER_RR__2PNUPA20051202_184636_000026322043_00056_19649_7307.N1",
            "MER_RR__2PNUPA20051202_202713_000026292043_00057_19650_7308.N1",
            "MER_RR__2PNUPA20051202_220749_000026292043_00058_19651_1188.N1",
            "MER_RR__2PNUPA20051202_234825_000026292043_00059_19652_7309.N1",
            "MER_RR__2PNUPA20051203_012901_000026292043_00060_19653_7310.N1",
            "MER_RR__2PNUPA20051203_030937_000026292043_00061_19654_7311.N1",
            "MER_RR__2PNUPA20051203_063049_000026292043_00063_19656_7312.N1",
            "MER_RR__2PNUPA20051203_081125_000026322043_00064_19657_7313.N1",
            "MER_RR__2PNUPA20051203_095201_000026292043_00065_19658_7314.N1",
            "MER_RR__2PNUPA20051203_113237_000026292043_00066_19659_7315.N1",
            "MER_RR__2PNUPA20051203_131314_000026552043_00067_19660_1189.N1",
            "MER_RR__2PNUPA20051203_145350_000026292043_00068_19661_7316.N1",
            "MER_RR__2PNUPA20051203_163426_000026292043_00069_19662_7317.N1",
            "MER_RR__2PNUPA20051203_181502_000026292043_00070_19663_7318.N1",
            "MER_RR__2PNUPA20051203_195538_000026292043_00071_19664_1190.N1",
            "MER_RR__2PNUPA20051203_213614_000026292043_00072_19665_7319.N1",
            "MER_RR__2PNUPA20051203_231651_000026292043_00073_19666_7320.N1",
            "MER_RR__2PNUPA20051204_005727_000026292043_00074_19667_7321.N1",
            "MER_RR__2PNUPA20051204_023803_000026292043_00075_19668_7322.N1",
            "MER_RR__2PNUPA20051204_041839_000026292043_00076_19669_7323.N1",
            "MER_RR__2PNUPA20051204_055915_000026292043_00077_19670_7324.N1",
            "MER_RR__2PNUPA20051204_073951_000026292043_00078_19671_1191.N1",
            "MER_RR__2PNUPA20051204_092027_000026292043_00079_19672_7325.N1"
    };

    /**
     * at the moment: just printing
     *
     * @throws Exception
     */
    @Test
    @Ignore("Ignoring in order to not irritate the build server")
    public void testName() throws Exception {

        instr input = new instr();
        input.sday = 2005337;    // tweak these in order to see different behaviour. 2005337 = the 337rd day of 2005 = Dec 2nd
        input.eday = 2005338;    //

        final int nfiles = INPUT_FILES.length;
        final l2_prod[] l2_str = new l2_prod[nfiles];

        final int proc_day_beg = input.sday;
        final int proc_day_end = input.eday;
        // ...

        /*
         * Values of brk_scan[ifile]:
         *     0 = fully contained input file
         *    -1 = product start date one day earlier than input start date, product start time is later than 19.00 PM, and product crosses the 180° meridian
         *    +1 = product start date one day after than input end date, product start time is later than 19.00 PM, and product crosses the 180° meridian
         * -9999 = "early" or "late" input file
         */
        int[] brk_scan = new int[nfiles];

        for (int ifile = 0; ifile < nfiles; ifile++) {

            final String inputFile = INPUT_FILES[ifile];
            Product product = ProductIO.readProduct("C:\\dev\\Ressourcen\\EOData\\binning_testdata_oc\\" + inputFile);
            l2_str[ifile] = new l2_prod();
            Calendar startTime = product.getStartTime().getAsCalendar();
            l2_str[ifile].syear = startTime.get(Calendar.YEAR);
            l2_str[ifile].sday = startTime.get(Calendar.DAY_OF_YEAR);
            int hourAsMillisecond = startTime.get(Calendar.HOUR_OF_DAY) * 60 * 60 * 1000;
            int minuteAsMillisecond = startTime.get(Calendar.MINUTE) * 60 * 1000;
            int secondAsMillisecond = startTime.get(Calendar.SECOND) * 1000;
            l2_str[ifile].smsec =
                    hourAsMillisecond + minuteAsMillisecond + secondAsMillisecond + startTime.get(Calendar.MILLISECOND);
            l2_str[ifile].nrec = product.getSceneRasterHeight();
            l2_str[ifile].nsamp = product.getSceneRasterWidth();
            l2_str[ifile].longitude = new float[product.getSceneRasterHeight() * product.getSceneRasterWidth()];
            l2_str[ifile].latitude = new float[product.getSceneRasterHeight() * product.getSceneRasterWidth()];
            fillArray(l2_str[ifile].longitude, product, "longitude");
            fillArray(l2_str[ifile].latitude, product, "latitude");
            product.closeIO();

            // ...
            int syear = l2_str[ifile].syear;
            int sday = l2_str[ifile].sday;
            int smsec = l2_str[ifile].smsec;

            int date = date(syear, sday);
            int diffday_beg = diffday(date, proc_day_beg);
            int diffday_end = diffday(date, proc_day_end);
            int ssec = smsec / 1000;

            // ...

            float[] slon = new float[product.getSceneRasterHeight()];
            float[] elon = new float[product.getSceneRasterHeight()];
            for (int i = 0; i < product.getSceneRasterHeight(); i++) {
                slon[i] = l2_str[ifile].longitude[i * product.getSceneRasterWidth()];
                elon[i] = l2_str[ifile].longitude[i * product.getSceneRasterWidth() + product.getSceneRasterWidth() -
                                                  1];
            }
            boolean scancross = scancross(product.getSceneRasterHeight(), slon, elon);

            /* Determine brk_scan value */
            /* ------------------------ */
            brk_scan[ifile] = 0;

            // ...

            // } else if (strcmp(small_buf, "MERIS") == 0) {

            // ...

            // What is p1hr? Unit is hours. See also:
            //
            // Revision 2.4.8 10/14/12
            // Put MERIS p1hr dataday parameter back to 19
            // J. Gales

            int p1hr = 19;

            if (diffday_beg <= -2) {
                brk_scan[ifile] = -9999;
            } else if (diffday_end >= +2) {
                brk_scan[ifile] = -9999;
            }

            if (diffday_beg == -1) {
                boolean later = ssec > p1hr * 60 * 60;
                if (later && scancross) {
                    /*
                     * Conditions for this:
                     *
                     * o product start date one day earlier than input date
                     * o product start time is later than 19.00 PM
                     * o product crosses the 180° meridian
                     */
                    brk_scan[ifile] = -1;
                } else if (later && (!scancross)) {
                    brk_scan[ifile] = 0;
                } else {
                    brk_scan[ifile] = -9999;
                }
            }

            if (diffday_end == +1) {
                brk_scan[ifile] = -9999;
            } else if (date == proc_day_beg && date == proc_day_end) {
                if (ssec > p1hr * 60 * 60) {
                    if (scancross) {
                    /*
                     * Conditions for this:
                     *
                     * o product start date is one day after the input end date
                     * o product start time is later than 19.00 PM
                     * o product crosses the 180° meridian
                     */
                        brk_scan[ifile] = +1;
                    } else {
                        brk_scan[ifile] = -9999;
                    }
                }
            }

            // ...

            System.out.println("brk_scan[" + INPUT_FILES[ifile] + "] = " + brk_scan[ifile]);
            l2_str[ifile] = null; // free resources
        }

        System.exit(0);

        for (int ifile = 0; ifile < nfiles; ifile++) {

            /* if "early" or "late" input file then skip */
            /* ----------------------------------------- */
            if (brk_scan[ifile] == -9999) {
                continue;
            }

            // ...

            /* Get date stuff */
            /* -------------- */
            int date = l2_str[ifile].syear * 1000 + l2_str[ifile].sday;
            int diffday_beg = diffday(date, proc_day_beg);
            int diffday_end = diffday(date, proc_day_end);
            int sday = l2_str[ifile].sday;
            int ssec = l2_str[ifile].smsec / 1000;

            // ...

            /* Loop over swath rows */
            /* ^^^^^^^^^^^^^^^^^^^^ */
            for (int jsrow = 0; jsrow < l2_str[ifile].nrec; jsrow++) {

                // ...

                /* Compute scan_frac */
                /* ----------------- */
                int nsamp = l2_str[ifile].nsamp; // compute_scanfrac(ifile, ipixl, flagusemask, required);
                if (nsamp == 0) {
                    continue;
                }

                // ...

                /* ##### Loop over L2 pixels ##### */
                /* ------------------------------- */
                for (int isamp = 0; isamp < nsamp; isamp++) {

                    // ipixl = floor((float64) scan_frac[isamp]);
                    // nf: scan_frac is used for input.interp == 1 only
                    int ipixl = isamp;

                    // ...

                    /* Check for dateline crossing */
                    /* --------------------------- */
                    if (input.night) {

                        if ((brk_scan[ifile] == -1) &&
                            (diffday_beg == -1) &&
                            (l2_str[ifile].longitude[ipixl] < 0)) {
                            continue;
                        }

                        if ((brk_scan[ifile] == +1) &&
                            (diffday_end == 0) &&
                            (l2_str[ifile].longitude[ipixl] > 0)) {
                            continue;
                        }

                    } else {

                        if ((brk_scan[ifile] == -1) &&
                            (diffday_beg <= 0) &&
                            (l2_str[ifile].longitude[ipixl] < 0)) {
                            continue;
                        }

                        if ((brk_scan[ifile] == +1) &&
                            (diffday_end >= 0) &&
                            (l2_str[ifile].longitude[ipixl] > 0)) {
                            continue;
                        }
                    }

                    // if we come here, ipixl is valid
                }
            }
        }

    }

    private void fillArray(float[] array, Product product, String gridName) {
        int j = 0;
        for (int y = 0; y < product.getSceneRasterHeight(); y++) {
            for (int x = 0; x < product.getSceneRasterWidth(); x++) {
                array[j] = product.getTiePointGrid(gridName).getSampleFloat(x, y);
                j++;
            }
        }
    }


    @Test
    public void testScancross() throws Exception {

        assertFalse(scancross(5,
                              new float[]{0.0f, 1.0f, 2.0f, 3.0f, 4.0f},
                              new float[]{0.5f, 1.5f, 2.5f, 3.5f, 4.5f}));

        // Crossing 0-meridian W-->E. Why shall this be true?
        assertTrue(scancross(6,
                             new float[]{-2.0f, -1.0f, 0.0f, 1.0f, 2.0f, 3.0f},
                             new float[]{-1.5f, -0.5f, 0.5f, 1.5f, 2.5f, 3.5f}));
        // Crossing 0-meridian E-->W.
        assertFalse(scancross(6,
                              new float[]{2.0f, 1.0f, 0.0f, -1.0f, -2.0f, -3.0f},
                              new float[]{2.5f, 1.5f, 0.5f, -0.5f, -1.5f, -2.5f}));

        // Crossing 180-meridian W-->E.
        assertTrue(scancross(5,
                             new float[]{-178.0f, -179.0f, 180.0f, 179.0f, 178.0f},
                             new float[]{-177.5f, -178.5f, -179.5f, 179.5f, 178.5f}));
        // Crossing 180-meridian E-->W.
        assertTrue(scancross(5,
                             new float[]{178.0f, 179.0f, 180.0f, -179.0f, -178.0f},
                             new float[]{178.5f, 179.5f, -179.5f, -178.5f, -177.5f}));

    }


    public static int date(int year, int doy) {
        return year * 1000 + doy;
    }


    /**
     * Determine if swath crossed dateline
     */
    public static boolean scancross(int nrec, float[] slon, float[] elon) {
        /*
        Original C code:

        scancross = 0;
        for (jsrow=l2_str[ifile].nrec-1; jsrow>=1; jsrow--) {
            scancross = slon[jsrow] >= 0 && slon[jsrow-1] < 0;
            if (scancross == 1) break;
            scancross = slon[jsrow] >= 0 && elon[jsrow] < 0;
            if (scancross == 1) break;
       }

         */

        boolean scancross = false;
        for (int jsrow = nrec - 1; jsrow >= 1; jsrow--) {
            scancross = slon[jsrow] >= 0 && slon[jsrow - 1] < 0;
            if (scancross) {
                break;
            }
            scancross = slon[jsrow] >= 0 && elon[jsrow] < 0;
            if (scancross) {
                break;
            }
        }
        return scancross;
    }

    public static boolean isleap(int year) {
        return ((year % 400) == 0) || (((year % 4) == 0) && ((year % 100) != 0));
    }

    /**
     * date1 - date 2
     */
    public static int diffday(int date1, int date2) {

        int i;
        int year1, year2;
        int day1, day2;

        year1 = date1 / 1000;
        year2 = date2 / 1000;
        day1 = date1 % 1000;
        day2 = date2 % 1000;

        for (i = year2; i < year1; i++) {
            if (isleap(i)) {
                day1 += 366;
            } else {
                day1 += 365;
            }
        }

        for (i = year1; i < year2; i++) {
            if (isleap(i)) {
                day2 += 366;
            } else {
                day2 += 365;
            }
        }

        return day1 - day2;
    }


}
