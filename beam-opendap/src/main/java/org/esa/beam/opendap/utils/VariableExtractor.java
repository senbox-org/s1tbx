package org.esa.beam.opendap.utils;

import opendap.dap.BaseType;
import opendap.dap.DArray;
import opendap.dap.DArrayDimension;
import opendap.dap.DConnect2;
import opendap.dap.DDS;
import opendap.dap.DGrid;
import org.esa.beam.opendap.datamodel.DAPVariable;
import org.esa.beam.opendap.datamodel.OpendapLeaf;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class VariableExtractor {

    public DAPVariable[] extractVariables(OpendapLeaf leaf) {
        DDS dds = getDDS(leaf.getDdsUri());
        return extractVariables(dds);
    }

    public DAPVariable[] extractVariables(DDS dds) {
        final Enumeration ddsVariables = dds.getVariables();
        final List<DAPVariable> dapVariables = new ArrayList<DAPVariable>();
        while (ddsVariables.hasMoreElements()) {
            final BaseType ddsVariable = (BaseType) ddsVariables.nextElement();
            DAPVariable dapVariable = convertToDAPVariable(ddsVariable);
            dapVariables.add(dapVariable);
        }
        return dapVariables.toArray(new DAPVariable[dapVariables.size()]);
    }

    private DDS getDDS(String uri) {
        InputStream stream = null;
        DConnect2 dConnect2 = null;
        DDS dds = new DDS();
        try {
            stream = new URI(uri).toURL().openStream();
            dConnect2 = new DConnect2(stream);
            dds = dConnect2.getDDS();
        } catch (Exception e) {
            // ok, no opendap leaf
        } finally {
            if (dConnect2 != null) {
                dConnect2.closeSession();
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
//                    ignore
                }
            }
        }
        return dds;
    }

    private static DAPVariable convertToDAPVariable(BaseType ddsVariable) {
        final DArray array;
        if (ddsVariable instanceof DGrid) {
            final DGrid grid = (DGrid) ddsVariable;
            array = grid.getArray();
        } else if (ddsVariable instanceof DArray) {
            array = (DArray) ddsVariable;
        } else {
            return convertToAtomicDAPVariable(ddsVariable);
        }

        final String name = ddsVariable.getEncodedName();
        final String typeName = ddsVariable.getTypeName();
        final String dataTypeName = getDataTypeName(array);
        final DArrayDimension[] dimensions = getDimensions(array);

        return new DAPVariable(name, typeName, dataTypeName, dimensions);
    }

    private static DAPVariable convertToAtomicDAPVariable(BaseType ddsVariable) {
        final String name = ddsVariable.getEncodedName();
        final String typeName = "atomic";
        final String dataTypeName = ddsVariable.getTypeName();
        final DArrayDimension[] dimensions = new DArrayDimension[0];

        return new DAPVariable(name, typeName, dataTypeName, dimensions);

    }

    private static String getDataTypeName(DArray array) {
        return array.getPrimitiveVector().getTemplate().getTypeName();
    }

    private static DArrayDimension[] getDimensions(DArray array) {
        final Enumeration dimensions = array.getDimensions();
        final List<DArrayDimension> dims = new ArrayList<DArrayDimension>();
        while (dimensions.hasMoreElements()) {
            DArrayDimension dimension = (DArrayDimension) dimensions.nextElement();
            dims.add(dimension);
        }
        return dims.toArray(new DArrayDimension[dims.size()]);
    }

}
