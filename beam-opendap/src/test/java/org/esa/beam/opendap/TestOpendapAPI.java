package org.esa.beam.opendap;

import opendap.dap.Attribute;
import opendap.dap.AttributeTable;
import opendap.dap.BaseType;
import opendap.dap.DAS;
import opendap.dap.DArray;
import opendap.dap.DArrayDimension;
import opendap.dap.DConnect2;
import opendap.dap.DDS;
import opendap.dap.DGrid;
import opendap.dap.DataDDS;
import org.esa.beam.util.Debug;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import thredds.catalog.InvAccess;
import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvCatalogRef;
import thredds.catalog.InvDataset;
import thredds.catalog.InvDocumentation;
import thredds.catalog.InvMetadata;
import thredds.catalog.InvProperty;
import thredds.catalog.InvService;
import thredds.catalog2.Access;
import thredds.catalog2.Catalog;
import thredds.catalog2.CatalogRef;
import thredds.catalog2.Dataset;
import thredds.catalog2.DatasetNode;
import thredds.catalog2.Metadata;
import thredds.catalog2.Property;
import thredds.catalog2.Service;
import thredds.catalog2.ThreddsMetadata;
import thredds.catalog2.xml.parser.ThreddsXmlParser;
import thredds.catalog2.xml.parser.stax.StaxThreddsXmlParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import static org.junit.Assert.*;

/**
 * This class contains solely tests which document the behavior and the usage of the OPeNDAP Java API.
 * Because the tests rely on hardcoded URLs and do not test any classes of the org.esa.beam.opendap package, they are
 * all ignored.
 *
 * @author Thomas Storm
 * @author Tonio Fincke
 */
public class TestOpendapAPI {

    private DConnect2 dConnect;

    @Before
    public void setUp() throws Exception {
        String url = "http://test.opendap.org/dap/data/nc/sst.mnmean.nc.gz";
        dConnect = new DConnect2(url);
    }

    @Test
    @Ignore
    public void testGetCatalog() throws Exception {
//        final String url = "http://stellwagen.er.usgs.gov/opendap/SED_TRANS/catalog.xml";
//        final String url = "http://www.usgodae.org/dods/GDS/fnmoc_ghrsst/catalog.xml";  // Not a Catalog
//        final String url = "http://www.usgodae.org/dods/GDS/coamps_cent_am/catalog.xml";  // Not a Catalog
//        final String url = "http://acdisc.sci.gsfc.nasa.gov/opendap/EarthProbe_TOMS_Level3/TOMSEPL3.008/1996/catalog.xml";
//        final String url = "http://acdisc.sci.gsfc.nasa.gov/opendap/EarthProbe_TOMS_Level3/catalog.xml";
//        final String url = "http://acdisc.sci.gsfc.nasa.gov/opendap/catalog.xml";
//        final String url = "http://10.3.13.120:8084/thredds/catalog/testAll/catalog.xml";
        final String url = "http://10.3.13.120:8084/thredds/catalog/catalog.xml";
//        final String url = "http://10.3.13.120:8084/thredds/catalog.xml";
//        final String url = "http://opendap.hzg.de/opendap/data/catalog.xml";
//        final String url = "http://opendap.hzg.de/opendap/data/cosyna/MERIS/2012/catalog.xml";
//        final String url = "http://opendap.hzg.de/opendap/data/cosyna/gridded/meris/catalog.xml";
//        final String url = "http://test.opendap.org/dap/data/nc/catalog.xml";

        final InvCatalogFactory defaultFactory = InvCatalogFactory.getDefaultFactory(true);
        final InvCatalogImpl invCatalog = defaultFactory.readXML(url);

        final ThreddsXmlParser xmlParser = StaxThreddsXmlParser.newInstance();
        final Catalog catalog = xmlParser.parse(new URL(url).toURI());

        System.out.println("invCatalog.getBaseUri() = " + invCatalog.getBaseURI());
//        System.out.println("catalog.getDocBaseUri() = " + catalog.getDocBaseUri());

        System.out.println("invCatalog.getVersion() = " + invCatalog.getVersion());
//        System.out.println("catalog.getVersion()    = " + catalog.getVersion());

        final List<InvDataset> datasets = invCatalog.getDatasets();

        final InvDataset invDataset = datasets.get(0);
//        final DatasetNode dataset = catalog.getDatasets().get(0);

        System.out.println("invDataset.getID() = " + invDataset.getID());
//        System.out.println("dataset.getId()    = " + dataset.getId());


        System.out.println("-----------------------------------------------------------------------");
        System.out.println("-----------------------------------------------------------------------");
        printInvDatasets(datasets);
        System.out.println("-----------------------------------------------------------------------");
        System.out.println("-----------------------------------------------------------------------");
//        printDatasets(dataset.getDatasets());
    }

