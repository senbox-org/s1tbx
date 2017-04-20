/*
 * Copyright (C) 2017 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.dem.dataio;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.dem.ElevationModelRegistry;
import org.esa.snap.core.dataop.resamp.ResamplingFactory;
import org.esa.snap.core.util.SystemUtils;
import org.junit.Test;

/**
 * Created by lveci on 3/31/2017.
 */
public class TestElevations {

    private static final GeoPos toronto = new GeoPos(43.6532, -79.3832);

    private static final ElevationModelRegistry elevationModelRegistry = ElevationModelRegistry.getInstance();
    private static final ElevationModelDescriptor ace30Descriptor = elevationModelRegistry.getDescriptor("ACE30");
    private static final ElevationModelDescriptor srtm3Descriptor = elevationModelRegistry.getDescriptor("SRTM 3Sec");
    private static final ElevationModelDescriptor srtm1Descriptor = elevationModelRegistry.getDescriptor("SRTM 1Sec HGT");
    private static final ElevationModelDescriptor cdemDescriptor = elevationModelRegistry.getDescriptor("CDEM");
    private static final ElevationModel ace30Dem = ace30Descriptor.createDem(ResamplingFactory.createResampling(ResamplingFactory.BILINEAR_INTERPOLATION_NAME));
    private static final ElevationModel srtm3Dem = srtm3Descriptor.createDem(ResamplingFactory.createResampling(ResamplingFactory.BILINEAR_INTERPOLATION_NAME));
    private static final ElevationModel srtm1Dem = srtm1Descriptor.createDem(ResamplingFactory.createResampling(ResamplingFactory.BILINEAR_INTERPOLATION_NAME));
    private static final ElevationModel cdemDem = cdemDescriptor.createDem(ResamplingFactory.createResampling(ResamplingFactory.BILINEAR_INTERPOLATION_NAME));

    @Test
    public void testGetElevation() throws Exception {

        double ace30 = ace30Dem.getElevation(toronto);
        SystemUtils.LOG.info("ace30 = " + ace30);

        double srtm3 = srtm3Dem.getElevation(toronto);
        SystemUtils.LOG.info("SRTM3 = " + srtm3);

        double srtm1 = srtm1Dem.getElevation(toronto);
        SystemUtils.LOG.info("SRTM1 = " + srtm1);

        double cdem = cdemDem.getElevation(toronto);
        SystemUtils.LOG.info("CDEM = " + cdem);
    }
}


