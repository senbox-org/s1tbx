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

import org.esa.beam.framework.param.ParamProperties;
import org.esa.beam.framework.param.Parameter;
import org.junit.Test;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JInternalFrame;
import javax.swing.JSpinner;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Panel;
import java.awt.Rectangle;

import static org.junit.Assert.*;

public class UIUtilsTest {

    @Test
    public void testCenterComponent() {
        try {
            Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
            Component comp = new Panel();
            Component alignComp = new Panel();

            comp.setBounds(0, 0, 100, 100);
            UIUtils.centerComponent(comp);
            assertEquals(comp.getBounds(), new Rectangle(screenSize.width / 2 - 50, screenSize.height / 2 - 50, 100, 100));

            comp.setBounds(0, 0, 100, 100);
            alignComp.setBounds(100, 100, 200, 200);
            UIUtils.centerComponent(comp, alignComp);
            assertEquals(comp.getBounds(), new Rectangle(150, 150, 100, 100));
        } catch (HeadlessException e) {
            warnHeadless();
        }
    }

    @Test
    public void testGetScreenSize() {
        try {
            Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
            assertNotNull(UIUtils.getScreenSize());
            assertEquals(UIUtils.getScreenSize(), screenSize);
        } catch (HeadlessException e) {
            warnHeadless();
        }
    }

    @Test
    public void testGetScreenWidth() {
        try {
            Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
            assertEquals(UIUtils.getScreenWidth(), screenSize.width);
        } catch (HeadlessException e) {
            warnHeadless();
        }
    }

    @Test
    public void testGetScreenHeight() {
        try {
            Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
            assertEquals(UIUtils.getScreenHeight(), screenSize.height);
        } catch (HeadlessException e) {
            warnHeadless();
        }
    }


    @Test
    public void testGetUniqueFrameTitle() {
        String title;

        title = UIUtils.getUniqueFrameTitle(
                new JInternalFrame[]{
                        new JInternalFrame("Image1"),
                        new JInternalFrame("Image2"),
                        new JInternalFrame("Image3"),
                }, "Image");
        assertEquals("Image", title);

        title = UIUtils.getUniqueFrameTitle(
                new JInternalFrame[]{
                        new JInternalFrame("Image"),
                        new JInternalFrame("Data"),
                        new JInternalFrame("Raw"),
                }, "Image");
        assertEquals("Image (2)", title);

        title = UIUtils.getUniqueFrameTitle(
                new JInternalFrame[]{
                        new JInternalFrame("Image (3)"),
                        new JInternalFrame("Data"),
                        new JInternalFrame("Raw"),
                }, "Image");
        assertEquals("Image", title);

        title = UIUtils.getUniqueFrameTitle(
                new JInternalFrame[]{
                        new JInternalFrame("Image"),
                        new JInternalFrame("Image (2)"),
                        new JInternalFrame("Image (3)"),
                }, "Image");
        assertEquals("Image (4)", title);

        title = UIUtils.getUniqueFrameTitle(
                new JInternalFrame[]{
                        new JInternalFrame("Image"),
                        new JInternalFrame("Image (2)"),
                        new JInternalFrame("Image (4)"),
                }, "Image");
        assertEquals("Image (3)", title);

        title = UIUtils.getUniqueFrameTitle(
                new JInternalFrame[]{
                        new JInternalFrame("Image"),
                        new JInternalFrame("Image (1)"),
                        new JInternalFrame("Image (2)"),
                        new JInternalFrame("Image (3)"),
                }, "Image");
        assertEquals("Image (4)", title);
    }

    @Test
    public void testCreateSpinner_WithParameter() {
        final String labelname = "paramLabel";
        final ParamProperties properties = new ParamProperties(Integer.class, Integer.valueOf(3));
        properties.setLabel(labelname);
        final Parameter parameter = new Parameter("paramName", properties);

        final JSpinner spinner = UIUtils.createSpinner(parameter, Integer.valueOf(10), "#0");
        assertEquals(labelname, spinner.getName());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSelectProfile() throws Exception {
        DefaultComboBoxModel profileModel = new DefaultComboBoxModel(new String[]{
                "wrong_name_1",
                "this_is_the_correct_name",
                "wrong_name_2",
                "wrong_name_3"
        });
        UIUtils.selectProfileThatContains("correct_name", profileModel);
        assertEquals("this_is_the_correct_name", profileModel.getSelectedItem());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSelectProfile_NotFound() throws Exception {
        DefaultComboBoxModel profileModel = new DefaultComboBoxModel(new String[]{
                "wrong_name_1",
                "wrong_name_2",
                "wrong_name_3"
        });
        UIUtils.selectProfileThatContains("i_am_not_included", profileModel);
        assertFalse("i_am_not_included".equals(profileModel.getSelectedItem()));
    }

    private void warnHeadless() {
        System.out.println("A " + UIUtilsTest.class + " test has not been performed: HeadlessException");
    }

}
