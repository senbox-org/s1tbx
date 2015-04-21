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

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderedRegistryMode;
import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;

/**
 * An <code>OperationDescriptor</code> describing the "Reinterpret"
 * operation.
 * <p>
 * The Reinterpret operation creates a single-banded, tiled rendered
 * image, where the source samples are rescaled or reformatted.
 */
public class ReinterpretDescriptor extends OperationDescriptorImpl {

    public static final String PARAM_NAME_FACTOR = "factor";
    public static final String PARAM_NAME_OFFSET = "offset";
    public static final String PARAM_NAME_SCALING_TYPE = "scalingType";
    public static final String PARAM_NAME_INTERPRETATION_TYPE = "interpretationType";

    /**
     * The default interpretation type: all data types are interpreted as in AWT
     */
    public static final InterpretationType AWT = new InterpretationType("AWT", 0);
    /**
     * Interpretation type for interpreting byte data as signed.
     */
    public static final InterpretationType INTERPRET_BYTE_SIGNED = new InterpretationType("INTERPRET_BYTE_SIGNED", 1);
    /**
     * Interpretation type for interpreting integer data as unsigned.
     */
    public static final InterpretationType INTERPRET_INT_UNSIGNED = new InterpretationType("INTERPRET_INT_UNSIGNED", 2);
    /**
     * The default scaling type: source samples are scaled linearly.
     */
    public static final ScalingType LINEAR = new ScalingType("LINEAR", 0);
    /**
     * Exponential scaling type: source samples are scaled linearly
     * and then raised to the power of 10.
     */
    public static final ScalingType EXPONENTIAL = new ScalingType("EXPONENTIAL", 1);
    /**
     * Logarithmic scaling type: source samples are scaled linearly
     * and then their logarithms to the basis of 10 are computed.
     */
    public static final ScalingType LOGARITHMIC = new ScalingType("LOGARITHMIC", 2);

    private static final String[][] RESOURCES = {
            {"GlobalName", "Reinterpret"},
            {"LocalName", "Reinterpret"},
            {"Vendor", "com.bc.ceres.jai"},
            {"Description", "Reinterprets an image by applying a rescaling or reformatting."},
            {"DocURL", ""},
            {"Version", "1.0"},
            {"arg0Desc", "Scaling factor"},
            {"arg1Desc", "Scaling offset"},
            {"arg2Desc", "Scaling type"},
            {"arg3Desc", "Interpretation type"},
    };

    private static final String[] SUPPORTED_MODES = new String[]{RenderedRegistryMode.MODE_NAME};
    private static final String[] PARAM_NAMES =
            new String[]{PARAM_NAME_FACTOR, PARAM_NAME_OFFSET, PARAM_NAME_SCALING_TYPE, PARAM_NAME_INTERPRETATION_TYPE};

    private static final Class[] PARAM_CLASSES =
            new Class[]{Double.class, Double.class, ScalingType.class, InterpretationType.class};


    private static final Object[] PARAM_DEFAULTS =
            new Object[]{1.0, 0.0, LINEAR, AWT};

    /**
     * Constructs a new instance of this class.
     */
    public ReinterpretDescriptor() {
        super(RESOURCES, SUPPORTED_MODES, 1, PARAM_NAMES, PARAM_CLASSES, PARAM_DEFAULTS, null);
    }

    /**
     * Reinterprets an image.
     * <p>Creates a <code>ParameterBlockJAI</code> from all supplied arguments (except <code>hints</code>) and
     * invokes {@link JAI#create(String,java.awt.image.renderable.ParameterBlock,java.awt.RenderingHints)}.
     *
     * @param source             The <code>RenderedImage</code> source.
     * @param factor             The scaling factor.
     * @param offset             The scaling offset.
     * @param scalingType        The manner of scaling.
     * @param interpretationType The interpretation type.
     * @param hints              The <code>RenderingHints</code> to use. May be <code>null</code>.
     *
     * @return The <code>RenderedOp</code> destination.
     *
     * @throws IllegalArgumentException if <code>source</code> is <code>null</code>.
     * @see javax.media.jai.JAI
     * @see javax.media.jai.ParameterBlockJAI
     * @see javax.media.jai.RenderedOp
     */
    public static RenderedOp create(RenderedImage source, double factor, double offset,
                                    ScalingType scalingType, InterpretationType interpretationType,
                                    RenderingHints hints) {
        final ParameterBlockJAI pb = new ParameterBlockJAI("Reinterpret", RenderedRegistryMode.MODE_NAME);
        pb.setSource("source0", source);
        pb.setParameter(PARAM_NAME_FACTOR, factor);
        pb.setParameter(PARAM_NAME_OFFSET, offset);
        pb.setParameter(PARAM_NAME_SCALING_TYPE, scalingType);
        pb.setParameter(PARAM_NAME_INTERPRETATION_TYPE, interpretationType);

        return JAI.create("Reinterpret", pb, hints);
    }

    /**
     * Creates the target image layout for a given source and a given target sample model.
     *
     * @param source      The source.
     * @param sampleModel The target sample model.
     *
     * @return the target image layout.
     */
    public static ImageLayout createTargetImageLayout(RenderedImage source, SampleModel sampleModel) {
        final ImageLayout imageLayout = new ImageLayout(source);
        imageLayout.setSampleModel(sampleModel);
        return imageLayout;
    }

    /**
     * Returns the target data type for a given source data type and reinterpretation properties.
     *
     * @param sourceDataType     The source data type
     * @param factor             The scaling factor.
     * @param offset             The scaling offset.
     * @param scalingType        The manner of scaling.
     * @param interpretationType The interpretation type.
     *
     * @return the target data type.
     *
     * @see java.awt.image.DataBuffer
     */
    public static int getTargetDataType(int sourceDataType, double factor, double offset, ScalingType scalingType,
                                        InterpretationType interpretationType) {
        final boolean rescale = scalingType != LINEAR || factor != 1.0 || offset != 0.0;
        if (rescale) {
            switch (sourceDataType) {
                case DataBuffer.TYPE_BYTE:
                case DataBuffer.TYPE_USHORT:
                case DataBuffer.TYPE_SHORT:
                case DataBuffer.TYPE_FLOAT:
                    return DataBuffer.TYPE_FLOAT;
                case DataBuffer.TYPE_INT:
                case DataBuffer.TYPE_DOUBLE:
                    return DataBuffer.TYPE_DOUBLE;
                default:
                    return DataBuffer.TYPE_UNDEFINED;
            }
        } else {
            switch (sourceDataType) {
                case DataBuffer.TYPE_BYTE:
                    if (interpretationType == ReinterpretDescriptor.INTERPRET_BYTE_SIGNED) {
                        return DataBuffer.TYPE_SHORT;
                    }
                case DataBuffer.TYPE_INT:
                    if (interpretationType == ReinterpretDescriptor.INTERPRET_INT_UNSIGNED) {
                        return DataBuffer.TYPE_DOUBLE;
                    }
                default:
                    return sourceDataType;
            }
        }
    }
}
