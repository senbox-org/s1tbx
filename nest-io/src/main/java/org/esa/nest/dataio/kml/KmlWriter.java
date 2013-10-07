/*
 * Copyright (c) 2013. Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.nest.dataio.kml;

import org.esa.beam.framework.datamodel.GeoPos;

import java.io.IOException;
import java.io.Writer;

/**
 * Simple utility class for writing Google Earth KML files.
 *
 * @author Norman Fomferra
 */
public class KmlWriter {

    private Writer writer;

    public KmlWriter(Writer writer, String name, String description) throws IOException {
        this.writer = writer;
        writer.write(String.format("" +
                                   "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                   "<kml xmlns=\"http://www.opengis.net/kml/2.2\"\n" +
                                   "     xmlns:gx=\"http://www.google.com/kml/ext/2.2\">\n" +
                                   "<Document>\n" +
                                   "  <name>%s</name>\n" +
                                   "  <description>%s</description>\n",
                                   name, description));
    }

    public void writeGroundOverlay(String name, GeoPos ulPos, GeoPos lrPos, String imagePath) throws IOException {
        writer.write(String.format("" +
                                   "  <GroundOverlay>\n" +
                                   "    <name>%s</name>\n" +
                                   "    <Icon><href>%s</href></Icon>\n" +
                                   "    <LatLonBox>\n" +
                                   "      <north>%s</north>\n" +
                                   "      <south>%s</south>\n" +
                                   "      <west>%s</west>\n" +
                                   "      <east>%s</east>\n" +
                                   "    </LatLonBox>\n" +
                                   "  </GroundOverlay>\n",
                                   name, imagePath,
                                   ulPos.lat, lrPos.lat,
                                   ulPos.lon, lrPos.lon));
    }

    public void writeGroundOverlayEx(String name, GeoPos[] quadCoords, String imagePath) throws IOException {
        writer.write(String.format("" +
                                   "  <GroundOverlay>\n" +
                                   "    <name>%s</name>\n" +
                                   "    <Icon><href>%s</href></Icon>\n" +
                                   "    <gx:LatLonQuad>\n" +
                                   "      <coordinates>%s,%s %s,%s %s,%s %s,%s</coordinates>\n" +
                                   "    </gx:LatLonQuad>\n" +
                                   "  </GroundOverlay>\n",
                                   name, imagePath,
                                   quadCoords[0].lon, quadCoords[0].lat,
                                   quadCoords[1].lon, quadCoords[1].lat,
                                   quadCoords[2].lon, quadCoords[2].lat,
                                   quadCoords[3].lon, quadCoords[3].lat));
    }

    public void close() throws IOException {
        writer.write("" +
                     "  </Document>\n" +
                     "</kml>\n");
        writer.close();
    }

}