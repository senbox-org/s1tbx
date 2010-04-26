package org.esa.beam.dataio.netcdf4.convention.hdfeos;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.jdom.Element;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.iosp.hdf4.ODLparser;

import java.io.IOException;
import java.util.StringTokenizer;


public class HdfEosUtils {
    static final String STRUCT_METADATA = "StructMetadata";
    static final String CORE_METADATA = "CoreMetadata";
    static final String ARCHIVE_METADATA = "ArchiveMetadata";

    static Element getEosElement(String name, Group eosGroup) throws IOException {
        String smeta = getEosMetadata(name, eosGroup);
        smeta = smeta.replaceAll("\\s+=\\s+", "=");

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
        if (structMetadataVar == null) break;
        if ((structMetadata != null) && (sbuff == null)) { // more than 1 StructMetadata
          sbuff = new StringBuilder(64000);
          sbuff.append(structMetadata);
        }

        // read and parse the ODL
        Array A = structMetadataVar.read();
        ArrayChar ca = (ArrayChar) A;
        structMetadata = ca.getString(); // common case only StructMetadata.0, avoid extra copy

        if (sbuff != null)
          sbuff.append(structMetadata);
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
        System.out.println("HdfEosUtils.getValue");
        Element element = root;
        int index = 0;
        while (element != null && index < childs.length) {
            String childName = childs[index++];
            System.out.println("childName = " + childName);
            element = element.getChild(childName);
        }
        return element != null ? element.getValue() : null;

    }

}
