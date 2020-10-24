/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.analysis.rcp.toolviews.timeseries.actions;

import org.esa.s1tbx.analysis.rcp.toolviews.timeseries.GraphData;
import org.esa.s1tbx.analysis.rcp.toolviews.timeseries.TimeSeriesSettings;
import org.esa.s1tbx.analysis.rcp.toolviews.timeseries.TimeSeriesToolView;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.engine_utilities.gpf.StackUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.ModalDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.*;

public class TimeSeriesFilterAction extends AbstractAction {

    private final TimeSeriesToolView toolView;
    private final TimeSeriesSettings settings;

    public TimeSeriesFilterAction(final TimeSeriesToolView toolView, final TimeSeriesSettings settings) {
        super("exportTimeSeries");
        this.toolView = toolView;
        this.settings = settings;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        selectBands();
    }

    private void selectBands() {
        final List<Product> productList = new ArrayList<>();
        for (GraphData data : settings.getGraphDataList()) {
            productList.addAll(Arrays.asList(data.getProducts()));
        }

        final Product[] products = productList.toArray(new Product[0]);
        final Band[] allBandNames = getAvailableBands(products);
        Band[] selectedBands = getSelectedBands(products, settings.getSelectedBands());
        if (selectedBands == null) {
            selectedBands = allBandNames;
        }

        final String[] allVectors = getAllVectors(products, toolView.getCurrentProduct());

        final TimeSeriesFiltersDlg bandChooser = new TimeSeriesFiltersDlg(SnapApp.getDefault().getMainFrame(), "Time Series Filters",
                "help", allBandNames, selectedBands, allVectors, settings);
        if (bandChooser.show() == ModalDialog.ID_OK) {
            final List<String> bandNames = new ArrayList<>();
            for(Band band : bandChooser.getSelectedBands()) {
                bandNames.add(band.getName());
            }
            settings.setSelectedBands(bandNames.toArray(new String[0]));
            settings.setSelectedVectors(bandChooser.getSelectedVectors());
            settings.setVectorStatistic(bandChooser.getStatistic());
            toolView.refresh();
        }
    }

    private static Band[] getAvailableBands(final Product[] products) {
        final ArrayList<Band> availBands = new ArrayList<>(15);
        final Set<String> nameSet = new HashSet<>(15);

        for (Product prod : products) {
            final Band[] bands = prod.getBands();
            final boolean isCoreg = StackUtils.isCoregisteredStack(prod);

            for (Band band : bands) {
                String bandName = band.getName();
                if (isCoreg) {
                    bandName = getCoregBandName(band.getName());
                }
                if (!nameSet.contains(bandName)) {
                    availBands.add(band);
                    nameSet.add(bandName);
                }
            }
        }
        return availBands.toArray(new Band[0]);
    }

    private static Band[] getSelectedBands(final Product[] products, final String[] selectedBandNames) {
        final ArrayList<Band> selectedBands = new ArrayList<>(15);
        if(selectedBandNames != null && selectedBandNames.length > 0) {
            final Set<String> nameSet = new HashSet<>(15);

            for (Product prod : products) {
                final Band[] bands = prod.getBands();
                final boolean isCoreg = StackUtils.isCoregisteredStack(prod);

                for (Band band : bands) {
                    String bandName = band.getName();
                    if (isCoreg) {
                        bandName = getCoregBandName(band.getName());
                    }
                    if (!nameSet.contains(bandName)) {
                        for(String selBandName : selectedBandNames) {
                            if (isCoreg) {
                                selBandName = getCoregBandName(selBandName);
                                if (bandName.contains(selBandName)) {
                                    selectedBands.add(band);
                                }
                            } else {
                                if (bandName.equals(selBandName)) {
                                    selectedBands.add(band);
                                }
                            }
                        }
                        nameSet.add(bandName);
                    }
                }
            }
        }
        return selectedBands.toArray(new Band[0]);
    }

    private String[] getAllPins(final Product[] products, final Product currentProduct) {
        final List<String> allVectorNames = new ArrayList<>();
        if(currentProduct != null) {
            final ProductNodeGroup<VectorDataNode> vectorNodeGroup = currentProduct.getVectorDataGroup();
            if (vectorNodeGroup != null) {
                final String[] vectorNames = vectorNodeGroup.getNodeNames();
                for (String name : vectorNames) {
                    if (!name.equals("pins") && !name.equals("ground_control_points")) {
                        allVectorNames.add(name);
                    }
                }
            }
        } else {
            for(Product product : products) {
                final ProductNodeGroup<VectorDataNode> vectorNodeGroup = product.getVectorDataGroup();
                if (vectorNodeGroup != null) {
                    final String[] vectorNames = vectorNodeGroup.getNodeNames();
                    for (String name : vectorNames) {
                        if (!name.equals("pins") && !name.equals("ground_control_points")) {
                            allVectorNames.add(name);
                        }
                    }
                }
                if(!allVectorNames.isEmpty()) {
                    break;
                }
            }
        }
        return allVectorNames.toArray(new String[0]);
    }

    private String[] getAllVectors(final Product[] products, final Product currentProduct) {
        final List<String> allVectorNames = new ArrayList<>();
        if(currentProduct != null) {
            final ProductNodeGroup<VectorDataNode> vectorNodeGroup = currentProduct.getVectorDataGroup();
            if (vectorNodeGroup != null) {
                final String[] vectorNames = vectorNodeGroup.getNodeNames();
                for (String name : vectorNames) {
                    if (!name.equals("pins") && !name.equals("ground_control_points")) {
                        allVectorNames.add(name);
                    }
                }
            }
        } else {
            for(Product product : products) {
                final ProductNodeGroup<VectorDataNode> vectorNodeGroup = product.getVectorDataGroup();
                if (vectorNodeGroup != null) {
                    final String[] vectorNames = vectorNodeGroup.getNodeNames();
                    for (String name : vectorNames) {
                        if (!name.equals("pins") && !name.equals("ground_control_points")) {
                            allVectorNames.add(name);
                        }
                    }
                }
                if(!allVectorNames.isEmpty()) {
                    break;
                }
            }
        }
        return allVectorNames.toArray(new String[0]);
    }

    public static String getCoregBandName(final String bandName) {
        int suffixLoc = bandName.indexOf(StackUtils.MST);
        if (suffixLoc < 0) {
            suffixLoc = bandName.indexOf(StackUtils.SLV);
        }
        if (suffixLoc < 0) {
            suffixLoc = bandName.lastIndexOf('_');
        }
        return bandName.substring(0, suffixLoc);
    }
}