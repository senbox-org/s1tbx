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
package org.esa.snap.core.datamodel;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import com.bc.ceres.glevel.support.GenericMultiLevelSource;
import com.bc.ceres.jai.operator.InterpretationType;
import com.bc.ceres.jai.operator.ReinterpretDescriptor;
import com.bc.ceres.jai.operator.ScalingType;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductWriter;
import org.esa.snap.core.dataop.barithm.BandArithmetic;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.image.SingleBandedOpImage;
import org.esa.snap.core.transform.MathTransform2D;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.ObjectUtils;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.jai.SingleBandedSampleModel;
import org.esa.snap.core.util.math.Histogram;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.core.util.math.Quantizer;
import org.esa.snap.core.util.math.Range;
import org.esa.snap.runtime.Config;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.NoninvertibleTransformException;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.prefs.BackingStoreException;

/**
 * The <code>RasterDataNode</code> class ist the abstract base class for all objects in the product package that contain
 * rasterized data. i.e. <code>Band</code> and <code>TiePointGrid</code>. It unifies the access to raster data in the
 * product model. A raster is considered as a rectangular raw data array with a fixed width and height. A raster data
 * node can scale its raw raster data samples in order to return geophysically meaningful pixel values.
 *
 * @author Norman Fomferra
 * @see #getRasterData()
 * @see #getRasterWidth()
 * @see #getRasterHeight()
 * @see #isScalingApplied()
 * @see #isLog10Scaled()
 * @see #getScalingFactor()
 * @see #getScalingOffset()
 */
public abstract class RasterDataNode extends DataNode implements Scaling, SceneTransformProvider {

    public static final String PROPERTY_NAME_IMAGE_INFO = "imageInfo";
    public static final String PROPERTY_NAME_LOG_10_SCALED = "log10Scaled";
    public static final String PROPERTY_NAME_SCALING_FACTOR = "scalingFactor";
    public static final String PROPERTY_NAME_SCALING_OFFSET = "scalingOffset";
    public static final String PROPERTY_NAME_NO_DATA_VALUE = "noDataValue";
    public static final String PROPERTY_NAME_NO_DATA_VALUE_USED = "noDataValueUsed";
    public static final String PROPERTY_NAME_VALID_PIXEL_EXPRESSION = "validPixelExpression";
    public static final String PROPERTY_NAME_GEO_CODING = "geoCoding";
    public static final String PROPERTY_NAME_TIME_CODING = "timeCoding";
    public static final String PROPERTY_NAME_STX = "stx";
    public static final String PROPERTY_NAME_ANCILLARY_VARIABLES = "ancillaryVariables";
    public static final String PROPERTY_NAME_ANCILLARY_RELATIONS = "ancillaryRelations";
    public static final String PROPERTY_NAME_IMAGE_TO_MODEL_TRANSFORM = "imageToModelTransform";
    public static final String PROPERTY_NAME_MODEL_TO_SCENE_TRANSFORM = "modelToSceneTransform";
    public static final String PROPERTY_NAME_SCENE_TO_MODEL_TRANSFORM = "sceneToModelTransform";


    /**
     * Number of bytes used for internal read buffer.
     */
    private static final int READ_BUFFER_MAX_SIZE = 8 * 1024 * 1024; // 8 MB

    /**
     * Text returned by the <code>{@link #getPixelString(int, int)}</code> method if no data is available at the given pixel
     * position.
     */
    public static final String NO_DATA_TEXT = "NaN"; /*I18N*/
    /**
     * Text returned by the <code>{@link #getPixelString(int, int)}</code> method if no data is available at the given pixel
     * position.
     */
    public static final String INVALID_POS_TEXT = "Invalid pos."; /*I18N*/
    /**
     * Text returned by the <code>{@link #getPixelString(int, int)}</code> method if an I/O error occurred while pixel data was
     * reloaded.
     */
    public static final String IO_ERROR_TEXT = "I/O error"; /*I18N*/


    private double scalingFactor;
    private double scalingOffset;
    private boolean log10Scaled;
    private boolean scalingApplied;

    private boolean noDataValueUsed;
    private ProductData noData;
    private double geophysicalNoDataValue; // invariant, depending on _noData
    private String validPixelExpression;

    private GeoCoding geoCoding;
    private TimeCoding timeCoding;

    private AffineTransform imageToModelTransform;
    private MathTransform2D modelToSceneTransform;
    private MathTransform2D sceneToModelTransform;

    private Stx stx;

    private ImageInfo imageInfo;

    private final ProductNodeGroup<Mask> overlayMasks;

    private Pointing pointing;

    private MultiLevelImage sourceImage;
    private MultiLevelImage geophysicalImage;
    private MultiLevelImage validMaskImage;

    private ROI validMaskROI;

    private ProductNodeGroup<RasterDataNode> ancillaryVariables;


    private String[] ancillaryRelations;
    private AncillaryBandRemover ancillaryBandRemover;

    /**
     * Constructs an object of type <code>RasterDataNode</code>.
     *
     * @param name     the name of the new object
     * @param dataType the data type used by the raster, must be one of the multiple <code>ProductData.TYPE_<i>X</i></code>
     *                 constants, with the exception of <code>ProductData.TYPE_UINT32</code>
     * @param numElems the number of elements in this data node.
     */
    protected RasterDataNode(String name, int dataType, long numElems) {
        super(name, dataType, numElems);
        if (dataType != ProductData.TYPE_INT8
            && dataType != ProductData.TYPE_INT16
            && dataType != ProductData.TYPE_INT32
            && dataType != ProductData.TYPE_UINT8
            && dataType != ProductData.TYPE_UINT16
            && dataType != ProductData.TYPE_UINT32
            && dataType != ProductData.TYPE_FLOAT32
            && dataType != ProductData.TYPE_FLOAT64) {
            throw new IllegalArgumentException("dataType is invalid");
        }

        scalingFactor = 1.0;
        scalingOffset = 0.0;
        log10Scaled = false;
        scalingApplied = false;

        noData = null;
        noDataValueUsed = false;
        geophysicalNoDataValue = 0.0;
        validPixelExpression = null;

        imageToModelTransform = null;
        modelToSceneTransform = MathTransform2D.IDENTITY;
        sceneToModelTransform = MathTransform2D.IDENTITY;

        overlayMasks = new ProductNodeGroup<>(this, "overlayMasks", false);
    }

    /**
     * @return The native width of the raster in pixels.
     */
    public abstract int getRasterWidth();

    /**
     * @return The native height of the raster in pixels.
     */
    public abstract int getRasterHeight();

    /**
     * @return The native size of the raster in pixels.
     */
    public Dimension getRasterSize() {
        return new Dimension(getRasterWidth(), getRasterHeight());
    }


    @Override
    public void setModified(boolean modified) {
        boolean oldState = isModified();
        if (oldState != modified) {
            if (!modified && overlayMasks != null) {
                overlayMasks.setModified(false);
            }
            super.setModified(modified);
        }
    }

    /**
     * Gets the transformation used to convert this raster's image (pixel) coordinates to model coordinates
     * used for rendering the image together with other images and vector data.
     * <p>
     * If this raster data node's ({@link #isSourceImageSet() source image is set})
     * the {@link MultiLevelModel#getImageToModelTransform(int) image-to-model transformation} of the image pyramid's
     * lowest level image is returned.
     * Otherwise the transformation which has been set using the {@link #setImageToModelTransform(AffineTransform)}
     * is returned.
     * If the transformation was not set explicitly, the method tries to determine it from geo-codings.
     * If this fails, the identity transform is returned.
     *
     * @return The image-to-model transformation.
     * @see #getProduct()
     * @see Product#getSceneCRS()
     * @see #setImageToModelTransform(AffineTransform)
     * @see #getSourceImage()
     * @see #getGeoCoding()
     * @since SNAP 2.0
     */
    public AffineTransform getImageToModelTransform() {
        // If a source image is already set, we must return the actual image-to-model transformation in use
        if (isSourceImageSet()) {
            return getSourceImage().getModel().getImageToModelTransform(0);
        }
        // If image-to-model transformation is explicitly set, return it
        if (imageToModelTransform != null) {
            return new AffineTransform(imageToModelTransform);
        }
        // Try to derive from source product
        Product product = getProduct();
        if (product != null) {
            CoordinateReferenceSystem sceneCRS = product.getSceneCRS();
            GeoCoding sceneGeoCoding = product.getSceneGeoCoding();
            GeoCoding rasterGeoCoding = getGeoCoding();
            CoordinateReferenceSystem appropriateSceneCRS = Product.findModelCRS(rasterGeoCoding);
            if (sceneCRS.equals(appropriateSceneCRS)) {
                // If both model CRS are equal
                return Product.findImageToModelTransform(rasterGeoCoding);
            }
            if (sceneGeoCoding == null && rasterGeoCoding == null) {
                // Fallback: identity transform, works fine for (single-size) products without geo-coding
                return new AffineTransform();
            }
        }
        // Fallback: avoid returning null
        return new AffineTransform();
    }

    /**
     * Sets the transformation used to convert this raster's image (pixel) coordinates to model coordinates
     * used for rendering the image together with other images and vector data.
     * <p>
     * The method call will fail if this raster data node has already a source image
     * which uses a different image-to-model transformation.
     * <p>
     * <i>WARNING: This method belongs to a preliminary API and may change in an incompatible
     * way or may even be removed in a next SNAP release.</i>
     *
     * @param imageToModelTransform The new image-to-model transformation
     * @throws IllegalStateException If a source image is already set which uses a different image-to-model transformation.
     * @see #getImageToModelTransform()
     * @see #createSourceImage()
     * @since SNAP 2.0
     */
    public void setImageToModelTransform(AffineTransform imageToModelTransform) {
        Assert.notNull(imageToModelTransform, "imageToModelTransform");
        AffineTransform imageToModelTransformOld = getImageToModelTransform();
        if (!imageToModelTransformOld.equals(imageToModelTransform)) {
            if (isSourceImageSet()) {
                throw new IllegalStateException("sourceImage already set, imageToModelTransform is now read-only");
            }
            this.imageToModelTransform = new AffineTransform(imageToModelTransform);
            fireProductNodeChanged(PROPERTY_NAME_IMAGE_TO_MODEL_TRANSFORM, imageToModelTransformOld, imageToModelTransform);
        }
    }

    /**
     * Gets the transformation that transforms from local {@link RasterDataNode} model coordinates
     * to the {@link Product}'s scene coordinates.
     * <p>
     * If no model-to-scene transformation has been explicitly set but a scene-to-model transformation
     * exists, then the latter's {@code inverse()} is returned.
     *
     * @return The model-to-scene transformation, or {@code null} if no such exists.
     * @see Product#getSceneCRS()
     * @see #getImageToModelTransform()
     * @since SNAP 2.0.3
     */
    public MathTransform2D getModelToSceneTransform() {
        if (modelToSceneTransform == MathTransform2D.IDENTITY && sceneToModelTransform != MathTransform2D.IDENTITY) {
            try {
                return sceneToModelTransform.inverse();
            } catch (NoninvertibleTransformException e) {
                return MathTransform2D.NULL;
            }
        }
        return modelToSceneTransform;
    }

    /**
     * Sets a {@code MathTransform2D} from the local model CRS of this {@code RasterDataNode} to
     * a {@code Product}'s scene coordinate reference system. If the sceneToModelTransform is not the inverse
     * of this transform or cannot be derived from it, it must be set using {@code setSceneToModelTransform()}.
     *
     * Only use this method when you know that the model CRS of this {@code RasterDataNode} is different
     * than the {@code Product}'s scene CRS or when you want to model a special relationship between
     * different {@code RasterDataNode}'s. When no transformation from local model to scene is possible, use
     * {@link MathTransform2D#NULL}.
     *
     * @param modelToSceneTransform The {@code MathTransform2D} to convert local model coordinates to
     *                              product scene coordinates
     * @see RasterDataNode#setSceneToModelTransform(MathTransform2D)
     * @since SNAP 2.0.3
     */
    public void setModelToSceneTransform(MathTransform2D modelToSceneTransform) {
        Assert.notNull(modelToSceneTransform, PROPERTY_NAME_MODEL_TO_SCENE_TRANSFORM);
        MathTransform2D oldTransform = this.modelToSceneTransform;
        this.modelToSceneTransform = modelToSceneTransform;
        fireProductNodeChanged(PROPERTY_NAME_MODEL_TO_SCENE_TRANSFORM, oldTransform, this.modelToSceneTransform);
    }

