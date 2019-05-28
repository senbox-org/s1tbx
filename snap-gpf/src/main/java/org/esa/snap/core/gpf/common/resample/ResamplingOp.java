package org.esa.snap.core.gpf.common.resample;

import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import com.vividsolutions.jts.geom.Geometry;
import org.esa.snap.core.datamodel.AbstractGeoCoding;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.Scene;
import org.esa.snap.core.datamodel.SceneFactory;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.image.FillConstantOpImage;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.image.ReplaceValueOpImage;
import org.esa.snap.core.image.ResolutionLevel;
import org.esa.snap.core.transform.MathTransform2D;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.jai.JAIUtils;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * @author Tonio Fincke
 */
@OperatorMetadata(alias = "Resample",
        category = "Raster/Geometric",
        version = "2.0",
        authors = "Tonio Fincke",
        copyright = "(c) 2016 by Brockmann Consult",
        description = "Resampling of a multi-size source product to a single-size target product.")
public class ResamplingOp extends Operator {

    private static final String NAME_EXTENSION = "resampled";

    @SourceProduct(description = "The source product which is to be resampled.", label = "Name")
    Product sourceProduct;

    @TargetProduct(description = "The resampled target product.")
    Product targetProduct;

    @Parameter(alias = "referenceBand", label = "Reference band", description = "The name of the reference band. " +
            "All other bands will be re-sampled to match its size and resolution. Either this or targetResolution" +
            "or targetWidth and targetHeight must be set.", rasterDataNodeType = Band.class)
    String referenceBandName;

    @Parameter(alias = "targetWidth", label = "Target width", description = "The width that all bands of the " +
            "target product shall have. If this is set, targetHeight must be set, too. " +
            "Either this and targetHeight or referenceBand or targetResolution must be set.")
    Integer targetWidth;

    @Parameter(alias = "targetHeight", label = "Target height", description = "The height that all bands of the " +
            "target product shall have. If this is set, targetWidth must be set, too. " +
            "Either this and targetWidth or referenceBand or targetResolution must be set.")
    Integer targetHeight;

    @Parameter(alias = "targetResolution", label = "Target resolution", description = "The resolution that all bands of the " +
            "target product shall have. The same value will be applied to scale image widths and heights. " +
            "Either this or referenceBand or targetwidth and targetHeight must be set.")
    Integer targetResolution;

    @Parameter(alias = "upsampling",
            label = "Upsampling method",
            description = "The method used for interpolation (upsampling to a finer resolution).",
            valueSet = {"Nearest", "Bilinear", "Bicubic"/*, "Cubic_Convolution"*/}, //TODO this has to be extended in the future with upsampling registry
            defaultValue = "Nearest"
    )
    private String upsamplingMethod;

    @Parameter(alias = "downsampling",
            label = "Downsampling method",
            description = "The method used for aggregation (downsampling to a coarser resolution).",
            valueSet = {"First", "Min", "Max", "Mean", "Median"}, //TODO this has to be extended in the future with downsampling registry
            defaultValue = "First")
    private String downsamplingMethod;

    @Parameter(alias = "flagDownsampling",
            label = "Flag downsampling method",
            description = "The method used for aggregation (downsampling to a coarser resolution) of flags.",
            valueSet = {"First", "FlagAnd", "FlagOr", "FlagMedianAnd", "FlagMedianOr"}, //TODO this has to be extended with downsampling registry
            defaultValue = "First")
    private String flagDownsamplingMethod;

    @Parameter(alias = "resamplingPreset",
            label = "Resampling Preset",
            description = "The resampling preset. This will over rules the settings for upsampling, downsampling and flagDownsampling.",
            defaultValue = "")
    private String resamplingPreset;
    private ResamplingPreset selectedResamplingPreset = null;

    //over rules resamplingPreset
    @Parameter(alias = "bandResamplings",
            label = "Band Resamplings",
            description = "The band resamplings. This will over rules the settings for resamplingPreset.",
            defaultValue = "")
    private String bandResamplings;



    @Parameter(label = "Resample on pyramid levels (for faster imaging)", defaultValue = "true",
            description = "This setting will increase performance when viewing the image, but accurate resamplings " +
                    "are only retrieved when zooming in on a pixel.")
//            description = "<html>When this is set, the resampling will be performed on level images, not on the underlying " +
//                    "source image.<br/>This will significantly increase performance when viewing the image, but the pixel values " +
//                    "are only approximations. <br/>To get the value at the lowest level, you need to request it by, e.g.,  " +
//                    "zooming in close to the area around the pixel in question. <br/>" +
//                    "When performing operations, the image on level 0 will be used, so this setting will not have an effect.</html>")
    private boolean resampleOnPyramidLevels;

    private AggregationType aggregationType;
    private AggregationType flagAggregationType;

    private int referenceWidth;
    private int referenceHeight;
    private Dimension referenceTileSize;
    private AffineTransform referenceImageToModelTransform;
    private MultiLevelModel referenceMultiLevelModel;

