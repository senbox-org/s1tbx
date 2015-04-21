/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package com.bc.ceres.jai.operator;

import com.bc.ceres.jai.ExpressionCompilerConfig;

import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderedRegistryMode;
import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


/**
 * An <code>OperationDescriptor</code> describing the "Expression"
 * operation.
 * <p> The Expression operation creates a single-banded, tiled rendered
 * image, where all the samples are computed from a given mathematical
 * (Java) expression.
 */
public class ExpressionDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
            {"GlobalName", "Expression"},
            {"LocalName", "Expression"},
            {"Vendor", "com.bc.ceres.jai"},
            {"Description", "Computes a single-banded image using a Java expression."},
            {"DocURL", ""},
            {"Version", "1.0"},
            {"arg0Desc", "The type of the destination image."},
            {"arg1Desc", "An arbitrary Java expression."},
            {"arg2Desc", "The configuration for the Java expression compiler."},
    };

    private static final String[] supportedModes = {RenderedRegistryMode.MODE_NAME};
    private static final String[] sourceNames = {"sources"};
    private static final Class[][] sourceTypes = {{Map.class}};
    private static final String[] paramNames = {"dataType", "expression", "compilerConfig"};
    private static final Class[] paramClasses = {Integer.class, String.class, ExpressionCompilerConfig.class};
    private static final Object[] paramDefaults = {NO_PARAMETER_DEFAULT, NO_PARAMETER_DEFAULT, NO_PARAMETER_DEFAULT};
    private static final Set<Integer> validTypeValues = new TreeSet<Integer>(Arrays.asList(DataBuffer.TYPE_BYTE,
                                                                                           DataBuffer.TYPE_SHORT,
                                                                                           DataBuffer.TYPE_USHORT,
                                                                                           DataBuffer.TYPE_INT,
                                                                                           DataBuffer.TYPE_FLOAT,
                                                                                           DataBuffer.TYPE_DOUBLE));
    private static final Object[] validParamValues = {null, null, null};

    /**
     * Constructor.
     */
    public ExpressionDescriptor() {
        super(resources,
              supportedModes,
              sourceNames,
              sourceTypes,
              paramNames,
              paramClasses,
              paramDefaults,
              validParamValues);
    }

    /**
     * Validates the sources.
     */
    @Override
    protected boolean validateSources(String modeName, ParameterBlock args, StringBuffer message) {
        if (!super.validateSources(modeName, args, message)) {
            return false;
        }
        final Map<Object, Object> source = (Map<Object, Object>) args.getSource(0);
        if (source.isEmpty()) {
            message.append("At least a single source must be given.");
            return false;
        }
        final Map.Entry[] entries = source.entrySet().toArray(new Map.Entry[0]);
        RenderedImage image0 = null;
        for (Map.Entry entry : entries) {
            if (!(entry.getKey() instanceof String)) {
                message.append("Invalid key in source map.");
                return false;
            }
            if (!(entry.getValue() instanceof RenderedImage)) {
                message.append("Invalid value in source map.");
                return false;
            }
            final RenderedImage image = (RenderedImage) entry.getValue();
            if (image.getSampleModel().getNumBands() != 1) {
                message.append("All images in the source map must have exactly one band.");
                return false;
            }
            if (image0 != null) {
                if (image.getWidth() != image0.getWidth()
                        || image.getHeight() != image0.getHeight()) {
                    message.append("All images in the source map must have the same width x height.");
                    return false;
                }
            } else {
                image0 = image;
            }
        }
        return true;
    }

    /**
     * Validates the parameters.
     */
    @Override
    protected boolean validateParameters(String modeName,
                                         ParameterBlock args,
                                         StringBuffer message) {
        if (!super.validateParameters(modeName, args, message)) {
            return false;
        }

        final int dataType = args.getIntParameter(0);
        if (!validTypeValues.contains(dataType)) {
            message.append("Parameter 'dataType' is not valid.");
            return false;
        }

        final String expression = ((String) args.getObjectParameter(1)).trim();
        int length = expression.length();
        if (length == 0) {
            message.append("Parameter 'expression' must not be empty.");
            return false;
        }
        // todo - further validate expression here

        return true;
    }


    /**
     * Creates an expression image.
     * <p>Creates a <code>ParameterBlockJAI</code> from all
     * supplied arguments except <code>hints</code> and invokes
     * {@link JAI#create(String,ParameterBlock,RenderingHints)}.
     *
     * @param sources        The name-to-source map.
     * @param dataType       The destination image's data type.
     * @param expression     The mathematical expression.
     * @param compilerConfig The configuration for the Java expression compiler.
     * @param hints          The <code>RenderingHints</code> to use.
     *                       May be <code>null</code>.
     *
     * @return The <code>RenderedOp</code> destination.
     *
     * @throws IllegalArgumentException if <code>expression</code> is <code>invalid</code>.
     */
    public static RenderedOp create(Map<String, RenderedImage> sources,
                                    int dataType,
                                    String expression,
                                    ExpressionCompilerConfig compilerConfig,
                                    RenderingHints hints) {
        ParameterBlockJAI pb =
                new ParameterBlockJAI("Expression",
                                      RenderedRegistryMode.MODE_NAME);

        pb.addSource(sources);
        pb.setParameter("dataType", dataType);
        pb.setParameter("expression", expression);
        pb.setParameter("compilerConfig", compilerConfig);

        return JAI.create("Expression", pb, hints);
    }
}
