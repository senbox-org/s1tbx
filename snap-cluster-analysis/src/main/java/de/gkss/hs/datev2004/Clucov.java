/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package de.gkss.hs.datev2004;

//import de.gkss.hs.datev.*;


import java.util.Iterator;
import java.util.Random;
import java.util.HashMap;
import java.util.logging.Logger;


/**
 * Clustering @see <a href="http://w3g.gkss.de/schiller/vorlesung/cluster/clucov_dok.ps">clucov description (german)</a> or @see <a href="http://w3g.gkss.de/schiller/handbook.ps">clucov description in a handbook article (english)</a><p>
 * Default initialization with 10 randomly assigned cluster numbers
 *
 * @author H. Schiller / GKSS
 */
public class Clucov {

    /**
     * number of iterations to be performed; default 20
     */
    public int max_iteration = 20;
    /**
     * number of hyperplanes for structure detection; default 100
     */
    public int n_planes = 100;
    /**
     * points further away  then this (Mahalanobis distance)**2 go to background; default 9.0
     */
    public double mahadistsqCut = 100;
    /**
     * test value for splitting a cluster; default 0.25
     */
    public double t_div = 0.;
    /**
     * test value for combining two clusters; default 0.35
     */
    public double t_comb = 0.05;
    /**
     * clusters with less number of points are killed; default 20
     */
    public int cont_min = 8;
    /**
     * max number of clusters to be considered; default 1000
     */
    public int max_clusters = 10;
    /**
     * if less points change membership iterations are stopped; default 0
     */
    public int few_changes = 0;

    public DataSet ds;
    Logger logger;
    Random rgen;
    public HashMap<Short, Cluster> clusters;
    int iteration;
    short next_cluster;

    /**
     * logger goes to System.out
     *
     * @param ds the data to be clustered
     */
    public Clucov(DataSet ds) {
        this(ds, new Random());
    }

    /**
     * @param ds     the data to be clustered
     * @param logger the logger
     */
    public Clucov(DataSet ds, Logger logger) {
        this(ds, logger, new Random());
    }

    /**
     * @param ds     the data to be clustered
     * @param logger the logger
     * @param rgen   seeded to users needs
     */
    public Clucov(DataSet ds, Logger logger, Random rgen) {
        this.ds = ds;
        this.rgen = rgen;
        this.logger = logger;
    }

    /**
     * @param ds   the data to be clustered
     * @param rgen seeded to users needs
     */
    public Clucov(DataSet ds, Random rgen) {
        this(ds, Logger.getAnonymousLogger(), rgen);
    }


    public static class Cluster {

        public short group;
        public String history;
        public Gaussian gauss;
        public double[] plane;
        public RecMom right, left;
        public FirstMoments fm;

        Cluster(short group) {
            this.group = group;
            this.history = "" + group;
        }

        Cluster(short group, String history) {
            this.group = group;
            this.history = history;
        }

    }

    void printCluster(Cluster cluster) {
        logger.info("this is cluster " + cluster.group + " history: " + cluster.history);
    }

    Cluster[] alive() {
        Cluster[] res = new Cluster[clusters.size()];
        Iterator<Short> keys = this.clusters.keySet().iterator();
        int i = 0;
        while (keys.hasNext()) {
            res[i] = this.clusters.get(keys.next());
            i++;
        }
        return res;
    }


    void calculate_moments() {
        Cluster cl;
        for (Short s : this.clusters.keySet()) {
            cl = this.clusters.get(s);
            cl.right = new RecMom(this.ds.dim);
            cl.left = new RecMom(this.ds.dim);
        }
        for (int np = 0; np < this.ds.npoints; np++) {
            if (this.ds.group[np] == (short) 0) continue;
            cl = this.clusters.get(this.ds.group[np]);
            int lr = side(this.ds.pt[np], cl.plane, cl.fm.avg);
            if (lr == 0)
                cl.right.recalc(this.ds.pt[np], this.ds.wgt[np]);
            else
                cl.left.recalc(this.ds.pt[np], this.ds.wgt[np]);
        }
    }

