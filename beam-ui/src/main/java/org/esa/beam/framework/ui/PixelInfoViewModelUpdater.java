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

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MapGeoCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.dataop.maptransf.MapTransform;
import org.esa.beam.framework.ui.PixelInfoView.DisplayFilter;
import org.esa.beam.framework.ui.PixelInfoView.DockablePaneKey;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.math.MathUtils;
import org.geotools.geometry.DirectPosition2D;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import javax.media.jai.PlanarImage;
import javax.swing.SwingUtilities;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Vector;

/**
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.5.2
 */
class PixelInfoViewModelUpdater {

    private static final String _INVALID_POS_TEXT = "Invalid pos.";

    private final PixelInfoViewTableModel geolocModel;
    private final PixelInfoViewTableModel scanlineModel;
    private final PixelInfoViewTableModel bandModel;
    private final PixelInfoViewTableModel tiePointModel;
    private final PixelInfoViewTableModel flagModel;

    private volatile Product currentProduct;
    private volatile RasterDataNode currentRaster;
    private volatile ProductSceneView currentView;
    private Band[] currentFlagBands;

    private int _pixelX;
    private int _pixelY;
    private int _level;
    private int levelZeroX;
    private int levelZeroY;
    private boolean _pixelPosValid;

    private final PixelInfoView pixelInfoView;

    PixelInfoViewModelUpdater(PixelInfoViewTableModel geolocModel, PixelInfoViewTableModel scanlineModel,
                              PixelInfoViewTableModel bandModel, PixelInfoViewTableModel tiePointModel,
                              PixelInfoViewTableModel flagModel, PixelInfoView pixelInfoView) {
        this.geolocModel = geolocModel;
        this.scanlineModel = scanlineModel;
        this.bandModel = bandModel;
        this.tiePointModel = tiePointModel;
        this.flagModel = flagModel;
        this.pixelInfoView = pixelInfoView;
    }

    Product getCurrentProduct() {
        return currentProduct;
    }

    RasterDataNode getCurrentRaster() {
        return currentRaster;
    }

    void update(PixelInfoState state) {
        update(state.view, state.pixelX, state.pixelY, state.level, state.pixelPosValid);
    }

    void update(ProductSceneView view, int pixelX, int pixelY, int level, boolean pixelPosValid) {
        Guardian.assertNotNull("view", view);
        boolean clearRasterTableSelection = false;
        RasterDataNode raster = view.getRaster();
        final Product product = raster.getProduct();
        if (product == currentProduct && view.isRGB()) {
            resetBandTableModel();
        }
        if (product != currentProduct) {
            ProductNodeListener productNodeListener = pixelInfoView.getProductNodeListener();
            if (currentProduct != null) {
                currentProduct.removeProductNodeListener(productNodeListener);
            }
            product.addProductNodeListener(productNodeListener);
            currentProduct = product;
            registerFlagDatasets();
            resetTableModels();
        }
        if (raster != currentRaster) {
            currentRaster = raster;
            registerFlagDatasets();
            resetTableModels();
        }
        if (bandModel.getRowCount() != getBandRowCount()) {
            resetTableModels();
        }
        if (view != currentView) {
            currentView = view;
            resetTableModels();
            clearRasterTableSelection = true;
        }
        Debug.assertTrue(currentProduct != null);
        _pixelX = pixelX;
        _pixelY = pixelY;
        _level = level;
        _pixelPosValid = pixelPosValid;
        AffineTransform i2mTransform = currentView.getBaseImageLayer().getImageToModelTransform(level);
        Point2D modelP = i2mTransform.transform(new Point2D.Double(pixelX + 0.5, pixelY + 0.5), null);
        AffineTransform m2iTransform = view.getBaseImageLayer().getModelToImageTransform();
        Point2D levelZeroP = m2iTransform.transform(modelP, null);
        levelZeroX = (int) Math.floor(levelZeroP.getX());
        levelZeroY = (int) Math.floor(levelZeroP.getY());

        updateDataDisplay(clearRasterTableSelection);
    }

    private void resetTableModels() {
        resetGeolocTableModel();
        resetScanLineTableModel();
        resetBandTableModel();
        resetTiePointGridTableModel();
        resetFlagTableModel();
    }