    /**
     * Gets the transformation that transforms from the {@link Product}'s scene coordinates
     * to the local {@link RasterDataNode} model coordinates.
     * <p>
     * If no scene-to-model transformation has been explicitly set but a model-to-scene transformation
     * exists, then the latter's {@code inverse()} is returned.
     *
     * @return The model-to-scene transformation
     * @see Product#getSceneCRS()
     * @see #getImageToModelTransform()
     * @since SNAP 2.0.3
     */
    public MathTransform2D getSceneToModelTransform() {
        if (sceneToModelTransform == MathTransform2D.IDENTITY && modelToSceneTransform != MathTransform2D.IDENTITY) {
            try {
                return modelToSceneTransform.inverse();
            } catch (NoninvertibleTransformException e) {
                return MathTransform2D.NULL;
            }
        }
        return sceneToModelTransform;
    }

    /**
     * Sets a {@code MathTransform2D} from a {@code Product}'s scene coordinate reference system to
     * the local model CRS of this {@code RasterDataNode}. If the modelToSceneTransform is not the inverse
     * of this transformor cannot be derived from it, it must be set using {@code setModelToSceneTransform()}.
     *
     * Only use this method when you know that the model CRS of this {@code RasterDataNode} is different
     * than the {@code Product}'s scene CRS or when you want to model a special relationship between
     * different {@code RasterDataNode}'s. When no transformation from scene to local model is possible, use
     * {@link MathTransform2D#NULL}.
     *
     * @param sceneToModelTransform The {@code MathTransform2D} to convert product scene coordinates
     *                              to local model coordinates
     * @see RasterDataNode#setModelToSceneTransform(MathTransform2D)
     * @since SNAP 2.0.3
     */
    public void setSceneToModelTransform(MathTransform2D sceneToModelTransform) {
        Assert.notNull(sceneToModelTransform, PROPERTY_NAME_SCENE_TO_MODEL_TRANSFORM);
        MathTransform2D oldTransform = this.sceneToModelTransform;
        this.sceneToModelTransform = sceneToModelTransform;
        fireProductNodeChanged(PROPERTY_NAME_SCENE_TO_MODEL_TRANSFORM, oldTransform, this.sceneToModelTransform);
    }

    /**
     * Returns the geo-coding of this {@link RasterDataNode}.
     *
     * @return the geo-coding, or {@code null} if not available.
     */
    public GeoCoding getGeoCoding() {
        if (geoCoding == null) {
            final Product product = getProduct();
            if (product != null) {
                return product.getSceneGeoCoding();
            }
        }
        return geoCoding;
    }

    /**
     * Sets the geo-coding for this {@link RasterDataNode}.
     * Also sets the geo-coding of the parent {@link Product} if it has no geo-coding yet.
     * <p>On property change, the method calls {@link #fireProductNodeChanged(String)} with the property
     * name {@link #PROPERTY_NAME_GEO_CODING}.
     *
     * @param geoCoding the new geo-coding
     * @see Product#setSceneGeoCoding(GeoCoding)
     */
    public void setGeoCoding(final GeoCoding geoCoding) {
        if (!ObjectUtils.equalObjects(geoCoding, this.geoCoding)) {
            this.geoCoding = geoCoding;
            // If our product has no geo-coding yet, it is set to the current one, if any
            if (this.geoCoding != null) {
                final Product product = getProduct();
                if (product != null && product.getSceneGeoCoding() == null && product.getSceneRasterSize().equals(getRasterSize())) {
                    product.setSceneGeoCoding(this.geoCoding);
                }
            }
            fireProductNodeChanged(PROPERTY_NAME_GEO_CODING);
        }
    }

    /**
     * Gets the time-coding of this {@link RasterDataNode}.
     *
     * @return the time-coding, or {@code null} if not available.
     * @see Product#getSceneTimeCoding()
     * @since SNAP 2.0
     */
    public TimeCoding getTimeCoding() {
        return timeCoding;
    }


    /**
     * Sets the time-coding for this {@link RasterDataNode}.
     *
     * @param timeCoding the new time-coding
     * @see Product#setSceneTimeCoding(TimeCoding)
     * @since SNAP 2.0
     */
    public void setTimeCoding(final TimeCoding timeCoding) {
        if (!ObjectUtils.equalObjects(timeCoding, this.timeCoding)) {
            final TimeCoding oldValue = this.timeCoding;
            this.timeCoding = timeCoding;
            fireProductNodeChanged(PROPERTY_NAME_TIME_CODING, oldValue, timeCoding);
        }
    }

    /**
     * Creates a {@link Pointing} applicable for this raster.
     *
     * @return the pointing object, or null if a pointing is not available
     */
    protected Pointing createPointing() {
        if (getGeoCoding() == null || getProduct() == null) {
            return null;
        }
        final PointingFactory factory = getProduct().getPointingFactory();
        if (factory == null) {
            return null;
        }
        return factory.createPointing(this);
    }

    /**
     * Gets a {@link Pointing} if one is available for this raster.
     * The methods calls {@link #createPointing()} if a pointing has not been set so far or if its {@link GeoCoding} changed
     * since the last creation of this raster's {@link Pointing} instance.
     *
     * @return the pointing object, or null if a pointing is not available
     */
    public Pointing getPointing() {
        if (pointing == null || pointing.getGeoCoding() == getGeoCoding()) {
            pointing = createPointing();
        }
        return pointing;
    }

    /**
     * Tests if this raster data node can be orthorectified.
     *
     * @return true, if so
     */
    public boolean canBeOrthorectified() {
        final Pointing pointing = getPointing();
        return pointing != null && pointing.canGetViewDir();
    }

    /**
     * Returns <code>true</code> if the pixel data contained in this band is "naturally" a floating point number type.
     *
     * @return true, if so
     */
    @Override
    public boolean isFloatingPointType() {
        return scalingApplied || super.isFloatingPointType();
    }

    /**
     * Returns the geophysical data type of this <code>RasterDataNode</code>. The value returned is always one of the
     * <code>ProductData.TYPE_XXX</code> constants.
     *
     * @return the geophysical data type
     * @see ProductData
     * @see #isScalingApplied()
     */
    public int getGeophysicalDataType() {
        return ImageManager.getProductDataType(
                ReinterpretDescriptor.getTargetDataType(ImageManager.getDataBufferType(getDataType()),
                                                        getScalingFactor(),
                                                        getScalingOffset(),
                                                        getScalingType(),
                                                        getInterpretationType()));
    }

    /**
     * Gets the scaling factor which is applied to raw {@link ProductData}. The default value is
     * <code>1.0</code> (no factor).
     *
     * @return the scaling factor
     * @see #isScalingApplied()
     */
    public final double getScalingFactor() {
        return scalingFactor;
    }

    /**
     * Sets the scaling factor which is applied to raw {@link ProductData}.
     *
     * @param scalingFactor the scaling factor
     * @see #isScalingApplied()
     */
    public final void setScalingFactor(double scalingFactor) {
        if (this.scalingFactor != scalingFactor) {
            this.scalingFactor = scalingFactor;
            setScalingApplied();
            resetGeophysicalImage();
            fireProductNodeChanged(PROPERTY_NAME_SCALING_FACTOR);
            setGeophysicalNoDataValue();
            resetValidMask();
            setModified(true);
        }
    }

    /**
     * Gets the scaling offset which is applied to raw {@link ProductData}. The default value is
     * <code>0.0</code> (no offset).
     *
     * @return the scaling offset
     * @see #isScalingApplied()
     */
    public final double getScalingOffset() {
        return scalingOffset;
    }

    /**
     * Sets the scaling offset which is applied to raw {@link ProductData}.
     *
     * @param scalingOffset the scaling offset
     * @see #isScalingApplied()
     */
    public final void setScalingOffset(double scalingOffset) {
        if (this.scalingOffset != scalingOffset) {
            this.scalingOffset = scalingOffset;
            setScalingApplied();
            resetGeophysicalImage();
            fireProductNodeChanged(PROPERTY_NAME_SCALING_OFFSET);
            setGeophysicalNoDataValue();
            resetValidMask();
            setModified(true);
        }
    }

    /**
     * Gets whether or not the {@link ProductData} of this band has a negative binomial distribution and
     * thus the common logarithm (base 10) of the values is stored in the raw data. The default value is
     * <code>false</code>.
     *
     * @return whether or not the data is logging-10 scaled
     * @see #isScalingApplied()
     */
    public final boolean isLog10Scaled() {
        return log10Scaled;
    }

    /**
     * Sets whether or not the {@link ProductData} of this band has a negative binomial distribution and
     * thus the common logarithm (base 10) of the values is stored in the raw data.
     *
     * @param log10Scaled whether or not the data is logging-10 scaled
     * @see #isScalingApplied()
     */
    public final void setLog10Scaled(boolean log10Scaled) {
        if (this.log10Scaled != log10Scaled) {
            this.log10Scaled = log10Scaled;
            setScalingApplied();
            resetGeophysicalImage();
            setGeophysicalNoDataValue();
            resetValidMask();
            fireProductNodeChanged(PROPERTY_NAME_LOG_10_SCALED);
            setModified(true);
        }
    }

    /**
     * Tests whether scaling of raw raster data values is applied before they are returned as geophysically meaningful
     * pixel values. <p>The methods which return geophysical pixel values are all {@link #getPixels(int, int, int, int, int[])},
     * {@link #setPixels(int, int, int, int, int[])}, {@link #readPixels(int, int, int, int, int[])} and
     * {@link #writePixels(int, int, int, int, int[])} methods as well as the <code>getPixel&lt;Type&gt;</code> and
     * <code>setPixel&lt;Type&gt;</code> methods such as  {@link #getPixelFloat(int, int)} * and
     * {@link #setPixelFloat(int, int, float)}.
     *
     * @return <code>true</code> if a conversion is applyied to raw data samples before the are retuned.
     * @see #getScalingOffset
     * @see #getScalingFactor
     * @see #isLog10Scaled
     */
    public final boolean isScalingApplied() {
        return scalingApplied;
    }

    /**
     * Tests if the given name is the name of a property which is relevant for the computation of the valid mask.
     *
     * @param propertyName the  name to test
     * @return {@code true}, if so.
     * @since BEAM 4.2
     */
    public static boolean isValidMaskProperty(final String propertyName) {
        return PROPERTY_NAME_NO_DATA_VALUE.equals(propertyName)
                || PROPERTY_NAME_NO_DATA_VALUE_USED.equals(propertyName)
                || PROPERTY_NAME_VALID_PIXEL_EXPRESSION.equals(propertyName)
                || PROPERTY_NAME_DATA.equals(propertyName);
    }


    /**
     * Tests whether or not a no-data value has been specified. The no-data value is not-specified unless either
     * {@link #setNoDataValue(double)} or {@link #setGeophysicalNoDataValue(double)} is called.
     *
     * @return true, if so
     * @see #isNoDataValueUsed()
     * @see #setNoDataValue(double)
     */
    public boolean isNoDataValueSet() {
        return noData != null;
    }

    /**
     * Clears the no-data value, so that {@link #isNoDataValueSet()} will return <code>false</code>.
     */
    public void clearNoDataValue() {
        noData = null;
        setGeophysicalNoDataValue();
    }

    /**
     * Tests whether or not the no-data value is used.
     * <p>The no-data value is used to determine valid pixels. For more information
     * on valid pixels, please refer to the documentation of the {@link #isPixelValid(int, int, javax.media.jai.ROI)}
     * method.
     *
     * @return true, if so
     * @see #setNoDataValueUsed(boolean)
     * @see #isNoDataValueSet()
     */
    public boolean isNoDataValueUsed() {
        return noDataValueUsed;
    }

    /**
     * Sets whether or not the no-data value is used.
     * If the no-data value is enabled and the no-data value has not been set so far,
     * a default no-data value it is set with a value of to zero.
     * <p>The no-data value is used to determine valid pixels. For more information
     * on valid pixels, please refer to the documentation of the {@link #isPixelValid(int, int, javax.media.jai.ROI)}
     * method.
     * <p>On property change, the method calls {@link #fireProductNodeChanged(String)} with the property
     * name {@link #PROPERTY_NAME_NO_DATA_VALUE_USED}.
     *
     * @param noDataValueUsed true, if so
     * @see #isNoDataValueUsed()
     */
    public void setNoDataValueUsed(boolean noDataValueUsed) {
        if (this.noDataValueUsed != noDataValueUsed) {
            this.noDataValueUsed = noDataValueUsed;
            resetValidMask();
            setModified(true);
            fireProductNodeChanged(PROPERTY_NAME_NO_DATA_VALUE_USED);
            fireProductNodeDataChanged();
        }
    }