    private void printInvDatasets(final List<InvDataset> invDatasets) {
        for (InvDataset datasetNode : invDatasets) {
            System.out.println("-----------------------------------------------------------------------");
            System.out.println("datasetNode.getID()           = " + datasetNode.getID());
            if (datasetNode instanceof InvCatalogRef) {
                final InvCatalogRef catalogRef = (InvCatalogRef) datasetNode;
//                System.out.println("    catalogRef.getReference() = " + catalogRef.getUrlPath());
                System.out.println("    catalogRef.getReference() = " + catalogRef.getXlinkHref());
            }

            if (datasetNode instanceof InvDataset) {
                final InvDataset ds = datasetNode;
                System.out.println("    ds.hasAccess() = " + ds.hasAccess());
                List<InvAccess> dsAccesses = ds.getAccess();
                for (InvAccess dsAccess : dsAccesses) {
                    InvService dsAccessService = dsAccess.getService();
                    System.out.println("    dsAccessService.getName() = " + dsAccessService.getName());
                    List<InvService> services = dsAccessService.getServices();
                    for (InvService service : services) {
                        System.out.println("        service.getName() = " + service.getName());
                    }
                }
            }
            System.out.println("datasetNode.getIdAuthority() = " + datasetNode.getAuthority());
            List<InvMetadata> metadatas = datasetNode.getMetadata();
            for (InvMetadata metadata : metadatas) {
                System.out.println("    metadata.getContent()           = " + metadata.getContentObject());
//                System.out.println("    metadata.getTitle()             = " + metadata.title);
                System.out.println("    metadata.getExternalReference() = " + metadata.getXlinkURI());
                System.out.println("    metadata.isContainedContent()   = " + metadata.getContentObject() != null);
                if (metadata.isThreddsMetadata()) {
                    thredds.catalog.ThreddsMetadata threddsMD = metadata.getThreddsMetadata();
                    if (threddsMD != null) {
                        System.out.println("    threddsMD.getAuthority() = " + threddsMD.getAuthority());
                        System.out.println("    threddsMD.getHistory() = " + threddsMD.getHistory());
                        System.out.println("    threddsMD.getProcessing() = " + threddsMD.getProcessing());
                        System.out.println("    threddsMD.getRights() = " + threddsMD.getRights());
                        System.out.println("    threddsMD.getServiceName() = " + threddsMD.getServiceName());
                        System.out.println("    threddsMD.getSummary() = " + threddsMD.getSummary());
                        System.out.println("    threddsMD.getDataFormatType() = " + threddsMD.getDataFormatType());
                        System.out.println("    threddsMD.getDataSize() = " + threddsMD.getDataSize());
                        System.out.println("    threddsMD.getDataType() = " + threddsMD.getDataType());
                        System.out.println("    threddsMD.getGeospatialCoverage() = " + threddsMD.getGeospatialCoverage());
                        System.out.println("    threddsMD.getTimeCoverage() = " + threddsMD.getTimeCoverage());

                        final List<thredds.catalog.ThreddsMetadata.Contributor> contributors = threddsMD.getContributors();
                        System.out.println("    threddsMD.getContributors() = " + contributors);
                        for (thredds.catalog.ThreddsMetadata.Contributor contributor : contributors) {
                            System.out.println("        contributor.getName() = " + contributor.getName());
                            System.out.println("        contributor.getRole() = " + contributor.getRole());
                        }

                        final List<thredds.catalog.ThreddsMetadata.Source> creators = threddsMD.getCreators();
                        System.out.println("    threddsMD.getCreators() = " + creators);
                        for (thredds.catalog.ThreddsMetadata.Source creator : creators) {
                            System.out.println("        creator.getName() = " + creator.getName());
                            System.out.println("        creator.getEmail() = " + creator.getEmail());
                            System.out.println("        creator.getUrl() = " + creator.getUrl());
                            System.out.println("        creator.getVocabulary() = " + creator.getVocabulary());
                        }

                        final List<thredds.catalog.ThreddsMetadata.Source> publishers = threddsMD.getPublishers();
                        System.out.println("    threddsMD.getPublishers() = " + publishers);
                        for (thredds.catalog.ThreddsMetadata.Source publisher : publishers) {
                            System.out.println("        publisher.getName() = " + publisher.getName());
                            System.out.println("        publisher.getEmail() = " + publisher.getEmail());
                            System.out.println("        publisher.getUrl() = " + publisher.getUrl());
                            System.out.println("        publisher.getVocabulary() = " + publisher.getVocabulary());
                        }

                        final List<InvDocumentation> documentation = threddsMD.getDocumentation();
                        System.out.println("    threddsMD.getDocumentation() = " + documentation);
                        for (InvDocumentation invDocumentation : documentation) {
                            System.out.println("        invDocumentation.getInlineContent() = " + invDocumentation.getInlineContent());
                            System.out.println("        invDocumentation.getType() = " + invDocumentation.getType());
                            try {
                                System.out.println("        invDocumentation.getXlinkContent() = " + invDocumentation.getXlinkContent());
                            } catch (IOException e) {
                                Debug.trace(e);
                            }
                            System.out.println("        invDocumentation.getXlinkHref() = " + invDocumentation.getXlinkHref());
                            System.out.println("        invDocumentation.getXlinkTitle() = " + invDocumentation.getXlinkTitle());
                            System.out.println("        invDocumentation.getURI() = " + invDocumentation.getURI());
                        }

                        final List<InvProperty> properties = threddsMD.getProperties();
                        System.out.println("    threddsMD.getProperties() = " + properties);
                        for (InvProperty property : properties) {
                            System.out.println("        property.getName() = " + property.getName());
                            System.out.println("        property.getValue() = " + property.getValue());
                        }

                        final List<thredds.catalog.ThreddsMetadata.Variables> variables = threddsMD.getVariables();
                        System.out.println("    threddsMD.getVariables() = " + variables);
                        for (thredds.catalog.ThreddsMetadata.Variables variable : variables) {
                            System.out.println("        variable.getMapHref() = " + variable.getMapHref());
                            System.out.println("        variable.getVocabHref() = " + variable.getVocabHref());
                            System.out.println("        variable.getVocabulary() = " + variable.getVocabulary());
                            System.out.println("        variable.getMapUri() = " + variable.getMapUri());
                        }
                    }
                }
            }
            List<InvProperty> properties = datasetNode.getProperties();
            for (InvProperty property : properties) {
                System.out.println("    property.getName()   = " + property.getName());
                System.out.println("    property.getValue()  = " + property.getValue());
            }
            printInvDatasets(datasetNode.getDatasets());
        }
    }