    private void fireTableChanged(final boolean clearRasterTableSelection) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (clearRasterTableSelection) {
                    pixelInfoView.clearSelectionInRasterTables();
                }
                geolocModel.fireTableDataChanged();
                scanlineModel.fireTableDataChanged();
                bandModel.fireTableDataChanged();
                tiePointModel.fireTableDataChanged();
                flagModel.fireTableDataChanged();
            }
        });
    }

    private void updateDataDisplay(boolean clearRasterTableSelection) {
        if (currentProduct == null) {
            return;
        }
        if (pixelInfoView.isDockablePaneVisible(DockablePaneKey.GEOLOCATION)) {
            updateGeolocValues();
        }
        if (pixelInfoView.isDockablePaneVisible(DockablePaneKey.SCANLINE)) {
            updateScanLineValues();
        }
        if (pixelInfoView.isDockablePaneVisible(DockablePaneKey.BANDS)) {
            updateBandPixelValues();
        }
        if (pixelInfoView.isDockablePaneVisible(DockablePaneKey.TIEPOINTS)) {
            updateTiePointGridPixelValues();
        }
        if (pixelInfoView.isDockablePaneVisible(DockablePaneKey.FLAGS)) {
            updateFlagPixelValues();
        }
        fireTableChanged(clearRasterTableSelection);
    }

    private void resetGeolocTableModel() {
        geolocModel.clear();
        if (currentRaster != null) {
            final GeoCoding geoCoding = currentRaster.getGeoCoding();
            geolocModel.addRow("Image-X", "", "pixel");
            geolocModel.addRow("Image-Y", "", "pixel");

            if (geoCoding != null) {
                geolocModel.addRow("Longitude", "", "degree");
                geolocModel.addRow("Latitude", "", "degree");

                if (geoCoding instanceof MapGeoCoding) {
                    final MapGeoCoding mapGeoCoding = (MapGeoCoding) geoCoding;
                    final String mapUnit = mapGeoCoding.getMapInfo().getMapProjection().getMapUnit();

                    geolocModel.addRow("Map-X", "", mapUnit);
                    geolocModel.addRow("Map-Y", "", mapUnit);
                } else if (geoCoding instanceof CrsGeoCoding) {
                    String xAxisUnit = geoCoding.getMapCRS().getCoordinateSystem().getAxis(0).getUnit().toString();
                    String yAxisUnit = geoCoding.getMapCRS().getCoordinateSystem().getAxis(1).getUnit().toString();
                    geolocModel.addRow("Map-X", "", xAxisUnit);
                    geolocModel.addRow("Map-Y", "", yAxisUnit);

                }
            }
        }
    }

    private void resetScanLineTableModel() {
        scanlineModel.clear();
        if (currentRaster != null) {
            scanlineModel.addRow("Date", "", "YYYY-MM-DD");
            scanlineModel.addRow("Time (UTC)", "", "HH:MM:SS:mm [AM/PM]");
        }
    }

    private void resetBandTableModel() {
        bandModel.clear();
        if (currentRaster != null) {
            final int numBands = currentProduct.getNumBands();
            for (int i = 0; i < numBands; i++) {
                final Band band = currentProduct.getBandAt(i);
                if (shouldDisplayBand(band)) {
                    bandModel.addRow(band.getName(), "", band.getUnit());
                }
            }
        }
    }

    private boolean shouldDisplayBand(final Band band) {
        DisplayFilter displayFilter = pixelInfoView.getDisplayFilter();
        if (displayFilter != null) {
            return displayFilter.accept(band);
        }
        return band.hasRasterData();
    }

    private void resetTiePointGridTableModel() {
        tiePointModel.clear();
        if (currentRaster != null) {
            final int numTiePointGrids = currentProduct.getNumTiePointGrids();
            for (int i = 0; i < numTiePointGrids; i++) {
                final TiePointGrid tiePointGrid = currentProduct.getTiePointGridAt(i);
                tiePointModel.addRow(tiePointGrid.getName(), "", tiePointGrid.getUnit());
            }
        }
    }

    private void resetFlagTableModel() {
        flagModel.clear();
        if (currentRaster != null) {
            for (Band band : currentFlagBands) {
                final FlagCoding flagCoding = band.getFlagCoding();
                final int numFlags = flagCoding.getNumAttributes();
                final String bandNameDot = band.getName() + ".";
                for (int j = 0; j < numFlags; j++) {
                    String name = bandNameDot + flagCoding.getAttributeAt(j).getName();
                    flagModel.addRow(name, "", "");
                }
            }
        }
    }

    private void registerFlagDatasets() {
        final Band[] bands = currentProduct.getBands();
        Vector<Band> flagBandsVector = new Vector<Band>();
        for (Band band : bands) {
            if (isFlagBand(band)) {
                flagBandsVector.add(band);
            }
        }
        currentFlagBands = flagBandsVector.toArray(new Band[flagBandsVector.size()]);
    }

    private boolean isFlagBand(final Band band) {
        return band.getFlagCoding() != null;
    }

    private int getBandRowCount() {
        int rowCount = 0;
        if (currentProduct != null) {
            Band[] bands = currentProduct.getBands();
            for (final Band band : bands) {
                if (shouldDisplayBand(band)) {
                    rowCount++;
                }
            }
        }
        return rowCount;
    }

    private int getFlagRowCount() {
        int rowCount = 0;
        for (Band band : currentFlagBands) {
            rowCount += band.getFlagCoding().getNumAttributes();
        }
        return rowCount;
    }

    private void updateGeolocValues() {
        final boolean available = isSampleValueAvailable(levelZeroX, levelZeroY, _pixelPosValid);
        final float pX = levelZeroX + pixelInfoView.getPixelOffsetX();
        final float pY = levelZeroY + pixelInfoView.getPixelOffsetY();

        String tix, tiy, tmx, tmy, tgx, tgy;
        tix = tiy = tmx = tmy = tgx = tgy = _INVALID_POS_TEXT;

        GeoCoding geoCoding = currentRaster.getGeoCoding();
        if (available) {
            PixelPos pixelPos = new PixelPos(pX, pY);
            if (pixelInfoView.showPixelPosDecimal()) {
                tix = String.valueOf(pX);
                tiy = String.valueOf(pY);
            } else {
                tix = String.valueOf(levelZeroX);
                tiy = String.valueOf(levelZeroY);
            }
            if (geoCoding != null) {
                GeoPos geoPos = geoCoding.getGeoPos(pixelPos, null);
                if (pixelInfoView.showGeoPosDecimal()) {
                    tgx = String.valueOf(geoPos.getLon());
                    tgy = String.valueOf(geoPos.getLat());
                } else {
                    tgx = geoPos.getLonString();
                    tgy = geoPos.getLatString();
                }
                if (geoCoding instanceof MapGeoCoding) {
                    final MapGeoCoding mapGeoCoding = (MapGeoCoding) geoCoding;
                    final MapTransform mapTransform = mapGeoCoding.getMapInfo().getMapProjection().getMapTransform();
                    Point2D mapPoint = mapTransform.forward(geoPos, null);
                    tmx = String.valueOf(MathUtils.round(mapPoint.getX(), 10000.0));
                    tmy = String.valueOf(MathUtils.round(mapPoint.getY(), 10000.0));
                } else if (geoCoding instanceof CrsGeoCoding) {
                    MathTransform transform = geoCoding.getImageToMapTransform();
                    try {
                        DirectPosition position = transform.transform(new DirectPosition2D(pX, pY), null);
                        double[] coordinate = position.getCoordinate();
                        tmx = String.valueOf(coordinate[0]);
                        tmy = String.valueOf(coordinate[1]);
                    } catch (TransformException ignore) {
                    }

                }
            }
        }
        geolocModel.updateValue(tix, 0);
        geolocModel.updateValue(tiy, 1);
        if (geoCoding != null) {
            geolocModel.updateValue(tgx, 2);
            geolocModel.updateValue(tgy, 3);
            if (geoCoding instanceof MapGeoCoding || geoCoding instanceof CrsGeoCoding) {
                geolocModel.updateValue(tmx, 4);
                geolocModel.updateValue(tmy, 5);
            }
        }
    }

    private void updateScanLineValues() {
        final ProductData.UTC utcStartTime = currentProduct.getStartTime();
        final ProductData.UTC utcEndTime = currentProduct.getEndTime();

        if (utcStartTime == null || utcEndTime == null || !isSampleValueAvailable(0, levelZeroY, true)) {
            scanlineModel.updateValue("No date information", 0);
            scanlineModel.updateValue("No time information", 1);
        } else {
            final float pY = levelZeroY + pixelInfoView.getPixelOffsetY();
            final ProductData.UTC utcCurrentLine = ProductUtils.getScanLineTime(currentProduct, pY);
            final Calendar currentLineTime = utcCurrentLine.getAsCalendar();

            final String dateString = String.format("%1$tF", currentLineTime);
            final String timeString = String.format("%1$tI:%1$tM:%1$tS:%1$tL %1$Tp", currentLineTime);

            scanlineModel.updateValue(dateString, 0);
            scanlineModel.updateValue(timeString, 1);
        }
    }

    private void updateBandPixelValues() {
        Band[] bands = currentProduct.getBands();
        int rowIndex = 0;
        for (final Band band : bands) {
            if (shouldDisplayBand(band)) {
                bandModel.updateValue(getPixelString(band), rowIndex);
                rowIndex++;
            }
        }
    }

    private String getPixelString(Band band) {
        if (!_pixelPosValid) {
            return RasterDataNode.INVALID_POS_TEXT;
        }
        if (isPixelValid(band, _pixelX, _pixelY, _level)) {
            if (band.isFloatingPointType()) {
                return String.valueOf((float) ProductUtils.getGeophysicalSampleDouble(band, _pixelX, _pixelY, _level));
            } else {
                return String.valueOf(ProductUtils.getGeophysicalSampleLong(band, _pixelX, _pixelY, _level));
            }
        } else {
            return RasterDataNode.NO_DATA_TEXT;
        }
    }

    private boolean isPixelValid(Band band, int pixelX, int pixelY, int level) {
        if (band.isValidMaskUsed()) {
            PlanarImage image = ImageManager.getInstance().getValidMaskImage(band, level);
            Raster data = getRasterTile(image, pixelX, pixelY);
            return data.getSample(pixelX, pixelY, 0) != 0;
        } else {
            return true;
        }
    }

    private Raster getRasterTile(PlanarImage image, int pixelX, int pixelY) {
        final int tileX = image.XToTileX(pixelX);
        final int tileY = image.YToTileY(pixelY);
        return image.getTile(tileX, tileY);
    }

    private void updateTiePointGridPixelValues() {
        final int numTiePointGrids = currentProduct.getNumTiePointGrids();
        int rowIndex = 0;
        for (int i = 0; i < numTiePointGrids; i++) {
            final TiePointGrid grid = currentProduct.getTiePointGridAt(i);
            tiePointModel.updateValue(grid.getPixelString(levelZeroX, levelZeroY), rowIndex);
            rowIndex++;
        }
    }

    private void updateFlagPixelValues() {
        final boolean available = isSampleValueAvailable(levelZeroX, levelZeroY, _pixelPosValid);

        if (flagModel.getRowCount() != getFlagRowCount()) {
            resetFlagTableModel();
        }
        int rowIndex = 0;
        for (Band band : currentFlagBands) {
            long pixelValue = available ? ProductUtils.getGeophysicalSampleLong(band, _pixelX, _pixelY, _level) : 0;

            for (int j = 0; j < band.getFlagCoding().getNumAttributes(); j++) {
                if (available) {
                    MetadataAttribute attribute = band.getFlagCoding().getAttributeAt(j);
                    int mask = attribute.getData().getElemInt();
                    flagModel.updateValue(String.valueOf((pixelValue & mask) == mask), rowIndex);
                } else {
                    flagModel.updateValue(_INVALID_POS_TEXT, rowIndex);
                }
                rowIndex++;
            }
        }
    }

    private boolean isSampleValueAvailable(int pixelX, int pixelY, boolean pixelValid) {
        return currentProduct != null
               && pixelValid
               && pixelX >= 0
               && pixelY >= 0
               && pixelX < currentProduct.getSceneRasterWidth()
               && pixelY < currentProduct.getSceneRasterHeight();
    }

    void clearProductNodeRefs() {
        currentProduct = null;
        currentRaster = null;
        currentView = null;
        currentFlagBands = new Band[0];
    }
}
