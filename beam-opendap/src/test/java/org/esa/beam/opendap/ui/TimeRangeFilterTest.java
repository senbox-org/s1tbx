/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.opendap.ui;

import com.jidesoft.utils.Lm;
import org.esa.beam.opendap.datamodel.OpendapLeaf;
import org.esa.beam.util.TimeStampExtractor;
import org.junit.BeforeClass;
import org.junit.Test;
import thredds.catalog.InvDataset;
import ucar.nc2.units.DateRange;

import javax.swing.JCheckBox;
import java.util.GregorianCalendar;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 * @author Tonio Fincke
 */
public class TimeRangeFilterTest {

    @BeforeClass
    public static void setUp() throws Exception {
        Lm.verifyLicense("Brockmann Consult", "BEAM", "lCzfhklpZ9ryjomwWxfdupxIcuIoCxg2");
    }

    @Test
    public void testAccept_UserStartFileStart() throws Exception {
        final JCheckBox filterCheckBox = new JCheckBox();
        filterCheckBox.setSelected(true);
        TimeRangeFilter filter = new TimeRangeFilter(filterCheckBox);
        filter.startDate = new GregorianCalendar(2010, 0, 1, 12, 37, 15).getTime();
        filter.endDate = null;
        filter.timeStampExtractor = new TimeStampExtractor("yyyyMMdd:hhmmss", "*${startDate}*");

        assertTrue(filter.accept(new OpendapLeaf("sth__20100101:192345.nc", createNullDataset())));
        assertFalse(filter.accept(new OpendapLeaf("sth__20091231:192345.nc", createNullDataset())));
        assertTrue(filter.accept(new OpendapLeaf("does_not_match_naming_pattern", createNullDataset())));
    }

    @Test
    public void testAccept_UserStartFileBoth() throws Exception {
        final JCheckBox filterCheckBox = new JCheckBox();
        filterCheckBox.setSelected(true);
        TimeRangeFilter filter = new TimeRangeFilter(filterCheckBox);
        filter.startDate = new GregorianCalendar(2010, 0, 1, 12, 37, 15).getTime();
        filter.endDate = null;
        filter.timeStampExtractor = new TimeStampExtractor("yyyyMMdd:hhmmss", "*${startDate}*${endDate}*");

        assertTrue(filter.accept(new OpendapLeaf("sth__20100101:192345___20100102:012345__.nc", createNullDataset())));
        assertFalse(filter.accept(new OpendapLeaf("sth__20091231:192345___20100102:012345__.nc", createNullDataset())));
        assertFalse(filter.accept(new OpendapLeaf("sth__20091231:192345___20091231:233012__.nc", createNullDataset())));
    }

    @Test
    public void testAccept_UserEndFileStart() throws Exception {
        final JCheckBox filterCheckBox = new JCheckBox();
        filterCheckBox.setSelected(true);
        TimeRangeFilter filter = new TimeRangeFilter(filterCheckBox);
        filter.startDate = null;
        filter.endDate = new GregorianCalendar(2010, 0, 2, 12, 37, 15).getTime();

        filter.timeStampExtractor = new TimeStampExtractor("yyyyMMdd:hhmmss", "*${startDate}*");

        assertTrue(filter.accept(new OpendapLeaf("sth__20080101:192345.nc", createNullDataset())));
        assertFalse(filter.accept(new OpendapLeaf("sth__20111231:192345.nc", createNullDataset())));
    }

    @Test
    public void testAccept_UserEndFileBoth() throws Exception {
        TimeRangeFilter filter = new TimeRangeFilter(new JCheckBox());
        filter.startDate = null;
        filter.endDate = new GregorianCalendar(2010, 0, 2, 12, 37, 15).getTime();

        filter.timeStampExtractor = new TimeStampExtractor("yyyyMMdd:hhmmss", "*${startDate}*${endDate}*");

        assertTrue(filter.accept(new OpendapLeaf("sth__20100101:192345___20100102:012345__.nc", createNullDataset())));
        assertFalse(filter.accept(new OpendapLeaf("sth__20091231:192345___20100103:012345__.nc", createNullDataset())));
        assertFalse(filter.accept(new OpendapLeaf("sth__20100103:192345___20100103:233012__.nc", createNullDataset())));
    }

