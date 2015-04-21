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

package com.bc.ceres.resource;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.StringWriter;

/**
 * An engine to transform resources using a {@link VelocityEngine}.
 * <p>
 * Resources given to this engine are processed and added to the embedded
 * {@link VelocityContext}. For the evaluation the current {@link VelocityContext}
 * and its content is used.
 * <p>
 * Resources can be retrieved afterwards.
 * <p>
 *
 * @author MarcoZ
 * @author Bettina
 * @since Ceres 0.13.2
 */
public class ResourceEngine {

    private final VelocityContext velocityContext;
    private final VelocityEngine velocityEngine;

    /**
     * Create a resource engine.
     */
    public ResourceEngine() {
        this(createVelocityEngine());
    }

    /**
     * Create a resource engine using the given velocity engine.
     *
     * @param velocityEngine A velocity engine
     */
    public ResourceEngine(VelocityEngine velocityEngine) {
        this.velocityEngine = velocityEngine;
        this.velocityContext = new VelocityContext();
    }

    private static VelocityEngine createVelocityEngine() {
        VelocityEngine velocityEngine = new VelocityEngine();
        try {
            velocityEngine.init();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize velocity engine", e);
        }
        return velocityEngine;
    }

    /**
     * Return the used velocity context.
     *
     * @return the velocity context.
     */
    public VelocityContext getVelocityContext() {
        return velocityContext;
    }

    /**
     * Evaluates the given {@link Resource} using the current {@link VelocityContext} and return the result.
     *
     * @param resource the resource
     * @return The result of the processing
     */
    public Resource processResource(Resource resource) {
        StringWriter stringWriter = new StringWriter();
        String content = resource.getContent();
        velocityEngine.evaluate(velocityContext, stringWriter, "resourceEngine", content);
        String processedContent = stringWriter.toString();
        Resource processedResource = resource;
        if (!processedContent.equals(content)) {
            processedResource = new StringResource(resource.getPath(), processedContent, resource);
        }

        return processedResource;
    }

    /**
     * Evaluates the given {@link Resource} using the current {@link VelocityContext} and adds the
     * result of this processing to the {@link VelocityContext} under given name.
     * <p>
     * The evaluated {@link Resource} is returned.
     *
     * @param name     The name under which the resource is added
     * @param resource the resource
     * @return The result of the processing
     */
    public Resource processAndAddResource(String name, Resource resource) {
        Resource processedResource = processResource(resource);
        velocityContext.put(name, processedResource);
        return processedResource;
    }


    /**
     * Retrieves a resource registered under the given name in the {@link VelocityContext}.
     * If no resource is registered under the given name a {@link IllegalArgumentException} is thrown.
     *
     * @param name The name of the resource
     * @return The registered resource
     * @throws IllegalArgumentException If no resource of the given name exists.
     */
    public Resource getResource(String name) {
        Object object = velocityContext.get(name);
        if (object instanceof Resource) {
            return (Resource) object;
        }
        throw new IllegalArgumentException("The requested resource is not of type resource.");
    }
}