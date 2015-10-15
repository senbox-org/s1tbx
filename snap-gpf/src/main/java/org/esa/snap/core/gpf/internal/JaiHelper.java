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

package org.esa.snap.core.gpf.internal;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.util.ProductUtils;

import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import java.awt.RenderingHints;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class JaiHelper {

    public static final Interpolation DEFAULT_INTERPOLATION = Interpolation.getInstance(Interpolation.INTERP_NEAREST);

    public static Product createTargetProduct(Product sourceProduct,
                                              String[] bandNames,
                                              String operationName,
                                              HashMap<String, Object> operationParameters,
                                              RenderingHints renderingHints) throws OperatorException {
        if (operationName == null) {
            throw new OperatorException("Missing parameter 'operationName'.");
        }
        if (operationParameters == null) {
            operationParameters = new HashMap<String, Object>(0);
        }
        if (renderingHints == null) {
            renderingHints = new RenderingHints(JAI.KEY_INTERPOLATION, DEFAULT_INTERPOLATION);
        } else if (!renderingHints.containsKey(JAI.KEY_INTERPOLATION)) {
            renderingHints.put(JAI.KEY_INTERPOLATION, DEFAULT_INTERPOLATION);
        }

        Band[] sourceBands = getBands(sourceProduct, bandNames);
        TiePointGrid[] tiePointGrids = getTiePointGrids(sourceProduct, bandNames);

        if (sourceBands.length == 0 && tiePointGrids.length == 0) {
            return new Product("jai", "jai", 0, 0);
        }

        final RenderedOp[] bandRenderedOps = new RenderedOp[sourceBands.length];
        int maxWidth = -1;
        int maxHeight = -1;
        for (int i = 0; i < sourceBands.length; i++) {
            bandRenderedOps[i] = createTargetImage(sourceBands[i], operationName, operationParameters, renderingHints);
            if (bandRenderedOps[i].getWidth() > maxWidth) {
                maxWidth = bandRenderedOps[i].getWidth();
            }
            if (bandRenderedOps[i].getHeight() > maxHeight) {
                maxHeight = bandRenderedOps[i].getHeight();
            }
        }

        Product targetProduct = new Product("jai", "jai", maxWidth, maxHeight);
        for (int i = 0; i < sourceBands.length; i++) {
            Band sourceBand = sourceBands[i];
            Band targetBand = new Band(sourceBand.getName(), sourceBand.getDataType(), bandRenderedOps[i].getWidth(),
                                       bandRenderedOps[i].getHeight());
            ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);
            targetProduct.addBand(targetBand);
            targetBand.setSourceImage(bandRenderedOps[i]);
        }
        for (final TiePointGrid sourceGrid : tiePointGrids) {
            Band targetBand = new Band(sourceGrid.getName(), sourceGrid.getDataType(), maxWidth, maxHeight);
            ProductUtils.copyRasterDataNodeProperties(sourceGrid, targetBand);
            targetBand.setSourceImage(createTargetImage(sourceGrid, operationName, operationParameters, renderingHints));
            targetProduct.addBand(targetBand);
        }

        // todo
        // ProductUtils.copyGeoCoding();
        return targetProduct;
    }

    private static TiePointGrid[] getTiePointGrids(Product sourceProduct, String[] names) {
        TiePointGrid[] tiePointGrids = sourceProduct.getTiePointGrids();
        if (names != null && names.length > 0) {
            ArrayList<TiePointGrid> tiePointGridList = new ArrayList<TiePointGrid>(sourceProduct.getNumBands());
            for (String name : names) {
                final TiePointGrid tiePointGrid = sourceProduct.getTiePointGrid(name);
                if (tiePointGrid != null) {
                    tiePointGridList.add(tiePointGrid);
                }
            }
            tiePointGrids = tiePointGridList.toArray(new TiePointGrid[tiePointGridList.size()]);
        }
        return tiePointGrids;
    }

    private static Band[] getBands(Product product, String[] names) {
        Band[] bands = product.getBands();
        if (names != null && names.length > 0) {
            ArrayList<Band> sourceBandList = new ArrayList<Band>(product.getNumBands());
            for (String name : names) {
                final Band band = product.getBand(name);
                if (band != null) {
                    sourceBandList.add(band);
                }
            }
            bands = sourceBandList.toArray(new Band[sourceBandList.size()]);
        }
        return bands;
    }

    public static RenderedOp createTargetImage(RasterDataNode sourceBand,
                                                String operationName,
                                                HashMap<String, Object> operationParameters,
                                                RenderingHints renderingHints) {
        final ParameterBlockJAI parameterBlock = new ParameterBlockJAI(operationName);
        parameterBlock.addSource(sourceBand.getSourceImage());
        for (Map.Entry<String, Object> parameter : operationParameters.entrySet()) {
            try {
                parameterBlock.setParameter(parameter.getKey(), parameter.getValue());
            } catch (IllegalArgumentException e) {
                throw new OperatorException(MessageFormat.format("Illegal parameter ''{0}'' for JAI operation ''{1}''.",
                                                                 parameter.getKey(), operationName), e);
            }
        }
        try {
            return JAI.create(operationName, parameterBlock, renderingHints);
        } catch (Exception e) {
            throw new OperatorException(MessageFormat.format("Illegal source or parameters for JAI operation ''{0}''.", operationName), e);
        }
    }
}
