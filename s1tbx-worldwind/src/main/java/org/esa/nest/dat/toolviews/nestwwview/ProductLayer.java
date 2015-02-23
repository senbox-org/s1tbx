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

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
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
import gov.nasa.worldwindx.examples.util.DirectedPath;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.util.ProductUtils;
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

    private static double HUE_BLUE = 240d / 360d;
    private static double HUE_RED = 0d / 360d;
    private static double HUE_MAX_RED = 1.0;

    public double theCurrMinHue;
    public double theCurrMaxHue;

    public ScreenAnnotation infoAnnotation;
    //private AnalyticSurface analyticSurface = null;
    //private BufferWrapper analyticSurfaceValueBuffer = null;

    public ColorBarLegend owiColorBarLegend = null;
    public ColorBarLegend oswColorBarLegend = null;
    public ColorBarLegend rvlColorBarLegend = null;

    // product associated with the current colorBar legend
    public Product colorBarLegendProduct = null;

    public HashMap<DirectedPath, String> theObjectInfoHash = null;
    public HashMap<Product, ProductRenderablesInfo> theProductRenderablesInfoHash = null;

    public ShapeAttributes dpAttrs = null;
    //public ShapeAttributes dpHighlightAttrs = null;

    public WorldWindowGLCanvas theWWD = null;

    ProductLayer(boolean showSurfaceImages) {

        enableSurfaceImages = showSurfaceImages;

        theObjectInfoHash = new HashMap<DirectedPath, String>();
        theProductRenderablesInfoHash = new HashMap<Product, ProductRenderablesInfo>();

        dpAttrs = new BasicShapeAttributes();
        dpAttrs.setOutlineMaterial(Material.BLACK);
        dpAttrs.setOutlineWidth(2d);

        //dpHighlightAttrs = new BasicShapeAttributes();
        //dpHighlightAttrs.setOutlineMaterial(Material.WHITE);
        //dpHighlightAttrs.setOutlineWidth(2d);

        // this is copied from gov.nasa.worldwindx.examples.util.LayerManagerLayer
        infoAnnotation = new ScreenAnnotation("", new Point(120, 520));

        // Set annotation so that it will not force text to wrap (large width) and will adjust it's width to
        // that of the text. A height of zero will have the annotation height follow that of the text too.
        infoAnnotation.getAttributes().setSize(new Dimension(Integer.MAX_VALUE, 0));
        infoAnnotation.getAttributes().setAdjustWidthToText(AVKey.SIZE_FIT_TEXT);

        // Set appearance attributes
        infoAnnotation.getAttributes().setCornerRadius(0);
        //infoAnnotation.getAttributes().setFont(this.font);
        infoAnnotation.getAttributes().setHighlightScale(1);
        infoAnnotation.getAttributes().setTextColor(Color.WHITE);
        infoAnnotation.getAttributes().setBackgroundColor(new Color(0f, 0f, 0f, .5f));
        infoAnnotation.getAttributes().setInsets(new Insets(6, 6, 6, 6));
        infoAnnotation.getAttributes().setBorderWidth(1);


        infoAnnotation.getAttributes().setVisible(false);

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

    public void addProduct(final Product product, boolean addVisualization, WorldWindowGLCanvas wwd) {
        theWWD = wwd;
        ProductRenderablesInfo productRenderablesInfo = new ProductRenderablesInfo ();

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
                 if (addVisualization && (product.getName().indexOf("S1A_S1_OCN_") >= 0 || product.getName().indexOf("003197_05B7") >= 0)) {
                    final Product newProduct = product;

                     addRenderable(infoAnnotation);

                     final StringBuilder text = new StringBuilder(255);
                     text.append("First line<br />");
                     text.append("Second line");
                     infoAnnotation.setText(text.toString());


                     // There is code in LayerMagerLayer that updates the size
                     //  it's re-rendered
                     // Update current size and adjust annotation draw offset according to it's width
                     //this.size = infoAnnotation.getPreferredSize(dc);
                     //this.annotation.getAttributes().setDrawOffset(new Point(this.size.width / 2, 0));

                     infoAnnotation.getAttributes().setVisible(false);
                    //System.out.println("called");
                    //final Band band = newProduct.getBandAt(0);
                    /*
                    for (String currBandName : newProduct.getBandNames()) {
                        System.out.println("currBandName " + currBandName);
                    }
                    */
                    System.out.println("product " + product.getName());

                     String prefix = "vv";


                    if (newProduct.getBand(prefix + "_001_owiLon") == null) {
                        prefix = "hh";
                    }
                    final Band lonBand = newProduct.getBand(prefix + "_001_owiLon");
                    final Band latBand = newProduct.getBand(prefix + "_001_owiLat");
                    final Band incAngleBand = newProduct.getBand(prefix + "_001_owiIncidenceAngle");
                    final Band windSpeedBand = newProduct.getBand(prefix + "_001_owiWindSpeed");
                    final Band windDirBand = newProduct.getBand(prefix + "_001_owiWindDirection");
                    final Band rvlRadVelBand = newProduct.getBand(prefix + "_001_rvlRadVel");

                     //final Band oswLonBand = newProduct.getBand("hh_001_oswLon");
                     //final Band oswLatBand = newProduct.getBand("hh_001_oswLat");
                     //final Band oswWindDirBand = newProduct.getBand("hh_001_oswWindDirection");

                     //final Band rvlLonBand = newProduct.getBand("hh_001_rvlLon");
                     //final Band rvlLatBand = newProduct.getBand("hh_001_rvlLat");
                     //final Band rvlRadVelBand = newProduct.getBand("hh_001_rvlRadVel");

                    //final Band band = newProduct.getBand();
                    System.out.println("band 0 " + lonBand);
                    System.out.println("band width " + lonBand.getRasterWidth());
                    System.out.println("band height " + lonBand.getRasterHeight());

                    final float[] lonValues = new float[lonBand.getRasterWidth() * lonBand.getRasterHeight()];
                    lonBand.readPixels(0, 0, lonBand.getRasterWidth(), lonBand.getRasterHeight(), lonValues, com.bc.ceres.core.ProgressMonitor.NULL);

                    final float[] latValues = new float[latBand.getRasterWidth() * latBand.getRasterHeight()];
                    latBand.readPixels(0, 0, latBand.getRasterWidth(), latBand.getRasterHeight(), latValues, com.bc.ceres.core.ProgressMonitor.NULL);

                    final double[] incAngleValues = new double[incAngleBand.getRasterWidth() * incAngleBand.getRasterHeight()];
                    incAngleBand.readPixels(0, 0, incAngleBand.getRasterWidth(), incAngleBand.getRasterHeight(), incAngleValues, com.bc.ceres.core.ProgressMonitor.NULL);

                    final double[] windSpeedValues = new double[windSpeedBand.getRasterWidth() * windSpeedBand.getRasterHeight()];
                    windSpeedBand.readPixels(0, 0, windSpeedBand.getRasterWidth(), windSpeedBand.getRasterHeight(), windSpeedValues, com.bc.ceres.core.ProgressMonitor.NULL);

                    final double[] windDirValues = new double[windDirBand.getRasterWidth() * windDirBand.getRasterHeight()];
                    windDirBand.readPixels(0, 0, windDirBand.getRasterWidth(), windDirBand.getRasterHeight(), windDirValues, com.bc.ceres.core.ProgressMonitor.NULL);

                    final double[] rvlRadVelValues = new double[rvlRadVelBand.getRasterWidth() * rvlRadVelBand.getRasterHeight()];
                     rvlRadVelBand.readPixels(0, 0, rvlRadVelBand.getRasterWidth(), rvlRadVelBand.getRasterHeight(), rvlRadVelValues, com.bc.ceres.core.ProgressMonitor.NULL);


                     //final float[] oswLonValues = new float[oswLonBand.getRasterWidth() * oswLonBand.getRasterHeight()];
                     //incAngleBand.readPixels(0, 0, oswLonBand.getRasterWidth(), oswLonBand.getRasterHeight(), oswLonValues, com.bc.ceres.core.ProgressMonitor.NULL);

                     //final float[] oswLatValues = new float[oswLatBand.getRasterWidth() * oswLatBand.getRasterHeight()];
                     //oswLatBand.readPixels(0, 0, oswLatBand.getRasterWidth(), oswLatBand.getRasterHeight(), oswLatValues, com.bc.ceres.core.ProgressMonitor.NULL);

                     //final float[] oswWindDirValues = new float[oswWindDirBand.getRasterWidth() * oswWindDirBand.getRasterHeight()];
                     //oswWindDirBand.readPixels(0, 0, oswWindDirBand.getRasterWidth(), oswWindDirBand.getRasterHeight(), oswWindDirValues, com.bc.ceres.core.ProgressMonitor.NULL);

                     //final float[] rvlLonValues = new float[rvlLonBand.getRasterWidth() * rvlLonBand.getRasterHeight()];
                     //rvlLonBand.readPixels(0, 0, rvlLonBand.getRasterWidth(), rvlLonBand.getRasterHeight(), rvlLonValues, com.bc.ceres.core.ProgressMonitor.NULL);

                     //final float[] rvlLatValues = new float[rvlLatBand.getRasterWidth() * rvlLatBand.getRasterHeight()];
                     //windSpeedBand.readPixels(0, 0, rvlLatBand.getRasterWidth(), rvlLatBand.getRasterHeight(), rvlLatValues, com.bc.ceres.core.ProgressMonitor.NULL);

                     //final float[] rvlRadVelValues = new float[rvlRadVelBand.getRasterWidth() * rvlRadVelBand.getRasterHeight()];
                     //rvlRadVelBand.readPixels(0, 0, rvlRadVelBand.getRasterWidth(), rvlRadVelBand.getRasterHeight(), rvlRadVelValues, com.bc.ceres.core.ProgressMonitor.NULL);

                    final GeoPos geoPos1 = product.getGeoCoding().getGeoPos(new PixelPos(0, 0), null);
                    final GeoPos geoPos2 = product.getGeoCoding().getGeoPos(new PixelPos(product.getSceneRasterWidth() - 1,
                                    product.getSceneRasterHeight() - 1), null);


                     colorBarLegendProduct = product;

                     int[] cellSizeArr = {4, 8, 16, 24, 32, 40, 48, 64};

                     for (int cellSizeInd = 0; cellSizeInd < cellSizeArr.length; cellSizeInd++) {
                         double minHeight = 0;
                         double maxHeight = cellSizeArr[cellSizeInd] * 0.5e6 / 16;
                         if (cellSizeInd > 0) {
                             minHeight = cellSizeArr[cellSizeInd - 1] * 0.5e6 / 16;
                         }
                         addWindSpeedArrows (latValues, lonValues, incAngleValues, windSpeedValues, windDirValues, lonBand.getRasterWidth(), lonBand.getRasterHeight(), cellSizeArr[cellSizeInd], minHeight, maxHeight, productRenderablesInfo.owiRenderableList);
                     }

                     createColorSurfaceAndBar(geoPos1, geoPos2, windSpeedValues, 0, 10, HUE_RED, 1.0, false, productRenderablesInfo.owiRenderableList, "OWI Wind Speed", productRenderablesInfo, "owi");
                     createColorSurfaceAndBar(geoPos1, geoPos2, rvlRadVelValues, -6, 6, HUE_RED, 1.0, true, productRenderablesInfo.rvlRenderableList, "RVL Rad. Vel.", productRenderablesInfo, "rvl");

                     theProductRenderablesInfoHash.put(product, productRenderablesInfo);

                     setComponentVisible(colorBarLegendProduct, "owi", true);
                }
            } catch (Exception e) {
                System.out.println("exception " + e);
                e.printStackTrace();
            }
            // add image
            if (!(addVisualization && (product.getName().indexOf("S1A_S1_OCN_") >= 0 || product.getName().indexOf("003197_05B7") >= 0))) {
                if (enableSurfaceImages)
                    addSurfaceImage(product);
            }
            // add outline
            addOutline(product);
        }
    }


    // ADDED
    public void createColorSurfaceAndBar (GeoPos geoPos1, GeoPos geoPos2, double[] values, double minValue, double maxValue, double minHue, double maxHue, boolean whiteZero, ArrayList<Renderable> renderableList, String legendTitle, ProductRenderablesInfo prodRenderInfo, String comp) {
        createColorSurface(geoPos2.getLat(), geoPos1.getLat(), geoPos1.getLon(), geoPos2.getLon(), values, 40, 40, minValue, maxValue, renderableList, prodRenderInfo, comp);
        //createColorSurface(geoPos2.getLat(), geoPos1.getLat(), geoPos1.getLon(), geoPos2.getLon(), rvlRadVelValues, 40, 40, minValue, maxValue, renderableList);

        theCurrMinHue = minHue;
        theCurrMaxHue = maxHue;

        //createRandomColorSurface(25, 35, -110, -100, HUE_BLUE, HUE_RED, 40, 40, minValue, maxValue, this);
        //createRandomColorSurface(geoPos2.getLat(),  geoPos1.getLat(), 55, 57, HUE_BLUE, HUE_RED, 40, 40, minValue, maxValue, this);
        System.out.println("geoPos1.getLat(), geoPos2.getLat(), geoPos1.getLon(), geoPos2.getLon() " + geoPos1.getLat() + " " + geoPos2.getLat() + " " + geoPos1.getLon() + " " + geoPos2.getLon());
        createColorGradient(minValue, maxValue, theCurrMinHue, theCurrMaxHue, whiteZero, prodRenderInfo, comp);
        createColorBarLegend(minValue, maxValue, theCurrMinHue, theCurrMaxHue, whiteZero, legendTitle, comp);
    }



    public void createColorBarLegend(double minValue, double maxValue, double minHue, double maxHue, boolean whiteZero, String title, String comp) {
        System.out.println("createColorBarLegend");
        ColorBarLegend colorBarLegend = null;

        Format legendLabelFormat = new DecimalFormat("# m/s");
        colorBarLegend = ColorBarLegend.fromColorGradient(32, 256, minValue, maxValue, minHue, maxHue,
                Color.WHITE,
                ColorBarLegend.createDefaultColorGradientLabels(minValue, maxValue, legendLabelFormat),
                ColorBarLegend.createDefaultTitle(title),
                whiteZero);
        colorBarLegend.setOpacity(0.8);
        colorBarLegend.setScreenLocation(new Point(900, 320));
        //addRenderable(colorBarLegend);

        if (comp.equalsIgnoreCase("owi")) {
            owiColorBarLegend = colorBarLegend;
        }
        else if (comp.equalsIgnoreCase("osw")) {
            oswColorBarLegend = colorBarLegend;
        }
        else if (comp.equalsIgnoreCase("rvl")) {
            rvlColorBarLegend = colorBarLegend;
        }

    }

    public void setComponentVisible (Product product, String comp, boolean visible) {
        System.out.println("setComponentVisible " + comp);
        ProductRenderablesInfo productRenderablesInfo = theProductRenderablesInfoHash.get(product);
        if (productRenderablesInfo != null) {
            ArrayList<Renderable> renderableList = null;
            if (comp.equalsIgnoreCase("owi")) {
                renderableList = productRenderablesInfo.owiRenderableList;

                if (owiColorBarLegend != null) {
                    removeRenderable(owiColorBarLegend);
                    if (visible) {
                        addRenderable(owiColorBarLegend);
                    }

                }

            }
            else if (comp.equalsIgnoreCase("osw")) {
                renderableList = productRenderablesInfo.oswRenderableList;

                if (oswColorBarLegend != null) {
                    removeRenderable(oswColorBarLegend);
                    if (visible) {
                        addRenderable(oswColorBarLegend);
                    }
                }

            }
            else if (comp.equalsIgnoreCase("rvl")) {
                renderableList = productRenderablesInfo.rvlRenderableList;

                if (rvlColorBarLegend != null) {
                    removeRenderable(rvlColorBarLegend);
                    if (visible) {
                        addRenderable(rvlColorBarLegend);
                    }
                }
            }



            for (Renderable renderable : renderableList) {
                System.out.println(" renderable " + renderable);
                removeRenderable(renderable);
                if (visible) {
                    addRenderable(renderable);
                }
            }

            theWWD.redrawNow();
        }
    }

    private void addWindSpeedArrows (float[] latValues,
                                     float[] lonValues,
                                     double[] incAngleValues,
                                     double[] windSpeedValues,
                                     double[] windDirValues,
                                     int width,
                                     int height,
                                     int cellSize,
                                     double minHeight,
                                     double maxHeight,
                                     ArrayList<Renderable> renderableList) {
        float pixelWidth = Math.abs(lonValues[0] - lonValues[lonValues.length - 1]) / width;
        float pixelHeight = Math.abs(latValues[0] - latValues[latValues.length - 1]) / height;

        System.out.println("pixelWidth " + pixelWidth + " pixelHeight " + pixelHeight);
        // take half of the smaller dimension
        float arrowLength = pixelWidth;
        if (pixelHeight < pixelWidth) {
            arrowLength = pixelHeight;
        }
        arrowLength = arrowLength * cellSize / 2;



        for (int row = 0; row < height; row=row+cellSize) {
            for (int col = 0; col < width; col=col+cellSize) {
                //int i = row*width + col;
                int globalInd = row*width + col;
                float avgLat = 0;
                float avgLon = 0;
                double avgIncAngle = 0;
                double avgWindSpeed = 0;
                double avgWindDir = 0;
                int finalCellRow = row +  cellSize;
                int finalCellCol = col +  cellSize;

                if (finalCellRow > height) {
                    finalCellRow = height;
                }
                if (finalCellCol > width) {
                    finalCellCol = width;
                }
                for (int currCellRow = row; currCellRow < finalCellRow; currCellRow++) {
                    for (int currCellCol = col; currCellCol < finalCellCol; currCellCol++) {
                        int i = currCellRow*width + currCellCol;
                        avgLat += latValues[i];
                        avgLon += lonValues[i];
                        avgIncAngle += incAngleValues[i];
                        avgWindSpeed += windSpeedValues[i];
                        avgWindDir += windDirValues[i];
                    }
                }

                avgLat = avgLat / ((finalCellRow - row) * (finalCellCol - col));
                avgLon = avgLon / ((finalCellRow - row) * (finalCellCol - col));
                avgIncAngle = avgIncAngle / ((finalCellRow - row) * (finalCellCol - col));
                avgWindSpeed = avgWindSpeed / ((finalCellRow - row) * (finalCellCol - col));
                avgWindDir = avgWindDir / ((finalCellRow - row) * (finalCellCol - col));

                           /*
                           avgLat = latValues[globalInd];
                           avgLon = lonValues[globalInd];
                           avgWindDir = windDirValues[globalInd];
                            */
                            /*
                           System.out.println("avgLat " + avgLat);
                           System.out.println("avgLon " + avgLon);
                           System.out.println("avgWindDir " + avgWindDir);
                            */
                System.out.println("avgIncAngle " + avgIncAngle);
                //for (int i = 0; i < latValues.length; i=i+50) {
                //System.out.println(lonValues[i] + "::==::" + latValues[i] + "::==::" + incAngleValues[i] + "::==::" + windSpeedValues[i] + "::==::" + windDirValues[i] + "::==::");
                final Position startPos = new Position(Angle.fromDegreesLatitude(avgLat), Angle.fromDegreesLongitude(avgLon), 10.0);
                final Position endPos = new Position(LatLon.greatCircleEndPosition(startPos, Angle.fromDegrees(avgWindDir), Angle.fromDegrees(arrowLength)), 10.0);

                //System.out.println("startPos " + startPos + " endPos " + endPos);

                ArrayList<Position> positions = new ArrayList<Position>();
                positions.add(startPos);
                positions.add(endPos);


                final DirectedPath directedPath = new DirectedPath(positions);
                Renderable renderable = new Renderable() {
                    public void render (DrawContext dc) {

                        directedPath.setAttributes(dpAttrs);
                        //directedPath.setHighlightAttributes(highlightAttrs);
                        directedPath.setVisible(true);
                        directedPath.setFollowTerrain(true);
                        directedPath.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
                        directedPath.setPathType(AVKey.GREAT_CIRCLE);
                        //directedPath.setHighlighted(true);
                        // this is the length of the arrow head actually
                        double arrowHeadLength = computeSegmentLength(directedPath, dc, startPos, endPos) / 4;
                        directedPath.setArrowLength(arrowHeadLength);
                        //double maxHeight = cellSize * 0.5e6 / 16;

                        if (dc.getView().getCurrentEyePosition().getAltitude() > minHeight && dc.getView().getCurrentEyePosition().getAltitude() < maxHeight) {
                            directedPath.render(dc);
                            //System.out.println("arrowHeadLength " + arrowHeadLength);
                        }

                        //System.out.println("eyePosition " + dc.getView().getCurrentEyePosition());
                    }
                };
                addRenderable(renderable);
                if (renderableList != null) {
                    renderableList.add(renderable);
                }
                String info = "Wind Speed: " + avgWindSpeed + "<br/>";
                info += "Wind Direction: " + avgWindDir + "<br/>";
                info += "Incidence Angle: " + avgIncAngle + "<br/>";
                theObjectInfoHash.put(directedPath, info);
            }
        }
    }

    protected double computeSegmentLength(Path path, DrawContext dc, Position posA,
                                          Position posB)
    {
        LatLon llA = new LatLon(posA.getLatitude(), posA.getLongitude());
        LatLon llB = new LatLon(posB.getLatitude(), posB.getLongitude());

        Angle ang;
        String pathType = path.getPathType();
        if (pathType == AVKey.LINEAR)
            ang = LatLon.linearDistance(llA, llB);
        else if (pathType == AVKey.RHUMB_LINE || pathType == AVKey.LOXODROME)
            ang = LatLon.rhumbDistance(llA, llB);
        else // Great circle
            ang = LatLon.greatCircleDistance(llA, llB);

        if (path.getAltitudeMode() == WorldWind.CLAMP_TO_GROUND)
            return ang.radians * (dc.getGlobe().getRadius());

        double height = 0.5 * (posA.getElevation() + posB.getElevation());
        return ang.radians * (dc.getGlobe().getRadius() + height * dc.getVerticalExaggeration());
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
    protected void createColorSurface(double minLat, double maxLat, double minLon, double maxLon, double[] values, int width, int height, double minValue, double maxValue, ArrayList<Renderable> renderableList, ProductRenderablesInfo prodRenderInfo, String comp)
    {
        //double minValue = -200e3;
        //double maxValue = 200e3;

        AnalyticSurface analyticSurface = new AnalyticSurface();
        analyticSurface.setSector(Sector.fromDegrees(minLat, maxLat, minLon, maxLon));
        analyticSurface.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
        analyticSurface.setDimensions(width, height);

        AnalyticSurfaceAttributes attr = new AnalyticSurfaceAttributes();
        attr.setDrawShadow(false);
        attr.setInteriorOpacity(0.6);
        //attr.setOutlineWidth(3);
        attr.setDrawOutline(false);
        analyticSurface.setSurfaceAttributes(attr);


        analyticSurface.setClientLayer(this);
        //addRenderable(analyticSurface);

        //analyticSurfaceValueBuffer = randomGridValues(width, height, minValue, maxValue);
        BufferWrapper analyticSurfaceValueBuffer = (new BufferFactory.DoubleBufferFactory()).newBuffer(values.length);
        analyticSurfaceValueBuffer.putDouble(0, values, 0, values.length);

        smoothValues(width, height, values, 0.5d);
        scaleValues(values, values.length, minValue, maxValue);

        //mixValuesOverTime(2000L, firstBuffer, analyticSurfaceValueBuffer, minValue, maxValue, minHue, maxHue, analyticSurface);

        prodRenderInfo.setAnalyticSurfaceAndBuffer (analyticSurface, analyticSurfaceValueBuffer, comp);
        if (renderableList != null) {
            renderableList.add(analyticSurface);
        }

    }

    public void createColorGradient(double minValue, double maxValue, double minHue, double maxHue, boolean whiteZero, ProductRenderablesInfo prodRenderInfo, String comp) {
        System.out.println("createColorGradient");
        AnalyticSurface analyticSurface = null;
        BufferWrapper analyticSurfaceValueBuffer = null;

        if (comp.equalsIgnoreCase("owi")) {
            analyticSurface = prodRenderInfo.owiAnalyticSurface;
            analyticSurfaceValueBuffer = prodRenderInfo.owiAnalyticSurfaceValueBuffer;
        }
        else if (comp.equalsIgnoreCase("rvl")) {
            analyticSurface = prodRenderInfo.rvlAnalyticSurface;
            analyticSurfaceValueBuffer = prodRenderInfo.rvlAnalyticSurfaceValueBuffer;
        }

        ArrayList<AnalyticSurface.GridPointAttributes> attributesList = new ArrayList<AnalyticSurface.GridPointAttributes>();
        for (int i = 0; i < analyticSurfaceValueBuffer.length(); i++)
        {
            attributesList.add(
                    createColorGradientAttributes(analyticSurfaceValueBuffer.getDouble(i), minValue, maxValue, minHue, maxHue, whiteZero));
        }

        analyticSurface.setValues(attributesList);
    }

    // ADDED:
    // this method is copied from gov.nasa.worldwindx.examples.analytics.AnalyticSurface
    public static AnalyticSurface.GridPointAttributes createColorGradientAttributes(final double value,
                                  double minValue, double maxValue, double minHue, double maxHue, boolean whiteZero)
    {
        double hueFactor = WWMath.computeInterpolationFactor(value, minValue, maxValue);

        double hue = WWMath.mixSmooth(hueFactor, minHue, maxHue);
        double sat = 1.0;
        if (whiteZero) {
            sat = Math.abs(WWMath.mixSmooth(hueFactor, -1, 1));
        }
        Color color = Color.getHSBColor((float) hue, (float) sat, 1f);
        double opacity = WWMath.computeInterpolationFactor(value, minValue, minValue + (maxValue - minValue) * 0.1);
        Color rgbaColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (255 * opacity));

        return AnalyticSurface.createGridPointAttributes(value, rgbaColor);
    }
    /*
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
    */
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
        setComponentVisible(product, "owi", false);
        setComponentVisible(product, "osw", false);
        setComponentVisible(product, "rvl", false);

        ProductRenderablesInfo productRenderablesInfo = theProductRenderablesInfoHash.get(product);
        if (productRenderablesInfo != null) {
            productRenderablesInfo.owiRenderableList.clear();
            productRenderablesInfo.oswRenderableList.clear();
            productRenderablesInfo.rvlRenderableList.clear();

            owiColorBarLegend = null;
            oswColorBarLegend = null;
            rvlColorBarLegend = null;
        }

        /*
        ArrayList<Renderable> renderableList = theProductOWIRenderableHash.get(product);
        if (renderableList != null) {
            for (Renderable renderable : renderableList) {
                removeRenderable(renderable);
            }
        }
        */
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