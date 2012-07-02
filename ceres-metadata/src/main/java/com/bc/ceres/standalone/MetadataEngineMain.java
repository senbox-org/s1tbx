package com.bc.ceres.standalone;

import com.bc.ceres.metadata.DefaultSimpleFileSystem;
import com.bc.ceres.metadata.MetadataEngine;
import org.apache.velocity.VelocityContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MetadataEngineMain {
    public static final String KEY_METADATA = "metadata";
    public static final String KEY_TEMPLATE = "templateX";
    public static final String KEY_SOURCE = "sourceX";
    public static final String KEY_OUTPUT = "output";
    private MetadataEngine metadataEngine;

    public MetadataEngineMain(MetadataEngine metadataEngine) {
        this.metadataEngine = metadataEngine;
    }

    public static void main(String[] commandLineArgs) {
        try {
            MetadataEngineMain metadataEngineMain = new MetadataEngineMain(new MetadataEngine(new DefaultSimpleFileSystem()));
            if (System.getProperty("template1") != null && System.getProperty("output") != null) {
                metadataEngineMain.processMetadata(commandLineArgs);
            } else {
                metadataEngineMain.printUsage();
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    void processMetadata(String[] commandLineArgs) throws Exception {
        VelocityContext velocityContext = metadataEngine.getVelocityContext();
        String metadataPath = System.getProperty(KEY_METADATA);
        if (metadataPath != null) {
            metadataEngine.readMetadata(KEY_METADATA, metadataPath, false);
        }

        Map<String, String> sourcePaths = fetchProperty(KEY_SOURCE);
        for (String key : sourcePaths.keySet()) {
            metadataEngine.readSourceMetadata(key, sourcePaths.get(key));
        }
        velocityContext.put("sourcePaths", sourcePaths.values());

        velocityContext.put("system", System.getProperties());
        velocityContext.put("commandLineArgs", Arrays.asList(commandLineArgs));

        Map<String, String> templatePaths = fetchProperty(KEY_TEMPLATE);
        String outputItemPath = System.getProperty(KEY_OUTPUT);
        for (String templateKey : templatePaths.keySet()) {
            metadataEngine.writeTargetMetadata(templatePaths.get(templateKey), outputItemPath);
        }
    }

    private void printUsage() {
        System.out.println("Usage: metadata-engine ");
        System.out.println("       [-Dmetadata=/input/metadata.properties[/.xml]]");
        System.out.println("       [-Dsource1=input/MER_L1_1.N1 [-Dsource2=input/MER_L1_2.N1 ...] ]");
        System.out.println("       -Dtemplate1=templates/report.xml.vm [-Dtemplate2=templates/report.txt.vm ...]");
        System.out.println("       -Doutput=output/MER_L2_1.dim");
        System.out.println();
        System.out.println("Objects to be used in the velocity template:");
        System.out.println("*  under '$metadata': (Static) metadata resource. Get the content with '$metadata.getContent'");
        System.out.println("  -> corresponding to -Dmetadata=/input/metadata.properties[/.xml]]");
        System.out.println("*  under '$sourcePaths': A list of source item's paths.");
        System.out.println("*  under '$sourceX.get(\"templateBasename.suffix\")': Source item's metadata resource.");
        System.out.println("  -> corresponding to -Dsource1=input/MER_L1_1.N1 [-Dsource2=input/MER_L1_2.N1 ...] and");
        System.out.println("  -> corresponding to -Dtemplate1=templates/report.xml.vm [-Dtemplate2=templates/report.txt.vm ...]");
        System.out.println("  -> (e.g templateBasename.suffix -> report.txt)");
        System.out.println("*  under '$system: Class System.");
        System.out.println("*  under '$commandLineArgs: The command line arguments. (With: $commandLineArgs.get(0))");
        System.out.println("");
    }

    HashMap<String, String> fetchProperty(String keyBaseName) {
        HashMap<String, String> map = new HashMap<String, String>();

        for (int i = 1; ; i++) {
            String key = keyBaseName.replace("X", String.valueOf(i));
            String property = System.getProperty(key);
            if (property == null) {
                break;
            }
            map.put(key, property);
        }
        return map;
    }
}
