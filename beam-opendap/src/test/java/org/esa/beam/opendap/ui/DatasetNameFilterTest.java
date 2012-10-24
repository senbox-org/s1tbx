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

import org.esa.beam.opendap.datamodel.OpendapLeaf;
import org.junit.Test;
import thredds.catalog.InvDataset;

import javax.swing.JCheckBox;

import static org.junit.Assert.*;

/**
 * @author Tonio Fincke
 * @author Thomas Storm
 */
public class DatasetNameFilterTest {

    @Test
    public void testAccept() throws Exception {
        DatasetNameFilter datasetNameFilter = new DatasetNameFilter(new JCheckBox());
        assertTrue(datasetNameFilter.accept(new OpendapLeaf("leafName", new InvDataset(null, "") {
                })));
        assertTrue(datasetNameFilter.accept(new OpendapLeaf("leafName", new InvDataset(null, "") {
                })));
        datasetNameFilter.expressionTextField.setText("x");
        assertFalse(datasetNameFilter.accept(new OpendapLeaf("leafName", new InvDataset(null, "") {
                })));
        datasetNameFilter.expressionTextField.setText("leafName");
        assertTrue(datasetNameFilter.accept(new OpendapLeaf("leafName", new InvDataset(null, "") {
                })));
        datasetNameFilter.expressionTextField.setText("*afNa*");
        assertTrue(datasetNameFilter.accept(new OpendapLeaf("leafName", new InvDataset(null, "") {
                })));
        datasetNameFilter.expressionTextField.setText("*afNa+");
        assertTrue(datasetNameFilter.accept(new OpendapLeaf("leafNaaaa", new InvDataset(null, "") {
                })));
        assertFalse(datasetNameFilter.accept(new OpendapLeaf("leafN", new InvDataset(null, "") {
                })));
        datasetNameFilter.expressionTextField.setText("*afNa?");
        assertTrue(datasetNameFilter.accept(new OpendapLeaf("leafN", new InvDataset(null, "") {
                })));
        assertTrue(datasetNameFilter.accept(new OpendapLeaf("leafNa", new InvDataset(null, "") {
                })));
        assertFalse(datasetNameFilter.accept(new OpendapLeaf("leafNam", new InvDataset(null, "") {
                })));
        datasetNameFilter.expressionTextField.setText("*afna*");
        assertTrue(datasetNameFilter.accept(new OpendapLeaf("leafNam", new InvDataset(null, "") {
                })));
    }
}