    @Override
    public void initialize() throws OperatorException {
        if (!allNodesHaveIdentitySceneTransform(sourceProduct)) {
            throw new OperatorException("Not all nodes have identity model-to-scene transform.");
        }
        validateInterpolationParameter();
        setReferenceValues();
        setResamplingTypes();
        loadResamplingPreset();
        targetProduct = new Product(sourceProduct.getName() + "_" + NAME_EXTENSION, sourceProduct.getProductType(),
                                    referenceWidth, referenceHeight);
        resampleBands();
        resampleTiePointGrids();
        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
        ProductUtils.copyIndexCodings(sourceProduct, targetProduct);
        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyTimeInformation(sourceProduct, targetProduct);
        transferGeoCoding(targetProduct);
        copyMasks(sourceProduct, targetProduct);
        ProductUtils.copyVectorData(sourceProduct, targetProduct);
        targetProduct.setAutoGrouping(sourceProduct.getAutoGrouping());
        targetProduct.setPreferredTileSize(referenceTileSize);
    }

    private void transferGeoCoding(Product targetProduct) {
        final Scene srcScene = SceneFactory.createScene(sourceProduct);
        final Scene destScene = SceneFactory.createScene(targetProduct);
        if (srcScene != null && destScene != null) {
            final GeoCoding sourceGeoCoding = srcScene.getGeoCoding();
            if (sourceGeoCoding == null) {
                targetProduct.setSceneGeoCoding(null);
            } else if (sourceGeoCoding instanceof CrsGeoCoding) {
                final CrsGeoCoding srcCrsGeoCoding = (CrsGeoCoding) sourceGeoCoding;
                final RasterDataNode anyRasterDataNode = getAnyRasterDataNode(targetProduct);
                if (anyRasterDataNode != null) {
                    final AffineTransform destImage2MapTransform = anyRasterDataNode.getImageToModelTransform();
                    AffineTransform destImageToMapTransform = new AffineTransform(destImage2MapTransform);
                    try {
                        final CrsGeoCoding destCrsGeoCoding = new CrsGeoCoding(srcCrsGeoCoding.getMapCRS(),
                                                                               anyRasterDataNode.getSourceImage().getBounds(),
                                                                               destImageToMapTransform);
                        targetProduct.setSceneGeoCoding(destCrsGeoCoding);
                    } catch (FactoryException | TransformException e) {
                        //do not set geo-coding then
                    }
                }
            } else if (sourceGeoCoding instanceof AbstractGeoCoding) {
                AbstractGeoCoding abstractGeoCoding = (AbstractGeoCoding) sourceGeoCoding;
                abstractGeoCoding.transferGeoCoding(srcScene, destScene, null);
            }
        }
    }

    private RasterDataNode getAnyRasterDataNode(Product product) {
        RasterDataNode node = null;
        if (product != null) {
            final ProductNodeGroup<Band> bandGroup = product.getBandGroup();
            if (bandGroup.getNodeCount() == 0) {
                final ProductNodeGroup<TiePointGrid> tiePointGridGroup = product.getTiePointGridGroup();
                if (tiePointGridGroup.getNodeCount() > 0) {
                    node = tiePointGridGroup.get(0);
                }
            } else {
                node = bandGroup.get(0);
            }
        }
        return node;
    }

    //convenience method to increase speed for band maths type masks
    private void copyMasks(Product sourceProduct, Product targetProduct) {
        final ProductNodeGroup<Mask> sourceMaskGroup = sourceProduct.getMaskGroup();
        for (int i = 0; i < sourceMaskGroup.getNodeCount(); i++) {
            final Mask mask = sourceMaskGroup.get(i);
            final Mask.ImageType imageType = mask.getImageType();
            if (imageType.getName().equals(Mask.BandMathsType.TYPE_NAME)) {
                String expression = Mask.BandMathsType.getExpression(mask);
                final Mask targetMask = Mask.BandMathsType.create(mask.getName(), mask.getDescription(),
                                                                  targetProduct.getSceneRasterWidth(),
                                                                  targetProduct.getSceneRasterHeight(), expression,
                                                                  mask.getImageColor(), mask.getImageTransparency());
                targetProduct.addMask(targetMask);
            } else if (imageType.getName().equals(Mask.VectorDataType.TYPE_NAME)) {
                final VectorDataNode vectorDataMaskNode = Mask.VectorDataType.getVectorData(mask);
                final String vectorDataNodeName = vectorDataMaskNode.getName();
                //deal with case that a mask's vector data node is not in a product's vector data group
                if (sourceProduct.getVectorDataGroup().get(vectorDataNodeName) == null) {
                    final VectorDataNode targetVectorDataNode = transferVectorDataNode(targetProduct, vectorDataMaskNode);
                    if (targetVectorDataNode != null) {
                        targetProduct.addMask(mask.getName(), targetVectorDataNode, mask.getDescription(), mask.getImageColor(),
                                              mask.getImageTransparency());
                    }
                }
            } else if (imageType.canTransferMask(mask, targetProduct)) {
                imageType.transferMask(mask, targetProduct);
            }
        }
    }

