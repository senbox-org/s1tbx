/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.esa.beam.gpf.common;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

import javax.media.jai.JAI;
import javax.media.jai.Warp;
import javax.media.jai.WarpAffine;
import javax.media.jai.RenderedOp;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.BorderExtender;
import javax.media.jai.BorderExtenderConstant;
import javax.media.jai.operator.MosaicDescriptor;
import javax.media.jai.InterpolationNearest;

import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.geometry.Envelope;

import org.esa.beam.util.logging.BeamLogManager;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.GeneralGridRange;
import org.geotools.factory.Hints;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.operation.AbstractCoordinateOperationFactory;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.geotools.referencing.operation.transform.DimensionFilter;
import org.geotools.referencing.operation.transform.IdentityTransform;
import org.geotools.referencing.operation.transform.WarpTransform2D;
import org.geotools.resources.XArray;
import org.geotools.resources.i18n.Errors;
import org.geotools.resources.i18n.ErrorKeys;
import org.geotools.resources.i18n.Loggings;
import org.geotools.resources.i18n.LoggingKeys;
import org.geotools.resources.image.ImageUtilities;
import org.geotools.resources.coverage.CoverageUtilities;


/**
 * Implementation of the {@link Resample} operation. This implementation is provided as a
 * separated class for two purpose: avoid loading this code before needed and provide some
 * way to check if a grid coverages is a result of a resample operation.
 *
 * @since 2.2
 * @source $URL: http://svn.osgeo.org/geotools/tags/2.5.3/modules/library/coverage/src/main/java/org/geotools/coverage/processing/operation/Resampler2D.java $
 * @version $Id: Resampler2D.java 31446 2008-09-07 18:29:44Z desruisseaux $
 * @author Martin Desruisseaux (IRD)
 */
final class Resampler2D {
    /**
     * For compatibility during cross-version serialization.
     */
    private static final long serialVersionUID = -8593569923766544474L;

    /**
     * The corner to use for performing calculation. By default {@link GridGeometry#getGridToCRS()}
     * maps to pixel center (as of OGC specification). In JAI, the transforms rather map to the
     * upper left corner.
     *
     * @todo Left to CENTER for now because we need to pass this argument to {@link GridGeometry2D}
     *       constructors.
     */
    private static final PixelOrientation CORNER = PixelOrientation.CENTER; //UPPER_LEFT;

    /**
     * Small tolerance threshold for floating point number comparaisons.
     */
    private static final double EPS = 1E-6;