    @Test
    public void testAccept_UserBothFileStart() throws Exception {
        TimeRangeFilter filter = new TimeRangeFilter(new JCheckBox());
        filter.startDate = new GregorianCalendar(2010, 0, 1, 12, 37, 15).getTime();
        filter.endDate = new GregorianCalendar(2010, 0, 2, 12, 37, 15).getTime();

        filter.timeStampExtractor = new TimeStampExtractor("yyyyMMdd:hhmmss", "*${startDate}*");

        assertTrue(filter.accept(new OpendapLeaf("sth__20100101:192345.nc", createNullDataset())));
        assertFalse(filter.accept(new OpendapLeaf("sth__20091231:192345.nc", createNullDataset())));
        assertFalse(filter.accept(new OpendapLeaf("sth__20111231:192345.nc", createNullDataset())));
        assertTrue(filter.accept(new OpendapLeaf("does_not_match_naming_pattern", createNullDataset())));
    }

    private static InvDataset createNullDataset() {
        return new InvDataset(null, "") {
                };
    }


    @Test
    public void testAccept_UserBothFileBoth() throws Exception {
        TimeRangeFilter filter = new TimeRangeFilter(new JCheckBox());
        filter.startDate = new GregorianCalendar(2010, 0, 1, 12, 37, 15).getTime();
        filter.endDate = new GregorianCalendar(2010, 0, 2, 12, 37, 15).getTime();
        filter.timeStampExtractor = new TimeStampExtractor("yyyyMMdd:hhmmss", "*${startDate}*${endDate}*");

        assertTrue(filter.accept(new OpendapLeaf("sth__20100101:192345___20100102:012345__.nc", createNullDataset())));
        assertFalse(filter.accept(new OpendapLeaf("sth__20091231:192345___20100102:012345__.nc", createNullDataset())));
        assertFalse(filter.accept(new OpendapLeaf("sth__20100101:192345___20100103:012345__.nc", createNullDataset())));
        assertFalse(filter.accept(new OpendapLeaf("sth__20091231:192345___20100103:012345__.nc", createNullDataset())));
        assertFalse(filter.accept(new OpendapLeaf("sth__20091231:192345___20091231:233012__.nc", createNullDataset())));
        assertFalse(filter.accept(new OpendapLeaf("sth__20100103:004523___20100103:012345__.nc", createNullDataset())));
        assertTrue(filter.accept(new OpendapLeaf("sth__20100101:192345_does_not_match_naming_pattern.nc", createNullDataset())));
        assertTrue(filter.accept(new OpendapLeaf("sth__20100104:192345_does_not_match_naming_pattern.nc", createNullDataset())));
        assertTrue(filter.accept(new OpendapLeaf("does_not_match_naming_pattern", createNullDataset())));
    }

    @Test
    public void testAccept_ServerSpecifiedTimeRange() throws Exception {
        TimeRangeFilter filter = new TimeRangeFilter(new JCheckBox());
        filter.startDate = new GregorianCalendar(2010, 0, 1).getTime();
        filter.endDate = new GregorianCalendar(2010, 0, 2).getTime();

        OpendapLeaf leaf = createLeaf();
        assertFalse(filter.accept(leaf));

        filter.startDate = new GregorianCalendar(2010, 0, 1).getTime();
        filter.endDate = new GregorianCalendar(2011, 0, 2).getTime();

        assertTrue(filter.accept(leaf));

        filter.startDate = new GregorianCalendar(2010, 0, 2).getTime();
        filter.endDate = null;

        assertFalse(filter.accept(leaf));

        filter.startDate = null;
        filter.endDate = new GregorianCalendar(2010, 11, 31).getTime();

        assertFalse(filter.accept(leaf));

        filter.startDate = new GregorianCalendar(2009, 11, 31).getTime();
        filter.endDate = null;

        assertTrue(filter.accept(leaf));

        filter.startDate = null;
        filter.endDate = new GregorianCalendar(2011, 11, 31).getTime();

        assertTrue(filter.accept(leaf));

    }

    private OpendapLeaf createLeaf() {
        return new OpendapLeaf("", new InvDataset(null, "") {
            @Override
            public DateRange getTimeCoverage() {
                return new DateRange(new GregorianCalendar(2010, 0, 1).getTime(), new GregorianCalendar(2011, 0, 1).getTime());
            }
        });
    }
}
