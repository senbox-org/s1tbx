/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.framework.ui.product.tree;

import org.esa.beam.framework.datamodel.*;

import java.util.HashMap;

class ProductTN extends AbstractTN {
    
    private static final String IDENTIFICATION = "Identification";
    private static final String METADATA = "Metadata";
    private static final String BANDS = "Bands";
    private static final String VECTOR_DATA = "Vector data";
    private static final String TIE_POINT_GRIDS = "Tie-point grids";
    private static final String FLAG_CODINGS = "Flag codings";
    private static final String INDEX_CODINGS = "Index codings";

    private Product product;

    ProductTN(Product product, AbstractTN parent) {
        super(product.getDisplayName(), product, parent);
        this.product = product;
    }

    public Product getProduct() {
        return product;
    }

    @Override
    public AbstractTN getChildAt(int index) {
        int childIndex = -1;
        if (hasIdentification(product)) {
            childIndex++;
            if (childIndex == index) {
                return new IdentificationTN(IDENTIFICATION, product.getMetadataRoot(), this);
            }
        }
        if (hasMetadata(product)) {
            childIndex++;
            if (childIndex == index) {
                return new MetadataTN(METADATA, product.getMetadataRoot(), this);
            }
        }
        if (hasFlagCoding(product)) {
            childIndex++;
            if (childIndex == index) {
                return new ProductNodeTN(FLAG_CODINGS, product.getFlagCodingGroup(), this);
            }
        }
        if (hasIndexCoding(product)) {
            childIndex++;
            if (childIndex == index) {
                return new ProductNodeTN(INDEX_CODINGS, product.getIndexCodingGroup(), this);
            }
        }
        if (hasTiePoints(product)) {
            childIndex++;
            if (childIndex == index) {
                final Product.AutoGrouping autoGrouping = product.getAutoGrouping();
                if (autoGrouping != null) {
                    return new ProductNodeTN(TIE_POINT_GRIDS, group(product.getTiePointGridGroup(), autoGrouping), this);
                } else {
                    return new ProductNodeTN(TIE_POINT_GRIDS, product.getTiePointGridGroup(), this);
                }
            }
        }
        if (mustShowVectorData(product)) {
            childIndex++;
            if (childIndex == index) {
                return new ProductNodeTN(ProductTN.VECTOR_DATA, this.product.getVectorDataGroup(), this);
            }
        }
        if (hasBands(product)) {
            childIndex++;
            if (childIndex == index) {
                final Product.AutoGrouping autoGrouping = product.getAutoGrouping();
                if (autoGrouping != null) {
                    return new ProductNodeTN(BANDS, group(product.getBandGroup(), autoGrouping), this);
                } else {
                    return new ProductNodeTN(BANDS, product.getBandGroup(), this);
                }
            }
        }

        throw new IndexOutOfBoundsException(String.format("No child for index <%d>.", index));
    }

    private static ProductNode group(ProductNodeGroup<? extends RasterDataNode> bandGroup, Product.AutoGrouping autoGrouping) {

        HashMap<String, ProductNodeGroup<ProductNode>> subGroupMap = new HashMap<String, ProductNodeGroup<ProductNode>>();

        ProductNodeGroup<ProductNode> newGroup = new ProductNodeGroup<ProductNode>(null, bandGroup.getName(), false);
        newGroup.setDescription(bandGroup.getDescription());

        final int count = bandGroup.getNodeCount();
        for (int i = 0; i < count; i++) {
            RasterDataNode band = bandGroup.get(i);
            String bandName = band.getName();
            int groupPathIndex = autoGrouping.indexOf(bandName);
            if (groupPathIndex >= 0) {
                // todo - this is still wrong, must support group separators ('/') for nested groups  (nf 20100622)
                String subGroupName = createGroupName(autoGrouping.get(groupPathIndex));
                ProductNodeGroup<ProductNode> subGroup = subGroupMap.get(subGroupName);
                if (subGroup == null) {
                    subGroup = new ProductNodeGroup<ProductNode>(null, subGroupName, false);
                    subGroupMap.put(subGroupName, subGroup);
                    newGroup.add(subGroup);
                }
                subGroup.add(band);
            } else {
                newGroup.add(band);
            }
        }
        return newGroup;
    }

