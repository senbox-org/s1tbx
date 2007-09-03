/*
 * $Id: DimapProductHelpersTest.java,v 1.1.1.1 2006/09/11 08:16:51 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.dataio.dimap;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.FXYGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.MapGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.dataop.maptransf.Ellipsoid;
import org.esa.beam.framework.dataop.maptransf.LambertConformalConicDescriptor;
import org.esa.beam.framework.dataop.maptransf.MapInfo;
import org.esa.beam.framework.dataop.maptransf.MapProjection;
import org.esa.beam.framework.dataop.maptransf.MapTransform;
import org.esa.beam.framework.dataop.maptransf.MapTransformDescriptor;
import org.esa.beam.framework.dataop.resamp.Resampling;
import org.esa.beam.util.ArrayUtils;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.math.FXYSum;
import org.jdom.Document;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Vector;

public class DimapProductHelpersTest extends TestCase {

    private final String _ls = SystemUtils.LS;
    private final String _projectionName = "ProjectionName";
    private final String _mapUnit = "mapUnit";
    private final String _ellipsoidName = "EllipsoidName";
    private final double _semiMinor = 1234.0;
    private final double _semiMajor = 5678.0;
    private final double[] _expValues = new double[]{_semiMajor, _semiMinor, 15, 16, 17, 18, 19};
    private final String _datumName = "DatumName";
    private final float _pixelX = 3.2f;
    private final float _pixelY = 4.3f;
    private final float _orientation = 11.12f;
    private final float _easting = 5.4f;
    private final float _northing = 6.5f;
    private final float _pixelSizeX = 7.6f;
    private final float _pixelSizeY = 8.7f;
    private final double _noDataValue = 1e-3;
    private final boolean _orthorectified = false;
    private final String _elevModelName = "DEM";
    private final String _typeId = LambertConformalConicDescriptor.TYPE_ID;
    private final int _sceneWidth = 200;
    private final int _sceneHeight = 300;
    private final boolean _sceneFitted = true;
    private final String _resamplingName = Resampling.NEAREST_NEIGHBOUR.getName();
    private final String[] _paramUnit = LambertConformalConicDescriptor.PARAMETER_UNITS;
    private final String[] _paramNames = LambertConformalConicDescriptor.PARAMETER_NAMES;

    private final int[] _notMandatoryLines = new int[]{2, 4, 5, 7, 25, 29, 33, 37, 41, 45, 49};

    private final String[] _xmlMapGeocodingStringStyleV1_4_0 = new String[]{
        "<" + DimapProductConstants.TAG_ROOT + ">" + _ls,
        /*  1 */ "    <Coordinate_Reference_System>" + _ls,
        /*  2 */ "        <GEO_TABLES version=\"1.0\">CUSTOM</GEO_TABLES>" + _ls, // notMandatory
        /*  3 */ "        <Horizontal_CS>" + _ls,
        /*  4 */ "            <HORIZONTAL_CS_TYPE>PROJECTED</HORIZONTAL_CS_TYPE>" + _ls, // notMandatory
        /*  5 */ "            <HORIZONTAL_CS_NAME>" + _projectionName + "</HORIZONTAL_CS_NAME>" + _ls, // notMandatory
        /*  6 */ "            <Geographic_CS>" + _ls,
        /*  7 */ "                <GEOGRAPHIC_CS_NAME>" + _projectionName + "</GEOGRAPHIC_CS_NAME>" + _ls, // notMandatory
        /*  8 */ "                <Horizontal_Datum>" + _ls,
        /*  9 */ "                    <HORIZONTAL_DATUM_NAME>" + _datumName + "</HORIZONTAL_DATUM_NAME>" + _ls,
        /* 10 */ "                    <Ellipsoid>" + _ls,
        /*  1 */ "                        <ELLIPSOID_NAME>" + _ellipsoidName + "</ELLIPSOID_NAME>" + _ls,
        /*  2 */ "                        <Ellipsoid_Parameters>" + _ls,
        /*  3 */ "                            <ELLIPSOID_MAJ_AXIS unit=\"M\">" + _semiMajor + "</ELLIPSOID_MAJ_AXIS>" + _ls,
        /*  4 */ "                            <ELLIPSOID_MIN_AXIS unit=\"M\">" + _semiMinor + "</ELLIPSOID_MIN_AXIS>" + _ls,
        /*  5 */ "                        </Ellipsoid_Parameters>" + _ls,
        /*  6 */ "                    </Ellipsoid>" + _ls,
        /*  7 */ "                </Horizontal_Datum>" + _ls,
        /*  8 */ "            </Geographic_CS>" + _ls,
        /*  9 */ "            <Projection>" + _ls,
        /* 20 */ "                <NAME>" + _projectionName + "</NAME>" + _ls,
        /*  1 */ "                <Projection_CT_Method>" + _ls,
        /*  2 */ "                    <PROJECTION_CT_NAME>" + _typeId + "</PROJECTION_CT_NAME>" + _ls,
        /*  3 */ "                    <Projection_Parameters>" + _ls,
        /*  4 */ "                        <Projection_Parameter>" + _ls,
        /*  5 */ "                            <PROJECTION_PARAMETER_NAME>" + _paramNames[0] + "</PROJECTION_PARAMETER_NAME>" + _ls, // notMandatory
        /*  6 */ "                            <PROJECTION_PARAMETER_VALUE unit=\"" + _paramUnit[0] + "\">" + _expValues[0] + "</PROJECTION_PARAMETER_VALUE>" + _ls,
        /*  7 */ "                        </Projection_Parameter>" + _ls,
        /*  8 */ "                        <Projection_Parameter>" + _ls,
        /*  9 */ "                            <PROJECTION_PARAMETER_NAME>" + _paramNames[1] + "</PROJECTION_PARAMETER_NAME>" + _ls, // notMandatory
        /* 30 */ "                            <PROJECTION_PARAMETER_VALUE unit=\"" + _paramUnit[1] + "\">" + _expValues[1] + "</PROJECTION_PARAMETER_VALUE>" + _ls,
        /*  1 */ "                        </Projection_Parameter>" + _ls,
        /*  2 */ "                        <Projection_Parameter>" + _ls,
        /*  3 */ "                            <PROJECTION_PARAMETER_NAME>" + _paramNames[2] + "</PROJECTION_PARAMETER_NAME>" + _ls, // notMandatory
        /*  4 */ "                            <PROJECTION_PARAMETER_VALUE unit=\"" + _paramUnit[2] + "\">" + _expValues[2] + "</PROJECTION_PARAMETER_VALUE>" + _ls,
        /*  5 */ "                        </Projection_Parameter>" + _ls,
        /*  6 */ "                        <Projection_Parameter>" + _ls,
        /*  7 */ "                            <PROJECTION_PARAMETER_NAME>" + _paramNames[3] + "</PROJECTION_PARAMETER_NAME>" + _ls, // notMandatory
        /*  8 */ "                            <PROJECTION_PARAMETER_VALUE unit=\"" + _paramUnit[3] + "\">" + _expValues[3] + "</PROJECTION_PARAMETER_VALUE>" + _ls,
        /*  9 */ "                        </Projection_Parameter>" + _ls,
        /* 40 */ "                        <Projection_Parameter>" + _ls,
        /*  1 */ "                            <PROJECTION_PARAMETER_NAME>" + _paramNames[4] + "</PROJECTION_PARAMETER_NAME>" + _ls, // notMandatory
        /*  2 */ "                            <PROJECTION_PARAMETER_VALUE unit=\"" + _paramUnit[4] + "\">" + _expValues[4] + "</PROJECTION_PARAMETER_VALUE>" + _ls,
        /*  3 */ "                        </Projection_Parameter>" + _ls,
        /*  4 */ "                        <Projection_Parameter>" + _ls,
        /*  5 */ "                            <PROJECTION_PARAMETER_NAME>" + _paramNames[5] + "</PROJECTION_PARAMETER_NAME>" + _ls, // notMandatory
        /*  6 */ "                            <PROJECTION_PARAMETER_VALUE unit=\"" + _paramUnit[5] + "\">" + _expValues[5] + "</PROJECTION_PARAMETER_VALUE>" + _ls,
        /*  7 */ "                        </Projection_Parameter>" + _ls,
        /*  8 */ "                        <Projection_Parameter>" + _ls,
        /*  9 */ "                            <PROJECTION_PARAMETER_NAME>" + _paramNames[6] + "</PROJECTION_PARAMETER_NAME>" + _ls, // notMandatory
        /* 50 */ "                            <PROJECTION_PARAMETER_VALUE unit=\"" + _paramUnit[6] + "\">" + _expValues[6] + "</PROJECTION_PARAMETER_VALUE>" + _ls,
        /*  1 */ "                        </Projection_Parameter>" + _ls,
        /*  2 */ "                    </Projection_Parameters>" + _ls,
        /*  3 */ "                </Projection_CT_Method>" + _ls,
        /*  4 */ "            </Projection>" + _ls,
        /*  5 */ "            <MAP_INFO>" + _ls +
        /*  5 */ "                <PIXEL_X value=\"" + _pixelX + "\" />" + _ls,
        /*  6 */ "                <PIXEL_Y value=\"" + _pixelY + "\" />" + _ls,
        /*  7 */ "                <EASTING value=\"" + _easting + "\" />" + _ls,
        /*  8 */ "                <NORTHING value=\"" + _northing + "\" />" + _ls,
        /*  9 */ "                <ORIENTATION value=\"" + _orientation + "\" />" + _ls,
        /* 60 */ "                <PIXELSIZE_X value=\"" + _pixelSizeX + "\" />" + _ls,
        /*  1 */ "                <PIXELSIZE_Y value=\"" + _pixelSizeY + "\" />" + _ls,
        /*  2 */ "                <NODATA_VALUE value=\"" + _noDataValue + "\" />" + _ls,
        /*  3 */ "                <MAPUNIT value=\"" + _mapUnit + "\" />" + _ls,
        /*  4 */ "                <ORTHORECTIFIED value=\"" + _orthorectified + "\" />" + _ls,
        /*  5 */ "                <ELEVATION_MODEL value=\"" + _elevModelName + "\" />" + _ls,
        /*  6 */ "                <SCENE_FITTED value=\"" + _sceneFitted + "\" />" + _ls,
        /*  7 */ "                <SCENE_WIDTH value=\"" + _sceneWidth + "\" />" + _ls,
        /*  8 */ "                <SCENE_HEIGHT value=\"" + _sceneHeight + "\" />" + _ls,
        /*  9 */ "                <RESAMPLING value=\"" + _resamplingName + "\" />" + _ls,
        /* 70 */ "            </MAP_INFO>" + _ls,
        /*  1 */ "        </Horizontal_CS>" + _ls,
        /*  2 */ "    </Coordinate_Reference_System>" + _ls,
        /*  3 */ "</" + DimapProductConstants.TAG_ROOT + ">" + _ls
    };

    // fields special for FXYGeoCoding
    private final Datum _datum = Datum.WGS_84;
    private final double[] _xCoefficients = new double[]{0, 1, 2};
    private final double[] _yCoefficients = new double[]{3, 4, 5};
    private final double[] _latCoefficients = new double[]{6, 7, 8};
    private final double[] _lonCoefficients = new double[]{9, 10, 11};
    private final FXYGeoCoding _fxyGeoCoding = new FXYGeoCoding(_pixelX, _pixelY, _pixelSizeX, _pixelSizeY,
                                                                FXYSum.createFXYSum(1, _xCoefficients),
                                                                FXYSum.createFXYSum(1, _yCoefficients),
                                                                FXYSum.createFXYSum(1, _latCoefficients),
                                                                FXYSum.createFXYSum(1, _lonCoefficients),
                                                                _datum);

    private final String _xmlFXYGeoCodingString =
            "<" + DimapProductConstants.TAG_ROOT + ">" + _ls +
            "    <Coordinate_Reference_System>" + _ls +
            "        <Horizontal_CS>" + _ls +
            "            <HORIZONTAL_CS_TYPE>GEOGRAPHIC</HORIZONTAL_CS_TYPE>" + _ls +
            "            <Geographic_CS>" + _ls +
            "                <Horizontal_Datum>" + _ls +
            "                    <HORIZONTAL_DATUM_NAME>" + _datum.getName() + "</HORIZONTAL_DATUM_NAME>" + _ls +
            "                    <Ellipsoid>" + _ls +
            "                        <ELLIPSOID_NAME>" + _datum.getEllipsoid().getName() + "</ELLIPSOID_NAME>" + _ls +
            "                        <Ellipsoid_Parameters>" + _ls +
            "                            <ELLIPSOID_MAJ_AXIS unit=\"M\">" + _datum.getEllipsoid().getSemiMajor() + "</ELLIPSOID_MAJ_AXIS>" + _ls +
            "                            <ELLIPSOID_MIN_AXIS unit=\"M\">" + _datum.getEllipsoid().getSemiMinor() + "</ELLIPSOID_MIN_AXIS>" + _ls +
            "                        </Ellipsoid_Parameters>" + _ls +
            "                    </Ellipsoid>" + _ls +
            "                </Horizontal_Datum>" + _ls +
            "            </Geographic_CS>" + _ls +
            "        </Horizontal_CS>" + _ls +
            "    </Coordinate_Reference_System>" + _ls +
            "    <Geoposition>" + _ls +
            "        <Geoposition_Insert>" + _ls +
            "            <ULXMAP unit=\"M\">" + _pixelX + "</ULXMAP>" + _ls +
            "            <ULYMAP unit=\"M\">" + _pixelY + "</ULYMAP>" + _ls +
            "            <XDIM unit=\"M\">" + _pixelSizeX + "</XDIM>" + _ls +
            "            <YDIM unit=\"M\">" + _pixelSizeY + "</YDIM>" + _ls +
            "        </Geoposition_Insert>" + _ls +
            "        <Simplified_Location_Model>" + _ls +
            "            <Direct_Location_Model order=\"" + _fxyGeoCoding.getPixelXFunction().getOrder() + "\">" + _ls +
            "                <lc_List>" + _ls +
            "                    <lc index=\"0\">" + _lonCoefficients[0] + "</lc>" + _ls +
            "                    <lc index=\"1\">" + _lonCoefficients[1] + "</lc>" + _ls +
            "                    <lc index=\"2\">" + _lonCoefficients[2] + "</lc>" + _ls +
            "                </lc_List>" + _ls +
            "                <pc_List>" + _ls +
            "                    <pc index=\"0\">" + _latCoefficients[0] + "</pc>" + _ls +
            "                    <pc index=\"1\">" + _latCoefficients[1] + "</pc>" + _ls +
            "                    <pc index=\"2\">" + _latCoefficients[2] + "</pc>" + _ls +
            "                </pc_List>" + _ls +
            "            </Direct_Location_Model>" + _ls +
            "            <Reverse_Location_Model order=\"" + _fxyGeoCoding.getLatFunction().getOrder() + "\">" + _ls +
            "                <ic_List>" + _ls +
            "                    <ic index=\"0\">" + _xCoefficients[0] + "</ic>" + _ls +
            "                    <ic index=\"1\">" + _xCoefficients[1] + "</ic>" + _ls +
            "                    <ic index=\"2\">" + _xCoefficients[2] + "</ic>" + _ls +
            "                </ic_List>" + _ls +
            "                <jc_List>" + _ls +
            "                    <jc index=\"0\">" + _yCoefficients[0] + "</jc>" + _ls +
            "                    <jc index=\"1\">" + _yCoefficients[1] + "</jc>" + _ls +
            "                    <jc index=\"2\">" + _yCoefficients[2] + "</jc>" + _ls +
            "                </jc_List>" + _ls +
            "            </Reverse_Location_Model>" + _ls +
            "        </Simplified_Location_Model>" + _ls +
            "    </Geoposition>" + _ls +
            "</" + DimapProductConstants.TAG_ROOT + ">";

    private final double[] _xCoefficients1 = new double[]{0, 1, 2};
    private final double[] _yCoefficients1 = new double[]{3, 4, 5};
    private final double[] _latCoefficients1 = new double[]{6, 7, 8};
    private final double[] _lonCoefficients1 = new double[]{9, 10, 11};
    private final FXYGeoCoding _fxyGeoCoding1 = new FXYGeoCoding(_pixelX, _pixelY, _pixelSizeX, _pixelSizeY,
                                                                 FXYSum.createFXYSum(1, _xCoefficients1),
                                                                 FXYSum.createFXYSum(1, _yCoefficients1),
                                                                 FXYSum.createFXYSum(1, _latCoefficients1),
                                                                 FXYSum.createFXYSum(1, _lonCoefficients1),
                                                                 _datum);
    private final double[] _xCoefficients2 = new double[]{12, 13, 14};
    private final double[] _yCoefficients2 = new double[]{15, 16, 17};
    private final double[] _latCoefficients2 = new double[]{18, 19, 20};
    private final double[] _lonCoefficients2 = new double[]{21, 22, 23};
    private final FXYGeoCoding _fxyGeoCoding2 = new FXYGeoCoding(_pixelX, _pixelY, _pixelSizeX, _pixelSizeY,
                                                                 FXYSum.createFXYSum(1, _xCoefficients2),
                                                                 FXYSum.createFXYSum(1, _yCoefficients2),
                                                                 FXYSum.createFXYSum(1, _latCoefficients2),
                                                                 FXYSum.createFXYSum(1, _lonCoefficients2),
                                                                 _datum);

    private final String _xmlBandedFXYGeoCodingString =
            "<" + DimapProductConstants.TAG_ROOT + ">" + _ls +
            "    <Coordinate_Reference_System>" + _ls +
            "        <Horizontal_CS>" + _ls +
            "            <HORIZONTAL_CS_TYPE>GEOGRAPHIC</HORIZONTAL_CS_TYPE>" + _ls +
            "            <Geographic_CS>" + _ls +
            "                <Horizontal_Datum>" + _ls +
            "                    <HORIZONTAL_DATUM_NAME>" + _datum.getName() + "</HORIZONTAL_DATUM_NAME>" + _ls +
            "                    <Ellipsoid>" + _ls +
            "                        <ELLIPSOID_NAME>" + _datum.getEllipsoid().getName() + "</ELLIPSOID_NAME>" + _ls +
            "                        <Ellipsoid_Parameters>" + _ls +
            "                            <ELLIPSOID_MAJ_AXIS unit=\"M\">" + _datum.getEllipsoid().getSemiMajor() + "</ELLIPSOID_MAJ_AXIS>" + _ls +
            "                            <ELLIPSOID_MIN_AXIS unit=\"M\">" + _datum.getEllipsoid().getSemiMinor() + "</ELLIPSOID_MIN_AXIS>" + _ls +
            "                        </Ellipsoid_Parameters>" + _ls +
            "                    </Ellipsoid>" + _ls +
            "                </Horizontal_Datum>" + _ls +
            "            </Geographic_CS>" + _ls +
            "        </Horizontal_CS>" + _ls +
            "    </Coordinate_Reference_System>" + _ls +
            "    <Geoposition>" + _ls +
            "        <BAND_INDEX>0</BAND_INDEX>" + _ls +
            "        <Geoposition_Insert>" + _ls +
            "             <ULXMAP>" + _pixelX + "</ULXMAP>" + _ls +
            "             <ULYMAP>" + _pixelY + "</ULYMAP>" + _ls +
            "             <XDIM>" + _pixelSizeX + "</XDIM>" + _ls +
            "             <YDIM>" + _pixelSizeY + "</YDIM>" + _ls +
            "        </Geoposition_Insert>" + _ls +
            "         <Simplified_Location_Model>" + _ls +
            "             <Direct_Location_Model order=\"" + _fxyGeoCoding1.getPixelXFunction().getOrder() + "\">" + _ls +
            "                <lc_List>" + _ls +
            "                    <lc index=\"0\">" + _lonCoefficients1[0] + "</lc>" + _ls +
            "                    <lc index=\"1\">" + _lonCoefficients1[1] + "</lc>" + _ls +
            "                    <lc index=\"2\">" + _lonCoefficients1[2] + "</lc>" + _ls +
            "                </lc_List>" + _ls +
            "                <pc_List>" + _ls +
            "                    <pc index=\"0\">" + _latCoefficients1[0] + "</pc>" + _ls +
            "                    <pc index=\"1\">" + _latCoefficients1[1] + "</pc>" + _ls +
            "                    <pc index=\"2\">" + _latCoefficients1[2] + "</pc>" + _ls +
            "                </pc_List>" + _ls +
            "            </Direct_Location_Model>" + _ls +
            "            <Reverse_Location_Model order=\"" + _fxyGeoCoding1.getLatFunction().getOrder() + "\">" + _ls +
            "                <ic_List>" + _ls +
            "                    <ic index=\"0\">" + _xCoefficients1[0] + "</ic>" + _ls +
            "                    <ic index=\"1\">" + _xCoefficients1[1] + "</ic>" + _ls +
            "                    <ic index=\"2\">" + _xCoefficients1[2] + "</ic>" + _ls +
            "                </ic_List>" + _ls +
            "                <jc_List>" + _ls +
            "                    <jc index=\"0\">" + _yCoefficients1[0] + "</jc>" + _ls +
            "                    <jc index=\"1\">" + _yCoefficients1[1] + "</jc>" + _ls +
            "                    <jc index=\"2\">" + _yCoefficients1[2] + "</jc>" + _ls +
            "                </jc_List>" + _ls +
            "            </Reverse_Location_Model>" + _ls +
            "        </Simplified_Location_Model>" + _ls +
            "    </Geoposition>" + _ls +
            "    <Geoposition>" + _ls +
            "        <BAND_INDEX>1</BAND_INDEX>" + _ls +
            "        <Geoposition_Insert>" + _ls +
            "             <ULXMAP unit=\"M\">" + _pixelX + "</ULXMAP>" + _ls +
            "             <ULYMAP unit=\"M\">" + _pixelY + "</ULYMAP>" + _ls +
            "             <XDIM unit=\"M\">" + _pixelSizeX + "</XDIM>" + _ls +
            "             <YDIM unit=\"M\">" + _pixelSizeY + "</YDIM>" + _ls +
            "        </Geoposition_Insert>" + _ls +
            "         <Simplified_Location_Model>" + _ls +
            "             <Direct_Location_Model order=\"" + _fxyGeoCoding2.getPixelXFunction().getOrder() + "\">" + _ls +
            "                <lc_List>" + _ls +
            "                    <lc index=\"0\">" + _lonCoefficients2[0] + "</lc>" + _ls +
            "                    <lc index=\"1\">" + _lonCoefficients2[1] + "</lc>" + _ls +
            "                    <lc index=\"2\">" + _lonCoefficients2[2] + "</lc>" + _ls +
            "                </lc_List>" + _ls +
            "                <pc_List>" + _ls +
            "                    <pc index=\"0\">" + _latCoefficients2[0] + "</pc>" + _ls +
            "                    <pc index=\"1\">" + _latCoefficients2[1] + "</pc>" + _ls +
            "                    <pc index=\"2\">" + _latCoefficients2[2] + "</pc>" + _ls +
            "                </pc_List>" + _ls +
            "            </Direct_Location_Model>" + _ls +
            "            <Reverse_Location_Model order=\"" + _fxyGeoCoding2.getLatFunction().getOrder() + "\">" + _ls +
            "                <ic_List>" + _ls +
            "                    <ic index=\"0\">" + _xCoefficients2[0] + "</ic>" + _ls +
            "                    <ic index=\"1\">" + _xCoefficients2[1] + "</ic>" + _ls +
            "                    <ic index=\"2\">" + _xCoefficients2[2] + "</ic>" + _ls +
            "                </ic_List>" + _ls +
            "                <jc_List>" + _ls +
            "                    <jc index=\"0\">" + _yCoefficients2[0] + "</jc>" + _ls +
            "                    <jc index=\"1\">" + _yCoefficients2[1] + "</jc>" + _ls +
            "                    <jc index=\"2\">" + _yCoefficients2[2] + "</jc>" + _ls +
            "                </jc_List>" + _ls +
            "            </Reverse_Location_Model>" + _ls +
            "        </Simplified_Location_Model>" + _ls +
            "    </Geoposition>" + _ls +
            "</" + DimapProductConstants.TAG_ROOT + ">";


    private Product _product;

    protected void setUp() throws Exception {
        _product = new Product("product", "type", 200, 300);
        _product.addBand("b1", ProductData.TYPE_INT8);
        _product.addBand("b2", ProductData.TYPE_INT8);
    }

    protected void tearDown() throws Exception {
    }

    public void testCreateGeoCodingForMapProjectionWithOldDimapFormat() {
        final String projectionName = "Lambert Conformal Conic";

        final double pixelX = 1.0;
        final double pixelY = 2.0;
        final double easting = 3.0;
        final double northing = 4.0;
        final double pixelSizeX = 5.0;
        final double pixelSizeY = 6.0;
        final String datumName = "WGS-84";
        final int sceneWidth = 140;
        final int sceneHeight = 232;

        final String xmlMapGeocodingStringStyleOldFormat =
                "<" + DimapProductConstants.TAG_ROOT + ">" + _ls +
                /*  1 */ "    <Coordinate_Reference_System>" + _ls +
                /*  2 */ "        <Geocoding_Map>" + _ls +
                /*  3 */ "            <MAP_INFO>" + projectionName + ", " + pixelX + ", " + pixelY + ", " + easting + ", " + northing + ", " + pixelSizeX + ", " + pixelSizeY + ", " + datumName + ", units=meter, " + sceneWidth + ", " + sceneHeight + "</MAP_INFO>" + _ls +
                /*  4 */ "        </Geocoding_Map>" + _ls +
                /*  5 */ "    </Coordinate_Reference_System>" + _ls +
                /*  6 */ "</" + DimapProductConstants.TAG_ROOT + ">" + _ls;

        final Ellipsoid expEllipsoid = Ellipsoid.WGS_84;
        final LambertConformalConicDescriptor expDescriptor = new LambertConformalConicDescriptor();
        final MapTransform expTransform = expDescriptor.createTransform(null);
        final Document dom = DimapProductHelpers.createDom(
                new ByteArrayInputStream(xmlMapGeocodingStringStyleOldFormat.getBytes()));

        //test
        GeoCoding[] geoCodings = DimapProductHelpers.createGeoCoding(dom, _product);
        assertNotNull(geoCodings);
        assertEquals(1, geoCodings.length);

        final GeoCoding geoCoding = geoCodings[0];

        assertNotNull(geoCoding);
        assertNotNull(geoCoding);
        assertEquals(MapGeoCoding.class, geoCoding.getClass());
        final MapGeoCoding mapGeoCoding = (MapGeoCoding) geoCoding;

        final MapInfo actualMapinfo = mapGeoCoding.getMapInfo();
        assertEquals(pixelX, actualMapinfo.getPixelX(), 1e-6);
        assertEquals(pixelY, actualMapinfo.getPixelY(), 1e-6);
        assertEquals(easting, actualMapinfo.getEasting(), 1e-6);
        assertEquals(northing, actualMapinfo.getNorthing(), 1e-6);
        assertEquals(pixelSizeX, actualMapinfo.getPixelSizeX(), 1e-6);
        assertEquals(pixelSizeY, actualMapinfo.getPixelSizeY(), 1e-6);
        assertEquals(sceneWidth, actualMapinfo.getSceneWidth());
        assertEquals(sceneHeight, actualMapinfo.getSceneHeight());

        final Datum actualDatum = actualMapinfo.getDatum();
        assertEquals(datumName, actualDatum.getName());

        final MapProjection actualProjection = actualMapinfo.getMapProjection();
        assertEquals(projectionName, actualProjection.getName());

        final Ellipsoid actualEllipsoid = actualDatum.getEllipsoid();
        assertEquals(expEllipsoid.getName(), actualEllipsoid.getName());
        assertEquals(expEllipsoid.getSemiMajor(), actualEllipsoid.getSemiMajor(), 1e-10);
        assertEquals(expEllipsoid.getSemiMinor(), actualEllipsoid.getSemiMinor(), 1e-10);

        final MapTransform actualTransform = actualProjection.getMapTransform();
        assertEquals(expTransform.getClass(), actualTransform.getClass());

        final double[] actualValues = actualTransform.getParameterValues();
        assertEquals(true, Arrays.equals(expTransform.getParameterValues(), actualValues));

        final MapTransformDescriptor actualDescriptor = actualTransform.getDescriptor();
        assertEquals(expDescriptor.getClass(), actualDescriptor.getClass());
    }

    public void testCreateGeoCodingForMapProjectionWithFullValidDimap_1_4_0_Format() {
        final int[] allLines = new int[0];

        final String fullXMLString = createXMLString(allLines);
        final Document dom = DimapProductHelpers.createDom(new ByteArrayInputStream(fullXMLString.getBytes()));
        final GeoCoding geoCoding = DimapProductHelpers.createGeoCoding(dom, _product)[0];

        assertNotNull(geoCoding);
        assertEquals(MapGeoCoding.class, geoCoding.getClass());
        final MapGeoCoding mapGeoCoding = (MapGeoCoding) geoCoding;

        final MapInfo actualMapinfo = mapGeoCoding.getMapInfo();
        assertEquals(_pixelX, actualMapinfo.getPixelX(), 1e-6);
        assertEquals(_pixelY, actualMapinfo.getPixelY(), 1e-6);
        assertEquals(_easting, actualMapinfo.getEasting(), 1e-6);
        assertEquals(_northing, actualMapinfo.getNorthing(), 1e-6);
        assertEquals(_pixelSizeX, actualMapinfo.getPixelSizeX(), 1e-6);
        assertEquals(_pixelSizeY, actualMapinfo.getPixelSizeY(), 1e-6);
        assertEquals(_sceneWidth, actualMapinfo.getSceneWidth());
        assertEquals(_sceneHeight, actualMapinfo.getSceneHeight());

        final Datum actualDatum = actualMapinfo.getDatum();
        assertEquals(_datumName, actualDatum.getName());

        final MapProjection actualProjection = actualMapinfo.getMapProjection();
        assertEquals(_projectionName, actualProjection.getName());
        assertEquals(_mapUnit, actualProjection.getMapUnit());

        final Ellipsoid actualEllipsoid = actualDatum.getEllipsoid();
        assertEquals(_ellipsoidName, actualEllipsoid.getName());
        assertEquals(_semiMajor, actualEllipsoid.getSemiMajor(), 1e-10);
        assertEquals(_semiMinor, actualEllipsoid.getSemiMinor(), 1e-10);

        final MapTransform actualTransform = actualProjection.getMapTransform();
        assertEquals(LambertConformalConicDescriptor.LCCT.class, actualTransform.getClass());

        final double[] actualValues = actualTransform.getParameterValues();
        for (int i = 0; i < actualValues.length; i++) {
            assertEquals(_expValues[i], actualValues[i], 1e-5);
        }

        final MapTransformDescriptor actualDescriptor = actualTransform.getDescriptor();
        assertEquals(LambertConformalConicDescriptor.class, actualDescriptor.getClass());
    }

    public void testCreateGeoCodingForMapProjectionWithValidDimap_1_4_0_Format() {

        final String xmlStringWithoutNotMandatoryLines = createXMLString(_notMandatoryLines);
        final byte[] bytes = xmlStringWithoutNotMandatoryLines.getBytes();
        final Document dom = DimapProductHelpers.createDom(new ByteArrayInputStream(bytes));
        final GeoCoding geoCoding = DimapProductHelpers.createGeoCoding(dom, _product)[0];

        assertNotNull(geoCoding);
        assertEquals(MapGeoCoding.class, geoCoding.getClass());
        final MapGeoCoding mapGeoCoding = (MapGeoCoding) geoCoding;

        final MapInfo actualMapinfo = mapGeoCoding.getMapInfo();
        assertEquals(_pixelX, actualMapinfo.getPixelX(), 1e-6);
        assertEquals(_pixelY, actualMapinfo.getPixelY(), 1e-6);
        assertEquals(_easting, actualMapinfo.getEasting(), 1e-6);
        assertEquals(_northing, actualMapinfo.getNorthing(), 1e-6);
        assertEquals(_pixelSizeX, actualMapinfo.getPixelSizeX(), 1e-6);
        assertEquals(_pixelSizeY, actualMapinfo.getPixelSizeY(), 1e-6);
        assertEquals(_sceneWidth, actualMapinfo.getSceneWidth());
        assertEquals(_sceneHeight, actualMapinfo.getSceneHeight());

        final Datum actualDatum = actualMapinfo.getDatum();
        assertEquals(_datumName, actualDatum.getName());

        final MapProjection actualProjection = actualMapinfo.getMapProjection();
        assertEquals(_projectionName, actualProjection.getName());
        assertEquals(_mapUnit, actualProjection.getMapUnit());

        final Ellipsoid actualEllipsoid = actualDatum.getEllipsoid();
        assertEquals(_ellipsoidName, actualEllipsoid.getName());
        assertEquals(_semiMajor, actualEllipsoid.getSemiMajor(), 1e-10);
        assertEquals(_semiMinor, actualEllipsoid.getSemiMinor(), 1e-10);

        final MapTransform actualTransform = actualProjection.getMapTransform();
        assertEquals(LambertConformalConicDescriptor.LCCT.class, actualTransform.getClass());

        final double[] actualValues = actualTransform.getParameterValues();
        assertEquals(true, Arrays.equals(_expValues, actualValues));

        final MapTransformDescriptor actualDescriptor = actualTransform.getDescriptor();
        assertEquals(LambertConformalConicDescriptor.class, actualDescriptor.getClass());
    }

    public void testCreateGeoCodingForMapProjectionWithInvalidDimap_1_4_0_Format() {
        Document dom;

        // without ellipsoid name Element
        dom = createDom(withoutMandatoryLines(new int[]{11}));
        assertEquals(null, DimapProductHelpers.createGeoCoding(dom, _product));

        // without ellipsoid major axis Element
        dom = createDom(withoutMandatoryLines(new int[]{13}));
        assertEquals(null, DimapProductHelpers.createGeoCoding(dom, _product));

        // without ellipsoid minor axis Element
        dom = createDom(withoutMandatoryLines(new int[]{14}));
        assertEquals(null, DimapProductHelpers.createGeoCoding(dom, _product));

        // without ellipsoid parameters Element
        dom = createDom(withoutMandatoryLines(ArrayUtils.createIntArray(12, 15)));
        assertEquals(null, DimapProductHelpers.createGeoCoding(dom, _product));

        // without ellipsoid Element
        dom = createDom(withoutMandatoryLines(ArrayUtils.createIntArray(10, 16)));
        assertEquals(null, DimapProductHelpers.createGeoCoding(dom, _product));

        // without horizontal datum name Element
        dom = createDom(withoutMandatoryLines(new int[]{9}));
        assertEquals(null, DimapProductHelpers.createGeoCoding(dom, _product));

        // without horizontal datum Element
        dom = createDom(withoutMandatoryLines(ArrayUtils.createIntArray(8, 17)));
        assertEquals(null, DimapProductHelpers.createGeoCoding(dom, _product));

        // without Geographic_CS Element
        dom = createDom(withoutMandatoryLines(ArrayUtils.createIntArray(6, 18)));
        assertEquals(null, DimapProductHelpers.createGeoCoding(dom, _product));

        // without Horizontal_CS Element
        dom = createDom(withoutMandatoryLines(ArrayUtils.createIntArray(3, 71)));
        assertEquals(null, DimapProductHelpers.createGeoCoding(dom, _product));

        // without Coordinate_Reference_System Element
        dom = createDom(withoutMandatoryLines(ArrayUtils.createIntArray(1, 72)));
        assertEquals(null, DimapProductHelpers.createGeoCoding(dom, _product));

        // without Projection Element
        dom = createDom(withoutMandatoryLines(ArrayUtils.createIntArray(19, 54)));
        assertEquals(null, DimapProductHelpers.createGeoCoding(dom, _product));

        // without NAME Element
        dom = createDom(withoutMandatoryLines(new int[]{20}));
        assertEquals(null, DimapProductHelpers.createGeoCoding(dom, _product));

        // without Projection_CT_Method Element
        dom = createDom(withoutMandatoryLines(ArrayUtils.createIntArray(21, 53)));
        assertEquals(null, DimapProductHelpers.createGeoCoding(dom, _product));

        // without PROJECTION_CT_NAME Element
        dom = createDom(withoutMandatoryLines(new int[]{22}));
        assertEquals(null, DimapProductHelpers.createGeoCoding(dom, _product));

        // without Projection_Parameters Element
        dom = createDom(withoutMandatoryLines(ArrayUtils.createIntArray(23, 52)));
        assertEquals(null, DimapProductHelpers.createGeoCoding(dom, _product));

        // without all Projection_Parameter Elements
        dom = createDom(withoutMandatoryLines(ArrayUtils.createIntArray(24, 51)));
        assertEquals(null, DimapProductHelpers.createGeoCoding(dom, _product));

        // without PROJECTION_PARAMETER_VALUE Element
        dom = createDom(withoutMandatoryLines(new int[]{26}));
        assertEquals(null, DimapProductHelpers.createGeoCoding(dom, _product));

        // without MAP_INFO Element
        dom = createDom(withoutMandatoryLines(ArrayUtils.createIntArray(55, 70)));
        assertEquals(null, DimapProductHelpers.createGeoCoding(dom, _product));
    }

    public void testCreateGeoCodingForFXYGeoCoding() {
        final byte[] bytes = _xmlFXYGeoCodingString.getBytes();
        final Document dom = DimapProductHelpers.createDom(new ByteArrayInputStream(bytes));
        final GeoCoding geoCoding = DimapProductHelpers.createGeoCoding(dom, _product)[0];

        assertNotNull(geoCoding);
        assertEquals(FXYGeoCoding.class, geoCoding.getClass());
        final FXYGeoCoding fxyGeoCoding = (FXYGeoCoding) geoCoding;

        assertEqual(_fxyGeoCoding, fxyGeoCoding);

    }


    public void testReadingGeoCodingPerBand() {
        final byte[] bytes = _xmlBandedFXYGeoCodingString.getBytes();
        final Document dom = DimapProductHelpers.createDom(new ByteArrayInputStream(bytes));
        final GeoCoding[] geoCodings = DimapProductHelpers.createGeoCoding(dom, _product);

        assertFalse(_product.isUsingSingleGeoCoding());
        assertTrue(_product.getGeoCoding() == null);

        final GeoCoding geoCoding1 = geoCodings[0];
        final GeoCoding geoCoding2 = geoCodings[1];
        assertTrue(geoCoding1 instanceof FXYGeoCoding);
        assertTrue(geoCoding2 instanceof FXYGeoCoding);
        assertNotSame(geoCoding1, geoCoding2);

        final FXYGeoCoding fxyGeoCoding1 = (FXYGeoCoding) geoCoding1;
        final FXYGeoCoding fxyGeoCoding2 = (FXYGeoCoding) geoCoding2;

        assertEqual(_fxyGeoCoding1, fxyGeoCoding1);
        assertEqual(_fxyGeoCoding2, fxyGeoCoding2);
    }

    private void assertEqual(final FXYGeoCoding expectedGeoCoding, final FXYGeoCoding actualGeoCoding) {
        assertEquals(expectedGeoCoding.getDatum().getName(), actualGeoCoding.getDatum().getName());
        assertEquals(expectedGeoCoding.getDatum().getEllipsoid().getName(),
                     actualGeoCoding.getDatum().getEllipsoid().getName());
        assertEquals(expectedGeoCoding.getDatum().getEllipsoid().getSemiMajor(),
                     actualGeoCoding.getDatum().getEllipsoid().getSemiMajor(), 1e-6);
        assertEquals(expectedGeoCoding.getDatum().getEllipsoid().getSemiMinor(),
                     actualGeoCoding.getDatum().getEllipsoid().getSemiMinor(), 1e-6);
        assertEquals(expectedGeoCoding.getLatFunction().getOrder(), actualGeoCoding.getLatFunction().getOrder());
        assertTrue(ArrayUtils.equalArrays(expectedGeoCoding.getLatFunction().getCoefficients(),
                                          actualGeoCoding.getLatFunction().getCoefficients(), 1e-6));
        assertEquals(expectedGeoCoding.getLonFunction().getOrder(), actualGeoCoding.getLonFunction().getOrder());
        assertTrue(ArrayUtils.equalArrays(expectedGeoCoding.getLonFunction().getCoefficients(),
                                          actualGeoCoding.getLonFunction().getCoefficients(), 1e-6));
        assertEquals(expectedGeoCoding.getPixelXFunction().getOrder(), actualGeoCoding.getPixelXFunction().getOrder());
        assertTrue(ArrayUtils.equalArrays(expectedGeoCoding.getPixelXFunction().getCoefficients(),
                                          actualGeoCoding.getPixelXFunction().getCoefficients(), 1e-6));
        assertEquals(expectedGeoCoding.getPixelYFunction().getOrder(), actualGeoCoding.getPixelYFunction().getOrder());
        assertTrue(ArrayUtils.equalArrays(expectedGeoCoding.getPixelYFunction().getCoefficients(),
                                          actualGeoCoding.getPixelYFunction().getCoefficients(), 1e-6));
        assertEquals(expectedGeoCoding.getPixelOffsetX(), actualGeoCoding.getPixelOffsetX(), 1e-6);
        assertEquals(expectedGeoCoding.getPixelOffsetY(), actualGeoCoding.getPixelOffsetY(), 1e-6);
        assertEquals(expectedGeoCoding.getPixelSizeX(), actualGeoCoding.getPixelSizeX(), 1e-6);
        assertEquals(expectedGeoCoding.getPixelSizeY(), actualGeoCoding.getPixelSizeY(), 1e-6);
    }

    public void testConvertBeamUnitToDimapUnit() {
        assertEquals("M", DimapProductHelpers.convertBeamUnitToDimapUnit("meter"));
        assertEquals("M", DimapProductHelpers.convertBeamUnitToDimapUnit("meters"));

        assertEquals("KM", DimapProductHelpers.convertBeamUnitToDimapUnit("kilometer"));
        assertEquals("KM", DimapProductHelpers.convertBeamUnitToDimapUnit("kilometers"));

        assertEquals("DEG", DimapProductHelpers.convertBeamUnitToDimapUnit("deg"));
        assertEquals("DEG", DimapProductHelpers.convertBeamUnitToDimapUnit("DEG"));
        assertEquals("DEG", DimapProductHelpers.convertBeamUnitToDimapUnit("degree"));

        assertEquals("RAD", DimapProductHelpers.convertBeamUnitToDimapUnit("rad"));
        assertEquals("RAD", DimapProductHelpers.convertBeamUnitToDimapUnit("RADIAN"));
        assertEquals("RAD", DimapProductHelpers.convertBeamUnitToDimapUnit("radian"));

    }

    public void testConvertDimaUnitToBeamUnit() {
        assertEquals("meter", DimapProductHelpers.convertDimapUnitToBeamUnit("M"));

        assertEquals("kilometer", DimapProductHelpers.convertDimapUnitToBeamUnit("KM"));

        assertEquals("deg", DimapProductHelpers.convertDimapUnitToBeamUnit("DEG"));

        assertEquals("rad", DimapProductHelpers.convertDimapUnitToBeamUnit("RAD"));

    }

    private int[] withoutMandatoryLines(final int[] value) {
        return ArrayUtils.addArrays(_notMandatoryLines, value);
    }

    private Document createDom(final int[] withoutMandatoryLine) {
        final String xmlStringWithoutNotMandatoryLines = createXMLString(withoutMandatoryLine);
        final byte[] bytes = xmlStringWithoutNotMandatoryLines.getBytes();
        return DimapProductHelpers.createDom(new ByteArrayInputStream(bytes));
    }

    private String createXMLString(final int[] notAccepted) {
        Arrays.sort(notAccepted);
        final Vector<String> vector = new Vector<String>();
        for (int i = 0; i < _xmlMapGeocodingStringStyleV1_4_0.length; i++) {
            if (Arrays.binarySearch(notAccepted, i) < 0) {
                vector.add(_xmlMapGeocodingStringStyleV1_4_0[i]);
            }
        }
        final String[] strings = vector.toArray(new String[vector.size()]);
        return StringUtils.arrayToString(strings, "");
    }
}
