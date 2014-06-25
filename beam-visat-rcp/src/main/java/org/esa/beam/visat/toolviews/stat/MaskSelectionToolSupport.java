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
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.jfree.chart.ChartPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Norman Fomferra
 */
public abstract class MaskSelectionToolSupport implements PlotAreaSelectionTool.Action {
    private final PagePanel pagePanel;
    private final ChartPanel chartPanel;
    private final String maskName;
    private final String maskDescription;
    private final Color maskColor;
    private final PlotAreaSelectionTool.AreaType areaType;

    private PlotAreaSelectionTool plotAreaSelectionTool;

    protected MaskSelectionToolSupport(PagePanel pagePanel, ChartPanel chartPanel, String maskName, String maskDescription, Color maskColor, PlotAreaSelectionTool.AreaType areaType) {
        this.pagePanel = pagePanel;
        this.chartPanel = chartPanel;
        this.maskName = maskName;
        this.maskDescription = maskDescription;
        this.maskColor = maskColor;
        this.areaType = areaType;
    }

    public JCheckBoxMenuItem createMaskSelectionModeMenuItem() {
        final JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(String.format("Select Mask '%s'", maskName));
        menuItem.setName("maskSelectionMode");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (menuItem.isSelected()) {
                    if (plotAreaSelectionTool == null) {
                        plotAreaSelectionTool = new PlotAreaSelectionTool(chartPanel, MaskSelectionToolSupport.this);
                        plotAreaSelectionTool.setAreaType(areaType);
                        plotAreaSelectionTool.setFillPaint(createAlphaColor(maskColor, 50));
                    }
                    plotAreaSelectionTool.install();
                } else {
                    if (plotAreaSelectionTool != null) {
                        plotAreaSelectionTool.uninstall();
                    }
                }
            }
        });
        return menuItem;
    }

    public JMenuItem createDeleteMaskMenuItem() {
        final JMenuItem menuItem = new JMenuItem(String.format("Delete Mask '%s' ", maskName));
        menuItem.setName("deleteMask");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (plotAreaSelectionTool != null) {
                    plotAreaSelectionTool.removeAnnotation();
                }
                Product product = pagePanel.getProduct();
                if (product != null) {
                    Mask mask = product.getMaskGroup().get(maskName);
                    if (mask != null) {
                        product.getMaskGroup().remove(mask);
                    }
                }
            }
        });
        return menuItem;
    }

    @Override
    public void areaSelected(PlotAreaSelectionTool.AreaType areaType, Shape shape) {

        Product product = pagePanel.getProduct();
        if (product == null) {
            return;
        }

        String expression = createMaskExpression(areaType, shape);

        Mask mask = product.getMaskGroup().get(maskName);
        if (mask != null) {
            mask.getImageConfig().setValue("expression", expression);
        } else {
            mask = product.addMask(maskName, expression, maskDescription, maskColor, 0.5);
        }

        RasterDataNode raster = pagePanel.getRaster();
        if (raster != null) {
            ProductNodeGroup<Mask> overlayMaskGroup = raster.getOverlayMaskGroup();
            if (!overlayMaskGroup.contains(mask)) {
                overlayMaskGroup.add(mask);
            }
        }
    }

    protected abstract String createMaskExpression(PlotAreaSelectionTool.AreaType areaType, Shape shape);

    private static Color createAlphaColor(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}
