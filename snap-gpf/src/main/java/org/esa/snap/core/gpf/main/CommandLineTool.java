/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.gpf.main;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.dom.DefaultDomConverter;
import com.bc.ceres.binding.dom.DefaultDomElement;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.XppDomElement;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.metadata.MetadataResourceEngine;
import com.bc.ceres.resource.Resource;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import com.thoughtworks.xstream.io.xml.XppDomWriter;
import com.thoughtworks.xstream.io.xml.XppReader;
import com.thoughtworks.xstream.io.xml.xppdom.XppDom;
import org.apache.velocity.VelocityContext;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.ParameterDescriptorFactory;
import org.esa.snap.core.gpf.common.ReadOp;
import org.esa.snap.core.gpf.common.WriteOp;
import org.esa.snap.core.gpf.descriptor.OperatorDescriptor;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphContext;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.core.gpf.graph.GraphProcessingObserver;
import org.esa.snap.core.gpf.graph.Node;
import org.esa.snap.core.gpf.graph.NodeContext;
import org.esa.snap.core.gpf.graph.NodeSource;
import org.esa.snap.core.gpf.internal.OperatorExecutor;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.xmlpull.mxp1.MXParser;

import javax.media.jai.JAI;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The common command-line tool for the GPF.
 * For usage, see {@link org/esa/snap/core/gpf/main/CommandLineUsage.txt}.
 */
class CommandLineTool implements GraphProcessingObserver {

    static final String TOOL_NAME = "gpt";
    static final String DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat(DATETIME_PATTERN, Locale.ENGLISH);
    static final String READ_OP_ID_PREFIX = "ReadOp@";
    public static final String WRITE_OP_ID_PREFIX = "WriteOp@";

    private final CommandLineContext commandLineContext;
    //    private final VelocityContext velocityContext;
    private final MetadataResourceEngine metadataResourceEngine;
    private CommandLineArgs commandLineArgs;

    /**
     * Constructs a new tool.
     */
    CommandLineTool() {
        this(new DefaultCommandLineContext());
    }

    /**
     * Constructs a new tool with the given context.
     *
     * @param commandLineContext The context used to run the tool.
     */
    CommandLineTool(CommandLineContext commandLineContext) {
        this.commandLineContext = commandLineContext;
        this.metadataResourceEngine = new MetadataResourceEngine(commandLineContext);
    }

    void run(String... args) throws Exception {
        boolean stackTraceDumpEnabled = CommandLineArgs.isStackTraceDumpEnabled(args);
        try {
            commandLineArgs = CommandLineArgs.parseArgs(args);
            if (commandLineArgs.isHelpRequested()) {
                printHelp();
                return;
            }
            if (commandLineArgs.isDiagnosticRequested()) {
                printDiagnostics();
                return;
            }
            run();
        } catch (Error | RuntimeException e) {
            e.printStackTrace(System.err);
            throw e;
        } catch (Exception e) {
            if (stackTraceDumpEnabled) {
                e.printStackTrace(System.err);
            }
            throw e;
        }
    }

    private void printHelp() {
        if (commandLineArgs.getOperatorName() != null) {
            commandLineContext.print(CommandLineUsage.getUsageTextForOperator(commandLineArgs.getOperatorName()));
        } else if (commandLineArgs.getGraphFilePath() != null) {
            commandLineContext.print(CommandLineUsage.getUsageTextForGraph(commandLineArgs.getGraphFilePath(),
                                                                           commandLineContext));
        } else {
            commandLineContext.print(CommandLineUsage.getUsageText());
        }
    }

