package com.bc.ceres.standalone;

import com.bc.ceres.metadata.DefaultSimpleFileSystem;
import com.bc.ceres.metadata.MetadataResourceEngine;
import com.bc.ceres.metadata.XPathHandler;
import org.apache.velocity.VelocityContext;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * A default executable class for the metadata engine. It implements common usage of the MatadataEngine API.
 * <p>
 * <b>usage: java -classpath path com.bc.ceres.standalone.MetadataEngineMain -t /path/targetItem.suff -v templateX=/path/metadata.vm.txt [-v templateY=/path/report.vm.xml] [optional options] [arg1] [arg2] ...</b>
 * <table summary="">
 * <tr>
 * <td>-m &lt;filePath&gt; </td>
 * <td>Optional. The absolute path and name of a text file to be included. E.g. global metadata. Refer to as $metadata in velocity templates.</td>
 * </tr>
 * <tr>
 * <td>-S &lt;source&gt;=&lt;filePath&gt; </td>
 * <td>Optional. The absolute path and name of the source items. Could be several given by key-value-pairs. In the velocity templates the key will give you the content of the associated metadata file. The reference $sourcePaths holds a list of the input item paths.</td>
 * </tr>
 * <tr>
 * <td>-t &lt;filePath&gt; </td>
 * <td>The absolute item path (e.g. a product), the metadata file will be places next to the item with the name 'itemName-templateName.templateSuffix. Refer to as $targetPath in velocity templates.</td>
 * </tr>
 * <tr>
 * <td>-v &lt;template&gt;=&lt;filePath&gt; </td>
 * <td>The absolute path of the velocity templates (*.vm). Could be several given by key-value-pairs.</td>
 * </tr>
 * </table>
 *
 * @author Bettina
 * @since Ceres 0.13.2
 */
public class MetadataEngineMain {
    public static final String KEY_METADATA = "metadata";
    public static final String KEY_SOURCES = "sourcePaths";
    public static final String KEY_XPATH = "xpath";
    public static final String KEY_TARGET = "targetPath";
    public static final String KEY_SYSTEM = "system";
    public static final String KEY_ARGS = "commandLineArgs";
    public static final String KEY_DATE_FORMAT = "dateFormat";
    public static final String KEY_DATE = "date";
    private MetadataResourceEngine metadataResourceEngine;
    private CliHandler cliHandler;

    public MetadataEngineMain(MetadataResourceEngine metadataResourceEngine) {
        this.metadataResourceEngine = metadataResourceEngine;
    }

    public static void main(String[] commandLineArgs) {
        MetadataEngineMain metadataEngineMain = null;
        try {
            metadataEngineMain = new MetadataEngineMain(new MetadataResourceEngine(new DefaultSimpleFileSystem()));
            metadataEngineMain.setCliHandler(new CliHandler(commandLineArgs));
            if (commandLineArgs.length < 2) {
                metadataEngineMain.cliHandler.printUsage();
                System.err.print("Error in MetadataEngineMain: The two options -v and -t are mandatory. ");
                System.exit(1);
            } else {
                metadataEngineMain.processMetadata();
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Error in MetadataEngineMain:" + e.getMessage());
            metadataEngineMain.cliHandler.printUsage();
            System.exit(2);
        } catch (Exception e) {
            System.err.println(e.getClass() + "Error in MetadataEngineMain:" + e.getMessage());
            System.exit(3);
        }
    }

    void processMetadata() throws Exception {
        VelocityContext velocityContext = metadataResourceEngine.getVelocityContext();
        velocityContext.put(KEY_DATE_FORMAT, new SimpleDateFormat("yyyy-MM-dd"));
        velocityContext.put(KEY_DATE, new Date());

        HashMap<String, String> metadataPaths = cliHandler.fetchGlobalMetadataFiles();
        for (String key : metadataPaths.keySet()) {
            metadataResourceEngine.readResource(key, metadataPaths.get(key));
        }

        Map<String, String> sourcePaths = cliHandler.fetchSourceItemFiles();
        for (String key : sourcePaths.keySet()) {
            metadataResourceEngine.readRelatedResource(key, sourcePaths.get(key));
        }
        velocityContext.put(KEY_XPATH, new XPathHandler());
        velocityContext.put(KEY_SOURCES, sourcePaths);

        velocityContext.put(KEY_SYSTEM, System.getProperties());
        velocityContext.put(KEY_ARGS, Arrays.asList(cliHandler.fetchArguments()));

        Map<String, String> templatePaths = cliHandler.fetchTemplateFiles();
        String outputItemPath = cliHandler.fetchTargetItemFile();
        velocityContext.put(KEY_TARGET, outputItemPath);
        for (String templateKey : templatePaths.keySet()) {
            metadataResourceEngine.writeRelatedResource(templatePaths.get(templateKey), outputItemPath);
        }
    }

    void setCliHandler(CliHandler cliHandler) { //only for tests
        this.cliHandler = cliHandler;
    }
}