    /**
     * Gets the no-data value as a primitive <code>double</code>.
     * <p>Note that the value returned is NOT necessarily the same as the value returned by
     * {@link #getGeophysicalNoDataValue()} because no scaling is applied.
     * <p>The no-data value is used to determine valid pixels. For more information
     * on valid pixels, please refer to the documentation of the {@link #isPixelValid(int, int, javax.media.jai.ROI)}
     * method.
     * <p>The method returns <code>0.0</code>, if no no-data value has been specified so far.
     *
     * @return the no-data value. It is returned as a <code>double</code> in order to cover all other numeric types.
     * @see #setNoDataValue(double)
     * @see #isNoDataValueSet()
     * @see #isNoDataValueUsed()
     */
    public double getNoDataValue() {
        return isNoDataValueSet() ? noData.getElemDouble() : 0.0;
    }

    /**
     * Sets the no-data value as a primitive <code>double</code>.
     * <p>Note that the given value is related to the "raw", un-scaled raster data.
     * In order to set the geophysical, scaled no-data value use the method
     * {@link #setGeophysicalNoDataValue(double)}.
     * <p>The no-data value is used to determine valid pixels. For more information
     * on valid pixels, please refer to the documentation of the {@link #isPixelValid(int, int, javax.media.jai.ROI)}
     * method.
     * <p>On property change, the method calls {@link #fireProductNodeChanged(String)} with the property
     * name {@link #PROPERTY_NAME_NO_DATA_VALUE}.
     *
     * @param noDataValue the no-data value. It is passed as a <code>double</code> in order to cover all other numeric types.
     * @see #getNoDataValue()
     * @see #isNoDataValueSet()
     */
    public void setNoDataValue(final double noDataValue) {
        if (noData == null || getNoDataValue() != noDataValue) {
            if (noData == null) {
                noData = createCompatibleProductData(1);
            }
            noData.setElemDouble(noDataValue);
            setGeophysicalNoDataValue();
            if (isNoDataValueUsed()) {
                resetValidMask();
            }
            setModified(true);
            fireProductNodeChanged(PROPERTY_NAME_NO_DATA_VALUE);
            if (isNoDataValueUsed()) {
                fireProductNodeDataChanged();
            }
        }
    }

    /**
     * Gets the geophysical no-data value which is simply the scaled "raw" no-data value
     * returned by {@link #getNoDataValue()}.
     * <p>The no-data value is used to determine valid pixels. For more information
     * on valid pixels, please refer to the documentation of the {@link #isPixelValid(int, int, javax.media.jai.ROI)}
     * method.
     *
     * @return the geophysical no-data value
     * @see #setGeophysicalNoDataValue(double)
     */
    public double getGeophysicalNoDataValue() {
        return geophysicalNoDataValue;
    }

    /**
     * Sets the geophysical no-data value which is simply the scaled "raw" no-data value
     * returned by {@link #getNoDataValue()}.
     * <p>The no-data value is used to determine valid pixels. For more information
     * on valid pixels, please refer to the documentation of the {@link #isPixelValid(int, int, javax.media.jai.ROI)}
     * method.
     * <p>On property change, the method calls {@link #fireProductNodeChanged(String)} with the property
     * name {@link #PROPERTY_NAME_NO_DATA_VALUE}.
     *
     * @param noDataValue the new geophysical no-data value
     * @see #setGeophysicalNoDataValue(double)
     * @see #isNoDataValueSet()
     */
    public void setGeophysicalNoDataValue(double noDataValue) {
        setNoDataValue(scaleInverse(noDataValue));
    }

    /**
     * Gets the expression that is used to determine whether a pixel is valid or not.
     * For more information
     * on valid pixels, please refer to the documentation of the {@link #isPixelValid(int, int, javax.media.jai.ROI)}
     * method.
     *
     * @return the valid mask expression.
     */
    public String getValidPixelExpression() {
        return validPixelExpression;
    }

    /**
     * Sets the expression that is used to determine whether a pixel is valid or not.
     * <p>The valid-pixel expression is used to determine valid pixels. For more information
     * on valid pixels, please refer to the documentation of the {@link #isPixelValid(int, int, javax.media.jai.ROI)}
     * method.
     * <p>On property change, the method calls {@link #fireProductNodeChanged(String)} with the property
     * name {@link #PROPERTY_NAME_VALID_PIXEL_EXPRESSION}.
     *
     * @param validPixelExpression the valid mask expression, can be null
     */
    public void setValidPixelExpression(final String validPixelExpression) {
        if (!ObjectUtils.equalObjects(this.validPixelExpression, validPixelExpression)) {
            this.validPixelExpression = validPixelExpression;
            resetValidMask();
            setModified(true);
            fireProductNodeChanged(PROPERTY_NAME_VALID_PIXEL_EXPRESSION);
            fireProductNodeDataChanged();
        }
    }

    /**
     * Tests whether or not this raster data node uses a data-mask in order to determine valid pixels. The method returns
     * true if either {@link #isValidPixelExpressionSet()} or {@link #isNoDataValueUsed()} returns true.
     * <p>The data-mask is used to determine valid pixels. For more information
     * on valid pixels, please refer to the documentation of the {@link #isPixelValid(int, int, javax.media.jai.ROI)}
     * method.
     *
     * @return true, if so
     */
    public boolean isValidMaskUsed() {
        return isValidPixelExpressionSet() || isNoDataValueUsed();
    }


    /**
     * Resets the valid mask of this raster.
     * The mask will be lazily regenerated when requested the next time.
     */
    public void resetValidMask() {
        validMaskROI = null;
        validMaskImage = null;
        stx = null;
    }

    /**
     * Gets the expression used for the computation of the mask which identifies valid pixel values.
     * It recognizes the value of the {@link #getNoDataValue() noDataValue} and the
     * {@link #getValidPixelExpression() validPixelExpression} properties, if any.
     * The method returns {@code null},  if none of these properties are set.
     *
     * @return The expression used for the computation of the mask which identifies valid pixel values,
     * or {@code null}.
     * @see #getValidPixelExpression()
     * @see #getNoDataValue()
     * @since BEAM 4.2
     */
    public String getValidMaskExpression() {
        String dataMaskExpression = null;
        if (isValidPixelExpressionSet()) {
            dataMaskExpression = getValidPixelExpression();
            if (isNoDataValueUsed()) {
                final String dataMaskExpression2 = createValidMaskExpressionForNoDataValue();
                if (!dataMaskExpression2.equals(dataMaskExpression)) {
                    dataMaskExpression = "(" + dataMaskExpression + ") && " + dataMaskExpression2;
                }
            }
        } else if (isNoDataValueUsed()) {
            dataMaskExpression = createValidMaskExpressionForNoDataValue();
        }
        return dataMaskExpression;
    }

