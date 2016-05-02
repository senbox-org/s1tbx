/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.classification.gpf;

import java.io.Serializable;

/**
 * This class encapsulates a classifier and its associated information to be saved to a file.
 */
public class ClassifierDescriptor implements Serializable {

    private String classifierType;
    private String filename;
    private Object object;
    private double[] sortedClassValues;
    private String className;
    private String classUnit;
    private String[] featureNames;
    private double[] featureMinValues;
    private double[] featureMaxValues;
    private boolean doClassValQuantization;
    private double minClassValue;
    private double classValStepSize;
    private int classLevels;
    private String[] polygonsAsClasses;

    ClassifierDescriptor(final String classifierType, final String filename, final Object object,
                         final double[] sortedClassValues, final String className, final String classUnit,
                         final String[] featureNames,
                         final double[] featureMinValues, final double[] featureMaxValues,
                         final boolean doClassValQuantization, final double minClassValue,
                         final double classValStepSize, final int classLevels,
                         final String[] polygonsAsClasses) {
        this.classifierType = classifierType;
        this.filename = filename;
        this.object = object;
        this.sortedClassValues = sortedClassValues;
        this.className = className;
        this.classUnit = classUnit;
        this.featureNames = featureNames;
        this.featureMinValues = featureMinValues;
        this.featureMaxValues = featureMaxValues;
        this.doClassValQuantization = doClassValQuantization;
        this.minClassValue = minClassValue;
        this.classValStepSize = classValStepSize;
        this.classLevels = classLevels;
        this.polygonsAsClasses = polygonsAsClasses;
    }

    public String getClassifierType() {
        return classifierType;
    }

    public String getFilename() {
        return filename;
    }

    public Object getObject() {
        return object;
    }

    public double[] getSortedClassValues() {
        return sortedClassValues;
    }

    public String getClassName() {
        return className;
    }

    public String getClassUnit() { return classUnit; }

    public String[] getFeatureNames() {
        return featureNames;
    }

    public double[] getFeatureMinValues() {
        return featureMinValues;
    }

    public double[] getFeatureMaxValues() {
        return featureMaxValues;
    }

    public boolean getDoClassValQuantization() { return doClassValQuantization; }

    public double getMinClassValue() { return minClassValue; }

    public double getClassValStepSize() { return classValStepSize; }

    public int getClassLevels() { return classLevels; }

    public String[] getPolygonsAsClasses() { return polygonsAsClasses; }
}
