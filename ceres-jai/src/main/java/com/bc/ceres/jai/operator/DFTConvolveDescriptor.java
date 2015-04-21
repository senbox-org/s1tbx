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

import com.sun.media.jai.util.AreaOpPropertyGenerator;

import javax.media.jai.JAI;
import javax.media.jai.KernelJAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PropertyGenerator;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderedRegistryMode;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

/**
 * An <code>OperationDescriptor</code> describing the "DFTConvolve" operation.
 * <p> This operation behaves as the JAI standard "Convolve" operation but uses
 * a Fast Fourier Transformation for the convolution.
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>DFTConvolve</td></tr>
 * <tr><td>LocalName</td>   <td>DFTConvolve</td></tr>
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
public class DFTConvolveDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation and
     * specify the parameter list for a Convolve operation.
     */
    private static final String[][] resources = {
            {"GlobalName", "DFTConvolve"},
            {"LocalName", "DFTConvolve"},
            {"Vendor", "com.bc.ceres.jai"},
            {"Description", "Performs kernel-based convolution on an image using a DFT."},
            {"DocURL", "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/ConvolveDescriptor.html"},
            {"Version", "1.0"},
            {"arg0Desc", "The convolution kernel."},
            {"arg1Desc", "The Fourier-transformed convolution kernel image."}
    };

    /**
     * The parameter names for the Convolve operation.
     */
    private static final String[] paramNames = {
            "kernel",
            "kernelFT"
    };

    /**
     * The parameter class types for the Convolve operation.
     */
    private static final Class[] paramClasses = {
            javax.media.jai.KernelJAI.class,
            RenderedImage.class
    };

    /**
     * The parameter default values for the Convolve operation.
     */
    private static final Object[] paramDefaults = {
            NO_PARAMETER_DEFAULT,
            null
    };

    /**
     * Constructor.
     */
    public DFTConvolveDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }


    @Override
    protected boolean validateParameters(String s, ParameterBlock parameterBlock, StringBuffer stringBuffer) {
        // todo - implement (e.g. check kernelFT)
//        if (kernelFT.getMinX() != extendedImage.getMinX() 
//            && kernelFT.getMinX() != extendedImage.getMinX()
//                && kernelFT.getMinX() != extendedImage.getMinX()
//                && kernelFT.getMinX() != extendedImage.getMinX()) {
//            throw new
//        }
//
        return super.validateParameters(s, parameterBlock, stringBuffer);
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
     * {@link JAI#create(String, java.awt.image.renderable.ParameterBlock, java.awt.RenderingHints)}.
     *
     * @param source0  <code>RenderedImage</code> source 0.
     * @param kernel   The convolution kernel.
     * @param kernelFT The Fourier-transformed convolution kernel image, may be null.
     * @param hints    The <code>RenderingHints</code> to use.
     *                 May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code> or if
     *                                  if <code>kernel</code> is <code>null</code>.
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderedOp
     */
    public static RenderedOp create(RenderedImage source0,
                                    KernelJAI kernel,
                                    RenderedImage kernelFT,
                                    RenderingHints hints) {
        ParameterBlockJAI pb = new ParameterBlockJAI("DFTConvolve", RenderedRegistryMode.MODE_NAME);
        pb.setSource("source0", source0);
        pb.setParameter("kernel", kernel);
        pb.setParameter("kernelFT", kernelFT);
        return JAI.create("DFTConvolve", pb, hints);
    }
}