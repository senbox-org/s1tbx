package org.esa.nest.clustering.dirichlet;

import org.esa.nest.clustering.fuzzykmeans.*;
import java.util.ArrayList;
import java.util.List;
import org.apache.mahout.clustering.Cluster;
import org.apache.mahout.clustering.Model;
import org.apache.mahout.clustering.ModelDistribution;
import org.apache.mahout.clustering.dirichlet.UncommonDistributions;
import org.apache.mahout.clustering.dirichlet.models.DistributionDescription;
import org.apache.mahout.clustering.dirichlet.models.GaussianClusterDistribution;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.SequentialAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.function.TimesFunction;
import org.esa.nest.clustering.meanshift.ClusterIteration;

/**
 * <
 * pre>
 * \theta_i ~ prior() \lambda_i ~ Dirichlet(\alpha_0) z_j ~ Multinomial( \lambda
 * ) x_j ~ model(\theta_i)
 * </pre>
 */
class DirichletClusterer<VectorWritable> {

    // observed data
    private final List<VectorWritable> data = new ArrayList<>();
    // observed data
    // the ModelDistribution for the computation
    private final ModelDistribution modelFactory;
    // the state of the clustering process
    private final DirichletState<VectorWritable> state;
    private final int thin;
    private final int burnin;
    private final int numClusters;
    private double alpha_0 = 1.0;
    private final int dimensionCount;

    /**
     * Init the fuzzy dirichlet clusterer with the distance measure to use for
     * comparison.
     */
    public DirichletClusterer(int dimensionCount,
            double alpha_0,
            int numClusters,
            int thin,
            int burnin) {
//         new DistributionDescription(), 
        Vector vw = new DenseVector(dimensionCount);
        this.dimensionCount = dimensionCount;
//        this.sampleData = sampleData;
        this.modelFactory = new GaussianClusterDistribution();
        this.thin = thin;
        this.alpha_0 = alpha_0;
        this.burnin = burnin;
        this.numClusters = numClusters;
        state = new DirichletState<>(modelFactory, numClusters, alpha_0);
    }

    void initialize(RandomSceneIteration sceneIter) {
        for (int c = 0; c < sceneIter.getRoiMemberCount(); ++c) {
            Vector denseVector = new DenseVector(sceneIter.getNextValue());
            data.add( (VectorWritable)denseVector);
        }
    }

    /**
     * Creates a DirichletState object from the given arguments. Note that the
     * modelFactory is presumed to be a subclass of VectorModelDistribution that
     * can be initialized with a concrete Vector prototype.
     *
     * @param modelFactory a String which is the class name of the model factory
     * @param modelPrototype a String which is the class name of the Vector used
     * to initialize the factory
     * @param prototypeSize an int number of dimensions of the model prototype
     * vector
     * @param numModels an int number of models to be created
     * @param alpha_0 the double alpha_0 argument to the algorithm
     * @return an initialized DirichletState
     */
//    public static DirichletState<VectorWritable> createState(String modelFactory,
//            String modelPrototype,
//            int prototypeSize,
//            int numModels,
//            double alpha_0) throws ClassNotFoundException,
//            InstantiationException,
//            IllegalAccessException,
//            SecurityException,
//            NoSuchMethodException,
//            IllegalArgumentException,
//            InvocationTargetException {
//
//        ClassLoader ccl = Thread.currentThread().getContextClassLoader();
//        Class<? extends VectorModelDistribution> cl = ccl.loadClass(modelFactory).asSubclass(
//                VectorModelDistribution.class);
//        VectorModelDistribution factory = cl.newInstance();
//
//        Class<? extends Vector> vcl = ccl.loadClass(modelPrototype).asSubclass(Vector.class);
//        Constructor<? extends Vector> v = vcl.getConstructor(int.class);
//        factory.setModelPrototype(new VectorWritable(v.newInstance(prototypeSize)));
//        return new DirichletState<VectorWritable>(factory, numModels, alpha_0);
//    }

