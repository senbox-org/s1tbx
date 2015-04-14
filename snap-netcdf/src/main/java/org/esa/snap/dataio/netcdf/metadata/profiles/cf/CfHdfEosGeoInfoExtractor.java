package org.esa.snap.dataio.netcdf.metadata.profiles.cf;

import ucar.nc2.Attribute;

import java.util.List;
import java.util.StringTokenizer;

/**
 * Class to extract HDF originated geo info
 *
 * @author olafd
 */
public class CfHdfEosGeoInfoExtractor {

    private List<Attribute> netcdfAttributes;

    private int xDim = -1;
    private int yDim = -1;
    private double ulLon;
    private double ulLat;
    private double lrLon;
    private double lrLat;
    private String projection;

    public CfHdfEosGeoInfoExtractor(List<Attribute> netcdfAttributes) {
        this.netcdfAttributes = netcdfAttributes;
    }

    public void extractInfo() throws NumberFormatException {
        Attribute structMetadataAttr = null;
        for (Attribute att : netcdfAttributes) {
            if (att.getShortName().startsWith("StructMetadata")) {
                structMetadataAttr = att;
                break;
            }
        }

        final String structMetadataString = structMetadataAttr.getValue(0).toString();
        final String[] strings = structMetadataString.split("\n");
        for (String string : strings) {
            string = string.replaceAll("\t", "");
            System.out.println(string);
            final StringTokenizer st = new StringTokenizer(string, "\n", false);
            while (st.hasMoreTokens()) {
                final String s = st.nextToken();
                System.out.println("s = " + s);
                final String[] sSplit = s.split("=");
                if (sSplit != null && sSplit.length == 2) {
                    final String sArgString = sSplit[0];
                    final String sValString = sSplit[1];
                    if (sArgString.equalsIgnoreCase("projection")) {
                        projection = sValString;
                    } else if (sArgString.equalsIgnoreCase("xdim")) {
                        try {
                            xDim = Integer.parseInt(sValString);
                        } catch (NumberFormatException e) {
                            System.out.println("Cannot extract XDIM value: " + e.getMessage());
                        }
                    } else if (sArgString.equalsIgnoreCase("ydim")) {
                        try {
                            yDim = Integer.parseInt(sValString);
                        } catch (NumberFormatException e) {
                            System.out.println("Cannot extract YDIM value: " + e.getMessage());
                        }
                    } else if (sArgString.toLowerCase().contains("upperleft")) {
                        try {
                            final String[] ulNumberStrings = sValString.substring(1, sValString.length() - 1).split(",");
                            ulLon = Double.parseDouble(ulNumberStrings[0]);
                            ulLat = Double.parseDouble(ulNumberStrings[1]);
                        } catch (NumberFormatException e) {
                            System.out.println("Cannot extract UPPERLEFT values: " + e.getMessage());
                        }
                    } else if (sArgString.toLowerCase().contains("lowerright")) {
                        try {
                            final String[] lrNumberStrings = sValString.substring(1, sValString.length() - 1).split(",");
                            lrLon = Double.parseDouble(lrNumberStrings[0]);
                            lrLat = Double.parseDouble(lrNumberStrings[1]);
                        } catch (NumberFormatException e) {
                            System.out.println("Cannot extract LOWERRIGHT values: " + e.getMessage());
                        }
                    }
                }
            }
        }
    }

    public int getxDim() {
        return xDim;
    }

    public int getyDim() {
        return yDim;
    }

    public double getUlLon() {
        return ulLon;
    }

    public double getUlLat() {
        return ulLat;
    }

    public double getLrLon() {
        return lrLon;
    }

    public double getLrLat() {
        return lrLat;
    }

    public String getProjection() {
        return projection;
    }
}