    private VectorDataNode transferVectorDataNode(Product targetProduct, VectorDataNode sourceVDN) {
        AffineTransform referenceModelToImageTransform;
        try {
            referenceModelToImageTransform = referenceImageToModelTransform.createInverse();
        } catch (NoninvertibleTransformException e) {
            return null;
        }
        final GeometryCoordinateSequenceTransformer transformer = new GeometryCoordinateSequenceTransformer();
        final AffineTransform targetImageToModelTransform = Product.findImageToModelTransform(targetProduct.getSceneGeoCoding());
        referenceModelToImageTransform.concatenate(targetImageToModelTransform);
        final AffineTransform2D mathTransform = new AffineTransform2D(referenceModelToImageTransform);
        transformer.setMathTransform(mathTransform);
        final FeatureCollection<SimpleFeatureType, SimpleFeature> sourceCollection = sourceVDN.getFeatureCollection();
        final DefaultFeatureCollection targetCollection = new DefaultFeatureCollection(sourceCollection.getID(), sourceCollection.getSchema());
        final FeatureIterator<SimpleFeature> featureIterator = sourceCollection.features();
        while (featureIterator.hasNext()) {
            final SimpleFeature srcFeature = featureIterator.next();
            final Object defaultGeometry = srcFeature.getDefaultGeometry();
            if (defaultGeometry != null && defaultGeometry instanceof Geometry) {
                try {
                    final Geometry transformedGeometry = transformer.transform((Geometry) defaultGeometry);
                    final SimpleFeature targetFeature = SimpleFeatureBuilder.copy(srcFeature);
                    targetFeature.setDefaultGeometry(transformedGeometry);
                    targetCollection.add(targetFeature);
                } catch (TransformException e) {
                    return null;
                }
            }
        }
        VectorDataNode targetVDN = new VectorDataNode(sourceVDN.getName(), sourceCollection.getSchema());
        targetVDN.getFeatureCollection().addAll((FeatureCollection<?, ?>) targetCollection);
        targetVDN.setDefaultStyleCss(sourceVDN.getDefaultStyleCss());
        targetVDN.setDescription(sourceVDN.getDescription());
        targetVDN.setOwner(targetProduct);
        return targetVDN;
    }

    public static boolean canBeApplied(Product product) {
        return allNodesHaveIdentitySceneTransform(product);
    }

    static boolean allNodesHaveIdentitySceneTransform(Product product) {
        final ProductNodeGroup<Band> bandGroup = product.getBandGroup();
        for (int i = 0; i < bandGroup.getNodeCount(); i++) {
            if (bandGroup.get(i).getModelToSceneTransform() != MathTransform2D.IDENTITY) {
                return false;
            }
        }
        final ProductNodeGroup<TiePointGrid> tiePointGridGroup = product.getTiePointGridGroup();
        for (int i = 0; i < tiePointGridGroup.getNodeCount(); i++) {
            if (tiePointGridGroup.get(i).getModelToSceneTransform() != MathTransform2D.IDENTITY) {
                return false;
            }
        }
        return true;
    }

    private void resampleTiePointGrids() {
        final ProductNodeGroup<TiePointGrid> tiePointGridGroup = sourceProduct.getTiePointGridGroup();
        AffineTransform targetImageToModelTransform;
        double scaledReferenceOffsetX;
        double scaledReferenceOffsetY;
        if (sourceProduct.getSceneGeoCoding() instanceof CrsGeoCoding) {
            targetImageToModelTransform = referenceImageToModelTransform;
            scaledReferenceOffsetX = 0;
            scaledReferenceOffsetY = 0;
        } else {
            targetImageToModelTransform = new AffineTransform();
            scaledReferenceOffsetX = referenceImageToModelTransform.getTranslateX() / referenceImageToModelTransform.getScaleX();
            scaledReferenceOffsetY = referenceImageToModelTransform.getTranslateY() / referenceImageToModelTransform.getScaleY();
        }
        AffineTransform transform;
        for (int i = 0; i < tiePointGridGroup.getNodeCount(); i++) {
            try {
                transform = new AffineTransform(referenceImageToModelTransform.createInverse());
            } catch (NoninvertibleTransformException e) {
                throw new OperatorException("Cannot resample: " + e.getMessage());
            }
            final TiePointGrid grid = tiePointGridGroup.get(i);
            final AffineTransform gridTransform = grid.getImageToModelTransform();
            transform.concatenate(gridTransform);
            double subSamplingX = grid.getSubSamplingX() * transform.getScaleX();
            double subSamplingY = grid.getSubSamplingY() * transform.getScaleY();
            double offsetX = (grid.getOffsetX() * transform.getScaleX()) - scaledReferenceOffsetX;
            double offsetY = (grid.getOffsetY() * transform.getScaleY()) - scaledReferenceOffsetY;
            final float[] srcTiePoints = grid.getTiePoints();
            final float[] destTiePoints = new float[srcTiePoints.length];
            System.arraycopy(srcTiePoints, 0, destTiePoints, 0, srcTiePoints.length);
            final TiePointGrid resampledGrid = new TiePointGrid(grid.getName(), grid.getGridWidth(),
                                                                grid.getGridHeight(), offsetX, offsetY,
                                                                subSamplingX, subSamplingY, destTiePoints,
                                                                grid.getDiscontinuity());
            ProductUtils.copyRasterDataNodeProperties(grid, resampledGrid);
            resampledGrid.setImageToModelTransform(targetImageToModelTransform);
            targetProduct.addTiePointGrid(resampledGrid);
        }
    }

