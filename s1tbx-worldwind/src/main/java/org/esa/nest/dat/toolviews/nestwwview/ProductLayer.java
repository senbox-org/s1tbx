/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat.toolviews.nestwwview;

import com.bc.ceres.core.*;
import com.sun.media.jfxmedia.logging.Logger;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.*;


import gov.nasa.worldwind.util.BufferFactory;
import gov.nasa.worldwind.util.BufferWrapper;
import gov.nasa.worldwind.util.WWMath;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurface;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurfaceAttributes;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurfaceLegend;
import gov.nasa.worldwindx.examples.util.DirectedPath;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.logging.BeamLogManager;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.eo.Constants;
import org.esa.snap.eo.GeoUtils;
import org.esa.snap.gpf.OperatorUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**

 */
public class ProductLayer extends RenderableLayer {
    private Product selectedProduct = null;
    private final boolean enableSurfaceImages;

    private final ConcurrentHashMap<String, Polyline[]> outlineTable = new ConcurrentHashMap<String, Polyline[]>();
    private final ConcurrentHashMap<String, SurfaceImage> imageTable = new ConcurrentHashMap<String, SurfaceImage>();

    ProductLayer(boolean showSurfaceImages) {
        enableSurfaceImages = showSurfaceImages;
    }

    public String[] getProductNames() {
        return outlineTable.keySet().toArray(new String[outlineTable.size()]);
    }

    private static String getUniqueName(final Product product) {
        return product.getProductRefString();
       /* String name = product.getName();
        File file = product.getFileLocation();
        if(file != null)
            name += file.getAbsolutePath();
        return name; */
    }

    @Override
    public void setOpacity(double opacity) {
        super.setOpacity(opacity);

        for (Map.Entry<String, SurfaceImage> entry : this.imageTable.entrySet()) {
            entry.getValue().setOpacity(opacity);
        }
    }

    public void setOpacity(String name, double opacity) {
        final SurfaceImage img = imageTable.get(name);
        if (img != null)
            img.setOpacity(opacity);
    }

    public double getOpacity(String name) {
        final SurfaceImage img = imageTable.get(name);
        if (img != null)
            return img.getOpacity();
        else {
            final Polyline[] lineList = outlineTable.get(name);
            return lineList != null ? 1 : 0;
        }
    }

    public void setSelectedProduct(Product product) {
        selectedProduct = product;
        if (selectedProduct != null) {
            final String selName = getUniqueName(selectedProduct);
            for (String name : outlineTable.keySet()) {
                final Polyline[] lineList = outlineTable.get(name);
                final boolean highlight = name.equals(selName);
                for (Polyline line : lineList) {
                    line.setHighlighted(highlight);
                    line.setHighlightColor(Color.RED);
                }
            }
        }
    }

    public Product getSelectedProduct() {
        return selectedProduct;
    }

