/*
 * $Id: Radiance2ReflectanceFactorCalibrator.java,v 1.1 2006/09/12 11:42:42 marcop Exp $
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

public class Radiance2ReflectanceFactorCalibrator implements RadianceCalibrator {

	private float conversionFactor;

	public Radiance2ReflectanceFactorCalibrator(double equivalentWidth,
			double solarIrradiance, double earthSunDistance) {
		conversionFactor = (float) (solarIrradiance / (100.0 * Math.PI
				* equivalentWidth * earthSunDistance * earthSunDistance));
	}

	@Override
    public float calibrate(float radiances) {
		return radiances / conversionFactor;
	}
	
	public float getConversionFactor() {
		return conversionFactor;
	}
}