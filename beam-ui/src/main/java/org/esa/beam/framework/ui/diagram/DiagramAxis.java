/*
 * $Id: DiagramAxis.java,v 1.1 2006/10/10 14:47:36 norman Exp $
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
package org.esa.beam.framework.ui.diagram;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;

import org.esa.beam.util.ObjectUtils;
import org.esa.beam.util.math.MathUtils;

/**
 * Represents an axis in a <code>{@link Diagram}</code>. By default an axis has no name, no units and a range set to
 * (0,100).
 */
public class DiagramAxis implements Serializable {

    private static final double[] _tickFactors = new double[]{1.0, 1.5, 2.0, 2.5, 4.0, 5.0, 7.5, 10.0};

    private String _name;
    private String _unit;
    private double _unitFactor;
    private double _minValue;
    private double _maxValue;
    private int _numMajorTicks;
    private int _numMinorTicks;
    private PropertyChangeSupport _propertyChangeSupport;

    public DiagramAxis() {
        this(null, null);
    }

    public DiagramAxis(String name, String unit) {
        _name = name;
        _unit = unit;
        _unitFactor = 1.0;
        _minValue = 0.0;
        _maxValue = 100.0;
        _numMajorTicks = 3;
        _numMinorTicks = 5;
        _propertyChangeSupport = new PropertyChangeSupport(this);
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        String oldValue = _name;
        if (!ObjectUtils.equalObjects(oldValue, name)) {
            _name = name;
            _propertyChangeSupport.firePropertyChange("name", oldValue, _name);
        }
    }

    public String getUnit() {
        return _unit;
    }

    public void setUnit(String unit) {
        String oldValue = _unit;
        if (!ObjectUtils.equalObjects(oldValue, unit)) {
            _unit = unit;
            _propertyChangeSupport.firePropertyChange("unit", oldValue, _unit);
        }
    }

    public double getUnitFactor() {
        return _unitFactor;
    }

    public void setUnitFactor(double unitFactor) {
        double oldValue = _unitFactor;
        if (oldValue != unitFactor) {
            _unitFactor = unitFactor;
            _propertyChangeSupport.firePropertyChange("unitFactor", oldValue, _unitFactor);
        }
    }

    public double getMinValue() {
        return _minValue;
    }

    private void setMinValue(double minValue) {
        double oldMinValue = _minValue;
        if (oldMinValue != minValue) {
            _minValue = minValue;
            _propertyChangeSupport.firePropertyChange("minValue", oldMinValue, _minValue);
        }
    }

    public double getMaxValue() {
        return _maxValue;
    }

    private void setMaxValue(double maxValue) {
        double oldMaxValue = _maxValue;
        if (oldMaxValue != maxValue) {
            _maxValue = maxValue;
            _propertyChangeSupport.firePropertyChange("maxValue", oldMaxValue, _maxValue);
        }
    }

    public void setValueRange(double minValue, double maxValue) {
        if (minValue >= maxValue) {
            throw new IllegalArgumentException("minValue >= maxValue");
        }
        setMinValue(minValue);
        setMaxValue(maxValue);
    }

    public int getNumMajorTicks() {
        return _numMajorTicks;
    }

    public void setNumMajorTicks(int numMajorTicks) {
        if (numMajorTicks < 2) {
            throw new IllegalArgumentException("numMajorTicks < 2");
        }
        int oldValue = _numMajorTicks;
        if (oldValue != numMajorTicks) {
            _numMajorTicks = numMajorTicks;
            _propertyChangeSupport.firePropertyChange("numMajorTicks", oldValue, _numMajorTicks);
        }
    }

    public int getNumMinorTicks() {
        return _numMinorTicks;
    }

    public void setNumMinorTicks(int numMinorTicks) {
        if (numMinorTicks < 2) {
            throw new IllegalArgumentException("numMinorTicks < 2");
        }
        int oldValue = _numMinorTicks;
        if (oldValue != numMinorTicks) {
            _numMinorTicks = numMinorTicks;
            _propertyChangeSupport.firePropertyChange("numMinorTicks", oldValue, _numMinorTicks);
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
        final double oldMinValue = _minValue;
        final double oldMaxValue = _maxValue;
        final double oldDelta = oldMaxValue - oldMinValue;
        double deltaDeltaMin = Double.MAX_VALUE;
        int numMajorTicksOpt = _numMajorTicks;
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

    public void addPropertyChangeListener(PropertyChangeListener l) {
        _propertyChangeSupport.addPropertyChangeListener(l);
    }

    public void addPropertyChangeListener(String name, PropertyChangeListener l) {
        _propertyChangeSupport.addPropertyChangeListener(name, l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        _propertyChangeSupport.removePropertyChangeListener(l);
    }

    public void removePropertyChangeListener(String name, PropertyChangeListener l) {
        _propertyChangeSupport.removePropertyChangeListener(name, l);
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
        for (int i = 0; i < _tickFactors.length; i++) {
            tickDistOpt = _tickFactors[i] * scale;
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
}