    public void addProduct(final Product product) {
        final String name = getUniqueName(product);
        if (this.outlineTable.get(name) != null)
            return;

        final GeoCoding geoCoding = product.getGeoCoding();
        if (geoCoding == null) {
            final String productType = product.getProductType();
            if (productType.equals("ASA_WVW_2P") || productType.equals("ASA_WVS_1P") || productType.equals("ASA_WVI_1P")) {
                addWaveProduct(product);
            }
        } else {

            try {
                // CHANGED
                 if (product.getName().indexOf("_OCN_") >= 0) {
                    final Product newProduct = product;


                    //System.out.println("called");
                    //final Band band = newProduct.getBandAt(0);
                    /*
                    for (String currBandName : newProduct.getBandNames()) {
                        System.out.println("currBandName " + currBandName);
                    }
                    */
                    System.out.println("product " + product.getName());

                    final Band lonBand = newProduct.getBand("hh_001_owiLon");
                    final Band latBand = newProduct.getBand("hh_001_owiLat");
                    final Band incAngleBand = newProduct.getBand("hh_001_owiIncidenceAngle");
                    final Band windSpeedBand = newProduct.getBand("hh_001_owiWindSpeed");
                    final Band windDirBand = newProduct.getBand("hh_001_owiWindDirection");

                     final Band oswLonBand = newProduct.getBand("hh_001_oswLon");
                     final Band oswLatBand = newProduct.getBand("hh_001_oswLat");
                     final Band oswWindDirBand = newProduct.getBand("hh_001_oswWindDirection");

                     final Band rvlLonBand = newProduct.getBand("hh_001_rvlLon");
                     final Band rvlLatBand = newProduct.getBand("hh_001_rvlLat");
                     final Band rvlRadVelBand = newProduct.getBand("hh_001_rvlRadVel");

                    //final Band band = newProduct.getBand();
                    System.out.println("band 0 " + lonBand);
                    System.out.println("band width " + lonBand.getRasterWidth());
                    System.out.println("band height " + lonBand.getRasterHeight());

                    final float[] lonValues = new float[lonBand.getRasterWidth() * lonBand.getRasterHeight()];
                    lonBand.readPixels(0, 0, lonBand.getRasterWidth(), lonBand.getRasterHeight(), lonValues, com.bc.ceres.core.ProgressMonitor.NULL);

                    final float[] latValues = new float[latBand.getRasterWidth() * latBand.getRasterHeight()];
                    latBand.readPixels(0, 0, latBand.getRasterWidth(), latBand.getRasterHeight(), latValues, com.bc.ceres.core.ProgressMonitor.NULL);

                    final float[] incAngleValues = new float[incAngleBand.getRasterWidth() * incAngleBand.getRasterHeight()];
                    incAngleBand.readPixels(0, 0, incAngleBand.getRasterWidth(), incAngleBand.getRasterHeight(), incAngleValues, com.bc.ceres.core.ProgressMonitor.NULL);

                    final float[] windSpeedValues = new float[windSpeedBand.getRasterWidth() * windSpeedBand.getRasterHeight()];
                    windSpeedBand.readPixels(0, 0, windSpeedBand.getRasterWidth(), windSpeedBand.getRasterHeight(), windSpeedValues, com.bc.ceres.core.ProgressMonitor.NULL);

                    final float[] windDirValues = new float[windDirBand.getRasterWidth() * windDirBand.getRasterHeight()];
                    windDirBand.readPixels(0, 0, windDirBand.getRasterWidth(), windDirBand.getRasterHeight(), windDirValues, com.bc.ceres.core.ProgressMonitor.NULL);


                     final float[] oswLonValues = new float[oswLonBand.getRasterWidth() * oswLonBand.getRasterHeight()];
                     incAngleBand.readPixels(0, 0, oswLonBand.getRasterWidth(), oswLonBand.getRasterHeight(), oswLonValues, com.bc.ceres.core.ProgressMonitor.NULL);

                     final float[] oswLatValues = new float[oswLatBand.getRasterWidth() * oswLatBand.getRasterHeight()];
                     oswLatBand.readPixels(0, 0, oswLatBand.getRasterWidth(), oswLatBand.getRasterHeight(), oswLatValues, com.bc.ceres.core.ProgressMonitor.NULL);

                     final float[] oswWindDirValues = new float[oswWindDirBand.getRasterWidth() * oswWindDirBand.getRasterHeight()];
                     oswWindDirBand.readPixels(0, 0, oswWindDirBand.getRasterWidth(), oswWindDirBand.getRasterHeight(), oswWindDirValues, com.bc.ceres.core.ProgressMonitor.NULL);

                     final float[] rvlLonValues = new float[rvlLonBand.getRasterWidth() * rvlLonBand.getRasterHeight()];
                     rvlLonBand.readPixels(0, 0, rvlLonBand.getRasterWidth(), rvlLonBand.getRasterHeight(), rvlLonValues, com.bc.ceres.core.ProgressMonitor.NULL);

                     final float[] rvlLatValues = new float[rvlLatBand.getRasterWidth() * rvlLatBand.getRasterHeight()];
                     windSpeedBand.readPixels(0, 0, rvlLatBand.getRasterWidth(), rvlLatBand.getRasterHeight(), rvlLatValues, com.bc.ceres.core.ProgressMonitor.NULL);

                     final float[] rvlRadVelValues = new float[rvlRadVelBand.getRasterWidth() * rvlRadVelBand.getRasterHeight()];
                     rvlRadVelBand.readPixels(0, 0, rvlRadVelBand.getRasterWidth(), rvlRadVelBand.getRasterHeight(), rvlRadVelValues, com.bc.ceres.core.ProgressMonitor.NULL);

                    final GeoPos geoPos1 = product.getGeoCoding().getGeoPos(new PixelPos(0, 0), null);
                    final GeoPos geoPos2 = product.getGeoCoding().getGeoPos(new PixelPos(product.getSceneRasterWidth() - 1,
                                    product.getSceneRasterHeight() - 1), null);

                    double HUE_BLUE = 240d / 360d;
                    double HUE_RED = 0d / 360d;
                    double minValue = -200e3 * 2d;
                    double maxValue = 200e3 / 2d;

                    createRandomColorSurface(geoPos2.getLat(), geoPos1.getLat(), geoPos1.getLon(), geoPos2.getLon(), HUE_BLUE, HUE_RED, 40, 40, minValue, maxValue, this);
                    //createRandomColorSurface(25, 35, -110, -100, HUE_BLUE, HUE_RED, 40, 40, minValue, maxValue, this);
                    //createRandomColorSurface(geoPos2.getLat(),  geoPos1.getLat(), 55, 57, HUE_BLUE, HUE_RED, 40, 40, minValue, maxValue, this);
                    System.out.println("geoPos1.getLat(), geoPos2.getLat(), geoPos1.getLon(), geoPos2.getLon() " + geoPos1.getLat() + " " + geoPos2.getLat() + " " + geoPos1.getLon() + " " + geoPos2.getLon());
                    ShapeAttributes attrs = new BasicShapeAttributes();
                    attrs.setOutlineMaterial(Material.BLACK);
                    attrs.setOutlineWidth(2d);

                     Format legendLabelFormat = new DecimalFormat("# m/s");
                         /*
                     {
                         public StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition)
                         {

                             double altitudeMeters = altitude + verticalScale * number;
                             double altitudeKm = altitudeMeters * WWMath.METERS_TO_KILOMETERS;
                             return super.format(altitudeKm, result, fieldPosition);
                         }
                     };
                     */
                     AnalyticSurfaceLegend legend = AnalyticSurfaceLegend.fromColorGradient(minValue, maxValue, HUE_BLUE, HUE_RED,
                             AnalyticSurfaceLegend.createDefaultColorGradientLabels(minValue, maxValue, legendLabelFormat),
                             AnalyticSurfaceLegend.createDefaultTitle("Wind Speed"));
                     legend.setOpacity(0.8);
                     legend.setScreenLocation(new Point(650, 300));
                    addRenderable(legend);

                    for (int i = 0; i < latValues.length; i++) {
                        //System.out.println(lonValues[i] + "::==::" + latValues[i] + "::==::" + incAngleValues[i] + "::==::" + windSpeedValues[i] + "::==::" + windDirValues[i] + "::==::");
                        Position startPos = new Position(Angle.fromDegreesLatitude(latValues[i]), Angle.fromDegreesLongitude(lonValues[i]), 10.0);
                        Position endPos = new Position(LatLon.greatCircleEndPosition(startPos, Angle.fromDegrees(windDirValues[i]), Angle.fromDegrees(0.01)), 10.0);

                        //System.out.println("startPos " + startPos + " endPos " + endPos);

                        ArrayList<Position> positions = new ArrayList<Position>();
                        positions.add(startPos);
                        positions.add(endPos);

                        /*
                        Polyline pl = new Polyline();
                        pl.setColor(Color.YELLOW);
                        pl.setPositions(positions);
                        pl.setPathType(Polyline.RHUMB_LINE);
                        pl.setNumSubsegments(2);
                        pl.setFollowTerrain(true);
                        */

                        DirectedPath directedPath = new DirectedPath(positions);
                        directedPath.setAttributes(attrs);
                        directedPath.setVisible(true);
                        directedPath.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
                        directedPath.setPathType(AVKey.GREAT_CIRCLE);

                        addRenderable(directedPath);
                    }
                }
            } catch (Exception e) {
                System.out.println("exception " + e);
                e.printStackTrace();
            }
            // add image
            if (product.getName().indexOf("_OCN_") < 0) {
                if (enableSurfaceImages)
                    addSurfaceImage(product);
            }
            // add outline
            addOutline(product);
        }
    }

