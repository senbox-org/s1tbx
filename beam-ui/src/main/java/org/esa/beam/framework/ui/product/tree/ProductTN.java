package org.esa.beam.framework.ui.product.tree;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.VectorDataNode;

import java.util.HashMap;

class ProductTN extends AbstractTN {
    private static final String METADATA = "Metadata";
    private static final String BANDS = "Bands";
    private static final String VECTOR_DATA = "Geometries";
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
                return new ProductNodeTN(TIE_POINT_GRIDS, product.getTiePointGridGroup(), this);
            }
        }
        if (hasVectorData(product)) {
            childIndex++;
            if (childIndex == index) {
                return new VectorDataGroupTN(ProductTN.VECTOR_DATA, this.product.getVectorDataGroup(), this);
            }
        }
        if (hasBands(product)) {
            childIndex++;
            if (childIndex == index) {
                String[] groupNames = product.getBandSubGroupPaths();
                boolean autoGrouping = groupNames != null;
                if (autoGrouping) {
                    return new ProductNodeTN(BANDS, group(product.getBandGroup(), groupNames), this);
                } else {
                    return new ProductNodeTN(BANDS, product.getBandGroup(), this);
                }
            }
        }

        throw new IndexOutOfBoundsException(String.format("No child for index <%d>.", index));
    }

    private ProductNode group(ProductNodeGroup<Band> bandGroup, String[] groupNames) {

        HashMap<String, ProductNodeGroup<Band>> subGroupMap = new HashMap<String, ProductNodeGroup<Band>>();

        final ProductNodeGroup newGroup = new ProductNodeGroup(null, bandGroup.getName(), false);
        newGroup.setDescription(bandGroup.getDescription());

        final int count = bandGroup.getNodeCount();
        for (int i = 0; i < count; i++) {
            final Band band = bandGroup.get(i);
            final String bandName = band.getName();
            String subGroupName = getGroupName(groupNames, bandName);
            if (subGroupName != null) {
                ProductNodeGroup<Band> subGroup = subGroupMap.get(subGroupName);
                if (subGroup == null) {
                    subGroup = new ProductNodeGroup<Band>(null, subGroupName, false);
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

    private String getGroupName(String[] groupNames, String bandName) {
        for (String groupName : groupNames) {
            if (bandName.length() > groupName.length()
                    && bandName.startsWith(groupName)
                    && "_ .-".indexOf(bandName.charAt(groupName.length())) >= 0) {
                return groupName;
            }
        }
        return null;
    }


    @Override
    public int getChildCount() {
        int childCount = 0;
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
        if (hasVectorData(product)) {
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
        if (hasVectorData(product)) {
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

    private boolean hasBands(Product product) {
        return product.getBandGroup().getNodeCount() > 0;
    }

    private boolean hasTiePoints(Product product) {
        return product.getTiePointGridGroup().getNodeCount() > 0;
    }

    private boolean hasVectorData(Product product) {
        final ProductNodeGroup<VectorDataNode> vectorNodeGroup = product.getVectorDataGroup();
        for (int i = 0; i < vectorNodeGroup.getNodeCount(); i++) {
            final VectorDataNode vectorDataNode = vectorNodeGroup.get(i);
            if (!vectorDataNode.isInternalNode() ||
                    (vectorDataNode.isInternalNode() && !vectorDataNode.getFeatureCollection().isEmpty())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasIndexCoding(Product product) {
        return product.getIndexCodingGroup().getNodeCount() > 0;
    }

    private boolean hasFlagCoding(Product product) {
        return product.getFlagCodingGroup().getNodeCount() > 0;
    }

    private boolean hasMetadata(Product product) {
        return product.getMetadataRoot().getNumElements() > 0 ||
                product.getMetadataRoot().getNumAttributes() > 0;
    }

}