    /**
     * Creates a new coverage with a different coordinate reference reference system. If a
     * grid geometry is supplied, only its {@linkplain GridGeometry2D#getRange grid range}
     * and {@linkplain GridGeometry2D#getGridToCRS grid to CRS} transform are taken in account.
     *
     * @param sourceCoverage
     *          The source grid coverage.
     * @param targetCRS
     *          Coordinate reference system for the new grid coverage, or {@code null}.
     * @param targetGG
     *          The target grid geometry, or {@code null} for default.
     * @param interpolation
     *          The interpolation to use, or {@code null} if none.
     * @param hints
     *          The rendering hints. This is usually provided by {@link AbstractProcessor}.
     *          This method will looks for {@link Hints#COORDINATE_OPERATION_FACTORY} and
     *          {@link Hints#JAI_INSTANCE} keys.
     * @return
     *          The new grid coverage, or {@code sourceCoverage} if no resampling was needed.
     * @throws FactoryException
     *          if a transformation step can't be created.
     * @throws TransformException
     *          if a transformation failed.
     */
    public static RenderedImage reproject(GridCoverage2D                    sourceCoverage,
                                          final RenderedImage               sourceImage,
                                          final CoordinateReferenceSystem   sourceCRS,
                                          CoordinateReferenceSystem         targetCRS,
                                          GridGeometry2D                    targetGG,
                                          final Interpolation               interpolation,
                                          final Hints                       hints)
            throws FactoryException, TransformException
    {
        ////////////////////////////////////////////////////////////////////////////////////////
        ////                                                                                ////
        //// =======>>  STEP 1: Extracts needed informations from the parameters   <<====== ////
        ////            STEP 2: Creates the "target to source" MathTransform                ////
        ////            STEP 3: Computes the target image layout                            ////
        ////            STEP 4: Applies the JAI operation ("Affine", "Warp", etc)           ////
        ////                                                                                ////
        ////////////////////////////////////////////////////////////////////////////////////////

        /*
         * If the source coverage is already the result of a previous "Resample" operation,
         * go up in the chain and check if a previously computed image could fits (i.e. the
         * requested resampling may be the inverse of a previous resampling). This method
         * may stop immediately if a suitable image is found.
         */
        final GridGeometry2D sourceGG = sourceCoverage.getGridGeometry();
        final CoordinateReferenceSystem compatibleSourceCRS = compatibleSourceCRS(
                sourceCoverage.getCoordinateReferenceSystem2D(), sourceCRS, targetCRS);

        ////////////////////////////////////////////////////////////////////////////////////////
        ////                                                                                ////
        ////            STEP 1: Extracts needed informations from the parameters            ////
        //// =======>>  STEP 2: Creates the "target to source" MathTransform       <<====== ////
        ////            STEP 3: Computes the target image layout                            ////
        ////            STEP 4: Applies the JAI operation ("Affine", "Warp", etc)           ////
        ////                                                                                ////
        ////////////////////////////////////////////////////////////////////////////////////////

        final CoordinateOperationFactory factory =
                ReferencingFactoryFinder.getCoordinateOperationFactory(hints);
        final MathTransformFactory mtFactory;
        if (factory instanceof AbstractCoordinateOperationFactory) {
            mtFactory = ((AbstractCoordinateOperationFactory) factory).getMathTransformFactory();
        } else {
            mtFactory = ReferencingFactoryFinder.getMathTransformFactory(hints);
        }
        /*
         * Computes the INVERSE of the math transform from [Source Grid] to [Target Grid].
         * The transform will be computed using the following path:
         *
         *      Target Grid --> Target CRS --> Source CRS --> Source Grid
         *                   ^              ^              ^
         *                 step 1         step 2         step 3
         *
         * If source and target CRS are equal, a shorter path is used. This special
         * case is needed because 'sourceCRS' and 'targetCRS' may be null.
         *
         *      Target Grid --> Common CRS --> Source Grid
         *                   ^              ^
         *                 step 1         step 3
         */
        final MathTransform step1, step2, step3, allSteps;
        if (CRS.equalsIgnoreMetadata(sourceCRS, targetCRS)) {
            step1    = targetGG.getGridToCRS(CORNER);
            step2    = IdentityTransform.create(step1.getTargetDimensions());
            step3    = sourceGG.getGridToCRS(CORNER).inverse();
            allSteps = mtFactory.createConcatenatedTransform(step1, step3);
        } else {
            final Envelope        sourceEnvelope;
            final GeneralEnvelope targetEnvelope;
            final CoordinateOperation operation = factory.createOperation(sourceCRS, targetCRS);
            final boolean force2D = (sourceCRS != compatibleSourceCRS);
            step2          = factory.createOperation(targetCRS, compatibleSourceCRS).getMathTransform();
            step3          = (force2D ? sourceGG.getGridToCRS2D(CORNER) : sourceGG.getGridToCRS(CORNER)).inverse();
            sourceEnvelope = sourceCoverage.getEnvelope(); // Don't force this one to 2D.
            targetEnvelope = CRS.transform(operation, sourceEnvelope);
            targetEnvelope.setCoordinateReferenceSystem(targetCRS);
            // 'targetCRS' may be different than the one set by CRS.transform(...).
            /*
             * If the target GridGeometry is incomplete, provides default
             * values for the missing fields. Three cases may occurs:
             *
             * - User provided no GridGeometry at all. Then, constructs an image of the same size
             *   than the source image and set an envelope big enough to contains the projected
             *   coordinates. The transform will derive from the grid range and the envelope.
             *
             * - User provided only a grid range.  Then, set an envelope big enough to contains
             *   the projected coordinates. The transform will derive from the grid range and
             *   the envelope.
             *
             * - User provided only a "grid to CRS" transform. Then, transform the projected
             *   envelope to "grid units" using the specified transform and create a grid range
             *   big enough to hold the result.
             */
            step1 = targetGG.getGridToCRS(CORNER);
            if (!targetGG.isDefined(GridGeometry2D.GRID_RANGE)) {
                GeneralEnvelope gridRange = CRS.transform(step1.inverse(), targetEnvelope);
                // According OpenGIS specification, GridGeometry maps pixel's center.
                targetGG = new GridGeometry2D(new GeneralGridRange(gridRange,
                        PixelInCell.CELL_CENTER), step1, targetCRS);
            }
            /*
             * Computes the final transform.
             */
            if (step1.equals(step3.inverse())) {
                allSteps = step2;
            } else {
                allSteps = mtFactory.createConcatenatedTransform(
                           mtFactory.createConcatenatedTransform(step1, step2), step3);
            }
        }
        MathTransform2D allSteps2D = toMathTransform2D(allSteps, mtFactory, targetGG);

        ////////////////////////////////////////////////////////////////////////////////////////
        ////                                                                                ////
        ////            STEP 1: Extracts needed informations from the parameters            ////
        ////            STEP 2: Creates the "target to source" MathTransform                ////
        //// =======>>  STEP 3: Computes the target image layout                   <<====== ////
        ////            STEP 4: Applies the JAI operation ("Affine", "Warp", etc)           ////
        ////                                                                                ////
        ////////////////////////////////////////////////////////////////////////////////////////

        final RenderingHints targetHints = getRenderingHints(sourceImage, interpolation);
        if (hints != null) {
            targetHints.add(hints);
        }
        ImageLayout layout = (ImageLayout) targetHints.get(JAI.KEY_IMAGE_LAYOUT);
        if (layout != null) {
            layout = (ImageLayout) layout.clone();
        } else {
            layout = new ImageLayout();
            // Do not inherit the color model and sample model from the 'sourceImage';
            // Let the operation decide itself. This is necessary in case we change the
            // source, as we do if we choose the "Mosaic" operation.
        }
        final Rectangle sourceBB = sourceGG.getGridRange2D();
        final Rectangle targetBB = targetGG.getGridRange2D();
        if (isBoundsUndefined(layout, false)) {
            layout.setMinX  (targetBB.x);
            layout.setMinY  (targetBB.y);
            layout.setWidth (targetBB.width);
            layout.setHeight(targetBB.height);
        }
        if (isBoundsUndefined(layout, true)) {
            Dimension size = new Dimension(layout.getWidth (sourceImage),
                                           layout.getHeight(sourceImage));
            size = ImageUtilities.toTileSize(size);
            layout.setTileGridXOffset(layout.getMinX(sourceImage));
            layout.setTileGridYOffset(layout.getMinY(sourceImage));
            layout.setTileWidth (size.width);
            layout.setTileHeight(size.height);
        }
        /*
         * Creates the border extender from the background values. We add it inconditionnaly as
         * a matter of principle, but it will be ignored by all JAI operations except "Affine".
         * There is an exception for the case where the user didn't specified explicitly the
         * desired target grid range. NOT specifying border extender will allows "Affine" to
         * shrink the target image bounds to the range containing computed values.
         */
        final double[] background = CoverageUtilities.getBackgroundValues(sourceCoverage);
        if (background != null && background.length != 0) {
            final BorderExtender borderExtender;
            if (XArray.allEquals(background, 0)) {
                borderExtender = BorderExtender.createInstance(BorderExtender.BORDER_ZERO);
            } else {
                borderExtender = new BorderExtenderConstant(background);
            }
            hints.put(JAI.KEY_BORDER_EXTENDER, borderExtender);
        }
        /*
         * We need to correctly manage the Hints to control the replacement of IndexColorModel.
         * It is worth to point out that setting the JAI.KEY_REPLACE_INDEX_COLOR_MODEL hint to
         * Boolean.TRUE is not enough to force the operators to do an expansion. If we explicitly
         * provide an ImageLayout built with the source image where the CM and the SM are valid.
         * those will be employed overriding a the possibility to expand the color model.
         */
//        if (ViewType.PHOTOGRAPHIC.equals(processingView)) {
//            layout.unsetValid(ImageLayout.COLOR_MODEL_MASK | ImageLayout.SAMPLE_MODEL_MASK);
//        }
//        targetHints.put(JAI.KEY_IMAGE_LAYOUT, layout);

        ////////////////////////////////////////////////////////////////////////////////////////
        ////                                                                                ////
        ////            STEP 1: Extracts needed informations from the parameters            ////
        ////            STEP 2: Creates the "target to source" MathTransform                ////
        ////            STEP 3: Computes the target image layout                            ////
        //// =======>>  STEP 4: Applies the JAI operation ("Affine", "Warp", etc)  <<====== ////
        ////                                                                                ////
        ////////////////////////////////////////////////////////////////////////////////////////
        /*
         * If the user requests a new grid geometry with the same coordinate reference system,
         * and if the grid geometry is equivalents to a simple extraction of a sub-area, then
         * delegates the work to a "Crop" operation.
         */
        final String operation;
        final ParameterBlock paramBlk = new ParameterBlock().addSource(sourceImage);
        if (allSteps.isIdentity() || (allSteps instanceof AffineTransform &&
                XAffineTransform.isIdentity((AffineTransform) allSteps, EPS)))
        {
            /*
             * Since there is no interpolation to perform, use the native view (which may be
             * packed or geophysics - it is just the view which is closest to original data).
             */
            paramBlk.removeSources();
            paramBlk.addSource(sourceImage);
            if (targetBB.equals(sourceBB)) {
                /*
                 * Optimization in case we have nothing to do, not even a crop. Reverts to the
                 * original coverage BEFORE to creates Resampler2D. Note that while there is
                 * nothing to do, the target CRS is not identical to the source CRS (so we need
                 * to create a new coverage) otherwise this condition would have been detected
                 * sooner in this method.
                 */
                return sourceImage;
            }
            if (sourceBB.contains(targetBB)) {
                operation = "Crop";
                paramBlk.add(Float.valueOf(targetBB.x))
                        .add(Float.valueOf(targetBB.y))
                        .add(Float.valueOf(targetBB.width))
                        .add(Float.valueOf(targetBB.height));
            } else {
                operation = "Mosaic";
                paramBlk.add(MosaicDescriptor.MOSAIC_TYPE_OVERLAY)
                        .add(null).add(null).add(null).add(background);
            }
        } else {
            /*
             * Special case for the affine transform. Try to use the JAI "Affine" operation
             * instead of the more general "Warp" one. JAI provides native acceleration for
             * the affine operation.
             *
             * NOTE 1: There is no need to check for "Scale" and "Translate" as special cases
             *         of "Affine" since JAI already does this check for us.
             *
             * NOTE 2: "Affine", "Scale", "Translate", "Rotate" and similar operations ignore
             *         the 'xmin', 'ymin', 'width' and 'height' image layout. Consequently, we
             *         can't use this operation if the user provided explicitly a grid range.
             *
             * NOTE 3: If the user didn't specified any grid geometry, then a yet cheaper approach
             *         is to just update the 'gridToCRS' value. We returns a grid coverage wrapping
             *         the SOURCE image with the updated grid geometry.
             */
            if ((targetBB.equals(sourceBB)) && allSteps instanceof AffineTransform) {
                // More general approach: apply the affine transform.
                operation = "Affine";
                final AffineTransform affine = (AffineTransform) allSteps.inverse();
                paramBlk.add(affine).add(interpolation).add(background);
            } else {
                /*
                 * General case: constructs the warp transform.
                 *
                 * TODO: JAI 1.1.3 seems to have a bug when the target envelope is greater than
                 *       the source envelope:  Warp on float values doesn't set to 'background'
                 *       the points outside the envelope. The operation seems to work correctly
                 *       on integer values, so as a workaround we restart the operation without
                 *       interpolation (which increase the chances to get it down on integers).
                 *       Remove this hack when this JAI bug will be fixed.
                 *
                 * TODO: Move the check for AffineTransform into WarpTransform2D.
                 */
                boolean forceAdapter = false;
                switch (sourceImage.getSampleModel().getTransferType()) {
                    case DataBuffer.TYPE_DOUBLE:
                    case DataBuffer.TYPE_FLOAT: {
                        Envelope source = CRS.transform(sourceGG.getEnvelope(), targetCRS);
                        Envelope target = CRS.transform(targetGG.getEnvelope(), targetCRS);
                        source = targetGG.reduce(source);
                        target = targetGG.reduce(target);
                        if (!(new GeneralEnvelope(source).contains(target, true))) {
                            if (interpolation != null && !(interpolation instanceof InterpolationNearest)) {
                                return reproject(sourceCoverage, sourceImage, sourceCRS, targetCRS, targetGG, null, hints);
                            } else {
                                // If we were already using nearest-neighbor interpolation, force
                                // usage of WarpAdapter2D instead of WarpAffine. The price will be
                                // a slower reprojection.
                                forceAdapter = true;
                            }
                        }
                    }
                }
                final CharSequence name = sourceCoverage.getName();
                operation = "Warp";
                final Warp warp;
                if (forceAdapter) {
                    warp = WarpTransform2D.getWarp(name, allSteps2D);
                } else {
                    warp = createWarp(name, allSteps2D);
                }
                paramBlk.add(warp).add(interpolation).add(background);
            }
        }
        final RenderedOp targetImage = JAI.getDefaultInstance().createNS(operation, paramBlk, targetHints);
        final Locale locale = sourceCoverage.getLocale();  // For logging purpose.
        /*
         * The JAI operation sometime returns an image with a bounding box different than what we
         * expected. This is true especially for the "Affine" operation: the JAI documentation said
         * explicitly that xmin, ymin, width and height image layout hints are ignored for this one.
         * As a safety, we check the bounding box in any case. If it doesn't matches, then we will
         * reconstruct the target grid geometry.
         */
        final GridEnvelope targetGR = targetGG.getGridRange();
        final int[] lower = targetGR.getLow().getCoordinateValues();
        final int[] upper = targetGR.getHigh().getCoordinateValues();
        for (int i=0; i<upper.length; i++) {
            upper[i]++; // Make them exclusive.
        }
        lower[targetGG.gridDimensionX] = targetImage.getMinX();
        lower[targetGG.gridDimensionY] = targetImage.getMinY();
        upper[targetGG.gridDimensionX] = targetImage.getMaxX();
        upper[targetGG.gridDimensionY] = targetImage.getMaxY();
        final GridEnvelope actualGR = new GeneralGridRange(lower, upper);
        if (!targetGR.equals(actualGR)) {
            MathTransform gridToCRS = targetGG.getGridToCRS(CORNER);
            targetGG = new GridGeometry2D(actualGR, gridToCRS, targetCRS);
            log(Loggings.getResources(locale).getLogRecord(Level.WARNING,
                    LoggingKeys.ADJUSTED_GRID_GEOMETRY_$1, sourceCoverage.getName().toString(locale)));
        }
        /*
         * Constructs the final grid coverage, then log a message as in the following example:
         *
         *     Resampled coverage "Foo" from coordinate system "myCS" (for an image of size
         *     1000x1500) to coordinate system "WGS84" (image size 1000x1500). JAI operation
         *     is "Warp" with "Nearest" interpolation on geophysics pixels values. Background
         *     value is 255.
         */
        return targetImage;
    }

