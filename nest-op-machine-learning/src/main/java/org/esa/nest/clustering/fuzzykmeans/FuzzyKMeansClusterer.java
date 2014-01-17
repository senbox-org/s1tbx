package org.esa.nest.clustering.fuzzykmeans;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.mahout.clustering.fuzzykmeans.SoftCluster;
import org.apache.mahout.common.distance.DistanceMeasure;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.SequentialAccessSparseVector;
import org.apache.mahout.math.Vector;

class FuzzyKMeansClusterer {

    private final int dimensionCount;
    private final int clusterCount;
    private final double[][] means;
    private double[][] sums;
    private static final Collection<Vector> PARAMS = Lists.newArrayList();
    protected static List<SoftCluster> clusters = new ArrayList<>();
    List<List<SoftCluster>> clustersPerIter = new ArrayList<>();
    /**
     * Constructs a new instance of this class.
     *
     * @param clusterCount the number of clusters.
     * @param dimensionCount the number of dimensions.
     */
    private static final double MINIMAL_VALUE = 0.0000000001;
    private DistanceMeasure measure;
    private final double convergenceDelta;
    private double m = 2.0; // default value
//    private boolean emitMostLikely = true;
//    private double threshold;
    private int maxIterations;
    int iteration = 0;

    /**
     * Init the fuzzy k-means clusterer with the distance measure to use for
     * comparison.
     */
    FuzzyKMeansClusterer(int clusterCount, int dimensionCount, DistanceMeasure measure, double convergenceDelta, double m, int maxIterations) {
        this.clusterCount = clusterCount;
        this.dimensionCount = dimensionCount;
        this.means = new double[clusterCount][dimensionCount];
        this.measure = measure;
        this.convergenceDelta = convergenceDelta;
        this.m = m;
        this.maxIterations = maxIterations;
    }

    void initialize(RandomSceneIteration sceneIter) {
        int id = 0;
        for (int c = 0; c < clusterCount; ++c) {
            boolean accepted = true;
            do {
                means[c] = sceneIter.getNextValue();

                for (int i = 0; i < c; ++i) {

                    accepted = !Arrays.equals(means[c], means[i]);

                    if (!accepted) {
                        break;
                    }
                    DenseVector denseVector = new DenseVector(means[c]);
                    clusters.add(new SoftCluster(denseVector, id++, measure));
                }
            } while (!accepted);
            clustersPerIter.add(clusters);
        }
    }

    public double getM() {
        return m;
    }

    public DistanceMeasure getMeasure() {
        return this.measure;
    }

    void startIteration() {
        iteration = 0;
    }

    public static List<Vector> getPoints(double[][] raw) {
        List<Vector> points = new ArrayList<>();
        for (double[] fr : raw) {
            Vector vec = new SequentialAccessSparseVector(fr.length);
            vec.assign(fr);
            points.add(vec);
        }
        return points;
    }

    boolean iterateTile(KMeansIteration pixelIter) {
        final double[] point = new double[dimensionCount];
        List<Vector> points = new ArrayList<>();
        while (pixelIter.next(point) != null) {
            Vector pointVector = new DenseVector(point);
            points.add(pointVector);
        }
        List<SoftCluster> next = new ArrayList<>();
        List<SoftCluster> cs = clustersPerIter.get(iteration++);
        for (SoftCluster c : cs) {
            next.add(new SoftCluster(c.getCenter(), c.getId(), measure));
        }
        clustersPerIter.add(next);
        boolean converged = runFuzzyKMeansIteration(points, clustersPerIter.get(iteration));
        return converged;
    }

    /**
     * Perform a single iteration over the points and clusters, assigning points
     * to clusters and returning if the iterations are completed.
     *
     * @param points the List<Vector> having the input points
     * @param clusters the List<Cluster> clusters
     */
    protected boolean runFuzzyKMeansIteration(Iterable<Vector> points,
            List<SoftCluster> clusterList) {
        for (Vector point : points) {
            this.addPointToClusters(clusterList, point);
        }
        return testConvergence(clusterList);
    }

    protected void addPointToClusters(List<SoftCluster> clusterList, Vector point) {
        List<Double> clusterDistanceList = new ArrayList<>();
        for (SoftCluster cluster : clusterList) {
            clusterDistanceList.add(getMeasure().distance(point, cluster.getCenter()));
        }
        for (int i = 0; i < clusterList.size(); i++) {
            double probWeight = computeProbWeight(clusterDistanceList.get(i), clusterDistanceList);
            clusterList.get(i).observe(point, Math.pow(probWeight, getM()));
        }
    }

    protected boolean testConvergence(Iterable<SoftCluster> clusters) {
        boolean converged = true;
        for (SoftCluster cluster : clusters) {
            if (!cluster.computeConvergence(measure, convergenceDelta)) {
                converged = false;
            }
            cluster.computeParameters();
        }
        return converged;
    }

    /**
     * Computes the probability of a point belonging to a cluster
     */
    public double computeProbWeight(double clusterDistance, Iterable<Double> clusterDistanceList) {
        if (clusterDistance == 0) {
            clusterDistance = MINIMAL_VALUE;
        }
        double denom = 0.0;
        for (double eachCDist : clusterDistanceList) {
            if (eachCDist == 0.0) {
                eachCDist = MINIMAL_VALUE;
            }
            denom += Math.pow(clusterDistance / eachCDist, 2.0 / (m - 1));
        }
        return 1.0 / denom;
    }

    /**
     * Returns the clusters found.
     *
     * @return the clusters found.
     */
    List<SoftCluster> getClusters() {
        return clusters;
    }
}
