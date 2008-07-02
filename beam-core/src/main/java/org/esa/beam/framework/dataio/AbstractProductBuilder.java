/*
 * $id$
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.framework.dataio;

import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.Guardian;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

public abstract class AbstractProductBuilder extends AbstractProductReader {

    protected Product _sourceProduct;
    protected boolean _sourceProductOwner;
    protected int _sceneRasterWidth;
    protected int _sceneRasterHeight;
    protected String _newProductName;
    protected String _newProductDesc;
    protected Map<Band, RasterDataNode> _bandMap;

    public AbstractProductBuilder(final boolean sourceProductOwner) {
        super(null);
        _bandMap = new Hashtable<Band, RasterDataNode>();
        _sourceProductOwner = sourceProductOwner;
    }

    public Product getSourceProduct() {
        return _sourceProduct;
    }

    public boolean isSourceProductOwner() {
        return _sourceProductOwner;
    }

    public void setNewProductDesc(String newProductDesc) {
        _newProductDesc = newProductDesc;
    }

    public void setNewProductName(String newProductName) {
        _newProductName = newProductName;
    }

    public int getSceneRasterWidth() {
        return _sceneRasterWidth;
    }

    public int getSceneRasterHeight() {
        return _sceneRasterHeight;
    }

    protected Product readProductNodes(Product sourceProduct, ProductSubsetDef subsetDef, String name,
                                       String desc) throws IOException {
        Guardian.assertNotNull("sourceProduct", sourceProduct);
        setNewProductName(name != null ? name : sourceProduct.getName());
        setNewProductDesc(desc != null ? desc : sourceProduct.getDescription());
        final Product product = readProductNodes(sourceProduct, subsetDef);
        if (sourceProduct.getQuicklookBandName() != null
                && product.getQuicklookBandName() == null
                && product.containsBand(sourceProduct.getQuicklookBandName())) {
            product.setQuicklookBandName(sourceProduct.getQuicklookBandName());
        }
        product.setModified(true);
        return product;
    }

    @Override
    protected abstract Product readProductNodesImpl() throws IOException;

    /**
     * Closes the access to all currently opened resources such as file input streams and all resources of this children
     * directly owned by this reader. Its primary use is to allow the garbage collector to perform a vanilla job.
     * <p/>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>close()</code> are undefined.
     * <p/>
     * <p>Overrides of this method should always call <code>super.close();</code> after disposing this instance.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        disposeBandMap();
        if (_sourceProductOwner && _sourceProduct != null) {
            _sourceProduct.dispose();
        }
        _sourceProduct = null;
        super.close();
    }

    protected void addFlagCodingsToProduct(Product product) {
        final ProductNodeGroup<FlagCoding> flagCodingGroup = getSourceProduct().getFlagCodingGroup();
        for (int i = 0; i < flagCodingGroup.getNodeCount(); i++) {
            FlagCoding sourceFlagCoding = flagCodingGroup.get(i);
            FlagCoding destFlagCoding = new FlagCoding(sourceFlagCoding.getName());
            destFlagCoding.setDescription(sourceFlagCoding.getDescription());
            cloneFlags(sourceFlagCoding, destFlagCoding);
            product.getFlagCodingGroup().add(destFlagCoding);
        }
    }

    protected void addIndexCodingsToProduct(Product product) {
        final ProductNodeGroup<IndexCoding> indexCodingGroup = getSourceProduct().getIndexCodingGroup();
        for (int i = 0; i < indexCodingGroup.getNodeCount(); i++) {
            IndexCoding sourceIndexCoding = indexCodingGroup.get(i);
            IndexCoding destIndexCoding = new IndexCoding(sourceIndexCoding.getName());
            destIndexCoding.setDescription(sourceIndexCoding.getDescription());
            cloneIndexes(sourceIndexCoding, destIndexCoding);
            product.getIndexCodingGroup().add(destIndexCoding);
        }
    }

    protected static void addAttribString(String name, String value, MetadataElement subsetElem) {
        ProductData data = ProductData.createInstance(value);
        subsetElem.addAttribute(new MetadataAttribute(name, data, true));
    }

    protected void addBitmaskDefsToProduct(Product product) {
        for (int i = 0; i < getSourceProduct().getNumBitmaskDefs(); i++) {
            BitmaskDef bitmaskDef = getSourceProduct().getBitmaskDefAt(i);
            if (product.isCompatibleBitmaskDef(bitmaskDef)) {
                product.addBitmaskDef(bitmaskDef.createCopy());
            }
        }
    }

    protected void addBitmaskOverlayInfosToBandAndTiePointGrids(final Product product) {
        copyBitmaskOverlayInfo(getSourceProduct().getBands(), product);
        copyBitmaskOverlayInfo(getSourceProduct().getTiePointGrids(), product);
    }

    private static void copyBitmaskOverlayInfo(final RasterDataNode[] sourceNodes, final Product product) {
        for (final RasterDataNode sourceNode : sourceNodes) {
            final RasterDataNode destNode = product.getRasterDataNode(sourceNode.getName());
            if (destNode != null) {
                final BitmaskOverlayInfo bitmaskOverlayInfo = sourceNode.getBitmaskOverlayInfo();
                if (bitmaskOverlayInfo != null) {
                    final BitmaskOverlayInfo info = new BitmaskOverlayInfo();
                    final BitmaskDef[] bitmaskDefs = bitmaskOverlayInfo.getBitmaskDefs();
                    for (BitmaskDef bitmaskDef : bitmaskDefs) {
                        info.addBitmaskDef(product.getBitmaskDef(bitmaskDef.getName()));
                    }
                    destNode.setBitmaskOverlayInfo(info);
                }
            }
        }
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
        _bandMap.clear();
    }


}
