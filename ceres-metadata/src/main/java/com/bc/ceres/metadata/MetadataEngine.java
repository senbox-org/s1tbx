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

package com.bc.ceres.metadata;

import com.bc.ceres.core.Assert;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * A engine for processing metadata using velocity. It supports:
 * <ul>
 * <li>reading metadata resources in XML or property format</li>
 * <li>reading of metadata items belonging to source items</li>
 * <li>allows to manipulate the used velocity context</li>
 * <li>writing the context using given templates</li>
 * </ul>
 *
 * @author MarcoZ
 * @author Bettina
 * @since Ceres 0.13.2
 */
public class MetadataEngine {

    private static final String VELOCITY_TEMPLATE_EXTENSION = ".vm";

    private final VelocityContext velocityContext;
    private final VelocityEngine velocityEngine;
    private final SimpleFileSystem simpleFileSystem;

    /**
     * Creates the metadata engine.
     *
     * @param simpleFileSystem A abstraction of the used filesystem.
     * @throws Exception If the initialization of velocity fails.
     */
    public MetadataEngine(SimpleFileSystem simpleFileSystem) throws Exception {
        Assert.notNull(simpleFileSystem, "ioAccessor");
        this.velocityContext = new VelocityContext();
        this.simpleFileSystem = simpleFileSystem;
        this.velocityEngine = createEngine();
    }

    private static VelocityEngine createEngine() throws Exception {
        final Properties veConfig = new Properties();
        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.init(veConfig);
        return velocityEngine;
    }

    /**
     * Return the used velocity context.
     *
     * @return the velocity context
     */
    public VelocityContext getVelocityContext() {
        return velocityContext;
    }

    /**
     * Reads the metadata from the resource with the given path and stores its content in the velocity
     * context using the given key. If evaluate is set to {@code true}. The content will be evaluated
     * using the current state of the velocity context.
     *
     * @param key      The name under which the metadata are placed into the velocity context
     * @param path     The path name of the metadata resource
     * @param evaluate Whether to evaluate the metadata
     * @throws Exception If an error occurs
     */
    public void readMetadata(String key, String path, boolean evaluate) throws Exception {
        Reader reader = simpleFileSystem.getReader(path);
        try {
            velocityContext.put(key, readConfigurationFile(reader, evaluate));
        } finally {
            reader.close();
        }
    }

    /**
     * Reads the all metadata file belonging to the given source item and places them into
     * the velocity context.
     * <p/>
     * Metadata files belong to a source if they follow a naming pattern:
     * For a given {@code sourcePath} "chl_a.nc", e.g. "chl_a-metadata.xml" and "chl_a-report.html" are considered.
     *
     * @param sourceId   The name under which the metadata are placed into the velocity context
     * @param sourcePath The path name of the source item
     * @throws Exception If an error occurs
     */
    public void readSourceMetadata(String sourceId, String sourcePath) throws Exception {
        velocityContext.put(sourceId, getSourceMetadataResources(sourcePath));
    }

    /**
     * Writes metadata belonging to the given target item. For this the velocity template is evaluated
     * using the current context.
     * <p/>
     * The target metadata files follow a naming pattern:
     * For a given {@code templatePath} "report.xml.vm" and {@code targetPath} "chl_a.nc"
     * a target metadata file "chl_a-report.xml" gets written.
     *
     * @param templatePath The path name of the velocity template
     * @param targetPath   The path name of the target item
     * @throws Exception If an error occurs
     */
    public void writeTargetMetadata(String templatePath, String targetPath) throws IOException {
        String templateName = getBasename(templatePath);
        String templateBaseName = templateName.substring(0, templateName.length() - VELOCITY_TEMPLATE_EXTENSION.length());
        velocityContext.put("templateName", templateName);
        velocityContext.put("templateBaseName", templateBaseName);

        String outputPath = removeFileExtension(targetPath) + "-" + templateBaseName;
        Reader templateReader = simpleFileSystem.getReader(templatePath);
        Writer writer = simpleFileSystem.getWriter(outputPath);
        try {
            velocityEngine.evaluate(velocityContext, writer, "metadataEngine", templateReader);
        } finally {
            templateReader.close();
            writer.close();
        }
    }

    private MetadataResource readConfigurationFile(Reader reader, boolean evaluate) throws Exception {
        String content = readTextFile(reader, evaluate);
        return new MetadataResource(content);
    }

    private String readTextFile(Reader reader, boolean evaluate) throws Exception {
        if (evaluate) {
            StringWriter stringWriter = new StringWriter();
            velocityEngine.evaluate(velocityContext, stringWriter, "metadataEngine", reader);
            return stringWriter.toString();
        } else {
            return readText(reader).trim();
        }
    }

    private Map<String, MetadataResource> getSourceMetadataResources(String sourcePath) throws Exception {
        final String fileName = getBasename(sourcePath);
        final String dirName = getDirname(sourcePath);
        final String wantedPrefix = removeFileExtension(fileName) + "-";

        final Map<String, MetadataResource> sourceFileContentMap = new HashMap<String, MetadataResource>();

        String[] directoryList = simpleFileSystem.list(dirName);
        if (directoryList != null) {
            for (String name : directoryList) {
                if (!name.equalsIgnoreCase(fileName) && name.startsWith(wantedPrefix)) {
                    Reader reader = simpleFileSystem.getReader(dirName + "/" + name);
                    try {
                        final MetadataResource metadataResource = readConfigurationFile(reader, true);
                        String metadataBaseName = name.substring(wantedPrefix.length());
                        sourceFileContentMap.put(metadataBaseName, metadataResource);
                    } finally {
                        reader.close();
                    }
                }
            }
        }
        return sourceFileContentMap;
    }

    static String removeFileExtension(String fileName) {
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            return fileName.substring(0, i);
        }
        return fileName;
    }

    static String getBasename(String path) {
        String pathNormalized = path.replace('\\', '/');
        int i = pathNormalized.lastIndexOf('/');
        if (i >= 0) {
            return pathNormalized.substring(i + 1);
        }
        return pathNormalized;
    }

    static String getDirname(String path) {
        String pathNormalized = path.replace('\\', '/');
        int i = pathNormalized.lastIndexOf('/');
        if (i > 0) {
            return pathNormalized.substring(0, i);
        }
        return "";
    }

    private static String readText(Reader reader) throws IOException {
        final BufferedReader br = new BufferedReader(reader);
        StringBuilder text = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            text.append(line);
            text.append("\n");
        }
        return text.toString();
    }
}