    private void printDiagnostics() {
        initializeSystemProperties();
        initializeJAI();
        final Runtime runtime = Runtime.getRuntime();

        commandLineContext.print("SNAP Release version " + SystemUtils.getReleaseVersion() + '\n');

        commandLineContext.print("SNAP home: " + System.getProperty("snap.home") + '\n');
        commandLineContext.print("SNAP debug: " + System.getProperty("snap.debug") + '\n');
        commandLineContext.print("SNAP log level: " + System.getProperty("snap.log.level") + '\n');

        commandLineContext.print("Java home: " + System.getProperty("java.home") + '\n');
        commandLineContext.print("Java version: " + System.getProperty("java.version") + '\n');

        commandLineContext.print("Processors: " + runtime.availableProcessors() + '\n');
        commandLineContext.print("Max memory: " + fromBytes(runtime.maxMemory()) + '\n');

        commandLineContext.print("Cache size: " + fromBytes(JAI.getDefaultInstance().getTileCache().getMemoryCapacity()) + '\n');
        commandLineContext.print("Tile parallelism: " + JAI.getDefaultInstance().getTileScheduler().getParallelism() + '\n');
        commandLineContext.print("Tile size: " + (int)JAI.getDefaultTileSize().getWidth() + " x " +
                (int)JAI.getDefaultTileSize().getHeight() + " pixels" + '\n');

        commandLineContext.print("\nTo configure your gpt memory usage:\n");
        commandLineContext.print("Edit snap/bin/gpt.vmoptions\n");
        commandLineContext.print("\nTo configure your gpt cache size and parallelism:\n");
        commandLineContext.print("Edit .snap/etc/snap.properties or gpt -c ${cachesize-in-GB}G -q ${parallelism} \n");
    }

    static String fromBytes(long bytes) {
        if(bytes > CommandLineArgs.G) {
            return String.format("%.1f GB",(double)bytes / CommandLineArgs.G);
        } else if(bytes > CommandLineArgs.M) {
            return String.format("%.1f MB",(double)bytes / CommandLineArgs.M);
        } else if(bytes > CommandLineArgs.K) {
            return String.format("%.1f KB",(double)bytes / CommandLineArgs.K);
        }
        return String.format("%d B", bytes);
    }

    private void run() throws Exception {
        initializeSystemProperties();
        initializeJAI();
        initVelocityContext();
        readMetadata();
        runGraphOrOperator();
        runVelocityTemplates();
    }

    private void initializeSystemProperties() {
        Map<String, String> systemPropertiesMap = commandLineArgs.getSystemPropertiesMap();
        for (Entry<String, String> properties : systemPropertiesMap.entrySet()) {
            System.setProperty(properties.getKey(), properties.getValue());
        }
    }

    private void initializeJAI() {
        long tileCacheCapacity = commandLineArgs.getTileCacheCapacity();
        int tileSchedulerParallelism = commandLineArgs.getTileSchedulerParallelism();
        if (tileCacheCapacity > 0) {
            JAI.enableDefaultTileCache();
            JAI.getDefaultInstance().getTileCache().setMemoryCapacity(tileCacheCapacity);
        } else {
            JAI.getDefaultInstance().getTileCache().setMemoryCapacity(0L);
            JAI.disableDefaultTileCache();
        }
        if (tileSchedulerParallelism > 0) {
            JAI.getDefaultInstance().getTileScheduler().setParallelism(tileSchedulerParallelism);
        }
        final long tileCacheSize = JAI.getDefaultInstance().getTileCache().getMemoryCapacity() / (1024L * 1024L);
        commandLineContext.getLogger().fine(MessageFormat.format("JAI tile cache size is {0} MB", tileCacheSize));
        final int schedulerParallelism = JAI.getDefaultInstance().getTileScheduler().getParallelism();
        commandLineContext.getLogger().fine(MessageFormat.format("JAI tile scheduler parallelism is {0}", schedulerParallelism));
    }

    private void initVelocityContext() throws Exception {
        VelocityContext velocityContext = metadataResourceEngine.getVelocityContext();
        velocityContext.put("system", System.getProperties());
        velocityContext.put("softwareName", "SNAP gpt");
        String versionKey = String.format("%s.version", SystemUtils.getApplicationContextId());
        velocityContext.put("softwareVersion", System.getProperty(versionKey, ""));
        velocityContext.put("commandLineArgs", commandLineArgs);

        // Derived properties (shortcuts).
        // Check if we really want them, if so, we have to maintain them in the future (nf)
        File targetFile = new File(commandLineArgs.getTargetFilePath());
        File parentFile = targetFile.getParentFile();
        velocityContext.put("targetFile", targetFile);
        velocityContext.put("targetDir", parentFile != null ? parentFile : new File("."));
        velocityContext.put("targetBaseName", FileUtils.getFilenameWithoutExtension(targetFile));
        velocityContext.put("targetName", targetFile.getName());
        velocityContext.put("targetFormat", commandLineArgs.getTargetFormatName());

        // Check if we also put the following into the context?
        // Actually no, because this puts the ontext in an unknown state, because we don't know which are the key's names (nf)
        //velocityContext.putAll(commandLineArgs.getParameterMap());
        //velocityContext.putAll(commandLineArgs.getTargetFilePathMap());
        //velocityContext.putAll(commandLineArgs.getSourceFilePathMap());
    }