    int closest(double[] pt, Cluster[] clusters) {
        double[] d = new double[clusters.length];
        for (int c = 0; c < clusters.length; c++) {
            d[c] = clusters[c].fm.swgt * clusters[c].gauss.density(pt);
        }
        int fnd = General.indmax(d);
        if (clusters[fnd].gauss.distancesqu(pt) < mahadistsqCut)
            return fnd;
        else
            return -1;
    }

    /**
     * does one iteration step
     *
     * @return <code>true</code> if significant change was made
     */
    public boolean clustering(boolean last) {
        boolean res = false;
        int find_res = this.find_planes();
        if (!last) {
            if (find_res < 0)
                return false;
            if (find_res > this.few_changes)
                res = true;
            if (this.removeSmall() != 0)
                res = true;

            calculate_moments();

            if (split())
                res = true;
            if (combine())
                res = true;
        }
        if (this.iteration == 1) {
            res = true;
        }
        for (Short s : this.clusters.keySet()) {
            Cluster rc = this.clusters.get(s);
            rc.gauss = new Gaussian(rc.fm.avg, rc.fm.cov);
        }
        return res;
    }

    boolean combine() {
        boolean res = false;
        boolean change = true;
        while (change) {
            double thigh = -1.0;
            Cluster[] clusters = alive();
            int cmax = 0, dmax = 0;
            for (int c = 0; c < clusters.length; c++) {
                for (int d = 0; d < c; d++) {
                    double tv = test_ndim(clusters[c].fm, clusters[d].fm);
                    this.logger.fine(clusters[c].group + " | " + clusters[d].group +
                            " have a combinated t of " + tv);
                    if (tv > thigh) {
                        thigh = tv;
                        cmax = c;
                        dmax = d;
                    }
                }
            }
            if (thigh > this.t_comb) {
                String bn =
                        "(" + clusters[cmax].history + "," + clusters[dmax].history + ")";
                short nn = this.next_cluster;
                this.next_cluster++;
                this.logger.info(
                        clusters[cmax].group + " | " + clusters[dmax].group +
                                " t=" + thigh + ": groups are combined" +
                                " into the new group " + nn);
                Cluster both = new Cluster(nn, bn);
                both.fm = clusters[cmax].fm.combine(clusters[dmax].fm);
                this.clusters.put(nn, both);
                this.clusters.remove(clusters[cmax].group);
                this.clusters.remove(clusters[dmax].group);
                res = true;
            } else {
                change = false;
            }
        }
        return res;
    }


    int find_planes() {
        int changes = 0;
        double[][] planes = new double[n_planes][this.ds.dim];
        for (int i = 0; i < this.n_planes; i++) {
            planes[i] = General.vector_rand(this.ds.dim, this.rgen);
        }
        Cluster[] cl = alive();
        if ((cl.length < 2) & (this.iteration > 1)) {
            logger.info("only " + cl.length + " clusters left");
            this.rename();
            return -1;
        }
        RecMom[][][] r1 = new RecMom[2][this.n_planes][cl.length];
        for (int i = 0; i < this.n_planes; i++) {
            for (int k = 0; k < cl.length; k++) {
                r1[0][i][k] = new RecMom(1);
                r1[1][i][k] = new RecMom(1);
            }
        }
        for (int np = 0; np < this.ds.npoints; np++) {
            int indneu = closest(this.ds.pt[np], cl);
            if (indneu < 0) {
                this.ds.group[np] = 0;
                continue;
            }
            if (cl[indneu].group != this.ds.group[np]) {
                changes++;
                this.ds.group[np] = cl[indneu].group;
            }
            for (int k = 0; k < this.n_planes; k++) {
                double proj =
                        MathUtil.scalarProduct
                                (planes[k], MathUtil.vectorSubtract
                                        (this.ds.pt[np], cl[indneu].fm.avg));
                if (proj < 0.)
                    r1[0][k][indneu].recalc
                            (proj, this.ds.wgt[np]);
                else
                    r1[1][k][indneu].recalc
                            (proj, this.ds.wgt[np]);
            }
        }
        for (int c = 0; c < cl.length; c++) {
            double[] d = new double[this.n_planes];
            for (int k = 0; k < this.n_planes; k++) {
                if ((r1[0][k][c].npoints < 2) ||
                        (r1[1][k][c].npoints < 2)) {
                    d[k] = Double.MAX_VALUE;
                } else {
                    d[k] = test_1dim(r1[0][k][c], r1[1][k][c]);
                }
            }
            int fnd = General.indmin(d);
            if (d[fnd] > 1.)
                cl[c].plane =
                        General.vector_rand(this.ds.dim, this.rgen);
            else
                cl[c].plane = planes[fnd];
        }
        logger.info(changes + " points changed group membership");
        return changes;
    }

