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
package org.esa.snap.core.dataio;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.util.Guardian;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

public abstract class AbstractProductBuilder extends AbstractProductReader {

    protected Product sourceProduct;
    protected boolean sourceProductOwner;
    protected int sceneRasterWidth;
    protected int sceneRasterHeight;
    protected String newProductName;
    protected String newProductDesc;
    protected Map<Band, RasterDataNode> bandMap;

    public AbstractProductBuilder(final boolean sourceProductOwner) {
        super(null);
        bandMap = new Hashtable<Band, RasterDataNode>(16);
        this.sourceProductOwner = sourceProductOwner;
    }

    public Product getSourceProduct() {
        return sourceProduct;
    }

    public boolean isSourceProductOwner() {
        return sourceProductOwner;
    }

    public void setNewProductDesc(String newProductDesc) {
        this.newProductDesc = newProductDesc;
    }

    public void setNewProductName(String newProductName) {
        this.newProductName = newProductName;
    }

    public int getSceneRasterWidth() {
        return sceneRasterWidth;
    }

    public int getSceneRasterHeight() {
        return sceneRasterHeight;
    }

    protected Product readProductNodes(Product sourceProduct, ProductSubsetDef subsetDef, String name,
                                       String desc) throws IOException {
        Guardian.assertNotNull("sourceProduct", sourceProduct);
        setNewProductName(name != null ? name : sourceProduct.getName());
        setNewProductDesc(desc != null ? desc : sourceProduct.getDescription());
        final Product product = readProductNodes(sourceProduct, subsetDef);
        product.setModified(true);
        return product;
    }

    @Override
    protected abstract Product readProductNodesImpl() throws IOException;

    /**
     * Closes the access to all currently opened resources such as file input streams and all resources of this children
     * directly owned by this reader. Its primary use is to allow the garbage collector to perform a vanilla job.
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>close()</code> are undefined.
     * <p>Overrides of this method should always call <code>super.close();</code> after disposing this instance.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        disposeBandMap();
        if (sourceProductOwner && sourceProduct != null) {
            sourceProduct.dispose();
        }
        sourceProduct = null;
        super.close();
    }

    protected void addFlagCodingsToProduct(Product target) {
        ProductNodeGroup<FlagCoding> sourceGroup = getSourceProduct().getFlagCodingGroup();
        ProductNodeGroup<FlagCoding> targetGroup = target.getFlagCodingGroup();
        for (int i = 0; i < sourceGroup.getNodeCount(); i++) {
            FlagCoding flagCoding = sourceGroup.get(i);
            String flagCodingName = flagCoding.getName();
            if(targetGroup.get(flagCodingName) == null) {
                FlagCoding destFlagCoding = new FlagCoding(flagCodingName);
                destFlagCoding.setDescription(flagCoding.getDescription());
                cloneFlags(flagCoding, destFlagCoding);
                targetGroup.add(destFlagCoding);
            }
        }
    }

    protected void addIndexCodingsToProduct(Product target) {
        ProductNodeGroup<IndexCoding> indexCodingGroup = getSourceProduct().getIndexCodingGroup();
        ProductNodeGroup<IndexCoding> targetGroup = target.getIndexCodingGroup();
        for (int i = 0; i < indexCodingGroup.getNodeCount(); i++) {
            IndexCoding sourceIndexCoding = indexCodingGroup.get(i);
            String indexCodingName = sourceIndexCoding.getName();
            if (targetGroup.get(indexCodingName) == null) {
                IndexCoding destIndexCoding = new IndexCoding(indexCodingName);
                destIndexCoding.setDescription(sourceIndexCoding.getDescription());
                cloneIndexes(sourceIndexCoding, destIndexCoding);
                target.getIndexCodingGroup().add(destIndexCoding);
            }
        }
    }

    protected static void addAttribString(String name, String value, MetadataElement subsetElem) {
        ProductData data = ProductData.createInstance(value);
        subsetElem.addAttribute(new MetadataAttribute(name, data, true));
    }

    protected void cloneFlags(FlagCoding sourceFlagCoding, FlagCoding destFlagCoding) {
        cloneMetadataElementsAndAttributes(sourceFlagCoding, destFlagCoding, 1);
    }

    protected void cloneIndexes(IndexCoding sourceFlagCoding, IndexCoding destFlagCoding) {
        cloneMetadataElementsAndAttributes(sourceFlagCoding, destFlagCoding, 1);
    }

    protected void addMetadataToProduct(Product product) {
        cloneMetadataElementsAndAttributes(getSourceProduct().getMetadataRoot(), product.getMetadataRoot(), 0);
    }

    protected void cloneMetadataElementsAndAttributes(MetadataElement sourceRoot, MetadataElement destRoot, int level) {
        cloneMetadataElements(sourceRoot, destRoot, level);
        cloneMetadataAttributes(sourceRoot, destRoot);
    }

    protected void cloneMetadataElements(MetadataElement sourceRoot, MetadataElement destRoot, int level) {
        for (int i = 0; i < sourceRoot.getNumElements(); i++) {
            MetadataElement sourceElement = sourceRoot.getElementAt(i);
            if (level > 0 || isNodeAccepted(sourceElement.getName())) {
                MetadataElement element = new MetadataElement(sourceElement.getName());
                element.setDescription(sourceElement.getDescription());
                destRoot.addElement(element);
                cloneMetadataElementsAndAttributes(sourceElement, element, level + 1);
            }
        }
    }

    protected void cloneMetadataAttributes(MetadataElement sourceRoot, MetadataElement destRoot) {
        for (int i = 0; i < sourceRoot.getNumAttributes(); i++) {
            MetadataAttribute sourceAttribute = sourceRoot.getAttributeAt(i);
            destRoot.addAttribute(sourceAttribute.createDeepClone());
        }
    }

    @Override
    protected boolean isInstanceOfValidInputType(Object input) {
        return input instanceof Product;
    }

    protected void disposeBandMap() {
        bandMap.clear();
    }


}
