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

package com.bc.ceres.jai.opimage;

import com.bc.ceres.compiler.CodeCompiler;
import com.bc.ceres.jai.ExpressionCompilerConfig;

import javax.media.jai.CRIFImpl;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.OpImage;
import java.awt.RenderingHints;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.util.Map;
import java.util.Vector;

/**
 * A <code>CRIF</code> supporting the "Expression" operation in the rendered
 * and renderable image layers.
 *
 * @see com.bc.ceres.jai.operator.ExpressionDescriptor
 */
public class ExpressionCRIF extends CRIFImpl {

    /**
     * Constructor.
     */
    public ExpressionCRIF() {
        super("Expression");
    }

    /**
     * Creates a new instance of <code>ExpressionOpImage</code> in the rendered
     * layer. This method satisfies the implementation of RIF.
     *
     * @param paramBlock  The two source images to be added.
     * @param renderHints Optionally contains destination image layout.
     */
    @Override
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {

        Map<String, RenderedImage> sourceMap = (Map<String, RenderedImage>) paramBlock.getSource(0);

        int dataType = paramBlock.getIntParameter(0);
        String expression = (String) paramBlock.getObjectParameter(1);
        ExpressionCompilerConfig compilerConfig = (ExpressionCompilerConfig) paramBlock.getObjectParameter(2);

        ImageLayout layout = renderHints != null ? (ImageLayout) renderHints.get(JAI.KEY_IMAGE_LAYOUT) : null;
        if (layout == null) {
            layout = new ImageLayout();
        }
        final RenderedImage sourceImage = sourceMap.values().iterator().next();
        layout.setWidth(sourceImage.getWidth());
        layout.setHeight(sourceImage.getHeight());
        layout.setSampleModel(new PixelInterleavedSampleModel(dataType,
                                                              sourceImage.getWidth(),
                                                              sourceImage.getHeight(),
                                                              1,
                                                              sourceImage.getWidth(),
                                                              new int[]{0}));
        return create(sourceMap, expression, compilerConfig, renderHints, layout);
    }

    private RenderedImage create(Map<String, RenderedImage> sourceMap,
                                 String expression,
                                 ExpressionCompilerConfig compilerConfig,
                                 Map config,
                                 ImageLayout layout) {
        final String packageName = getClass().getPackage().getName();
        final String className = "ExpressionOpImage_" + Long.toHexString(System.nanoTime());
        ExpressionCode code = ExpressionCodeGenerator.generate(packageName,
                                                               className,
                                                               sourceMap,
                                                               layout.getSampleModel(null).getDataType(),
                                                               expression);
        try {
            Class<?> opImageClass = new CodeCompiler(compilerConfig.getOutputDir(), compilerConfig.getClassPath()).compile(code);
            return (OpImage) opImageClass.getConstructor(Vector.class, Map.class, ImageLayout.class).newInstance(code.getSources(), config, layout);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
