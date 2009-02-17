/*
 *    GeoTools - The Open Source Java GIS Tookit
 *    http://geotools.org
 *
 *    (C) 2006-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This file is hereby placed into the Public Domain. This means anyone is
 *    free to do whatever they wish with this file. Use it well and enjoy!
 */
package org.esa.beam.visat.actions;

import java.awt.Component;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.apache.commons.lang.StringUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.ows.Layer;
import org.geotools.data.wms.WebMapServer;
import org.geotools.data.wms.request.GetMapRequest;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.DefaultMapLayer;
import org.geotools.map.MapContext;
import org.geotools.map.MapLayer;
import org.geotools.map.event.MapBoundsEvent;
import org.geotools.map.event.MapBoundsListener;
import org.geotools.map.event.MapLayerEvent;
import org.geotools.referencing.CRS;
import org.geotools.resources.coverage.FeatureUtilities;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.Symbolizer;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


public class WMSMapLayer extends DefaultMapLayer implements MapLayer, MapBoundsListener,
    ComponentListener {
    static GridCoverageFactory gcf = new GridCoverageFactory();
    private Layer layer;
    private WebMapServer wms;
    GridCoverage2D grid;
    private ReferencedEnvelope bbox;
    private ReferencedEnvelope bounds;
    private int width = 400;
    private int height = 200;
    private StyleFactory factory = CommonFactoryFinder.getStyleFactory(null);
    private boolean transparent = true;
    private String bgColour;
    private String exceptions = "application/vnd.ogc.se_inimage";

    public WMSMapLayer(WebMapServer wms, Layer layer) {
        super((FeatureSource<SimpleFeatureType, SimpleFeature>) null, null, "");
        this.layer = layer;
        this.wms = wms;
        setDefaultStyle();
        setGrid();
    }

    public void setDefaultStyle() {
        RasterSymbolizer symbolizer = factory.createRasterSymbolizer();

        // SLDParser stylereader = new SLDParser(factory,sld);
        // org.geotools.styling.Style[] style = stylereader.readXML();
        Style style = factory.createStyle();
        Rule[] rules = new Rule[1];
        rules[0] = factory.createRule();
        rules[0].setSymbolizers(new Symbolizer[] { symbolizer });

        FeatureTypeStyle type = factory.createFeatureTypeStyle(rules);
        style.addFeatureTypeStyle(type);
        setStyle(style);
    }

    private void setGrid() {
        GetMapRequest mapRequest = wms.createGetMapRequest();
        mapRequest.addLayer(layer);
        //System.out.println(width + " " + height);
        mapRequest.setDimensions(getWidth(), getHeight());
        mapRequest.setFormat("image/png");

        if (bgColour != null) {
            mapRequest.setBGColour(bgColour);
        }

        mapRequest.setExceptions(exceptions);

        Set srs = layer.getSrs();
        String crs = "EPSG:4326";

        if (srs.contains("EPSG:4326")) { // really we should get the underlying
                                         // map pane CRS
            crs = "EPSG:4326";
        } else {
            crs = (String) srs.iterator().next();
        }

        //System.out.println("crs = " + crs);
        if (bbox == null) {
            HashMap bboxes = layer.getBoundingBoxes();
            Set keys = bboxes.keySet();
            String k = "";

            for (Iterator it = keys.iterator(); it.hasNext(); k = (String) it.next()) {
                //System.out.println(k + " -> " + bboxes.get(k));
            }

            CRSEnvelope bb = (CRSEnvelope) bboxes.get(crs);

            if (bb == null) { // something bad happened
                bb = layer.getLatLonBoundingBox();
                bb.setEPSGCode("EPSG:4326"); // for some reason WMS doesn't
                                             // set
                                             // this.

                crs = "EPSG:4326";
            }

            CoordinateReferenceSystem coordinateReferenceSystem = null;

            try {
                coordinateReferenceSystem = CRS.decode(crs);
            } catch (FactoryRegistryException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (FactoryException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            bbox = new ReferencedEnvelope(bb.getMinX(), bb.getMaxX(), bb.getMinY(), bb.getMaxY(),
                    coordinateReferenceSystem);

            bounds = bbox;
        }

        // fix the bounds for the shape of the window.

        //System.out.println(bbox.toString());
        mapRequest.setSRS(crs);
        mapRequest.setBBox(bbox.getMinX() + "," + bbox.getMinY() + "," + bbox.getMaxX() + ","
            + bbox.getMaxY());
        mapRequest.setTransparent(transparent);

        URL request = mapRequest.getFinalURL();

        //System.out.println(request.toString());
        InputStream is = null;

        try {
            URLConnection connection = request.openConnection();
            String type = connection.getContentType();
            is = connection.getInputStream();

            if (type.equalsIgnoreCase("image/png")) {
                BufferedImage image = ImageIO.read(is);

                /*
                 * if (bbox == null) { Envelope2D env = new Envelope2D(bb
                 * .getCoordinateReferenceSystem(), bb.getMinX(), bb .getMinY(),
                 * bb.getLength(0), bb.getLength(1)); bbox = new
                 * ReferencedEnvelope(env); }
                 */
                grid = gcf.create(layer.getTitle(), image, bbox);
                //System.out.println("fetched new grid");
                //if (featureSource == null)
                featureSource = DataUtilities.source(FeatureUtilities.wrapGridCoverage(grid));

                fireMapLayerListenerLayerChanged(new MapLayerEvent(this, MapLayerEvent.DATA_CHANGED));
            } else {
                System.out.println("error content type is " + type);

                if (StringUtils.contains(type, "text") || StringUtils.contains(type,"xml")) {
                    String line = "";
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));

                    while ((line = br.readLine()) != null) {
                        System.out.println(line);
                    }
                }
            }
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (Exception ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void mapBoundsChanged(MapBoundsEvent event) {
        bbox = ((MapContext) event.getSource()).getAreaOfInterest();

        //		System.out.println("old:" + bbox + "\n" + "new:"
        //				+ event.getOldAreaOfInterest());
        if (!bbox.equals(event.getOldAreaOfInterest())) {
            System.out.println("bbox changed - fetching new grid");

            Thread t = new Thread() {
                    public void run() {
                        setGrid();
                    }
                };

            SwingUtilities.invokeLater(t);
        }
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public GridCoverage2D getGrid() {
        return grid;
    }

    public boolean isTransparent() {
        return transparent;
    }

    public void setTransparent(boolean transparent) {
        this.transparent = transparent;
    }

    public void componentHidden(ComponentEvent e) {
        // TODO Auto-generated method stub
    }

    public void componentMoved(ComponentEvent e) {
        // TODO Auto-generated method stub
    }

    public void componentResized(ComponentEvent e) {
        Component c = (Component) e.getSource();
        width = c.getWidth();
        height = c.getHeight();
    }

    public void componentShown(ComponentEvent e) {
        Component c = (Component) e.getSource();
        width = c.getWidth();
        height = c.getHeight();
    }

    public String getBgColour() {
        return bgColour;
    }

    public void setBgColour(String bgColour) {
        this.bgColour = bgColour;
    }

    public String getExceptions() {
        return exceptions;
    }

    /**
     * Set the type of exception reports.
     * Valid values are:
     * "application/vnd.ogc.se_xml" (the default)
     * "application/vnd.ogc.se_inimage"
     * "application/vnd.ogc.se_blank"
     *
     * @param exceptions
     */
    public void setExceptions(String exceptions) {
        this.exceptions = exceptions;
    }

    public ReferencedEnvelope getBounds() {
        //System.out.println("got bounds");
        return bounds;
    }
}