    private void readMetadata() throws Exception {
        if (commandLineArgs.getMetadataFilePath() != null) {
            readMetadata(commandLineArgs.getMetadataFilePath(), true);
        } else {
            readMetadata(CommandLineArgs.DEFAULT_METADATA_FILEPATH, false);
        }
        readSourceMetadataFiles();
    }

    private void readMetadata(String path, boolean fail) throws Exception {
        try {
            metadataResourceEngine.readResource("metadata", path);
        } catch (Exception e) {
            if (fail) {
                throw e;
            }
            final String message = String.format("Failed to read metadata file '%s': %s", path, e.getMessage());
            if (commandLineContext.fileExists(path)) {
                logSevereProblem(message, e);
            }
        }
    }

    void readSourceMetadataFiles() {
        final SortedMap<String, String> sourceFilePathMap = commandLineArgs.getSourceFilePathMap();
        for (String sourceId : sourceFilePathMap.keySet()) {
            final String sourcePath = sourceFilePathMap.get(sourceId);
            try {
                metadataResourceEngine.readRelatedResource(sourceId, sourcePath);
            } catch (Exception e) {
                String msgPattern = "Failed to load metadata file associated with '%s = %s': %s";
                logSevereProblem(String.format(msgPattern, sourceId, sourcePath, e.getMessage()), e);
            }
        }
    }

    private void runGraphOrOperator() throws Exception {
        VelocityContext velocityContext = metadataResourceEngine.getVelocityContext();
        velocityContext.put("processingStartTime", DATETIME_FORMAT.format(new Date()));
        if (commandLineArgs.getOperatorName() != null) {
            // Operator name given: parameters and sources are parsed from command-line args
            runOperator();
        } else if (commandLineArgs.getGraphFilePath() != null) {
            // Path to Graph XML given: parameters and sources are parsed from command-line args
            runGraph();
        }
        velocityContext.put("processingStopTime", DATETIME_FORMAT.format(new Date()));
    }

    private void runOperator() throws Exception {
        Map<String, String> parameterMap = getRawParameterMap();
        String operatorName = commandLineArgs.getOperatorName();
        Map<String, Product> sourceProducts = getSourceProductMap();
        Map<String, Object> parameters = convertParameterMap(operatorName, parameterMap, sourceProducts);

        OperatorSpiRegistry operatorSpiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        OperatorSpi operatorSpi = operatorSpiRegistry.getOperatorSpi(operatorName);
        if (operatorSpi == null) {
            throw new OperatorException(String.format("Unknown operator name '%s'.", operatorName));
        }
        Operator operator = operatorSpi.createOperator(parameters, sourceProducts);

        // Force call to Operator.initialize()
        Product targetProduct = operator.getTargetProduct();

        OperatorDescriptor operatorDescriptor = operatorSpi.getOperatorDescriptor();
        if (operatorDescriptor.isAutoWriteDisabled()) {
            // operator has its own output management, we "execute" by pulling at tiles
            final OperatorExecutor executor = OperatorExecutor.create(operator);
            executor.execute(ProgressMonitor.NULL);
        } else {
            // framework writes target product
            String filePath = commandLineArgs.getTargetFilePath();
            String formatName = commandLineArgs.getTargetFormatName();
            writeProduct(targetProduct, filePath, formatName, commandLineArgs.isClearCacheAfterRowWrite());
        }

        // Fill velocity context with operator metadata
        VelocityContext velocityContext = metadataResourceEngine.getVelocityContext();
        velocityContext.put("operator", operator);
        velocityContext.put("operatorSpi", operatorSpi);
        velocityContext.put("operatorMetadata", operatorDescriptor.getOperatorClass().getAnnotation(OperatorMetadata.class));
        velocityContext.put("operatorDescriptor", operatorDescriptor);
        velocityContext.put("operatorName", operatorName);
        velocityContext.put("parameters", parameters); // Check if we should use parameterMap here (nf)
        velocityContext.put("sourceProduct", sourceProducts.get("sourceProduct"));
        velocityContext.put("sourceProducts", sourceProducts); // Check if we should use an array here (nf)
        velocityContext.put("targetProduct", targetProduct);
        velocityContext.put("targetProducts", new Product[]{targetProduct});
    }