    /**
     * Returns {@code true} if the image or tile location and size are totally undefined.
     *
     * @param layout The image layout to query.
     * @param tile {@code true} for testing tile bounds, or {@code false} for testing image bounds.
     */
    private static boolean isBoundsUndefined(final ImageLayout layout, final boolean tile) {
        final int mask;
        if (tile) {
            mask = ImageLayout.TILE_GRID_X_OFFSET_MASK | ImageLayout.TILE_WIDTH_MASK |
                   ImageLayout.TILE_GRID_Y_OFFSET_MASK | ImageLayout.TILE_HEIGHT_MASK;
        } else {
            mask = ImageLayout.MIN_X_MASK | ImageLayout.WIDTH_MASK |
                   ImageLayout.MIN_Y_MASK | ImageLayout.HEIGHT_MASK;
        }
        return (layout.getValidMask() & mask) == 0;
    }

    /**
     * Returns a source CRS compatible with the given target CRS. This method try to returns
     * a CRS which would not thrown an {@link NoninvertibleTransformException} if attempting
     * to transform from "target" to "source" (reminder: Warp works on <strong>inverse</strong>
     * transforms).
     *
     * @param sourceCRS2D
     *          The two-dimensional source CRS. Actually, this method accepts arbitrary dimension
     *          provided that are not greater than {@code sourceCRS}, but in theory it is 2D.
     * @param sourceCRS
     *          The n-dimensional source CRS.
     * @param targetCRS
     *          The n-dimensional target CRS.
     */
    private static CoordinateReferenceSystem compatibleSourceCRS(
             final CoordinateReferenceSystem sourceCRS2D,
             final CoordinateReferenceSystem sourceCRS,
             final CoordinateReferenceSystem targetCRS)
    {
        final int dim2D = sourceCRS2D.getCoordinateSystem().getDimension();
        return (targetCRS.getCoordinateSystem().getDimension() == dim2D &&
                sourceCRS.getCoordinateSystem().getDimension()  > dim2D) ? sourceCRS2D : sourceCRS;
    }

