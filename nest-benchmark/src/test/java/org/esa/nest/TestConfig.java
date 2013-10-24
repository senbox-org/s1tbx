package org.esa.nest;

import org.esa.beam.util.PropertyMap;
import org.esa.nest.util.Config;
import org.esa.nest.util.ResourceUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 Configuration for automated test
 */
public class TestConfig {

    final Properties prop = Config.getAutomatedTestConfigPropertyMap().getProperties();
    private final static String contextID = ResourceUtils.getContextID();

    private final List<TestInfo> testList = new ArrayList<TestInfo>(20);
    private int maxProductsPerInputFolder = -1;

    public TestConfig() throws Exception {
        importTests();
    }

    public List<TestInfo> getTestList() {
        return testList;
    }

    public int getMaxProductsPerInputFolder() {
        return maxProductsPerInputFolder;
    }

    private void importTests() throws Exception {
        final String prefix = contextID+".test.";

        String maxIn = readProp("maxProductsPerInputFolder");
        if(maxIn != null) {
            maxProductsPerInputFolder = Integer.parseInt(maxIn);
        }

        final int numProperties = prop.size()/4;
        for(int i=0; i < numProperties; ++i) {
            final String key = prefix+i;
            final String graph = readProp(key + ".graph");
            if(graph != null) {
                final String skip = readProp(key + ".skip");
                if(skip != null && skip.equalsIgnoreCase("true")) {
                    continue;
                }

                final String input_products = readProp(key + ".input_products");
                final String expected_results = readProp(key + ".expected_results") + "\\test"+i;
                final String output_products = readProp(key + ".output_products") + "\\test"+i;

                if(input_products == null || output_products == null) {
                    throw new Exception("Test configuration "+key+" is incomplete");
                }

                final TestInfo test = new TestInfo(i, graph, input_products, expected_results, output_products);
                if(!test.graphFile.exists())
                    throw new Exception(test.graphFile.getAbsolutePath() +" does not exist for "+key);
                if(!test.inputFolder.exists())
                    throw new Exception(test.inputFolder.getAbsolutePath() +" does not exist for "+key);

                testList.add(test);
            }
        }
    }

    private String readProp(final String tag) {
        String val = prop.getProperty(tag);
        if(val != null && val.contains("${")) {
            val = resolve(val);
        }

        return val;
    }

    private String resolve(String value)
    {
        final int idx1 = value.indexOf("${");
        final int idx2 = value.indexOf('}') + 1;
        final String keyWord = value.substring(idx1+2, idx2-1);
        final String fullKey = value.substring(idx1, idx2);

        final String property = prop.getProperty(keyWord);
        if (property != null && property.length() > 0) {
            value = value.replace(fullKey, property);
        }

        if(value.contains("${"))
            value = resolve(value);

        return value;
    }
}
