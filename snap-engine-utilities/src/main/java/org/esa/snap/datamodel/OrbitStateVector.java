package org.esa.snap.datamodel;

import org.esa.beam.framework.datamodel.ProductData;

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
