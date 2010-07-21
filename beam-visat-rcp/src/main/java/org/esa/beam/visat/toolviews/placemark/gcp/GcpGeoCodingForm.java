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

package org.esa.beam.visat.toolviews.placemark.gcp;

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.GcpGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.PlacemarkGroup;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.util.Debug;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingWorker;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * GCP geo-coding form.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
class GcpGeoCodingForm extends JPanel {

    private JTextField methodTextField;
    private JTextField rmseLatTextField;
    private JTextField rmseLonTextField;

    private JComboBox methodComboBox;
    private JToggleButton attachButton;
    private JTextField warningLabel;

    private Product currentProduct;
    private Format rmseNumberFormat;
    private GcpGroupListener currentGcpGroupListener;

    public GcpGeoCodingForm() {
        rmseNumberFormat = new RmseNumberFormat();
        currentGcpGroupListener = new GcpGroupListener();
        initComponents();
    }

    private void initComponents() {
        TableLayout layout = new TableLayout(2);
        this.setLayout(layout);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableWeightY(1.0);
        layout.setTableFill(TableLayout.Fill.BOTH);
        layout.setTablePadding(2, 2);
        layout.setColumnWeightX(0, 0.5);
        layout.setColumnWeightX(1, 0.5);

        add(createInfoPanel());
        add(createAttachDetachPanel());

        updateUIState();
    }

    private JPanel createInfoPanel() {
        TableLayout layout = new TableLayout(2);
        layout.setTablePadding(2, 4);
        layout.setColumnWeightX(0, 0.0);
        layout.setColumnWeightX(1, 1.0);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.BOTH);

        JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder("Current GCP Geo-Coding"));
        panel.add(new JLabel("Method:"));
        methodTextField = new JTextField();
        setComponentName(methodTextField, "methodTextField");
        methodTextField.setEditable(false);
        methodTextField.setHorizontalAlignment(JLabel.TRAILING);
        panel.add(methodTextField);
        rmseLatTextField = new JTextField();
        setComponentName(rmseLatTextField, "rmseLatTextField");
        rmseLatTextField.setEditable(false);
        rmseLatTextField.setHorizontalAlignment(JLabel.TRAILING);
        panel.add(new JLabel("RMSE Lat:"));
        panel.add(rmseLatTextField);

        rmseLonTextField = new JTextField();
        setComponentName(rmseLonTextField, "rmseLonTextField");
        rmseLonTextField.setEditable(false);
        rmseLonTextField.setHorizontalAlignment(JLabel.TRAILING);
        panel.add(new JLabel("RMSE Lon:"));
        panel.add(rmseLonTextField);
        return panel;
    }

    private JPanel createAttachDetachPanel() {
        methodComboBox = new JComboBox(GcpGeoCoding.Method.values());
        setComponentName(methodComboBox, "methodComboBox");
        methodComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateUIState();
            }
        });
        attachButton = new JToggleButton();
        setComponentName(attachButton, "attachButton");
        attachButton.setName("attachButton");

        AbstractAction attachDetachAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

                if (!(currentProduct.getGeoCoding() instanceof GcpGeoCoding)) {
                    attachGeoCoding(currentProduct);
                } else {
                    detachGeoCoding(currentProduct);
                }
            }
        };

        attachButton.setAction(attachDetachAction);
        attachButton.setHideActionText(true);
        warningLabel = new JTextField();
        warningLabel.setEditable(false);

        TableLayout layout = new TableLayout(2);
        layout.setTablePadding(2, 4);
        layout.setColumnWeightX(0, 0.0);
        layout.setColumnWeightX(1, 1.0);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.BOTH);
        layout.setCellColspan(2, 0, 2);
        layout.setCellFill(2, 0, TableLayout.Fill.VERTICAL);
        layout.setCellAnchor(2, 0, TableLayout.Anchor.CENTER);

        JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder("Attach / Detach GCP Geo-Coding"));
        panel.add(new JLabel("Method:"));
        panel.add(methodComboBox);
        panel.add(new JLabel("Status:"));
        panel.add(warningLabel);
        panel.add(attachButton);

        return panel;
    }

    void updateUIState() {
        if (currentProduct != null && currentProduct.getGeoCoding() instanceof GcpGeoCoding) {
            final GcpGeoCoding gcpGeoCoding = (GcpGeoCoding) currentProduct.getGeoCoding();

            rmseLatTextField.setText(rmseNumberFormat.format(gcpGeoCoding.getRmseLat()));
            rmseLonTextField.setText(rmseNumberFormat.format(gcpGeoCoding.getRmseLon()));
            methodTextField.setText(gcpGeoCoding.getMethod().getName());
            methodComboBox.setSelectedItem(gcpGeoCoding.getMethod());

            methodComboBox.setEnabled(false);
            attachButton.setText("Detach");
            attachButton.setSelected(true);
            attachButton.setEnabled(true);
            warningLabel.setText("GCP geo-coding attached");
            warningLabel.setForeground(Color.BLACK);
        } else {
            methodComboBox.setEnabled(true);
            methodTextField.setText("n/a");
            rmseLatTextField.setText(rmseNumberFormat.format(Double.NaN));
            rmseLonTextField.setText(rmseNumberFormat.format(Double.NaN));
            attachButton.setText("Attach");
            attachButton.setSelected(false);
            updateAttachButtonAndStatus();
        }
    }

    private void updateAttachButtonAndStatus() {
        final GcpGeoCoding.Method method = (GcpGeoCoding.Method) methodComboBox.getSelectedItem();
        if (currentProduct != null && getValidGcpCount(currentProduct.getGcpGroup()) >= method.getTermCountP()) {
            attachButton.setEnabled(true);
            warningLabel.setText("OK, enough GCPs for selected method");
            warningLabel.setForeground(Color.GREEN.darker());
        } else {
            attachButton.setEnabled(false);
            warningLabel.setText("Not enough (valid) GCPs for selected method");
            warningLabel.setForeground(Color.RED.darker());
        }
    }

    private void detachGeoCoding(Product product) {
        if (product.getGeoCoding() instanceof GcpGeoCoding) {
            GeoCoding gc = ((GcpGeoCoding) product.getGeoCoding()).getOriginalGeoCoding();
            product.setGeoCoding(gc);
        }
        updateUIState();
    }

    private void attachGeoCoding(final Product product) {
        final GcpGeoCoding.Method method = (GcpGeoCoding.Method) methodComboBox.getSelectedItem();
        final Placemark[] gcps = getValidGcps(product.getGcpGroup());
        final GeoCoding geoCoding = product.getGeoCoding();
        final Datum datum;
        if (geoCoding == null) {
            datum = Datum.WGS_84;
        } else {
            datum = geoCoding.getDatum();
        }

        SwingWorker sw = new SwingWorker<GcpGeoCoding, GcpGeoCoding>() {
            @Override
            protected GcpGeoCoding doInBackground() throws Exception {
                GcpGeoCoding gcpGeoCoding = new GcpGeoCoding(method, gcps,
                                                             product.getSceneRasterWidth(),
                                                             product.getSceneRasterHeight(),
                                                             datum);
                gcpGeoCoding.setOriginalGeoCoding(product.getGeoCoding());
                return gcpGeoCoding;
            }

            @Override
            protected void done() {
                final GcpGeoCoding gcpGeoCoding;
                try {
                    gcpGeoCoding = get();
                    product.setGeoCoding(gcpGeoCoding);
                    updateUIState();
                } catch (InterruptedException e) {
                    Debug.trace(e);
                } catch (ExecutionException e) {
                    Debug.trace(e.getCause());
                }
            }
        };
        sw.execute();
    }

    public void setProduct(Product product) {
        if (product == currentProduct) {
            return;
        }
        if (currentProduct != null) {
            currentProduct.removeProductNodeListener(currentGcpGroupListener);
        }
        currentProduct = product;
        if (currentProduct != null) {
            currentProduct.addProductNodeListener(currentGcpGroupListener);
        }
    }

    private void setComponentName(JComponent component, String name) {
        component.setName(getClass().getName() + name);
    }

    private static Placemark[] getValidGcps(ProductNodeGroup<Placemark> gcpGroup) {
        final List<Placemark> gcpList = new ArrayList<Placemark>(gcpGroup.getNodeCount());
        for (int i = 0; i < gcpGroup.getNodeCount(); i++) {
            final Placemark p = gcpGroup.get(i);
            final PixelPos pixelPos = p.getPixelPos();
            final GeoPos geoPos = p.getGeoPos();
            if (pixelPos != null && pixelPos.isValid() && geoPos != null && geoPos.isValid()) {
                gcpList.add(p);
            }
        }
        return gcpList.toArray(new Placemark[gcpList.size()]);
    }

    private static int getValidGcpCount(PlacemarkGroup gcpGroup) {
        int count = 0;
        for (int i = 0; i < gcpGroup.getNodeCount(); i++) {
            final Placemark p = gcpGroup.get(i);
            if (isValid(p)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isValid(Placemark p) {
        final PixelPos pixelPos = p.getPixelPos();
        final GeoPos geoPos = p.getGeoPos();
        return pixelPos != null && pixelPos.isValid() && geoPos != null && geoPos.isValid();
    }

    private static class RmseNumberFormat extends NumberFormat {

        DecimalFormat format = new DecimalFormat("0.0####");

        @Override
        public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
            if (Double.isNaN(number)) {
                return toAppendTo.append("n/a");
            } else {
                return format.format(number, toAppendTo, pos);
            }
        }

        @Override
        public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
            return format.format(number, toAppendTo, pos);
        }

        @Override
        public Number parse(String source, ParsePosition parsePosition) {
            return format.parse(source, parsePosition);
        }
    }

    private class GcpGroupListener implements ProductNodeListener {

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            // exclude geo-coding changes to prevent recursion
            if (Product.PROPERTY_NAME_GEOCODING.equals(event.getPropertyName())) {
                return;
            }
            final ProductNode sourceNode = event.getSourceNode();
            if (sourceNode instanceof Placemark) {
                if (currentProduct.getGcpGroup().contains((Placemark) sourceNode)) {
                    updateGcpGeoCoding();
                }
            }
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            nodeChanged(event);
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            if (event.getGroup() == currentProduct.getGcpGroup()) {
                updateGcpGeoCoding();
            }
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            if (event.getGroup() == currentProduct.getGcpGroup()) {
                updateGcpGeoCoding();
            }
        }

        private void updateGcpGeoCoding() {
            final GeoCoding geoCoding = currentProduct.getGeoCoding();
            if (geoCoding instanceof GcpGeoCoding) {
                final GcpGeoCoding gcpGeoCoding = ((GcpGeoCoding) geoCoding);
                final PlacemarkGroup gcpGroup = currentProduct.getGcpGroup();
                final int gcpCount = gcpGroup.getNodeCount();
                if (gcpCount < gcpGeoCoding.getMethod().getTermCountP()) {
                    detachGeoCoding(currentProduct);
                } else {
                    gcpGeoCoding.setGcps(gcpGroup.toArray(new Placemark[gcpCount]));
                    currentProduct.fireProductNodeChanged(Product.PROPERTY_NAME_GEOCODING);
                    updateUIState();
                }
            }
        }
    }
}
