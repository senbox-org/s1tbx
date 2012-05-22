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

package org.esa.beam.binning.operator;

import java.util.Arrays;

/**
 * Configuration of a binning aggregator.
 *
 * @author Norman Fomferra
 * @see org.esa.beam.binning.Aggregator
 */
public class AggregatorConfig {
    private String type;

    private String varName;

    private String[] varNames;

    private Integer percentage;

    private Double weightCoeff;

    private Float fillValue;

    public AggregatorConfig() {
    }

    public AggregatorConfig(String type) {
        this.type = type;
    }

    public String getAggregatorName() {
        return type;
    }

    public void setAggregatorName(String aggregatorName) {
        type = aggregatorName;
    }

    public String getVarName() {
        return varName;
    }

    public void setVarName(String varName) {
        this.varName = varName;
    }

    public String[] getVarNames() {
        return varNames;
    }

    public void setVarNames(String[] varNames) {
        this.varNames = varNames;
    }

    public Integer getPercentage() {
        return percentage;
    }

    public void setPercentage(Integer percentage) {
        this.percentage = percentage;
    }

    public Double getWeightCoeff() {
        return weightCoeff;
    }

    public void setWeightCoeff(Double weightCoeff) {
        this.weightCoeff = weightCoeff;
    }

    public Float getFillValue() {
        return fillValue;
    }

    public void setFillValue(Float fillValue) {
        this.fillValue = fillValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AggregatorConfig that = (AggregatorConfig) o;

        if (fillValue != null ? !fillValue.equals(that.fillValue) : that.fillValue != null) return false;
        if (percentage != null ? !percentage.equals(that.percentage) : that.percentage != null) return false;
        if (!type.equals(that.type)) return false;
        if (varName != null ? !varName.equals(that.varName) : that.varName != null) return false;
        if (!Arrays.equals(varNames, that.varNames)) return false;
        if (weightCoeff != null ? !weightCoeff.equals(that.weightCoeff) : that.weightCoeff != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + (varName != null ? varName.hashCode() : 0);
        result = 31 * result + (varNames != null ? Arrays.hashCode(varNames) : 0);
        result = 31 * result + (percentage != null ? percentage.hashCode() : 0);
        result = 31 * result + (weightCoeff != null ? weightCoeff.hashCode() : 0);
        result = 31 * result + (fillValue != null ? fillValue.hashCode() : 0);
        return result;
    }
}