    private void printDatasets(final List<DatasetNode> datasets) {
        for (DatasetNode datasetNode : datasets) {
            System.out.println("-----------------------------------------------------------------------");
            System.out.println("datasetNode.getId()           = " + datasetNode.getId());
            if (datasetNode instanceof CatalogRef) {
                final CatalogRef catalogRef = (CatalogRef) datasetNode;
                System.out.println("    catalogRef.getReference() = " + catalogRef.getReference());
            }

            if (datasetNode instanceof Dataset) {
                final Dataset ds = (Dataset) datasetNode;
                System.out.println("    ds.isAccessible() = " + ds.isAccessible());
                List<Access> dsAccesses = ds.getAccesses();
                for (Access dsAccess : dsAccesses) {
                    Service dsAccessService = dsAccess.getService();
                    System.out.println("    dsAccessService.getName() = " + dsAccessService.getName());
                    List<Service> services = dsAccessService.getServices();
                    for (Service service : services) {
                        System.out.println("        service.getName() = " + service.getName());
                    }
                }
            }
//            System.out.println("datasetNode.getIdAuthority() = " + datasetNode.getIdAuthority());
            List<Metadata> metadatas = datasetNode.getMetadata();
            for (Metadata metadata : metadatas) {
                System.out.println("    metadata.getContent()           = " + metadata.getContent());
                System.out.println("    metadata.getTitle()             = " + metadata.getTitle());
                System.out.println("    metadata.getExternalReference() = " + metadata.getExternalReference());
                System.out.println("    metadata.isContainedContent()   = " + metadata.isContainedContent());
            }
            List<Property> properties = datasetNode.getProperties();
            for (Property property : properties) {
                System.out.println("    property.getName()   = " + property.getName());
                System.out.println("    property.getValue()  = " + property.getValue());
            }
            ThreddsMetadata threddsMetadata = datasetNode.getThreddsMetadata();
            if (threddsMetadata != null) {
                System.out.println("    threddsMetadata.getCollectionType() = " + threddsMetadata.getCollectionType());
            }
        }
    }