    private void addSurfaceImage(final Product product) {
        final String name = getUniqueName(product);

        final SwingWorker worker = new SwingWorker() {

            @Override
            protected SurfaceImage doInBackground() throws Exception {




                    final Product newProduct = createSubsampledProduct(product);
                    final Band band = newProduct.getBandAt(0);
                    final BufferedImage image = ProductUtils.createRgbImage(new RasterDataNode[]{band},
                            band.getImageInfo(com.bc.ceres.core.ProgressMonitor.NULL),
                            com.bc.ceres.core.ProgressMonitor.NULL);

                    final GeoPos geoPos1 = product.getGeoCoding().getGeoPos(new PixelPos(0, 0), null);
                    final GeoPos geoPos2 = product.getGeoCoding().getGeoPos(new PixelPos(product.getSceneRasterWidth() - 1,
                                    product.getSceneRasterHeight() - 1),
                            null
                    );

                    final Sector sector = new Sector(Angle.fromDegreesLatitude(geoPos1.getLat()),
                            Angle.fromDegreesLatitude(geoPos2.getLat()),
                            Angle.fromDegreesLongitude(geoPos1.getLon()),
                            Angle.fromDegreesLongitude(geoPos2.getLon()));

                    final SurfaceImage si = new SurfaceImage(image, sector);
                    si.setOpacity(getOpacity());

                return si;
            }

            @Override
            public void done() {

                try {
                    if (imageTable.contains(name))
                        removeImage(name);
                    final SurfaceImage si = (SurfaceImage) get();
                    addRenderable(si);
                    imageTable.put(name, si);
                } catch (Exception e) {
                    //VisatApp.getApp().showErrorDialog(e.getMessage());
                }
            }
        };
        worker.execute();
    }