    private void runGraph() throws Exception {
        final OperatorSpiRegistry operatorSpiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();

        Map<String, String> templateVariables = getRawParameterMap();

        Map<String, String> sourceNodeIdMap = getSourceNodeIdMap();
        templateVariables.putAll(sourceNodeIdMap);
        // todo - use Velocity and the current Velocity context for reading the graph XML! (nf, 20120610)
        Graph graph = readGraph(commandLineArgs.getGraphFilePath(), templateVariables);
        Node lastNode = graph.getNode(graph.getNodeCount() - 1);
        SortedMap<String, String> sourceFilePathsMap = commandLineArgs.getSourceFilePathMap();

        // For each source path add a ReadOp to the graph
        String readOperatorAlias = OperatorSpi.getOperatorAlias(ReadOp.class);
        for (Entry<String, String> entry : sourceFilePathsMap.entrySet()) {
            String sourceId = entry.getKey();
            String sourceFilePath = entry.getValue();
            String sourceNodeId = sourceNodeIdMap.get(sourceId);
            if (graph.getNode(sourceNodeId) == null) {

                DomElement configuration = new DefaultDomElement("parameters");
                configuration.createChild("file").setValue(sourceFilePath);

                Node sourceNode = new Node(sourceNodeId, readOperatorAlias);
                sourceNode.setConfiguration(configuration);

                graph.addNode(sourceNode);
            }
        }

        final String operatorName = lastNode.getOperatorName();
        final OperatorSpi operatorSpi = operatorSpiRegistry.getOperatorSpi(operatorName);
        if (operatorSpi == null) {
            throw new GraphException(String.format("Unknown operator name '%s'.", operatorName));
        }
        OperatorDescriptor operatorDescriptor = operatorSpi.getOperatorDescriptor();

        if (!operatorDescriptor.isAutoWriteDisabled()) {
            // Auto-writing is permitted, so add a WriteOp as last node
            String writeOperatorAlias = OperatorSpi.getOperatorAlias(WriteOp.class);

            DomElement configuration = new DefaultDomElement("parameters");
            configuration.createChild("file").setValue(commandLineArgs.getTargetFilePath());
            configuration.createChild("formatName").setValue(commandLineArgs.getTargetFormatName());
            configuration.createChild("clearCacheAfterRowWrite").setValue(
                    Boolean.toString(commandLineArgs.isClearCacheAfterRowWrite()));

            Node targetNode = new Node(WRITE_OP_ID_PREFIX + lastNode.getId(), writeOperatorAlias);
            targetNode.addSource(new NodeSource("source", lastNode.getId()));
            targetNode.setConfiguration(configuration);

            graph.addNode(targetNode);
        }

        executeGraph(graph);

        VelocityContext velocityContext = metadataResourceEngine.getVelocityContext();
        File graphFile = new File(commandLineArgs.getGraphFilePath());
        velocityContext.put("graph", graph);

        metadataResourceEngine.readResource("graphXml", graphFile.getPath());
    }

    private Map<String, Object> convertParameterMap(String operatorName, Map<String, String> parameterMap,
                                                    Map<String, Product> sourceProductMap) throws ValidationException {
        HashMap<String, Object> parameters = new HashMap<>();
        PropertyContainer container = ParameterDescriptorFactory.createMapBackedOperatorPropertyContainer(operatorName,
                                                                                                          parameters,
                                                                                                          sourceProductMap);
        // explicitly set default values for putting them into the backing map
        container.setDefaultValues();

        // handle xml parameters
        Object parametersObject = metadataResourceEngine.getVelocityContext().get("parameterFile");
        if (parametersObject instanceof Resource) {
            Resource parametersResource = (Resource) parametersObject;
            if (parametersResource.isXml()) {
                OperatorSpiRegistry operatorSpiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
                OperatorSpi operatorSpi = operatorSpiRegistry.getOperatorSpi(operatorName);
                Class<? extends Operator> operatorClass = operatorSpi.getOperatorDescriptor().getOperatorClass();
                DefaultDomConverter domConverter = new DefaultDomConverter(operatorClass, new ParameterDescriptorFactory());

                DomElement parametersElement = createDomElement(parametersResource.getContent());
                try {
                    domConverter.convertDomToValue(parametersElement, container);
                } catch (ConversionException e) {
                    String msgPattern = "Operator '%s': cannot convert XML parameters for reason: %s";
                    throw new RuntimeException(String.format(msgPattern, operatorName, e.getMessage()), e);
                }
            }
        }

        for (Entry<String, String> entry : parameterMap.entrySet()) {
            String paramName = entry.getKey();
            String paramValue = entry.getValue();
            final Property property = container.getProperty(paramName);
            if (property != null) {
                property.setValueFromText(paramValue);
            } else {
                throw new RuntimeException(String.format(
                        "Operator '%s': unknown parameter '%s'", operatorName, paramName));
            }
        }
        return parameters;
    }

