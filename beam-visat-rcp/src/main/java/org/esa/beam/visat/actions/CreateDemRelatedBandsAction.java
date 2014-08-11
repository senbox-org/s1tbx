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

package org.esa.beam.visat.actions;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.ComponentAdapter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.dataop.dem.ElevationModel;
import org.esa.beam.framework.dataop.dem.ElevationModelDescriptor;
import org.esa.beam.framework.dataop.dem.ElevationModelRegistry;
import org.esa.beam.framework.dataop.dem.Orthorectifier;
import org.esa.beam.framework.dataop.dem.Orthorectifier2;
import org.esa.beam.framework.dataop.resamp.Resampling;
import org.esa.beam.framework.dataop.resamp.ResamplingFactory;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.RasterDataNodeSampleOpImage;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.Insets;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class CreateDemRelatedBandsAction extends ExecCommand {

    public static final String DIALOG_TITLE = "Create Elevation Band";
    public static final String DEFAULT_ELEVATION_BAND_NAME = "elevation";
    public static final String DEFAULT_LATITUDE_BAND_NAME = "corr_latitude";
    public static final String DEFAULT_LONGITUDE_BAND_NAME = "corr_longitude";

    @Override
    public void actionPerformed(CommandEvent event) {
        final Product product = VisatApp.getApp().getSelectedProduct();
        final DialogData dialogData = requestDialogData(product);
        if (dialogData == null) {
            return;
        }

        final ElevationModelRegistry elevationModelRegistry = ElevationModelRegistry.getInstance();
        final ElevationModelDescriptor demDescriptor = elevationModelRegistry.getDescriptor(dialogData.demName);
        if (demDescriptor == null) {
            VisatApp.getApp().showErrorDialog(DIALOG_TITLE, "The DEM '" + dialogData.demName + "' is not supported.");
            return;
        }
        if (demDescriptor.isInstallingDem()) {
            VisatApp.getApp().showErrorDialog(DIALOG_TITLE,
                                              "The DEM '" + dialogData.demName + "' is currently being installed.");
            return;
        }
        if (!demDescriptor.isDemInstalled()) {
            demDescriptor.installDemFiles(VisatApp.getApp().getMainFrame());
            return;
        }

        Resampling resampling = Resampling.BILINEAR_INTERPOLATION;
        if(dialogData.resamplingMethod != null) {
            resampling = ResamplingFactory.createResampling(dialogData.resamplingMethod);
        }

        computeBands(product,
                     demDescriptor,
                     dialogData.outputElevationBand ? dialogData.elevationBandName : null,
                     resampling,
                     dialogData.outputDemCorrectedBands ? dialogData.latitudeBandName : null,
                     dialogData.outputDemCorrectedBands ? dialogData.longitudeBandName : null);
    }

    @Override
    public void updateState(CommandEvent event) {
        final Product product = VisatApp.getApp().getSelectedProduct();
        setEnabled(product != null && product.getGeoCoding() != null);
    }

    private void computeBands(final Product product,
                              final ElevationModelDescriptor demDescriptor,
                              final String elevationBandName,
                              final Resampling resampling,
                              final String latitudeBandName,
                              final String longitudeBandName) {

        final ElevationModel dem = demDescriptor.createDem(resampling);
        if (elevationBandName != null) {
            addElevationBand(product, dem, elevationBandName);
        }
        if (latitudeBandName != null && longitudeBandName != null) {
            addGeoPosBands(product, dem, latitudeBandName, longitudeBandName);
        }
    }

    private static void addGeoPosBands(Product product, ElevationModel dem, String latitudeBandName, String longitudeBandName) {
        final int width = product.getSceneRasterWidth();
        final int height = product.getSceneRasterHeight();
        final Orthorectifier orthorectifier = new Orthorectifier2(width, height, product.getBandAt(0).getPointing(), dem, 25);

        final Band latitudeBand = product.addBand(latitudeBandName, ProductData.TYPE_FLOAT32);
        latitudeBand.setSynthetic(true);
        latitudeBand.setNoDataValue(Double.NaN);
        latitudeBand.setUnit("deg");
        latitudeBand.setDescription("DEM-corrected latitude");
        latitudeBand.setSourceImage(createLatitudeSourceImage(orthorectifier, latitudeBand));

        final Band longitudeBand = product.addBand(longitudeBandName, ProductData.TYPE_FLOAT32);
        longitudeBand.setSynthetic(true);
        longitudeBand.setNoDataValue(Double.NaN);
        longitudeBand.setUnit("deg");
        longitudeBand.setDescription("DEM-corrected longitude");
        longitudeBand.setSourceImage(createLongitudeSourceImage(orthorectifier, longitudeBand));
    }

    private static void addElevationBand(Product product, ElevationModel dem, String elevationBandName) {
        final GeoCoding geoCoding = product.getGeoCoding();
        ElevationModelDescriptor demDescriptor = dem.getDescriptor();
        final float noDataValue = dem.getDescriptor().getNoDataValue();
        final Band elevationBand = product.addBand(elevationBandName, ProductData.TYPE_FLOAT32);
        elevationBand.setSynthetic(true);
        elevationBand.setNoDataValue(noDataValue);
        elevationBand.setUnit("m");
        elevationBand.setDescription(demDescriptor.getName());
        elevationBand.setSourceImage(createElevationSourceImage(dem, geoCoding, elevationBand));
    }

    private static RenderedImage createElevationSourceImage(final ElevationModel dem, final GeoCoding geoCoding, final Band band) {
        return new DefaultMultiLevelImage(new AbstractMultiLevelSource(ImageManager.getMultiLevelModel(band)) {
            @Override
            protected RenderedImage createImage(final int level) {
                return new ElevationSourceImage(dem, geoCoding, band, ResolutionLevel.create(getModel(), level));
            }
        });
    }

    private static DefaultMultiLevelImage createLongitudeSourceImage(final Orthorectifier orthorectifier, final Band band) {
        return new DefaultMultiLevelImage(new AbstractMultiLevelSource(ImageManager.getMultiLevelModel(band)) {
            @Override
            protected RenderedImage createImage(final int level) {
                return new LongitudeSourceImage(orthorectifier, band, ResolutionLevel.create(getModel(), level));
            }
        });
    }

    private static DefaultMultiLevelImage createLatitudeSourceImage(final Orthorectifier orthorectifier, final Band band) {
        return new DefaultMultiLevelImage(new AbstractMultiLevelSource(ImageManager.getMultiLevelModel(band)) {
            @Override
            protected RenderedImage createImage(final int level) {
                return new LatitudeSourceImage(orthorectifier, band, ResolutionLevel.create(getModel(), level));
            }
        });
    }

    private static boolean isOrtorectifiable(Product product) {
        return product.getNumBands() > 0 && product.getBandAt(0).canBeOrthorectified();
    }

    private DialogData requestDialogData(final Product product) {

        boolean ortorectifiable = isOrtorectifiable(product);

        final ElevationModelDescriptor[] descriptors = ElevationModelRegistry.getInstance().getAllDescriptors();
        String[] demNames = new String[descriptors.length];
        for (int i = 0; i < descriptors.length; i++) {
            demNames[i] = descriptors[i].getName();
        }
        // sort the list
        final List<String> sortedDEMNames = Arrays.asList(demNames);
        java.util.Collections.sort(sortedDEMNames);
        demNames = sortedDEMNames.toArray(new String[sortedDEMNames.size()]);

        final DialogData dialogData = new DialogData("SRTM 3sec", ResamplingFactory.BILINEAR_INTERPOLATION_NAME, ortorectifiable);
        PropertySet propertySet = PropertyContainer.createObjectBacked(dialogData);
        configureDemNameProperty(propertySet, "demName", demNames, "SRTM 3sec");
        configureDemNameProperty(propertySet, "resamplingMethod", ResamplingFactory.resamplingNames,
                                 ResamplingFactory.BILINEAR_INTERPOLATION_NAME);
        configureBandNameProperty(propertySet, "elevationBandName", product);
        configureBandNameProperty(propertySet, "latitudeBandName", product);
        configureBandNameProperty(propertySet, "longitudeBandName", product);
        final BindingContext ctx = new BindingContext(propertySet);

        JList demList = new JList();
        demList.setVisibleRowCount(7);
        ctx.bind("demName", new SingleSelectionListComponentAdapter(demList));

        JTextField elevationBandNameField = new JTextField();
        elevationBandNameField.setColumns(10);
        ctx.bind("elevationBandName", elevationBandNameField);

        JCheckBox outputDemCorrectedBandsChecker = new JCheckBox("Output DEM-corrected bands");
        ctx.bind("outputDemCorrectedBands", outputDemCorrectedBandsChecker);

        JLabel latitudeBandNameLabel = new JLabel("Latitude band name:");
        JTextField latitudeBandNameField = new JTextField();
        latitudeBandNameField.setEnabled(ortorectifiable);
        ctx.bind("latitudeBandName", latitudeBandNameField).addComponent(latitudeBandNameLabel);
        ctx.bindEnabledState("latitudeBandName", true, "outputGeoCodingBands", true);

        JLabel longitudeBandNameLabel = new JLabel("Longitude band name:");
        JTextField longitudeBandNameField = new JTextField();
        longitudeBandNameField.setEnabled(ortorectifiable);
        ctx.bind("longitudeBandName", longitudeBandNameField).addComponent(longitudeBandNameLabel);
        ctx.bindEnabledState("longitudeBandName", true, "outputGeoCodingBands", true);

        TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setCellColspan(0, 0, 2);
        tableLayout.setCellColspan(1, 0, 2);
      /*  tableLayout.setCellColspan(3, 0, 2);
        tableLayout.setCellWeightX(0, 0, 1.0);
        tableLayout.setRowWeightX(1, 1.0);
        tableLayout.setCellWeightX(2, 1, 1.0);
        tableLayout.setCellWeightX(4, 1, 1.0);
        tableLayout.setCellWeightX(5, 1, 1.0);
        tableLayout.setCellPadding(4, 0, new Insets(0, 24, 0, 4));
        tableLayout.setCellPadding(5, 0, new Insets(0, 24, 0, 4));   */

        JPanel parameterPanel = new JPanel(tableLayout);
        /*row 0*/
        parameterPanel.add(new JLabel("Digital elevation model (DEM):"));
        parameterPanel.add(new JScrollPane(demList));
        /*row 1*/
        parameterPanel.add(new JLabel("Resampling method:"));
        final JComboBox resamplingCombo = new JComboBox(ResamplingFactory.resamplingNames);
        parameterPanel.add(resamplingCombo);
        ctx.bind("resamplingMethod", resamplingCombo);

        parameterPanel.add(new JLabel("Elevation band name:"));
        parameterPanel.add(elevationBandNameField);
        if(ortorectifiable) {
            /*row 2*/
            parameterPanel.add(outputDemCorrectedBandsChecker);
            /*row 3*/
            parameterPanel.add(latitudeBandNameLabel);
            parameterPanel.add(latitudeBandNameField);
            /*row 4*/
            parameterPanel.add(longitudeBandNameLabel);
            parameterPanel.add(longitudeBandNameField);

            outputDemCorrectedBandsChecker.setSelected(ortorectifiable);
            outputDemCorrectedBandsChecker.setEnabled(ortorectifiable);
        }

        final ModalDialog dialog = new ModalDialog(VisatApp.getApp().getMainFrame(), DIALOG_TITLE, ModalDialog.ID_OK_CANCEL, getHelpId());
        dialog.setContent(parameterPanel);
        if (dialog.show() == ModalDialog.ID_OK) {
            return dialogData;
        }

        return null;
    }

    private static void configureDemNameProperty(PropertySet propertySet, String propertyName, String[] demNames, String defaultValue) {
        PropertyDescriptor descriptor = propertySet.getProperty(propertyName).getDescriptor();
        descriptor.setValueSet(new ValueSet(demNames));
        descriptor.setDefaultValue(defaultValue);
        descriptor.setNotNull(true);
        descriptor.setNotEmpty(true);
    }

    private static void configureBandNameProperty(PropertySet propertySet, String propertyName, Product product) {
        Property property = propertySet.getProperty(propertyName);
        PropertyDescriptor descriptor = property.getDescriptor();
        descriptor.setNotNull(true);
        descriptor.setNotEmpty(true);
        descriptor.setValidator(new BandNameValidator(product));
        setValidBandName(property, product);
    }

    private static void setValidBandName(Property property, Product product) {
        String bandName = (String) property.getValue();
        String bandNameStub = bandName;
        for (int i = 2; product.containsBand(bandName); i++) {
            bandName = String.format("%s_%d", bandNameStub, i);
        }
        try {
            property.setValue(bandName);
        } catch (ValidationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static class SingleSelectionListComponentAdapter extends ComponentAdapter implements ListSelectionListener, PropertyChangeListener {

        private final JList list;

        public SingleSelectionListComponentAdapter(JList list) {
            this.list = list;
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }

        @Override
        public JComponent[] getComponents() {
            return new JComponent[]{list};
        }

        @Override
        public void bindComponents() {
            updateListModel();
            getPropertyDescriptor().addAttributeChangeListener(this);
            list.addListSelectionListener(this);
        }

        @Override
        public void unbindComponents() {
            getPropertyDescriptor().removeAttributeChangeListener(this);
            list.removeListSelectionListener(this);
        }

        @Override
        public void adjustComponents() {
            Object value = getBinding().getPropertyValue();
            if (value != null) {
                list.setSelectedValue(value, true);
            } else {
                list.clearSelection();
            }
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getSource() == getPropertyDescriptor() && evt.getPropertyName().equals("valueSet")) {
                updateListModel();
            }
        }

        private PropertyDescriptor getPropertyDescriptor() {
            return getBinding().getContext().getPropertySet().getDescriptor(getBinding().getPropertyName());
        }

        private void updateListModel() {
            ValueSet valueSet = getPropertyDescriptor().getValueSet();
            if (valueSet != null) {
                list.setListData(valueSet.getItems());
                adjustComponents();
            }
        }

        @Override
        public void valueChanged(ListSelectionEvent event) {
            if (event.getValueIsAdjusting()) {
                return;
            }
            if (getBinding().isAdjustingComponents()) {
                return;
            }
            final Property property = getBinding().getContext().getPropertySet().getProperty(getBinding().getPropertyName());
            Object selectedValue = list.getSelectedValue();
            try {
                property.setValue(selectedValue);
                // Now model is in sync with UI
                getBinding().clearProblem();
            } catch (ValidationException e) {
                getBinding().reportProblem(e);
            }
        }
    }

    private static class BandNameValidator implements Validator {
        private final Product product;

        public BandNameValidator(Product product) {
            this.product = product;
        }

        @Override
        public void validateValue(Property property, Object value) throws ValidationException {
            final String bandName = value.toString().trim();
            if (!ProductNode.isValidNodeName(bandName)) {
                throw new ValidationException(MessageFormat.format("The band name ''{0}'' appears not to be valid.\n" +
                                                                           "Please choose another one.",
                                                                   bandName));
            } else if (product.containsBand(bandName)) {
                throw new ValidationException(MessageFormat.format("The selected product already contains a band named ''{0}''.\n" +
                                                                           "Please choose another one.",
                                                                   bandName));
            }

        }
    }

    private static class ElevationSourceImage extends RasterDataNodeSampleOpImage {
        private final ElevationModel dem;
        private final GeoCoding geoCoding;
        private double noDataValue;

        public ElevationSourceImage(ElevationModel dem, GeoCoding geoCoding, Band band, ResolutionLevel level) {
            super(band, level);
            this.dem = dem;
            this.geoCoding = geoCoding;
            noDataValue = band.getNoDataValue();
        }

        @Override
        protected double computeSample(int sourceX, int sourceY) {
            try {
                return dem.getElevation(geoCoding.getGeoPos(new PixelPos(sourceX + 0.5f, sourceY + 0.5f), null));
            } catch (Exception e) {
                return noDataValue;
            }
        }
    }

    private static class LongitudeSourceImage extends RasterDataNodeSampleOpImage {
        private final Orthorectifier orthorectifier;

        public LongitudeSourceImage(Orthorectifier orthorectifier, Band band, ResolutionLevel level) {
            super(band, level);
            this.orthorectifier = orthorectifier;
        }

        @Override
        protected double computeSample(int sourceX, int sourceY) {
            GeoPos geoPos = orthorectifier.getGeoPos(new PixelPos(sourceX, sourceY), null);
            return geoPos.lon;
        }
    }

    private static class LatitudeSourceImage extends RasterDataNodeSampleOpImage {
        private final Orthorectifier orthorectifier;

        public LatitudeSourceImage(Orthorectifier orthorectifier, Band band, ResolutionLevel level) {
            super(band, level);
            this.orthorectifier = orthorectifier;
        }

        @Override
        protected double computeSample(int sourceX, int sourceY) {
            GeoPos geoPos = orthorectifier.getGeoPos(new PixelPos(sourceX, sourceY), null);
            return geoPos.lat;
        }
    }


    class DialogData {
        String demName;
        String resamplingMethod;
        boolean outputElevationBand;
        boolean outputDemCorrectedBands;
        String elevationBandName = DEFAULT_ELEVATION_BAND_NAME;
        String latitudeBandName = DEFAULT_LATITUDE_BAND_NAME;
        String longitudeBandName = DEFAULT_LONGITUDE_BAND_NAME;

        public DialogData(String demName, String resamplingMethod, boolean ortorectifiable) {
            this.demName = demName;
            this.resamplingMethod = resamplingMethod;
            outputElevationBand = true;
            outputDemCorrectedBands = ortorectifiable;
        }
    }

}
