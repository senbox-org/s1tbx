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
package org.esa.beam.processor.smac;

/**
 * Interface for classes implementing a sensor coefficient set for the <code>SmacAlgorithm</code>.
 *
 * @deprecated since BEAM 4.11. No replacement.
 */
@Deprecated
interface SmacSensorCoefficients {

    /**
     * Retrieves the gaseous absorption coefficient for h20.
     */
    double getAh2o();

    /**
     * Retrieves the gaseous absorption exponent for h2o.
     */
    double getNh2o();

    /**
     * Retrieves the gaseous absorption coefficient for o3.
     */
    double getAo3();

    /**
     * Retrieves the gaseous absorption exponent for o3.
     */
    double getNo3();

    /**
     * Retrieves the gaseous absorption coefficient for o2.
     */
    double getAo2();

    /**
     * Retrieves the gaseous absorption exponent for o2.
     */
    double getNo2();

    /**
     * Retrieves the gaseous transmission coefficient for o2.
     */
    double getPo2();

    /**
     * Retrieves the gaseous absorption coefficient for co2.
     */
    double getAco2();

    /**
     * Retrieves the gaseous absorption exponent for co2.
     */
    double getNco2();

    /**
     * Retrieves the gaseous transmission coefficient for co2.
     */
    double getPco2();

    /**
     * Retrieves the gaseous absorption coefficient for ch4.
     */
    double getAch4();

    /**
     * Retrieves the gaseous absorption exponent for ch4.
     */
    double getNch4();

    /**
     * Retrieves the gaseous transmission coefficient for ch4.
     */
    double getPch4();

    /**
     * Retrieves the gaseous absorption coefficient for no2.
     */
    double getAno2();

    /**
     * Retrieves the gaseous absorption exponent for no2.
     */
    double getNno2();

    /**
     * Retrieves the gaseous transmission coefficient for no2.
     */
    double getPno2();

    /**
     * Retrieves the gaseous absorption coefficient for co2.
     */
    double getAco();

    /**
     * Retrieves the gaseous absorption exponent for co2.
     */
    double getNco();

    /**
     * Retrieves the gaseous transmission coefficient for co.
     */
    double getPco();

    /**
     * Retrieves the spherical albedo coefficient 0.
     */
    double getA0s();

    /**
     * Retrieves the spherical albedo coefficient 1.
     */
    double getA1s();

    /**
     * Retrieves the spherical albedo coefficient 2.
     */
    double getA2s();

    /**
     * Retrieves the spherical albedo coefficient 3.
     */
    double getA3s();

    /**
     * Retrieves the scattering transmission coefficient 0.
     */
    double getA0T();

    /**
     * Retrieves the scattering transmission coefficient 1.
     */
    double getA1T();

    /**
     * Retrieves the scattering transmission coefficient 2.
     */
    double getA2T();

    /**
     * Retrieves the scattering transmission coefficient 3.
     */
    double getA3T();

    /**
     * Retrieves the molecular optical depth.
     */
    double getTaur();

    double getSr();

    /**
     * Retrieves aerosol optical depth coefficient 0.
     */
    double getA0taup();

    /**
     * Retrieves aerosol optical depth coefficient 1.
     */
    double getA1taup();

    double getWo();

    double getGc();

    /**
     * Retrieves aerosol reflectance coefficient 0.
     */
    double getA0P();

    /**
     * Retrieves aerosol reflectance coefficient 1.
     */
    double getA1P();

    /**
     * Retrieves aerosol reflectance coefficient 2.
     */
    double getA2P();

    /**
     * Retrieves aerosol reflectance coefficient 3.
     */
    double getA3P();

    /**
     * Retrieves aerosol reflectance coefficient 4.
     */
    double getA4P();

    /**
     * Retrieves the residual transmission coefficient 1.
     */
    double getRest1();

    /**
     * Retrieves the residual transmission coefficient 2.
     */
    double getRest2();

    /**
     * Retrieves the residual transmission coefficient 3.
     */
    double getRest3();

    /**
     * Retrieves the residual transmission coefficient 4.
     */
    double getRest4();

    /**
     * Retrieves the residual rayleigh coefficient 1.
     */
    double getResr1();

    /**
     * Retrieves the residual rayleigh coefficient 2.
     */
    double getResr2();

    /**
     * Retrieves the residual rayleigh coefficient 3.
     */
    double getResr3();

    /**
     * Retrieves the residual aerosol coefficient 1.
     */
    double getResa1();

    /**
     * Retrieves the residual aerosol coefficient 2.
     */
    double getResa2();

    /**
     * Retrieves the residual aerosol coefficient 3.
     */
    double getResa3();

    /**
     * Retrieves the residual aerosol coefficient 4.
     */
    double getResa4();
}