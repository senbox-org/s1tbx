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
package org.esa.beam.framework.ui.diagram;

import java.io.Serializable;

import org.esa.beam.util.ObjectUtils;
import org.esa.beam.util.math.MathUtils;

/**
 * Represents an axis in a <code>{@link Diagram}</code>. By default an axis has no name, no units and a range set to
 * (0,100).
 */
public class DiagramAxis implements Serializable {

    private static final double[] _tickFactors = new double[]{1.0, 1.5, 2.0, 2.5, 4.0, 5.0, 7.5, 10.0};

    private Diagram diagram;
    private String name;
    private String unit;
    private double unitFactor;
    private double minValue;
    private double maxValue;
    private int numMajorTicks;
    private int numMinorTicks;
    private boolean isMinToMax;

    public DiagramAxis() {
        this(null, null);
    }

    public DiagramAxis(String name, String unit) {
        this.name = name;
        this.unit = unit;
        unitFactor = 1.0;
        minValue = 0.0;
        maxValue = 100.0;
        numMajorTicks = 3;
        numMinorTicks = 5;
        isMinToMax = true;
    }

    public String getName() {
        return name;
    }

    public Diagram getDiagram() {
        return diagram;
    }

    public void setDiagram(Diagram diagram) {
        this.diagram = diagram;
    }

    public void setName(String name) {
        if (!ObjectUtils.equalObjects(this.name, name)) {
            this.name = name;
            invalidate();
        }
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        if (!ObjectUtils.equalObjects(this.unit, unit)) {
            this.unit = unit;
            invalidate();
        }
    }

    public double getUnitFactor() {
        return unitFactor;
    }

    public void setUnitFactor(double unitFactor) {
        if (this.unitFactor != unitFactor) {
            this.unitFactor = unitFactor;
            invalidate();
        }
    }

    /**
     * Sets if Axis increases from min to max or decreases max to min
     * isMinToMax true if increases min to max
     */
    public void setMinToMax(final boolean isMinToMax) {
        this.isMinToMax = isMinToMax;
    }

    /**
     * Does Axis increase from min to max or decrease max to min
     * @return true if increases min to max
     */
    public boolean isMinToMax() {
        return isMinToMax;
    }

    public double getMinValue() {
        return minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public void setValueRange(double minValue, double maxValue) {
        if (minValue >= maxValue) {
            throw new IllegalArgumentException("minValue >= maxValue");
        }
        if (this.minValue != minValue || this.maxValue != maxValue) {
            this.minValue = minValue;
            this.maxValue = maxValue;
            invalidate();
        }
    }

    public int getNumMajorTicks() {
        return numMajorTicks;
    }

    public void setNumMajorTicks(int numMajorTicks) {
        if (numMajorTicks < 2) {
            throw new IllegalArgumentException("numMajorTicks < 2");
        }
        if (this.numMajorTicks != numMajorTicks) {
            this.numMajorTicks = numMajorTicks;
            invalidate();
        }
    }

    public int getNumMinorTicks() {
        return numMinorTicks;
    }

    public void setNumMinorTicks(int numMinorTicks) {
        if (numMinorTicks < 2) {
            throw new IllegalArgumentException("numMinorTicks < 2");
        }
        if (this.numMinorTicks != numMinorTicks) {
            this.numMinorTicks = numMinorTicks;
            invalidate();
        }
    }

    public double getMajorTickMarkDistance() {
        return (getMaxValue() - getMinValue()) / (getNumMajorTicks() - 1);
    }

    public void setSubDivision(double minValue, double maxValue, int numMajorTicks, int numMinorTicks) {
        setValueRange(minValue, maxValue);
        setNumMajorTicks(numMajorTicks);
        setNumMinorTicks(numMinorTicks);
    }

    public void setOptimalSubDivision(int numMajorTicksMin, int numMajorTicksMax, int numMinorTicks) {
        final double oldMinValue = minValue;
        final double oldMaxValue = maxValue;
        final double oldDelta = oldMaxValue - oldMinValue;
        double deltaDeltaMin = Double.MAX_VALUE;
        int numMajorTicksOpt = numMajorTicks;
        double newMinValueOpt = oldMinValue;
        double newMaxValueOpt = oldMaxValue;
        for (int numMajorTicks = numMajorTicksMin; numMajorTicks <= numMajorTicksMax; numMajorTicks++) {
            final double tickDist = getOptimalTickDistance(oldMinValue, oldMaxValue, numMajorTicks);
            final double newMinValue = adjustFloor(oldMinValue, tickDist);
            final double newMaxValue = adjustCeil(oldMaxValue, tickDist);
            final double newDelta = newMaxValue - newMinValue;
            final double deltaDelta = Math.abs(newDelta - oldDelta);
            if (deltaDelta < deltaDeltaMin) {
                deltaDeltaMin = deltaDelta;
                numMajorTicksOpt = numMajorTicks;
                newMinValueOpt = newMinValue;
                newMaxValueOpt = newMaxValue;
            }
        }
        setSubDivision(newMinValueOpt, newMaxValueOpt, numMajorTicksOpt, numMinorTicks);
    }

    public static double getOptimalTickDistance(double minValue, double maxValue, int numMajorTicks) {
        if (minValue >= maxValue) {
            throw new IllegalArgumentException("minValue >= maxValue");
        }
        if (numMajorTicks < 2) {
            throw new IllegalArgumentException("numMajorTicks < 2");
        }

        final double tickDist = (maxValue - minValue) / (numMajorTicks - 1);
        final double oom = MathUtils.getOrderOfMagnitude(tickDist);
        final double scale = Math.pow(10.0, oom);

        double tickDistOpt = 0.0;
        for (double tickFactor : _tickFactors) {
            tickDistOpt = tickFactor * scale;
            if (tickDistOpt >= tickDist) {
                break;
            }
        }

        return tickDistOpt;
    }

    private static double adjustCeil(double x, double dx) {
        return Math.ceil(x / dx) * dx;
    }

    private static double adjustFloor(double x, double dx) {
        return Math.floor(x / dx) * dx;
    }

    public String[] createTickmarkTexts() {
        double roundFactor = MathUtils.computeRoundFactor(getMinValue(), getMaxValue(), 3);
        return createTickmarkTexts(getMinValue(), getMaxValue(), getNumMajorTicks(), roundFactor);
    }

    private static String[] createTickmarkTexts(double min, double max, int n, double roundFactor) {
        String[] texts = new String[n];
        double x;
        long xi;
        for (int i = 0; i < n; i++) {
            x = min + i * (max - min) / (n - 1);
            x = MathUtils.round(x, roundFactor);
            xi = (long) Math.floor(x);
            if (x == xi) {
                texts[i] = String.valueOf(xi);
            } else {
                texts[i] = String.valueOf(x);
            }
        }
        return texts;
    }

    private void invalidate() {
        if (diagram != null) {
            diagram.invalidate();
        }
    }
}
