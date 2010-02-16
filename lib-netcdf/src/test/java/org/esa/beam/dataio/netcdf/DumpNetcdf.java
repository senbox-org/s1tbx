package org.esa.beam.dataio.netcdf;

import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.List;

public class DumpNetcdf {
    public static void main(String[] args) throws IOException {
        final NetcdfFile netcdfFile = NetcdfFile.open(args[0]);
        dumpNetcdfFile(netcdfFile);
    }

    private static void dumpNetcdfFile(NetcdfFile netcdfFile) {
        System.out.println("NetcdfFile [" + netcdfFile.getLocation());
        dumpGroup(netcdfFile.getRootGroup(), "");
    }

    private static void dumpGroups(List<Group> groups, String indent) {
        for (Group group : groups) {
            dumpGroup(group, indent);
        }
    }

    private static void dumpGroup(Group group, String indent) {
        System.out.println(indent + "Group [" + group.getName() + "]");
        indent += "  ";
        dumpAttributes(group.getAttributes(), indent);
        dumpDimensions(group.getDimensions(), indent);
        dumpVariables(group.getVariables(), indent);
        dumpGroups(group.getGroups(), indent);
    }

    private static void dumpAttributes(List<Attribute> attributes, String indent) {
        for (Attribute attribute : attributes) {
            dumpAttribute(attribute, indent);
        }
    }

    private static void dumpAttribute(Attribute attribute, String indent) {
        System.out.println(indent + "Attribute [" + attribute.toString() + "]");
    }

    private static void dumpVariables(List<Variable> variableList, String indent) {
        for (Variable variable : variableList) {
            dumpVariable(variable, indent);
        }
    }

    private static void dumpVariable(Variable variable, String indent) {
        System.out.println(indent + "Variable [" + variable.getName() + " (" + variable.getDataType() + ")]");
        dumpDimensions(variable.getDimensions(), indent + "  ");
        dumpAttributes(variable.getAttributes(), indent + "  ");
    }

    private static void dumpDimensions(List<Dimension> dimensions, String indent) {
        for (Dimension dimension : dimensions) {
            dumpDimension(dimension, indent);
        }
    }

    private static void dumpDimension(Dimension dimension, String indent) {
        System.out.println(indent + "Dimension [" + dimension.getName() + "]");
    }
}
