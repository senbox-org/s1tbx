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

import java.util.Comparator;

/**
 storage for orbit state vectors
 */
public class Orbits {

    public final static class OrbitVector {
        public double utcMJD;
        public double xPos;
        public double yPos;
        public double zPos;
        public double xVel;
        public double yVel;
        public double zVel;

        public OrbitVector(final double utcMJD) {
            this.utcMJD = utcMJD;
        }

        public OrbitVector(final double utcMJD,
                           final double xPos, final double yPos, final double zPos,
                           final double xVel, final double yVel, final double zVel) {
            this.utcMJD = utcMJD;
            this.xPos = xPos;
            this.yPos = yPos;
            this.zPos = zPos;
            this.xVel = xVel;
            this.yVel = yVel;
            this.zVel = zVel;
        }
    }

    public static class OrbitComparator implements Comparator<OrbitVector> {
        @Override
        public int compare(OrbitVector osv1, OrbitVector osv2) {
            if (osv1.utcMJD < osv2.utcMJD) {
                return -1;
            } else if (osv1.utcMJD > osv2.utcMJD) {
                return 1;
            } else {
                return 0;
            }
        }
    };
}
