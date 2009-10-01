package org.esa.beam.framework.datamodel;

import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;

/**
 * A
 * This is a preliminary API under construction. Don't use.
 * TODO:
 * <ol>
 *   <li>Add add/remove/get API to {@link Product}</li>
 *   <li>Discuss ways to make it persistent (e.g. path to shapefile in BEAM-DIMAP)</li>
 * </ol>
 *
 * @author Norman Fomferra
 * @since BEAM 4.7
 */
public class VectorDataNode extends ProductNode {
    private final FeatureSource featureSource;

    /**
     * Constructs a new vector data node with the given name.
     *
     * @param featureSource a feature source
     * @throws IllegalArgumentException if the given name is not a valid node identifier
     */
    public VectorDataNode(FeatureSource featureSource) {
        super(featureSource.getName().toString(),
              featureSource.getInfo().getDescription());
        this.featureSource = featureSource;
    }

    public FeatureSource getFeatureSource() {
        return featureSource;
    }

    public FeatureStore getFeatureStore() {
        if (featureSource instanceof FeatureStore) {
            return (FeatureStore) featureSource;
        }
        return null;
    }

    /**
     * Gets an estimated, raw storage size in bytes of this product node.
     *
     * @param subsetDef if not <code>null</code> the subset may limit the size returned
     * @return the size in bytes.
     */
    @Override
    public long getRawStorageSize(ProductSubsetDef subsetDef) {
        return 0;
    }

    /**
     * Accepts the given visitor. This method implements the well known 'Visitor' design pattern of the gang-of-four.
     * The visitor pattern allows to define new operations on the product data model without the need to add more code
     * to it. The new operation is implemented by the visitor.
     *
     * @param visitor the visitor
     */
    @Override
    public void acceptVisitor(ProductVisitor visitor) {
    }
}
