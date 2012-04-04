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
package org.esa.beam.visat.internal;

import com.bc.ceres.core.Assert;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.datamodel.Mask.ImageType;
import org.esa.beam.util.Debug;
import org.esa.beam.visat.VisatApp;

import java.beans.PropertyVetoException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;

/**
 * Confirms Raster Data Node deletion by the user and performs them.
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.7
 */
public class RasterDataNodeDeleter {
    
    private static final String INDENT = "    ";
    
    public static void deleteVectorDataNode(VectorDataNode vectorDataNode) {
        Assert.notNull(vectorDataNode);
        Product product = vectorDataNode.getProduct();
        ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
        Mask vectorMask = null;
        for (int i = 0; i < maskGroup.getNodeCount(); i++) {
            Mask mask = maskGroup.get(i);
            if (mask.getImageType() == Mask.VectorDataType.INSTANCE && 
                    Mask.VectorDataType.getVectorData(mask) == vectorDataNode) {
                    vectorMask = mask;
                    break;
            }
        }
        String message;
        if (vectorMask != null) {
            List<RasterDataNode> virtualBands = getReferencedVirtualBands(vectorMask);
            List<RasterDataNode> validMaskNodes = getReferencedValidMasks(vectorMask);
            List<RasterDataNode> masks = getReferencedMasks(vectorMask);
            VectorDataNode[] nodes = new VectorDataNode[] {vectorDataNode};
            message = formatPromptMessage("Geometry", nodes, virtualBands, validMaskNodes, masks);
        } else {
            message = MessageFormat.format("Do you really want to delete the geometry ''{0}''?\nThis action cannot be undone.\n\n", vectorDataNode.getName());
        }
        int status = VisatApp.getApp().showQuestionDialog("Delete Geometry", message, null);
        if (status == JOptionPane.YES_OPTION) {
            product.getVectorDataGroup().remove(vectorDataNode);
        }
    }
    
    public static void deleteRasterDataNodes(RasterDataNode[] rasterNodes) {
        Assert.notNull(rasterNodes);
        if (rasterNodes.length == 0) {
            return;
        }
        Set<RasterDataNode> virtualBandsSet = new HashSet<RasterDataNode>();
        Set<RasterDataNode> validMaskNodesSet = new HashSet<RasterDataNode>();
        Set<RasterDataNode> masksSet = new HashSet<RasterDataNode>();
        
        for (RasterDataNode raster : rasterNodes) {
            virtualBandsSet.addAll(getReferencedVirtualBands(raster));
            validMaskNodesSet.addAll(getReferencedValidMasks(raster));
            masksSet.addAll(getReferencedMasks(raster));
        }
        for (RasterDataNode raster : rasterNodes) {
            virtualBandsSet.remove(raster);
            validMaskNodesSet.remove(raster);
            masksSet.remove(raster);
        }
        String typeName = getTypeName(rasterNodes);
        String message = formatPromptMessage(typeName, rasterNodes, virtualBandsSet, validMaskNodesSet, masksSet);
        deleteRasterDataNodesImpl(rasterNodes, message);
    }
    
    public static void deleteRasterDataNode(RasterDataNode raster) {
        Assert.notNull(raster);
        List<RasterDataNode> virtualBands = getReferencedVirtualBands(raster);
        List<RasterDataNode> validMaskNodes = getReferencedValidMasks(raster);
        List<RasterDataNode> masks = getReferencedMasks(raster);
        
        RasterDataNode[] rasters = new RasterDataNode[] {raster};
        String typeName = getTypeName(rasters);
        String message = formatPromptMessage(typeName, rasters, virtualBands, validMaskNodes, masks);
        deleteRasterDataNodesImpl(rasters, message);
    }

    private static void deleteRasterDataNodesImpl(RasterDataNode[] rasters, String message) {

        final int status = VisatApp.getApp().showQuestionDialog("Delete Raster Data",
                                                                message, null);
        if (status == JOptionPane.YES_OPTION) {
            for (RasterDataNode raster : rasters) {
                final JInternalFrame[] internalFrames = VisatApp.getApp().findInternalFrames(raster);
                for (final JInternalFrame internalFrame : internalFrames) {
                    try {
                        internalFrame.setClosed(true);
                    } catch (PropertyVetoException e) {
                        Debug.trace(e);
                    }
                }
                if (raster.hasRasterData()) {
                    raster.unloadRasterData();
                }
                final Product product = raster.getProduct();
                if (raster instanceof Mask) {
                    Mask mask = (Mask) raster;
                    product.getMaskGroup().remove(mask);
                    for (Band band : product.getBands()) {
                        deleteMaskFromGroup(band.getOverlayMaskGroup(), mask);
                    }
                    TiePointGrid[] tiePointGrids = product.getTiePointGrids();
                    for (TiePointGrid tiePointGrid : tiePointGrids) {
                        deleteMaskFromGroup(tiePointGrid.getOverlayMaskGroup(), mask);
                    }
                    ImageType imageType = mask.getImageType();
                    if (imageType  == Mask.VectorDataType.INSTANCE) {
                        VectorDataNode vectorDataNode = Mask.VectorDataType.getVectorData(mask);
                        product.getVectorDataGroup().remove(vectorDataNode);
                    }
                } else if (raster instanceof Band) {
                    product.removeBand((Band) raster);
                } else if (raster instanceof TiePointGrid) {
                    product.removeTiePointGrid((TiePointGrid) raster);
                }
            }
        }
    }
    
