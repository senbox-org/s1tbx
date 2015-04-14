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

package org.esa.snap.dataio.netcdf.metadata.profiles.hdfeos;

import org.jdom2.Element;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.iosp.hdf4.ODLparser;

import java.io.IOException;
import java.util.StringTokenizer;

/**
 * Utility function to deal with the HDF-EOS products.
 */
public class HdfEosUtils {
    static final String STRUCT_METADATA = "StructMetadata";
    static final String CORE_METADATA = "CoreMetadata";
    static final String ARCHIVE_METADATA = "ArchiveMetadata";

    public static Element getEosElement(String name, Group eosGroup) throws IOException {
        String smeta = getEosMetadata(name, eosGroup);
        if (smeta == null) {
            return null;
        }
        smeta = smeta.replaceAll("\\s+=\\s+", "=");
        smeta = smeta.replaceAll("\\?", "_"); // XML names cannot contain the character "?".

        StringBuilder sb = new StringBuilder(smeta.length());
        StringTokenizer lineFinder = new StringTokenizer(smeta, "\t\n\r\f");
        while (lineFinder.hasMoreTokens()) {
            String line = lineFinder.nextToken().trim();
            sb.append(line);
            sb.append("\n");
        }

        ODLparser parser = new ODLparser();
        return parser.parseFromString(sb.toString());// now we have the ODL in JDOM elements
    }

   private static String getEosMetadata(String name, Group eosGroup) throws IOException {
      StringBuilder sbuff = null;
      String structMetadata = null;

      int n = 0;
      while (true) {
        Variable structMetadataVar = eosGroup.findVariable(name + "." + n);
        if (structMetadataVar == null) {
            break;
        }
        if ((structMetadata != null) && (sbuff == null)) { // more than 1 StructMetadata
          sbuff = new StringBuilder(64000);
          sbuff.append(structMetadata);
        }

        Array metadataArray = structMetadataVar.read();
        structMetadata = ((ArrayChar) metadataArray).getString(); // common case only StructMetadata.0, avoid extra copy

        if (sbuff != null) {
          sbuff.append(structMetadata);
        }
        n++;
      }
      return (sbuff != null) ? sbuff.toString() : structMetadata;
    }

    // look for a group with the given name. recurse into subgroups if needed. breadth first
    static Group findGroupNested(Group parent, String name) {

      for (Group g : parent.getGroups()) {
        if (g.getShortName().equals(name))
          return g;
      }
      for (Group g : parent.getGroups()) {
        Group result = findGroupNested(g, name);
        if (result != null)
          return result;
      }
      return null;
    }

    static String getValue(Element root, String... childs) {
        Element element = root;
        int index = 0;
        while (element != null && index < childs.length) {
            String childName = childs[index++];
            element = element.getChild(childName);
        }
        return element != null ? element.getValue() : null;

    }

}