    private String createValidMaskExpressionForNoDataValue() {
        final String ref = BandArithmetic.createExternalName(getName());
        final double noDataValue = getGeophysicalNoDataValue();
        if (Double.isNaN(noDataValue)) {
            return "!nan(" + ref + ")";
        } else if (Double.isInfinite(noDataValue)) {
            return "!inf(" + ref + ")";
        } else {
            if (ProductData.isIntType(getDataType())) {
                double rawNoDataValue = getNoDataValue();
                String rawSymbol = getName() + ".raw";
                String extName = BandArithmetic.createExternalName(rawSymbol);
                return extName + " != " + rawNoDataValue;
            } else {
                return "fneq(" + ref + "," + noDataValue + ")";
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateExpression(final String oldExternalName, final String newExternalName) {
        if (validPixelExpression == null) {
            return;
        }
        final String expression = StringUtils.replaceWord(validPixelExpression, oldExternalName, newExternalName);
        if (!validPixelExpression.equals(expression)) {
            validPixelExpression = expression;
            setModified(true);
        }
        super.updateExpression(oldExternalName, newExternalName);
    }

    /**
     * Returns true if the raster data of this <code>RasterDataNode</code> is loaded or elsewhere available, otherwise
     * false.
     *
     * @return true, if so.
     */
    public boolean hasRasterData() {
        return getRasterData() != null;
    }


    /**
     * Gets the raster data for this dataset. If the data hasn't been loaded so far the method returns
     * <code>null</code>.
     *
     * @return the raster data for this band, or <code>null</code> if data has not been loaded
     */
    public ProductData getRasterData() {
        return getData();
    }

    /**
     * Sets the raster data of this dataset.
     * <p> Note that this method does not copy data at all. If the supplied raster data is compatible with this product
     * raster, then simply its reference is stored. Modifications in the supplied raster data will also affect this
     * dataset's data.
     *
     * @param rasterData The raster data for this raster data node.
     * @see #getRasterData()
     */
    public void setRasterData(ProductData rasterData) {
        ProductData oldData = getData();
        if (oldData != rasterData) {
            if (rasterData != null) {
                if (rasterData.getType() != getDataType()) {
                    throw new IllegalArgumentException("rasterData.getType() != getDataType()");
                }
                if (rasterData.getNumElems() != getRasterWidth() * getRasterHeight()) {
                    throw new IllegalArgumentException("rasterData.getNumElems() != getRasterWidth() * getRasterHeight()");
                }
            }
            setData(rasterData);
        }
    }

    /**
     * @throws java.io.IOException if an I/O error occurs
     * @see #loadRasterData(com.bc.ceres.core.ProgressMonitor)
     */
    public void loadRasterData() throws IOException {
        loadRasterData(ProgressMonitor.NULL);
    }

    /**
     * Loads the raster data for this <code>RasterDataNode</code>. After this method has been called successfully,
     * <code>hasRasterData()</code> should always return <code>true</code> and <code>getRasterData()</code> should
     * always return a valid <code>ProductData</code> instance with at least <code>getRasterWidth()*getRasterHeight()</code>
     * elements (samples).
     * <p>The default implementation of this method does nothing.
     *
     * @param pm a monitor to inform the user about progress
     * @throws IOException if an I/O error occurs
     * @see #unloadRasterData()
     */
    public void loadRasterData(ProgressMonitor pm) throws IOException {
    }

    /**
     * Un-loads the raster data for this <code>RasterDataNode</code>.
     * <p>It is up to the implementation whether after this method has been called successfully, the
     * <code>hasRasterData()</code> method returns <code>false</code> or <code>true</code>.
     * <p>The default implementation of this method does nothing.
     *
     * @see #loadRasterData()
     */
    public void unloadRasterData() {
    }

    /**
     * Releases all of the resources used by this object instance and all of its owned children. Its primary use is to
     * allow the garbage collector to perform a vanilla job.
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>dispose()</code> are undefined.
     * <p>Overrides of this method should always call <code>super.dispose();</code> after disposing this instance.
     */
    @Override
    public void dispose() {
        if (imageInfo != null) {
            imageInfo.dispose();
            imageInfo = null;
        }
        if (sourceImage != null) {
            sourceImage.dispose();
            sourceImage = null;
        }
        if (validMaskROI != null) {
            validMaskROI = null;
        }
        if (validMaskImage != null) {
            validMaskImage.dispose();
            validMaskImage = null;
        }
        if (geophysicalImage != null && geophysicalImage != sourceImage) {
            geophysicalImage.dispose();
            geophysicalImage = null;
        }
        overlayMasks.removeAll();
        overlayMasks.clearRemovedList();

        if (ancillaryVariables != null) {
            ancillaryVariables.removeAll();
            ancillaryVariables.clearRemovedList();
        }

        super.dispose();
    }

    /**
     * Checks whether or not the pixel located at (x,y) is valid.
     * A pixel is assumed to be valid either if  {@link #getValidMaskImage() validMaskImage} is null or
     * or if the bit corresponding to (x,y) is set within the returned mask image.
     * <p>
     * <i>Note: Implementation changed by Norman (2011-08-09) in order to increase performance since
     * a synchronised block was used due to problem with the JAI ROI class that has been used in
     * the former implementation.</i>
     *
     * @param x the X co-ordinate of the pixel location
     * @param y the Y co-ordinate of the pixel location
     * @return <code>true</code> if the pixel is valid
     *
     * @throws ArrayIndexOutOfBoundsException if the co-ordinates are not in bounds
     * @see #isPixelValid(int, int, javax.media.jai.ROI)
     * @see #isPixelValid(int)
     * @see #setNoDataValueUsed(boolean)
     * @see #setNoDataValue(double)
     * @see #setValidPixelExpression(String)
     */
    public boolean isPixelValid(int x, int y) {
        if (!isValidMaskUsed()) {
            return true;
        }
        final PlanarImage image = getValidMaskImage();
        if (image != null) {
            int tx = image.XToTileX(x);
            int ty = image.YToTileY(y);
            Raster tile = image.getTile(tx, ty);
            return tile != null && tile.getSample(x, y, 0) != 0;
        }
        return true;
    }

    /**
     * Gets a geo-physical sample value at the given pixel coordinate as {@code int} value.
     * <p>
     * <i>Note: This method does not belong to the public API.
     * It has been added by Norman (2011-08-09) in order to perform performance tests.</i>
     *
     * @param x pixel X coordinate
     * @param y pixel Y coordinate
     * @return The geo-physical sample value.
     */
    public int getSampleInt(int x, int y) {
        final PlanarImage image = getGeophysicalImage();
        int tx = image.XToTileX(x);
        int ty = image.YToTileY(y);
        Raster tile = image.getTile(tx, ty);
        return tile.getSample(x, y, 0);
    }

    /**
     * Gets a geo-physical sample value at the given pixel coordinate as {@code float} value.
     * <p>
     * <i>Note: This method does not belong to the public API.
     * It has been added by Norman (2011-08-09) in order to perform performance tests.</i>
     *
     * @param x pixel X coordinate
     * @param y pixel Y coordinate
     * @return The geo-physical sample value.
     */
    public float getSampleFloat(int x, int y) {
        final PlanarImage image = getGeophysicalImage();
        int tx = image.XToTileX(x);
        int ty = image.YToTileY(y);
        Raster tile = image.getTile(tx, ty);
        return tile.getSampleFloat(x, y, 0);
    }

    /**
     * Checks whether or not the pixel located at (x,y) is valid.
     * A pixel is assumed to be valid either if  {@link #getValidMaskImage() validMaskImage} is null or
     * or if the bit corresponding to (x,y) is set within the returned mask image.
     *
     * @param pixelIndex the linear pixel index in the range 0 to width * height - 1
     * @return <code>true</code> if the pixel is valid
     *
     * @throws ArrayIndexOutOfBoundsException if the co-ordinates are not in bounds
     * @see #isPixelValid(int, int, javax.media.jai.ROI)
     * @see #isPixelValid(int, int)
     * @see #setNoDataValueUsed(boolean)
     * @see #setNoDataValue(double)
     * @see #setValidPixelExpression(String)
     * @since 4.1
     */
    public boolean isPixelValid(int pixelIndex) {
        if (!isValidMaskUsed()) {
            return true;
        }
        final int y = pixelIndex / getRasterWidth();
        final int x = pixelIndex - (y * getRasterWidth());
        return isPixelValid(x, y);
    }

    /**
     * Checks whether or not the pixel located at (x,y) is valid.
     * The method first test whether a pixel is valid by using the {@link #isPixelValid(int, int)} method,
     * and secondly, if the pixel is within the ROI (if any).
     *
     * @param x   the X co-ordinate of the pixel location
     * @param y   the Y co-ordinate of the pixel location
     * @param roi the ROI, if null the method returns {@link #isPixelValid(int, int)}
     * @return <code>true</code> if the pixel is valid
     *
     * @throws ArrayIndexOutOfBoundsException if the co-ordinates are not in bounds
     * @see #isPixelValid(int, int)
     * @see #isPixelValid(int)
     * @see #setNoDataValueUsed(boolean)
     * @see #setNoDataValue(double)
     * @see #setValidPixelExpression(String)
     * @see #readValidMask(int, int, int, int, boolean[]) 
     */
    public boolean isPixelValid(int x, int y, ROI roi) {
        return isPixelValid(x, y) && (roi == null || roi.contains(x, y));
    }

    /**
     * Returns the pixel located at (x,y) as an integer value.
     * <p>
     * Note that this method can only be used if this object's internal raster data buffer has been
     * {@link #setRasterData(ProductData) set} or {@link #loadRasterData() loaded}.
     *
     * @param x the X co-ordinate of the pixel location
     * @param y the Y co-ordinate of the pixel location
     * @return the pixel value at (x,y)
     * @throws ArrayIndexOutOfBoundsException if the co-ordinates are not in bounds
     * @throws IllegalStateException          if this object has no internal data buffer
     */
    public abstract int getPixelInt(int x, int y);

    /**
     * Returns the pixel located at (x,y) as a float value.
     * <p>
     * Note that this method can only be used if this object's internal raster data buffer has been
     * {@link #setRasterData(ProductData) set} or {@link #loadRasterData() loaded}.
     *
     * @param x the X co-ordinate of the pixel location
     * @param y the Y co-ordinate of the pixel location
     * @return the pixel value at (x,y)
     * @throws ArrayIndexOutOfBoundsException if the co-ordinates are not in bounds
     * @throws IllegalStateException          if this object has no internal data buffer
     */
    public abstract float getPixelFloat(int x, int y);

    /**
     * Returns the pixel located at (x,y) as a double value.
     * <p>
     * Note that this method can only be used if this object's internal raster data buffer has been
     * {@link #setRasterData(ProductData) set} or {@link #loadRasterData() loaded}.
     *
     * @param x the X co-ordinate of the pixel location
     * @param y the Y co-ordinate of the pixel location
     * @return the pixel value at (x,y)
     * @throws ArrayIndexOutOfBoundsException if the co-ordinates are not in bounds
     * @throws IllegalStateException          if this object has no internal data buffer
     */
    public abstract double getPixelDouble(int x, int y);

    /**
     * Sets the pixel located at (x,y) to the given integer value.
     * <p>
     * Note that this method can only be used if this object's internal raster data buffer has been
     * {@link #setRasterData(ProductData) set} or {@link #loadRasterData() loaded}.
     *
     * @param x          the X co-ordinate of the pixel location
     * @param y          the Y co-ordinate of the pixel location
     * @param pixelValue the new pixel value at (x,y)
     * @throws ArrayIndexOutOfBoundsException if the co-ordinates are not in bounds
     * @throws IllegalStateException          if this object has no internal data buffer
     */
    public abstract void setPixelInt(int x, int y, int pixelValue);

    /**
     * Sets the pixel located at (x,y) to the given float value.
     * <p>
     * Note that this method can only be used if this object's internal raster data buffer has been
     * {@link #setRasterData(ProductData) set} or {@link #loadRasterData() loaded}.
     *
     * @param x          the X co-ordinate of the pixel location
     * @param y          the Y co-ordinate of the pixel location
     * @param pixelValue the new pixel value at (x,y)
     * @throws ArrayIndexOutOfBoundsException if the co-ordinates are not in bounds
     * @throws IllegalStateException          if this object has no internal data buffer
     */
    public abstract void setPixelFloat(int x, int y, float pixelValue);

    /**
     * Sets the pixel located at (x,y) to the given double value.
     * <p>
     * Note that this method can only be used if this object's internal raster data buffer has been
     * {@link #setRasterData(ProductData) set} or {@link #loadRasterData() loaded}.
     *
     * @param x          the X co-ordinate of the pixel location
     * @param y          the Y co-ordinate of the pixel location
     * @param pixelValue the new pixel value at (x,y)
     * @throws ArrayIndexOutOfBoundsException if the co-ordinates are not in bounds
     * @throws IllegalStateException          if this object has no internal data buffer
     */
    public abstract void setPixelDouble(int x, int y, double pixelValue);

    /**
     * @see #getPixels(int, int, int, int, int[], ProgressMonitor)
     */
    public int[] getPixels(int x, int y, int w, int h, int[] pixels) {
        return getPixels(x, y, w, h, pixels, ProgressMonitor.NULL);
    }

    /**
     * Retrieves the range of pixels specified by the coordinates as integer array.
     * <p>
     * Note that this method can only be used if this object's internal raster data buffer has been
     * {@link #setRasterData(ProductData) set} or {@link #loadRasterData() loaded}.
     * You can use the {@link #readPixels(int, int, int, int, double[], ProgressMonitor)} method
     * to read or compute pixel values without a raster data buffer.
     * <p>
     * If the {@code pixels} array is <code>null</code> a new one will be created and returned.
     *
     * @param x      x offset into the band
     * @param y      y offset into the band
     * @param w      width of the pixel array to be read
     * @param h      height of the pixel array to be read.
     * @param pixels integer array to be filled with data
     * @param pm     a progress monitor
     * @throws IllegalStateException if this object has no internal data buffer
     */
    public abstract int[] getPixels(int x, int y, int w, int h, int[] pixels, ProgressMonitor pm);

    /**
     * @see #getPixels(int, int, int, int, float[], ProgressMonitor)
     */
    public float[] getPixels(int x, int y, int w, int h, float[] pixels) {
        return getPixels(x, y, w, h, pixels, ProgressMonitor.NULL);
    }

    /**
     * Retrieves the range of pixels specified by the coordinates as float array.
     * <p>
     * Note that this method can only be used if this object's internal raster data buffer has been
     * {@link #setRasterData(ProductData) set} or {@link #loadRasterData() loaded}.
     * You can use the {@link #readPixels(int, int, int, int, double[], ProgressMonitor)} method
     * to read or compute pixel values without a raster data buffer.
     * <p>
     * If the {@code pixels} array is <code>null</code> a new one will be created and returned.
     *
     * @param x      x offset into the band
     * @param y      y offset into the band
     * @param w      width of the pixel array to be read
     * @param h      height of the pixel array to be read.
     * @param pixels float array to be filled with data
     * @param pm     a progress monitor
     * @throws IllegalStateException if this object has no internal data buffer
     */
    public abstract float[] getPixels(int x, int y, int w, int h, float[] pixels, ProgressMonitor pm);

    /**
     * @see #getPixels(int, int, int, int, double[], ProgressMonitor)
     * @deprecated since BEAM 4.11. Use {@link #getSourceImage()} instead.
     */
    public double[] getPixels(int x, int y, int w, int h, double[] pixels) {
        return getPixels(x, y, w, h, pixels, ProgressMonitor.NULL);
    }

    /**
     * Retrieves the range of pixels specified by the coordinates as double array.
     * <p>
     * Note that this method can only be used if this object's internal raster data buffer has been
     * {@link #setRasterData(ProductData) set} or {@link #loadRasterData() loaded}.
     * You can use the {@link #readPixels(int, int, int, int, double[], ProgressMonitor)} method
     * to read or compute pixel values without a raster data buffer.
     * <p>
     * If the {@code pixels} array is <code>null</code> a new one will be created and returned.
     *
     * @param x      x offset into the band
     * @param y      y offset into the band
     * @param w      width of the pixel array to be read
     * @param h      height of the pixel array to be read.
     * @param pixels double array to be filled with data
     * @param pm     a monitor to inform the user about progress
     * @throws IllegalStateException if this object has no internal data buffer
     */
    public abstract double[] getPixels(int x, int y, int w, int h, double[] pixels, ProgressMonitor pm);


    /**
     * Sets a range of pixels specified by the coordinates as integer array.
     * <p>
     * Note that this method can only be used if this object's internal raster data buffer has been
     * {@link #setRasterData(ProductData) set} or {@link #loadRasterData() loaded}.
     * You can use the {@link #writePixels(int, int, int, int, double[], ProgressMonitor)} method
     * to write pixels directly to the associated {@link Product#setProductWriter(ProductWriter) product writer}.
     *
     * @param x      x offset into the band
     * @param y      y offset into the band
     * @param w      width of the pixel array to be written
     * @param h      height of the pixel array to be written.
     * @param pixels integer array to be written
     * @throws NullPointerException  if this band has no raster data
     * @throws IllegalStateException if this object has no internal data buffer
     */
    public abstract void setPixels(int x, int y, int w, int h, int[] pixels);

    /**
     * Sets a range of pixels specified by the coordinates as float array.
     * <p>
     * Note that this method can only be used if this object's internal raster data buffer has been
     * {@link #setRasterData(ProductData) set} or {@link #loadRasterData() loaded}.
     * You can use the {@link #writePixels(int, int, int, int, double[], ProgressMonitor)} method
     * to write pixels directly to the associated {@link Product#setProductWriter(ProductWriter) product writer}.
     *
     * @param x      x offset into the band
     * @param y      y offset into the band
     * @param w      width of the pixel array to be written
     * @param h      height of the pixel array to be written.
     * @param pixels float array to be written
     * @throws NullPointerException  if this band has no raster data
     * @throws IllegalStateException if this object has no internal data buffer
     */
    public abstract void setPixels(int x, int y, int w, int h, float[] pixels);

    /**
     * Sets a range of pixels specified by the coordinates as double array.
     * <p>
     * Note that this method can only be used if this object's internal raster data buffer has been
     * {@link #setRasterData(ProductData) set} or {@link #loadRasterData() loaded}.
     * You can use the {@link #writePixels(int, int, int, int, double[], ProgressMonitor)} method
     * to write pixels directly to the associated {@link Product#setProductWriter(ProductWriter) product writer}.
     *
     * @param x      x offset into the band
     * @param y      y offset into the band
     * @param w      width of the pixel array to be written
     * @param h      height of the pixel array to be written.
     * @param pixels double array to be written
     * @throws NullPointerException  if this band has no raster data
     * @throws IllegalStateException if this object has no internal data buffer
     */
    public abstract void setPixels(int x, int y, int w, int h, double[] pixels);

    /**
     * @see #readPixels(int, int, int, int, int[], ProgressMonitor)
     */
    public int[] readPixels(int x, int y, int w, int h, int[] pixels) throws IOException {
        return readPixels(x, y, w, h, pixels, ProgressMonitor.NULL);
    }

    /**
     * Retrieves the band data at the given offset (x, y), width and height as int data. If the data is already in
     * memory, it merely copies the data to the buffer provided. If not, it calls the attached product reader or operator
     * to read or compute the data.
     * <p>
     * If the {@code pixels} array is <code>null</code> a new one will be created and returned.
     *
     * @param x      x offset into the band
     * @param y      y offset into the band
     * @param w      width of the pixel array to be read
     * @param h      height of the pixel array to be read
     * @param pixels array to be filled with data
     * @param pm     a progress monitor
     * @return the pixels read
     * @throws IOException           if an /IO error occurs
     * @throws IllegalStateException if this object has no attached {@link Product#setProductReader(ProductReader) product reader}
     */
    public abstract int[] readPixels(int x, int y, int w, int h, int[] pixels, ProgressMonitor pm) throws IOException;

    /**
     * @see #readPixels(int, int, int, int, float[], ProgressMonitor)
     */
    public float[] readPixels(int x, int y, int w, int h, float[] pixels) throws IOException {
        return readPixels(x, y, w, h, pixels, ProgressMonitor.NULL);
    }

    /**
     * Retrieves the band data at the given offset (x, y), width and height as int data. If the data is already in
     * memory, it merely copies the data to the buffer provided. If not, it calls the attached product reader or operator
     * to read or compute the data.
     * <p>
     * If the {@code pixels} array is <code>null</code> a new one will be created and returned.
     *
     * @param x      x offset into the band
     * @param y      y offset into the band
     * @param w      width of the pixel array to be read
     * @param h      height of the pixel array to be read
     * @param pixels array to be filled with data
     * @param pm     a progress monitor
     * @return the pixels read
     * @throws IllegalStateException if this object has no attached {@link Product#setProductReader(ProductReader) product reader}
     */
    public abstract float[] readPixels(int x, int y, int w, int h, float[] pixels, ProgressMonitor pm) throws
                                                                                                       IOException;

    /**
     * @see #readPixels(int, int, int, int, double[], ProgressMonitor)
     */
    public double[] readPixels(int x, int y, int w, int h, double[] pixels) throws IOException {
        return readPixels(x, y, w, h, pixels, ProgressMonitor.NULL);
    }

    /**
     * Retrieves the band data at the given offset (x, y), width and height as int data. If the data is already in
     * memory, it merely copies the data to the buffer provided. If not, it calls the attached product reader or operator
     * to read or compute the data.
     * <p>
     * If the {@code pixels} array is <code>null</code> a new one will be created and returned.
     *
     * @param x      x offset into the band
     * @param y      y offset into the band
     * @param w      width of the pixel array to be read
     * @param h      height of the pixel array to be read
     * @param pixels array to be filled with data
     * @param pm     a progress monitor
     * @return the pixels read
     * @throws IllegalStateException if this object has no attached {@link Product#setProductReader(ProductReader) product reader}
     */
    public abstract double[] readPixels(int x, int y, int w, int h, double[] pixels, ProgressMonitor pm) throws
                                                                                                         IOException;

    /**
     * @see #writePixels(int, int, int, int, int[], ProgressMonitor)
     */
    @SuppressWarnings("unused") // may be useful API for scripting languages
    public void writePixels(int x, int y, int w, int h, int[] pixels) throws IOException {
        writePixels(x, y, w, h, pixels, ProgressMonitor.NULL);
    }

    /**
     * Writes the range of given pixels specified to the specified coordinates as integers.
     *
     * @param x      x offset into the band
     * @param y      y offset into the band
     * @param w      width of the pixel array to be written
     * @param h      height of the pixel array to be written
     * @param pixels array of pixels to write
     * @param pm     a progress monitor
     * @throws IllegalStateException if this object has no attached {@link Product#setProductWriter(ProductWriter) product writer}
     * @throws IOException           if an I/O error occurs
     */
    public abstract void writePixels(int x, int y, int w, int h, int[] pixels, ProgressMonitor pm) throws IOException;

    /**
     * @see #writePixels(int, int, int, int, float[], ProgressMonitor)
     */
    @SuppressWarnings("unused") // may be useful API for scripting languages
    public synchronized void writePixels(int x, int y, int w, int h, float[] pixels) throws IOException {
        writePixels(x, y, w, h, pixels, ProgressMonitor.NULL);
    }

    /**
     * Writes the range of given pixels specified to the specified coordinates as floats.
     *
     * @param x      x offset into the band
     * @param y      y offset into the band
     * @param w      width of the pixel array to be written
     * @param h      height of the pixel array to be written
     * @param pixels array of pixels to write
     * @param pm     a progress monitor
     * @throws IllegalStateException if this object has no attached {@link Product#setProductWriter(ProductWriter) product writer}
     * @throws IOException           if an I/O error occurs
     */
    public abstract void writePixels(int x, int y, int w, int h, float[] pixels, ProgressMonitor pm) throws IOException;

    /**
     * @see #writePixels(int, int, int, int, double[], ProgressMonitor)
     */
    @SuppressWarnings("unused") // may be useful API for scripting languages
    public void writePixels(int x, int y, int w, int h, double[] pixels) throws IOException {
        writePixels(x, y, w, h, pixels, ProgressMonitor.NULL);
    }

    /**
     * Writes the range of given pixels specified to the specified coordinates as doubles.
     *
     * @param x      x offset into the band
     * @param y      y offset into the band
     * @param w      width of the pixel array to be written
     * @param h      height of the pixel array to be written
     * @param pixels array of pixels to write
     * @param pm     a progress monitor
     * @throws IllegalStateException if this object has no attached {@link Product#setProductWriter(ProductWriter) product writer}
     * @throws IOException           if an I/O error occurs
     */
    public abstract void writePixels(int x, int y, int w, int h, double[] pixels, ProgressMonitor pm) throws
                                                                                                      IOException;

    /**
     * Reads the valid mask values for the specified area. The mask indicates if a pixel is valid or not.
     * The values are retrieved from the {@link #getValidMaskImage()}.
     * For a single pixel it is also possible to us {@link #isPixelValid(int, int)}
     *
     * @param x         x offset into the band
     * @param y         y offset into the band
     * @param w         width of the pixel array to be written
     * @param h         height of the pixel array to be written
     * @param validMask mask for the specified area if the pixels are valid. Can be {@code null}, then a new array
     *                  will be created and returned. If the array is provided it must have the size ({@code w * h})
     * @return the valid mask. Either the provided array or a newly created.
     *
     * @throws IOException if an I/O error occurs
     *
     * @see #getValidMaskImage()
     * @see #getValidPixelExpression()
     * @see #isPixelValid(int, int)
     */
    public boolean[] readValidMask(int x, int y, int w, int h, boolean[] validMask) throws IOException {
        if (validMask == null) {
            validMask = new boolean[w * h];
        }
        if (isValidMaskUsed()) {
            int index = 0;
            ROI roi = getValidMaskROI();
            for (int yi = y; yi < y + h; yi++) {
                for (int xi = x; xi < x + w; xi++) {
                    validMask[index] = roi.contains(xi, yi);
                    index++;
                }
            }
        } else {
            Arrays.fill(validMask, true);
        }
        return validMask;
    }

    /**
     * @throws java.io.IOException if an I/O error occurs
     * @see #readRasterDataFully(ProgressMonitor)
     * @see #unloadRasterData()
     */
    public void readRasterDataFully() throws IOException {
        readRasterDataFully(ProgressMonitor.NULL);
    }

    /**
     * Reads the complete underlying raster data.
     * <p>After this method has been called successfully, <code>hasRasterData()</code> should always return
     * <code>true</code> and <code>getRasterData()</code> should always return a valid <code>ProductData</code> instance
     * with at least <code>getRasterWidth()*getRasterHeight()</code> elements (samples).
     * <p>In opposite to the <code>loadRasterData</code> method, the <code>readRasterDataFully</code> method always
     * reloads the data of this product raster, independently of whether its has already been loaded or not.
     *
     * @param pm a monitor to inform the user about progress
     * @throws java.io.IOException if an I/O error occurs
     * @see #loadRasterData
     * @see #readRasterData(int, int, int, int, ProductData, com.bc.ceres.core.ProgressMonitor)
     */
    public abstract void readRasterDataFully(ProgressMonitor pm) throws IOException;

    /**
     * Reads raster data from the node's associated data source into the given data
     * buffer.
     *
     * @param offsetX    the X-offset in the raster co-ordinates where reading starts
     * @param offsetY    the Y-offset in the raster co-ordinates where reading starts
     * @param width      the width of the raster data buffer
     * @param height     the height of the raster data buffer
     * @param rasterData a raster data buffer receiving the pixels to be read
     * @throws java.io.IOException      if an I/O error occurs
     * @throws IllegalArgumentException if the raster is null
     * @throws IllegalStateException    if this product raster was not added to a product so far, or if the product to
     *                                  which this product raster belongs to, has no associated product reader
     * @see ProductReader#readBandRasterData(Band, int, int, int, int, ProductData, com.bc.ceres.core.ProgressMonitor)
     */
    public void readRasterData(int offsetX, int offsetY,
                               int width, int height,
                               ProductData rasterData) throws IOException {
        readRasterData(offsetX, offsetY, width, height, rasterData, ProgressMonitor.NULL);
    }

    /**
     * The method behaves exactly as {@link #readRasterData(int, int, int, int, ProductData)},
     * but clients can additionally pass a {@link ProgressMonitor}.
     *
     * @param offsetX    the X-offset in the raster co-ordinates where reading starts
     * @param offsetY    the Y-offset in the raster co-ordinates where reading starts
     * @param width      the width of the raster data buffer
     * @param height     the height of the raster data buffer
     * @param rasterData a raster data buffer receiving the pixels to be read
     * @param pm         a monitor to inform the user about progress
     * @throws java.io.IOException      if an I/O error occurs
     * @throws IllegalArgumentException if the raster is null
     * @throws IllegalStateException    if this product raster was not added to a product so far, or if the product to
     *                                  which this product raster belongs to, has no associated product reader
     */
    public abstract void readRasterData(int offsetX, int offsetY,
                                        int width, int height,
                                        ProductData rasterData,
                                        ProgressMonitor pm) throws IOException;

    @SuppressWarnings("unused") // may be useful API for scripting languages
    public void writeRasterDataFully() throws IOException {
        writeRasterDataFully(ProgressMonitor.NULL);
    }

    /**
     * Writes the complete underlying raster data.
     *
     * @param pm a monitor to inform the user about progress
     * @throws java.io.IOException if an I/O error occurs
     */
    public abstract void writeRasterDataFully(ProgressMonitor pm) throws IOException;

    @SuppressWarnings("unused") // may be useful API for scripting languages
    public void writeRasterData(int offsetX, int offsetY, int width, int height, ProductData rasterData)
            throws IOException {
        writeRasterData(offsetX, offsetY, width, height, rasterData, ProgressMonitor.NULL);
    }

    /**
     * Writes data from this product raster into the specified region of the user-supplied raster.
     * <p> It is important to know that this method does not change this product raster's internal state nor does it
     * write into this product raster's internal raster.
     *
     * @param rasterData a raster data buffer receiving the pixels to be read
     * @param offsetX    the X-offset in raster co-ordinates where reading starts
     * @param offsetY    the Y-offset in raster co-ordinates where reading starts
     * @param width      the width of the raster data buffer
     * @param height     the height of the raster data buffer
     * @param pm         a monitor to inform the user about progress
     * @throws java.io.IOException      if an I/O error occurs
     * @throws IllegalArgumentException if the raster is null
     * @throws IllegalStateException    if this product raster was not added to a product so far, or if the product to
     *                                  which this product raster belongs to, has no associated product reader
     * @see ProductReader#readBandRasterData(Band, int, int, int, int, ProductData, com.bc.ceres.core.ProgressMonitor)
     */
    public abstract void writeRasterData(int offsetX, int offsetY,
                                         int width, int height,
                                         ProductData rasterData,
                                         ProgressMonitor pm) throws IOException;

    /**
     * Creates raster data that is compatible to this dataset's data type. The data buffer returned contains exactly
     * <code>getRasterWidth()*getRasterHeight()</code> elements of a compatible data type.
     *
     * @return raster data compatible with this product raster
     * @see #createCompatibleSceneRasterData
     */
    public ProductData createCompatibleRasterData() {
        return createCompatibleRasterData(getRasterWidth(), getRasterHeight());
    }

    /**
     * Creates raster data that is compatible to this dataset's data type. The data buffer returned contains exactly
     * <code>getBandOutputRasterWidth()*getBandOutputRasterHeight()</code> elements of a compatible data type.
     *
     * @return raster data compatible with this product raster
     * @see #createCompatibleRasterData
     */
    @SuppressWarnings("unused") // may be useful API for scripting languages
    public ProductData createCompatibleSceneRasterData() {
        return createCompatibleRasterData(getRasterWidth(), getRasterHeight());
    }

    /**
     * Creates raster data that is compatible to this dataset's data type. The data buffer returned contains exactly
     * <code>width*height</code> elements of a compatible data type.
     *
     * @param width  the width of the raster data to be created
     * @param height the height of the raster data to be created
     * @return raster data compatible with this product raster
     * @see #createCompatibleRasterData
     * @see #createCompatibleSceneRasterData
     */
    public ProductData createCompatibleRasterData(int width, int height) {
        return createCompatibleProductData(width * height);
    }

    /**
     * Tests whether the given parameters specify a compatible raster or not.
     *
     * @param rasterData the raster data
     * @param w          the raster width
     * @param h          the raster height
     * @return {@code true} if so
     */
    public boolean isCompatibleRasterData(ProductData rasterData, int w, int h) {
        return rasterData != null
               && rasterData.getType() == getDataType()
               && rasterData.getNumElems() == w * h;
    }

    /**
     * Throws an <code>IllegalArgumentException</code> if the given parameters dont specify a compatible raster.
     *
     * @param rasterData the raster data
     * @param w          the raster width
     * @param h          the raster height
     */
    @SuppressWarnings("unused") // may be useful API for scripting languages
    public void checkCompatibleRasterData(ProductData rasterData, int w, int h) {
        if (!isCompatibleRasterData(rasterData, w, h)) {
            throw new IllegalArgumentException("invalid raster data buffer for '" + getName() + "'");
        }
    }

    /**
     * Determines whether this raster data node contains integer samples.
     *
     * @return true if this raster data node contains integer samples.
     */
    public boolean hasIntPixels() {
        return ProductData.isIntType(getDataType());
    }

    /**
     * Creates a transect profile for the given shape (-outline).
     *
     * @param shape the shape
     * @return the profile data
     * @throws IOException if an I/O error occurs
     */
    public TransectProfileData createTransectProfileData(Shape shape) throws IOException {
        return new TransectProfileDataBuilder().raster(this).path(shape).build();
    }

    /**
     * Accepts the given visitor. This method implements the well known 'Visitor' design pattern of the gang-of-four.
     * The visitor pattern allows to define new operations on the product data model without the need to add more code
     * to it. The new operation is implemented by the visitor.
     *
     * @param visitor the visitor, must not be <code>null</code>
     */
    @Override
    public abstract void acceptVisitor(ProductVisitor visitor);

    /**
     * Gets the image information for image display.
     *
     * @return the image info or <code>null</code>
     */
    public ImageInfo getImageInfo() {
        return imageInfo;
    }

    /**
     * Sets the image information for image display.
     *
     * @param imageInfo the image info, can be <code>null</code>
     */
    public void setImageInfo(ImageInfo imageInfo) {
        setImageInfo(imageInfo, true);
    }

    protected void setImageInfo(ImageInfo imageInfo, boolean change) {
        if (this.imageInfo != imageInfo) {
            this.imageInfo = imageInfo;
            if (change) {
                fireImageInfoChanged();
            }
        }
    }

    /**
     * Notifies listeners that the image (display) information has changed.
     *
     * @since BEAM 4.7
     */
    public void fireImageInfoChanged() {
        fireProductNodeChanged(PROPERTY_NAME_IMAGE_INFO);
        setModified(true);
    }

    /**
     * Returns the image information for this raster data node.
     * <p>The method simply returns the value of <code>ensureValidImageInfo(null, ProgressMonitor.NULL)</code>.
     *
     * @param pm A progress monitor.
     * @return A valid image information instance.
     * @see #getImageInfo(double[], ProgressMonitor)
     * @since BEAM 4.2
     */
    public final ImageInfo getImageInfo(ProgressMonitor pm) {
        return getImageInfo(null, pm);
    }

    /**
     * Gets the image creation information.
     * <p>If no image information has been assigned before, the <code>{@link #createDefaultImageInfo(double[], com.bc.ceres.core.ProgressMonitor)}</code> method is
     * called with the given parameters passed to this method.
     *
     * @param histoSkipAreas Only used, if new image info is created (see <code>{@link #createDefaultImageInfo(double[], com.bc.ceres.core.ProgressMonitor)}</code>
     *                       method).
     * @param pm             A progress monitor.
     * @return The image creation information.
     * @since BEAM 4.2
     */
    public final synchronized ImageInfo getImageInfo(double[] histoSkipAreas, ProgressMonitor pm) {
        ImageInfo imageInfo = getImageInfo();
        if (imageInfo == null) {
            imageInfo = createDefaultImageInfo(histoSkipAreas, pm);
            setImageInfo(imageInfo, false);
        }
        return imageInfo;
    }

    /**
     * Creates a default image information instance.
     * <p>An <code>IllegalStateException</code> is thrown in the case that this raster data node has no raster data.
     *
     * @param histoSkipAreas the left (at index 0) and right (at index 1) normalized areas of the raster data
     *                       histogram to be excluded when determining the value range for a linear constrast
     *                       stretching. Can be <code>null</code>, in this case <code>{0.01, 0.04}</code> resp. 5% of
     *                       the entire area is skipped.
     * @param pm             a monitor to inform the user about progress
     * @return a valid image information instance, never <code>null</code>.
     */
    public synchronized ImageInfo createDefaultImageInfo(double[] histoSkipAreas, ProgressMonitor pm) {
        Stx stx = getStx(false, pm);
        Histogram histogram = new Histogram(stx.getHistogramBins(),
                                            stx.getMinimum(),
                                            stx.getMaximum());
        return createDefaultImageInfo(histoSkipAreas, histogram);
    }

    /**
     * Creates an instance of a default image information.
     * <p>An <code>IllegalStateException</code> is thrown in the case that this raster data node has no raster data.
     *
     * @param histoSkipAreas the left (at index 0) and right (at index 1) normalized areas of the raster data
     *                       histogram to be excluded when determining the value range for a linear constrast
     *                       stretching. Can be <code>null</code>, in this case <code>{0.01, 0.04}</code> resp. 5% of
     *                       the entire area is skipped.
     * @param histogram      the histogram to create the image information.
     * @return a valid image information instance, never <code>null</code>.
     */
    public final ImageInfo createDefaultImageInfo(double[] histoSkipAreas, Histogram histogram) {
        ImageInfo customPalette = loadCustomColorPalette(histogram);
        if (customPalette != null) {
            return customPalette;
        }

        final Range range;
        if (histoSkipAreas != null) {
            range = histogram.findRange(histoSkipAreas[0], histoSkipAreas[1], true, false);
        } else {
            range = histogram.findRange(0.01, 0.04, true, false);
        }

        final double min, max;
        if (range.getMin() != range.getMax()) {
            min = range.getMin();
            max = range.getMax();
        } else {
            min = histogram.getMin();
            max = histogram.getMax();
        }

        double center = scale(0.5 * (scaleInverse(min) + scaleInverse(max)));
        final ColorPaletteDef gradationCurve = new ColorPaletteDef(min, center, max);

        return new ImageInfo(gradationCurve);
    }

    private ImageInfo loadCustomColorPalette(final Histogram histogram) {

        // set custom color palettes based on product types or units
        final String unit = getUnit();
        if (unit == null) {
            return null;
        }

        final String prefix = SystemUtils.getApplicationContextId() + ".color-palette.unit.";
        final String[] keys;
        try {
            keys = Config.instance().listKeys(prefix);
        } catch (BackingStoreException e) {
            SystemUtils.LOG.severe(String.format("Unable to load configuration '%s': %s", Config.instance().name(), e.getMessage()));
            return null;
        }

        String name = null;
        for (String key : keys) {
            final String unitKey = key.replace(prefix, "");
            if (unit.contains(unitKey)) {
                name = Config.instance().preferences().get(key, null);
                break;
            }
        }

        if (name != null) {
            try {
                return loadColorPalette(histogram, name);
            } catch (IOException e) {
                SystemUtils.LOG.severe(String.format("Unable to load custom color palette '%s': %s", name, e.toString()));
            }
        }

        return null;
    }

    private static ImageInfo loadColorPalette(final Histogram histogram, final String paletteFileName) throws IOException {
        final Path filePath = SystemUtils.getAuxDataPath().resolve("color_palettes").resolve(paletteFileName);
        final ColorPaletteDef colorPaletteDef = ColorPaletteDef.loadColorPaletteDef(filePath.toFile());
        final ImageInfo info = new ImageInfo(colorPaletteDef);
        final Range autoStretchRange = histogram.findRangeFor95Percent();
        info.setColorPaletteDef(colorPaletteDef, autoStretchRange.getMin(), autoStretchRange.getMax(), colorPaletteDef.isAutoDistribute());
        return info;
    }

    /**
     * @return The overlay mask group.
     */
    public ProductNodeGroup<Mask> getOverlayMaskGroup() {
        return overlayMasks;
    }

    /**
     * Creates an image for this raster data node. The method simply returns <code>ProductUtils.createColorIndexedImage(this,
     * null)</code>.
     *
     * @param pm a monitor to inform the user about progress
     * @return a greyscale/palette-based image for this raster data node
     * @throws IOException if the raster data is not loaded so far and reload causes an I/O error
     * @see #setImageInfo(ImageInfo)
     */
    @SuppressWarnings("unused") // may be useful API for scripting languages
    public BufferedImage createColorIndexedImage(ProgressMonitor pm) throws IOException {
        return ProductUtils.createColorIndexedImage(this, pm);
    }

    /**
     * Creates an RGB image for this raster data node.
     *
     * @param pm a monitor to inform the user about progress
     * @return a greyscale/palette-based image for this raster data node
     * @throws IOException if the raster data is not loaded so far and reload causes an I/O error
     * @see #setImageInfo(ImageInfo)
     */
    @SuppressWarnings("unused") // may be useful API for scripting languages
    public BufferedImage createRgbImage(ProgressMonitor pm) throws IOException {
        if (imageInfo != null) {
            return ProductUtils.createRgbImage(new RasterDataNode[]{this}, imageInfo, pm);
        } else {
            pm.beginTask("Creating image", 1 + 3);
            BufferedImage rgbImage;
            try {
                imageInfo = createDefaultImageInfo(null, SubProgressMonitor.create(pm, 1));
                rgbImage = ProductUtils.createRgbImage(new RasterDataNode[]{this}, imageInfo,
                                                       SubProgressMonitor.create(pm, 3));
            } finally {
                pm.done();
            }
            return rgbImage;
        }
    }

    public byte[] quantizeRasterData(final double newMin, final double newMax, final double gamma,
                                     ProgressMonitor pm) throws IOException {
        final byte[] colorIndexes = new byte[getRasterWidth() * getRasterHeight()];
        quantizeRasterData(newMin, newMax, gamma, colorIndexes, 0, 1, pm);
        return colorIndexes;
    }

    public void quantizeRasterData(double newMin, double newMax, double gamma, byte[] samples, int offset, int stride,
                                   ProgressMonitor pm) throws IOException {
        final ProductData sceneRasterData = getRasterData();
        final double rawMin = scaleInverse(newMin);
        final double rawMax = scaleInverse(newMax);
        byte[] gammaCurve = null;
        if (gamma != 0.0 && gamma != 1.0) {
            gammaCurve = MathUtils.createGammaCurve(gamma, new byte[256]);
        }
        if (sceneRasterData != null) {
            quantizeRasterData(sceneRasterData, rawMin, rawMax, samples, offset, stride, gammaCurve, pm);
        } else {
            quantizeRasterDataFromFile(rawMin, rawMax, samples, offset, stride, gammaCurve, pm);
        }
    }


    private void quantizeRasterDataFromFile(final double rawMin,
                                            final double rawMax,
                                            final byte[] samples,
                                            final int offset,
                                            final int stride,
                                            final byte[] gammaCurve, ProgressMonitor pm) throws IOException {
        processRasterData("Quantizing raster '" + getDisplayName() + "'",
                          (buffer, y0, numLines, pm1) -> {
                              int pos = y0 * getRasterWidth() * stride;
                              quantizeRasterData(buffer, rawMin, rawMax, samples, pos + offset, stride, gammaCurve,
                                                 pm1);
                          }, pm);
    }

    private static ProductData recycleOrCreateBuffer(final int dataType, final int buffersize, ProductData readBuffer) {
        if (readBuffer == null || readBuffer.getNumElems() != buffersize) {
            readBuffer = ProductData.createInstance(dataType, buffersize);
        }
        return readBuffer;
    }

    /**
     * Applies the scaling <code>v * scalingFactor + scalingOffset</code> the the given input value. If the
     * <code>log10Scaled</code> property is true, the result is taken to the power of 10 <i>after</i> the actual
     * scaling.
     *
     * @param v the input value
     * @return the scaled value
     */
    @Override
    public final double scale(double v) {
        v = v * scalingFactor + scalingOffset;
        if (log10Scaled) {
            v = Math.pow(10.0, v);
        }
        return v;
    }

    /**
     * Applies the inverse scaling <code>(v - scalingOffset) / scalingFactor</code> the the given input value. If the
     * <code>log10Scaled</code> property is true, the common logarithm is applied to the input <i>before</i> the actual
     * scaling.
     *
     * @param v the input value
     * @return the scaled value
     */
    @Override
    public final double scaleInverse(double v) {
        if (log10Scaled) {
            v = Math.log10(v);
        }
        return (v - scalingOffset) / scalingFactor;
    }


    private void setScalingApplied() {
        scalingApplied = getScalingFactor() != 1.0
                || getScalingOffset() != 0.0
                || isLog10Scaled();
    }

    /**
     * Returns the pixel located at (x,y) as a string value.
     *
     * @param x the X co-ordinate of the pixel location
     * @param y the Y co-ordinate of the pixel location
     * @return the pixel value at (x,y) as string or an error message text
     */
    public String getPixelString(int x, int y) {
        if (!isPixelWithinImageBounds(x, y)) {
            return INVALID_POS_TEXT;
        }
        if (hasRasterData()) {
            if (isPixelValid(x, y)) {
                int geophysicalDataType = getGeophysicalDataType();
                if (geophysicalDataType == ProductData.TYPE_FLOAT64) {
                    double pixel = getPixelDouble(x, y);
                    return Double.toString(pixel);
                } else if (geophysicalDataType == ProductData.TYPE_FLOAT32) {
                    float pixel = getPixelFloat(x, y);
                    return Float.toString(pixel);
                } else {
                    int pixel = getPixelInt(x, y);
                    return Integer.toString(pixel);
                }
            } else {
                return NO_DATA_TEXT;
            }
        } else {
            try {
                final boolean pixelValid = readValidMask(x, y, 1, 1, new boolean[1])[0];
                if (pixelValid) {
                    int geophysicalDataType = getGeophysicalDataType();
                    if (geophysicalDataType == ProductData.TYPE_FLOAT64) {
                        double pixel = readPixels(x, y, 1, 1, new double[1])[0];
                        return String.format("%.10f", pixel);
                    } else if (geophysicalDataType == ProductData.TYPE_FLOAT32) {
                        float pixel = readPixels(x, y, 1, 1, new float[1])[0];
                        return String.format("%.5f", pixel);
                    } else {
                        int pixel = readPixels(x, y, 1, 1, new int[1])[0];
                        return String.valueOf(pixel);
                    }
                } else {
                    return NO_DATA_TEXT;
                }
            } catch (IOException e) {
                return IO_ERROR_TEXT;
            }
        }
    }

    private boolean isPixelWithinImageBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < getRasterWidth() && y < getRasterHeight();
    }

    private boolean isValidPixelExpressionSet() {
        return getValidPixelExpression() != null && getValidPixelExpression().trim().length() > 0;
    }

    private int getReadBufferLineCount() {
        final int sizePerLine = getRasterWidth() * ProductData.getElemSize(getDataType());
        int bufferLineCount = READ_BUFFER_MAX_SIZE / sizePerLine;
        if (bufferLineCount == 0) {
            bufferLineCount = 1;
        }
        return bufferLineCount;
    }

    private static void quantizeRasterData(final ProductData sceneRasterData, final double rawMin, final double rawMax,
                                           byte[] samples, int offset, int stride, byte[] resampleLUT,
                                           ProgressMonitor pm) {
        Quantizer.quantizeGeneric(sceneRasterData.getElems(), sceneRasterData.isUnsigned(), rawMin, rawMax, samples,
                                  offset, stride, pm);
        if (resampleLUT != null && resampleLUT.length == 256) {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = resampleLUT[samples[i] & 0xff];
            }
        }
    }

    private void setGeophysicalNoDataValue() {
        geophysicalNoDataValue = scale(getNoDataValue());
    }

    /**
     * Returns whether the source image is set on this {@code RasterDataNode}.
     *
     * @return whether the source image is set.
     * @see #getSourceImage()
     * @see #setSourceImage(java.awt.image.RenderedImage)
     * @see #setSourceImage(com.bc.ceres.glevel.MultiLevelImage)
     * @see #createSourceImage()
     * @since BEAM 4.5
     */
    public boolean isSourceImageSet() {
        return sourceImage != null;
    }

    /**
     * Gets the source image associated with this {@code RasterDataNode}.
     *
     * @return The source image. Never {@code null}. In the case that {@link #isSourceImageSet()} returns {@code false},
     * the method {@link #createSourceImage()} will be called in order to set and return a valid source image.
     * @see #createSourceImage()
     * @see #isSourceImageSet()
     * @since BEAM 4.2
     */
    public MultiLevelImage getSourceImage() {
        if (!isSourceImageSet()) {
            synchronized (this) {
                if (!isSourceImageSet()) {
                    this.sourceImage = toMultiLevelImage(createSourceImage());
                }
            }
        }
        return sourceImage;
    }

    /**
     * Creates the source image associated with this {@code RasterDataNode}.
     * This shall preferably be a {@link MultiLevelImage} instance which recognises this raster data node's
     * {@link ##getImageToModelTransform() imageToModelTransform} property, if set.
     *
     * @return A new source image instance.
     * @since BEAM 4.5
     */
    protected abstract RenderedImage createSourceImage();

    /**
     * Sets the source image associated with this {@code RasterDataNode}.
     *
     * @param sourceImage The source image.
     *                    Can be {@code null}. If so, {@link #isSourceImageSet()} will return {@code false}.
     * @since BEAM 4.2
     */
    public synchronized void setSourceImage(RenderedImage sourceImage) {
        if (sourceImage != null) {
            setSourceImage(toMultiLevelImage(sourceImage));
        } else {
            //noinspection RedundantCast
            setSourceImage((MultiLevelImage) null);
        }
    }

    /**
     * Sets the source image associated with this {@code RasterDataNode}.
     *
     * @param sourceImage The source image.
     *                    Can be {@code null}. If so, {@link #isSourceImageSet()} will return {@code false}.
     * @since BEAM 4.6
     */
    public synchronized void setSourceImage(MultiLevelImage sourceImage) {
        final RenderedImage oldValue = this.sourceImage;
        //noinspection ObjectEquality
        if (oldValue != sourceImage) {
            this.sourceImage = sourceImage;
            resetGeophysicalImage();
            fireProductNodeChanged("sourceImage", oldValue, sourceImage);
        }
    }

    /**
     * Returns whether the geophysical image is set on this {@code RasterDataNode}.
     * <p>
     * This method belongs to preliminary API and may be removed or changed in the future.
     *
     * @return whether the geophysical image is set.
     * @since BEAM 4.6
     */
    public boolean isGeophysicalImageSet() {
        return geophysicalImage != null;
    }

    /**
     * @return The geophysical source image.
     * @since BEAM 4.5
     */
    public MultiLevelImage getGeophysicalImage() {
        if (geophysicalImage == null) {
            synchronized (this) {
                if (geophysicalImage == null) {
                    if (isScalingApplied()
                            || getDataType() == ProductData.TYPE_INT8
                            || getDataType() == ProductData.TYPE_UINT32) {
                        this.geophysicalImage = createGeophysicalImage();
                    } else {
                        this.geophysicalImage = getSourceImage();
                    }
                }
            }
        }
        return geophysicalImage;
    }

    /**
     * Gets the multi-level image (image pyramid) model that describes an image pyramid layout.
     * If this raster data node has a source image, its multi-level model will be returned.
     * Otherwise a new model will be created using {@link #createMultiLevelModel}.
     *
     * @return The multi-level image (image pyramid) model
     * @see #createMultiLevelModel
     */
    public MultiLevelModel getMultiLevelModel() {
        if (isSourceImageSet()) {
            return getSourceImage().getModel();
        }
        return createMultiLevelModel();
    }

    /**
     * Create a multi-level image model suited for source and geo-physical images returned by this
     * {@code RasterDataNode}
     *
     * @return A new suitable multi-level image (image pyramid) model
     * @see #getMultiLevelModel
     * @see Product#createMultiLevelModel()
     */
    public MultiLevelModel createMultiLevelModel() {
        int w = getRasterWidth();
        int h = getRasterHeight();
        AffineTransform i2mTransform = getImageToModelTransform();
        if (i2mTransform == null) {
            i2mTransform = new AffineTransform();
        }
        Product product = getProduct();
        if (product != null && product.getNumResolutionsMax() > 0) {
            return new DefaultMultiLevelModel(product.getNumResolutionsMax(), i2mTransform, w, h);
        } else {
            return new DefaultMultiLevelModel(i2mTransform, w, h);
        }
    }

    private MultiLevelImage createGeophysicalImage() {
        return new DefaultMultiLevelImage(new GenericMultiLevelSource(getSourceImage()) {

            @Override
            protected RenderedImage createImage(RenderedImage[] sourceImages, int level) {
                final RenderedImage source = sourceImages[0];
                final double factor = getScalingFactor();
                final double offset = getScalingOffset();
                final ScalingType scalingType = getScalingType();
                final InterpretationType interpretationType = getInterpretationType();
                final int sourceDataType = source.getSampleModel().getDataType();
                final int targetDataType = ReinterpretDescriptor.getTargetDataType(sourceDataType,
                                                                                   factor,
                                                                                   offset,
                                                                                   scalingType,
                                                                                   interpretationType);
                final SampleModel sampleModel = new SingleBandedSampleModel(targetDataType,
                                                                            source.getSampleModel().getWidth(),
                                                                            source.getSampleModel().getHeight());
                final ImageLayout imageLayout = ReinterpretDescriptor.createTargetImageLayout(source, sampleModel);
                return ReinterpretDescriptor.create(source, factor, offset, scalingType, interpretationType,
                                                    new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout));
            }
        });
    }

