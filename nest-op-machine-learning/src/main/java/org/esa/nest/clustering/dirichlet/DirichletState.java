/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.nest.clustering.dirichlet;

import java.util.ArrayList;
import java.util.List;
import org.apache.mahout.clustering.Model;
import org.apache.mahout.clustering.ModelDistribution;
import org.apache.mahout.clustering.dirichlet.UncommonDistributions;

import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Vector;

public class DirichletState<O> {

    private int numClusters; // the number of clusters
    private ModelDistribution<O> modelFactory; // the factory for models
    private List<DirichletCluster<O>> clusters; // the clusters for this iteration
    private Vector mixture; // the mixture vector
    private double alpha_0; // alpha_0

    public DirichletState(ModelDistribution<O> modelFactory,
            int numClusters,
            double alpha_0) {
        this.numClusters = numClusters;
        this.modelFactory = modelFactory;
        this.alpha_0 = alpha_0;
        // sample initial prior models
        clusters = new ArrayList<>();
        for (Model<O> m : modelFactory.sampleFromPrior(numClusters)) {
            clusters.add(new DirichletCluster<>(m));
        }
        // sample the mixture parameters from a Dirichlet distribution on the totalCounts
        mixture = UncommonDistributions.rDirichlet(totalCounts(), alpha_0);
    }

    public DirichletState() {
    }

    public int getNumClusters() {
        return numClusters;
    }

    public void setNumClusters(int numClusters) {
        this.numClusters = numClusters;
    }

    public ModelDistribution<O> getModelFactory() {
        return modelFactory;
    }

    public void setModelFactory(ModelDistribution<O> modelFactory) {
        this.modelFactory = modelFactory;
    }

    public List<DirichletCluster<O>> getClusters() {
        return clusters;
    }

    public void setClusters(List<DirichletCluster<O>> clusters) {
        this.clusters = clusters;
    }

    public Vector getMixture() {
        return mixture;
    }

    public void setMixture(Vector mixture) {
        this.mixture = mixture;
    }

    private Vector totalCounts() {
        Vector result = new DenseVector(numClusters);
        for (int i = 0; i < numClusters; i++) {
            result.set(i, clusters.get(i).getTotalCount());
        }
        return result;
    }

    /**
     * Update the receiver with the new models
     *
     * @param newModels a Model<Observation>[] of new models
     */
    public void update(Model<O>[] newModels) {
        // compute new model parameters based upon observations and update models
        for (int i = 0; i < newModels.length; i++) {
            newModels[i].computeParameters();
            clusters.get(i).setModel(newModels[i]);
        }
        // update the mixture
        mixture = UncommonDistributions.rDirichlet(totalCounts(), alpha_0);
    }

    /**
     * return the adjusted probability that x is described by the kth model
     *
     * @param x an Observation
     * @param k an int index of a model
     * @return the double probability
     */
    public double adjustedProbability(O x, int k) {
        double pdf = clusters.get(k).getModel().pdf(x);
        double mix = mixture.get(k);
        return mix * pdf;
    }

    public Model<O>[] getModels() {
        Model<O>[] result = new Model[numClusters];
        for (int i = 0; i < numClusters; i++) {
            result[i] = clusters.get(i).getModel();
        }
        return result;
    }
}