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

package org.esa.snap.binning;

/**
 * An aggregator provides the strategies for spatial and temporal binning. Operating on single bin cells,
 * an aggregator provides the answers for
 * <ul>
 * <li>A. Spatial binning: how are input samples of a single observation (swath) aggregated to spatial bins?</li>
 * <li>B. Temporal binning: how are spatial bins aggregated to temporal bins?</li>
 * <li>C. Final statistics: how are final statistic computed?</li>
 * </ul>
 * <p>
 * A. Spatial binning: For each bin found in a single observation (swath).
 * <ol>
 * <li>{@link #initSpatial(BinContext, WritableVector)}</li>
 * <li>For each contributing measurement: {@link #aggregateSpatial(BinContext, Observation, WritableVector)}</li>
 * <li>{@link #completeSpatial(BinContext, int, WritableVector)}</li>
 * </ol>
 * <p>
 * B. Temporal binning: For all bins found in all swaths.
 * <ol>
 * <li>{@link #initTemporal(BinContext, WritableVector)}</li>
 * <li>For each contributing spatial bin: {@link #aggregateTemporal(BinContext, Vector, int, WritableVector)}</li>
 * <li>{@link #completeTemporal(BinContext, int, WritableVector)}</li>
 * </ol>
 * <p>
 * C. Final statistics: For all bins found in all swaths compute the final statistics.
 * <ol>
 * <li>{@link #computeOutput(Vector, WritableVector)}</li>
 * </ol>
 * <p>
 * Note for implementors: Aggregators should have no state. In order to exchange information within the spatial or temporal
 * binning calling sequences, use the {@link BinContext}.
 *
 * @author Norman Fomferra
 */
public interface Aggregator {

    /**
     * @return The aggregator's name.
     */
    String getName();

    /**
     * @return The array of names of all statistical features used for spatial binning.
     */
    String[] getSpatialFeatureNames();

    /**
     * @return The array of names of all statistical features used for temporal binning.
     */
    String[] getTemporalFeatureNames();

    /**
     * @return The array of names of all statistical features produced as output.
     */
    String[] getOutputFeatureNames();

    /**
     * Initialises the spatial aggregation vector.
     *
     * @param ctx    The bin context which is shared between calls to {@link #initSpatial},
     *               {@link #aggregateSpatial} and {@link #completeSpatial}.
     * @param vector The aggregation vector to initialise.
     */
    void initSpatial(BinContext ctx, WritableVector vector);

    /**
     * Aggregates a new observation to a spatial aggregation vector.
     *
     * @param ctx               The bin context which is shared between calls to {@link #initSpatial},
     *                          {@link #aggregateSpatial} and {@link #completeSpatial}.
     * @param observationVector The observation.
     * @param spatialVector     The spatial aggregation vector to update.
     */
    void aggregateSpatial(BinContext ctx, Observation observationVector, WritableVector spatialVector);

    /**
     * Informs this aggregation instance that no more measurements will be added to the spatial vector.
     *
     * @param ctx           The bin context which is shared between calls to {@link #initSpatial},
     *                      {@link #aggregateSpatial} and {@link #completeSpatial}.
     * @param numSpatialObs The number of observations added so far.
     * @param spatialVector The spatial aggregation vector to complete.
     */
    void completeSpatial(BinContext ctx, int numSpatialObs, WritableVector spatialVector);

    /**
     * Initialises the temporal aggregation vector.
     *
     * @param ctx    The bin context which is shared between calls to {@link #initTemporal},
     *               {@link #aggregateTemporal} and {@link #completeTemporal}.
     * @param vector The aggregation vector to initialise.
     */
    void initTemporal(BinContext ctx, WritableVector vector);

    /**
     * Aggregates a spatial aggregation to a temporal aggregation vector.
     *
     * @param ctx            The bin context which is shared between calls to {@link #initTemporal},
     *                       {@link #aggregateTemporal} and {@link #completeTemporal}.
     * @param spatialVector  The spatial aggregation.
     * @param numSpatialObs  The number of total observations made in the spatial aggregation.
     * @param temporalVector The temporal aggregation vector to be updated.
     */
    void aggregateTemporal(BinContext ctx, Vector spatialVector, int numSpatialObs, WritableVector temporalVector);

    /**
     * Informs this aggregation instance that no more measurements will be added to the temporal vector.
     *
     * @param ctx            The bin context which is shared between calls to {@link #initTemporal},
     *                       {@link #aggregateTemporal} and {@link #completeTemporal}.
     * @param numTemporalObs The number of observations added so far.
     * @param temporalVector The temporal aggregation vector to complete.
     */
    void completeTemporal(BinContext ctx, int numTemporalObs, WritableVector temporalVector);

    /**
     * Computes the output vector from the temporal vector.
     *
     * @param temporalVector The temporal vector.
     * @param outputVector   The output vector to be computed.
     */
    void computeOutput(Vector temporalVector, WritableVector outputVector);

}