    private static DomElement createDomElement(String xml) {
        XppDomWriter domWriter = new XppDomWriter();
        new HierarchicalStreamCopier().copy(new XppReader(new StringReader(xml), new MXParser()), domWriter);
        XppDom xppDom = domWriter.getConfiguration();
        return new XppDomElement(xppDom);
    }

    private Map<String, Product> getSourceProductMap() throws IOException {
        SortedMap<File, Product> fileToProductMap = new TreeMap<>();
        SortedMap<String, Product> productMap = new TreeMap<>();
        SortedMap<String, String> sourceFilePathsMap = commandLineArgs.getSourceFilePathMap();
        for (Entry<String, String> entry : sourceFilePathsMap.entrySet()) {
            String sourceId = entry.getKey();
            String sourceFilePath = entry.getValue();
            Product product = addProduct(sourceFilePath, fileToProductMap);
            productMap.put(sourceId, product);
        }
        return productMap;
    }


    private Product addProduct(String sourceFilepath, Map<File, Product> fileToProductMap) throws IOException {
        File sourceFile = new File(sourceFilepath).getCanonicalFile();
        Product product = fileToProductMap.get(sourceFile);
        if (product == null) {
            String s = sourceFile.getPath();
            product = readProduct(s);
            if (product == null) {
                throw new IOException("No appropriate product reader found for " + sourceFile);
            }
            fileToProductMap.put(sourceFile, product);
        }
        return product;
    }

    // TODO - also use this scheme in the GPF GUIs (nf, 2012-03-02)
    // See also [BEAM-1375] Allow gpt to use template variables in parameter files
    private Map<String, String> getRawParameterMap() throws Exception {
        Map<String, String> parameterMap;
        String parameterFilePath = commandLineArgs.getParameterFilePath();
        if (parameterFilePath != null) {
            // put command line parameters in the Velocity context so that we can reference them in the parameters file
            VelocityContext velocityContext = metadataResourceEngine.getVelocityContext();
            velocityContext.put("parameters", commandLineArgs.getParameterMap());

            Resource parameterFile = metadataResourceEngine.readResource("parameterFile", parameterFilePath);
            Map<String, String> configFileMap = parameterFile.getMap();
            if (!parameterFile.isXml()) {
                configFileMap.putAll(commandLineArgs.getParameterMap());
            }
            parameterMap = configFileMap;
        } else {
            parameterMap = new HashMap<>();
        }

        // CLI parameters shall always overwrite file parameters
        parameterMap.putAll(commandLineArgs.getParameterMap());
        metadataResourceEngine.getVelocityContext().put("parameters", parameterMap);
        return parameterMap;
    }

    private Map<String, String> getSourceNodeIdMap() throws IOException {
        SortedMap<File, String> fileToNodeIdMap = new TreeMap<>();
        SortedMap<String, String> nodeIdMap = new TreeMap<>();
        SortedMap<String, String> sourceFilePathsMap = commandLineArgs.getSourceFilePathMap();
        for (Entry<String, String> entry : sourceFilePathsMap.entrySet()) {
            String sourceId = entry.getKey();
            String sourceFilePath = entry.getValue();
            String nodeId = addNodeId(sourceId, sourceFilePath, fileToNodeIdMap);
            nodeIdMap.put(sourceId, nodeId);
        }
        return nodeIdMap;
    }

    private String addNodeId(String sourceId, String sourceFilePath,
                             Map<File, String> fileToNodeId) throws IOException {
        File sourceFile = new File(sourceFilePath).getCanonicalFile();
        return fileToNodeId.computeIfAbsent(sourceFile, k -> READ_OP_ID_PREFIX + sourceId);
    }

    Product readProduct(String filePath) throws IOException {
        return commandLineContext.readProduct(filePath);
    }

    void writeProduct(Product targetProduct, String filePath, String formatName,
                      boolean clearCacheAfterRowWrite) throws IOException {
        commandLineContext.writeProduct(targetProduct, filePath, formatName, clearCacheAfterRowWrite);
    }

