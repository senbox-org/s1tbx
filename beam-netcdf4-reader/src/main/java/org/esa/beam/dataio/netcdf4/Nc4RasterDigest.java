package org.esa.beam.dataio.netcdf4;

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
public class Nc4RasterDigest {

    private final Nc4Dim _rasterDim;
    private final Variable[] _variables;
    private final Map<Nc4Dim, List<Variable>> rasters;


    public Nc4RasterDigest(Nc4Dim rasterDim, Variable[] variables) {
        _rasterDim = rasterDim;
        _variables = variables;
        rasters = new HashMap<Nc4Dim, List<Variable>>();
    }

    public Nc4Dim getRasterDim() {
        return _rasterDim;
    }

    public Variable[] getRasterVariables() {
        return _variables;
    }

    public static Nc4RasterDigest createRasterDigest(final Group group, final Nc4ReaderParameters rv) {
        Map<Nc4Dim, List<Variable>> variableListMap = getVariableListMap(group);
        if (variableListMap.isEmpty()) {
            return null;
        }
        final Nc4Dim rasterDim = getBestRasterDim(variableListMap);
        final Variable[] rasterVariables = getRasterVariables(variableListMap, rasterDim);
        return new Nc4RasterDigest(rasterDim, rasterVariables);
    }

    static Variable[] getRasterVariables(Map<Nc4Dim, List<Variable>> variableLists,
                                         Nc4Dim rasterDim) {
        final List<Variable> list = variableLists.get(rasterDim);
        return list.toArray(new Variable[list.size()]);
    }

    static Nc4Dim getBestRasterDim(Map<Nc4Dim, List<Variable>> variableListMap) {
        final Set<Nc4Dim> ncRasterDims = variableListMap.keySet();
        if (ncRasterDims.size() == 0) {
            return null;
        }

        Nc4Dim bestRasterDim = null;
        List<Variable> bestVarList = null;
        for (Nc4Dim rasterDim : ncRasterDims) {
            if (rasterDim.isTypicalRasterDim()) {
                return rasterDim;
            }
            // Otherwise, the best is the one which holds the most variables
            //  todo se -- Why not that list with the bigest dimensions? Which cover the most pixels.? 
            final List<Variable> varList = variableListMap.get(rasterDim);
            if (bestVarList == null || varList.size() > bestVarList.size()) {
                bestRasterDim = rasterDim;
                bestVarList = varList;
            }
        }

        return bestRasterDim;
    }

    static Map<Nc4Dim, List<Variable>> getVariableListMap(final Group group) {
        Map<Nc4Dim, List<Variable>> variableLists = new HashMap<Nc4Dim, List<Variable>>();
        collectVariableLists(group, variableLists);
        return variableLists;
    }

    static void collectVariableLists(Group group, Map<Nc4Dim, List<Variable>> variableLists) {
        final List<Variable> variables = group.getVariables();
        for (final Variable variable : variables) {
            final int rank = variable.getRank();
            if (rank >= 2 && Nc4ReaderUtils.isValidRasterDataType(variable.getDataType())) {
                final Dimension dimX = variable.getDimension(rank - 1);
                final Dimension dimY = variable.getDimension(rank - 2);
                if (dimX.getLength() > 1 && dimY.getLength() > 1) {
                    Nc4Dim rasterDim = new Nc4Dim(new Dimension[]{dimX, dimY});
                    List<Variable> list = variableLists.get(rasterDim);
                    if (list == null) {
                        list = new ArrayList<Variable>();
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