    /**
     * Iterate over the sample data, obtaining cluster samples periodically and
     * returning them.
     *
     * @param numIterations the int number of iterations to perform
     * @return a List<List<Model<Observation>>> of the observed models
     */
    /**
     * Iterate over the sample data, obtaining cluster samples periodically and
     * returning them.
     *
     * @param numIterations the int number of iterations to perform
     * @return a List<List<Model<Observation>>> of the observed models
     */
    public static List<Vector> getPoints(double[][] raw) {
        List<Vector> points = new ArrayList<>();
        for (double[] fr : raw) {
            Vector vec = new SequentialAccessSparseVector(fr.length);
            vec.assign(fr);
            points.add(vec);
        }
        return points;
    }

    /**
     * Perform one iteration of the clustering process, iterating over the
     * samples to build a new array of models, then updating the state for the
     * next iteration
     *
     * @param state the DirichletState<Observation> of this iteration
     */
    public void iterateTile(ClusterIteration pixelIter, int iteration) {

        // create new posterior models
        Model<VectorWritable>[] newModels = modelFactory.sampleFromPosterior(state.getModels());

        // iterate over the samples, assigning each to a model
        final double[] point = new double[dimensionCount];
        List<Vector> points = new ArrayList<>();
        while (pixelIter.next(point) != null) {
            Vector pointVector = new DenseVector(point);
//            RandomAccessSparseVector sparseVector = new RandomAccessSparseVector(pointVector);
            points.add(pointVector);
        }
        for (Vector x : points) {
            // compute normalized vector of probabilities that x is described by each model
            Vector pi = normalizedProbabilities((VectorWritable)x);
            // then pick one cluster by sampling a Multinomial distribution based upon them
            // see: http://en.wikipedia.org/wiki/Multinomial_distribution
            int k = UncommonDistributions.rMultinom(pi);
            // ask the selected model to observe the datum
            newModels[k].observe((VectorWritable)x);
        }

        // periodically add models to the cluster samples after the burn-in period
        if ((iteration >= burnin) && (iteration % thin == 0)) {
            
//            data.add(newModels);
        }
        // update the state from the new models
        state.update(newModels);
    }

    /**
     * Compute a normalized vector of probabilities that x is described by each
     * model using the mixture and the model pdfs
     *
     * @param state the DirichletState<Observation> of this iteration
     * @param x an Observation
     * @return the Vector of probabilities
     */
    private Vector normalizedProbabilities(VectorWritable x) {
        Vector pi = new DenseVector(numClusters);
        double max = 0;
        for (int k = 0; k < numClusters; k++) {
            double p = state.adjustedProbability(x, k);
            pi.set(k, p);
            if (max < p) {
                max = p;
            }
        }
        // normalize the probabilities by largest observed value
        pi.assign(new TimesFunction(), 1.0 / max);
        return pi;
    }

    /**
     * @param newModels
     * @param observation
     */
    protected void observe(Model<VectorWritable>[] newModels, VectorWritable observation) {
        int k = assignToModel(observation);
        // ask the selected model to observe the datum
        newModels[k].observe(observation);
    }

    /**
     * Assign the observation to one of the models based upon probabilities
     *
     * @param observation
     * @return the assigned model's index
     */
    protected int assignToModel(VectorWritable observation) {
        // compute an unnormalized vector of probabilities that x is described by each model
        Vector pi = new DenseVector(numClusters);
        for (int k1 = 0; k1 < numClusters; k1++) {
            System.err.println(k1);
            pi.set(k1, state.adjustedProbability(observation, k1));
        }
        // then pick one cluster by sampling a Multinomial distribution based upon them
        // see: http://en.wikipedia.org/wiki/Multinomial_distribution
        int k = UncommonDistributions.rMultinom(pi);
        return k;
    }

    protected void updateModels(Model<VectorWritable>[] newModels) {
        state.update(newModels);
    }

    protected Model<VectorWritable>[] samplePosteriorModels() {
        return state.getModelFactory().sampleFromPosterior(state.getModels());
    }

    protected DirichletCluster updateCluster(Cluster model, int k) {
        model.computeParameters();
        DirichletCluster cluster = state.getClusters().get(k);
        cluster.setModel(model);
        return cluster;
    }

    /**
     * Returns the clusters found.
     *
     * @return the clusters found.
     */
    List<DirichletCluster<VectorWritable>> getClusters() {
        return state.getClusters();
    }
}