    @Test
    @Ignore
    public void testGetDDS() throws Exception {
        final DDS dds = dConnect.getDDS();
        final Enumeration variables = dds.getVariables();
        Set<String> variableNames = new HashSet<String>();
        while (variables.hasMoreElements()) {
            final Object currentVariable = variables.nextElement();
            assertTrue(currentVariable instanceof DArray || currentVariable instanceof DGrid);
            variableNames.add(((BaseType) currentVariable).getName());
            if (currentVariable instanceof DArray) {
                final DArray variable = (DArray) currentVariable;
                if (variable.getName().equals("lat")) {
                    assertEquals(1, variable.numDimensions());
                    assertEquals(89, variable.getDimension(0).getSize());
                } else if (variable.getName().equals("lon")) {
                    assertEquals(1, variable.numDimensions());
                    assertEquals(180, variable.getDimension(0).getSize());
                } else if (variable.getName().equals("time")) {
                    assertEquals(1, variable.numDimensions());
                    assertEquals(1857, variable.getDimension(0).getSize());
                } else if (variable.getName().equals("time_bnds")) {
                    assertEquals(2, variable.numDimensions());
                    assertEquals(1857, variable.getDimension(0).getSize());
                    assertEquals(2, variable.getDimension(1).getSize());
                }
            } else if (currentVariable instanceof DGrid) {
                final DGrid variable = (DGrid) currentVariable;
                final DArray gridArray = variable.getArray();
                assertEquals(3, gridArray.numDimensions());
                assertEquals(1857, gridArray.getDimension(0).getSize());
                assertEquals(89, gridArray.getDimension(1).getSize());
                assertEquals(180, gridArray.getDimension(2).getSize());
                final Vector<DArrayDimension> gridMaps = variable.getArrayDims();
                testMap(gridMaps.get(0), "time", 1857);
                testMap(gridMaps.get(1), "lat", 89);
                testMap(gridMaps.get(2), "lon", 180);

            }
        }
        assertTrue(variableNames.contains("lat"));
        assertTrue(variableNames.contains("lon"));
        assertTrue(variableNames.contains("time"));
        assertTrue(variableNames.contains("time_bnds"));
        assertTrue(variableNames.contains("sst"));
    }

    @Test
    @Ignore
    public void testGetDAS() throws Exception {
        final DAS das = dConnect.getDAS();
        final Enumeration attributeNames = das.getNames();
        assertTrue(attributeNames.hasMoreElements());
        final Set<String> attributeNameSet = new HashSet<String>();
        while (attributeNames.hasMoreElements()) {
            attributeNameSet.add(attributeNames.nextElement().toString());
        }
        assertTrue(attributeNameSet.contains("lat"));
        assertTrue(attributeNameSet.contains("lon"));
        assertTrue(attributeNameSet.contains("time"));
        assertTrue(attributeNameSet.contains("time_bnds"));
        assertTrue(attributeNameSet.contains("sst"));
        assertTrue(attributeNameSet.contains("NC_GLOBAL"));

        final AttributeTable attributeTableLat = das.getAttribute("lat").getContainer();
        final Enumeration attributeTableLatNames = attributeTableLat.getNames();
        testLatLonAttributes(attributeTableLat, attributeTableLatNames, "Latitude", 88.0f, -88.0f, "latitude_north", "y");

        final AttributeTable attributeTableLon = das.getAttribute("lon").getContainer();
        final Enumeration attributeTableLonNames = attributeTableLon.getNames();
        testLatLonAttributes(attributeTableLon, attributeTableLonNames, "Longitude", 0.0f, 358.0f, "longitude_east", "x");

        final AttributeTable globalAttributes = das.getAttributeTable("NC_GLOBAL");
        assertNotNull(globalAttributes);

        final HashSet<String> globalAttributesNamesSet = new HashSet<String>();
        final Enumeration globalAttributesNames = globalAttributes.getNames();
        while (globalAttributesNames.hasMoreElements()) {
            globalAttributesNamesSet.add(globalAttributesNames.nextElement().toString());
        }
        assertTrue(globalAttributesNamesSet.contains("title"));
        assertTrue(globalAttributesNamesSet.contains("conventions"));
        assertTrue(globalAttributesNamesSet.contains("history"));
        assertTrue(globalAttributesNamesSet.contains("comments"));
        assertTrue(globalAttributesNamesSet.contains("platform"));
        assertTrue(globalAttributesNamesSet.contains("source"));
        assertTrue(globalAttributesNamesSet.contains("institution"));
        assertTrue(globalAttributesNamesSet.contains("references"));
        assertTrue(globalAttributesNamesSet.contains("citation"));

        assertEquals("NOAA Extended Reconstructed SST V3", globalAttributes.getAttribute("title").getValueAt(0));
    }