    private ScalingType getScalingType() {
        return isLog10Scaled() ? ReinterpretDescriptor.EXPONENTIAL : ReinterpretDescriptor.LINEAR;
    }

    private InterpretationType getInterpretationType() {
        switch (getDataType()) {
            case ProductData.TYPE_INT8:
                return ReinterpretDescriptor.INTERPRET_BYTE_SIGNED;
            case ProductData.TYPE_UINT32:
                return ReinterpretDescriptor.INTERPRET_INT_UNSIGNED;
            default:
                return ReinterpretDescriptor.AWT;
        }
    }

    private void resetGeophysicalImage() {
        geophysicalImage = null;
    }

    /**
     * Returns whether the valid mask image is set on this {@code RasterDataNode}.
     *
     * @return Whether the source image is set.
     * @since BEAM 4.5
     */
    public boolean isValidMaskImageSet() {
        return validMaskImage != null;
    }

    /**
     * Gets the valid-mask image associated with this {@code RasterDataNode}.
     * The image is based on the {@link #getValidMaskExpression()}
     *
     * @return The rendered image.
     * @since BEAM 4.2
     */
    public MultiLevelImage getValidMaskImage() {
        if (!isValidMaskImageSet() && isValidMaskUsed()) {
            synchronized (this) {
                if (!isValidMaskImageSet() && isValidMaskUsed()) {
                    validMaskImage = getProduct().getMaskImage(getValidMaskExpression(), this);
                }
            }
        }
        return validMaskImage;
    }

