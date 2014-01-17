/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.nest.clustering.meanshift;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.mahout.clustering.fuzzykmeans.SoftCluster;
import org.apache.mahout.clustering.meanshift.MeanShiftCanopy;
import org.apache.mahout.common.distance.DistanceMeasure;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Vector;
import org.esa.nest.clustering.fuzzykmeans.RandomSceneIteration;

/**
 *
 * @author emmab
 */
public class MeanShiftCanopyClusterer {

    private final double convergenceDelta;

    // the T1 distance threshold 3.1
    private final double t1;

    // the T2 distance threshold 2.1
    private final double t2;
    // the distance measure
    private final DistanceMeasure measure;

    private int dimensionCount;
    int nextCanopyId = 0;
    protected static List<MeanShiftCanopy> canopies = new ArrayList<>();
//        List<MeanShiftCanopy> canopies = new ArrayList<MeanShiftCanopy>();

    public MeanShiftCanopyClusterer(double convergenceDelta, double t1, double t2, DistanceMeasure measure, int dimensionCount) {
        this.convergenceDelta = convergenceDelta;
        this.t1 = t1;
        this.t2 = t2;
        this.measure = measure;
        this.dimensionCount = dimensionCount;
    }

    public double getT1() {
        return t1;
    }

    public double getT2() {
        return t2;
    }

    void initialize(RandomSceneIteration sceneIter) {
        int id = 0;
        for (int c = 0; c < sceneIter.getRoiMemberCount(); ++c) {
            DenseVector denseVector = new DenseVector(sceneIter.getNextValue());
            canopies.add(new MeanShiftCanopy(denseVector, id++, measure));
        }
    }

    boolean iterateTile(ClusterIteration pixelIter) {
        boolean[] converged = {false};
        final double[] point = new double[dimensionCount];
        List<Vector> points = new ArrayList<>();
        while (pixelIter.next(point) != null) {
            Vector pointVector = new DenseVector(point);
            points.add(pointVector);
        }

        for (Vector vector : points) {
            this.mergeCanopy(new MeanShiftCanopy(vector, nextCanopyId++, measure), canopies);
        }
        List<MeanShiftCanopy> newCanopies = canopies;
        newCanopies = this.iterate(newCanopies, converged);
        canopies = newCanopies;
        return converged[0];
    }

    public List<MeanShiftCanopy> getCanopies() {
        return canopies;
    }

    protected List<MeanShiftCanopy> iterate(Iterable<MeanShiftCanopy> canopies, boolean[] converged) {
        converged[0] = true;
        List<MeanShiftCanopy> migratedCanopies = new ArrayList<>();
        for (MeanShiftCanopy canopy : canopies) {
            converged[0] = shiftToMean(canopy) && converged[0];
            mergeCanopy(canopy, migratedCanopies);
        }
        return migratedCanopies;
    }

    /**
     * Merge the given canopy into the canopies list. If it touches any existing
     * canopy (norm<T1) then add the center of each to the other. If it covers
     * any other canopies (norm<T2), then merge the given canopy with the
     * closest covering canopy. If the given canopy does not cover any other
     * canopies, add it to the canopies list.
     *
     * @param aCanopy a MeanShiftCanopy to be merged
     * @param canopies the List<Canopy> to be appended
     */
    public void mergeCanopy(MeanShiftCanopy aCanopy, Collection<MeanShiftCanopy> canopies) {
        MeanShiftCanopy closestCoveringCanopy = null;
        double closestNorm = Double.MAX_VALUE;
        for (MeanShiftCanopy canopy : canopies) {
            double norm = measure.distance(canopy.getCenter(), aCanopy.getCenter());
            if (norm < t1) {
                aCanopy.observe(canopy);
            }
            if (norm < t2 && ((closestCoveringCanopy == null) || (norm < closestNorm))) {
                closestNorm = norm;
                closestCoveringCanopy = canopy;
            }
        }
        if (closestCoveringCanopy == null) {
            canopies.add(aCanopy);
        } else {
            closestCoveringCanopy.observe(aCanopy);
        }
    }

    /**
     * Shift the center to the new centroid of the cluster
     *
     * @param canopy the canopy to shift.
     * @return if the cluster is converged
     */
    public boolean shiftToMean(MeanShiftCanopy canopy) {
        canopy.computeConvergence(measure, convergenceDelta);
        canopy.computeParameters();
        return canopy.isConverged();
    }

    /**
     * Return if the point is covered by this canopy
     *
     * @param canopy a canopy.
     * @param point a Vector point
     * @return if the point is covered
     */
    boolean covers(MeanShiftCanopy canopy, Vector point) {
        return measure.distance(canopy.getCenter(), point) < t1;
    }

    /**
     * Return if the point is closely covered by the canopy
     *
     * @param canopy a canopy.
     * @param point a Vector point
     * @return if the point is covered
     */
    public boolean closelyBound(MeanShiftCanopy canopy, Vector point) {
        return measure.distance(canopy.getCenter(), point) < t2;
    }
}
