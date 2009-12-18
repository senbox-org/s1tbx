package org.esa.beam.framework.datamodel;

import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.geotools.feature.CollectionEvent;
import org.geotools.feature.CollectionListener;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import com.bc.ceres.core.Assert;

/**
 * A container which allows to store vector data in the BEAM product model.
 * <p/>
 * This is a preliminary API under construction for BEAM 4.7. Not intended for public use.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see Product#getVectorDataGroup()
 * @since BEAM 4.7
 */
public class VectorDataNode extends ProductNode {

    public static final String PROPERTY_NAME_FEATURE_COLLECTION = "featureCollection";

    private static final String DEFAULT_STYLE_FORMAT = "fill:%s; fill-opacity:0.5; stroke:#ffffff; stroke-opacity:1.0; stroke-width:1.0";
    private static final String[] FILL_COLORS = {
            "#ff0000", // red
            "#00ff00", // green
            "#0000ff", // blue
            "#00aaff",
            "#aa00ff",
            "#ffaa00",
            "#ff00aa",
            "#aaff00",
            "#00ffaa",
    };
    static int fillColorIndex;


    private final SimpleFeatureType featureType;
    private final FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection;
    private final CollectionListener featureCollectionListener;
    private String defaultCSS;

    /**
     * Constructs a new vector data node for the given feature collection.
     *
     * @param name        The node name.
     * @param featureType The feature type.
     *
     * @throws IllegalArgumentException if the given name is not a valid node identifier
     */
    public VectorDataNode(String name, SimpleFeatureType featureType) {
        this(name, new DefaultFeatureCollection(name, featureType));
    }

    /**
     * Constructs a new vector data node for the given feature collection.
     *
     * @param name              The node name.
     * @param featureCollection A feature collection.
     *
     * @throws IllegalArgumentException if the given name is not a valid node identifier
     */
    public VectorDataNode(String name, FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection) {
        super(name, "");
        this.featureType = featureCollection.getSchema();
        this.featureCollection = featureCollection;
        this.featureCollectionListener = new CollectionListener() {
            @Override
            public void collectionChanged(CollectionEvent tce) {
                fireFeatureCollectionChanged();
            }
        };
        this.featureCollection.addListener(featureCollectionListener);
        this.defaultCSS = String.format(DEFAULT_STYLE_FORMAT, FILL_COLORS[(fillColorIndex++) % FILL_COLORS.length]);
    }

    public void fireFeatureCollectionChanged() {
        System.out.println("VectorDataNode '" + getName() + "': fireProductNodeChanged");
        fireProductNodeChanged(PROPERTY_NAME_FEATURE_COLLECTION);
    }

    /**
     * @return The feature type (= feature source schema).
     */
    public SimpleFeatureType getFeatureType() {
        return featureType;
    }

    /**
     * @return The feature collection.
     */
    public FeatureCollection<SimpleFeatureType, SimpleFeature> getFeatureCollection() {
        return featureCollection;
    }

    @Override
    public long getRawStorageSize(ProductSubsetDef subsetDef) {
        // todo - estimate shapefile size (nf, 10.2009)
        return 0;
    }

    public String getDefaultCSS() {
        return defaultCSS;
    }

    public void setDefaultCSS(String defaultCSS) {
        Assert.notNull(defaultCSS, "defaultCSS");
        this.defaultCSS = defaultCSS;
    }

    @Override
    public void acceptVisitor(ProductVisitor visitor) {
        visitor.visit(this);
    }

    /**
     * Releases all of the resources used by this object instance and all of its owned children. Its primary use is to
     * allow the garbage collector to perform a vanilla job.
     * <p/>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>dispose()</code> are undefined.
     * <p/>
     * <p>Overrides of this method should always call <code>super.dispose();</code> after disposing this instance.
     */
    @Override
    public void dispose() {
        featureCollection.removeListener(featureCollectionListener);
        super.dispose();
    }
}
