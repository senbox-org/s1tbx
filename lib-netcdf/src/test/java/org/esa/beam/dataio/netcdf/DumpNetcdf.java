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
