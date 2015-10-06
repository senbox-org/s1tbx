/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.dataop.dem;

import org.esa.snap.core.datamodel.AngularDirection;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Pointing;

class PointingMock implements Pointing {

    private GeoCodingMock _geoCoding;

    PointingMock(GeoCodingMock geoCoding) {
        _geoCoding = geoCoding;
    }

    @Override
    public GeoCoding getGeoCoding() {
        return _geoCoding;
    }

    @Override
    public AngularDirection getSunDir(PixelPos pixelPos, AngularDirection angularDirection) {
        return null;
    }

    @Override
    public AngularDirection getViewDir(PixelPos pixelPos, AngularDirection angularDirection) {
        if (angularDirection == null) {
            angularDirection = new AngularDirection();
        }
        angularDirection.azimuth = 0;
        angularDirection.zenith = 45;
        return angularDirection;
    }

    @Override
    public double getElevation(PixelPos pixelPos) {
        return 6000 * pixelPos.y / OrthorectifierTest.SCENE_HEIGHT;
    }

    public boolean canGetGeoPos() {
        return true;
    }

    @Override
    public boolean canGetElevation() {
        return true;
    }

    @Override
    public boolean canGetSunDir() {
        return true;
    }

    @Override
    public boolean canGetViewDir() {
        return true;
    }
}
