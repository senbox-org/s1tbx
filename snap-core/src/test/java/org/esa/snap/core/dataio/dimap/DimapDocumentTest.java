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
package org.esa.snap.core.dataio.dimap;

import org.esa.snap.GlobalTestTools;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.GcpGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.MapGeoCoding;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.Stx;
import org.esa.snap.core.datamodel.StxFactory;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.esa.snap.core.dataop.maptransf.LambertConformalConicDescriptor;
import org.esa.snap.core.dataop.maptransf.MapInfo;
import org.esa.snap.core.dataop.maptransf.MapProjection;
import org.esa.snap.core.dataop.maptransf.MapTransform;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.util.BeamConstants;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class DimapDocumentTest {

    private static final String _nameDataDirectory = "test.data";
    private final static int TIE_POINT_GEOCODING = 1;
    private final static int MAP_GEOCODING = 2;
    private static final int GCP_GEOCODING = 3;

    @Before
    public void setUp() {
        GlobalTestTools.deleteTestDataOutputDirectory();
    }

    @After
    public void tearDown() {
        GlobalTestTools.deleteTestDataOutputDirectory();
    }

    @Test
    public void testCreateProduct_with_MapGeoCoding() {
        // preparation
        int geocodingType = MAP_GEOCODING;
        Product product = createProduct(geocodingType);

        // execution
        String xml1 = writeXml(product);
        String xml2 = writeXml(createProductFromXML(xml1));

        // verification
        assertEquals(xml1, xml2);
    }

    @Test
    public void testCreateProduct_with_TiePointGeoCoding() throws ParseException {
        // preparation
        int geocodingType = TIE_POINT_GEOCODING;
        Product product = createProduct(geocodingType);

        // execution
        String xml1 = writeXml(product);
        String xml2 = writeXml(createProductFromXML(xml1));

        // verification
        assertEquals(xml1, xml2);
    }

    @Test
    @Ignore //can not work becaus GCP Points are not writen to the XML header
    public void testCreateProduct_with_GcpGeoCoding() throws ParseException {
        // preparation
        int geocodingType = GCP_GEOCODING;
        Product product = createProduct(geocodingType);

        // execution
        String xml1 = writeXml(product);
        Product productFromXML = createProductFromXML(xml1);
        GeoCoding gc1 = product.getSceneGeoCoding();
        GeoCoding gc2 = productFromXML.getSceneGeoCoding();

        String xml2 = writeXml(productFromXML);
        assertEquals(xml1, xml2);
    }

    @Test
    public void testCanReadOldUtcFormat() {
        // preparation
        int geocodingType = TIE_POINT_GEOCODING;
        final boolean oldUtcFormat = true;
        final boolean newUtcFormat = false;
        final String oldUtcStyle = getExpectedXML(null, geocodingType, false, oldUtcFormat);

        // execution
        final Product product = createProductFromXML(oldUtcStyle);
        String newExport = writeXml(product);

        // verification
        final String newUtcStyleExpected = getExpectedXML(null, geocodingType, false, newUtcFormat);
        assertEquals(newUtcStyleExpected, newExport);
    }

    private Document createDom(String xml) {
        final InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        return DimapProductHelpers.createDom(inputStream);
    }

    @Test
    public void testGetTiePointDataFile() throws ParseException {
        Product product = createProduct(TIE_POINT_GEOCODING);
        Document dom1 = createDOM(product, _nameDataDirectory);
        String file = DimapProductHelpers.getTiePointDataFile(dom1, product.getTiePointGridNames()[0]);
        String expected = _nameDataDirectory + File.separator + DimapProductConstants.TIE_POINT_GRID_DIR_NAME + File.separator + product.getTiePointGridNames()[0];
        assertEquals(expected + EnviHeader.FILE_EXTENSION, file);
    }

    @Test
    public void testGetBandDataFiles() throws ParseException {
        final Product product = createProduct(TIE_POINT_GEOCODING);
        final Document dom1 = createDOM(product, _nameDataDirectory);
        final Band[] bands = product.getBands();
        final File inputDir = new File("inputPath");

        //Test jDom contains no root element
        final Map emptyMap = DimapProductHelpers.getBandDataFiles(new Document(), product, inputDir);
        assertNotNull("bandDataFiles must be not null", emptyMap);
        assertEquals("bandDataFiles must be empty", 0, emptyMap.size());

        //Test alles ok
        final Map bandDataFiles = DimapProductHelpers.getBandDataFiles(dom1, product, inputDir);

        assertEquals(bands.length, bandDataFiles.size());
        for (final Band band : bands) {
            final String expectedDimapDataFilePath = _nameDataDirectory + File.separator + band.getName() + DimapProductConstants.IMAGE_FILE_EXTENSION;
            final String expectedCompleteDataFilePath = inputDir.getPath() + File.separator + expectedDimapDataFilePath;
            final File bandDataFile = (File) bandDataFiles.get(band);
            assertEquals(expectedCompleteDataFilePath, bandDataFile.getPath());
        }

        final Band firstBand = bands[0];
        final Stx stx = firstBand.getStx();
        assertNotNull(stx);
        assertEquals(-0.2, stx.getMinimum(), 1.0e-6);
        assertEquals(3.0, stx.getMaximum(), 1.0e-6);
        assertEquals(5.5, stx.getMean(), 1.0e-6);
        assertEquals(3.67, stx.getStandardDeviation(), 1.0e-6);

        //Test jDom ist null
        try {
            DimapProductHelpers.getBandDataFiles(null, product, inputDir);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            final String expectedString = "dom";
            assertTrue("Error message must contain '" + expectedString + "'",
                       e.getMessage().indexOf(expectedString) > -1);
        }

        //Test product ist null
        try {
            DimapProductHelpers.getBandDataFiles(dom1, null, inputDir);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            final String expectedString = "product";
            assertTrue("Error message must contain '" + expectedString + "'",
                       e.getMessage().indexOf(expectedString) > -1);
        }

        //Test inputDir ist null
        try {
            DimapProductHelpers.getBandDataFiles(dom1, product, null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            final String expectedString = "inputDir";
            assertTrue("Error message must contain '" + expectedString + "'",
                       e.getMessage().indexOf(expectedString) > -1);
        }
    }

//***************************************************************************************
//************************************ END OF PUBLIC ************************************
//***************************************************************************************

    private Product createProductFromXML(String code) {
        final Document dom = createDom(code);
        Product product = DimapProductHelpers.createProduct(dom);
        GeoCoding[] geocodings = DimapProductHelpers.createGeoCoding(dom, product);
        if (geocodings != null && geocodings.length > 0) {
            final GeoCoding geoCoding = geocodings[0];
            product.setSceneGeoCoding(geoCoding);
        }
        DimapProductHelpers.addMaskUsages(dom, product);
        return product;
    }

    private String writeXml(Product product) {
        StringWriter sw;
        sw = new StringWriter();
        DimapHeaderWriter writer = new DimapHeaderWriter(product, sw, _nameDataDirectory);
        writer.writeHeader();
        return sw.toString();
    }

    private Product createProduct(int geocodingType) {
        Product product = new Product("test_product", BeamConstants.MERIS_FR_L1B_PRODUCT_TYPE_NAME, 1121, 2241);
        product.setDescription("description");
        product.setStartTime(new ProductData.UTC(1234, 2045, 34));
        product.setEndTime(new ProductData.UTC(1234, 3045, 34));
        addMasks(product);
        addTiePointGrids(product);
        addGeocoding(product, geocodingType);
        addBands(product);
        addVirtualBands(product);
        addSampleCodings(product);
        addMetadata(product);
        return product;
    }

    private void addMasks(Product product) {
        product.addMask("name1", "sin(X)", "mask.description1", Color.black, 1.0F);
        product.addMask("name2", "cos(X)", "mask.description2", Color.blue, 0.75F);
        product.addMask("name3", "tan(Y)", "mask.description3", Color.green, 0.2341F);
    }

    private void addMetadata(Product product) {
        final MetadataElement metadataRoot = product.getMetadataRoot();
        metadataRoot.setDescription("metadata-desc");

        final ProductData productData1 = ProductData.createInstance(ProductData.TYPE_INT16);
        productData1.setElemInt(123);
        final MetadataAttribute attribute1 = new MetadataAttribute("attrib1", productData1, false);
        metadataRoot.addAttribute(attribute1);

        final ProductData productData2 = ProductData.createInstance(new float[]{1, 2, 3, 4, 5, 6});
        final MetadataAttribute attribute2 = new MetadataAttribute("attrib2", productData2, true);
        metadataRoot.addAttribute(attribute2);

        final ProductData productData3 = ProductData.createInstance(new double[]{7, 8, 9, 10, 11, 12});
        final MetadataAttribute attribute3 = new MetadataAttribute("attrib3", productData3, false);
        metadataRoot.addAttribute(attribute3);

        final MetadataElement mdElem1 = new MetadataElement("mdElemName1");
        mdElem1.setDescription("mdElem1-desc");
        metadataRoot.addElement(mdElem1);

        final ProductData productData4 = ProductData.createInstance(new double[]{23.547, -8.0001, -59.989898});
        final MetadataAttribute attribute4 = new MetadataAttribute("attrib4", productData4, true);
        mdElem1.addAttribute(attribute4);
        MetadataAttribute stringAttrib = new MetadataAttribute("StringAttrib",
                                                               ProductData.createInstance("StringAttribValue"), true);
        mdElem1.addAttribute(stringAttrib);

        final ProductData productData5 = ProductData.createInstance(ProductData.TYPE_UTC);
        productData5.setElemIntAt(0, 123);
        productData5.setElemIntAt(1, 234);
        productData5.setElemIntAt(2, 345);
        final MetadataAttribute attribute5 = new MetadataAttribute("attrib5", productData5, false);
        metadataRoot.addAttribute(attribute5);
    }

    private void addGeocoding(Product product, int codingType) {
        switch (codingType) {
            case MAP_GEOCODING:
                final LambertConformalConicDescriptor descriptor = new LambertConformalConicDescriptor();
                final MapTransform transform = descriptor.createTransform(new double[]{12, 13, 14, 15, 16, 17, 18});
                final MapProjection projection = new MapProjection(LambertConformalConicDescriptor.NAME, transform);
                MapInfo mapInfo = new MapInfo(projection, 1f, 2f, 3f, 4f, 5f, 6f, Datum.WGS_84);
                mapInfo.setOrientation(7.3f);
                mapInfo.setSceneWidth(product.getSceneRasterWidth());
                mapInfo.setSceneHeight(product.getSceneRasterHeight());
                mapInfo.setElevationModelName("GETASSE30");
                final MapGeoCoding mapGeoCoding = new MapGeoCoding(mapInfo);
                product.setSceneGeoCoding(mapGeoCoding);
                break;
            case GCP_GEOCODING:
                final double[] x = new double[]{1.0, 2.0, 3.0};
                final double[] y = new double[]{4.0, 5.0, 6.0};

                final double[] lons = new double[]{1.0, 2.0, 3.0};
                final double[] lats = new double[]{4.0, 5.0, 6.0};

                final GcpGeoCoding gcpGeoCoding = new GcpGeoCoding(GcpGeoCoding.Method.POLYNOMIAL1, x, y, lons, lats,
                                                                   product.getSceneRasterWidth(),
                                                                   product.getSceneRasterHeight(),
                                                                   Datum.WGS_84);
                gcpGeoCoding.setOriginalGeoCoding(new TiePointGeoCoding(product.getTiePointGrid("tpg1"),
                                                                        product.getTiePointGrid("tpg2"),
                                                                        Datum.WGS_84));
                product.setSceneGeoCoding(gcpGeoCoding);
                break;
            default:
                final TiePointGeoCoding tiePointGeoCoding = new TiePointGeoCoding(product.getTiePointGrid("tpg1"),
                                                                                  product.getTiePointGrid("tpg2"),
                                                                                  Datum.WGS_84);
                product.setSceneGeoCoding(tiePointGeoCoding);
        }
    }

    private void addTiePointGrids(Product product) {
        final int sceneRasterWidth = product.getSceneRasterWidth();
        final int sceneRasterHeight = product.getSceneRasterHeight();
        final TiePointGrid tiePointGrid = createTiePointGrid("tpg1", sceneRasterWidth, sceneRasterHeight, 21.1, 14.2,
                                                             16.3, 32.004,
                                                             false);
        product.addTiePointGrid(tiePointGrid);

        tiePointGrid.getOverlayMaskGroup().add(product.getMaskGroup().get("name2"));
        tiePointGrid.getOverlayMaskGroup().add(product.getMaskGroup().get("name3"));

        product.addTiePointGrid(
                    createTiePointGrid("tpg2", sceneRasterWidth, sceneRasterHeight, 21.1, 14.2, 16.3, 32.004, true));
    }

    private TiePointGrid createTiePointGrid(String name, int sceneW, int sceneH, double offX, double offY, double stepX,
                                            double stepY, boolean cyclic) {
        int gridWidth = (int) Math.round((sceneW - 1) / stepX + 1);
        int gridHeight = (int) Math.round((sceneH - 1) / stepY + 1);
        float[] floats = new float[gridWidth * gridHeight];
        for (int i = 0; i < floats.length; i++) {
            floats[i] = (float) (20.0 + 10.0 * Math.random());
        }
        TiePointGrid tpg = null;
        if (!cyclic) {
            tpg = new TiePointGrid(name,
                                   gridWidth, gridHeight,
                                   offX, offY,
                                   stepX, stepY,
                                   floats);
        } else {
            tpg = new TiePointGrid(name,
                                   gridWidth, gridHeight,
                                   offX, offY,
                                   stepX, stepY,
                                   floats,
                                   TiePointGrid.DISCONT_AT_180);
        }
        tpg.setDescription(name + "-Description");
        tpg.setUnit(name + "-unit");
        return tpg;
    }

    private void addSampleCodings(Product product) {
        String codingName1 = "FlagCoding1";
        FlagCoding flagCoding1 = new FlagCoding(codingName1);
        flagCoding1.addFlag("Flag1A", 0, "Flag1A-Description");
        flagCoding1.addFlag("Flag1B", 1, "Flag1B-Description");
        flagCoding1.addFlag("Flag1C", 2, "Flag1C-Description");
        product.getFlagCodingGroup().add(flagCoding1);
        // Add flag coding to band
        product.getBand("Flags1").setSampleCoding(product.getFlagCodingGroup().get(codingName1));

        String codingName2 = "FlagCoding2";
        FlagCoding flagCoding2 = new FlagCoding(codingName2);
        flagCoding2.addFlag("Flag2A", 5, "Flag2A-Description");
        flagCoding2.addFlag("Flag2B", 6, "Flag2B-Description");
        product.getFlagCodingGroup().add(flagCoding2);
        // Add flag coding to band
        product.getBand("Flags2").setSampleCoding(product.getFlagCodingGroup().get(codingName2));

        String codingName3 = "IndexCoding";
        IndexCoding indexCoding = new IndexCoding(codingName3);
        indexCoding.addIndex("Index1", 0, "Index1-Description");
        indexCoding.addIndex("Index2", 1, "Index2-Description");
        indexCoding.addIndex("Index3", 2, "Index3-Description");
        product.getIndexCodingGroup().add(indexCoding);
        // Add flag coding to band
        product.getBand("Index").setSampleCoding(indexCoding);
    }

    private void addBands(Product product) {
        final int sceneRasterWidth = product.getSceneRasterWidth();
        final int sceneRasterHeight = product.getSceneRasterHeight();

        Band band1 = new Band("Band1", ProductData.TYPE_INT16, sceneRasterWidth, sceneRasterHeight);
        band1.setUnit("unit for " + band1.getName());
        band1.setDescription(band1.getName() + "-Description");
        band1.setImageInfo(createImageInfo());
        band1.setSolarFlux(0.12f);
        band1.setSpectralWavelength(23.45f);
        band1.setSpectralBandIndex(0);
        band1.setValidPixelExpression("Flags1.Flag1C");
        band1.setStx(createStx());
        product.addBand(band1);

        Band band2 = new Band("Band2", ProductData.TYPE_INT8, sceneRasterWidth, sceneRasterHeight);
        band2.setUnit("unit for " + band2.getName());
        band2.setDescription(band2.getName() + "-Description");
        band2.setSolarFlux(0.23f);
        band2.setSpectralWavelength(243.56f);
        band2.setSpectralBandIndex(3);
        product.addBand(band2);

        band2.getOverlayMaskGroup().add(product.getMaskGroup().get("name1"));
        band2.getOverlayMaskGroup().add(product.getMaskGroup().get("name3"));

        Band flags1 = new Band("Flags1", ProductData.TYPE_INT8, sceneRasterWidth, sceneRasterHeight);
        flags1.setDescription(flags1.getName() + "-Description");
        flags1.setSpectralBandIndex(-1);
        product.addBand(flags1);

        Band flags2 = new Band("Flags2", ProductData.TYPE_INT8, sceneRasterWidth, sceneRasterHeight);
        flags2.setDescription(flags2.getName() + "-Description");
        flags2.setSpectralBandIndex(-1);
        product.addBand(flags2);

        Band index = new Band("Index", ProductData.TYPE_UINT16, sceneRasterWidth, sceneRasterHeight);
        index.setDescription(index.getName() + "-Description");
        index.setSpectralBandIndex(-1);
        product.addBand(index);
    }

    private void addVirtualBands(Product product) {
        final int sceneRasterWidth = product.getSceneRasterWidth();
        final int sceneRasterHeight = product.getSceneRasterHeight();
        VirtualBand virtualBand = new VirtualBand("vb1", ProductData.TYPE_FLOAT32, sceneRasterWidth, sceneRasterHeight,
                                                  "radiance_8");
        virtualBand.setNoDataValue(3f);
        virtualBand.setNoDataValueUsed(true);
        virtualBand.setDescription("VirtualBand-Description");
        product.addBand(virtualBand);
    }

    private Stx createStx() {
        return new StxFactory()
                    .withMinimum(-0.2)
                    .withMaximum(3)
                    .withMean(5.5)
                    .withStandardDeviation(3.67)
                    .withHistogramBins(new int[]{4, 5, 4, 7, 5, 8})
                    .create();
    }

    private ImageInfo createImageInfo() {
        ColorPaletteDef.Point[] points = new ColorPaletteDef.Point[3];
        points[0] = new ColorPaletteDef.Point(0.1d, Color.black); //black = new Color(  0,   0,   0, 255)
        points[1] = new ColorPaletteDef.Point(1.3d, Color.cyan);  //cyan  = new Color(  0, 255, 255, 255)
        points[2] = new ColorPaletteDef.Point(2.8d, Color.white); //white = new Color(255, 255, 255, 255)
        ColorPaletteDef paleteDefinition = new ColorPaletteDef(points, 180);
        final ImageInfo imageInfo = new ImageInfo(paleteDefinition);
        imageInfo.setNoDataColor(Color.BLUE);
        imageInfo.setHistogramMatching(ImageInfo.HistogramMatching.Normalize);
        return imageInfo;
    }

    public String getExpectedXML(Product product, int geocodingType, boolean withGeocoding,
                                 final boolean oldUtcFormat) {
        final String dataDir = _nameDataDirectory + "/";

        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);

        pw.println("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
        pw.println("<Dimap_Document name=\"test_product.dim\">");
        pw.println("    <Metadata_Id>");
        pw.println("        <METADATA_FORMAT version=\"" + DimapProductConstants.DIMAP_CURRENT_VERSION + "\">DIMAP</METADATA_FORMAT>");
        pw.println("        <METADATA_PROFILE>BEAM-DATAMODEL-V1</METADATA_PROFILE>");
        pw.println("    </Metadata_Id>");
        pw.println("    <Dataset_Id>");
        pw.println("        <DATASET_SERIES>BEAM-PRODUCT</DATASET_SERIES>");
        pw.println("        <DATASET_NAME>test_product</DATASET_NAME>"); // product name
        pw.println("    </Dataset_Id>");
        pw.println("    <Dataset_Use>");
        pw.println("        <DATASET_COMMENTS>description</DATASET_COMMENTS>");
        pw.println("    </Dataset_Use>");
        pw.println("    <Production>");
        pw.println("        <DATASET_PRODUCER_NAME />"); // product type
        // "        <DATASET_PRODUCER_NAME>" + DimapProductConstants.DATASET_PRODUCER_NAME + "</DATASET_PRODUCER_NAME>" // product type
        pw.println("        <PRODUCT_TYPE>MER_FR__1P</PRODUCT_TYPE>"); // product type
        pw.println("        <PRODUCT_SCENE_RASTER_START_TIME>19-MAY-2003 00:34:05.000034</PRODUCT_SCENE_RASTER_START_TIME>"); // product scene sensing start
        pw.println("        <PRODUCT_SCENE_RASTER_STOP_TIME>19-MAY-2003 00:50:45.000034</PRODUCT_SCENE_RASTER_STOP_TIME>"); // product scene sensing stopt
        pw.println("    </Production>");

        if (withGeocoding) {
            switch (geocodingType) {
                case MAP_GEOCODING:
                    final MapInfo mapInfo = ((MapGeoCoding) product.getSceneGeoCoding()).getMapInfo();
                    pw.println("    <Coordinate_Reference_System>");
                    pw.println("        <GEO_TABLES version=\"1.0\">CUSTOM</GEO_TABLES>");
                    pw.println("        <Horizontal_CS>");
                    pw.println("            <HORIZONTAL_CS_TYPE>PROJECTED</HORIZONTAL_CS_TYPE>");
                    pw.println("            <HORIZONTAL_CS_NAME>Lambert Conformal Conic</HORIZONTAL_CS_NAME>");
                    pw.println("            <Geographic_CS>");
                    pw.println("                <GEOGRAPHIC_CS_NAME>Lambert Conformal Conic</GEOGRAPHIC_CS_NAME>");
                    pw.println("                <Horizontal_Datum>");
                    pw.println("                    <HORIZONTAL_DATUM_NAME>WGS-84</HORIZONTAL_DATUM_NAME>");
                    pw.println("                    <Ellipsoid>");
                    pw.println("                        <ELLIPSOID_NAME>WGS-84</ELLIPSOID_NAME>");
                    pw.println("                        <Ellipsoid_Parameters>");
                    pw.println("                            <ELLIPSOID_MAJ_AXIS unit=\"meter\">6378137.0</ELLIPSOID_MAJ_AXIS>");
                    pw.println("                            <ELLIPSOID_MIN_AXIS unit=\"meter\">6356752.3</ELLIPSOID_MIN_AXIS>");
                    pw.println("                        </Ellipsoid_Parameters>");
                    pw.println("                    </Ellipsoid>");
                    pw.println("                </Horizontal_Datum>");
                    pw.println("            </Geographic_CS>");
                    pw.println("            <Projection>");
                    pw.println("                <NAME>Lambert Conformal Conic</NAME>");
                    pw.println("                <Projection_CT_Method>");
                    pw.println("                    <PROJECTION_CT_NAME>Lambert_Conformal_Conic</PROJECTION_CT_NAME>");
                    pw.println("                    <Projection_Parameters>");
                    pw.println("                        <Projection_Parameter>");
                    pw.println("                            <PROJECTION_PARAMETER_NAME>semi_major</PROJECTION_PARAMETER_NAME>");
                    pw.println("                            <PROJECTION_PARAMETER_VALUE unit=\"meter\">6378137.0</PROJECTION_PARAMETER_VALUE>");
                    pw.println("                        </Projection_Parameter>");
                    pw.println("                        <Projection_Parameter>");
                    pw.println("                            <PROJECTION_PARAMETER_NAME>semi_minor</PROJECTION_PARAMETER_NAME>");
                    pw.println("                            <PROJECTION_PARAMETER_VALUE unit=\"meter\">6356752.3</PROJECTION_PARAMETER_VALUE>");
                    pw.println("                        </Projection_Parameter>");
                    pw.println("                        <Projection_Parameter>");
                    pw.println("                            <PROJECTION_PARAMETER_NAME>latitude_of_origin</PROJECTION_PARAMETER_NAME>");
                    pw.println("                            <PROJECTION_PARAMETER_VALUE unit=\"degree\">14.0</PROJECTION_PARAMETER_VALUE>");
                    pw.println("                        </Projection_Parameter>");
                    pw.println("                        <Projection_Parameter>");
                    pw.println("                            <PROJECTION_PARAMETER_NAME>central_meridian</PROJECTION_PARAMETER_NAME>");
                    pw.println("                            <PROJECTION_PARAMETER_VALUE unit=\"degree\">15.0</PROJECTION_PARAMETER_VALUE>");
                    pw.println("                        </Projection_Parameter>");
                    pw.println("                        <Projection_Parameter>");
                    pw.println("                            <PROJECTION_PARAMETER_NAME>latitude_of_intersection_1</PROJECTION_PARAMETER_NAME>");
                    pw.println("                            <PROJECTION_PARAMETER_VALUE unit=\"degree\">16.0</PROJECTION_PARAMETER_VALUE>");
                    pw.println("                        </Projection_Parameter>");
                    pw.println("                        <Projection_Parameter>");
                    pw.println("                            <PROJECTION_PARAMETER_NAME>latitude_of_intersection_2</PROJECTION_PARAMETER_NAME>");
                    pw.println("                            <PROJECTION_PARAMETER_VALUE unit=\"degree\">17.0</PROJECTION_PARAMETER_VALUE>");
                    pw.println("                        </Projection_Parameter>");
                    pw.println("                        <Projection_Parameter>");
                    pw.println("                            <PROJECTION_PARAMETER_NAME>scale_factor</PROJECTION_PARAMETER_NAME>");
                    pw.println("                            <PROJECTION_PARAMETER_VALUE unit=\"\">18.0</PROJECTION_PARAMETER_VALUE>");
                    pw.println("                        </Projection_Parameter>");
                    pw.println("                    </Projection_Parameters>");
                    pw.println("                </Projection_CT_Method>");
                    pw.println("            </Projection>");
                    pw.println("            <MAP_INFO>");
                    pw.println("                <PIXEL_X value=\"" + mapInfo.getPixelX() + "\" />");
                    pw.println("                <PIXEL_Y value=\"" + mapInfo.getPixelY() + "\" />");
                    pw.println("                <EASTING value=\"" + mapInfo.getEasting() + "\" />");
                    pw.println("                <NORTHING value=\"" + mapInfo.getNorthing() + "\" />");
                    pw.println("                <ORIENTATION value=\"" + mapInfo.getOrientation() + "\" />");
                    pw.println("                <PIXELSIZE_X value=\"" + mapInfo.getPixelSizeX() + "\" />");
                    pw.println("                <PIXELSIZE_Y value=\"" + mapInfo.getPixelSizeY() + "\" />");
                    pw.println("                <NODATA_VALUE value=\"" + mapInfo.getNoDataValue() + "\" />");
                    pw.println("                <MAPUNIT value=\"" + mapInfo.getMapProjection().getMapUnit() + "\" />");
                    pw.println("                <ORTHORECTIFIED value=\"" + mapInfo.isOrthorectified() + "\" />");
                    pw.println("                <ELEVATION_MODEL value=\"" + mapInfo.getElevationModelName() + "\" />");
                    pw.println("                <SCENE_FITTED value=\"" + mapInfo.isSceneSizeFitted() + "\" />");
                    pw.println("                <SCENE_WIDTH value=\"" + mapInfo.getSceneWidth() + "\" />");
                    pw.println("                <SCENE_HEIGHT value=\"" + mapInfo.getSceneHeight() + "\" />");
                    pw.println("                <RESAMPLING value=\"" + mapInfo.getResampling().getName() + "\" />");
                    pw.println("            </MAP_INFO>");
                    pw.println("        </Horizontal_CS>");
                    pw.println("    </Coordinate_Reference_System>");
                    break;
                case GCP_GEOCODING:
                    pw.println("    <Coordinate_Reference_System>");
                    pw.println("        <Horizontal_CS>");
                    pw.println("            <HORIZONTAL_CS_TYPE>GEOGRAPHIC</HORIZONTAL_CS_TYPE>");
                    pw.println("            <Geographic_CS>");
                    pw.println("                <Horizontal_Datum>");
                    pw.println("                    <HORIZONTAL_DATUM_NAME>WGS-84</HORIZONTAL_DATUM_NAME>");
                    pw.println("                    <Ellipsoid>");
                    pw.println("                        <ELLIPSOID_NAME>WGS-84</ELLIPSOID_NAME>");
                    pw.println("                        <Ellipsoid_Parameters>");
                    pw.println("                            <ELLIPSOID_MAJ_AXIS unit=\"M\">6378137.0</ELLIPSOID_MAJ_AXIS>");
                    pw.println("                            <ELLIPSOID_MIN_AXIS unit=\"M\">6356752.3</ELLIPSOID_MIN_AXIS>");
                    pw.println("                        </Ellipsoid_Parameters>");
                    pw.println("                    </Ellipsoid>");
                    pw.println("                </Horizontal_Datum>");
                    pw.println("            </Geographic_CS>");
                    pw.println("        </Horizontal_CS>");
                    pw.println("    </Coordinate_Reference_System>");
                    pw.println("    <Geoposition>");
                    pw.println("        <Geoposition_Points>");
                    pw.println("            <INTERPOLATION_METHOD>POLYNOMIAL1</INTERPOLATION_METHOD>");
                    pw.println("            <Original_Geocoding>");
                    pw.println("                <Coordinate_Reference_System>");
                    pw.println("                    <Horizontal_CS>");
                    pw.println("                        <HORIZONTAL_CS_TYPE>GEOGRAPHIC</HORIZONTAL_CS_TYPE>");
                    pw.println("                        <Geographic_CS>");
                    pw.println("                            <Horizontal_Datum>");
                    pw.println("                                <HORIZONTAL_DATUM_NAME>WGS-84</HORIZONTAL_DATUM_NAME>");
                    pw.println("                                <Ellipsoid>");
                    pw.println("                                    <ELLIPSOID_NAME>WGS-84</ELLIPSOID_NAME>");
                    pw.println("                                    <Ellipsoid_Parameters>");
                    pw.println("                                        <ELLIPSOID_MAJ_AXIS unit=\"M\">6378137.0</ELLIPSOID_MAJ_AXIS>");
                    pw.println("                                        <ELLIPSOID_MIN_AXIS unit=\"M\">6356752.3</ELLIPSOID_MIN_AXIS>");
                    pw.println("                                    </Ellipsoid_Parameters>");
                    pw.println("                                </Ellipsoid>");
                    pw.println("                            </Horizontal_Datum>");
                    pw.println("                        </Geographic_CS>");
                    pw.println("                    </Horizontal_CS>");
                    pw.println("                </Coordinate_Reference_System>");
                    pw.println("                <Geoposition>");
                    pw.println("                    <Geoposition_Points>");
                    pw.println("                        <TIE_POINT_GRID_NAME_LAT>tpg1</TIE_POINT_GRID_NAME_LAT>");
                    pw.println("                        <TIE_POINT_GRID_NAME_LON>tpg2</TIE_POINT_GRID_NAME_LON>");
                    pw.println("                    </Geoposition_Points>");
                    pw.println("                </Geoposition>");
                    pw.println("            </Original_Geocoding>");
                    pw.println("        </Geoposition_Points>");
                    pw.println("    </Geoposition>");
                    break;
                case TIE_POINT_GEOCODING:
                    pw.println("    <Coordinate_Reference_System>");
                    pw.println("        <Horizontal_CS>");
                    pw.println("            <HORIZONTAL_CS_TYPE>GEOGRAPHIC</HORIZONTAL_CS_TYPE>");
                    pw.println("            <Geographic_CS>");
                    pw.println("                <Horizontal_Datum>");
                    pw.println("                    <HORIZONTAL_DATUM_NAME>WGS-84</HORIZONTAL_DATUM_NAME>");
                    pw.println("                    <Ellipsoid>");
                    pw.println("                        <ELLIPSOID_NAME>WGS-84</ELLIPSOID_NAME>");
                    pw.println("                        <Ellipsoid_Parameters>");
                    pw.println("                            <ELLIPSOID_MAJ_AXIS unit=\"M\">6378137.0</ELLIPSOID_MAJ_AXIS>");
                    pw.println("                            <ELLIPSOID_MIN_AXIS unit=\"M\">6356752.3</ELLIPSOID_MIN_AXIS>");
                    pw.println("                        </Ellipsoid_Parameters>");
                    pw.println("                    </Ellipsoid>");
                    pw.println("                </Horizontal_Datum>");
                    pw.println("            </Geographic_CS>");
                    pw.println("        </Horizontal_CS>");
                    pw.println("    </Coordinate_Reference_System>");
                    pw.println("    <Geoposition>");
                    pw.println("        <Geoposition_Points>");
                    pw.println("            <TIE_POINT_GRID_NAME_LAT>tpg1</TIE_POINT_GRID_NAME_LAT>");
                    pw.println("            <TIE_POINT_GRID_NAME_LON>tpg2</TIE_POINT_GRID_NAME_LON>");
                    pw.println("        </Geoposition_Points>");
                    pw.println("    </Geoposition>");
                    break;
                default: // old tie-point geo-coding XML
                    pw.println("    <Coordinate_Reference_System>");
                    pw.println("        <Geocoding_Tie_Point_Grids>");
                    pw.println("            <TIE_POINT_GRID_NAME_LAT>tpg1</TIE_POINT_GRID_NAME_LAT>");
                    pw.println("            <TIE_POINT_GRID_NAME_LON>tpg2</TIE_POINT_GRID_NAME_LON>");
                    pw.println("        </Geocoding_Tie_Point_Grids>");
                    pw.println("    </Coordinate_Reference_System>");
            }
        }

        pw.println("    <Flag_Coding name=\"FlagCoding1\">");
        pw.println("        <Flag>");
        pw.println("            <Flag_Name>Flag1A</Flag_Name>");
        pw.println("            <Flag_Index>0</Flag_Index>");
        pw.println("            <Flag_description>Flag1A-Description</Flag_description>");
        pw.println("        </Flag>");
        pw.println("        <Flag>");
        pw.println("            <Flag_Name>Flag1B</Flag_Name>");
        pw.println("            <Flag_Index>1</Flag_Index>");
        pw.println("            <Flag_description>Flag1B-Description</Flag_description>");
        pw.println("        </Flag>");
        pw.println("        <Flag>");
        pw.println("            <Flag_Name>Flag1C</Flag_Name>");
        pw.println("            <Flag_Index>2</Flag_Index>");
        pw.println("            <Flag_description>Flag1C-Description</Flag_description>");
        pw.println("        </Flag>");
        pw.println("    </Flag_Coding>");
        pw.println("    <Flag_Coding name=\"FlagCoding2\">");
        pw.println("        <Flag>");
        pw.println("            <Flag_Name>Flag2A</Flag_Name>");
        pw.println("            <Flag_Index>5</Flag_Index>");
        pw.println("            <Flag_description>Flag2A-Description</Flag_description>");
        pw.println("        </Flag>");
        pw.println("        <Flag>");
        pw.println("            <Flag_Name>Flag2B</Flag_Name>");
        pw.println("            <Flag_Index>6</Flag_Index>");
        pw.println("            <Flag_description>Flag2B-Description</Flag_description>");
        pw.println("        </Flag>");
        pw.println("    </Flag_Coding>");
        pw.println("    <Index_Coding name=\"IndexCoding\">");
        pw.println("        <Index>");
        pw.println("            <INDEX_NAME>Index1</INDEX_NAME>");
        pw.println("            <INDEX_VALUE>0</INDEX_VALUE>");
        pw.println("            <INDEX_DESCRIPTION>Index1-Description</INDEX_DESCRIPTION>");
        pw.println("        </Index>");
        pw.println("        <Index>");
        pw.println("            <INDEX_NAME>Index2</INDEX_NAME>");
        pw.println("            <INDEX_VALUE>1</INDEX_VALUE>");
        pw.println("            <INDEX_DESCRIPTION>Index2-Description</INDEX_DESCRIPTION>");
        pw.println("        </Index>");
        pw.println("        <Index>");
        pw.println("            <INDEX_NAME>Index3</INDEX_NAME>");
        pw.println("            <INDEX_VALUE>2</INDEX_VALUE>");
        pw.println("            <INDEX_DESCRIPTION>Index3-Description</INDEX_DESCRIPTION>");
        pw.println("        </Index>");
        pw.println("    </Index_Coding>");
        pw.println("    <Raster_Dimensions>");
        pw.println("        <NCOLS>1121</NCOLS>"); // scene width
        pw.println("        <NROWS>2241</NROWS>"); // scene height
        pw.println("        <NBANDS>6</NBANDS>"); // num Bands
        pw.println("    </Raster_Dimensions>");
        pw.println("    <Data_Access>");
        pw.println("        <DATA_FILE_FORMAT>" + DimapProductConstants.DATA_FILE_FORMAT + "</DATA_FILE_FORMAT>");
        pw.println("        <DATA_FILE_FORMAT_DESC>" + DimapProductConstants.DATA_FILE_FORMAT_DESCRIPTION + "</DATA_FILE_FORMAT_DESC>");
        pw.println("        <DATA_FILE_ORGANISATION>" + DimapProductConstants.DATA_FILE_ORGANISATION + "</DATA_FILE_ORGANISATION>");
        pw.println("        <Data_File>");
        pw.println("            <DATA_FILE_PATH href=\"" + dataDir + "Band1.hdr\" />");
        pw.println("            <BAND_INDEX>0</BAND_INDEX>");
        pw.println("        </Data_File>");
        pw.println("        <Data_File>");
        pw.println("            <DATA_FILE_PATH href=\"" + dataDir + "Band2.hdr\" />");
        pw.println("            <BAND_INDEX>1</BAND_INDEX>");
        pw.println("        </Data_File>");
        pw.println("        <Data_File>");
        pw.println("            <DATA_FILE_PATH href=\"" + dataDir + "Flags1.hdr\" />");
        pw.println("            <BAND_INDEX>2</BAND_INDEX>");
        pw.println("        </Data_File>");
        pw.println("        <Data_File>");
        pw.println("            <DATA_FILE_PATH href=\"" + dataDir + "Flags2.hdr\" />");
        pw.println("            <BAND_INDEX>3</BAND_INDEX>");
        pw.println("        </Data_File>");
        pw.println("        <Data_File>");
        pw.println("            <DATA_FILE_PATH href=\"" + dataDir + "Index.hdr\" />");
        pw.println("            <BAND_INDEX>4</BAND_INDEX>");
        pw.println("        </Data_File>");
        pw.println("        <Tie_Point_Grid_File>");
        pw.println("            <TIE_POINT_GRID_FILE_PATH href=\"" + dataDir + DimapProductConstants.TIE_POINT_GRID_DIR_NAME + "/tpg1.hdr\" />");
        pw.println("            <TIE_POINT_GRID_INDEX>0</TIE_POINT_GRID_INDEX>");
        pw.println("        </Tie_Point_Grid_File>");
        pw.println("        <Tie_Point_Grid_File>");
        pw.println("            <TIE_POINT_GRID_FILE_PATH href=\"" + dataDir + DimapProductConstants.TIE_POINT_GRID_DIR_NAME + "/tpg2.hdr\" />");
        pw.println("            <TIE_POINT_GRID_INDEX>1</TIE_POINT_GRID_INDEX>");
        pw.println("        </Tie_Point_Grid_File>");
        pw.println("    </Data_Access>");
        pw.println("    <Tie_Point_Grids>");
        pw.println("        <NUM_TIE_POINT_GRIDS>2</NUM_TIE_POINT_GRIDS>");
        pw.println("        <Tie_Point_Grid_Info>");
        pw.println("            <TIE_POINT_GRID_INDEX>0</TIE_POINT_GRID_INDEX>");
        pw.println("            <TIE_POINT_DESCRIPTION>tpg1-Description</TIE_POINT_DESCRIPTION>");
        pw.println("            <PHYSICAL_UNIT>tpg1-unit</PHYSICAL_UNIT>");
        pw.println("            <TIE_POINT_GRID_NAME>tpg1</TIE_POINT_GRID_NAME>");
        pw.println("            <DATA_TYPE>float32</DATA_TYPE>");
        pw.println("            <NCOLS>70</NCOLS>");
        pw.println("            <NROWS>71</NROWS>");
        pw.println("            <OFFSET_X>" + 21.1 + "</OFFSET_X>");
        pw.println("            <OFFSET_Y>" + 14.2 + "</OFFSET_Y>");
        pw.println("            <STEP_X>" + 16.3 + "</STEP_X>");
        pw.println("            <STEP_Y>" + 32.004 + "</STEP_Y>");
        pw.println("            <CYCLIC>false</CYCLIC>");
        pw.println("        </Tie_Point_Grid_Info>");
        pw.println("        <Tie_Point_Grid_Info>");
        pw.println("            <TIE_POINT_GRID_INDEX>1</TIE_POINT_GRID_INDEX>");
        pw.println("            <TIE_POINT_DESCRIPTION>tpg2-Description</TIE_POINT_DESCRIPTION>");
        pw.println("            <PHYSICAL_UNIT>tpg2-unit</PHYSICAL_UNIT>");
        pw.println("            <TIE_POINT_GRID_NAME>tpg2</TIE_POINT_GRID_NAME>");
        pw.println("            <DATA_TYPE>float32</DATA_TYPE>");
        pw.println("            <NCOLS>70</NCOLS>");
        pw.println("            <NROWS>71</NROWS>");
        pw.println("            <OFFSET_X>" + 21.1 + "</OFFSET_X>");
        pw.println("            <OFFSET_Y>" + 14.2 + "</OFFSET_Y>");
        pw.println("            <STEP_X>" + 16.3 + "</STEP_X>");
        pw.println("            <STEP_Y>" + 32.004 + "</STEP_Y>");
        pw.println("            <CYCLIC>true</CYCLIC>");
        pw.println("        </Tie_Point_Grid_Info>");
        pw.println("    </Tie_Point_Grids>");
        pw.println("    <Image_Display>");
        pw.println("        <Band_Statistics>");
        pw.println("            <BAND_INDEX>0</BAND_INDEX>");
        pw.println("            <STX_MIN>-0.2</STX_MIN>");
        pw.println("            <STX_MAX>3.0</STX_MAX>");
        pw.println("            <STX_MEAN>5.5</STX_MEAN>");
        pw.println("            <STX_STD_DEV>3.67</STX_STD_DEV>");
        pw.println("            <STX_RES_LEVEL>0</STX_RES_LEVEL>");
        pw.println("            <HISTOGRAM>4,5,4,7,5,8</HISTOGRAM>");
        pw.println("            <NUM_COLORS>180</NUM_COLORS>");
        pw.println("            <Color_Palette_Point>");
        pw.println("                <SAMPLE>0.1</SAMPLE>");
        pw.println("                <COLOR red=\"0\" green=\"0\" blue=\"0\" alpha=\"255\" />");
        pw.println("            </Color_Palette_Point>");
        pw.println("            <Color_Palette_Point>");
        pw.println("                <SAMPLE>1.3</SAMPLE>");
        pw.println("                <COLOR red=\"0\" green=\"255\" blue=\"255\" alpha=\"255\" />");
        pw.println("            </Color_Palette_Point>");
        pw.println("            <Color_Palette_Point>");
        pw.println("                <SAMPLE>2.8</SAMPLE>");
        pw.println("                <COLOR red=\"255\" green=\"255\" blue=\"255\" alpha=\"255\" />");
        pw.println("            </Color_Palette_Point>");
        pw.println("            <NO_DATA_COLOR red=\"0\" green=\"0\" blue=\"255\" alpha=\"255\" />");
        pw.println("            <HISTOGRAM_MATCHING>Normalize</HISTOGRAM_MATCHING>");
        pw.println("        </Band_Statistics>");
        pw.println("        <Mask_Usage>");
        pw.println("            <BAND_INDEX>1</BAND_INDEX>");
        pw.println("            <OVERLAY names=\"name1,name3\" />");
        pw.println("        </Mask_Usage>");
        pw.println("        <Mask_Usage>");
        pw.println("            <TIE_POINT_GRID_INDEX>0</TIE_POINT_GRID_INDEX>");
        pw.println("            <OVERLAY names=\"name2,name3\" />");
        pw.println("        </Mask_Usage>");
        pw.println("    </Image_Display>");
        pw.println("    <Masks>");
        pw.println("        <Mask type=\"Maths\">");
        pw.println("            <NAME value=\"name1\" />");
        pw.println("            <MASK_RASTER_WIDTH value=\"1121\" />");
        pw.println("            <MASK_RASTER_HEIGHT value=\"2241\" />");
        pw.println("            <DESCRIPTION value=\"mask.description1\" />");
        pw.println("            <COLOR red=\"0\" green=\"0\" blue=\"0\" alpha=\"255\" />");
        pw.println("            <TRANSPARENCY value=\"1.0\" />");
        pw.println("            <EXPRESSION value=\"mask.expression1\" />");
        pw.println("        </Mask>");
        pw.println("        <Mask type=\"Maths\">");
        pw.println("            <NAME value=\"name2\" />");
        pw.println("            <MASK_RASTER_WIDTH value=\"1121\" />");
        pw.println("            <MASK_RASTER_HEIGHT value=\"2241\" />");
        pw.println("            <DESCRIPTION value=\"mask.description2\" />");
        pw.println("            <COLOR red=\"0\" green=\"0\" blue=\"255\" alpha=\"255\" />");
        pw.println("            <TRANSPARENCY value=\"0.75\" />");
        pw.println("            <EXPRESSION value=\"mask.expression2\" />");
        pw.println("        </Mask>");
        pw.println("        <Mask type=\"Maths\">");
        pw.println("            <NAME value=\"name3\" />");
        pw.println("            <MASK_RASTER_WIDTH value=\"1121\" />");
        pw.println("            <MASK_RASTER_HEIGHT value=\"2241\" />");
        pw.println("            <DESCRIPTION value=\"mask.description3\" />");
        pw.println("            <COLOR red=\"0\" green=\"255\" blue=\"0\" alpha=\"255\" />");
        pw.println("            <TRANSPARENCY value=\"0.23409999907016754\" />");
        pw.println("            <EXPRESSION value=\"mask.expression3\" />");
        pw.println("        </Mask>");
        pw.println("    </Masks>");
        pw.println("    <Image_Interpretation>");
        pw.println("        <Spectral_Band_Info>");
        pw.println("            <BAND_INDEX>0</BAND_INDEX>");
        pw.println("            <BAND_DESCRIPTION>Band1-Description</BAND_DESCRIPTION>");
        pw.println("            <BAND_NAME>Band1</BAND_NAME>"); // band name
        pw.println("            <BAND_RASTER_WIDTH>1121</BAND_RASTER_WIDTH>");
        pw.println("            <BAND_RASTER_HEIGHT>2241</BAND_RASTER_HEIGHT>");
        pw.println("            <DATA_TYPE>int16</DATA_TYPE>");
        pw.println("            <PHYSICAL_UNIT>unit for Band1</PHYSICAL_UNIT>");
        pw.println("            <SOLAR_FLUX>" + 0.12f + "</SOLAR_FLUX>");
        pw.println("            <SPECTRAL_BAND_INDEX>0</SPECTRAL_BAND_INDEX>");
        pw.println("            <BAND_WAVELEN>" + 23.45f + "</BAND_WAVELEN>");
        pw.println("            <BANDWIDTH>0.0</BANDWIDTH>");
        pw.println("            <SCALING_FACTOR>1.0</SCALING_FACTOR>");
        pw.println("            <SCALING_OFFSET>0.0</SCALING_OFFSET>");
        pw.println("            <LOG10_SCALED>false</LOG10_SCALED>");
        pw.println("            <NO_DATA_VALUE_USED>false</NO_DATA_VALUE_USED>");
        pw.println("            <NO_DATA_VALUE>0.0</NO_DATA_VALUE>");
        pw.println("            <VALID_MASK_TERM>Flags1.Flag1C</VALID_MASK_TERM>");
        pw.println("            <IMAGE_TO_MODEL_TRANSFORM>4.959472210835188,0.7623876714199131,0.6353230595165943,-5.951366653002227,-3.230118329868377,15.14034563458454</IMAGE_TO_MODEL_TRANSFORM>");
        pw.println("        </Spectral_Band_Info>");
        pw.println("        <Spectral_Band_Info>");
        pw.println("            <BAND_INDEX>1</BAND_INDEX>");
        pw.println("            <BAND_DESCRIPTION>Band2-Description</BAND_DESCRIPTION>");
        pw.println("            <BAND_NAME>Band2</BAND_NAME>"); // band name
        pw.println("            <BAND_RASTER_WIDTH>1121</BAND_RASTER_WIDTH>");
        pw.println("            <BAND_RASTER_HEIGHT>2241</BAND_RASTER_HEIGHT>");
        pw.println("            <DATA_TYPE>int8</DATA_TYPE>");
        pw.println("            <PHYSICAL_UNIT>unit for Band2</PHYSICAL_UNIT>");
        pw.println("            <SOLAR_FLUX>" + 0.23f + "</SOLAR_FLUX>");
        pw.println("            <SPECTRAL_BAND_INDEX>3</SPECTRAL_BAND_INDEX>");
        pw.println("            <BAND_WAVELEN>" + 243.56f + "</BAND_WAVELEN>");
        pw.println("            <BANDWIDTH>0.0</BANDWIDTH>");
        pw.println("            <SCALING_FACTOR>1.0</SCALING_FACTOR>");
        pw.println("            <SCALING_OFFSET>0.0</SCALING_OFFSET>");
        pw.println("            <LOG10_SCALED>false</LOG10_SCALED>");
        pw.println("            <NO_DATA_VALUE_USED>false</NO_DATA_VALUE_USED>");
        pw.println("            <NO_DATA_VALUE>0.0</NO_DATA_VALUE>");
        pw.println("            <IMAGE_TO_MODEL_TRANSFORM>4.959472210835188,0.7623876714199131,0.6353230595165943,-5.951366653002227,-3.230118329868377,15.14034563458454</IMAGE_TO_MODEL_TRANSFORM>");
        pw.println("        </Spectral_Band_Info>");
        pw.println("        <Spectral_Band_Info>");
        pw.println("            <BAND_INDEX>2</BAND_INDEX>");
        pw.println("            <BAND_DESCRIPTION>Flags1-Description</BAND_DESCRIPTION>");
        pw.println("            <BAND_NAME>Flags1</BAND_NAME>"); // band name
        pw.println("            <BAND_RASTER_WIDTH>1121</BAND_RASTER_WIDTH>");
        pw.println("            <BAND_RASTER_HEIGHT>2241</BAND_RASTER_HEIGHT>");
        pw.println("            <DATA_TYPE>int8</DATA_TYPE>");
        pw.println("            <SOLAR_FLUX>" + 0.0f + "</SOLAR_FLUX>");
        pw.println("            <BAND_WAVELEN>" + 0.0f + "</BAND_WAVELEN>");
        pw.println("            <BANDWIDTH>0.0</BANDWIDTH>");
        pw.println("            <FLAG_CODING_NAME>FlagCoding1</FLAG_CODING_NAME>");
        pw.println("            <SCALING_FACTOR>1.0</SCALING_FACTOR>");
        pw.println("            <SCALING_OFFSET>0.0</SCALING_OFFSET>");
        pw.println("            <LOG10_SCALED>false</LOG10_SCALED>");
        pw.println("            <NO_DATA_VALUE_USED>false</NO_DATA_VALUE_USED>");
        pw.println("            <NO_DATA_VALUE>0.0</NO_DATA_VALUE>");
        pw.println("            <IMAGE_TO_MODEL_TRANSFORM>4.959472210835188,0.7623876714199131,0.6353230595165943,-5.951366653002227,-3.230118329868377,15.14034563458454</IMAGE_TO_MODEL_TRANSFORM>");
        pw.println("        </Spectral_Band_Info>");
        pw.println("        <Spectral_Band_Info>");
        pw.println("            <BAND_INDEX>3</BAND_INDEX>");
        pw.println("            <BAND_DESCRIPTION>Flags2-Description</BAND_DESCRIPTION>");
        pw.println("            <BAND_NAME>Flags2</BAND_NAME>"); // band name
        pw.println("            <BAND_RASTER_WIDTH>1121</BAND_RASTER_WIDTH>");
        pw.println("            <BAND_RASTER_HEIGHT>2241</BAND_RASTER_HEIGHT>");
        pw.println("            <DATA_TYPE>int8</DATA_TYPE>");
        pw.println("            <SOLAR_FLUX>" + 0.0f + "</SOLAR_FLUX>");
        pw.println("            <BAND_WAVELEN>" + 0.0f + "</BAND_WAVELEN>");
        pw.println("            <BANDWIDTH>0.0</BANDWIDTH>");
        pw.println("            <FLAG_CODING_NAME>FlagCoding2</FLAG_CODING_NAME>");
        pw.println("            <SCALING_FACTOR>1.0</SCALING_FACTOR>");
        pw.println("            <SCALING_OFFSET>0.0</SCALING_OFFSET>");
        pw.println("            <LOG10_SCALED>false</LOG10_SCALED>");
        pw.println("            <NO_DATA_VALUE_USED>false</NO_DATA_VALUE_USED>");
        pw.println("            <NO_DATA_VALUE>0.0</NO_DATA_VALUE>");
        pw.println("            <IMAGE_TO_MODEL_TRANSFORM>4.959472210835188,0.7623876714199131,0.6353230595165943,-5.951366653002227,-3.230118329868377,15.14034563458454</IMAGE_TO_MODEL_TRANSFORM>");
        pw.println("        </Spectral_Band_Info>");
        pw.println("        <Spectral_Band_Info>");
        pw.println("            <BAND_INDEX>4</BAND_INDEX>");
        pw.println("            <BAND_DESCRIPTION>Index-Description</BAND_DESCRIPTION>");
        pw.println("            <BAND_NAME>Index</BAND_NAME>"); // band name
        pw.println("            <BAND_RASTER_WIDTH>1121</BAND_RASTER_WIDTH>");
        pw.println("            <BAND_RASTER_HEIGHT>2241</BAND_RASTER_HEIGHT>");
        pw.println("            <DATA_TYPE>uint16</DATA_TYPE>");
        pw.println("            <SOLAR_FLUX>" + 0.0f + "</SOLAR_FLUX>");
        pw.println("            <BAND_WAVELEN>" + 0.0f + "</BAND_WAVELEN>");
        pw.println("            <BANDWIDTH>0.0</BANDWIDTH>");
        pw.println("            <INDEX_CODING_NAME>IndexCoding</INDEX_CODING_NAME>");
        pw.println("            <SCALING_FACTOR>1.0</SCALING_FACTOR>");
        pw.println("            <SCALING_OFFSET>0.0</SCALING_OFFSET>");
        pw.println("            <LOG10_SCALED>false</LOG10_SCALED>");
        pw.println("            <NO_DATA_VALUE_USED>false</NO_DATA_VALUE_USED>");
        pw.println("            <NO_DATA_VALUE>0.0</NO_DATA_VALUE>");
        pw.println("            <IMAGE_TO_MODEL_TRANSFORM>4.959472210835188,0.7623876714199131,0.6353230595165943,-5.951366653002227,-3.230118329868377,15.14034563458454</IMAGE_TO_MODEL_TRANSFORM>");
        pw.println("        </Spectral_Band_Info>");
        pw.println("        <Spectral_Band_Info>");
        pw.println("            <BAND_INDEX>5</BAND_INDEX>");
        pw.println("            <BAND_DESCRIPTION>VirtualBand-Description</BAND_DESCRIPTION>");
        pw.println("            <BAND_NAME>vb1</BAND_NAME>"); // band name
        pw.println("            <BAND_RASTER_WIDTH>1121</BAND_RASTER_WIDTH>");
        pw.println("            <BAND_RASTER_HEIGHT>2241</BAND_RASTER_HEIGHT>");
        pw.println("            <DATA_TYPE>float32</DATA_TYPE>");
        pw.println("            <SOLAR_FLUX>" + 0.0f + "</SOLAR_FLUX>");
        pw.println("            <BAND_WAVELEN>" + 0.0f + "</BAND_WAVELEN>");
        pw.println("            <BANDWIDTH>0.0</BANDWIDTH>");
        pw.println("            <SCALING_FACTOR>1.0</SCALING_FACTOR>");
        pw.println("            <SCALING_OFFSET>0.0</SCALING_OFFSET>");
        pw.println("            <LOG10_SCALED>false</LOG10_SCALED>");
        pw.println("            <NO_DATA_VALUE_USED>true</NO_DATA_VALUE_USED>");
        pw.println("            <NO_DATA_VALUE>3.0</NO_DATA_VALUE>");
        pw.println("            <VIRTUAL_BAND>true</VIRTUAL_BAND>");
        pw.println("            <EXPRESSION>radiance_8</EXPRESSION>");
        pw.println("            <IMAGE_TO_MODEL_TRANSFORM>4.959472210835188,0.7623876714199131,0.6353230595165943,-5.951366653002227,-3.230118329868377,15.14034563458454</IMAGE_TO_MODEL_TRANSFORM>");
        pw.println("        </Spectral_Band_Info>");
        pw.println("    </Image_Interpretation>");
        pw.println("    <Dataset_Sources>");
        pw.println("        <MDElem name=\"metadata\" desc=\"metadata-desc\">");
        pw.println("            <MDATTR name=\"attrib1\" type=\"int16\" mode=\"rw\">123</MDATTR>");
        pw.println("            <MDATTR name=\"attrib2\" type=\"float32\" elems=\"6\">1.0,2.0,3.0,4.0,5.0,6.0</MDATTR>");
        pw.println("            <MDATTR name=\"attrib3\" type=\"float64\" mode=\"rw\" elems=\"6\">7.0,8.0,9.0,10.0,11.0,12.0</MDATTR>");
        if (oldUtcFormat) {
            pw.println("            <MDATTR name=\"attrib5\" type=\"utc\" mode=\"rw\">123,234,345</MDATTR>");
        } else {
            pw.println("            <MDATTR name=\"attrib5\" type=\"utc\" mode=\"rw\">03-MAY-2000 00:03:54.000345</MDATTR>");
        }
        pw.println("            <MDElem name=\"mdElemName1\" desc=\"mdElem1-desc\">");
        pw.println("                <MDATTR name=\"attrib4\" type=\"float64\" elems=\"3\">23.547,-8.0001,-59.989898</MDATTR>");
        pw.println("                <MDATTR name=\"StringAttrib\" type=\"ascii\">StringAttribValue</MDATTR>");
        pw.println("            </MDElem>");
        pw.println("        </MDElem>");
        pw.println("    </Dataset_Sources>");
        pw.print("</Dimap_Document>");

        pw.close();

        return sw.toString();
    }

    /*
     * Creates a DOM represenation (BEAM-DIMAP format) of the given data product.
     *
     * @param product the data product
     *
     * @return a DOM in BEAM-DIMAP format
     */

    private static Document createDOM(Product product, String nameDataDirectory) {
        return new TestDOMBuilder(product, nameDataDirectory).createDOM();
    }

    /**
     * A class whose only purpose is to create a DOM from a given data product.
     */
    private static final class TestDOMBuilder {

        private final Product product;
        private final String nameDataDirectory;
        private Element root;

        TestDOMBuilder(Product product, String nameDataDirectory) {
            this.product = product;
            this.nameDataDirectory = nameDataDirectory;
        }

        final Product getProduct() {
            return product;
        }

        final Document createDOM() {
            root = createRootElement(getProductFilename());
            addMetadataIdElements();
            addDatasetIdElements();
            addProductionElements();
            addGeocodingElements();
            addFlagCodingElements();
            addRasterDimensionsElements();
            addDataAccessElements();
            addTiePointGridElements();
            addImageDisplayElements();
            addImageInterpretationElements();
            addAnnotatonDataSet();
            final Document document = new Document();
            document.setRootElement(root);
            return document;
        }

        private void addAnnotatonDataSet() { //Übernommen
            final MetadataElement metadataRoot = product.getMetadataRoot();
            if (metadataRoot != null) {
                final Element datasetSourcesElem = new Element(DimapProductConstants.TAG_DATASET_SOURCES);
                addMetadataElements(new MetadataElement[]{metadataRoot}, datasetSourcesElem);
                root.addContent(datasetSourcesElem);
            }
        }

        private void addMetadataElements(final MetadataElement[] elementes, final Element mdElem) { //Übernommen
            if (elementes == null) {
                return;
            }
            Debug.assertNotNull(mdElem);
            for (MetadataElement element : elementes) {
                final Element newElem = new Element(DimapProductConstants.TAG_METADATA_ELEMENT);
                newElem.setAttribute(DimapProductConstants.ATTRIB_NAME, element.getName());
                final String description = element.getDescription();
                if (description != null) {
                    newElem.setAttribute(DimapProductConstants.ATTRIB_DESCRIPTION, description);
                }

                addMetadataAttributes(element.getAttributes(), newElem);

                addMetadataElements(element.getElements(), newElem);

                mdElem.addContent(newElem);
            }
        }

        private void addMetadataAttributes(final MetadataAttribute[] attributes, final Element mdElem) { //Übernommen
            if (attributes == null) {
                return;
            }
            Debug.assertNotNull(mdElem);
            for (MetadataAttribute attribute : attributes) {
                final Element mdAttr = new Element(DimapProductConstants.TAG_METADATA_ATTRIBUTE);
                mdAttr.setAttribute(DimapProductConstants.ATTRIB_NAME, attribute.getName());
                final String description = attribute.getDescription();
                if (description != null) {
                    mdAttr.setAttribute(DimapProductConstants.ATTRIB_DESCRIPTION, description);
                }
                final String unit = attribute.getUnit();
                if (unit != null) {
                    mdAttr.setAttribute(DimapProductConstants.ATTRIB_UNIT, unit);
                }
                final String dataTypeString = attribute.getData().getTypeString();
                mdAttr.setAttribute(DimapProductConstants.ATTRIB_TYPE, dataTypeString);
                if (!attribute.isReadOnly()) {
                    mdAttr.setAttribute(DimapProductConstants.ATTRIB_MODE, "rw");
                }
                if (attribute.getNumDataElems() > 1 &&
                    !ProductData.TYPESTRING_ASCII.equals(dataTypeString) &&
                    !ProductData.TYPESTRING_UTC.equals(dataTypeString)) {
                    mdAttr.setAttribute(DimapProductConstants.ATTRIB_ELEMS,
                                        String.valueOf(attribute.getNumDataElems()));
                }
                if (ProductData.TYPESTRING_ASCII.equals(dataTypeString)) {
                    mdAttr.setText(new String((byte[]) attribute.getDataElems()));
                } else {
                    mdAttr.setText(StringUtils.arrayToCsv(attribute.getDataElems()));
                }
                mdElem.addContent(mdAttr);
            }
        }

        private String getProductFilename() {
            String productFilename;
            if (getProduct().getFileLocation() != null) {
                productFilename = getProduct().getFileLocation().getName();
            } else {
                productFilename = getProduct().getName() + DimapProductConstants.DIMAP_HEADER_FILE_EXTENSION;
            }
            return productFilename;
        }

        private Element createRootElement(String filename) {
            Element root = new Element(DimapProductConstants.TAG_ROOT);
            root.setAttribute(DimapProductConstants.ATTRIB_NAME, filename);
            return root;
        }

        private void addDataAccessElements() { //Übernommen
            Element dataAccess = new Element(DimapProductConstants.TAG_DATA_ACCESS);
            JDomHelper.addElement(DimapProductConstants.TAG_DATA_FILE_FORMAT, DimapProductConstants.DATA_FILE_FORMAT,
                                  dataAccess);
            JDomHelper.addElement(DimapProductConstants.TAG_DATA_FILE_FORMAT_DESC,
                                  DimapProductConstants.DATA_FILE_FORMAT_DESCRIPTION, dataAccess);
            JDomHelper.addElement(DimapProductConstants.TAG_DATA_FILE_ORGANISATION,
                                  DimapProductConstants.DATA_FILE_ORGANISATION, dataAccess);


            String[] names = getProduct().getBandNames();
            for (int i = 0; i < names.length; i++) {
                Element dataFile = new Element(DimapProductConstants.TAG_DATA_FILE);
                Element dataFilePath = new Element(DimapProductConstants.TAG_DATA_FILE_PATH);
                dataFilePath.setAttribute(DimapProductConstants.ATTRIB_HREF,
                                          nameDataDirectory + "/" + names[i] + EnviHeader.FILE_EXTENSION);
                dataFile.addContent(dataFilePath);
                JDomHelper.addElement(DimapProductConstants.TAG_BAND_INDEX, i, dataFile);
                dataAccess.addContent(dataFile);
            }


            String[] names1 = getProduct().getTiePointGridNames();
            for (int i = 0; i < names1.length; i++) {
                Element dataFile = new Element(DimapProductConstants.TAG_TIE_POINT_GRID_FILE);
                Element dataFilePath = new Element(DimapProductConstants.TAG_TIE_POINT_GRID_FILE_PATH);
                dataFilePath.setAttribute(DimapProductConstants.ATTRIB_HREF,
                                          nameDataDirectory + "/" + DimapProductConstants.TIE_POINT_GRID_DIR_NAME + "/" + names1[i] + EnviHeader.FILE_EXTENSION);
                dataFile.addContent(dataFilePath);
                JDomHelper.addElement(DimapProductConstants.TAG_TIE_POINT_GRID_INDEX, i, dataFile);
                dataAccess.addContent(dataFile);
            }


            root.addContent(dataAccess);
        }

        private void addTiePointGridElements() { //Übernommen
            int numTiePointGrids = getProduct().getNumTiePointGrids();
            if (numTiePointGrids > 0) {
                Element tiePointGrids = new Element(DimapProductConstants.TAG_TIE_POINT_GRIDS);
                JDomHelper.addElement(DimapProductConstants.TAG_TIE_POINT_NUM_TIE_POINT_GRIDS, numTiePointGrids,
                                      tiePointGrids);
                root.addContent(tiePointGrids);
                String[] gridNames = getProduct().getTiePointGridNames();
                for (int i = 0; i < gridNames.length; i++) {
                    String name = gridNames[i];
                    TiePointGrid tiePointGrid = getProduct().getTiePointGrid(name);
                    Element tiePointGridInfo = new Element(DimapProductConstants.TAG_TIE_POINT_GRID_INFO);
                    JDomHelper.addElement(DimapProductConstants.TAG_TIE_POINT_GRID_INDEX, i, tiePointGridInfo);
                    JDomHelper.addElement(DimapProductConstants.TAG_TIE_POINT_DESCRIPTION,
                                          tiePointGrid.getDescription(), tiePointGridInfo);
                    JDomHelper.addElement(DimapProductConstants.TAG_TIE_POINT_PHYSICAL_UNIT, tiePointGrid.getUnit(),
                                          tiePointGridInfo);
                    JDomHelper.addElement(DimapProductConstants.TAG_TIE_POINT_GRID_NAME, tiePointGrid.getName(),
                                          tiePointGridInfo);
                    JDomHelper.addElement(DimapProductConstants.TAG_TIE_POINT_DATA_TYPE,
                                          ProductData.getTypeString(tiePointGrid.getDataType()), tiePointGridInfo);
                    JDomHelper.addElement(DimapProductConstants.TAG_TIE_POINT_NCOLS, tiePointGrid.getGridWidth(),
                                          tiePointGridInfo);
                    JDomHelper.addElement(DimapProductConstants.TAG_TIE_POINT_NROWS, tiePointGrid.getGridHeight(),
                                          tiePointGridInfo);
                    JDomHelper.addElement(DimapProductConstants.TAG_TIE_POINT_OFFSET_X, tiePointGrid.getOffsetX(),
                                          tiePointGridInfo);
                    JDomHelper.addElement(DimapProductConstants.TAG_TIE_POINT_OFFSET_Y, tiePointGrid.getOffsetY(),
                                          tiePointGridInfo);
                    JDomHelper.addElement(DimapProductConstants.TAG_TIE_POINT_STEP_X, tiePointGrid.getSubSamplingX(),
                                          tiePointGridInfo);
                    JDomHelper.addElement(DimapProductConstants.TAG_TIE_POINT_STEP_Y, tiePointGrid.getSubSamplingY(),
                                          tiePointGridInfo);
                    tiePointGrids.addContent(tiePointGridInfo);
                }
            }
        }

        private void addImageDisplayElements() {
            final Band[] bands = getProduct().getBands();
            Element imageDisplayElem = new Element(DimapProductConstants.TAG_IMAGE_DISPLAY);
            for (int i = 0; i < bands.length; i++) {
                final Band band = bands[i];
                final ImageInfo imageInfo = band.getImageInfo();
                if (imageInfo != null) {
                    Element bandStatisticsElem = new Element(DimapProductConstants.TAG_BAND_STATISTICS);
                    imageDisplayElem.addContent(bandStatisticsElem);
                    JDomHelper.addElement(DimapProductConstants.TAG_BAND_INDEX, i, bandStatisticsElem);
                    if (band.isStxSet()) {
                        JDomHelper.addElement(DimapProductConstants.TAG_STX_MIN, band.getStx().getMinimum(),
                                              bandStatisticsElem);
                        JDomHelper.addElement(DimapProductConstants.TAG_STX_MAX, band.getStx().getMaximum(),
                                              bandStatisticsElem);
                        JDomHelper.addElement(DimapProductConstants.TAG_STX_MEAN, band.getStx().getMean(),
                                              bandStatisticsElem);
                        JDomHelper.addElement(DimapProductConstants.TAG_STX_STDDEV,
                                              band.getStx().getStandardDeviation(),
                                              bandStatisticsElem);
                        JDomHelper.addElement(DimapProductConstants.TAG_STX_LEVEL, band.getStx().getResolutionLevel(),
                                              bandStatisticsElem);

                        final int[] bins = band.getStx().getHistogramBins();
                        if (bins != null && bins.length > 0) {
                            JDomHelper.addElement(DimapProductConstants.TAG_HISTOGRAM, StringUtils.arrayToCsv(bins),
                                                  bandStatisticsElem);
                        }
                    }
                    JDomHelper.addElement(DimapProductConstants.TAG_NUM_COLORS,
                                          imageInfo.getColorPaletteDef().getNumColors(),
                                          bandStatisticsElem);
                    ColorPaletteDef paletteDefinition = imageInfo.getColorPaletteDef();

                    //addColorPalettePoints(paletteDefinition, bandStatisticsElem);
                    Iterator iterator = paletteDefinition.getIterator();
                    while (iterator.hasNext()) {
                        ColorPaletteDef.Point point = (ColorPaletteDef.Point) iterator.next();
                        Element colorPalettePointElem = new Element(DimapProductConstants.TAG_COLOR_PALETTE_POINT);
                        bandStatisticsElem.addContent(colorPalettePointElem);
                        JDomHelper.addElement(DimapProductConstants.TAG_SAMPLE, point.getSample(),
                                              colorPalettePointElem);
                        colorPalettePointElem.addContent(createColorElement(point.getColor()));
                    }
                }
            }

            final List children = imageDisplayElem.getChildren();
            if (children != null && children.size() > 0) {
//            if (imageDisplayElem.hasChildren()) {
                root.addContent(imageDisplayElem);
            }
        }

        private Element createColorElement(Color color) {  //Übernommen
            Element colorElem = new Element(DimapProductConstants.TAG_COLOR);
            colorElem.setAttribute(DimapProductConstants.ATTRIB_RED, String.valueOf(color.getRed()));
            colorElem.setAttribute(DimapProductConstants.ATTRIB_GREEN, String.valueOf(color.getGreen()));
            colorElem.setAttribute(DimapProductConstants.ATTRIB_BLUE, String.valueOf(color.getBlue()));
            colorElem.setAttribute(DimapProductConstants.ATTRIB_ALPHA, String.valueOf(color.getAlpha()));
            return colorElem;
        }

        private void addFlagCodingElements() { // Übernommen
            String[] codingNames = getProduct().getFlagCodingGroup().getNodeNames();
            for (String codingName : codingNames) {
                Element flagCodingElem = new Element(DimapProductConstants.TAG_FLAG_CODING);
                flagCodingElem.setAttribute(DimapProductConstants.ATTRIB_NAME, codingName);
                root.addContent(flagCodingElem);
                FlagCoding flagCoding = getProduct().getFlagCodingGroup().get(codingName);
                String[] flagNames = flagCoding.getFlagNames();
                for (String flagName : flagNames) {
                    MetadataAttribute flag = flagCoding.getFlag(flagName);
                    Element flagElem = new Element(DimapProductConstants.TAG_FLAG);
                    JDomHelper.addElement(DimapProductConstants.TAG_FLAG_NAME, flag.getName(), flagElem);
                    JDomHelper.addElement(DimapProductConstants.TAG_FLAG_INDEX, flag.getData().getElemInt(), flagElem);
                    JDomHelper.addElement(DimapProductConstants.TAG_FLAG_DESCRIPTION, flag.getDescription(), flagElem);
                    flagCodingElem.addContent(flagElem);
                }
            }
        }

        private void addImageInterpretationElements() { //Übernommen
            Element imageInterpreElem = new Element(DimapProductConstants.TAG_IMAGE_INTERPRETATION);
            Band[] bands = getProduct().getBands();
            for (int i = 0; i < bands.length; i++) {
                Band band = bands[i];
                Element sbiElem = new Element(DimapProductConstants.TAG_SPECTRAL_BAND_INFO);
                JDomHelper.addElement(DimapProductConstants.TAG_BAND_INDEX, i, sbiElem);
                JDomHelper.addElement(DimapProductConstants.TAG_BAND_DESCRIPTION, band.getDescription(), sbiElem);
                JDomHelper.addElement(DimapProductConstants.TAG_BAND_NAME, band.getName(), sbiElem);
                JDomHelper.addElement(DimapProductConstants.TAG_DATA_TYPE,
                                      ProductData.getTypeString(band.getDataType()), sbiElem);
                final String unit = band.getUnit();
                if (unit != null && unit.length() > 0) {
                    JDomHelper.addElement(DimapProductConstants.TAG_PHYSICAL_UNIT, unit, sbiElem);
                }
                JDomHelper.addElement(DimapProductConstants.TAG_SOLAR_FLUX, band.getSolarFlux(), sbiElem);
                if (band.getSpectralBandIndex() > -1) {
                    JDomHelper.addElement(DimapProductConstants.TAG_SPECTRAL_BAND_INDEX, band.getSpectralBandIndex(),
                                          sbiElem);
                }
                JDomHelper.addElement(DimapProductConstants.TAG_BAND_WAVELEN, band.getSpectralWavelength(), sbiElem);
                FlagCoding flagCoding = band.getFlagCoding();
                if (flagCoding != null) {
                    JDomHelper.addElement(DimapProductConstants.TAG_FLAG_CODING_NAME, flagCoding.getName(), sbiElem);
                }
                IndexCoding indexCoding = band.getIndexCoding();
                if (indexCoding != null) {
                    JDomHelper.addElement(DimapProductConstants.TAG_INDEX_CODING_NAME, indexCoding.getName(), sbiElem);
                }
                imageInterpreElem.addContent(sbiElem);
            }
            root.addContent(imageInterpreElem);
        }

        private void addMetadataIdElements() { //Übernommen
            Element metadataID = new Element(DimapProductConstants.TAG_METADATA_ID);
            addMetadataFormatElement(metadataID);
            JDomHelper.addElement(DimapProductConstants.TAG_METADATA_PROFILE,
                                  DimapProductConstants.DIMAP_METADATA_PROFILE, metadataID);
            root.addContent(metadataID);
        }

        private void addMetadataFormatElement(Element parent) {
            Element element = JDomHelper.createElement(DimapProductConstants.TAG_METADATA_FORMAT, "DIMAP");
            element.setAttribute(DimapProductConstants.ATTRIB_VERSION, DimapProductConstants.DIMAP_CURRENT_VERSION);
            parent.addContent(element);
        }

        private void addRasterDimensionsElements() { //Übernommen
            Element rasterDimension = new Element(DimapProductConstants.TAG_RASTER_DIMENSIONS);
            JDomHelper.addElement(DimapProductConstants.TAG_NCOLS, getProduct().getSceneRasterWidth(), rasterDimension);
            JDomHelper.addElement(DimapProductConstants.TAG_NROWS, getProduct().getSceneRasterHeight(),
                                  rasterDimension);
            JDomHelper.addElement(DimapProductConstants.TAG_NBANDS, getProduct().getNumBands(), rasterDimension);
            root.addContent(rasterDimension);
        }

        private void addProductionElements() { //Übernommen
            Element production = new Element(DimapProductConstants.TAG_PRODUCTION);
            JDomHelper.addElement(DimapProductConstants.TAG_DATASET_PRODUCER_NAME,
                                  DimapProductConstants.DATASET_PRODUCER_NAME, production);
            JDomHelper.addElement(DimapProductConstants.TAG_PRODUCT_TYPE, getProduct().getProductType(), production);
            root.addContent(production);
        }

        private void addGeocodingElements() { // Übernommen
            final GeoCoding geoCoding = getProduct().getSceneGeoCoding();
            if (geoCoding != null) {
                Element crsElem = new Element(DimapProductConstants.TAG_COORDINATE_REFERENCE_SYSTEM);
                if (geoCoding instanceof TiePointGeoCoding) {
                    TiePointGeoCoding tiePointGeoCoding = (TiePointGeoCoding) geoCoding;
                    String latGridName = tiePointGeoCoding.getLatGrid().getName();
                    String lonGridName = tiePointGeoCoding.getLonGrid().getName();
                    if (latGridName == null || lonGridName == null) {
                        return;
                    }
                    final Element gctpgElem = new Element(DimapProductConstants.TAG_GEOCODING_TIE_POINT_GRIDS);
                    JDomHelper.addElement(DimapProductConstants.TAG_TIE_POINT_GRID_NAME_LAT, latGridName, gctpgElem);
                    JDomHelper.addElement(DimapProductConstants.TAG_TIE_POINT_GRID_NAME_LON, lonGridName, gctpgElem);
                    crsElem.addContent(gctpgElem);
                    root.addContent(crsElem);
                } else if (geoCoding instanceof MapGeoCoding) {
                    MapGeoCoding mapGeoCoding = (MapGeoCoding) geoCoding;
                    MapInfo info = mapGeoCoding.getMapInfo();
                    if (info == null) {
                        return;
                    }
                    String infoString = info.toString();
                    if (infoString == null || infoString.length() == 0) {
                        return;
                    }
                    final Element mgcElem = new Element(DimapProductConstants.TAG_GEOCODING_MAP);
                    JDomHelper.addElement(DimapProductConstants.TAG_GEOCODING_MAP_INFO, infoString, mgcElem);
                    crsElem.addContent(mgcElem);
                    root.addContent(crsElem);
                }
            }
        }

        private void addDatasetIdElements() {  //Übernommen
            Element datasetID = new Element(DimapProductConstants.TAG_DATASET_ID);
            JDomHelper.addElement(DimapProductConstants.TAG_DATASET_SERIES, DimapProductConstants.DIMAP_DATASET_SERIES,
                                  datasetID);
            JDomHelper.addElement(DimapProductConstants.TAG_DATASET_NAME, getProduct().getName(), datasetID);
            final String description = getProduct().getDescription();
            if (description != null && description.length() > 0) {
                JDomHelper.addElement(DimapProductConstants.TAG_DATASET_DESCRIPTION, description, datasetID);
            }
            root.addContent(datasetID);
        }
    }
}
