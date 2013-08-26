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
 * Configuration parameters for the snake plug-in
 *
 * @author Thomas
 * @since 11 May 2004
 * @author Emanuela Boros
 * @updated September 2012
 */
public class ActiveContourConfigurationDriver {

    private double dMaxDisplacement0;
    private double dMaxDisplacement1;
    private double dInvAlphaD0;
    private double dInvAlphaD1;
    private double dReg0;
    private double dReg1;
    private double dStep;

    /**
     * Constructor for the ActiveContourConfigurationDriver
     */
    public ActiveContourConfigurationDriver() {
        dMaxDisplacement0 = 2.0;
        dMaxDisplacement1 = 0.1;
        dInvAlphaD0 = 1.0 / 0.5;
        dInvAlphaD1 = 1.0 / 2.0;
        dReg0 = 2.0;
        dReg1 = 0.1;
        dStep = 0.99;
    }

    /**
     * Sets the maxDisplacement attribute of the
     * ActiveContourConfigurationDriver object
     *
     * @param min The new maxDisplacement value
     * @param max The new maxDisplacement value
     */
    public void setMaxDisplacement(double min, double max) {
        dMaxDisplacement1 = min;
        dMaxDisplacement0 = max;
    }

    /**
     * Sets the invAlphaD attribute of the ActiveContourConfigurationDriver
     * object
     *
     * @param min The new invAlphaD value
     * @param max The new invAlphaD value
     */
    public void setInvAlphaD(double min, double max) {
        dInvAlphaD1 = min;
        dInvAlphaD0 = max;
    }

    /**
     * Sets the reg attribute of the ActiveContourConfigurationDriver object
     *
     * @param min The new reg value
     * @param max The new reg value
     */
    public void setReg(double min, double max) {
        dReg1 = min;
        dReg0 = max;
    }

    /**
     * Sets the step attribute of the ActiveContourConfigurationDriver object
     *
     * @param s The new step value
     */
    public void setStep(double s) {
        dStep = s;
    }

    /**
     * Gets the step attribute of the ActiveContourConfigurationDriver object
     *
     * @return The step value
     */
    public double getStep() {
        return dStep;
    }

    /**
     * Gets the alphaD attribute of the ActiveContourConfigurationDriver object
     *
     * @param min Description of the Parameter
     * @return The alphaD value
     */
    public double getInvAlphaD(boolean min) {
        if (min) {
            return dInvAlphaD1;
        } else {
            return dInvAlphaD0;
        }
    }

    /**
     * Description of the Method
     *
     * @param min Description of the Parameter
     * @return Description of the Return Value
     */
    public double getMaxDisplacement(boolean min) {
        if (min) {
            return dMaxDisplacement1;
        } else {
            return dMaxDisplacement0;
        }
    }

    /**
     * Description of the Method
     *
     * @param min Description of the Parameter
     * @return Description of the Return Value
     */
    public double getReg(boolean min) {
        if (min) {
            return dReg1;
        } else {
            return dReg0;
        }
    }
}