    private void resampleBands() {
        final ProductNodeGroup<Band> sourceBands = sourceProduct.getBandGroup();
        MultiLevelModel targetMultiLevelModel;
        if (sourceProduct.getSceneGeoCoding() instanceof CrsGeoCoding) {
            targetMultiLevelModel = referenceMultiLevelModel;
        } else {
            targetMultiLevelModel = new DefaultMultiLevelModel(new AffineTransform(), referenceWidth, referenceHeight);
        }
        for (int i = 0; i < sourceBands.getNodeCount(); i++) {
            Band sourceBand = sourceBands.get(i);
            int dataBufferType = ImageManager.getDataBufferType(sourceBand.getDataType());
            Band targetBand;
            AffineTransform sourceTransform = sourceBand.getImageToModelTransform();
            final boolean isVirtualBand = sourceBand instanceof VirtualBand;
            if (isVirtualBand) {
                targetBand = ProductUtils.copyVirtualBand(targetProduct, (VirtualBand) sourceBand, sourceBand.getName(), true);
            } else if ((sourceTransform.getScaleX() != referenceImageToModelTransform.getScaleX() ||
                    sourceTransform.getScaleY() != referenceImageToModelTransform.getScaleY() ||
                    sourceBand.getRasterWidth() != referenceWidth ||
                    sourceBand.getRasterHeight() != referenceHeight ||
                    sourceTransform.getTranslateX() != referenceImageToModelTransform.getTranslateX() ||
                    sourceTransform.getTranslateX() != referenceImageToModelTransform.getTranslateX())) {
                targetBand = new Band(sourceBand.getName(), sourceBand.getDataType(), referenceWidth, referenceHeight);
                MultiLevelImage targetImage = sourceBand.getSourceImage();
                MultiLevelImage sourceImage = createMaskedImage(sourceBand, Double.NaN);
                final boolean replacedNoData = sourceImage != sourceBand.getSourceImage();
                if (replacedNoData) {
                    dataBufferType = DataBuffer.TYPE_DOUBLE;
                }
                if (Math.abs(sourceTransform.getScaleX()) >= Math.abs(referenceImageToModelTransform.getScaleX()) ||
                        Math.abs(sourceTransform.getScaleY()) >= Math.abs(referenceImageToModelTransform.getScaleY())) {
                    targetImage = createInterpolatedImage(sourceBand, sourceImage, dataBufferType, sourceBand.getNoDataValue(),
                            sourceBand.getImageToModelTransform(), sourceBand.isFlagBand() || sourceBand.isIndexBand(), referenceMultiLevelModel);
                } else if (Math.abs(sourceTransform.getScaleX()) <= Math.abs(referenceImageToModelTransform.getScaleX()) ||
                        Math.abs(sourceTransform.getScaleY()) <= Math.abs(referenceImageToModelTransform.getScaleY())) {
                    targetImage = createAggregatedImage(sourceBand, sourceImage, dataBufferType, sourceBand.getNoDataValue(),
                            sourceBand.isFlagBand(), referenceMultiLevelModel);
                } else if (Math.abs(sourceTransform.getScaleX()) < Math.abs(referenceImageToModelTransform.getScaleX())) {
                    AffineTransform intermediateTransform = new AffineTransform(
                            referenceImageToModelTransform.getScaleX(), referenceImageToModelTransform.getShearX(), sourceTransform.getShearY(),
                            sourceTransform.getScaleY(), referenceImageToModelTransform.getTranslateX(), sourceTransform.getTranslateY());
                    final DefaultMultiLevelModel intermediateMultiLevelModel =
                            new DefaultMultiLevelModel(intermediateTransform, referenceWidth, sourceBand.getRasterHeight());
                    targetImage = createAggregatedImage(sourceBand, targetImage, dataBufferType, sourceBand.getNoDataValue(),
                                                        sourceBand.isFlagBand(), intermediateMultiLevelModel
                    );
                    targetImage = createInterpolatedImage(sourceBand, targetImage, dataBufferType,sourceBand.getNoDataValue(),
                                                          intermediateTransform,
                                                          sourceBand.isFlagBand() || sourceBand.isIndexBand(), intermediateMultiLevelModel);
                } else if (Math.abs(sourceTransform.getScaleY()) < Math.abs(referenceImageToModelTransform.getScaleY())) {
                    AffineTransform intermediateTransform = new AffineTransform(
                            sourceTransform.getScaleX(), sourceTransform.getShearX(), referenceImageToModelTransform.getShearY(),
                            referenceImageToModelTransform.getScaleY(), sourceTransform.getTranslateX(), referenceImageToModelTransform.getTranslateY());
                    final DefaultMultiLevelModel intermediateMultiLevelModel =
                            new DefaultMultiLevelModel(intermediateTransform, sourceBand.getRasterWidth(), referenceHeight);
                    targetImage = createAggregatedImage(sourceBand, targetImage, dataBufferType, sourceBand.getNoDataValue(),
                                                        sourceBand.isFlagBand(), intermediateMultiLevelModel
                    );
                    targetImage = createInterpolatedImage(sourceBand, targetImage, dataBufferType, sourceBand.getNoDataValue(),
                                                          intermediateTransform,
                                                          sourceBand.isFlagBand() || sourceBand.isIndexBand(), intermediateMultiLevelModel);
                }
                if (replacedNoData) {
                    targetImage = replaceNoDataValue(targetBand, targetImage, Double.NaN, sourceBand.getNoDataValue());
                }
                targetBand.setSourceImage(adjustImageToModelTransform(targetImage, targetMultiLevelModel));
                targetProduct.addBand(targetBand);
            } else {
                    targetBand = ProductUtils.copyBand(sourceBand.getName(), sourceProduct, targetProduct, false);
                    targetBand.setSourceImage(adjustImageToModelTransform(sourceBand.getSourceImage(), targetMultiLevelModel));
                }
            ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);
        }
    }

    private static MultiLevelImage createMaskedImage(final RasterDataNode node, Number maskValue) {
        MultiLevelImage varImage = node.getSourceImage();
        if (node.getValidPixelExpression() != null) {
            varImage = replaceInvalidValuesByNaN(node, varImage, node.getValidMaskImage(), maskValue);
        }
        if (node.isNoDataValueUsed() && node.isNoDataValueSet()) {
            varImage = replaceNoDataValue(node, varImage, node.getNoDataValue(), maskValue);
        }
        return varImage;
    }

    private static MultiLevelImage replaceInvalidValuesByNaN(final RasterDataNode rasterDataNode, final MultiLevelImage srcImage,
                                                             final MultiLevelImage maskImage, final Number fillValue) {

        final MultiLevelModel multiLevelModel = rasterDataNode.getMultiLevelModel();
        return new DefaultMultiLevelImage(new AbstractMultiLevelSource(multiLevelModel) {

            @Override
            public RenderedImage createImage(int sourceLevel) {
                return new FillConstantOpImage(srcImage.getImage(sourceLevel), maskImage.getImage(sourceLevel), fillValue);
            }
        });
    }

    private static MultiLevelImage replaceNoDataValue(final RasterDataNode rasterDataNode, final MultiLevelImage srcImage,
                                                      final double noDataValue, final Number newValue) {

        final MultiLevelModel multiLevelModel = rasterDataNode.getMultiLevelModel();
        final int targetDataType = ImageManager.getDataBufferType(rasterDataNode.getDataType());
        return new DefaultMultiLevelImage(new AbstractMultiLevelSource(multiLevelModel) {

            @Override
            public RenderedImage createImage(int sourceLevel) {
                return new ReplaceValueOpImage(srcImage.getImage(sourceLevel), noDataValue, newValue, targetDataType);
            }
        });
    }

    private RenderedImage adjustImageToModelTransform(final MultiLevelImage image, MultiLevelModel model) {
        MultiLevelModel actualModel = model;
        if (model.getLevelCount() > image.getModel().getLevelCount()) {
            actualModel = new DefaultMultiLevelModel(image.getModel().getLevelCount(), model.getImageToModelTransform(0),
                                                     image.getWidth(), image.getHeight());
        }
        final AbstractMultiLevelSource source = new AbstractMultiLevelSource(actualModel) {
            @Override
            protected RenderedImage createImage(int level) {
                return image.getImage(level);
            }
        };
        return new DefaultMultiLevelImage(source);
    }

    private MultiLevelImage createInterpolatedImage(RasterDataNode sourceRDN, MultiLevelImage sourceImage, int dataBufferType, double noDataValue,
                                                    AffineTransform sourceImageToModelTransform, boolean isFlagOrIndexBand, MultiLevelModel referenceModel) {
        Interpolation interpolation = null;
        Upsampling upsampling = null;

        String bandUpsamplingMethod = upsamplingMethod;

        if(isFlagOrIndexBand) {
            bandUpsamplingMethod = "Nearest";
        }

        //Load ResamplingPreset and set upsampling methid if it is valid
        if(selectedResamplingPreset != null) {
            BandResamplingPreset bandResamplingPreset = selectedResamplingPreset.getBandResamplingPreset(sourceRDN.getName());
            if (bandResamplingPreset != null) {
                String bandUpsamplingMethodTemp = bandResamplingPreset.getUpsamplingAlias();
                if(GPF.getDefaultInstance().getUpsamplerSpiRegistry().getAliases().contains(bandUpsamplingMethodTemp)) {
                    bandUpsamplingMethod = bandUpsamplingMethodTemp;
                }
            }
        }

        //over rules resampling preset with bandResampling if valid
        ResamplingPreset myResamplingPreset2 = ResamplingPreset.loadResamplingPreset(bandResamplings,"bandResampling");
        if(myResamplingPreset2 != null) {
            BandResamplingPreset bandResamplingPreset2 = myResamplingPreset2.getBandResamplingPreset(sourceRDN.getName());
            if (bandResamplingPreset2 != null) {
                String bandUpsamplingMethodTemp = bandResamplingPreset2.getUpsamplingAlias();
                if(GPF.getDefaultInstance().getUpsamplerSpiRegistry().getAliases().contains(bandUpsamplingMethodTemp)) {
                    bandUpsamplingMethod = bandUpsamplingMethodTemp;
                }
            }
        }

        try {
            //do it using Interpolation scaler
            interpolation = getInterpolation(bandUpsamplingMethod);
        } catch (IllegalArgumentException e){
            //do it using similar method to downsampling. Using InterpolatedOpImage
            interpolation = null;
            upsampling = GPF.getDefaultInstance().getUpsamplerSpiRegistry().getUpsamplerSpi(upsamplingMethod).createUpsampling();
        }

        //do it using Interpolation scaler. This is how it was done for Nearest, Bilinear and Bicubic
        if(interpolation != null) {
            return Resample.createInterpolatedMultiLevelImage(sourceImage, noDataValue, sourceImageToModelTransform,
                                                              referenceWidth, referenceHeight, referenceTileSize,
                                                              referenceMultiLevelModel, interpolation);
        }


        ////////////////////////
        //New implementation for v7.0 (similar to createAggregatedImage). Used with interpolation different to Nearest, Bilinear, Bicubic
        ////////////////////////
        MultiLevelSource source;
        Upsampling finalUpsampling = upsampling;
        if (resampleOnPyramidLevels) {

            source = new AbstractMultiLevelSource(referenceModel) {
                @Override
                protected RenderedImage createImage(int targetLevel) {
                    final MultiLevelModel targetModel = getModel();
                    final double targetScale = targetModel.getScale(targetLevel);
                    final MultiLevelModel sourceModel = sourceImage.getModel();
                    final int sourceLevel = sourceModel.getLevel(targetScale);
                    final RenderedImage sourceLevelImage = sourceImage.getImage(sourceLevel);
                    final ResolutionLevel resolutionLevel = ResolutionLevel.create(getModel(), targetLevel);
                    final ImageLayout imageLayout = ImageManager.createSingleBandedImageLayout(dataBufferType, null,
                                                                                               referenceWidth,
                                                                                               referenceHeight,
                                                                                               referenceTileSize,
                                                                                               resolutionLevel);
                    try {
                        return new InterpolatedOpImage(sourceRDN, sourceLevelImage, imageLayout, noDataValue,
                                                      dataBufferType, finalUpsampling,
                                                     sourceModel.getImageToModelTransform(sourceLevel),
                                                     targetModel.getImageToModelTransform(targetLevel));
                    } catch (NoninvertibleTransformException e) {
                        throw new OperatorException("Could not downsample band image");
                    }
                }
            };
        } else {
            try {
                final ImageLayout imageLayout = ImageManager.createSingleBandedImageLayout(dataBufferType, null,
                                                                                           referenceWidth, referenceHeight,
                                                                                           referenceTileSize,
                                                                                           ResolutionLevel.MAXRES);
                final RenderedImage image = new InterpolatedOpImage(sourceRDN, sourceImage, imageLayout, noDataValue, dataBufferType, finalUpsampling,
                                                                  sourceImage.getModel().getImageToModelTransform(0),
                                                                  referenceModel.getImageToModelTransform(0));
                source = new DefaultMultiLevelSource(image, referenceModel);
            } catch (NoninvertibleTransformException e) {
                throw new OperatorException("Could not downsample band image");
            }
        }
        return new DefaultMultiLevelImage(source);
    }

    private Interpolation getInterpolation() {
        int interpolation = getInterpolationType();
        return Interpolation.getInstance(interpolation);
    }

    private int getInterpolationType() {
        final int interpolationType;
        if ("Nearest".equalsIgnoreCase(upsamplingMethod)) {
            interpolationType = Interpolation.INTERP_NEAREST;
        } else if ("Bilinear".equalsIgnoreCase(upsamplingMethod)) {
            interpolationType = Interpolation.INTERP_BILINEAR;
        } else if ("Bicubic".equalsIgnoreCase(upsamplingMethod)) {
            interpolationType = Interpolation.INTERP_BICUBIC;
        } else {
            interpolationType = -1;
        }
        return interpolationType;
    }

    private Interpolation getInterpolation(String otherUpsamplingMethod) {
        int interpolation = getInterpolationType(otherUpsamplingMethod);
        return Interpolation.getInstance(interpolation);
    }

    private int getInterpolationType(String otherUpsamplingMethod) {
        final int interpolationType;
        if ("Nearest".equalsIgnoreCase(otherUpsamplingMethod)) {
            interpolationType = Interpolation.INTERP_NEAREST;
        } else if ("Bilinear".equalsIgnoreCase(otherUpsamplingMethod)) {
            interpolationType = Interpolation.INTERP_BILINEAR;
        } else if ("Bicubic".equalsIgnoreCase(otherUpsamplingMethod)) {
            interpolationType = Interpolation.INTERP_BICUBIC;
        } else {
            interpolationType = -1;
        }
        return interpolationType;
    }

    private MultiLevelImage createAggregatedImage(RasterDataNode sourceRDN, MultiLevelImage sourceImage, int dataBufferType, double noDataValue,
                                                  boolean isFlagBand, MultiLevelModel referenceModel) {
        //AggregationType type;
        Downsampling downsampling = GPF.getDefaultInstance().getDownsamplerSpiRegistry().getDownsamplerSpi(downsamplingMethod).createDownsampling();

        if(isFlagBand) {
            downsampling = GPF.getDefaultInstance().getDownsamplerSpiRegistry().getDownsamplerSpi(flagDownsamplingMethod).createDownsampling();
        }

        //Load ResamplingPreset and set downsampling if it is valid
        if(selectedResamplingPreset != null) {
            BandResamplingPreset bandResamplingPreset = selectedResamplingPreset.getBandResamplingPreset(sourceRDN.getName());
            if (bandResamplingPreset != null) {
                String bandDownsamplingMethodTemp = bandResamplingPreset.getDownsamplingAlias();
                if(GPF.getDefaultInstance().getDownsamplerSpiRegistry().getAliases().contains(bandDownsamplingMethodTemp)) {
                    downsampling = GPF.getDefaultInstance().getDownsamplerSpiRegistry().getDownsamplerSpi(bandDownsamplingMethodTemp).createDownsampling();
                }
            }
        }

        //over rules resampling preset with bandResampling if valid
        ResamplingPreset myResamplingPreset2 = ResamplingPreset.loadResamplingPreset(bandResamplings,"bandResampling");
        if(myResamplingPreset2 != null) {
            BandResamplingPreset bandResamplingPreset2 = myResamplingPreset2.getBandResamplingPreset(sourceRDN.getName());
            if (bandResamplingPreset2 != null) {
                String bandDownsamplingMethodTemp = bandResamplingPreset2.getDownsamplingAlias();
                if(GPF.getDefaultInstance().getDownsamplerSpiRegistry().getAliases().contains(bandDownsamplingMethodTemp)) {
                    downsampling = GPF.getDefaultInstance().getDownsamplerSpiRegistry().getDownsamplerSpi(bandDownsamplingMethodTemp).createDownsampling();
                }
            }
        }

        MultiLevelSource source;
        Downsampling finalDownsampling = downsampling;
        if (resampleOnPyramidLevels) {

            source = new AbstractMultiLevelSource(referenceModel) {
                @Override
                protected RenderedImage createImage(int targetLevel) {
                    final MultiLevelModel targetModel = getModel();
                    final double targetScale = targetModel.getScale(targetLevel);
                    final MultiLevelModel sourceModel = sourceImage.getModel();
                    final int sourceLevel = sourceModel.getLevel(targetScale);
                    final RenderedImage sourceLevelImage = sourceImage.getImage(sourceLevel);
                    final ResolutionLevel resolutionLevel = ResolutionLevel.create(getModel(), targetLevel);
                    final ImageLayout imageLayout = ImageManager.createSingleBandedImageLayout(dataBufferType, null,
                                                                                               referenceWidth,
                                                                                               referenceHeight,
                                                                                               referenceTileSize,
                                                                                               resolutionLevel);
                    try {
                        return new AggregatedOpImage(sourceRDN, sourceLevelImage, imageLayout, noDataValue,
                                                     finalDownsampling, dataBufferType,
                                                     sourceModel.getImageToModelTransform(sourceLevel),
                                                     targetModel.getImageToModelTransform(targetLevel));
                    } catch (NoninvertibleTransformException e) {
                        throw new OperatorException("Could not downsample band image");
                    }
                }
            };
        } else {
            try {
                final ImageLayout imageLayout = ImageManager.createSingleBandedImageLayout(dataBufferType, null,
                                                                                           referenceWidth, referenceHeight,
                                                                                           referenceTileSize,
                                                                                           ResolutionLevel.MAXRES);
                final RenderedImage image = new AggregatedOpImage(sourceRDN, sourceImage, imageLayout, noDataValue, finalDownsampling, dataBufferType,
                                                                  sourceImage.getModel().getImageToModelTransform(0),
                                                                  referenceModel.getImageToModelTransform(0));
                source = new DefaultMultiLevelSource(image, referenceModel);
            } catch (NoninvertibleTransformException e) {
                throw new OperatorException("Could not downsample band image");
            }
        }
        return new DefaultMultiLevelImage(source);
    }

    private void setReferenceValues() {
        validateReferenceSettings();
        Logger logger = Logger.getLogger(this.getClass().getName());
        if (referenceBandName != null) {
            logger.fine("Use reference band to derive resampling parameters");
            final Band referenceBand = sourceProduct.getBand(referenceBandName);
            referenceWidth = referenceBand.getRasterWidth();
            referenceHeight = referenceBand.getRasterHeight();
            referenceImageToModelTransform = referenceBand.getImageToModelTransform();
            referenceMultiLevelModel = referenceBand.getMultiLevelModel();
        } else if (targetWidth != null && targetHeight != null) {
            logger.fine("Use reference width and height to derive resampling parameters");
            referenceWidth = targetWidth;
            referenceHeight = targetHeight;
            double scaleX = (double) sourceProduct.getSceneRasterWidth() / referenceWidth;
            double scaleY = (double) sourceProduct.getSceneRasterHeight() / referenceHeight;
            GeoCoding sceneGeoCoding = sourceProduct.getSceneGeoCoding();
            if (sceneGeoCoding != null && sceneGeoCoding.getImageToMapTransform() instanceof AffineTransform) {
                AffineTransform mapTransform = (AffineTransform) sceneGeoCoding.getImageToMapTransform();
                referenceImageToModelTransform =
                        new AffineTransform(scaleX * mapTransform.getScaleX(), 0, 0, scaleY * mapTransform.getScaleY(),
                                            mapTransform.getTranslateX(), mapTransform.getTranslateY());
            } else {
                referenceImageToModelTransform = new AffineTransform(scaleX, 0, 0, scaleY, 0, 0);
            }
            referenceMultiLevelModel = new DefaultMultiLevelModel(referenceImageToModelTransform, referenceWidth, referenceHeight);
        } else {
            logger.fine("Use resolution to derive resampling parameters");
            final MathTransform imageToMapTransform = sourceProduct.getSceneGeoCoding().getImageToMapTransform();
            if (!(imageToMapTransform instanceof AffineTransform)) {
                throw new OperatorException("Use of target resolution parameter is not possible for this source product.");
            }
            final ProductNodeGroup<Band> productBands = sourceProduct.getBandGroup();
            final ProductNodeGroup<TiePointGrid> productTiePointGrids = sourceProduct.getTiePointGridGroup();
                AffineTransform mapTransform = (AffineTransform) imageToMapTransform;
            double translateX;
            double translateY;
            if (ResampleUtils.allGridsAlignAtUpperLeftPixelCenter(mapTransform, productBands, productTiePointGrids)) {
                translateX = mapTransform.getTranslateX() + 0.5 * mapTransform.getScaleX() - 0.5 * targetResolution;
                translateY = mapTransform.getTranslateY() + 0.5 * mapTransform.getScaleY() + 0.5 * targetResolution;
            } else if (ResampleUtils.allGridsAlignAtUpperLeftPixelCorner(mapTransform, productBands, productTiePointGrids)) {
                translateX = mapTransform.getTranslateX();
                translateY = mapTransform.getTranslateY();
            } else {
                throw new OperatorException("Use of target resolution parameter is not possible for this source product.");
            }
            referenceWidth = (int) Math.ceil(sourceProduct.getSceneRasterWidth() * Math.abs(mapTransform.getScaleX()) / targetResolution);
            referenceHeight = (int) Math.ceil(sourceProduct.getSceneRasterHeight() * Math.abs(mapTransform.getScaleY()) / targetResolution);
            referenceImageToModelTransform = new AffineTransform(targetResolution, 0, 0, -targetResolution,
                    translateX, translateY);
            referenceMultiLevelModel = new DefaultMultiLevelModel(referenceImageToModelTransform, referenceWidth, referenceHeight);
        }
        referenceTileSize = sourceProduct.getPreferredTileSize();
        if (referenceTileSize == null) {
            referenceTileSize = JAIUtils.computePreferredTileSize(referenceWidth, referenceHeight, 1);
        }
    }

    private void validateReferenceSettings() {
        if (referenceBandName == null && targetWidth == null && targetHeight == null && targetResolution == null) {
            throw new OperatorException("Either referenceBandName or targetResolution or targetWidth " +
                                                "together with targetHeight must be set.");
        }
        if (referenceBandName != null && (targetWidth != null || targetHeight != null || targetResolution != null)) {
            throw new OperatorException("If referenceBandName is set, targetWidth, targetHeight, " +
                                                "and targetResolution must not be set");
        }
        if (targetResolution != null && (targetWidth != null || targetHeight != null)) {
            throw new OperatorException("If targetResolution is set, targetWidth, targetHeight, " +
                                                "and referenceBandName must not be set");
        }
        if (targetWidth != null && targetHeight == null) {
            throw new OperatorException("If targetWidth is set, targetHeight must be set, too.");
        }
        if (targetWidth == null && targetHeight != null) {
            throw new OperatorException("If targetHeight is set, targetWidth must be set, too.");
        }
        if (targetResolution != null && !(sourceProduct.getSceneGeoCoding() instanceof CrsGeoCoding)) {
            throw new OperatorException("Use of targetResolution is only possible for products with crs geo-coding.");
        }
    }

    //todo this method has been copied from ReprojectionOp. Find a common place? - tf 20160210
    private void validateInterpolationParameter() {
        if (getInterpolationType() == -1 && GPF.getDefaultInstance().getUpsamplerSpiRegistry().getUpsamplerSpi(upsamplingMethod) == null) {
            throw new OperatorException("Invalid upsampling method: " + upsamplingMethod);
        }
    }

    private void setResamplingTypes() {
        aggregationType = getAggregationType(downsamplingMethod);
        flagAggregationType = getAggregationType(flagDownsamplingMethod);
    }

    private void loadResamplingPreset() {
        selectedResamplingPreset = null;
        if(resamplingPreset == null || resamplingPreset.length() == 0) {
            return;
        }
        //try to find it in manager
        selectedResamplingPreset = ResamplingPresetManager.getInstance().getResamplingPreset(resamplingPreset);
        if(selectedResamplingPreset != null) {
            return;
        }

        //if not found in manager, try to load it as a file
        if(!Files.exists(Paths.get(resamplingPreset))) {
            return;
        }

        try {
            selectedResamplingPreset = ResamplingPreset.loadResamplingPreset(Paths.get(resamplingPreset).toFile());
        } catch (IOException e) {
            return;
        }
    }

    private AggregationType getAggregationType(String aggregationMethod) {
        switch (aggregationMethod) {
            case "Mean":
                return AggregationType.Mean;
            case "Median":
                return AggregationType.Median;
            case "Min":
                return AggregationType.Min;
            case "Max":
                return AggregationType.Max;
            case "First":
                return AggregationType.First;
            case "FlagAnd":
                return AggregationType.FlagAnd;
            case "FlagOr":
                return AggregationType.FlagOr;
            case "FlagMedianAnd":
                return AggregationType.FlagMedianAnd;
            case "FlagMedianOr":
                return AggregationType.FlagMedianOr;
        }
        return null;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ResamplingOp.class);
        }
    }

}