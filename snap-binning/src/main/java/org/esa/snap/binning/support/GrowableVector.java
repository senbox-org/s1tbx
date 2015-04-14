/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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


package org.esa.snap.binning.support;

import org.esa.snap.binning.Vector;

import java.util.Arrays;

/**
 * A growable vector.
 *
 * @author Norman Fomferra
 */
public final class GrowableVector implements Vector {
    private float[] elements;
    private int size;

    public GrowableVector(int capacity) {
        this.elements = new float[capacity];
        this.size = 0;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public float get(int index) {
        return elements[index];
    }

    public float[] getElements() {
        return Arrays.copyOfRange(elements, 0, size);
    }

    public void add(float element) {
        if (size >= elements.length) {
            float[] temp = new float[(elements.length * 3) / 2 + 2];
            System.arraycopy(elements, 0, temp, 0, size);
            elements = temp;
        }
        elements[size++] = element;
    }

    @Override
    public String toString() {
        return Arrays.toString(getElements());
    }

}
