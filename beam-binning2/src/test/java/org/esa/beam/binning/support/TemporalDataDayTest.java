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
    @Test
    public void testName() throws Exception {

        // ...

        int proc_day_beg = date(2010, 100); // input.sday;
        int proc_day_end = date(2010, 101); // input.eday;

        // ...

        int nfiles = 5;
        int[] brk_scan = new int[nfiles];

        for (int ifile = 0; ifile < nfiles; ifile++) {

            // ...
            int syear = 2010;   // l2_str[ifile].syear
            int sday = 100;     // l2_str[ifile].sday;
            int smsec = 10000;  // l2_str[ifile].smsec

            int date = date(syear, sday);
            int diffday_beg = diffDay(date, proc_day_beg);
            int diffday_end = diffDay(date, proc_day_end);
            //sday = l2_str[ifile].sday;
            int ssec = smsec / 1000;

            // ...

            float[] slon = new float[0];
            float[] elon = new float[0];
            boolean scancross = isScancross(0, slon, elon);

            /* Determine brk_scan value */
            /* ------------------------ */
            brk_scan[ifile] = 0;
            int cde = 0;

            // ...

            // } else if (strcmp(small_buf, "MERIS") == 0) {

            // ...

            // What is p1hr? Unit is hours.
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
                } else brk_scan[ifile] = -9999;
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

        // test something on brk_scan/cde here
    }

    @Test
    public void testIsScancross() throws Exception {

        assertFalse(isScancross(5,
                                new float[]{0.0f, 1.0f, 2.0f, 3.0f, 4.0f},
                                new float[]{0.5f, 1.5f, 2.5f, 3.5f, 4.5f}));

        // Crossing 0-meridian W-->E. Why shall this be true?
        assertTrue(isScancross(6,
                               new float[]{-2.0f, -1.0f, 0.0f, 1.0f, 2.0f, 3.0f},
                               new float[]{-1.5f, -0.5f, 0.5f, 1.5f, 2.5f, 3.5f}));
        // Crossing 0-meridian E-->W.
        assertFalse(isScancross(6,
                                new float[]{2.0f, 1.0f, 0.0f, -1.0f, -2.0f, -3.0f},
                                new float[]{2.5f, 1.5f, 0.5f, -0.5f, -1.5f, -2.5f}));

        // Crossing 180-meridian W-->E.
        assertTrue(isScancross(5,
                               new float[]{-178.0f, -179.0f, 180.0f, 179.0f, 178.0f},
                               new float[]{-177.5f, -178.5f, -179.5f, 179.5f, 178.5f}));
        // Crossing 180-meridian E-->W.
        assertTrue(isScancross(5,
                               new float[]{178.0f, 179.0f, 180.0f, -179.0f, -178.0f},
                               new float[]{178.5f, 179.5f, -179.5f, -178.5f, -177.5f}));

    }


    public static int date(int year, int doy) {
        return year * 1000 + doy;
    }


    /**
     * Determine if swath crossed dateline
     */
    public static boolean isScancross(int nrec, float[] slon, float[] elon) {
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

    public static boolean isLeap(int year) {
        return ((year % 400) == 0) || (((year % 4) == 0) && ((year % 100) != 0));
    }

    /**
     * date1 - date 2
     */
    public static int diffDay(int date1, int date2) {

        int i;
        int year1, year2;
        int day1, day2;

        year1 = date1 / 1000;
        year2 = date2 / 1000;
        day1 = date1 % 1000;
        day2 = date2 % 1000;

        for (i = year2; i < year1; i++) {
            if (isLeap(i)) {
                day1 += 366;
            } else {
                day1 += 365;
            }
        }

        for (i = year1; i < year2; i++) {
            if (isLeap(i)) {
                day2 += 366;
            } else {
                day2 += 365;
            }
        }

        return day1 - day2;
    }


}
