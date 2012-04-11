/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.visat.toolviews.stat;

import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.jfree.chart.ChartPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Norman Fomferra
 */
abstract class MaskSelectionToolSupport implements PlotAreaSelectionTool.Action {
    private final PagePanel pagePanel;
    private final ChartPanel chartPanel;
    private final String maskName;
    private final String maskDescription;
    private final Color maskColor;

    PlotAreaSelectionTool plotAreaSelectionTool;

    protected MaskSelectionToolSupport(PagePanel pagePanel, ChartPanel chartPanel, String maskName, String maskDescription, Color maskColor) {
        this.pagePanel = pagePanel;
        this.chartPanel = chartPanel;
        this.maskName = maskName;
        this.maskDescription = maskDescription;
        this.maskColor = maskColor;
    }

    public JCheckBoxMenuItem createMenuItem(final PlotAreaSelectionTool.AreaType areaType) {
        final JCheckBoxMenuItem maskSelectionMenuItem = new JCheckBoxMenuItem("Mask Selection Mode");
        maskSelectionMenuItem.setName("maskSelectionMode");
        maskSelectionMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (maskSelectionMenuItem.isSelected()) {
                    if (plotAreaSelectionTool == null) {
                        plotAreaSelectionTool = new PlotAreaSelectionTool(chartPanel, MaskSelectionToolSupport.this);
                        plotAreaSelectionTool.setAreaType(areaType);
                        plotAreaSelectionTool.install();
                    }
                } else {
                    if (plotAreaSelectionTool != null) {
                        plotAreaSelectionTool.uninstall();
                        plotAreaSelectionTool = null;
                    }
                }
            }
        });
        return maskSelectionMenuItem;
    }

    @Override
    public void areaSelected(PlotAreaSelectionTool.AreaType areaType, double x0, double y0, double dx, double dy) {
        Product product = pagePanel.getProduct();
        if (product == null) {
            return;
        }

        String expression = createMaskExpression(areaType, x0, y0, dx, dy);

        final Mask magicWandMask = product.getMaskGroup().get(maskName);
        if (magicWandMask != null) {
            magicWandMask.getImageConfig().setValue("expression", expression);
        } else {
            final int width = product.getSceneRasterWidth();
            final int height = product.getSceneRasterHeight();
            product.getMaskGroup().add(Mask.BandMathsType.create(maskName,
                                                                 maskDescription,
                                                                 width, height,
                                                                 expression,
                                                                 maskColor, 0.5));
        }


    }

    protected abstract String createMaskExpression(PlotAreaSelectionTool.AreaType areaType, double x0, double y0, double dx, double dy);
}
