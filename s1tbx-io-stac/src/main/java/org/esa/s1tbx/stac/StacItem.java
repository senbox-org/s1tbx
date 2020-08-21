/*
 * Copyright (C) 2020 Skywatch Space Applications Inc. https://www.skywatch.com
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
package org.esa.s1tbx.stac;

import org.esa.s1tbx.stac.extensions.EO;
import org.esa.s1tbx.stac.extensions.SNAP;
import org.esa.s1tbx.stac.support.GeoCodingSupport;
import org.esa.s1tbx.stac.support.TimeSupport;
import org.esa.snap.core.datamodel.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opengis.geometry.BoundingBox;

import java.util.Collections;

@SuppressWarnings("unchecked")
public class StacItem {

    private final static String VERSION = "1.0.0";

    public final static String stac_version = "stac_version";
    public final static String stac_extensions = "stac_extensions";

    public final static String id = "id";
    public final static String description = "description";
    public final static String title = "title";
    public final static String type = "type";
    public final static String bbox = "bbox";

    public final static String geometry = "geometry";
    public final static String coordinates = "coordinates";

    public final static String properties = "properties";
    public final static String datetime = "datetime";
    public final static String start_datetime = "start_datetime";
    public final static String end_datetime = "end_datetime";

    public final static String links = "links";
    public final static String assets = "assets";
    public final static String providers = "providers";
    public final static String collection = "collection";
    public final static String keywords = "keywords";

    private final JSONObject json;
    private JSONArray stacExtensionsArray;
    private JSONObject propertiesJSON;
    private JSONObject assetsJSON;

    private JSONArray linksArray;
    private JSONArray providersArray;
    private JSONArray keywordsArray;
    private JSONArray bandsArray;

    public StacItem(final String identifier) {
        this(new JSONObject(), identifier);
    }

    public StacItem(final JSONObject json, final String identifier) {
        this.json = json;
        createStacItem(identifier);
    }

    private void createStacItem(final String identifier) {
        json.put(stac_version, VERSION);
        json.put(id, identifier);
        json.put(type, "Feature");

        stacExtensionsArray = new JSONArray();
        json.put(stac_extensions, stacExtensionsArray);

        propertiesJSON = new JSONObject();
        json.put(properties, propertiesJSON);

        providersArray = new JSONArray();
        propertiesJSON.put(providers, providersArray);

        assetsJSON = new JSONObject();
        json.put(assets, assetsJSON);

        keywordsArray = new JSONArray();
        json.put(keywords, keywordsArray);

        linksArray = new JSONArray();
        json.put(links, linksArray);
    }

    public void addExtension(final String... extensions) {
        Collections.addAll(stacExtensionsArray, extensions);
    }

    public void addKeywords(final String... keywords) {
        Collections.addAll(keywordsArray, keywords);
    }

    public void addProvider(final String name, final String role, final String url) {
        final JSONObject provider = new JSONObject();
        provider.put("name", name);
        provider.put("role", role);
        provider.put("url", url);
        providersArray.add(provider);
    }

    public JSONObject getGeometry() {
        return (JSONObject) json.get(geometry);
    }

    public JSONObject getProperties() {
        return propertiesJSON;
    }

    public void writeProductProperties(final Product product) {
        json.put(StacItem.description, product.getDescription());
        propertiesJSON.put(SNAP.product_type, product.getProductType());
        propertiesJSON.put(SNAP.scene_raster_width, product.getSceneRasterWidth());
        propertiesJSON.put(SNAP.scene_raster_height, product.getSceneRasterHeight());
        if(product.getQuicklookBandName() != null) {
            propertiesJSON.put(SNAP.quicklook_band_name, product.getQuicklookBandName());
        }

        writeBoundingBox(product);
        writeTimes(product);
        writeCoordinates(json, product);
        writeBands(product);
    }

    private void writeBoundingBox(final Product product) {
        final JSONArray bboxArray = new JSONArray();
        json.put(bbox, bboxArray);

        final BoundingBox box = GeoCodingSupport.getBoundingBox(product);
        bboxArray.add(box.getMaxX());
        bboxArray.add(box.getMaxY());
        bboxArray.add(box.getMinX());
        bboxArray.add(box.getMinY());
    }

    private void writeTimes(final Product product) {
        final String startTime = TimeSupport.getFormattedTime(product.getStartTime());
        final String endTime = TimeSupport.getFormattedTime(product.getEndTime());
        propertiesJSON.put(datetime, startTime);
        propertiesJSON.put(start_datetime, startTime);
        propertiesJSON.put(end_datetime, endTime);
    }

    private void writeBands(final Product product) {
        bandsArray = new JSONArray();
        propertiesJSON.put(EO.bands, bandsArray);

        for(Band band : product.getBands()) {
            if (!(band instanceof FilterBand)) {
                bandsArray.add(EO.writeBand(band));
            }
        }
    }

    private void writeCoordinates(final JSONObject json, final Product product) {

        final JSONObject geometry = new JSONObject();
        json.put(StacItem.geometry, geometry);
        geometry.put("type", "Polygon");

        final GeoCoding geoCoding = product.getSceneGeoCoding();
        if (geoCoding != null) {
            final GeoPos ul = geoCoding.getGeoPos(new PixelPos(0, 0), null);
            final GeoPos ur = geoCoding.getGeoPos(new PixelPos(product.getSceneRasterWidth() - 1, 0), null);
            final GeoPos lr = geoCoding.getGeoPos(new PixelPos(product.getSceneRasterWidth() - 1, product.getSceneRasterHeight() - 1), null);
            final GeoPos ll = geoCoding.getGeoPos(new PixelPos(0, product.getSceneRasterHeight() - 1), null);

            final JSONArray cornerArray = new JSONArray();
            cornerArray.add(createCoord(ul.lon, ul.lat));
            cornerArray.add(createCoord(ur.lon, ur.lat));
            cornerArray.add(createCoord(lr.lon, lr.lat));
            cornerArray.add(createCoord(ll.lon, ll.lat));
            cornerArray.add(createCoord(ul.lon, ul.lat));

            geometry.put(coordinates, cornerArray);
        }
    }

    private static JSONArray createCoord(final double lon, final double lat) {
        final JSONArray coordArray = new JSONArray();
        coordArray.add(lon);
        coordArray.add(lat);
        return coordArray;
    }
}
