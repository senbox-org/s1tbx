/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.framework.ui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class GUIElementFactoryTest extends TestCase {

    public GUIElementFactoryTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(GUIElementFactoryTest.class);
    }

    public void testAddComponentsToBorderPanel() {
        BorderLayout bl = new BorderLayout();
        JPanel panel = new JPanel(bl);
        JTextArea centerComp = new JTextArea("textArea");
        JLabel westComp = new JLabel("text");
        String place = BorderLayout.WEST;

        BorderLayoutUtils.addToPanel(panel, centerComp, westComp, place);

        assertEquals(2, panel.getComponentCount());
        assertEquals(true, panel.isAncestorOf(centerComp));
        assertEquals(true, panel.isAncestorOf(westComp));
    }

    public void testGridBagPanel_fourParams() {
        GridBagLayout gbl = new GridBagLayout();
        JPanel panel = new JPanel(gbl);
        JLabel comp = new JLabel("text");

        final GridBagConstraints gbconstr = GridBagUtils.createDefaultConstraints();
        GridBagUtils.setAttributes(gbconstr, "gridx=2, gridy=4");
        GridBagUtils.addToPanel(panel, comp, gbconstr);

        GridBagConstraints gbc = gbl.getConstraints(comp);
        assertEquals(GridBagConstraints.WEST, gbc.anchor);
        assertEquals(0, gbc.insets.top);
        assertEquals(3, gbc.insets.left);
        assertEquals(0, gbc.insets.bottom);
        assertEquals(3, gbc.insets.right);
        assertEquals(2, gbc.gridx);
        assertEquals(4, gbc.gridy);
        assertEquals(0, gbc.weightx, 0.01);
        assertEquals(0, gbc.weighty, 0.01);
        assertEquals(0, gbc.ipadx);
        assertEquals(0, gbc.ipady);
        assertEquals(1, gbc.gridheight);
        assertEquals(1, gbc.gridwidth);
    }

    public void testGridBagPanel_sevenParams() {
        GridBagLayout gbl = new GridBagLayout();
        JPanel panel = new JPanel(gbl);
        JLabel comp = new JLabel("text");


        final GridBagConstraints gbconstraints = GridBagUtils.createDefaultConstraints();
        GridBagUtils.setAttributes(gbconstraints, "gridx=2, gridy=4, anchor=SOUTHEAST, weighty=1.3, insets.top=5");
        GridBagUtils.addToPanel(panel, comp, gbconstraints);

        GridBagConstraints gbc = gbl.getConstraints(comp);
        assertEquals(GridBagConstraints.SOUTHEAST, gbc.anchor);
        assertEquals(5, gbc.insets.top);
        assertEquals(3, gbc.insets.left);
        assertEquals(0, gbc.insets.bottom);
        assertEquals(3, gbc.insets.right);
        assertEquals(2, gbc.gridx);
        assertEquals(4, gbc.gridy);
        assertEquals(0, gbc.weightx, 0.01);
        assertEquals(1.3, gbc.weighty, 0.01);
        assertEquals(0, gbc.ipadx);
        assertEquals(0, gbc.ipady);
        assertEquals(1, gbc.gridheight);
        assertEquals(1, gbc.gridwidth);
    }

    public void testGridBagPanel_eightParams() {
        GridBagLayout gbl = new GridBagLayout();
        JPanel panel = new JPanel(gbl);
        JLabel comp = new JLabel("text");

        final GridBagConstraints gbconstraints = GridBagUtils.createDefaultConstraints();
        GridBagUtils.setAttributes(gbconstraints,
                                   "gridx=2, gridy=4, anchor=SOUTHEAST, weighty=1.3, insets.top=5, gridwidth=3");
        GridBagUtils.addToPanel(panel, comp, gbconstraints);

        GridBagConstraints gbc = gbl.getConstraints(comp);
        assertEquals(GridBagConstraints.SOUTHEAST, gbc.anchor);
        assertEquals(5, gbc.insets.top);
        assertEquals(3, gbc.insets.left);
        assertEquals(0, gbc.insets.bottom);
        assertEquals(3, gbc.insets.right);
        assertEquals(2, gbc.gridx);
        assertEquals(4, gbc.gridy);
        assertEquals(0, gbc.weightx, 0.01);
        assertEquals(1.3, gbc.weighty, 0.01);
        assertEquals(0, gbc.ipadx);
        assertEquals(0, gbc.ipady);
        assertEquals(1, gbc.gridheight);
        assertEquals(3, gbc.gridwidth);
    }


    public void testSetAttributes() {
        GridBagConstraints gbc = new GridBagConstraints();

        gbc = GridBagUtils.setAttributes(gbc, "gridx=RELATIVE,gridy=RELATIVE");
        assertEquals(GridBagConstraints.RELATIVE, gbc.gridx);
        assertEquals(GridBagConstraints.RELATIVE, gbc.gridy);
        gbc = GridBagUtils.setAttributes(gbc, "gridx=12, gridy=34");
        assertEquals(12, gbc.gridx);
        assertEquals(34, gbc.gridy);

        gbc = GridBagUtils.setAttributes(gbc, "gridwidth=REMAINDER,gridheight=REMAINDER");
        assertEquals(GridBagConstraints.REMAINDER, gbc.gridwidth);
        assertEquals(GridBagConstraints.REMAINDER, gbc.gridheight);
        gbc = GridBagUtils.setAttributes(gbc, "gridwidth=RELATIVE,gridheight=RELATIVE");
        assertEquals(GridBagConstraints.RELATIVE, gbc.gridwidth);
        assertEquals(GridBagConstraints.RELATIVE, gbc.gridheight);
        gbc = GridBagUtils.setAttributes(gbc, "gridwidth=56, gridheight=78");
        assertEquals(56, gbc.gridwidth);
        assertEquals(78, gbc.gridheight);

        gbc = GridBagUtils.setAttributes(gbc, "weightx=0.4, weighty=0.6");
        assertEquals(0.4, gbc.weightx, 1e-12);
        assertEquals(0.6, gbc.weighty, 1e-12);

        gbc = GridBagUtils.setAttributes(gbc, "anchor=CENTER");
        assertEquals(GridBagConstraints.CENTER, gbc.anchor);
        gbc = GridBagUtils.setAttributes(gbc, "anchor=NORTH");
        assertEquals(GridBagConstraints.NORTH, gbc.anchor);
        gbc = GridBagUtils.setAttributes(gbc, "anchor=NORTHEAST");
        assertEquals(GridBagConstraints.NORTHEAST, gbc.anchor);
        gbc = GridBagUtils.setAttributes(gbc, "anchor=EAST");
        assertEquals(GridBagConstraints.EAST, gbc.anchor);
        gbc = GridBagUtils.setAttributes(gbc, "anchor=SOUTHEAST");
        assertEquals(GridBagConstraints.SOUTHEAST, gbc.anchor);
        gbc = GridBagUtils.setAttributes(gbc, "anchor=SOUTH");
        assertEquals(GridBagConstraints.SOUTH, gbc.anchor);
        gbc = GridBagUtils.setAttributes(gbc, "anchor=SOUTHWEST");
        assertEquals(GridBagConstraints.SOUTHWEST, gbc.anchor);
        gbc = GridBagUtils.setAttributes(gbc, "anchor=WEST");
        assertEquals(GridBagConstraints.WEST, gbc.anchor);
        gbc = GridBagUtils.setAttributes(gbc, "anchor=NORTHWEST");
        assertEquals(GridBagConstraints.NORTHWEST, gbc.anchor);
        gbc = GridBagUtils.setAttributes(gbc, "anchor=10");
        assertEquals(10, gbc.anchor);

        gbc = GridBagUtils.setAttributes(gbc, "fill=NONE");
        assertEquals(GridBagConstraints.NONE, gbc.fill);
        gbc = GridBagUtils.setAttributes(gbc, "fill=HORIZONTAL");
        assertEquals(GridBagConstraints.HORIZONTAL, gbc.fill);
        gbc = GridBagUtils.setAttributes(gbc, "fill=VERTICAL");
        assertEquals(GridBagConstraints.VERTICAL, gbc.fill);
        gbc = GridBagUtils.setAttributes(gbc, "fill=BOTH");
        assertEquals(GridBagConstraints.BOTH, gbc.fill);
        gbc = GridBagUtils.setAttributes(gbc, "fill=1");
        assertEquals(1, gbc.fill);

        gbc = GridBagUtils.setAttributes(gbc, "insets.bottom=10,insets.left=11,insets.right=12,insets.top=13");
        assertEquals(10, gbc.insets.bottom);
        assertEquals(11, gbc.insets.left);
        assertEquals(12, gbc.insets.right);
        assertEquals(13, gbc.insets.top);

        gbc = GridBagUtils.setAttributes(gbc, "ipadx=6,ipady=7");
        assertEquals(6, gbc.ipadx);
        assertEquals(7, gbc.ipady);

        assertEquals(12, gbc.gridx);
        assertEquals(34, gbc.gridy);
        assertEquals(56, gbc.gridwidth);
        assertEquals(78, gbc.gridheight);
        assertEquals(0.4, gbc.weightx, 1e-12);
        assertEquals(0.6, gbc.weighty, 1e-12);
        assertEquals(10, gbc.anchor);
        assertEquals(1, gbc.fill);
        assertEquals(10, gbc.insets.bottom);
        assertEquals(11, gbc.insets.left);
        assertEquals(12, gbc.insets.right);
        assertEquals(13, gbc.insets.top);
        assertEquals(6, gbc.ipadx);
        assertEquals(7, gbc.ipady);

        try {
            gbc = GridBagUtils.setAttributes(gbc, null);
        } catch (IllegalArgumentException e) {
            fail("IllegalArgumentException not expected");
        }

        try {
            gbc = GridBagUtils.setAttributes(gbc, "");
        } catch (IllegalArgumentException e) {
            fail("IllegalArgumentException not expected");
        }

        try {
            gbc = GridBagUtils.setAttributes(gbc, "ipadx");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        try {
            gbc = GridBagUtils.setAttributes(gbc, "ipadx=");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        try {
            gbc = GridBagUtils.setAttributes(gbc, "ipadx=,");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        try {
            gbc = GridBagUtils.setAttributes(gbc, "=9");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        try {
            gbc = GridBagUtils.setAttributes(gbc, "=");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }
    }
}