    /**
     * Returns the math transform for the two specified dimensions of the specified transform.
     *
     * @param  transform The transform.
     * @param  mtFactory The factory to use for extracting the sub-transform.
     * @param  sourceGG  The grid geometry which is the source of the <strong>transform</strong>.
     *                   This is {@code targetGG} in the {@link #reproject} method, because the
     *                   later computes a transform from target to source grid geometry.
     * @return The {@link MathTransform2D} part of {@code transform}.
     * @throws FactoryException If {@code transform} is not separable.
     */
    private static MathTransform2D toMathTransform2D(final MathTransform        transform,
                                                     final MathTransformFactory mtFactory,
                                                     final GridGeometry2D       sourceGG)
            throws FactoryException
    {
        final DimensionFilter filter = new DimensionFilter(mtFactory);
        filter.addSourceDimension(sourceGG.axisDimensionX);
        filter.addSourceDimension(sourceGG.axisDimensionY);
        MathTransform candidate = filter.separate(transform);
        if (candidate instanceof MathTransform2D) {
            return (MathTransform2D) candidate;
        }
        filter.addTargetDimension(sourceGG.axisDimensionX);
        filter.addTargetDimension(sourceGG.axisDimensionY);
        candidate = filter.separate(transform);
        if (candidate instanceof MathTransform2D) {
            return (MathTransform2D) candidate;
        }
        throw new FactoryException(Errors.format(ErrorKeys.NO_TRANSFORM2D_AVAILABLE));
    }