    public synchronized boolean isStxSet() {
        return stx != null;
    }

    /**
     * Returns a ROI for the validMask, if the a validMask is used.
     * Check before calling this method if a validMask is used.
     *
     * @return the ROI for the valid mask
     */
    private synchronized ROI getValidMaskROI() {
        if (validMaskROI == null) {
            synchronized (this) {
                if (validMaskROI == null) {
                    validMaskROI = new ROI(getValidMaskImage());
                }
            }
        }
        return validMaskROI;
    }

    /**
     * Gets the statistics. If statistics are not yet available,
     * the method will compute (possibly inaccurate) statistics and return those.
     * <p>
     * If accurate statistics are required, the {@link #getStx(boolean, com.bc.ceres.core.ProgressMonitor)}
     * shall be used instead.
     * <p>
     * This method belongs to preliminary API and may be removed or changed in the future.
     *
     * @return The statistics.
     * @see #getStx(boolean, com.bc.ceres.core.ProgressMonitor)
     * @see #setStx(Stx)
     * @since BEAM 4.2, revised in BEAM 4.5
     */
    public Stx getStx() {
        if (stx == null) {
            synchronized (this) {
                if (stx == null) {
                    getStx(false, ProgressMonitor.NULL);
                }
            }
        }
        return stx;
    }


