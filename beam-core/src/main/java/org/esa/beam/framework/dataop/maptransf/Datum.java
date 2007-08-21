/*
 * $Id: Datum.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.dataop.maptransf;


/**
 * Represents a geodetic datum. Geodetic datums define the size and shape of the earth and the origin and orientation of
 * the coordinate systems used to map the earth.
 */
public class Datum implements Cloneable {

    /**
     * The standard WGS-72 datum.
     */
    public static final Datum WGS_72 = new Datum("WGS-72", Ellipsoid.WGS_72, 0.0, 0.0, 5.0);

    /**
     * The standard WGS-84 datum.
     */
    public static final Datum WGS_84 = new Datum("WGS-84", Ellipsoid.WGS_84, 0.0, 0.0, 0.0);

    /**
     * The ITRF-97 datum.
     */
    public static final Datum ITRF_97 = new Datum("ITRF-97", Ellipsoid.GRS_80, 0.0, 0.0, 0.0);



    private final String _name;
    private final Ellipsoid _ellipsoid;
    private final double _dx;
    private final double _dy;
    private final double _dz;

    public Datum(String name, Ellipsoid ellipsoid, double dx, double dy, double dz) {
        _name = name;
        _ellipsoid = ellipsoid;
        _dx = dx;
        _dy = dy;
        _dz = dz;
    }

    public String getName() {
        return _name;
    }

    public Ellipsoid getEllipsoid() {
        return _ellipsoid;
    }


    public double getDX() {
        return _dx;
    }

    public double getDY() {
        return _dy;
    }

    public double getDZ() {
        return _dz;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }
}
