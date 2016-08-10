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
import com.bc.ceres.resource.ReaderResource;
import com.bc.ceres.resource.Resource;
import com.bc.ceres.resource.ResourceEngine;
import org.apache.velocity.VelocityContext;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

/**
 * A engine for processing text resources using velocity. It supports:
 * <ul>
 * <li>reading resources in XML or property format</li>
 * <li>reading of resources belonging to source(s)</li>
 * <li>allows to manipulate the used velocity context</li>
 * <li>writing the context using given templates</li>
 * </ul>
 *
 * @author MarcoZ
 * @author Bettina
 * @since Ceres 0.13.2
 */
public class MetadataResourceEngine {

    private final ResourceEngine resourceEngine;
    private final MetadataResourceResolver metadataResourceResolver;
    private final SimpleFileSystem simpleFileSystem;

    /**
     * Creates the metadata engine.
     *
     * @param simpleFileSystem A abstraction of the used filesystem.
     */
    public MetadataResourceEngine(SimpleFileSystem simpleFileSystem) {
        this(simpleFileSystem, new MetadataResourceResolver(simpleFileSystem));
    }

    /**
     * Creates the metadata engine.
     *
     * @param simpleFileSystem         A abstraction of the used filesystem.
     * @param metadataResourceResolver A resolver for metadata resource names..
     */
    public MetadataResourceEngine(SimpleFileSystem simpleFileSystem, MetadataResourceResolver metadataResourceResolver) {
        Assert.notNull(simpleFileSystem, "simpleFileSystem");
        Assert.notNull(metadataResourceResolver, "metadataResourceResolver");
        this.resourceEngine = new ResourceEngine();
        this.simpleFileSystem = simpleFileSystem;
        this.metadataResourceResolver = metadataResourceResolver;
    }

    /**
     * Return the used velocity context.
     *
     * @return the velocity context
     */
    public VelocityContext getVelocityContext() {
        return resourceEngine.getVelocityContext();
    }

    /**
     * Reads the resource with the given path and stores its content in the velocity
     * context using the given key. If evaluate is set to {@code true}. The content will be evaluated
     * using the current state of the velocity context.
     *
     * @param name The name under which the metadata are placed into the velocity context
     * @param path The path name of the metadata resource
     * @return The resource
     * @throws IOException If an I/O error occurs
     */
    public Resource readResource(String name, String path) throws IOException {
        Reader reader = simpleFileSystem.createReader(path);
        return resourceEngine.processAndAddResource(name, new ReaderResource(path, reader));
    }

    /**
     * Reads the all metadata file belonging to the given source item and places them into a map registered as
     * 'sourceMetadata' the velocity context. The 'sourceMetadata'-map contains the processed metadata files as another map.
     * The map of metadata files can be retrieved using the key 'sourceId'.
     * <p>
     * Metadata files belong to a source if they follow a naming pattern:
     * For a given {@code sourcePath} "chl_a.nc", e.g. "chl_a-metadata.xml" and "chl_a-report.html" are considered.
     *
     * @param sourceId   The name under which the metadata are placed in the 'sourceMetadata'-map
     * @param sourcePath The path name of the source item
     * @throws IOException If an I/O error occurs
     */
    public void readRelatedResource(String sourceId, String sourcePath) throws IOException {
        SortedMap<String, String> sourceNames = metadataResourceResolver.getSourceMetadataPaths(sourcePath);
        HashMap<String, Resource> resourceMap = new HashMap<>();

        for (Map.Entry<String, String> sourceEntries : sourceNames.entrySet()) {
            String metadataBaseName = sourceEntries.getKey();
            String path = sourceEntries.getValue();

            Reader reader = simpleFileSystem.createReader(path);
            Resource resource = new ReaderResource(path, reader);

            Resource processedResource = resourceEngine.processResource(resource);
            resourceMap.put(metadataBaseName.replace(".", "_"), processedResource);
        }
        getVelocityMapSafe("sourceMetadata").put(sourceId, resourceMap);
    }

    private Map<String, Map> getVelocityMapSafe(String name) {
        Object mapObject = resourceEngine.getVelocityContext().get(name);
        Map<String, Map> map;
        if (mapObject instanceof Map) {
            map = (Map<String, Map>) mapObject;
        } else {
            map = new HashMap<>();
            resourceEngine.getVelocityContext().put(name, map);
        }
        return map;
    }

    /**
     * Writes metadata belonging to the given target item. For this the velocity template is evaluated
     * using the current context.
     * <p>
     * The target metadata files follow a naming pattern:
     * For a given {@code templatePath} "report.xml.vm" and {@code targetPath} "chl_a.nc"
     * a target metadata file "chl_a-report.xml" gets written.
     *
     * @param templatePath The path name of the velocity template
     * @param targetPath   The path name of the target item
     * @throws IOException If an I/O error occurs
     */
    public void writeRelatedResource(String templatePath, String targetPath) throws IOException {
        MetadataResourceResolver.TargetResourceInfo targetResourceInfo = metadataResourceResolver.getTargetName(templatePath, targetPath);

        VelocityContext velocityContext = resourceEngine.getVelocityContext();
        velocityContext.put("templateName", targetResourceInfo.templateName);
        velocityContext.put("templateBaseName", targetResourceInfo.templateBaseName);

        Reader templateReader = simpleFileSystem.createReader(templatePath);
        Resource processedResource = resourceEngine.processResource(new ReaderResource(templatePath, templateReader));

        Writer writer = simpleFileSystem.createWriter(targetResourceInfo.targetName);
        try {
            writer.write(processedResource.getContent());
        } finally {
            templateReader.close();
            writer.close();
        }
    }
}