    private static String formatPromptMessage(String description, ProductNode[] nodes, 
                                              Collection<RasterDataNode> virtualBands, Collection<RasterDataNode> validMaskNodes,
                                              Collection<RasterDataNode> masks) {
        
        
        String name;
        StringBuilder message = new StringBuilder();
        if ((nodes.length>1)) {
            message.append(MessageFormat.format("Do you really want to delete the following {0}:\n", description));
            for (ProductNode node : nodes) {
                message.append(INDENT);
                message.append(node.getName());
                message.append("\n");
            }
        } else {
            name = nodes[0].getName();
            message.append(MessageFormat.format("Do you really want to delete the {0} ''{1}''?\n", description, name));
        }
        message.append("This action cannot be undone.\n\n");
        
        if (!virtualBands.isEmpty()
                || !validMaskNodes.isEmpty()
                || !masks.isEmpty()) {
            if ((nodes.length>1)) {
                message.append(MessageFormat.format("The {0} to be deleted are referenced by\n", description));
            } else {
                message.append(MessageFormat.format("The {0} to be deleted is referenced by\n", description));
            }
        }
        if (!virtualBands.isEmpty()) {
            message.append("the expression of virtual band(s):\n");
            for (RasterDataNode virtualBand : virtualBands) {
                message.append(INDENT);
                message.append(virtualBand.getName());
                message.append("\n");
            }
        }
        if (!validMaskNodes.isEmpty()) {
            message.append("the valid-mask expression of band(s) or tie-point grid(s)\n");
            for (RasterDataNode validMaskNode : validMaskNodes) {
                message.append(INDENT);
                message.append(validMaskNode.getName());
                message.append("\n");
            }
        }
        if (!masks.isEmpty()) {
            message.append("the mask(s):\n");
            for (RasterDataNode mask : masks) {
                message.append(INDENT);
                message.append(mask.getName());
                message.append("\n");
            }
        }
        return message.toString();
    }
    
    private static String getTypeName(RasterDataNode[] rasters) {
        String description = "";
        if (rasters[0] instanceof Mask) {
            description = "mask";
        } else if (rasters[0] instanceof Band) {
            description = "band";
        } else if (rasters[0] instanceof TiePointGrid) {
            description = "tie-point grid";
        }
        if (rasters.length>1) {
            description += "s";
        }
        return description;
    }
    
    private static void deleteMaskFromGroup(ProductNodeGroup<Mask> group, Mask mask) {
        if (group.contains(mask)) {
            group.remove(mask);
        }
    }

    private static List<RasterDataNode> getReferencedValidMasks(final RasterDataNode node) {
        final Product product = node.getProduct();
        final List<RasterDataNode> rasterList = new ArrayList<RasterDataNode>();
        if (product != null) {
            for (int i = 0; i < product.getNumBands(); i++) {
                final Band band = product.getBandAt(i);
                if (band != node) {
                    if (isNodeReferencedByExpression(node, band.getValidPixelExpression())) {
                        rasterList.add(band);
                    }
                }
            }
            for (int i = 0; i < product.getNumTiePointGrids(); i++) {
                final TiePointGrid tiePointGrid = product.getTiePointGridAt(i);
                if (tiePointGrid != node) {
                    if (isNodeReferencedByExpression(node, tiePointGrid.getValidPixelExpression())) {
                        rasterList.add(tiePointGrid);
                    }
                }
            }
        }
        return rasterList;
    }

    private static List<RasterDataNode> getReferencedMasks(final RasterDataNode node) {
        final Product product = node.getProduct();
        final List<RasterDataNode> rasterList = new ArrayList<RasterDataNode>();
        if (product != null) {
            final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
            final Mask[] masks = maskGroup.toArray(new Mask[maskGroup.getNodeCount()]);
            for (final Mask mask : masks) {
                final String expression;
                if (mask.getImageType() == Mask.BandMathsType.INSTANCE) {
                    expression = Mask.BandMathsType.getExpression(mask);
                } else if (mask.getImageType() == Mask.RangeType.INSTANCE) {
                    expression = Mask.RangeType.getRasterName(mask);
                } else {
                    expression = null;
                }
                if (isNodeReferencedByExpression(node, expression)) {
                    rasterList.add(mask);
                }
            }
        }
        return rasterList;
    }

    private static List<RasterDataNode> getReferencedVirtualBands(final RasterDataNode node) {
        final Product product = node.getProduct();
        final List<RasterDataNode> rasterList = new ArrayList<RasterDataNode>();
        if (product != null) {
            for (int i = 0; i < product.getNumBands(); i++) {
                final Band band = product.getBandAt(i);
                if (band instanceof VirtualBand) {
                    final VirtualBand virtualBand = (VirtualBand) band;
                    if (isNodeReferencedByExpression(node, virtualBand.getExpression())) {
                        rasterList.add(virtualBand);
                    }
                }
            }
        }
        return rasterList;
    }

    @SuppressWarnings({"SimplifiableIfStatement"})
    private static boolean isNodeReferencedByExpression(RasterDataNode node, String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }

        return expression.matches(".*\\b" + node.getName() + "\\b.*");
    }
}
