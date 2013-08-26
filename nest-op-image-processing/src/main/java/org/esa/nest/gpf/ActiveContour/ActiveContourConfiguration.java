/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.gpf.ActiveContour;

/**
 * Configuration parameters for the ActiveContour plug-in
 *
 * @author Thomas
 * @since 11 May 2004
 * @author Emanuela Boros
 * @updated September 2012
 */
public class ActiveContourConfiguration {

    private double gradThreshold;
    private double maxDisplacement;
    private double maxSearch;
    private double regMin;
    private double regMax;
    private double alphaDeriche;

    /**
     * Construct the active contour configuration
     *
     * @param gradThreshold the gradient threshold
     * @param maxDisplacement the maximum displacement
     * @param maxSearch maximum search
     * @param regMin the minimum reg
     * @param regMax the maximum reg
     * @param alphaDeriche alpha parameter for Deriche filtering
     */
    public ActiveContourConfiguration(double gradThreshold, double maxDisplacement,
            double maxSearch, double regMin, double regMax, double alphaDeriche) {
        this.gradThreshold = gradThreshold;
        this.maxDisplacement = maxDisplacement;
        this.maxSearch = maxSearch;
        this.regMin = regMin;
        this.regMax = regMax;
        this.alphaDeriche = alphaDeriche;
    }

    /**
     * Constructor for the ActiveContourConfiguration object
     *
     * @param configuration Description of the Parameter
     */
    public ActiveContourConfiguration(ActiveContourConfiguration configuration) {
        gradThreshold = configuration.getGradThreshold();
        maxDisplacement = configuration.getMaxDisplacement();
        maxSearch = configuration.getMaxSearch();
        regMin = configuration.getRegMin();
        regMax = configuration.getRegMax();
        alphaDeriche = configuration.getAlpha();
    }

    /**
     * Gets the gradThreshold attribute of the ActiveContourConfiguration object
     *
     * @return The gradThreshold value
     */
    public double getGradThreshold() {
        return gradThreshold;
    }

    /**
     * Gets the maxDisplacement attribute of the ActiveContourConfiguration
     * object
     *
     * @return The maxDisplacement value
     */
    public double getMaxDisplacement() {
        return maxDisplacement;
    }

    /**
     * Gets the maxSearch attribute of the ActiveContourConfiguration object
     *
     * @return The maxSearch value
     */
    public double getMaxSearch() {
        return maxSearch;
    }

    /**
     * Gets the regMin attribute of the ActiveContourConfiguration object
     *
     * @return The regMin value
     */
    public double getRegMin() {
        return regMin;
    }

    /**
     * Gets the regMax attribute of the ActiveContourConfiguration object
     *
     * @return The regMax value
     */
    public double getRegMax() {
        return regMax;
    }

    /**
     * Gets the alpha attribute of the ActiveContourConfiguration object
     *
     * @return The alpha value
     */
    public double getAlpha() {
        return alphaDeriche;
    }

    /**
     * Update parameters
     *
     * @param multiply the parameter of the update
     */
    public void update(double multiply) {
        alphaDeriche /= multiply;
        regMax *= multiply;
        regMin *= multiply;
    }
}
