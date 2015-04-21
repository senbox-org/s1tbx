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

import com.bc.ceres.jai.GeneralFilterFunction;
import com.sun.media.jai.util.AreaOpPropertyGenerator;

import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PropertyGenerator;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderedRegistryMode;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;

/**
 * An <code>OperationDescriptor</code> describing the "Convolve" operation.
 * <p> Convolution is a spatial operation that computes each output
 * sample by multiplying elements of a kernel with the samples
 * surrounding a particular source sample.
 * <p> For each destination sample, the kernel is rotated 180 degrees
 * and its "key element," or origin, is placed over the source pixel
 * corresponding with the destination pixel.  The kernel elements are
 * multiplied with the source pixels beneath them, and the resulting
 * products are summed together to produce the destination sample
 * value.
 * <p> Pseudocode for the convolution operation on a single sample
 * dst[x][y] is as follows, assuming the kernel is of size width x height
 * and has already been rotated through 180 degrees.  The kernel's Origin
 * element is located at position (xOrigin, yOrigin):
 * <pre>
 * dst[x][y] = 0;
 * for (int i = -xOrigin; i &lt; -xOrigin + width; i++) {
 *     for (int j = -yOrigin; j &lt; -yOrigin + height; j++) {
 *         dst[x][y] += src[x + i][y + j]*kernel[xOrigin + i][yOrigin + j];
 *     }
 * }
 * </pre>
 * <p> Convolution, like any neighborhood operation, leaves a band of
 * pixels around the edges undefined.  For example, for a 3x3 kernel
 * only four kernel elements and four source pixels contribute to the
 * convolution pixel at the corners of the source image.  Pixels that
 * do not allow the full kernel to be applied to the source are not
 * included in the destination image.  A "Border" operation may be used
 * to add an appropriate border to the source image in order to avoid
 * shrinkage of the image boundaries.
 * <p> The kernel may not be bigger in any dimension than the image data.
 * <p>
 * It should be noted that this operation automatically adds a
 * value of <code>Boolean.TRUE</code> for the
 * <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> to the given
 * <code>configuration</code> so that the operation is performed
 * on the pixel values instead of being performed on the indices into
 * the color map if the source(s) have an <code>IndexColorModel</code>.
 * This addition will take place only if a value for the
 * <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> has not already been
 * provided by the user. Note that the <code>configuration</code> Map
 * is cloned before the new hint is added to it. The operation can be
 * smart about the value of the <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code>
 * <code>RenderingHints</code>, i.e. while the default value for the
 * <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> is
 * <code>Boolean.TRUE</code>, in some cases the operator could set the
 * default.
 * <table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>Convolve</td></tr>
 * <tr><td>LocalName</td>   <td>Convolve</td></tr>
 * <tr><td>Vendor</td>      <td>com.sun.media.jai</td></tr>
 * <tr><td>Description</td> <td>Performs kernel-based convolution
 * on an image.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/ConvolveDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The convolution kernel.</td></tr>
 * </table>
 * <table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>   <th>Class Type</th>
 * <th>Default Value</th></tr>
 * <tr><td>kernel</td> <td>javax.media.jai.KernelJAI</td>
 * <td>NO_PARAMETER_DEFAULT</td>
 * </table>
 *
 * @see javax.media.jai.OperationDescriptor
 * @see javax.media.jai.KernelJAI
 */
public class GeneralFilterDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation and
     * specify the parameter list for a Convolve operation.
     */
    private static final String[][] resources = {
            {"GlobalName", "GeneralFilter"},
            {"LocalName", "GeneralFilter"},
            {"Vendor", "com.bc.ceres.jai"},
            {"Description", "Creates a filtered image using an arbitrary, non-linear filter."},
            {"DocURL", ""},
            {"Version", "1.0"},
            {"arg0Desc", "General filter function"}
    };

    /**
     * The parameter names for the Convolve operation.
     */
    private static final String[] paramNames = {
            "filterFunction"
    };

    /**
     * The parameter class types for the Convolve operation.
     */
    private static final Class[] paramClasses = {
            GeneralFilterFunction.class
    };

    /**
     * The parameter default values for the Convolve operation.
     */
    private static final Object[] paramDefaults = {
            NO_PARAMETER_DEFAULT
    };

    /**
     * Constructor.
     */
    public GeneralFilterDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    /**
     * Returns an array of <code>PropertyGenerators</code> implementing
     * property inheritance for the "Convolve" operation.
     *
     * @return An array of property generators.
     */
    public PropertyGenerator[] getPropertyGenerators() {
        PropertyGenerator[] pg = new PropertyGenerator[1];
        pg[0] = new AreaOpPropertyGenerator();
        return pg;
    }


    /**
     * Performs kernel-based convolution on an image.
     * <p>Creates a <code>ParameterBlockJAI</code> from all
     * supplied arguments except <code>hints</code> and invokes
     * {@code JAI#create(String,ParameterBlock,java.awt.RenderingHints)}.
     *
     * @param source0  <code>RenderedImage</code> source 0.
     * @param function The filter function.
     * @param hints    The <code>RenderingHints</code> to use.
     *                 May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> or <code>function</code> is <code>null</code>.
     * @see javax.media.jai.JAI
     * @see javax.media.jai.ParameterBlockJAI
     * @see javax.media.jai.RenderedOp
     */
    public static RenderedOp create(RenderedImage source0,
                                    GeneralFilterFunction function,
                                    RenderingHints hints) {
        ParameterBlockJAI pb = new ParameterBlockJAI("GeneralFilter", RenderedRegistryMode.MODE_NAME);
        pb.setSource("source0", source0);
        pb.setParameter("filterFunction", function);
        return JAI.create("GeneralFilter", pb, hints);
    }
}
