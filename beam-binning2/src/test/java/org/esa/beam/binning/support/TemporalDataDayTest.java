package org.esa.beam.binning.support;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * See http://oceancolor.gsfc.nasa.gov/DOCS/OCSSW/l2bin_8c_source.html
 *
 * @author Norman Fomferra
 * @author Thomas Storm
 */
public class TemporalDataDayTest {

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


    @Test
    public void testName() throws Exception {

        instr input = new instr();
        input.sday = 2010100;
        input.eday = 2010101;

        final int nfiles = 1;
        final l2_prod[] l2_str = new l2_prod[nfiles];
        l2_str[0] = new l2_prod();
        l2_str[0].syear = 2010;
        l2_str[0].sday = 62;
        l2_str[0].smsec = 1000;
        l2_str[0].nrec = 800;
        l2_str[0].nsamp = 1121;
        l2_str[0].longitude = new float[1121];
        l2_str[0].latitude = new float[1121];
        // ...

        final int proc_day_beg = input.sday;
        final int proc_day_end = input.eday;
        // ...


        /*
         * Values of brk_scan[ifile]:
         *     0 = regular input file
         *    -1 = ?
         *    +1 = ?
         * -9999 = "early" or "late" input file
         */
        int[] brk_scan = new int[nfiles];

        for (int ifile = 0; ifile < nfiles; ifile++) {

            // ...
            int syear = l2_str[ifile].syear;
            int sday = l2_str[ifile].sday;
            int smsec = l2_str[ifile].smsec;

            int date = date(syear, sday);
            int diffday_beg = diffday(date, proc_day_beg);
            int diffday_end = diffday(date, proc_day_end);
            int ssec = smsec / 1000;

            // ...

            float[] slon = new float[0];
            float[] elon = new float[0];
            boolean scancross = scancross(0, slon, elon);

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

            if (diffday_beg <= -2)
                brk_scan[ifile] = -9999;
            else if (diffday_end >= +2)
                brk_scan[ifile] = -9999;

            if (diffday_beg == -1) {
                if (ssec > p1hr * 60 * 60 && scancross) {
                    brk_scan[ifile] = -1;
                } else if ((ssec > p1hr * 60 * 60) && (!scancross)) {
                    brk_scan[ifile] = 0;
                } else
                    brk_scan[ifile] = -9999;
            }

            if (diffday_end == +1) {
                brk_scan[ifile] = -9999;
            } else if (date == proc_day_beg && date == proc_day_end) {
                if (ssec > p1hr * 60 * 60) {
                    if (scancross)
                        brk_scan[ifile] = +1;
                    else
                        brk_scan[ifile] = -9999;
                }
            }

            // ...
        }


        for (int ifile = 0; ifile < nfiles; ifile++) {

            /* if "early" or "late" input file then skip */
            /* ----------------------------------------- */
            if (brk_scan[ifile] == -9999) continue;

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
                if (nsamp == 0) continue;

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
                                (l2_str[ifile].longitude[ipixl] < 0)) continue;

                        if ((brk_scan[ifile] == +1) &&
                                (diffday_end == 0) &&
                                (l2_str[ifile].longitude[ipixl] > 0)) continue;

                    } else {

                        if ((brk_scan[ifile] == -1) &&
                                (diffday_beg <= 0) &&
                                (l2_str[ifile].longitude[ipixl] < 0)) continue;

                        if ((brk_scan[ifile] == +1) &&
                                (diffday_end >= 0) &&
                                (l2_str[ifile].longitude[ipixl] > 0)) continue;
                    }

                    // if we come here, ipixl is valid
                }
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
