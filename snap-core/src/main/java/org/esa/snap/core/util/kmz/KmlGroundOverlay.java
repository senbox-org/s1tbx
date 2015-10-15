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

package org.esa.snap.core.util.kmz;

import org.esa.snap.core.datamodel.ProductData;
import org.opengis.geometry.BoundingBox;

import java.awt.image.RenderedImage;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class KmlGroundOverlay extends KmlOverlay {

    private final BoundingBox latLonBox;
    private final ProductData.UTC startTime;
    private final ProductData.UTC endTime;

    public KmlGroundOverlay(String name, RenderedImage overlay, BoundingBox latLonBox) {
        this(name, overlay, latLonBox, null, null);
    }

    public KmlGroundOverlay(final String name, final RenderedImage overlay, final BoundingBox latLonBox,
                            final ProductData.UTC startTime,
                            final ProductData.UTC endTime) {
        super("GroundOverlay", name, overlay);
        this.latLonBox = latLonBox;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public ProductData.UTC getStartTime() {
        return startTime;
    }

    public ProductData.UTC getEndTime() {
        return endTime;
    }


    public BoundingBox getLatLonBox() {
        return latLonBox;
    }

    @Override
    protected void createKmlSpecifics(StringBuilder sb) {
        super.createKmlSpecifics(sb);

        final ProductData.UTC startTime = getStartTime();
        final ProductData.UTC endTime = getEndTime();
        if (startTime != null && endTime != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            sdf.setCalendar(startTime.getAsCalendar());
            String startTimeString = sdf.format(startTime.getAsDate());
            sdf.setCalendar(endTime.getAsCalendar());
            String endTimeString = sdf.format(endTime.getAsDate());
            sb.append("<TimeSpan>");
            sb.append("<begin>").append(startTimeString).append("</begin>");
            sb.append("<end>").append(endTimeString).append("</end>");
            sb.append("</TimeSpan>");
        }

        BoundingBox bbox = getLatLonBox();
        sb.append("<LatLonBox>");
        sb.append("<north>").append(bbox.getMaxY()).append("</north>");
        sb.append("<south>").append(bbox.getMinY()).append("</south>");
        sb.append("<east>").append(bbox.getMaxX()).append("</east>");
        sb.append("<west>").append(bbox.getMinX()).append("</west>");
        sb.append("</LatLonBox>");

    }

}