    /**
     * assigns all points to clusters. It randomly uses some points to be cluster centers
     * and then uses euclidean distance to determine cluster membership;
     *
     * @param nclinit clusters are initialized
     */
    public void initialize(int nclinit) {
        logger.info("\n----------------------------------------\nClucov.initialize try " + nclinit + " clusters");
        int nclini = nclinit;
        if (nclini > this.ds.npoints) {
            nclini = this.ds.npoints;
            logger.info("Clucov.initialize: nclinit changed to " + nclini);
        }
        int[] ptn = new int[nclini];
        boolean ok = false;
        while (!ok) {
            for (int i = 0; i < nclini; i++) {
                ptn[i] = (int) (this.ds.npoints * this.rgen.nextDouble());
            }
            ok = true;
            for (int i = 0; i < nclini - 1; i++) {
                for (int j = i + 1; j < nclini; j++) {
                    if (ptn[i] == ptn[j]) {
                        ok = false;
                        break;
                    }
                }
            }
        }
        double[] dp = new double[nclini];
        for (int i = 0; i < this.ds.npoints; i++) {
            for (int k = 0; k < nclini; k++)
                dp[k] = General.eucliddist
                        (this.ds.pt[i], this.ds.pt[ptn[k]]);
            int cl = General.indmin(dp);
            this.ds.group[i] = (short) (cl + 1);  // todo - check overflow!!!
        }
        this.makeClusters();
        this.iteration = 0;
    }

    /**
     * assigns all points to clusters. It assigns the first point to be center of cluster 1 and then assigns the first point with a distance larger the a given value away from all  existing cluster centers to be a new cluster center, etc;
     *
     * @param radius determines the largest distance from a cluster center
     *               14.10.2005: small change of algorithm, the old still below
     */
    public void initialize(double radius) {
        double[][] centers = new double[this.max_clusters][this.ds.dim];
        double[] dist = new double[this.max_clusters];
        logger.info("\n----------------------------------------\nClucov.initialize with cluster radius " + radius);
        for (int i = 0; i < this.max_clusters; i++) {
            dist[i] = Double.MAX_VALUE;
        }
        centers[0] = this.ds.pt[0];
        short nclex = 1;
        for (int np = 1; np < this.ds.npoints; np++) {
            for (int ncl = 0; ncl < nclex; ncl++) {
                dist[ncl] = General.eucliddist(centers[ncl], this.ds.pt[np]);
            }
            int near = General.indmin(dist);
            if (dist[near] > radius) {
                centers[nclex] = this.ds.pt[np];
                nclex++;
                this.ds.group[np] = nclex;
                if (nclex == this.max_clusters) {
                    General.error("Clucov.initialize: to many clusters");
                }
            }
        }

        this.ds.group[0] = 1;
        for (int np = 1; np < this.ds.npoints; np++) {
            for (int ncl = 0; ncl < nclex; ncl++) {
                dist[ncl] = General.eucliddist(centers[ncl], this.ds.pt[np]);
            }
            int near = General.indmin(dist);
            this.ds.group[np] = (short) (near + 1); // todo - check overflow!!!
        }
        this.makeClusters();
        this.iteration = 0;
    }

//    public void initialize(double radius) {
//        double[][] centers = new double[this.max_clusters][this.ds.dim];
//        double[] dist = new double[this.max_clusters];
//    	logger.println("\n----------------------------------------\nClucov.initialize with cluster radius "+radius);
//        for (int i = 0; i < this.max_clusters; i++)
//            dist[i] = Double.MAX_VALUE;
//        centers[0] = this.ds.pt[0];
//        this.ds.group[0] = new Integer(1).toString();
//        int nclex = 1;
//        for (int np = 1; np < this.ds.npoints; np++) {
//            for (int ncl = 0; ncl < nclex; ncl++)
//                dist[ncl] = General.eucliddist
//                        (centers[ncl], this.ds.pt[np]);
//            int near = General.indmin(dist);
//            if (dist[near] < radius) {
//                this.ds.group[np] =
//                        new Integer(near + 1).toString();
//            } else {
//                centers[nclex] = this.ds.pt[np];
//                nclex++;
//                this.ds.group[np] = new Integer(nclex).toString();
//                if (nclex == this.max_clusters)
//                    General.exit
//                            ("Clucov.initialize: to many clusters");
//            }
//        }
//        this.makeClusters();
//        this.iteration = 0;
//    }

