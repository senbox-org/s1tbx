/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
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
package org.esa.beam.smos.visat;

import junit.framework.TestCase;

import javax.swing.*;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class SnapshotSelectorModelTest extends TestCase {

    private SnapshotSelectorModel snapshotSelectorModel;
    private SpinnerModel spinnerModel;
    private BoundedRangeModel sliderModel;

    @Override
    protected void setUp() throws Exception {
        snapshotSelectorModel = new SnapshotSelectorModel(Arrays.asList(11, 17, 67));
        spinnerModel = snapshotSelectorModel.getSpinnerModel();
        sliderModel = snapshotSelectorModel.getSliderModel();
    }

    public void testSliderVariesWithSpinner() {
        spinnerModel.setValue(11);
        assertEquals(11, spinnerModel.getValue());
        assertEquals(0, sliderModel.getValue());

        spinnerModel.setValue(17);
        assertEquals(17, spinnerModel.getValue());
        assertEquals(1, sliderModel.getValue());

        spinnerModel.setValue(67);
        assertEquals(67, spinnerModel.getValue());
        assertEquals(2, sliderModel.getValue());
    }

    public void testSpinnerVariesWithSlider() {
        sliderModel.setValue(0);
        assertEquals(0, sliderModel.getValue());
        assertEquals(11, spinnerModel.getValue());

        sliderModel.setValue(1);
        assertEquals(1, sliderModel.getValue());
        assertEquals(17, spinnerModel.getValue());

        sliderModel.setValue(2);
        assertEquals(2, sliderModel.getValue());
        assertEquals(67, spinnerModel.getValue());
    }

    public void testSliderInfoVariesWithSlider() {
        sliderModel.setValue(0);
        assertEquals(0, sliderModel.getValue());
        assertEquals("1 / 3", snapshotSelectorModel.getSliderInfo());

        sliderModel.setValue(1);
        assertEquals(1, sliderModel.getValue());
        assertEquals("2 / 3", snapshotSelectorModel.getSliderInfo());

        sliderModel.setValue(2);
        assertEquals(2, sliderModel.getValue());
        assertEquals("3 / 3", snapshotSelectorModel.getSliderInfo());
    }


}
