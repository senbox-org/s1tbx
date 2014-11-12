package org.esa.beam.binning.operator.metadata;

import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.binning.aggregators.AggregatorAverage;
import org.esa.beam.binning.aggregators.AggregatorOnMaxSet;
import org.esa.beam.binning.operator.BinningConfig;
import org.esa.beam.binning.operator.BinningOp;
import org.esa.beam.binning.operator.VariableConfig;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.util.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.SortedMap;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GlobalMetadataTest {

    private static final String TEST_DIR = "test_dir";
    private static final String TEST_PROPERTIES = "param_a = aaaaa\n" +
            "param_b = bbbb\n" +
            "param_c = CCCC";

    @Test
    public void testCreate_fromBinningOp() throws ParseException {
        final BinningOp binningOp = createBinningOp();
        final GlobalMetadata metadata = GlobalMetadata.create(binningOp);
        assertNotNull(metadata);

        final SortedMap<String, String> metaProperties = metadata.asSortedMap();
        assertNotNull(metaProperties);
        assertEquals("org.esa.beam.binning.operator.BinningOp", metaProperties.get("software_qualified_name"));
        assertEquals("Binning", metaProperties.get("software_name"));
        assertEquals("1.0", metaProperties.get("software_version"));

        assertEquals(FileUtils.getFilenameWithoutExtension("output.file"), metaProperties.get("product_name"));
        //assertNotNull(metaProperties.get("processing_time"));
        assertEquals("2013-05-01", metaProperties.get("aggregation_period_start"));
        assertEquals("15.56 day(s)", metaProperties.get("aggregation_period_duration"));
        assertEquals("POLYGON ((10 10, 15 10, 15 12, 10 12, 10 10))", metaProperties.get("region"));
        assertEquals("8192", metaProperties.get("num_rows"));
        assertEquals("2.446286592055973", metaProperties.get("pixel_size_in_km"));
        assertEquals("3", metaProperties.get("super_sampling"));
        assertEquals("a_mask_expression", metaProperties.get("mask_expression"));
    }

    @Test
    public void testCreate_fromConfig() {
        final BinningConfig config = new BinningConfig();
        config.setNumRows(12);
        config.setSuperSampling(13);
        config.setMaskExpr("14");
        config.setVariableConfigs(new VariableConfig("var_name", "var_expr"));
        config.setAggregatorConfigs(new AggregatorAverage.Config("variable", "target", 2.076, false, true));
        // @todo 3 tb/** add check for PostProcessorConfig 2014-10-17
        config.setMinDataHour(15.0);
        config.setTimeFilterMethod(BinningOp.TimeFilterMethod.SPATIOTEMPORAL_DATA_DAY);
        config.setMetadataAggregatorName("NAME");
        config.setStartDateTime("here_we_go");
        config.setPeriodDuration(15.88);

        final GlobalMetadata metadata = GlobalMetadata.create(config);
        assertNotNull(metadata);

        final SortedMap<String, String> metaProperties = metadata.asSortedMap();
        assertNotNull(metaProperties);

        assertEquals("12", metaProperties.get("num_rows"));
        assertEquals("13", metaProperties.get("super_sampling"));
        assertEquals("14", metaProperties.get("mask_expression"));
        assertEquals("var_name", metaProperties.get("variable_config.0:name"));
        assertEquals("var_expr", metaProperties.get("variable_config.0:expr"));
        assertEquals("AVG", metaProperties.get("aggregator_config.0:type"));
        assertEquals("false", metaProperties.get("aggregator_config.0:outputCounts"));
        assertEquals("true", metaProperties.get("aggregator_config.0:outputSums"));
        assertEquals("target", metaProperties.get("aggregator_config.0:targetName"));
        assertEquals("variable", metaProperties.get("aggregator_config.0:varName"));
        assertEquals("2.076", metaProperties.get("aggregator_config.0:weightCoeff"));
        assertEquals("15.0", metaProperties.get("min_data_hour"));
        assertEquals("SPATIOTEMPORAL_DATA_DAY", metaProperties.get("time_filter_method"));
        assertEquals("NAME", metaProperties.get("metadata_aggregator_name"));
        assertEquals("here_we_go", metaProperties.get("aggregation_period_start"));
        assertEquals("15.88 day(s)", metaProperties.get("aggregation_period_duration"));
    }

    @Test
    public void testCreateFromConfig_noParametersSet() throws ParseException {
        final BinningConfig binningConfig = new BinningConfig();
        final GlobalMetadata metadata = GlobalMetadata.create(binningConfig);
        assertNotNull(metadata);

        final SortedMap<String, String> metaProperties = metadata.asSortedMap();
        assertNotNull(metaProperties);

        assertNull(metaProperties.get("product_name"));
        //assertNotNull(metaProperties.get("processing_time"));
        assertNull(metaProperties.get("aggregation_period_start"));
        assertNull(metaProperties.get("aggregation_period_duration"));
        assertNull(metaProperties.get("region"));
        assertNull(metaProperties.get("num_rows"));
        assertNull(metaProperties.get("pixel_size_in_km"));
        assertNull(metaProperties.get("super_sampling"));
        assertEquals("", metaProperties.get("mask_expression"));
        assertNull(metaProperties.get("variable_config.0:name"));
    }

    @Test
    public void testCreate_timeFilerMethod_timeRange() throws ParseException {
        final BinningConfig binningConfig = new BinningConfig();
        binningConfig.setTimeFilterMethod(BinningOp.TimeFilterMethod.TIME_RANGE);

        final GlobalMetadata metadata = GlobalMetadata.create(binningConfig);
        assertNotNull(metadata);

        final SortedMap<String, String> metaProperties = metadata.asSortedMap();
        assertNotNull(metaProperties);

        assertEquals("TIME_RANGE", metaProperties.get("time_filter_method"));
        assertNull(metaProperties.get("min_data_hour"));
    }

    @Test
    public void testCreate_timeFilterMethod_spatioTemporalDay() throws ParseException {
        final BinningConfig binningConfig = new BinningConfig();
        binningConfig.setTimeFilterMethod(BinningOp.TimeFilterMethod.SPATIOTEMPORAL_DATA_DAY);
        binningConfig.setMinDataHour(0.8876);

        final GlobalMetadata metadata = GlobalMetadata.create(binningConfig);
        assertNotNull(metadata);

        final SortedMap<String, String> metaProperties = metadata.asSortedMap();
        assertNotNull(metaProperties);

        assertEquals("SPATIOTEMPORAL_DATA_DAY", metaProperties.get("time_filter_method"));
        assertEquals("0.8876", metaProperties.get("min_data_hour"));
    }

    @Test
    public void testCreate_variableConfigs() {
        final VariableConfig[] variableConfigs = new VariableConfig[2];
        variableConfigs[0] = new VariableConfig("first", "one and one");
        variableConfigs[1] = new VariableConfig("second", "is two");

        final BinningOp binningOp = new BinningOp();
        binningOp.setVariableConfigs(variableConfigs);

        final GlobalMetadata metadata = GlobalMetadata.create(binningOp);
        assertNotNull(metadata);

        final SortedMap<String, String> metaProperties = metadata.asSortedMap();
        assertNotNull(metaProperties);

        assertEquals("first", metaProperties.get("variable_config.0:name"));
        assertEquals("one and one", metaProperties.get("variable_config.0:expr"));
        assertEquals("second", metaProperties.get("variable_config.1:name"));
        assertEquals("is two", metaProperties.get("variable_config.1:expr"));
    }

    @Test
    public void testCreate_aggregatorConfigs() {
        final AggregatorConfig[] aggregatorConfigs = new AggregatorConfig[2];
        aggregatorConfigs[0] = new AggregatorAverage.Config("variable_1", "the target", 1.087, true, false);
        aggregatorConfigs[1] = new AggregatorOnMaxSet.Config("variable_2", "another one", "set_1", "set_2");

        final BinningOp binningOp = new BinningOp();
        binningOp.setAggregatorConfigs(aggregatorConfigs);

        final GlobalMetadata metadata = GlobalMetadata.create(binningOp);
        assertNotNull(metadata);

        final SortedMap<String, String> metaProperties = metadata.asSortedMap();
        assertNotNull(metaProperties);

        assertEquals("AVG", metaProperties.get("aggregator_config.0:type"));
        assertEquals("true", metaProperties.get("aggregator_config.0:outputCounts"));
        assertEquals("false", metaProperties.get("aggregator_config.0:outputSums"));
        assertEquals("the target", metaProperties.get("aggregator_config.0:targetName"));
        assertEquals("variable_1", metaProperties.get("aggregator_config.0:varName"));
        assertEquals("1.087", metaProperties.get("aggregator_config.0:weightCoeff"));

        assertEquals("ON_MAX_SET", metaProperties.get("aggregator_config.1:type"));
        assertEquals("another one", metaProperties.get("aggregator_config.1:onMaxVarName"));
        assertEquals("set_1,set_2", metaProperties.get("aggregator_config.1:setVarNames"));
        assertEquals("variable_2", metaProperties.get("aggregator_config.1:targetName"));
    }

    @Test
    public void testLoad_fileIsNull() throws IOException {
        final Logger logger = mock(Logger.class);
        final GlobalMetadata globalMetadata = new GlobalMetadata();

        globalMetadata.load(null, logger);

        final SortedMap<String, String> metaProperties = globalMetadata.asSortedMap();
        assertNotNull(metaProperties);
        assertEquals(0, metaProperties.size());

        verifyNoMoreInteractions(logger);
    }

    @Test
    public void testLoad_fileDoesNotExist() throws IOException {
        final Logger logger = mock(Logger.class);
        final GlobalMetadata globalMetadata = new GlobalMetadata();

        globalMetadata.load(new File("over_the_rain.bow"), logger);

        final SortedMap<String, String> metaProperties = globalMetadata.asSortedMap();
        assertNotNull(metaProperties);
        assertEquals(0, metaProperties.size());

        verify(logger, times(1)).warning("Metadata properties file 'over_the_rain.bow' not found");
        verifyNoMoreInteractions(logger);
    }

    @Test
    public void testLoad() throws IOException {
        final Logger logger = mock(Logger.class);

        final GlobalMetadata globalMetadata = new GlobalMetadata();
        try {
            final File propertiesFile = writePropertiesFile();
            globalMetadata.load(propertiesFile, logger);

            final SortedMap<String, String> metaProperties = globalMetadata.asSortedMap();
            assertEquals("aaaaa", metaProperties.get("param_a"));
            assertEquals("bbbb", metaProperties.get("param_b"));
            assertEquals("CCCC", metaProperties.get("param_c"));

            verify(logger, times(1)).info(contains("Reading metadata properties file"));
            verifyNoMoreInteractions(logger);
        } finally {
            deletePropertiesFile();
        }
    }

    @Test
    public void testAsMetadataElement() throws ParseException {
        final BinningOp binningOp = createBinningOp();

        final GlobalMetadata globalMetadata = GlobalMetadata.create(binningOp);

        final MetadataElement processingGraphElement = globalMetadata.asMetadataElement();
        assertNotNull(processingGraphElement);
        assertEquals("Processing_Graph", processingGraphElement.getName());

        final MetadataElement node_0_Element = processingGraphElement.getElement("node.0");
        assertNotNull(node_0_Element);

        final MetadataElement parameterElement = node_0_Element.getElement("parameters");
        assertEquals(11, parameterElement.getNumAttributes());

        // @todo 2 tb/tb check for other meta elements 2014-10-10
        final MetadataAttribute software_qualified_name = parameterElement.getAttribute("software_qualified_name");
        assertNotNull(software_qualified_name);
        assertEquals("org.esa.beam.binning.operator.BinningOp", software_qualified_name.getData().getElemString());
    }

    @Test
    public void testAsMetadataElement_noMetadataContained() {
        final GlobalMetadata globalMetadata = new GlobalMetadata();

        final MetadataElement metadataElement = globalMetadata.asMetadataElement();
        assertNotNull(metadataElement);
        assertEquals("Processing_Graph", metadataElement.getName());
        assertEquals(0, metadataElement.getNumAttributes());
    }

    @Test
    public void testIsTimeFilterMetadataRequired() {
        assertFalse(GlobalMetadata.isTimeFilterMetadataRequired(null));
        assertFalse(GlobalMetadata.isTimeFilterMetadataRequired(BinningOp.TimeFilterMethod.NONE));

        assertTrue(GlobalMetadata.isTimeFilterMetadataRequired(BinningOp.TimeFilterMethod.SPATIOTEMPORAL_DATA_DAY));
        assertTrue(GlobalMetadata.isTimeFilterMetadataRequired(BinningOp.TimeFilterMethod.TIME_RANGE));
    }

    @Test
    public void testToPixelSizeString() {
        assertEquals("1821.593952320952", GlobalMetadata.toPixelSizeString(12));
        assertEquals("2.446286592055973", GlobalMetadata.toPixelSizeString(8192));
    }

    private void deletePropertiesFile() {
        final File testDir = new File(TEST_DIR);
        if (testDir.isDirectory()) {
            if (!FileUtils.deleteTree(testDir)) {
                fail("unable to delete test directory");
            }
        }
    }

    private File writePropertiesFile() throws IOException {
        final File testDir = new File(TEST_DIR);
        if (!testDir.mkdirs()) {
            fail("unable to create test directory");
        }

        final File testPropertiesFile = new File(testDir, "test.properties");
        if (!testPropertiesFile.createNewFile()) {
            fail("unable to create test file");
        }

        final FileOutputStream fileOutputStream = new FileOutputStream(testPropertiesFile);
        fileOutputStream.write(TEST_PROPERTIES.getBytes());
        fileOutputStream.close();

        return testPropertiesFile;
    }

    private BinningOp createBinningOp() throws ParseException {
        final BinningOp binningOp = new BinningOp();
        binningOp.setOutputFile("output.file");
        binningOp.setStartDateTime("2013-05-01");
        binningOp.setPeriodDuration(15.56);

        final WKTReader wktReader = new WKTReader();
        binningOp.setRegion(wktReader.read("POLYGON((10 10, 15 10, 15 12, 10 12, 10 10))"));
        binningOp.setTimeFilterMethod(BinningOp.TimeFilterMethod.NONE);
        binningOp.setNumRows(8192);
        binningOp.setSuperSampling(3);
        binningOp.setMaskExpr("a_mask_expression");
        return binningOp;
    }
}