    /**
     * Gets the statistics.
     * If the statistics have not been set before they are computed using the given progress monitor {@code pm} and then set.
     * This method belongs to preliminary API and may be removed or changed in the future.
     *
     * @param accurate If true, accurate statistics are computed.
     * @param pm       A progress monitor which is used to compute the new statistics, if required.
     * @return The statistics.
     * @since since BEAM 4.5
     */
    public synchronized Stx getStx(boolean accurate, ProgressMonitor pm) {
        if (stx == null || stx.getResolutionLevel() > 0 && accurate) {
            if (accurate) {
                setStx(computeStxImpl(0, pm));
            } else {
                final int levelCount = getSourceImage().getModel().getLevelCount();
                final int statisticsLevel = ImageManager.getInstance().getStatisticsLevel(this, levelCount);
                setStx(computeStxImpl(statisticsLevel, pm));
            }
        }
        return stx;
    }

    /**
     * Sets the statistics. It is the responsibility of the caller to ensure that the given statistics
     * are really related to this {@code RasterDataNode}'s raster data.
     * The method fires a property change event for the property {@link #PROPERTY_NAME_STX}.
     * This method belongs to preliminary API and may be removed or changed in the future.
     *
     * @param stx The statistics.
     * @since BEAM 4.2, revised in BEAM 4.5
     */
    public synchronized void setStx(Stx stx) {
        final Stx oldValue = this.stx;
        if (oldValue != stx) {
            this.stx = stx;
            fireProductNodeChanged(PROPERTY_NAME_STX, oldValue, stx);
        }
    }

