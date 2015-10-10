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
package org.esa.snap.engine_utilities.datamodel;

import org.esa.snap.core.datamodel.ProductData;

/**
 * Created by lveci on 03/06/2014.
 */
public class OrbitStateVector {

    public final ProductData.UTC time;
    public final double time_mjd;
    public double x_pos, y_pos, z_pos;
    public double x_vel, y_vel, z_vel;

    public OrbitStateVector(final ProductData.UTC t,
                            final double xpos, final double ypos, final double zpos,
                            final double xvel, final double yvel, final double zvel) {
        this.time = t;
        time_mjd = t.getMJD();
        this.x_pos = xpos;
        this.y_pos = ypos;
        this.z_pos = zpos;
        this.x_vel = xvel;
        this.y_vel = yvel;
        this.z_vel = zvel;
    }
}