    // ADDED
    protected static void createRandomColorSurface(double minLat, double maxLat, double minLon, double maxLon, double minHue, double maxHue, int width, int height, double minValue, double maxValue,
                                                   RenderableLayer outLayer)
    {
        //double minValue = -200e3;
        //double maxValue = 200e3;

        AnalyticSurface surface = new AnalyticSurface();
        surface.setSector(Sector.fromDegrees(minLat, maxLat, minLon, maxLon));
        surface.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
        surface.setDimensions(width, height);
        surface.setClientLayer(outLayer);
        outLayer.addRenderable(surface);

        //BufferWrapper firstBuffer = randomGridValues(width, height, minValue / 2d, maxValue * 2d);
        BufferWrapper secondBuffer = randomGridValues(width, height, minValue, maxValue);

        ArrayList<AnalyticSurface.GridPointAttributes> attributesList
                = new ArrayList<AnalyticSurface.GridPointAttributes>();
        for (int i = 0; i < secondBuffer.length(); i++)
        {
            attributesList.add(
                    AnalyticSurface.createColorGradientAttributes(secondBuffer.getDouble(i), minValue, maxValue, minHue, maxHue));
        }

        surface.setValues(attributesList);

        //mixValuesOverTime(2000L, firstBuffer, secondBuffer, minValue, maxValue, minHue, maxHue, surface);

        AnalyticSurfaceAttributes attr = new AnalyticSurfaceAttributes();
        attr.setDrawShadow(false);
        attr.setInteriorOpacity(0.6);
        //attr.setOutlineWidth(3);
        attr.setDrawOutline(false);
        surface.setSurfaceAttributes(attr);
    }

    public static BufferWrapper randomGridValues(int width, int height, double min, double max, int numIterations,
                                                 double smoothness, BufferFactory factory)
    {
        int numValues = width * height;
        double[] values = new double[numValues];

        for (int i = 0; i < numIterations; i++)
        {
            double offset = 1d - (i / (double) numIterations);

            int x1 = (int) Math.round(Math.random() * (width - 1));
            int x2 = (int) Math.round(Math.random() * (width - 1));
            int y1 = (int) Math.round(Math.random() * (height - 1));
            int y2 = (int) Math.round(Math.random() * (height - 1));
            int dx1 = x2 - x1;
            int dy1 = y2 - y1;

            for (int y = 0; y < height; y++)
            {
                int dy2 = y - y1;
                for (int x = 0; x < width; x++)
                {
                    int dx2 = x - x1;

                    if ((dx2 * dy1 - dx1 * dy2) >= 0)
                        values[x + y * width] += offset;
                }
            }
        }

        smoothValues(width, height, values, smoothness);
        scaleValues(values, numValues, min, max);
        BufferWrapper buffer = factory.newBuffer(numValues);
        buffer.putDouble(0, values, 0, numValues);

        return buffer;
    }