    /**
     * Computes the statistics. May be overridden.
     * This method belongs to preliminary API and may be removed or changed in the future.
     *
     * @param level The resolution level.
     * @param pm    A progress monitor.
     * @return The statistics.
     * @since BEAM 4.5
     */
    protected Stx computeStxImpl(int level, ProgressMonitor pm) {
        return new StxFactory().withResolutionLevel(level).create(this, pm);
    }

    /**
     * Gets the shape of the area where this raster data contains valid samples.
     * The method returns <code>null</code>, if the entire raster contains valid samples.
     *
     * @return The shape of the area where the raster data has samples, can be {@code null}.
     * @since BEAM 4.7
     */
    public Shape getValidShape() {
        return validMaskImage != null ? validMaskImage.getImageShape(0) : null;
    }

    /**
     * Finds the first associated ancillary band for the specified relations.
     *
     * @param relations Names of relations such as {@code "uncertainty"}, {@code "variance"}, or {@code null} (any).
     * @return The associated ancillary band or {@code null}.
     * @since SNAP 2.0
     */
    public RasterDataNode getAncillaryVariable(String... relations) {
        RasterDataNode[] variables = getAncillaryVariables(relations);
        return variables.length > 0 ? variables[0] : null;
    }

    /**
     * Finds any associated ancillary band for the specified relation.
     *
     * @param relations Names of relations such as {@code "uncertainty"}, {@code "variance"}, or {@code null} (any).
     * @return The associated ancillary bands or an empty array.
     * @since SNAP 2.0
     */
    public RasterDataNode[] getAncillaryVariables(String... relations) {
        if (ancillaryVariables == null) {
            return new RasterDataNode[0];
        }
        if (relations.length == 0) {
            return ancillaryVariables.toArray(new RasterDataNode[ancillaryVariables.getNodeCount()]);
        }
        assertRelationsAreAllNoneNull(relations);
        ArrayList<RasterDataNode> rasterDataNodes = new ArrayList<>();
        for (RasterDataNode ancillaryVariable : ancillaryVariables.toArray(new RasterDataNode[ancillaryVariables.getNodeCount()])) {
            String[] ancillaryRelations = ancillaryVariable.getAncillaryRelations();
            if (ancillaryRelations == null) {
                ancillaryRelations = new String[0];
            }
            for (String relation1 : relations) {
                if (equalAncillaryRelations(relation1, ancillaryRelations) && !rasterDataNodes.contains(ancillaryVariable)) {
                    rasterDataNodes.add(ancillaryVariable);
                }
            }
        }
        return rasterDataNodes.toArray(new RasterDataNode[rasterDataNodes.size()]);
    }

    // Compare "rel" attribute according to NetCDF-U convention
    private static boolean equalAncillaryRelations(String relation1, String... relations2) {
        if (relations2.length == 0) {
            return relation1.equalsIgnoreCase("uncertainty");
        }
        for (String relation2 : relations2) {
            if (relation1.equalsIgnoreCase(relation2)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds an associated ancillary variable and sets its relation names.
     *
     * @param variable  The associated ancillary variable.
     * @param relations The names of the relations, may be {@code "uncertainty"}, {@code "variance"}, or {@code null} (not set).
     * @since SNAP 2.0
     */
    public void addAncillaryVariable(RasterDataNode variable, String... relations) {
        boolean change = false;
        if (ancillaryVariables == null) {
            ancillaryVariables = new ProductNodeGroup<>(this, "ancillaryVariables", false);
        }
        if (!ancillaryVariables.contains(variable)) {
            change = ancillaryVariables.add(variable);
        }
        if (relations.length > 0) {
            assertRelationsAreAllNoneNull(relations);
            for (String relation : relations) {
                if (!equalAncillaryRelations(relation, variable.getAncillaryRelations())) {
                    change = true;
                }
            }
            variable.setAncillaryRelations(relations);
        }
        if (change) {
            fireProductNodeChanged(PROPERTY_NAME_ANCILLARY_VARIABLES, ancillaryVariables, ancillaryVariables);
        }

        Product product = getProduct();
        if (ancillaryVariables.getNodeCount() > 0 && ancillaryBandRemover == null && product != null) {
            ancillaryBandRemover = new AncillaryBandRemover();
            product.addProductNodeListener(ancillaryBandRemover);
        }
    }

    /**
     * Removes an associated ancillary variable.
     *
     * @param variable The associated ancillary variable.
     * @since SNAP 2.0
     */
    public void removeAncillaryVariable(RasterDataNode variable) {
        if (ancillaryVariables != null && ancillaryVariables.remove(variable)) {
            fireProductNodeChanged(PROPERTY_NAME_ANCILLARY_VARIABLES, ancillaryVariables, ancillaryVariables);
        }
    }

    /**
     * Gets the names of an ancillary relations to another raster data node.
     * See NetCDF-U 'rel' attribute.
     *
     * @return The names of an ancillary relations to another raster data node, or an empty array.
     * @see #addAncillaryVariable(RasterDataNode, String...)
     * @see #removeAncillaryVariable(RasterDataNode)
     * @see #getAncillaryVariable(String...)
     * @since SNAP 2.0
     */
    public String[] getAncillaryRelations() {
        return ancillaryRelations != null ? ancillaryRelations.clone() : new String[0];
    }

    /**
     * Sets the names of an ancillary relations to another raster data node.
     * See NetCDF-U 'rel' attribute.
     *
     * @param relations The names of an ancillary relations.
     * @see #addAncillaryVariable(RasterDataNode, String...)
     * @see #getAncillaryVariable(String...)
     * @since SNAP 2.0
     */
    public void setAncillaryRelations(String... relations) {
        assertRelationsAreAllNoneNull(relations);
        String[] oldValue = getAncillaryRelations();
        this.ancillaryRelations = relations;
        if (!ObjectUtils.equalObjects(oldValue, this.ancillaryRelations)) {
            fireProductNodeChanged(PROPERTY_NAME_ANCILLARY_RELATIONS, oldValue, getAncillaryRelations());
        }
    }

    private void assertRelationsAreAllNoneNull(String[] relations) {
        for (int i = 0; i < relations.length; i++) {
            Assert.argument(relations[i] != null, "relations has null element at index " + i);
        }
    }

    private MultiLevelImage toMultiLevelImage(RenderedImage sourceImage) {
        MultiLevelImage mli;
        if (sourceImage instanceof MultiLevelImage) {
            mli = (MultiLevelImage) sourceImage;
        } else {
            MultiLevelModel model = createMultiLevelModel();
            mli = new DefaultMultiLevelImage(new DefaultMultiLevelSource(sourceImage, model));
        }
        return mli;
    }


    /**
     * Processes the raster's data.
     * <p>
     * Users of this method may also consider implementing a GPF {@code org.esa.snap.core.gpf.Operator} or a {@link SingleBandedOpImage} instead.
     *
     * @param message   a task description
     * @param processor the raster processor
     * @param pm        a progress monitor
     */
    public void processRasterData(String message, RasterDataProcessor processor, ProgressMonitor pm) throws
                                                                                                     IOException {
        Debug.trace("RasterDataNode.processRasterData: " + message);
        int readBufferLineCount = getReadBufferLineCount();
        ProductData readBuffer = null;
        final int width = getRasterWidth();
        final int height = getRasterHeight();
        int numReadsMax = height / readBufferLineCount;
        if (numReadsMax * readBufferLineCount < height) {
            numReadsMax++;
        }
        Debug.trace(String.format("RasterDataNode.processRasterData: numReadsMax=%d, readBufferLineCount=%d",
                                  numReadsMax, readBufferLineCount));
        pm.beginTask(message, numReadsMax * 2);
        try {
            for (int i = 0; i < numReadsMax; i++) {
                final int y0 = i * readBufferLineCount;
                final int restheight = height - y0;
                final int linesToRead = restheight > readBufferLineCount ? readBufferLineCount : restheight;
                readBuffer = recycleOrCreateBuffer(getDataType(), width * linesToRead, readBuffer);
                readRasterData(0, y0, width, linesToRead, readBuffer, SubProgressMonitor.create(pm, 1));
                processor.processRasterDataBuffer(readBuffer, y0, linesToRead, SubProgressMonitor.create(pm, 1));
                if (pm.isCanceled()) {
                    break;
                }
            }
        } finally {
            pm.done();
        }
        Debug.trace("RasterDataNode.processRasterData: done");
    }

    /**
     * A raster data processor which is called for a set of raster lines to be processed.
     * <p>
     * For maximum performance, implementors may also consider implementing a GPF {@code org.esa.snap.core.gpf.Operator} or a
     * {@link SingleBandedOpImage} instead.
     */
    public interface RasterDataProcessor {

        /**
         * Processes some input raster lines.
         *
         * @param buffer   The input data buffer containing the data for {@code numLines} raster lines.
         * @param y0       The index of the first line.
         * @param numLines The number of lines.
         * @param pm       a progress monitor
         */
        void processRasterDataBuffer(ProductData buffer, int y0, int numLines, ProgressMonitor pm) throws IOException;
    }

    private class AncillaryBandRemover extends ProductNodeListenerAdapter {

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            if (ancillaryVariables != null
                    && event.getGroup() != ancillaryVariables
                    && event.getSourceNode() instanceof RasterDataNode) {
                ancillaryVariables.remove((RasterDataNode) event.getSourceNode());
            }
        }
    }
}
