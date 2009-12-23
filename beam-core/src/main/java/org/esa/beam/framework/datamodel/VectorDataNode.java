package org.esa.beam.framework.datamodel;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.geotools.feature.CollectionEvent;
import org.geotools.feature.CollectionListener;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

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
    public static final String PROPERTY_NAME_FEATURE = "feature";

    private static final String DEFAULT_STYLE_FORMAT = "fill:%s; fill-opacity:0.5; stroke:#ffffff; stroke-opacity:1.0; stroke-width:1.0";
    private static final String[] FILL_COLORS = {
            "#ff0000", // red
            "#00ff00", // green
            "#0000ff", // blue
            "#aaff00",
            "#00aaff",
            "#ffaa00",
            "#ff00aa",
            "#aa00ff",
            "#00ffaa",
    };
    static int fillColorIndex;


    private final SimpleFeatureType featureType;
    private final FeatureCollection featureCollection;
    private final CollectionListener featureCollectionListener;
    private String defaultCSS;

    /**
     * Constructs a new vector data node for the given feature collection.
     *
     * @param name        The node name.
     * @param featureType The feature type.
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
     * @throws IllegalArgumentException if the given name is not a valid node identifier
     */
    public VectorDataNode(String name, FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection) {
        super(name, "");
        this.featureType = featureCollection.getSchema();
        this.featureCollection = featureCollection;
        this.featureCollectionListener = new CollectionListener() {
            @Override
            public void collectionChanged(CollectionEvent tce) {
                if (tce.getEventType() == CollectionEvent.FEATURES_ADDED) {
                    fireFeatureCollectionChanged(null, tce.getFeatures());
                } else if (tce.getEventType() == CollectionEvent.FEATURES_REMOVED) {
                    fireFeatureCollectionChanged(tce.getFeatures(), null);
                } else if (tce.getEventType() == CollectionEvent.FEATURES_CHANGED) {
                    fireFeatureCollectionChanged(tce.getFeatures(), tce.getFeatures());
                }
            }
        };
        this.featureCollection.addListener(featureCollectionListener);
        this.defaultCSS = String.format(DEFAULT_STYLE_FORMAT, FILL_COLORS[(fillColorIndex++) % FILL_COLORS.length]);
    }

    /**
     * Informs clients which have registered a {@link ProductNodeListener}
     * with the {@link Product} containing this {@link VectorDataNode}, that one or more underlying
     * OpenGIS {@code SimpleFeature}s have changed.
     * <p/>
     * The method fires a product node property change event. The property name is always
     * {@link #PROPERTY_NAME_FEATURE_COLLECTION}. The following conventions apply for
     * the {@code oldValue} and {@code newValue} fields of the fired
     * {@link ProductNodeEvent}:
     * <ol>
     * <li>Features added: {@code oldValue} is {@code null}, {@code newValue} is the array containing all added features.</li>
     * <li>Features removed: {@code oldValue} is the array containing all removed features, {@code newValue} is {@code null}.</li>
     * <li>Features changed: {@code oldValue} is same as {@code newValue} which is the array containing all changed features.</li>
     * </ol>
     *
     * @param oldFeatures The {@code oldValue} of the node event to be fired.
     * @param newFeatures The {@code newValue} of the node event to be fired.
     */
    public void fireFeatureCollectionChanged(SimpleFeature[] oldFeatures, SimpleFeature[] newFeatures) {
        fireProductNodeChanged(PROPERTY_NAME_FEATURE_COLLECTION, oldFeatures, newFeatures);
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
    public FeatureCollection getFeatureCollection() {
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
