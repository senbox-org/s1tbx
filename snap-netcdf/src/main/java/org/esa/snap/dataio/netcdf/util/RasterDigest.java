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

package org.esa.snap.dataio.netcdf.util;

import ucar.ma2.DataType;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents an extract of all variables that could be converted to bands.
 */
public class RasterDigest {

    private final DimKey rasterDim;
    private final Variable[] variables;
    private ScaledVariable[] scaledVariables;


    public RasterDigest(DimKey rasterDim, Variable[] variables) {
        this(rasterDim, variables, new ScaledVariable[0]);
    }

    public RasterDigest(DimKey rasterDim, Variable[] variables, ScaledVariable[] scaledVariables) {
        this.rasterDim = rasterDim;
        this.variables = variables;
        this.scaledVariables = scaledVariables;
    }

    public DimKey getRasterDim() {
        return rasterDim;
    }

    public Variable[] getRasterVariables() {
        return variables;
    }

    public ScaledVariable[] getScaledVariables() {
        return scaledVariables;
    }

    public static RasterDigest createRasterDigest(final Group... groups) {
        Map<DimKey, List<Variable>> variableListMap = new HashMap<>();
        for (Group group : groups) {
            collectVariableLists(group, variableListMap);
        }
        if (variableListMap.isEmpty()) {
            return null;
        }
        final DimKey rasterDim = getBestRasterDim(variableListMap);
        final Variable[] rasterVariables = getRasterVariables(variableListMap, rasterDim);
        final ScaledVariable[] scaledVariables = getScaledVariables(variableListMap, rasterDim);
        return new RasterDigest(rasterDim, rasterVariables, scaledVariables);
    }

    private static ScaledVariable[] getScaledVariables(Map<DimKey, List<Variable>> variableListMap, DimKey rasterDim) {
        List<ScaledVariable> scaledVariableList = new ArrayList<>();
        for (DimKey dimKey : variableListMap.keySet()) {
            if (!dimKey.equals(rasterDim)) {
                double scaleX = getScale(dimKey.getDimensionX(), rasterDim.getDimensionX());
                double scaleY = getScale(dimKey.getDimensionY(), rasterDim.getDimensionY());
                if (scaleX == Math.round(scaleX) && scaleX == scaleY) {
                    List<Variable> variableList = variableListMap.get(dimKey);
                    for (Variable variable : variableList) {
                        scaledVariableList.add(new ScaledVariable((float) scaleX, variable));
                    }
                }
            }
        }
        return scaledVariableList.toArray(new ScaledVariable[scaledVariableList.size()]);
    }

    private static double getScale(Dimension scaledDim, Dimension rasterDim) {
        double length = scaledDim.getLength();
        double rasterLength = rasterDim.getLength();
        return rasterLength / length;
    }

    static Variable[] getRasterVariables(Map<DimKey, List<Variable>> variableLists,
                                         DimKey rasterDim) {
        final List<Variable> list = variableLists.get(rasterDim);
        return list.toArray(new Variable[list.size()]);
    }

    static DimKey getBestRasterDim(Map<DimKey, List<Variable>> variableListMap) {
        final Set<DimKey> ncRasterDims = variableListMap.keySet();
        if (ncRasterDims.size() == 0) {
            return null;
        }

        DimKey bestRasterDim = null;
        List<Variable> bestVarList = null;
        for (DimKey rasterDim : ncRasterDims) {
            if (rasterDim.isTypicalRasterDim()) {
                return rasterDim;
            }
            // Otherwise, we assume the best is the one which holds the most variables
            final List<Variable> varList = variableListMap.get(rasterDim);
            if (bestVarList == null || varList.size() > bestVarList.size()) {
                bestRasterDim = rasterDim;
                bestVarList = varList;
            }
        }

        return bestRasterDim;
    }

    static void collectVariableLists(Group group, Map<DimKey, List<Variable>> variableLists) {
        final List<Variable> variables = group.getVariables();
        for (final Variable variable : variables) {
            final int rank = variable.getRank();
            if (rank >= 2 && (DataTypeUtils.isValidRasterDataType(variable.getDataType()) || variable.getDataType() == DataType.LONG)) {
                DimKey rasterDim = new DimKey(variable.getDimensions().toArray(new Dimension[0]));
                final Dimension dimX = rasterDim.getDimensionX();
                final Dimension dimY = rasterDim.getDimensionY();
                if (dimX.getLength() > 1 && dimY.getLength() > 1) {
                    List<Variable> list = variableLists.get(rasterDim);
                    if (list == null) {
                        list = new ArrayList<>();
                        variableLists.put(rasterDim, list);
                    }
                    list.add(variable);
                }
            }
        }
        final List<Group> subGroups = group.getGroups();
        for (final Group subGroup : subGroups) {
            collectVariableLists(subGroup, variableLists);
        }
    }
}