    /**
     * Creates a warp for the given transform. This method performs some empirical adjustment
     * for working around the {@link ArrayIndexOutOfBoundsException} which occurs sometime in
     * {@code MlibWarpPolynomialOpImage.computeTile(...)}.
     *
     * @param  name       The coverage name, for information purpose.
     * @param  allSteps2D Transform from target to source CRS.
     * @return The warp.
     * @throws FactoryException if the warp can't be created.
     */
    private static Warp createWarp(final CharSequence name, final MathTransform2D allSteps2D) {
        /*
         * Creates the warp object, trying to optimize to WarpAffine if possible. The transform
         * should have been computed in such a way that the target rectangle, when transformed,
         * matches exactly the source rectangle. Checks if the bounding boxes calculated by the
         * Warp object match the expected ones. In the usual case where they do, we are done.
         * Otherwise we assume that the difference is caused by rounding error and we will try
         * progressive empirical adjustment in order to get the rectangles to fit.
         */
        final Warp warp;
        if (allSteps2D instanceof AffineTransform) {
            warp = new WarpAffine((AffineTransform) allSteps2D);
        } else {
            warp = WarpTransform2D.getWarp(name, allSteps2D);
        }
        return warp;
    }

    /**
     * Logs a message.
     */
    private static void log(final LogRecord record) {
        record.setSourceClassName("Resample");
        record.setSourceMethodName("doOperation");
        final Logger logger = BeamLogManager.getSystemLogger();
        record.setLoggerName(logger.getName());
        logger.log(record);
    }
    
    private static RenderingHints getRenderingHints(final RenderedImage image, Interpolation interpolation) {
        RenderingHints hints = ImageUtilities.getRenderingHints(image);
        if (hints == null) {
            hints = new RenderingHints(null);
        }
        hints.put(JAI.KEY_INTERPOLATION, interpolation);
        return hints;
    }
}
