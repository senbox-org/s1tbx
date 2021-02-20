/*
 * Copyright (C) 2021 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.s1tbx.commons.io;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.metadata.AbstractMetadataIO;
import org.jdom2.Element;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class JSONProductDirectory extends AbstractProductDirectory {

    protected JSONObject json;

    public JSONProductDirectory(final File headerFile) {
        super(headerFile);
    }

    public void readProductDirectory() throws IOException {
        try {
            final File headerFile = getFile(getRootFolder() + getHeaderFileName());
            this.json = (JSONObject) JSONUtils.loadJSONFile(headerFile);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    protected MetadataElement addMetaData() throws IOException {
        final MetadataElement root = new MetadataElement(Product.METADATA_ROOT_NAME);
        AbstractMetadataIO.AddXMLMetadata(jsonToXML("ProductMetadata", json), AbstractMetadata.addOriginalProductMetadata(root));

        addAbstractedMetadataHeader(root);

        return root;
    }

    public static Element jsonToXML(final String name, final JSONObject json) {
        final Element root = new Element(name);

        for(Object key : json.keySet()) {
            Object obj = json.get(key);
            if(obj instanceof JSONObject) {
                root.addContent(jsonToXML((String) key, (JSONObject) obj));
            } else if(obj instanceof JSONArray) {
                root.addContent(jsonArrayToXML((String) key, (JSONArray)obj));
            } else {
                root.setAttribute((String)key, String.valueOf(obj));
            }
        }
        return root;
    }

    private static Element jsonArrayToXML(final String name, final JSONArray jsonArray) {
        final Element root = new Element(name);

        int cnt = 1;
        for(Object obj : jsonArray) {
            if(obj instanceof JSONObject) {
                root.addContent(jsonToXML(name, (JSONObject) obj));
            } else if(obj instanceof JSONArray) {
                root.addContent(jsonArrayToXML(name, (JSONArray)obj));
            } else {
                root.setAttribute(name + cnt, String.valueOf(obj));
                ++cnt;
            }
        }
        return root;
    }

    public static GeoPos[] parseCoordinates(final JSONArray node) {
        final List<GeoPos> geoPosList = new ArrayList<>();
        for (Object o : node) {
            if (!(o instanceof JSONArray))
                continue;

            final JSONArray latLonList = (JSONArray) o;
            for (Object latlon : latLonList) {
                if (!(latlon instanceof JSONArray))
                    continue;
                final JSONArray coordList = (JSONArray) latlon;

                GeoPos geoPos = new GeoPos((Double)coordList.get(0), (Double)coordList.get(1));
                geoPosList.add(geoPos);
            }
        }
        return geoPosList.toArray(new GeoPos[geoPosList.size()]);
    }

    protected abstract void addAbstractedMetadataHeader(final MetadataElement root) throws IOException;
}
