/*
 * $Id: Radiance2TemperatureCalibrator.java,v 1.1 2006/09/12 11:42:42 marcop Exp $
 *
 * Copyright (C) 2006 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.dataio.avhrr.calibration;

/**
 * Computes blackbody temperatures TE for the thermal AVHRR channels 3b, 4 and 5
 * from the radiance
 */
public class Radiance2TemperatureCalibrator implements RadianceCalibrator {
	private static final double C1 = 1.1910427E-5;

	private static final double C2 = 1.4387752;

	private double constant1;

	private double constant2;

	private double c2vc;

	private double c1vc3;

	public Radiance2TemperatureCalibrator(double constant1, double constant2,
			double vc) {
		this.constant1 = constant1;
		this.constant2 = constant2;
		this.c2vc = C2 * vc;
		this.c1vc3 = C1 * vc * vc * vc;
	}

	@Override
    public float calibrate(float radiances) {
		final double teStar = c2vc / (Math.log(1.0 + (c1vc3 / radiances)));
		final double te = constant1 + constant2 * teStar;
		return (float) te;
	}
}