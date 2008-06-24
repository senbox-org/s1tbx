package org.esa.beam.visat.toolviews.stat;

import java.awt.Container;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.io.IOException;

import javax.swing.JInternalFrame;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TransectProfileData;
import org.esa.beam.framework.draw.Figure;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.visat.VisatApp;


/**
 * Utilities for the statistics dialog.
 *
 * @author Marco Peters
 */
public class StatisticsUtils {

    public static float round(final float x) {
        return round(x, 100.0f);
    }

    public static float round(final float x, final float v) {
        return MathUtils.round(x, v);
    }

    public static double round(final double x) {
        return round(x, 100.0);
    }

    public static double round(final double x, final double v) {
        return MathUtils.round(x, v);
    }

    public static String getDiagramLabel(final RasterDataNode raster) {
        final StringBuilder sb = new StringBuilder();
        sb.append(raster.getName());
        if (StringUtils.isNotNullAndNotEmpty(raster.getUnit())) {
            sb.append(" [");
            sb.append(raster.getUnit());
            sb.append("]");
        } else {
            sb.append(" [-]");
        }
        return sb.toString();
    }

    public static class TransectProfile {

        public static TransectProfileData getTransectProfileData(final RasterDataNode raster) throws IOException {
            final Shape transectShape = getTransectShape(raster);
            if (transectShape == null) {
                return null;
            } else {
                return raster.createTransectProfileData(transectShape);
            }
        }

        private static Shape getTransectShape(final RasterDataNode raster) {
            final VisatApp app = VisatApp.getApp();
            final JInternalFrame internalFrame = app.findInternalFrame(raster);
            if (internalFrame != null) {
                final Container contentPane = internalFrame.getContentPane();
                if (contentPane instanceof ProductSceneView) {
                    final ProductSceneView sceneView = (ProductSceneView) contentPane;
                    final Figure currentShapeFigure = sceneView.getCurrentShapeFigure();
                    if (currentShapeFigure != null) {
                        return currentShapeFigure.getShape();
                    }
                }
            }
            return null;
        }

        public static String createTransectProfileText(final RasterDataNode raster) throws IOException {
            final TransectProfileData data = getTransectProfileData(raster);
            if (data == null) {
                return null;
            }

            final Point2D[] pixelPositions = data.getPixelPositions();
            final GeoPos[] geoPositions = data.getGeoPositions();
            final float[] sampleValues = data.getSampleValues();

            final StringBuilder sb = new StringBuilder(1024);
            final String formatString = "%1$-10s\t";

            sb.append(String.format(formatString, "Index"));
            sb.append(String.format(formatString, "Pixel-X"));
            sb.append(String.format(formatString, "Pixel-Y"));
            if (geoPositions != null) {
                sb.append(String.format(formatString, "Lat"));
                sb.append(String.format(formatString, "Lon"));
            }
            sb.append(String.format(formatString, getDiagramLabel(raster)));
            sb.append("\n");

            for (int i = 0; i < pixelPositions.length; i++) {
                final Point2D pixelPos = pixelPositions[i];
                sb.append(String.format(formatString, String.valueOf(i)));
                sb.append(String.format(formatString, String.valueOf(pixelPos.getX())));
                sb.append(String.format(formatString, String.valueOf(pixelPos.getY())));
                if (geoPositions != null) {
                    final GeoPos geoPos = geoPositions[i];
                    sb.append(String.format(formatString, String.valueOf(geoPos.lat)));
                    sb.append(String.format(formatString, String.valueOf(geoPos.lon)));
                }
                if(Float.isNaN(sampleValues[i])){
                    sb.append(String.format(formatString, "No Data"));
                }else {
                    sb.append(String.format(formatString, String.valueOf(sampleValues[i])));
                }
                sb.append(" \n");
            }

            return sb.toString();
        }
    }
}
