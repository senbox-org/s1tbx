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

import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderedRegistryMode;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;

/**
 * An <code>OperationDescriptor</code> describing the "Paint" operation.
 * <p>
 *
 * @author Norman Fomferra
 * @author Marco Peters
 */
public class PaintDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation and
     * specify the parameter list for a Paint operation.
     */
    private static final String[][] resources = {
            {"GlobalName", "Paint"},
            {"LocalName", "Paint"},
            {"Vendor", "com.bc.ceres.jai"},
            {"Description", "Paints a given color using a mask image."},
            {"DocURL", ""},
            {"Version", "1.0"},
            {"arg0Desc", "Paint Color"},
            {"arg1Desc", "Alpha is first"},
    };

    /**
     * The parameter names for the Paint operation.
     */
    private static final String[] paramNames = {
            "paintColor",
            "alphaIsFirst",
    };

    /**
     * The parameter class types for the Paint operation.
     */
    private static final Class[] paramClasses = {
            Color.class,
            Boolean.class,
    };

    /**
     * The parameter default values for the Paint operation.
     */
    private static final Object[] paramDefaults = {
            NO_PARAMETER_DEFAULT,
            Boolean.FALSE,
    };

    /**
     * Constructor.
     */
    public PaintDescriptor() {
        super(resources, new String[]{"rendered"}, 2, paramNames, paramClasses, paramDefaults, null);
    }

    /**
     * Paints.
     *
     * @param source0      The source image to paint on.
     * @param source1      The paint mask. Must be a one-banded image of type BYTE.
     * @param paintColor   The paint color.
     * @param alphaIsFirst Whether alpha is the first band
     * @param hints        The <code>RenderingHints</code> to use.
     *                     May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     *
     * @throws IllegalArgumentException if <code>source0</code> or <code>function</code> is <code>null</code>.
     * @see javax.media.jai.JAI
     * @see javax.media.jai.ParameterBlockJAI
     * @see javax.media.jai.RenderedOp
     */
    public static RenderedOp create(RenderedImage source0,
                                    RenderedImage source1,
                                    Color paintColor,
                                    Boolean alphaIsFirst,
                                    RenderingHints hints) {
        ParameterBlockJAI pb = new ParameterBlockJAI("Paint", RenderedRegistryMode.MODE_NAME);
        pb.setSource("source0", source0);
        pb.setSource("source1", source1);
        pb.setParameter("paintColor", paintColor);
        pb.setParameter("alphaIsFirst", alphaIsFirst);
        return JAI.create("Paint", pb, hints);
    }
}