    private void testMap(DArrayDimension map, String expectedName, int expectedSize) {
        assertEquals(expectedName, map.getName());
        assertEquals(expectedSize, map.getSize());
    }

    @Test
    @Ignore
    public void testDownloadData() throws Exception {
        InputStream inputStream = null;
        OutputStream os = null;
        File file = null;
        try {
            URL url = new URL("http://test.opendap.org/dap/data/nc/data.nc");
            final URLConnection connection = url.openConnection();
            inputStream = connection.getInputStream();
            file = new File("data.nc");
            os = new FileOutputStream(file);
            final byte[] buffer = new byte[50 * 1024];
            while (inputStream.read(buffer) != -1) {
                os.write(buffer);
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (os != null) {
                os.close();
            }
            if (file != null) {
                file.delete();
            }
        }
    }

    @Test
    @Ignore
    public void testGetDDX() throws Exception {
        final DConnect2 dConnect2 = new DConnect2("http://test.opendap.org/opendap/data/nc/sst.mnmean.nc.gz");
        final DataDDS data = dConnect2.getData("geogrid(sst,-90,-150,-89,-140)");
//        final DataDDS data = dConnect2.getData("geogrid(sst,-89,-179,-88,-178");
        System.out.println("TestOpendapAPI.testGetDDX");
    }

    private void testLatLonAttributes(AttributeTable attributeTable, Enumeration attributeTableNames, String expectedLongName, float expectedMin, float expectedMax, String expectedStandardName, String expectedAxis) throws Exception {
        final Set<Attribute> attributeSet = new HashSet<Attribute>();
        final Set<String> attributeNamesSet = new HashSet<String>();
        while (attributeTableNames.hasMoreElements()) {
            final String attributeName = attributeTableNames.nextElement().toString();
            final Attribute attribute = attributeTable.getAttribute(attributeName);
            attributeSet.add(attribute);
            attributeNamesSet.add(attributeName);
        }
        for (Attribute attribute : attributeSet) {
            if (attribute.getName().equals("units")) {
                assertTrue(attribute.getValueAt(0).matches("degrees_.*"));
            } else if (attribute.getName().equals("long_name")) {
                assertEquals(expectedLongName, attribute.getValueAt(0));
            } else if (attribute.getName().equals("actual_range")) {
                assertEquals(Attribute.FLOAT32, attribute.getType());
                final Iterator valuesIterator = attribute.getValuesIterator();
                assertEquals(expectedMin, Float.parseFloat(valuesIterator.next().toString()), 1.0E-7);
                assertEquals(expectedMax, Float.parseFloat(valuesIterator.next().toString()), 1.0E-7);
            } else if (attribute.getName().equals("standard_name")) {
                assertEquals(expectedStandardName, attribute.getValueAt(0));
            } else if (attribute.getName().equals("axis")) {
                assertEquals(expectedAxis, attribute.getValueAt(0));
            } else if (attribute.getName().equals("coordinate_defines")) {
                assertEquals("center", attribute.getValueAt(0));
            }
        }

        assertTrue(attributeNamesSet.contains("units"));
        assertTrue(attributeNamesSet.contains("long_name"));
        assertTrue(attributeNamesSet.contains("actual_range"));
        assertTrue(attributeNamesSet.contains("standard_name"));
        assertTrue(attributeNamesSet.contains("axis"));
        assertTrue(attributeNamesSet.contains("coordinate_defines"));
    }
}
