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
import opendap.dap.DArrayDimension;
import org.esa.beam.framework.gpf.ui.DefaultAppContext;
import org.esa.beam.opendap.HeadlessTestRunner;
import org.esa.beam.opendap.datamodel.DAPVariable;
import org.esa.beam.opendap.datamodel.OpendapLeaf;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import thredds.catalog.InvDataset;

import javax.swing.JCheckBox;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 * @author Tonio Fincke
 */
@RunWith(HeadlessTestRunner.class)
public class VariableFilterTest {

    private OpendapLeaf leaf;
    private DAPVariable dapVariable;
    private VariableFilter variableFilter;

    @Before
    public void setUp() {
        Lm.verifyLicense("Brockmann Consult", "BEAM", "lCzfhklpZ9ryjomwWxfdupxIcuIoCxg2");
        variableFilter = new VariableFilter(new JCheckBox(), new CatalogTree(null, new DefaultAppContext(""), null));
        variableFilter.getUI();
        leaf = new OpendapLeaf("leafName", new InvDataset(null, "") {
        });
        DArrayDimension[] dArrayDimensions = {new DArrayDimension(10, "dimName")};
        dapVariable = new DAPVariable("vName", "vType", "vDataType", dArrayDimensions);
        leaf.addDAPVariable(dapVariable);
    }

    @Test
    public void testAccept_AcceptAllIfNoFilterSet() throws Exception {
        variableFilter.addVariable(dapVariable);

        assertTrue(variableFilter.accept(leaf));
    }

    @Test
    public void testAccept_AcceptIfFilterSet() throws Exception {
        OpendapLeaf leaf2 = new OpendapLeaf("leafName2", new InvDataset(null, "") {
        });
        DAPVariable dapVariable2 = createDAPVariable("vName2");
        leaf2.addDAPVariable(dapVariable2);

        variableFilter.addVariable(dapVariable);
        variableFilter.addVariable(dapVariable2);
        variableFilter.setVariableSelected(dapVariable, true);
        variableFilter.setVariableSelected(dapVariable2, false);

        assertTrue(variableFilter.accept(leaf));
        assertFalse(variableFilter.accept(leaf2));
    }

    @Test
    public void testAccept_AcceptAllIfNoVariableIsSelected() throws Exception {
        OpendapLeaf leaf2 = new OpendapLeaf("leafName2", new InvDataset(null, "") {
        });
        DAPVariable dapVariable2 = createDAPVariable("vName2");
        leaf2.addDAPVariable(dapVariable2);

        variableFilter.addVariable(dapVariable);
        variableFilter.addVariable(dapVariable2);
        variableFilter.setVariableSelected(dapVariable, false);
        variableFilter.setVariableSelected(dapVariable2, false);

        assertTrue(variableFilter.accept(leaf));
        assertTrue(variableFilter.accept(leaf2));
    }

    @Test
    public void testAccept_AcceptNothingIfNoMatchingVariableIsSelected() throws Exception {
        OpendapLeaf leaf2 = new OpendapLeaf("leafName2", new InvDataset(null, "") {
        });
        DAPVariable dapVariable2 = createDAPVariable("vName2");
        leaf2.addDAPVariable(dapVariable2);

        DAPVariable dapVariable3 = createDAPVariable("vName3");

        variableFilter.addVariable(dapVariable);
        variableFilter.addVariable(dapVariable2);
        variableFilter.addVariable(dapVariable3);
        variableFilter.setVariableSelected(dapVariable, false);
        variableFilter.setVariableSelected(dapVariable2, false);
        variableFilter.setVariableSelected(dapVariable3, true);

        assertFalse(variableFilter.accept(leaf));
        assertFalse(variableFilter.accept(leaf2));
    }

    private DAPVariable createDAPVariable(String variableName) {
        DArrayDimension[] dArrayDimensions2 = {new DArrayDimension(10, "dimName2")};
        return new DAPVariable(variableName, "vType2", "vDataType2", dArrayDimensions2);
    }

}