    // todo - this is a workaround  (nf 20100622)
    private static String createGroupName(String[] groupPath) {
        if (groupPath.length == 1) {
            return groupPath[0];
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < groupPath.length; i++) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(groupPath[i]);

            }
            return sb.toString();
        }
    }

    @Override
    public int getChildCount() {
        int childCount = 0;
        if(hasIdentification(product)) {
            childCount++;
        }
        if (hasMetadata(product)) {
            childCount++;
        }
        if (hasFlagCoding(product)) {
            childCount++;
        }
        if (hasIndexCoding(product)) {
            childCount++;
        }
        if (hasTiePoints(product)) {
            childCount++;
        }
        if (mustShowVectorData(product)) {
            childCount++;
        }
        if (hasBands(product)) {
            childCount++;
        }
        return childCount;
    }


    @Override
    protected int getIndex(AbstractTN child) {
        int childIndex = -1;
        if (hasIdentification(product)) {
            childIndex++;
            if(child.getName().equals(IDENTIFICATION)) {
                return childIndex;
            }
        }
        if (hasMetadata(product)) {
            childIndex++;
            if (child.getName().equals(METADATA)) {
                return childIndex;
            }
        }
        if (hasFlagCoding(product)) {
            childIndex++;
            if (child.getName().equals(FLAG_CODINGS)) {
                return childIndex;
            }
        }
        if (hasIndexCoding(product)) {
            childIndex++;
            if (child.getName().equals(INDEX_CODINGS)) {
                return childIndex;
            }
        }
        if (hasTiePoints(product)) {
            childIndex++;
            if (child.getName().equals(TIE_POINT_GRIDS)) {
                return childIndex;
            }
        }
        if (mustShowVectorData(product)) {
            childIndex++;
            if (child.getName().equals(VECTOR_DATA)) {
                return childIndex;
            }
        }
        if (hasBands(product)) {
            childIndex++;
            if (child.getName().equals(BANDS)) {
                return childIndex;
            }
        }
        return childIndex;
    }

    private static boolean hasBands(Product product) {
        return product.getBandGroup().getNodeCount() > 0;
    }

    private static boolean hasTiePoints(Product product) {
        return product.getTiePointGridGroup().getNodeCount() > 0;
    }

    private static boolean mustShowVectorData(Product product) {

        final ProductNodeGroup<VectorDataNode> vectorNodeGroup = product.getVectorDataGroup();
        VectorDataNode gcpVectorDataNode = product.getGcpGroup().getVectorDataNode();
        VectorDataNode pinVectorDataNode = product.getPinGroup().getVectorDataNode();
        for (int i = 0; i < vectorNodeGroup.getNodeCount(); i++) {
            final VectorDataNode vectorDataNode = vectorNodeGroup.get(i);
            boolean isPinOrGcpNode = vectorDataNode.equals(gcpVectorDataNode) || vectorDataNode.equals(pinVectorDataNode);
            if (isPinOrGcpNode) {
                if(vectorDataNode.getPlacemarkGroup().getNodeCount() > 0) {
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    private static boolean hasIndexCoding(Product product) {
        return product.getIndexCodingGroup().getNodeCount() > 0;
    }

    private static boolean hasFlagCoding(Product product) {
        return product.getFlagCodingGroup().getNodeCount() > 0;
    }

    private static boolean hasMetadata(Product product) {
        return (product.getMetadataRoot() != null && (product.getMetadataRoot().getNumElements() > 0 ||
                product.getMetadataRoot().getNumAttributes() > 0));
    }

    private static boolean hasIdentification(Product product) {
        return product.getMetadataRoot() != null && product.getMetadataRoot().getElement("Abstracted_Metadata") != null;
    }

}
