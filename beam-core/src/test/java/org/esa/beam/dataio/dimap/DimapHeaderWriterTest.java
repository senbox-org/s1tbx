/*
 * $Id: DimapHeaderWriterTest.java,v 1.1.1.1 2006/09/11 08:16:51 norman Exp $
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
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.dataop.maptransf.Ellipsoid;
import org.esa.beam.framework.dataop.maptransf.LambertConformalConicDescriptor;
import org.esa.beam.framework.dataop.maptransf.MapInfo;
import org.esa.beam.framework.dataop.maptransf.MapProjection;
import org.esa.beam.framework.dataop.maptransf.MapTransform;
import org.esa.beam.framework.dataop.resamp.Resampling;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.math.FXYSum;

import java.awt.Color;
import java.io.StringWriter;

public class DimapHeaderWriterTest extends TestCase {

    private static final String _ls = SystemUtils.LS;
    private static final String header =
            "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>" + _ls +
            "<Dimap_Document name=\"test.dim\">" + _ls +
            "    <Metadata_Id>" + _ls +
            "        <METADATA_FORMAT version=\"" + DimapProductConstants.DIMAP_CURRENT_VERSION + "\">DIMAP</METADATA_FORMAT>" + _ls +
            "        <METADATA_PROFILE>" + DimapProductConstants.DIMAP_METADATA_PROFILE + "</METADATA_PROFILE>" + _ls +
            "    </Metadata_Id>" + _ls +
            "    <Dataset_Id>" + _ls +
            "        <DATASET_SERIES>" + DimapProductConstants.DIMAP_DATASET_SERIES + "</DATASET_SERIES>" + _ls +
            "        <DATASET_NAME>test</DATASET_NAME>" + _ls +
            "    </Dataset_Id>" + _ls +
            "    <Production>" + _ls +
            "        <DATASET_PRODUCER_NAME />" + _ls +
//            "        <DATASET_PRODUCER_NAME>" + DimapProductConstants.DATASET_PRODUCER_NAME + "</DATASET_PRODUCER_NAME>" + _ls +
"        <PRODUCT_TYPE>MER_RR__2P</PRODUCT_TYPE>" + _ls +
"        <PRODUCT_SCENE_RASTER_START_TIME>19-MAY-2003 00:34:05.000034</PRODUCT_SCENE_RASTER_START_TIME>" + _ls + // product scene sensing start
"        <PRODUCT_SCENE_RASTER_STOP_TIME>19-MAY-2003 00:50:45.000034</PRODUCT_SCENE_RASTER_STOP_TIME>" + _ls + // product scene sensing stopt
"    </Production>" + _ls;
    private static final String rasterDimensions =
            "    <Raster_Dimensions>" + _ls +
            "        <NCOLS>200</NCOLS>" + _ls +
            "        <NROWS>300</NROWS>" + _ls +
            "        <NBANDS>0</NBANDS>" + _ls +
            "    </Raster_Dimensions>" + _ls;
    private static final String dataAccess =
            "    <Data_Access>" + _ls +
            "        <DATA_FILE_FORMAT>ENVI</DATA_FILE_FORMAT>" + _ls +
            "        <DATA_FILE_FORMAT_DESC>ENVI File Format</DATA_FILE_FORMAT_DESC>" + _ls +
            "        <DATA_FILE_ORGANISATION>BAND_SEPARATE</DATA_FILE_ORGANISATION>" + _ls +
            "        <Data_File>" + _ls +
            "            <DATA_FILE_PATH href=\"test.data/b1.hdr\" />" + _ls +
            "            <BAND_INDEX>0</BAND_INDEX>" + _ls +
            "        </Data_File>" + _ls +
            "        <Data_File>" + _ls +
            "            <DATA_FILE_PATH href=\"test.data/b2.hdr\" />" + _ls +
            "            <BAND_INDEX>1</BAND_INDEX>" + _ls +
            "        </Data_File>" + _ls +
            "    </Data_Access>" + _ls +
            "    <Image_Interpretation>" + _ls +
            "        <Spectral_Band_Info>" + _ls +
            "            <BAND_INDEX>0</BAND_INDEX>" + _ls +
            "            <BAND_DESCRIPTION />" + _ls +
            "            <BAND_NAME>b1</BAND_NAME>" + _ls +
            "            <DATA_TYPE>int8</DATA_TYPE>" + _ls +
            "            <SOLAR_FLUX>0.0</SOLAR_FLUX>" + _ls +
            "            <BAND_WAVELEN>0.0</BAND_WAVELEN>" + _ls +
            "            <BANDWIDTH>0.0</BANDWIDTH>" + _ls +
            "            <SCALING_FACTOR>1.0</SCALING_FACTOR>" + _ls +
            "            <SCALING_OFFSET>0.0</SCALING_OFFSET>" + _ls +
            "            <LOG10_SCALED>false</LOG10_SCALED>" + _ls +
            "            <NO_DATA_VALUE_USED>false</NO_DATA_VALUE_USED>" + _ls +
            "            <NO_DATA_VALUE>0.0</NO_DATA_VALUE>" + _ls +
            "        </Spectral_Band_Info>" + _ls +
            "        <Spectral_Band_Info>" + _ls +
            "            <BAND_INDEX>1</BAND_INDEX>" + _ls +
            "            <BAND_DESCRIPTION />" + _ls +
            "            <BAND_NAME>b2</BAND_NAME>" + _ls +
            "            <DATA_TYPE>int8</DATA_TYPE>" + _ls +
            "            <SOLAR_FLUX>0.0</SOLAR_FLUX>" + _ls +
            "            <BAND_WAVELEN>0.0</BAND_WAVELEN>" + _ls +
            "            <BANDWIDTH>0.0</BANDWIDTH>" + _ls +
            "            <SCALING_FACTOR>1.0</SCALING_FACTOR>" + _ls +
            "            <SCALING_OFFSET>0.0</SCALING_OFFSET>" + _ls +
            "            <LOG10_SCALED>false</LOG10_SCALED>" + _ls +
            "            <NO_DATA_VALUE_USED>false</NO_DATA_VALUE_USED>" + _ls +
            "            <NO_DATA_VALUE>0.0</NO_DATA_VALUE>" + _ls +
            "        </Spectral_Band_Info>" + _ls +
            "        <Spectral_Band_Info>" + _ls +
            "            <BAND_INDEX>2</BAND_INDEX>" + _ls +
            "            <BAND_DESCRIPTION />" + _ls +
            "            <BAND_NAME>vb1</BAND_NAME>" + _ls +
            "            <DATA_TYPE>int8</DATA_TYPE>" + _ls +
            "            <SOLAR_FLUX>0.0</SOLAR_FLUX>" + _ls +
            "            <BAND_WAVELEN>0.0</BAND_WAVELEN>" + _ls +
            "            <BANDWIDTH>0.0</BANDWIDTH>" + _ls +
            "            <SCALING_FACTOR>1.0</SCALING_FACTOR>" + _ls +
            "            <SCALING_OFFSET>0.0</SCALING_OFFSET>" + _ls +
            "            <LOG10_SCALED>false</LOG10_SCALED>" + _ls +
            "            <NO_DATA_VALUE_USED>false</NO_DATA_VALUE_USED>" + _ls +
            "            <NO_DATA_VALUE>0.0</NO_DATA_VALUE>" + _ls +
            "            <VIRTUAL_BAND>true</VIRTUAL_BAND>" + _ls +
            "            <CHECK_INVALIDS>false</CHECK_INVALIDS>" + _ls +
            "            <EXPRESSION>b1 * 0.4 + 1</EXPRESSION>" + _ls +
            "        </Spectral_Band_Info>" + _ls +
            "        <Spectral_Band_Info>" + _ls +
            "            <BAND_INDEX>3</BAND_INDEX>" + _ls +
            "            <BAND_NAME>cfb1</BAND_NAME>" + _ls +
            "            <BAND_DESCRIPTION />" + _ls +
            "            <DATA_TYPE>float32</DATA_TYPE>" + _ls +
            "            <PHYSICAL_UNIT />" + _ls +
            "            <SOLAR_FLUX>0.0</SOLAR_FLUX>" + _ls +
            "            <BAND_WAVELEN>0.0</BAND_WAVELEN>" + _ls +
            "            <BANDWIDTH>0.0</BANDWIDTH>" + _ls +
            "            <SCALING_FACTOR>1.0</SCALING_FACTOR>" + _ls +
            "            <SCALING_OFFSET>0.0</SCALING_OFFSET>" + _ls +
            "            <LOG10_SCALED>false</LOG10_SCALED>" + _ls +
            "            <NO_DATA_VALUE_USED>true</NO_DATA_VALUE_USED>" + _ls +
            "            <NO_DATA_VALUE>-9999.0</NO_DATA_VALUE>" + _ls +
            "            <Filter_Band_Info bandType=\"ConvolutionFilterBand\">" + _ls +
            "                <FILTER_SOURCE>b2</FILTER_SOURCE>" + _ls +
            "                <Filter_Kernel>" + _ls +
            "                    <KERNEL_WIDTH>3</KERNEL_WIDTH>" + _ls +
            "                    <KERNEL_HEIGHT>3</KERNEL_HEIGHT>" + _ls +
            "                    <KERNEL_FACTOR>1.0</KERNEL_FACTOR>" + _ls +
            "                    <KERNEL_DATA>1.0,2.0,3.0,4.0,5.0,6.0,7.0,8.0,9.0</KERNEL_DATA>" + _ls +
            "                </Filter_Kernel>" + _ls +
            "            </Filter_Band_Info>" + _ls +
            "        </Spectral_Band_Info>" + _ls +
            "        <Spectral_Band_Info>" + _ls +
            "            <BAND_INDEX>4</BAND_INDEX>" + _ls +
            "            <BAND_NAME>gfb1</BAND_NAME>" + _ls +
            "            <BAND_DESCRIPTION />" + _ls +
            "            <DATA_TYPE>float32</DATA_TYPE>" + _ls +
            "            <PHYSICAL_UNIT />" + _ls +
            "            <SOLAR_FLUX>0.0</SOLAR_FLUX>" + _ls +
            "            <BAND_WAVELEN>0.0</BAND_WAVELEN>" + _ls +
            "            <BANDWIDTH>0.0</BANDWIDTH>" + _ls +
            "            <SCALING_FACTOR>1.0</SCALING_FACTOR>" + _ls +
            "            <SCALING_OFFSET>0.0</SCALING_OFFSET>" + _ls +
            "            <LOG10_SCALED>false</LOG10_SCALED>" + _ls +
            "            <NO_DATA_VALUE_USED>true</NO_DATA_VALUE_USED>" + _ls +
            "            <NO_DATA_VALUE>-9999.0</NO_DATA_VALUE>" + _ls +
            "            <Filter_Band_Info bandType=\"GeneralFilterBand\">" + _ls +
            "                <FILTER_SOURCE>b2</FILTER_SOURCE>" + _ls +
            "                <FILTER_SUB_WINDOW_WIDTH>150</FILTER_SUB_WINDOW_WIDTH>" + _ls +
            "                <FILTER_SUB_WINDOW_HEIGHT>200</FILTER_SUB_WINDOW_HEIGHT>" + _ls +
            "                <FILTER_OPERATOR_CLASS_NAME>org.esa.beam.framework.datamodel.GeneralFilterBand$Mean</FILTER_OPERATOR_CLASS_NAME>" + _ls +
            "            </Filter_Band_Info>" + _ls +
            "        </Spectral_Band_Info>" + _ls +
            "    </Image_Interpretation>" + _ls;

    private static final String footer = "</Dimap_Document>";
    private Product _product;
    private StringWriter _stringWriter;
    private DimapHeaderWriter _dimapHeaderWriter;

    public DimapHeaderWriterTest(String s) {
        super(s);
    }

    protected void setUp() throws Exception {
        _product = new Product("test", "MER_RR__2P", 200, 300);
        _product.setStartTime(new ProductData.UTC(1234, 2045, 34));
        _product.setEndTime(new ProductData.UTC(1234, 3045, 34));
        _stringWriter = new StringWriter();
        _dimapHeaderWriter = new DimapHeaderWriter(_product, _stringWriter, "test.data");
    }


    // ###################################################
    // ##  W r i t e   X m l   H e a d e r   L i n e s  ##
    // ###################################################
    public void testWriteXmlHeaderLines() {
        _dimapHeaderWriter.writeHeader();

        final String expected = header + rasterDimensions + footer;
        assertEquals(expected, _stringWriter.toString());
    }

    // ###########################
    // ##  W r i t e   P i n s  ##
    // ###########################
    public void testWritePins() {
        addPinsToProduct();

        _dimapHeaderWriter.writeHeader();

        assertEquals(getExpectedForWritePins(), _stringWriter.toString());
    }

    // ###########################
    // ##  W r i t e   G C P s  ##
    // ###########################
    public void testWriteGcps() {
        addGcpsToProduct();

        _dimapHeaderWriter.writeHeader();

        assertEquals(getExpectedForWriteGcps(), _stringWriter.toString());
    }

    // #########################################
    // ##  W r i t e   B i t m a s k D e f s  ##
    // #########################################
    public void testWriteBitmaskDefs() {
        addBitmaskDefsToProduct();

        _dimapHeaderWriter.writeHeader();

        assertEquals(getExpectedForWriteBitmaskDefs(), _stringWriter.toString());
    }

    // #############################################
    // ##  W r i t e   M a p   G e o c o d i n g  ##
    // #############################################
    public void testWriteMapGeocoding() {
        final String expected = addMapGeocodingToProductAndGetExpected();

        _dimapHeaderWriter.writeHeader();

        assertEquals(expected, _stringWriter.toString());
    }

    // ###########################################
    // ##  W r i t e   F X Y G e o C o d i n g  ##
    // ###########################################
    public void testWriteFXYGeoCoding() {
        final String expectedForFXYGeoCoding = setFXYGeoCodingAndGetExpected();

        _dimapHeaderWriter.writeHeader();

        assertEquals(expectedForFXYGeoCoding, _stringWriter.toString());
    }

    // ###########################################
    // ##  W r i t e   B a n d e d  F X Y G e o C o d i n g  ##
    // ###########################################
    public void testWriteBandedFXYGeoCoding() {
        final String expectedForBandedFXYGeoCoding = setBandedFXYGeoCodingAndGetExpected();

        _dimapHeaderWriter.writeHeader();

        assertEquals(expectedForBandedFXYGeoCoding, _stringWriter.toString());
    }

    private void addPinsToProduct() {
        final Pin pin1 = new Pin("pin1", "pin1", "", null, new GeoPos(), PinSymbol.createDefaultPinSymbol());
        ProductNodeGroup<Pin> pinGroup = _product.getPinGroup();
        pinGroup.add(pin1);

        final Pin pin2 = new Pin("pin2", "pin2", "", null, new GeoPos(4,8), PinSymbol.createDefaultPinSymbol());
        pin2.setDescription("desc2");
        pinGroup.add(pin2);

        final Pin pin3 = new Pin("pin3", "pin3", "", null, new GeoPos(-23.1234f, -80.543f), PinSymbol.createDefaultPinSymbol());
        pinGroup.add(pin3);
    }

    private String getExpectedForWritePins() {
        return
                header +
                rasterDimensions +
                "    <Pin_Group>" + _ls +
                "        <Placemark name=\"pin1\">" + _ls +
                "            <LABEL>pin1</LABEL>" + _ls +
                "            <DESCRIPTION />" + _ls +
                "            <LATITUDE>0.0</LATITUDE>" + _ls +
                "            <LONGITUDE>0.0</LONGITUDE>" + _ls +
                "            <FillColor>" + _ls +
                "                <COLOR red=\"128\" green=\"128\" blue=\"255\" alpha=\"255\" />" + _ls +
                "            </FillColor>" + _ls +
                "            <OutlineColor>" + _ls +
                "                <COLOR red=\"0\" green=\"0\" blue=\"64\" alpha=\"255\" />" + _ls +
                "            </OutlineColor>" + _ls +
                "        </Placemark>" + _ls +
                "        <Placemark name=\"pin2\">" + _ls +
                "            <LABEL>pin2</LABEL>" + _ls +
                "            <DESCRIPTION>desc2</DESCRIPTION>" + _ls +
                "            <LATITUDE>4.0</LATITUDE>" + _ls +
                "            <LONGITUDE>8.0</LONGITUDE>" + _ls +
                "            <FillColor>" + _ls +
                "                <COLOR red=\"128\" green=\"128\" blue=\"255\" alpha=\"255\" />" + _ls +
                "            </FillColor>" + _ls +
                "            <OutlineColor>" + _ls +
                "                <COLOR red=\"0\" green=\"0\" blue=\"64\" alpha=\"255\" />" + _ls +
                "            </OutlineColor>" + _ls +
                "        </Placemark>" + _ls +
                "        <Placemark name=\"pin3\">" + _ls +
                "            <LABEL>pin3</LABEL>" + _ls +
                "            <DESCRIPTION />" + _ls +
                "            <LATITUDE>-23.1234</LATITUDE>" + _ls +
                "            <LONGITUDE>-80.543</LONGITUDE>" + _ls +
                "            <FillColor>" + _ls +
                "                <COLOR red=\"128\" green=\"128\" blue=\"255\" alpha=\"255\" />" + _ls +
                "            </FillColor>" + _ls +
                "            <OutlineColor>" + _ls +
                "                <COLOR red=\"0\" green=\"0\" blue=\"64\" alpha=\"255\" />" + _ls +
                "            </OutlineColor>" + _ls +
                "        </Placemark>" + _ls +
                "    </Pin_Group>" + _ls +
                footer;
    }

    private void addGcpsToProduct() {
        final Pin pin1 = new Pin("gcp1", "gcp1", "", null, new GeoPos(), PinSymbol.createDefaultGcpSymbol());
        ProductNodeGroup<Pin> pinGroup = _product.getGcpGroup();
        pinGroup.add(pin1);

        final Pin pin2 = new Pin("gcp2", "gcp2", "", null, new GeoPos(4,8), PinSymbol.createDefaultGcpSymbol());
        pin2.setDescription("desc2");
        pinGroup.add(pin2);

        final Pin pin3 = new Pin("gcp3", "gcp3", "", null, new GeoPos(-23.1234f, -80.543f), PinSymbol.createDefaultGcpSymbol());
        pinGroup.add(pin3);
    }

    private String getExpectedForWriteGcps() {
        return
                header +
                rasterDimensions +
                "    <Gcp_Group>" + _ls +
                "        <Placemark name=\"gcp1\">" + _ls +
                "            <LABEL>gcp1</LABEL>" + _ls +
                "            <DESCRIPTION />" + _ls +
                "            <LATITUDE>0.0</LATITUDE>" + _ls +
                "            <LONGITUDE>0.0</LONGITUDE>" + _ls +
                "        </Placemark>" + _ls +
                "        <Placemark name=\"gcp2\">" + _ls +
                "            <LABEL>gcp2</LABEL>" + _ls +
                "            <DESCRIPTION>desc2</DESCRIPTION>" + _ls +
                "            <LATITUDE>4.0</LATITUDE>" + _ls +
                "            <LONGITUDE>8.0</LONGITUDE>" + _ls +
                "        </Placemark>" + _ls +
                "        <Placemark name=\"gcp3\">" + _ls +
                "            <LABEL>gcp3</LABEL>" + _ls +
                "            <DESCRIPTION />" + _ls +
                "            <LATITUDE>-23.1234</LATITUDE>" + _ls +
                "            <LONGITUDE>-80.543</LONGITUDE>" + _ls +
                "        </Placemark>" + _ls +
                "    </Gcp_Group>" + _ls +
                footer;
    }

    private void addBitmaskDefsToProduct() {
        BitmaskDef bitmaskDef1 = new BitmaskDef("bitmaskDef1", "description1", "!l1_flags.INVALID", Color.BLUE, 0.75f);
        _product.addBitmaskDef(bitmaskDef1);

        BitmaskDef bitmaskDef2 = new BitmaskDef("bitmaskDef2", "description2", "l1_flags.LAND", Color.GREEN, 0.5f);
        _product.addBitmaskDef(bitmaskDef2);
    }

    private String getExpectedForWriteBitmaskDefs() {
        return
                header +
                rasterDimensions +
                "    <Bitmask_Definitions>" + _ls +
                "        <Bitmask_Definition name=\"bitmaskDef1\">" + _ls +
                "            <DESCRIPTION value=\"description1\" />" + _ls +
                "            <EXPRESSION value=\"!l1_flags.INVALID\" />" + _ls +
                "            <COLOR red=\"0\" green=\"0\" blue=\"255\" alpha=\"255\" />" + _ls +
                "            <TRANSPARENCY value=\"0.75\" />" + _ls +
                "        </Bitmask_Definition>" + _ls +
                "        <Bitmask_Definition name=\"bitmaskDef2\">" + _ls +
                "            <DESCRIPTION value=\"description2\" />" + _ls +
                "            <EXPRESSION value=\"l1_flags.LAND\" />" + _ls +
                "            <COLOR red=\"0\" green=\"255\" blue=\"0\" alpha=\"255\" />" + _ls +
                "            <TRANSPARENCY value=\"0.5\" />" + _ls +
                "        </Bitmask_Definition>" + _ls +
                "    </Bitmask_Definitions>" + _ls +
                footer;
    }

    private String addMapGeocodingToProductAndGetExpected() {
        final double semiMinor = 1234.0;
        final double semiMajor = 5678.0;
        final double[] values = new double[]{semiMajor, semiMinor, 15, 16, 17, 18, 19}; // must be seven values
        final String projectionName = "ProjectionName";
        final String ellipsoidName = "EllipsoidName";
        final String datumName = "DatumName";
        final float pixelX = 3.2f;
        final float pixelY = 4.3f;
        final float easting = 5.4f;
        final float northing = 6.5f;
        final float orientation = 7.3f;
        final float pixelSizeX = 7.6f;
        final float pixelSizeY = 8.7f;
        final boolean orthorectified = true;
        final String elevModelName = "GETASSE30";
        final double noDataValue = 99999.99;
        final String typeId = LambertConformalConicDescriptor.TYPE_ID;
        final int sceneWidth = _product.getSceneRasterWidth();
        final int sceneHeight = _product.getSceneRasterHeight();
        final boolean sceneFitted = true;
        final Resampling resampling = Resampling.NEAREST_NEIGHBOUR;
        final String mapUnit = "mapUnit";

        final LambertConformalConicDescriptor descriptor = new LambertConformalConicDescriptor();
        final MapTransform transform = descriptor.createTransform(values);
        final MapProjection projection = new MapProjection(projectionName, transform, mapUnit);
        final Ellipsoid ellipsoid = new Ellipsoid(ellipsoidName, semiMinor, semiMajor);
        final Datum datum = new Datum(datumName, ellipsoid, 0, 0, 0);
        final MapInfo mapInfo = new MapInfo(projection, pixelX, pixelY, easting, northing, pixelSizeX, pixelSizeY,
                                            datum);
        mapInfo.setOrientation(orientation);
        mapInfo.setOrthorectified(orthorectified);
        mapInfo.setElevationModelName(elevModelName);
        mapInfo.setNoDataValue(noDataValue);
        mapInfo.setSceneWidth(sceneWidth);
        mapInfo.setSceneHeight(sceneHeight);
        mapInfo.setSceneSizeFitted(sceneFitted);
        mapInfo.setResampling(resampling);
        _product.setGeoCoding(new MapGeoCoding(mapInfo));

        return header +
               "    <Coordinate_Reference_System>" + _ls +
               "        <GEO_TABLES version=\"1.0\">CUSTOM</GEO_TABLES>" + _ls +
               "        <Horizontal_CS>" + _ls +
               "            <HORIZONTAL_CS_TYPE>PROJECTED</HORIZONTAL_CS_TYPE>" + _ls +
               "            <HORIZONTAL_CS_NAME>" + projectionName + "</HORIZONTAL_CS_NAME>" + _ls +
               "            <Geographic_CS>" + _ls +
               "                <GEOGRAPHIC_CS_NAME>" + projectionName + "</GEOGRAPHIC_CS_NAME>" + _ls +
               "                <Horizontal_Datum>" + _ls +
               "                    <HORIZONTAL_DATUM_NAME>" + datumName + "</HORIZONTAL_DATUM_NAME>" + _ls +
               "                    <Ellipsoid>" + _ls +
               "                        <ELLIPSOID_NAME>" + ellipsoidName + "</ELLIPSOID_NAME>" + _ls +
               "                        <Ellipsoid_Parameters>" + _ls +
               "                            <ELLIPSOID_MAJ_AXIS unit=\"meter\">" + semiMajor + "</ELLIPSOID_MAJ_AXIS>" + _ls +
               "                            <ELLIPSOID_MIN_AXIS unit=\"meter\">" + semiMinor + "</ELLIPSOID_MIN_AXIS>" + _ls +
               "                        </Ellipsoid_Parameters>" + _ls +
               "                    </Ellipsoid>" + _ls +
               "                </Horizontal_Datum>" + _ls +
               "            </Geographic_CS>" + _ls +
               "            <Projection>" + _ls +
               "                <NAME>" + projectionName + "</NAME>" + _ls +
               "                <Projection_CT_Method>" + _ls +
               "                    <PROJECTION_CT_NAME>" + typeId + "</PROJECTION_CT_NAME>" + _ls +
               "                    <Projection_Parameters>" + _ls +
               "                        <Projection_Parameter>" + _ls +
               "                            <PROJECTION_PARAMETER_NAME>" + LambertConformalConicDescriptor.PARAMETER_NAMES[0] + "</PROJECTION_PARAMETER_NAME>" + _ls +
               "                            <PROJECTION_PARAMETER_VALUE unit=\"" + LambertConformalConicDescriptor.PARAMETER_UNITS[0] + "\">" + values[0] + "</PROJECTION_PARAMETER_VALUE>" + _ls +
               "                        </Projection_Parameter>" + _ls +
               "                        <Projection_Parameter>" + _ls +
               "                            <PROJECTION_PARAMETER_NAME>" + LambertConformalConicDescriptor.PARAMETER_NAMES[1] + "</PROJECTION_PARAMETER_NAME>" + _ls +
               "                            <PROJECTION_PARAMETER_VALUE unit=\"" + LambertConformalConicDescriptor.PARAMETER_UNITS[1] + "\">" + values[1] + "</PROJECTION_PARAMETER_VALUE>" + _ls +
               "                        </Projection_Parameter>" + _ls +
               "                        <Projection_Parameter>" + _ls +
               "                            <PROJECTION_PARAMETER_NAME>" + LambertConformalConicDescriptor.PARAMETER_NAMES[2] + "</PROJECTION_PARAMETER_NAME>" + _ls +
               "                            <PROJECTION_PARAMETER_VALUE unit=\"" + LambertConformalConicDescriptor.PARAMETER_UNITS[2] + "\">" + values[2] + "</PROJECTION_PARAMETER_VALUE>" + _ls +
               "                        </Projection_Parameter>" + _ls +
               "                        <Projection_Parameter>" + _ls +
               "                            <PROJECTION_PARAMETER_NAME>" + LambertConformalConicDescriptor.PARAMETER_NAMES[3] + "</PROJECTION_PARAMETER_NAME>" + _ls +
               "                            <PROJECTION_PARAMETER_VALUE unit=\"" + LambertConformalConicDescriptor.PARAMETER_UNITS[3] + "\">" + values[3] + "</PROJECTION_PARAMETER_VALUE>" + _ls +
               "                        </Projection_Parameter>" + _ls +
               "                        <Projection_Parameter>" + _ls +
               "                            <PROJECTION_PARAMETER_NAME>" + LambertConformalConicDescriptor.PARAMETER_NAMES[4] + "</PROJECTION_PARAMETER_NAME>" + _ls +
               "                            <PROJECTION_PARAMETER_VALUE unit=\"" + LambertConformalConicDescriptor.PARAMETER_UNITS[4] + "\">" + values[4] + "</PROJECTION_PARAMETER_VALUE>" + _ls +
               "                        </Projection_Parameter>" + _ls +
               "                        <Projection_Parameter>" + _ls +
               "                            <PROJECTION_PARAMETER_NAME>" + LambertConformalConicDescriptor.PARAMETER_NAMES[5] + "</PROJECTION_PARAMETER_NAME>" + _ls +
               "                            <PROJECTION_PARAMETER_VALUE unit=\"" + LambertConformalConicDescriptor.PARAMETER_UNITS[5] + "\">" + values[5] + "</PROJECTION_PARAMETER_VALUE>" + _ls +
               "                        </Projection_Parameter>" + _ls +
               "                        <Projection_Parameter>" + _ls +
               "                            <PROJECTION_PARAMETER_NAME>" + LambertConformalConicDescriptor.PARAMETER_NAMES[6] + "</PROJECTION_PARAMETER_NAME>" + _ls +
               "                            <PROJECTION_PARAMETER_VALUE unit=\"" + LambertConformalConicDescriptor.PARAMETER_UNITS[6] + "\">" + values[6] + "</PROJECTION_PARAMETER_VALUE>" + _ls +
               "                        </Projection_Parameter>" + _ls +
               "                    </Projection_Parameters>" + _ls +
               "                </Projection_CT_Method>" + _ls +
               "            </Projection>" + _ls +
               "            <MAP_INFO>" + _ls +
               "                <PIXEL_X value=\"" + pixelX + "\" />" + _ls +
               "                <PIXEL_Y value=\"" + pixelY + "\" />" + _ls +
               "                <EASTING value=\"" + easting + "\" />" + _ls +
               "                <NORTHING value=\"" + northing + "\" />" + _ls +
               "                <ORIENTATION value=\"" + orientation + "\" />" + _ls +
               "                <PIXELSIZE_X value=\"" + pixelSizeX + "\" />" + _ls +
               "                <PIXELSIZE_Y value=\"" + pixelSizeY + "\" />" + _ls +
               "                <NODATA_VALUE value=\"" + noDataValue + "\" />" + _ls +
               "                <MAPUNIT value=\"" + mapUnit + "\" />" + _ls +
               "                <ORTHORECTIFIED value=\"" + orthorectified + "\" />" + _ls +
               "                <ELEVATION_MODEL value=\"" + elevModelName + "\" />" + _ls +
               "                <SCENE_FITTED value=\"" + sceneFitted + "\" />" + _ls +
               "                <SCENE_WIDTH value=\"" + sceneWidth + "\" />" + _ls +
               "                <SCENE_HEIGHT value=\"" + sceneHeight + "\" />" + _ls +
               "                <RESAMPLING value=\"" + resampling.getName() + "\" />" + _ls +
               "            </MAP_INFO>" + _ls +
               "        </Horizontal_CS>" + _ls +
               "    </Coordinate_Reference_System>" + _ls +
               rasterDimensions +
               footer;
    }

    private String setFXYGeoCodingAndGetExpected() {
        final double[] xCoefficients = new double[]{0, 1, 2};
        final double[] yCoefficients = new double[]{3, 4, 5};
        final double[] lambdaCoefficients = new double[]{6, 7, 8};
        final double[] phiCoefficients = new double[]{9, 10, 11};
        final FXYSum xFunction = new FXYSum(FXYSum.FXY_LINEAR, 1, xCoefficients);
        final FXYSum yFunction = new FXYSum(FXYSum.FXY_LINEAR, 1, yCoefficients);
        final FXYSum lambdaFunction = new FXYSum(FXYSum.FXY_LINEAR, 1, lambdaCoefficients);
        final FXYSum phiFunction = new FXYSum(FXYSum.FXY_LINEAR, 1, phiCoefficients);
        final float pixelOffsetX = 0;
        final float pixelOffsetY = 0;
        final float pixelSizeX = 1;
        final float pixelSizeY = 1;
        final Datum datum = Datum.WGS_84;
        final FXYGeoCoding geoCoding = new FXYGeoCoding(pixelOffsetX, pixelOffsetY,
                                                        pixelSizeX, pixelSizeY,
                                                        xFunction, yFunction, phiFunction, lambdaFunction,
                                                        datum);
        _product.setGeoCoding(geoCoding);
        final Ellipsoid ellipsoid = datum.getEllipsoid();
        return header +
               "    <Coordinate_Reference_System>" + _ls +
               "        <Horizontal_CS>" + _ls +
               "            <HORIZONTAL_CS_TYPE>GEOGRAPHIC</HORIZONTAL_CS_TYPE>" + _ls +
               "            <Geographic_CS>" + _ls +
               "                <Horizontal_Datum>" + _ls +
               "                    <HORIZONTAL_DATUM_NAME>" + datum.getName() + "</HORIZONTAL_DATUM_NAME>" + _ls +
               "                    <Ellipsoid>" + _ls +
               "                        <ELLIPSOID_NAME>" + ellipsoid.getName() + "</ELLIPSOID_NAME>" + _ls +
               "                        <Ellipsoid_Parameters>" + _ls +
               "                            <ELLIPSOID_MAJ_AXIS unit=\"M\">" + ellipsoid.getSemiMajor() + "</ELLIPSOID_MAJ_AXIS>" + _ls +
               "                            <ELLIPSOID_MIN_AXIS unit=\"M\">" + ellipsoid.getSemiMinor() + "</ELLIPSOID_MIN_AXIS>" + _ls +
               "                        </Ellipsoid_Parameters>" + _ls +
               "                    </Ellipsoid>" + _ls +
               "                </Horizontal_Datum>" + _ls +
               "            </Geographic_CS>" + _ls +
               "        </Horizontal_CS>" + _ls +
               "    </Coordinate_Reference_System>" + _ls +
               "    <Geoposition>" + _ls +
               "        <Geoposition_Insert>" + _ls +
               "            <ULXMAP>" + pixelOffsetX + "</ULXMAP>" + _ls +
               "            <ULYMAP>" + pixelOffsetY + "</ULYMAP>" + _ls +
               "            <XDIM>" + pixelSizeX + "</XDIM>" + _ls +
               "            <YDIM>" + pixelSizeY + "</YDIM>" + _ls +
               "        </Geoposition_Insert>" + _ls +
               "        <Simplified_Location_Model>" + _ls +
               "            <Direct_Location_Model order=\"" + lambdaFunction.getOrder() + "\">" + _ls +
               "                <lc_List>" + _ls +
               "                    <lc index=\"0\">" + lambdaCoefficients[0] + "</lc>" + _ls +
               "                    <lc index=\"1\">" + lambdaCoefficients[1] + "</lc>" + _ls +
               "                    <lc index=\"2\">" + lambdaCoefficients[2] + "</lc>" + _ls +
               "                </lc_List>" + _ls +
               "                <pc_List>" + _ls +
               "                    <pc index=\"0\">" + phiCoefficients[0] + "</pc>" + _ls +
               "                    <pc index=\"1\">" + phiCoefficients[1] + "</pc>" + _ls +
               "                    <pc index=\"2\">" + phiCoefficients[2] + "</pc>" + _ls +
               "                </pc_List>" + _ls +
               "            </Direct_Location_Model>" + _ls +
               "            <Reverse_Location_Model order=\"" + xFunction.getOrder() + "\">" + _ls + "                <ic_List>" + _ls +
               "                    <ic index=\"0\">" + xCoefficients[0] + "</ic>" + _ls +
               "                    <ic index=\"1\">" + xCoefficients[1] + "</ic>" + _ls +
               "                    <ic index=\"2\">" + xCoefficients[2] + "</ic>" + _ls +
               "                </ic_List>" + _ls +
               "                <jc_List>" + _ls +
               "                    <jc index=\"0\">" + yCoefficients[0] + "</jc>" + _ls +
               "                    <jc index=\"1\">" + yCoefficients[1] + "</jc>" + _ls +
               "                    <jc index=\"2\">" + yCoefficients[2] + "</jc>" + _ls +
               "                </jc_List>" + _ls +
               "            </Reverse_Location_Model>" + _ls +
               "        </Simplified_Location_Model>" + _ls +
               "    </Geoposition>" + _ls +
               rasterDimensions +
               footer;
    }

    private String setBandedFXYGeoCodingAndGetExpected() {
        final double[] xCoefficients1 = new double[]{0, 1, 2};
        final double[] yCoefficients1 = new double[]{3, 4, 5};
        final double[] lonCoefficients1 = new double[]{6, 7, 8};
        final double[] latCoefficients1 = new double[]{9, 10, 11};
        final FXYSum xFunction1 = new FXYSum(FXYSum.FXY_LINEAR, 1, xCoefficients1);
        final FXYSum yFunction1 = new FXYSum(FXYSum.FXY_LINEAR, 1, yCoefficients1);
        final FXYSum lambdaFunction1 = new FXYSum(FXYSum.FXY_LINEAR, 1, lonCoefficients1);
        final FXYSum phiFunction1 = new FXYSum(FXYSum.FXY_LINEAR, 1, latCoefficients1);
        final double[] xCoefficients2 = new double[]{12, 13, 14};
        final double[] yCoefficients2 = new double[]{15, 16, 17};
        final double[] lonCoefficients2 = new double[]{18, 19, 20};
        final double[] latCoefficients2 = new double[]{21, 22, 23};
        final FXYSum xFunction2 = new FXYSum(FXYSum.FXY_LINEAR, 1, xCoefficients2);
        final FXYSum yFunction2 = new FXYSum(FXYSum.FXY_LINEAR, 1, yCoefficients2);
        final FXYSum lambdaFunction2 = new FXYSum(FXYSum.FXY_LINEAR, 1, lonCoefficients2);
        final FXYSum phiFunction2 = new FXYSum(FXYSum.FXY_LINEAR, 1, latCoefficients2);
        final float pixelOffsetX = 0;
        final float pixelOffsetY = 0;
        final float pixelSizeX = 1;
        final float pixelSizeY = 1;
        final Datum datum = Datum.WGS_84;
        final FXYGeoCoding geoCoding1 = new FXYGeoCoding(pixelOffsetX, pixelOffsetY,
                                                         pixelSizeX, pixelSizeY,
                                                         xFunction1, yFunction1, phiFunction1, lambdaFunction1,
                                                         datum);
        final FXYGeoCoding geoCoding2 = new FXYGeoCoding(pixelOffsetX, pixelOffsetY,
                                                         pixelSizeX, pixelSizeY,
                                                         xFunction2, yFunction2, phiFunction2, lambdaFunction2,
                                                         datum);
        final Band band1 = _product.addBand("b1", ProductData.TYPE_INT8);
        final Band band2 = _product.addBand("b2", ProductData.TYPE_INT8);
        _product.addBand(new VirtualBand("vb1", ProductData.TYPE_INT8, 200, 300, "b1 * 0.4 + 1"));
        _product.addBand(new ConvolutionFilterBand("cfb1", band2,
                                                   new Kernel(3, 3, 1, new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9})));
        _product.addBand(new GeneralFilterBand("gfb1", band2, 150, 200, GeneralFilterBand.MEAN));


        band1.setGeoCoding(geoCoding1);
        band2.setGeoCoding(geoCoding2);
        return header +
               "    <Coordinate_Reference_System>" + _ls +
               "        <Horizontal_CS>" + _ls +
               "            <HORIZONTAL_CS_TYPE>GEOGRAPHIC</HORIZONTAL_CS_TYPE>" + _ls +
               "            <Geographic_CS>" + _ls +
               "                <Horizontal_Datum>" + _ls +
               "                    <HORIZONTAL_DATUM_NAME>" + datum.getName() + "</HORIZONTAL_DATUM_NAME>" + _ls +
               "                    <Ellipsoid>" + _ls +
               "                        <ELLIPSOID_NAME>" + datum.getEllipsoid().getName() + "</ELLIPSOID_NAME>" + _ls +
               "                        <Ellipsoid_Parameters>" + _ls +
               "                            <ELLIPSOID_MAJ_AXIS unit=\"M\">" + datum.getEllipsoid().getSemiMajor() + "</ELLIPSOID_MAJ_AXIS>" + _ls +
               "                            <ELLIPSOID_MIN_AXIS unit=\"M\">" + datum.getEllipsoid().getSemiMinor() + "</ELLIPSOID_MIN_AXIS>" + _ls +
               "                        </Ellipsoid_Parameters>" + _ls +
               "                    </Ellipsoid>" + _ls +
               "                </Horizontal_Datum>" + _ls +
               "            </Geographic_CS>" + _ls +
               "        </Horizontal_CS>" + _ls +
               "    </Coordinate_Reference_System>" + _ls +
               "    <Geoposition>" + _ls +
               "        <BAND_INDEX>0</BAND_INDEX>" + _ls +
               "        <Geoposition_Insert>" + _ls +
               "            <ULXMAP>" + pixelOffsetX + "</ULXMAP>" + _ls +
               "            <ULYMAP>" + pixelOffsetY + "</ULYMAP>" + _ls +
               "            <XDIM>" + pixelSizeX + "</XDIM>" + _ls +
               "            <YDIM>" + pixelSizeY + "</YDIM>" + _ls +
               "        </Geoposition_Insert>" + _ls +
               "        <Simplified_Location_Model>" + _ls +
               "            <Direct_Location_Model order=\"" + geoCoding1.getPixelXFunction().getOrder() + "\">" + _ls +
               "                <lc_List>" + _ls +
               "                    <lc index=\"0\">" + lonCoefficients1[0] + "</lc>" + _ls +
               "                    <lc index=\"1\">" + lonCoefficients1[1] + "</lc>" + _ls +
               "                    <lc index=\"2\">" + lonCoefficients1[2] + "</lc>" + _ls +
               "                </lc_List>" + _ls +
               "                <pc_List>" + _ls +
               "                    <pc index=\"0\">" + latCoefficients1[0] + "</pc>" + _ls +
               "                    <pc index=\"1\">" + latCoefficients1[1] + "</pc>" + _ls +
               "                    <pc index=\"2\">" + latCoefficients1[2] + "</pc>" + _ls +
               "                </pc_List>" + _ls +
               "            </Direct_Location_Model>" + _ls +
               "            <Reverse_Location_Model order=\"" + geoCoding1.getLatFunction().getOrder() + "\">" + _ls +
               "                <ic_List>" + _ls +
               "                    <ic index=\"0\">" + xCoefficients1[0] + "</ic>" + _ls +
               "                    <ic index=\"1\">" + xCoefficients1[1] + "</ic>" + _ls +
               "                    <ic index=\"2\">" + xCoefficients1[2] + "</ic>" + _ls +
               "                </ic_List>" + _ls +
               "                <jc_List>" + _ls +
               "                    <jc index=\"0\">" + yCoefficients1[0] + "</jc>" + _ls +
               "                    <jc index=\"1\">" + yCoefficients1[1] + "</jc>" + _ls +
               "                    <jc index=\"2\">" + yCoefficients1[2] + "</jc>" + _ls +
               "                </jc_List>" + _ls +
               "            </Reverse_Location_Model>" + _ls +
               "        </Simplified_Location_Model>" + _ls +
               "    </Geoposition>" + _ls +
               "    <Geoposition>" + _ls +
               "        <BAND_INDEX>1</BAND_INDEX>" + _ls +
               "        <Geoposition_Insert>" + _ls +
               "            <ULXMAP>" + pixelOffsetX + "</ULXMAP>" + _ls +
               "            <ULYMAP>" + pixelOffsetY + "</ULYMAP>" + _ls +
               "            <XDIM>" + pixelSizeX + "</XDIM>" + _ls +
               "            <YDIM>" + pixelSizeY + "</YDIM>" + _ls +
               "        </Geoposition_Insert>" + _ls +
               "        <Simplified_Location_Model>" + _ls +
               "            <Direct_Location_Model order=\"" + geoCoding2.getPixelXFunction().getOrder() + "\">" + _ls +
               "                <lc_List>" + _ls +
               "                    <lc index=\"0\">" + lonCoefficients2[0] + "</lc>" + _ls +
               "                    <lc index=\"1\">" + lonCoefficients2[1] + "</lc>" + _ls +
               "                    <lc index=\"2\">" + lonCoefficients2[2] + "</lc>" + _ls +
               "                </lc_List>" + _ls +
               "                <pc_List>" + _ls +
               "                    <pc index=\"0\">" + latCoefficients2[0] + "</pc>" + _ls +
               "                    <pc index=\"1\">" + latCoefficients2[1] + "</pc>" + _ls +
               "                    <pc index=\"2\">" + latCoefficients2[2] + "</pc>" + _ls +
               "                </pc_List>" + _ls +
               "            </Direct_Location_Model>" + _ls +
               "            <Reverse_Location_Model order=\"" + geoCoding2.getLatFunction().getOrder() + "\">" + _ls +
               "                <ic_List>" + _ls +
               "                    <ic index=\"0\">" + xCoefficients2[0] + "</ic>" + _ls +
               "                    <ic index=\"1\">" + xCoefficients2[1] + "</ic>" + _ls +
               "                    <ic index=\"2\">" + xCoefficients2[2] + "</ic>" + _ls +
               "                </ic_List>" + _ls +
               "                <jc_List>" + _ls +
               "                    <jc index=\"0\">" + yCoefficients2[0] + "</jc>" + _ls +
               "                    <jc index=\"1\">" + yCoefficients2[1] + "</jc>" + _ls +
               "                    <jc index=\"2\">" + yCoefficients2[2] + "</jc>" + _ls +
               "                </jc_List>" + _ls +
               "            </Reverse_Location_Model>" + _ls +
               "        </Simplified_Location_Model>" + _ls +
               "    </Geoposition>" + _ls +
               "    <Geoposition>" + _ls +
               "        <BAND_INDEX>2</BAND_INDEX>" + _ls +
               "        <Geoposition_Insert>" + _ls +
               "            <ULXMAP>" + pixelOffsetX + "</ULXMAP>" + _ls +
               "            <ULYMAP>" + pixelOffsetY + "</ULYMAP>" + _ls +
               "            <XDIM>" + pixelSizeX + "</XDIM>" + _ls +
               "            <YDIM>" + pixelSizeY + "</YDIM>" + _ls +
               "        </Geoposition_Insert>" + _ls +
               "        <Simplified_Location_Model>" + _ls +
               "            <Direct_Location_Model order=\"" + geoCoding2.getPixelXFunction().getOrder() + "\">" + _ls +
               "                <lc_List>" + _ls +
               "                    <lc index=\"0\">" + lonCoefficients1[0] + "</lc>" + _ls +
               "                    <lc index=\"1\">" + lonCoefficients1[1] + "</lc>" + _ls +
               "                    <lc index=\"2\">" + lonCoefficients1[2] + "</lc>" + _ls +
               "                </lc_List>" + _ls +
               "                <pc_List>" + _ls +
               "                    <pc index=\"0\">" + latCoefficients1[0] + "</pc>" + _ls +
               "                    <pc index=\"1\">" + latCoefficients1[1] + "</pc>" + _ls +
               "                    <pc index=\"2\">" + latCoefficients1[2] + "</pc>" + _ls +
               "                </pc_List>" + _ls +
               "            </Direct_Location_Model>" + _ls +
               "            <Reverse_Location_Model order=\"" + geoCoding1.getLatFunction().getOrder() + "\">" + _ls +
               "                <ic_List>" + _ls +
               "                    <ic index=\"0\">" + xCoefficients1[0] + "</ic>" + _ls +
               "                    <ic index=\"1\">" + xCoefficients1[1] + "</ic>" + _ls +
               "                    <ic index=\"2\">" + xCoefficients1[2] + "</ic>" + _ls +
               "                </ic_List>" + _ls +
               "                <jc_List>" + _ls +
               "                    <jc index=\"0\">" + yCoefficients1[0] + "</jc>" + _ls +
               "                    <jc index=\"1\">" + yCoefficients1[1] + "</jc>" + _ls +
               "                    <jc index=\"2\">" + yCoefficients1[2] + "</jc>" + _ls +
               "                </jc_List>" + _ls +
               "            </Reverse_Location_Model>" + _ls +
               "        </Simplified_Location_Model>" + _ls +
               "    </Geoposition>" + _ls +
               "    <Geoposition>" + _ls +
               "        <BAND_INDEX>3</BAND_INDEX>" + _ls +
               "        <Geoposition_Insert>" + _ls +
               "            <ULXMAP>" + pixelOffsetX + "</ULXMAP>" + _ls +
               "            <ULYMAP>" + pixelOffsetY + "</ULYMAP>" + _ls +
               "            <XDIM>" + pixelSizeX + "</XDIM>" + _ls +
               "            <YDIM>" + pixelSizeY + "</YDIM>" + _ls +
               "        </Geoposition_Insert>" + _ls +
               "        <Simplified_Location_Model>" + _ls +
               "            <Direct_Location_Model order=\"" + geoCoding1.getPixelXFunction().getOrder() + "\">" + _ls +
               "                <lc_List>" + _ls +
               "                    <lc index=\"0\">" + lonCoefficients1[0] + "</lc>" + _ls +
               "                    <lc index=\"1\">" + lonCoefficients1[1] + "</lc>" + _ls +
               "                    <lc index=\"2\">" + lonCoefficients1[2] + "</lc>" + _ls +
               "                </lc_List>" + _ls +
               "                <pc_List>" + _ls +
               "                    <pc index=\"0\">" + latCoefficients1[0] + "</pc>" + _ls +
               "                    <pc index=\"1\">" + latCoefficients1[1] + "</pc>" + _ls +
               "                    <pc index=\"2\">" + latCoefficients1[2] + "</pc>" + _ls +
               "                </pc_List>" + _ls +
               "            </Direct_Location_Model>" + _ls +
               "            <Reverse_Location_Model order=\"" + geoCoding2.getLatFunction().getOrder() + "\">" + _ls +
               "                <ic_List>" + _ls +
               "                    <ic index=\"0\">" + xCoefficients1[0] + "</ic>" + _ls +
               "                    <ic index=\"1\">" + xCoefficients1[1] + "</ic>" + _ls +
               "                    <ic index=\"2\">" + xCoefficients1[2] + "</ic>" + _ls +
               "                </ic_List>" + _ls +
               "                <jc_List>" + _ls +
               "                    <jc index=\"0\">" + yCoefficients1[0] + "</jc>" + _ls +
               "                    <jc index=\"1\">" + yCoefficients1[1] + "</jc>" + _ls +
               "                    <jc index=\"2\">" + yCoefficients1[2] + "</jc>" + _ls +
               "                </jc_List>" + _ls +
               "            </Reverse_Location_Model>" + _ls +
               "        </Simplified_Location_Model>" + _ls +
               "    </Geoposition>" + _ls +
               "    <Geoposition>" + _ls +
               "        <BAND_INDEX>4</BAND_INDEX>" + _ls +
               "        <Geoposition_Insert>" + _ls +
               "            <ULXMAP>" + pixelOffsetX + "</ULXMAP>" + _ls +
               "            <ULYMAP>" + pixelOffsetY + "</ULYMAP>" + _ls +
               "            <XDIM>" + pixelSizeX + "</XDIM>" + _ls +
               "            <YDIM>" + pixelSizeY + "</YDIM>" + _ls +
               "        </Geoposition_Insert>" + _ls +
               "        <Simplified_Location_Model>" + _ls +
               "            <Direct_Location_Model order=\"" + geoCoding1.getPixelXFunction().getOrder() + "\">" + _ls +
               "                <lc_List>" + _ls +
               "                    <lc index=\"0\">" + lonCoefficients1[0] + "</lc>" + _ls +
               "                    <lc index=\"1\">" + lonCoefficients1[1] + "</lc>" + _ls +
               "                    <lc index=\"2\">" + lonCoefficients1[2] + "</lc>" + _ls +
               "                </lc_List>" + _ls +
               "                <pc_List>" + _ls +
               "                    <pc index=\"0\">" + latCoefficients1[0] + "</pc>" + _ls +
               "                    <pc index=\"1\">" + latCoefficients1[1] + "</pc>" + _ls +
               "                    <pc index=\"2\">" + latCoefficients1[2] + "</pc>" + _ls +
               "                </pc_List>" + _ls +
               "            </Direct_Location_Model>" + _ls +
               "            <Reverse_Location_Model order=\"" + geoCoding1.getLatFunction().getOrder() + "\">" + _ls +
               "                <ic_List>" + _ls +
               "                    <ic index=\"0\">" + xCoefficients1[0] + "</ic>" + _ls +
               "                    <ic index=\"1\">" + xCoefficients1[1] + "</ic>" + _ls +
               "                    <ic index=\"2\">" + xCoefficients1[2] + "</ic>" + _ls +
               "                </ic_List>" + _ls +
               "                <jc_List>" + _ls +
               "                    <jc index=\"0\">" + yCoefficients1[0] + "</jc>" + _ls +
               "                    <jc index=\"1\">" + yCoefficients1[1] + "</jc>" + _ls +
               "                    <jc index=\"2\">" + yCoefficients1[2] + "</jc>" + _ls +
               "                </jc_List>" + _ls +
               "            </Reverse_Location_Model>" + _ls +
               "        </Simplified_Location_Model>" + _ls +
               "    </Geoposition>" + _ls +
               "    <Raster_Dimensions>" + _ls +
               "        <NCOLS>200</NCOLS>" + _ls +
               "        <NROWS>300</NROWS>" + _ls +
               "        <NBANDS>5</NBANDS>" + _ls +
               "    </Raster_Dimensions>" + _ls +
               dataAccess +
               footer;
    }

}