    /**
     * cycles through iterations
     *
     * @return the actual number of iterations performed
     */
    public int iterate() {
        int res = 0;
        boolean clustering = true;
        while (clustering) {
            res++;
            this.iteration++;
            if (this.iteration == (this.max_iteration + 1)) {
                this.logger.info
                        ("break by max_iteration " +
                                "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
                break;
            }
            this.logger.info
                    ("------------------------------------------");
            this.logger.info("Start of iteration " + res);
            clustering = clustering(false);
            printSizes();
        }
        return res - 1;
    }


    void makeClusters() {
        this.clusters = new HashMap<Short, Cluster>();
        short[] cln = this.ds.groups();
        FirstMoments[] fm = this.ds.groupMoments();
        for (int i = 0; i < cln.length; i++) {
            if (fm[i].npoints < cont_min) {
                logger.info("ones clusters content too small: not generated");
                continue;
            }
            Cluster cl = new Cluster(cln[i]);
            printCluster(cl);
            cl.gauss = new Gaussian(fm[i].avg, fm[i].cov);
            cl.fm = fm[i];
            this.clusters.put(cln[i], cl);
        }
        this.next_cluster = (short) (cln.length + 1);
    }


    /**
     * prints the overlap matrix to the logger
     */
    public void printOverlap() {
        Cluster[] clusters = alive();

        if (clusters.length < 2) {
            return;
        }

        logger.info("overlap matrix:");
        double[][] ovm = new double[clusters.length][clusters.length];
        for (Cluster cluster : clusters) {
            logger.info("  group " + cluster.group);
        }
        logger.info("overlaps with ");
        for (int i = 0; i < clusters.length; i++) {
            for (int j = 0; j < clusters.length; j++) {
                ovm[i][j] = 0.;
            }
        }
        for (int np = 0; np < this.ds.npoints; np++) {
            double[] d = new double[clusters.length];
            for (int c = 0; c < clusters.length; c++) {
                d[c] = clusters[c].fm.swgt *
                        clusters[c].gauss.density(this.ds.pt[np]);
            }
            int fnd = General.indmax(d);
            if (clusters[fnd].gauss.distancesqu(this.ds.pt[np]) < mahadistsqCut) {
                for (int c = 0; c < clusters.length; c++) {
                    ovm[fnd][c] += d[c];
                }
                this.ds.group[np] = clusters[fnd].group;
            } else {
                this.ds.group[np] = 0;
            }
        }
        double[] sum = new double[clusters.length];
        for (int j = 0; j < clusters.length; j++) {
            sum[j] = 0.;
        }
        for (int c = 0; c < clusters.length; c++) {
            for (int j = 0; j < clusters.length; j++) {
                sum[c] += ovm[c][j];
            }
        }
        for (int j = 0; j < clusters.length; j++) {
            logger.info(clusters[j].group + "\t");
            for (int c = 0; c < clusters.length; c++) {
                logger.info((int) (1000. * ovm[c][j] / sum[c]) + "\t");
            }
        }
    }

    /**
     * prints the idents of the points belonging to the clusters
     */
    public void printClusters() {

        int[] idx;
        idx = this.ds.indices((short) 0);
        if (idx.length == 0) {
            logger.info("all points are assigned to clusters.");
        } else {
            logger.info
                    ("the following " + idx.length +
                            " points are not assigned to clusters:");
            for (int anIdx : idx) {
                logger.info("  P" + anIdx + " ");
            }
        }
        Iterator<Short> keys = this.clusters.keySet().iterator();
        logger.info("the " + this.clusters.size() +
                " clusters and their contents:");
        while (keys.hasNext()) {
            short grp = keys.next();
            Cluster cl = clusters.get(grp);
            idx = this.ds.indices(grp);
            logger.info("  cl. " + grp + " with history " + cl.history +
                    " has the following " + idx.length + " points");
            for (int anIdx : idx) {
                logger.info("    P" + anIdx + " ");
            }
        }
    }


    /**
     * prints the actual groups and their sizes
     */
    public void printSizes() {

        int[] idx;
        idx = this.ds.indices((short) 0);
        if (idx.length == 0) {
            logger.info("all points are assigned to clusters.");
        } else {
            logger.info
                    (idx.length + " points are not assigned to clusters. ");
        }
        Iterator<Short> keys = this.clusters.keySet().iterator();
        logger.info("the " + this.clusters.size() +
                " clusters and their contents:");
        while (keys.hasNext()) {
            short grp = keys.next();
            idx = this.ds.indices(grp);
            Cluster cl = clusters.get(grp);
            logger.info("  cluster " + grp + " with history " + cl.history +
                    " has " + idx.length + " points");
        }
    }

    /**
     * removes clusters with less then cont_min points
     *
     * @return the number of removed clusters
     */
    int removeSmall() {
        int res = 0;
        Iterator<Short> keys = this.clusters.keySet().iterator();
        while (keys.hasNext()) {
            short grp = keys.next();
            int[] idx = this.ds.indices(grp);
            if (idx.length < cont_min) {
                res++;
                for (int np = 0; np < this.ds.npoints; np++) {
                    if (grp == this.ds.group[np])
                        this.ds.group[np] = 0;
                }
                keys.remove();
                logger.info("cluster " + grp +
                        " removed. content was " + idx.length);
            }
        }
        return res;
    }

    /**
     * renames the groups to "1", "2",... in order of group size
     */
    public void rename() {
        Cluster[] clusters = alive();
        this.clusters = new HashMap<Short, Cluster>();
        int[][] ind = new int[clusters.length][];
        long[] cont = new long[clusters.length];
        for (int i = 0; i < clusters.length; i++) {
            ind[i] = this.ds.indices(clusters[i].group);
            cont[i] = (long) ind[i].length;
        }
        this.next_cluster = 1;
        for (Cluster cluster : clusters) {
            int ima = General.indmax(cont);
            short nn = this.next_cluster;
            this.next_cluster++;
            short on = clusters[ima].group;
            this.logger.info
                    ("old group " + on + " becomes new group " + nn);
            clusters[ima].group = nn;
            this.clusters.put(nn, clusters[ima]);
            for (int k = 0; k < ind[ima].length; k++) {
                this.ds.group[ind[ima][k]] = nn;
            }
            cont[ima] = -1;
        }
    }

    /**
     * just a clucov run. (You normally should initialize according your needs before you start it.) When finished the groups are renamed and the overlap matrix is printed.
     */
    public void run() {
        if (this.ds.npoints < 2 * this.ds.dim) {
            logger.info("no sense to cluster " +
                    this.ds.npoints + " points of dimension " +
                    this.ds.dim);
            return;
        }
        logger.info("\n--------------------------------------\nmax_iteration="
                + this.max_iteration + "\nn_planes="
                + this.n_planes + "\nsigma_cut=" + this.mahadistsqCut
                + "\nt_div=" + this.t_div + "\nt_comb=" + this.t_comb
                + "\ncont_min=" + this.cont_min + "\nmax_clusters="
                + this.max_clusters + "\nfew_changes=" + this.few_changes);
        this.iteration = 0;
        int numIter = this.iterate();
        logger.info("\nrun finished after " + numIter + " iterations.\n");
        this.rename();
        this.printOverlap();
        this.printSizes();
    }

    int side(double[] pt, double[] plane, double[] cog) {
        if (MathUtil.scalarProduct(plane,
                MathUtil.vectorSubtract(pt, cog)) < 0.)
            return 0;
        else
            return 1;
    }

    boolean split() {
        boolean res = false;
        Cluster[] clusters = alive();
        FirstMoments right, left;
        for (Cluster cluster : clusters) {
            right = cluster.right.get();
            left = cluster.left.get();
            double tv = test_ndim(right, left);
            this.logger.info
                    ("group " + cluster.group + " t=" + tv);
            boolean split = false;
            if (tv < this.t_div) {
                split = true;
                if ((right.npoints < cont_min) ||
                        (left.npoints < cont_min)) {
                    split = false;
                    this.logger.info
                            (" not splitted. Groups would be to small");
                }
                if (clusters.length > this.max_clusters - 1) {
                    split = false;
                    this.logger.info
                            (" not splitted. Would give to many groups.");
                }
            }
            if (split) {
                Cluster part;
                short nr = this.next_cluster;
                this.next_cluster++;
                part = new Cluster(nr, cluster.history + "r");
                part.fm = right;
                this.clusters.put(nr, part);
                short nl = this.next_cluster;
                this.next_cluster++;
                part = new Cluster(nl, cluster.history + "l");
                part.fm = left;
                this.clusters.put(nl, part);
                this.clusters.remove(cluster.group);
                this.logger.info
                        (" splitted into " + nr + " and " + nl);
                res = true;
            } else {
                cluster.fm = right.combine(left);
            }
        }
        return res;
    }

    double test_1(RecMom lm, RecMom rm, Gaussian l, Gaussian r,
                  double x) {
        return lm.swgt * l.density(x) + rm.swgt * r.density(x);
    }

    double test_1dim(RecMom lm, RecMom rm) {
        Gaussian l = new Gaussian(lm.avg, lm.var);
        Gaussian r = new Gaussian(rm.avg, rm.var);
        double h1 = test_1(lm, rm, l, r, lm.avg);
        double h2 = test_1(lm, rm, l, r, rm.avg);
        double dx = (lm.avg - rm.avg) / 50.;
        double df, h0 = Double.MAX_VALUE;
        for (int i = 1; i < 50; i++) {
            df = test_1(lm, rm, l, r, rm.avg + (double) i * dx);
            if (df < h0) h0 = df;
        }
        return h0 / Math.sqrt(h1 * h2);
    }

    double test_n(FirstMoments lm, FirstMoments rm,
                  Gaussian l, Gaussian r, double[] x) {
        return lm.swgt * l.density(x) + rm.swgt * r.density(x);
    }

    double test_ndim(FirstMoments lm, FirstMoments rm) {
        Gaussian l = new Gaussian(lm.avg, lm.cov);
        Gaussian r = new Gaussian(rm.avg, rm.cov);
        double h1 = test_n(lm, rm, l, r, lm.avg);
        double h2 = test_n(lm, rm, l, r, rm.avg);
        double[] dx = MathUtil.multiplyVecorWithScalar(MathUtil.vectorSubtract(lm.avg, rm.avg), 0.02
        );
        double df, h0 = Double.MAX_VALUE;
        for (int i = 1; i < 50; i++) {
            df = test_n(lm, rm, l, r, MathUtil.vectorAdd(rm.avg,
                    MathUtil.multiplyVecorWithScalar(dx, (double) i)));
            if (df < h0) h0 = df;
        }
        return h0 / Math.sqrt(h1 * h2);
    }

}