    public static BufferWrapper randomGridValues(int width, int height, double min, double max)
    {
        return randomGridValues(width, height, min, max, 1000, 0.5d,
                new BufferFactory.DoubleBufferFactory());
    }

    protected static void scaleValues(double[] values, int count, double minValue, double maxValue)
    {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (int i = 0; i < count; i++)
        {
            if (min > values[i])
                min = values[i];
            if (max < values[i])
                max = values[i];
        }

        for (int i = 0; i < count; i++)
        {
            values[i] = (values[i] - min) / (max - min);
            values[i] = minValue + values[i] * (maxValue - minValue);
        }
    }

    protected static void smoothValues(int width, int height, double[] values, double smoothness)
    {
        // top to bottom
        for (int x = 0; x < width; x++)
        {
            smoothBand(values, x, width, height, smoothness);
        }

        // bottom to top
        int lastRowOffset = (height - 1) * width;
        for (int x = 0; x < width; x++)
        {
            smoothBand(values, x + lastRowOffset, -width, height, smoothness);
        }

        // left to right
        for (int y = 0; y < height; y++)
        {
            smoothBand(values, y * width, 1, width, smoothness);
        }

        // right to left
        int lastColOffset = width - 1;
        for (int y = 0; y < height; y++)
        {
            smoothBand(values, lastColOffset + y * width, -1, width, smoothness);
        }
    }

    protected static void smoothBand(double[] values, int start, int stride, int count, double smoothness)
    {
        double prevValue = values[start];
        int j = start + stride;

        for (int i = 0; i < count - 1; i++)
        {
            values[j] = smoothness * prevValue + (1 - smoothness) * values[j];
            prevValue = values[j];
            j += stride;
        }
    }


    private void addOutline(final Product product) {
        // ADDED
        //System.out.println("TESTING ADD OUTLINE");
        final int step = Math.max(16, (product.getSceneRasterWidth() + product.getSceneRasterHeight()) / 250);
        final GeneralPath[] boundaryPaths = ProductUtils.createGeoBoundaryPaths(product, null, step);

        final Polyline[] polyLineList = new Polyline[boundaryPaths.length];
        int i = 0;
        for (GeneralPath boundaryPath : boundaryPaths) {
            final PathIterator it = boundaryPath.getPathIterator(null);
            final float[] floats = new float[2];
            final List<Position> positions = new ArrayList<Position>(4);

            it.currentSegment(floats);
            final Position firstPosition = new Position(Angle.fromDegreesLatitude(floats[1]),
                    Angle.fromDegreesLongitude(floats[0]), 0.0);
            positions.add(firstPosition);
            it.next();

            while (!it.isDone()) {
                it.currentSegment(floats);
                positions.add(new Position(Angle.fromDegreesLatitude(floats[1]),
                        Angle.fromDegreesLongitude(floats[0]), 0.0));
                it.next();
            }
            // close the loop
            positions.add(firstPosition);


            polyLineList[i] = new Polyline();
            polyLineList[i].setFollowTerrain(true);
            polyLineList[i].setPositions(positions);

            // ADDED
            //polyLineList[i].setColor(new Color(1f, 0f, 0f, 0.99f));
            //polyLineList[i].setLineWidth(10);

            addRenderable(polyLineList[i]);
            ++i;
        }
        outlineTable.put(getUniqueName(product), polyLineList);
    }