    Graph readGraph(String filePath, Map<String, String> templateVariables) throws IOException, GraphException {
        return commandLineContext.readGraph(filePath, templateVariables);
    }

    void executeGraph(Graph graph) throws GraphException {
        commandLineContext.executeGraph(graph, this);
    }

    private void runVelocityTemplates() {
        String velocityDirPath = commandLineArgs.getVelocityTemplateDirPath();
        File velocityDir;
        boolean velocityDirPathGiven;
        if (velocityDirPath != null) {
            velocityDir = new File(velocityDirPath);
            velocityDirPathGiven = true;
        } else {
            velocityDir = new File(CommandLineArgs.DEFAULT_VELOCITY_TEMPLATE_DIRPATH);
            velocityDirPathGiven = false;
        }

        String[] templateNames = velocityDir.list((dir, name) -> name.toLowerCase().endsWith(CommandLineArgs.VELOCITY_TEMPLATE_EXTENSION));

        Logger logger = commandLineContext.getLogger();

        if (templateNames == null) {
            if (velocityDirPathGiven) {
                String msgPattern = "Velocity template directory '%s' does not exist or inaccessible";
                logger.severe(String.format(msgPattern, velocityDir));
            }
            return;
        }

        if (templateNames.length == 0) {
            if (velocityDirPathGiven) {
                String msgPattern = "Velocity template directory '%s' does not contain any templates (*.vm)";
                logger.warning(String.format(msgPattern, velocityDir));
            }
            return;
        }


        // It can happen that we have no target file when the operator implements the Output interface
        if (!commandLineContext.isFile(commandLineArgs.getTargetFilePath())) {
            if (velocityDirPathGiven) {
                String msgPattern = "Target file '%s' does not exist, but is required to process velocity templates";
                logger.warning(String.format(msgPattern, commandLineArgs.getTargetFilePath()));
            }
            return;
        }

        for (String templateName : templateNames) {
            try {
                String templatePath = velocityDir + "/" + templateName;

                String msgPattern = "Processing metadata template " + templatePath;
                logger.info(String.format(msgPattern, commandLineArgs.getTargetFilePath()));

                metadataResourceEngine.writeRelatedResource(templatePath,
                                                            commandLineArgs.getTargetFilePath());
            } catch (IOException e) {
                String msgPattern = "Can't write related resource using template file '%s': %s";
                logSevereProblem(String.format(msgPattern, templateName, e.getMessage()), e);
            }
        }
    }

    private void logSevereProblem(String message, Exception e) {
        if (commandLineArgs.isStackTraceDump()) {
            commandLineContext.getLogger().log(Level.SEVERE, message, e);
        } else {
            commandLineContext.getLogger().severe(message);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    //  GraphProcessingObserver impl

    @Override
    public void graphProcessingStarted(GraphContext graphContext) {
    }

    @Override
    public void graphProcessingStopped(GraphContext graphContext) {
        VelocityContext velocityContext = metadataResourceEngine.getVelocityContext();
        velocityContext.put("graph", graphContext.getGraph());
        Product[] outputProducts = graphContext.getOutputProducts();
        if (outputProducts.length >= 1) {
            velocityContext.put("targetProduct", outputProducts[0]);
        }
        velocityContext.put("targetProducts", outputProducts);

        Product sourceProduct = null;
        Operator currentOperator = null;
        Map<String, Product> sourceProducts = new HashMap<>();
        for (Node node : graphContext.getGraph().getNodes()) {
            final NodeContext nodeContext = graphContext.getNodeContext(node);
            currentOperator = nodeContext.getOperator();
            if (currentOperator instanceof ReadOp) {
                final Product product = currentOperator.getTargetProduct();
                if (sourceProduct == null) {
                    sourceProduct = product;
                }
                if (node.getId().startsWith(READ_OP_ID_PREFIX)) {
                    final String sourceId = node.getId().substring(READ_OP_ID_PREFIX.length());
                    sourceProducts.put(sourceId, product);
                }
            }
        }
        if (currentOperator != null) {
            currentOperator.stopTileComputationObservation();
        }

        velocityContext.put("sourceProduct", sourceProduct);
        velocityContext.put("sourceProducts", sourceProducts);
    }

    @Override
    public void tileProcessingStarted(GraphContext graphContext, Rectangle tileRectangle) {
    }

    @Override
    public void tileProcessingStopped(GraphContext graphContext, Rectangle tileRectangle) {
    }

}