    private void addWaveProduct(final Product product) {
        final MetadataElement root = AbstractMetadata.getOriginalProductMetadata(product);
        final MetadataElement ggADS = root.getElement("GEOLOCATION_GRID_ADS");
        if (ggADS == null) return;

        final MetadataElement[] geoElemList = ggADS.getElements();
        final Polyline[] lineList = new Polyline[geoElemList.length];
        int cnt = 0;
        for (MetadataElement geoElem : geoElemList) {
            final double lat = geoElem.getAttributeDouble("center_lat", 0.0) / Constants.oneMillion;
            final double lon = geoElem.getAttributeDouble("center_long", 0.0) / Constants.oneMillion;
            final double heading = geoElem.getAttributeDouble("heading", 0.0);

            final GeoUtils.LatLonHeading r1 = GeoUtils.vincenty_direct(lon, lat, 5000, heading);
            final GeoUtils.LatLonHeading corner1 = GeoUtils.vincenty_direct(r1.lon, r1.lat, 2500, heading - 90.0);
            final GeoUtils.LatLonHeading corner2 = GeoUtils.vincenty_direct(r1.lon, r1.lat, 2500, heading + 90.0);

            final GeoUtils.LatLonHeading r2 = GeoUtils.vincenty_direct(lon, lat, 5000, heading + 180.0);
            final GeoUtils.LatLonHeading corner3 = GeoUtils.vincenty_direct(r2.lon, r2.lat, 2500, heading - 90.0);
            final GeoUtils.LatLonHeading corner4 = GeoUtils.vincenty_direct(r2.lon, r2.lat, 2500, heading + 90.0);

            final List<Position> positions = new ArrayList<Position>(4);
            positions.add(new Position(Angle.fromDegreesLatitude(corner1.lat), Angle.fromDegreesLongitude(corner1.lon), 0.0));
            positions.add(new Position(Angle.fromDegreesLatitude(corner2.lat), Angle.fromDegreesLongitude(corner2.lon), 0.0));
            positions.add(new Position(Angle.fromDegreesLatitude(corner4.lat), Angle.fromDegreesLongitude(corner4.lon), 0.0));
            positions.add(new Position(Angle.fromDegreesLatitude(corner3.lat), Angle.fromDegreesLongitude(corner3.lon), 0.0));
            positions.add(new Position(Angle.fromDegreesLatitude(corner1.lat), Angle.fromDegreesLongitude(corner1.lon), 0.0));

            final Polyline line = new Polyline();
            line.setFollowTerrain(true);
            line.setPositions(positions);

            addRenderable(line);
            lineList[cnt++] = line;
        }
        outlineTable.put(getUniqueName(product), lineList);
    }

    public void removeProduct(final Product product) {
        removeOutline(getUniqueName(product));
        removeImage(getUniqueName(product));
    }

    private void removeOutline(String imagePath) {
        final Polyline[] lineList = this.outlineTable.get(imagePath);
        if (lineList != null) {
            for (Polyline line : lineList) {
                this.removeRenderable(line);
            }
            this.outlineTable.remove(imagePath);
        }
    }

    private void removeImage(String imagePath) {
        final SurfaceImage si = this.imageTable.get(imagePath);
        if (si != null) {
            this.removeRenderable(si);
            this.imageTable.remove(imagePath);
        }
    }

    private static Product createSubsampledProduct(final Product product) throws IOException {

        final String quicklookBandName = ProductUtils.findSuitableQuicklookBandName(product);
        final ProductSubsetDef productSubsetDef = new ProductSubsetDef("subset");
        int scaleFactor = product.getSceneRasterWidth() / 1000;
        if (scaleFactor < 1) {
            scaleFactor = 1;
        }
        productSubsetDef.setSubSampling(scaleFactor, scaleFactor);
        productSubsetDef.setTreatVirtualBandsAsRealBands(true);
        productSubsetDef.setNodeNames(new String[]{quicklookBandName});
        Product productSubset = product.createSubset(productSubsetDef, quicklookBandName, null);

        if (!OperatorUtils.isMapProjected(product)) {
            try {
                final Map<String, Object> projParameters = new HashMap<String, Object>();
                Map<String, Product> projProducts = new HashMap<String, Product>();
                projProducts.put("source", productSubset);
                projParameters.put("crs", "WGS84(DD)");
                productSubset = GPF.createProduct("Reproject", projParameters, projProducts);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return productSubset;
    }
}