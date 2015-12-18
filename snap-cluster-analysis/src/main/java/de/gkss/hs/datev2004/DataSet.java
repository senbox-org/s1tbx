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

import Jama.Matrix;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * A set of idented, weighted data which might be grouped
 *
 * @author H. Schiller / GKSS
 */
public class DataSet {
    /**
     * the number of points belonging to the data set
     */
    public int npoints;
    /**
     * the dimension of the points
     */
    public int dim;
    /**
     * points pt[npoints][dim]
     */
    public double[][] pt;
    /**
     * weights wgt[npoints]
     */
    public double[] wgt;
    /**
     * group[npoints]
     */
    public short[] group;
    private int np;

    /**
     * an empty data set; points have to be added to it.
     *
     * @param npoints is the number of points in the data set
     * @param dim     is the dimension of the points
     */
    public DataSet(int npoints, int dim) {
        //-- -- -- -- -- -- -- -- -- -- -- -- -- -- -
        this.npoints = npoints;
        this.dim = dim;
        this.pt = new double[npoints][dim];
        this.wgt = new double[npoints];
        this.group = new short[npoints];
        for (int i = 0; i < this.npoints; i++) {
            this.group[i] = 0;
            this.wgt[i] = 1.;
        }
        this.np = 0;
    }


    /**
     * @param filename the name of the input data file
     */
    public DataSet(String filename) throws IOException {
        //-- -- -- -- -- -- -- -- -- -- -- -- -- -- -
        try {
            int weighted;
            int dimension;
            double[][] df;
            try (BufferedReader in = new BufferedReader(new FileReader(filename))) {
                String eing = "";
                while (!eing.equals("$")) {
                    eing = in.readLine();
                }
                eing = in.readLine();
                StringTokenizer stok = new StringTokenizer(eing);
                weighted = Integer.valueOf(stok.nextToken());
                dimension = Integer.valueOf(stok.nextToken());
                df = Matrix.read(in).getArray();
            }
            this.npoints = df.length;
            this.np = this.npoints;
            this.dim = df[0].length - 1 - weighted;
            if (this.dim != dimension) {
                throw new IOException("bad mdda file" + filename);
            }
            //System.err.println(weighted+" "+dimension+" "+this.np);
            this.pt = new double[this.npoints][this.dim];
            this.wgt = new double[this.npoints];
            this.group = new short[this.npoints];
            for (int n = 0; n < this.npoints; n++) {
                if (weighted == 0) {
                    System.arraycopy(df[n], 1, this.pt[n], 0, this.dim);
                    this.wgt[n] = 1.;
                } else {
                    System.arraycopy(df[n], 2, this.pt[n], 0, this.dim);
                    this.wgt[n] = df[n][1];
                }
            }
        } catch (NoSuchElementException e) {
            throw new IOException("file has wrong structure ");
        } catch (NumberFormatException e) {
            throw new IOException("bad number format " + e);
        } catch (FileNotFoundException e) {
            throw new IOException("file not found " + e);
        }
    }


    /**
     * add point to data set; ident from enumeration, group zero, weight 1.0
     *
     * @param pt [dim] point
     */

    public void add(double[] pt) {
        //-- -- -- -- -- -- -- -- -- -
        if (this.np == this.npoints) {
            throw new IllegalArgumentException("DataSet.add: to many points. "
                    + this.npoints + " were declared.");
        }
        if (this.dim != pt.length) {
            throw new IllegalArgumentException("DataSet.add: bad dimension"
                    + pt.length + ". dim of DataSet=" + this.dim);
        }
        System.arraycopy(pt, 0, this.pt[this.np], 0, this.dim);
        this.wgt[np] = 1.0;
        this.group[np] = 0;
        this.np++;
    }


    /**
     * @param group the group to be extracted
     * @return the indices of the points in the <code>group</code>
     */
    public int[] indices(short group) {
        //-----------------------------
        int times = 0;
        for (int i = 0; i < this.npoints; i++) {
            if (group == this.group[i]) {
                times++;
            }
        }
        //System.err.println(group+" "+times);
        int[] res = new int[times];
        times = 0;
        for (int i = 0; i < this.npoints; i++) {
            if (group == this.group[i]) {
                res[times] = i;
                times++;
            }
        }
        return res;
    }


    /**
     * @return the groups occurring in this data set
     */
    public short[] groups() {
        //----------------------------------
        short[] gr = new short[this.npoints];
        short[] gn = new short[this.npoints];
        System.arraycopy(this.group, 0, gr, 0, this.npoints);
        boolean ready = false;
        int next = 0;
        int ngr = 0;
        while (!ready) {
            short extrgr = gr[next];
            gn[ngr] = extrgr;
            ngr++;
            //int last = next;
            for (int i = 0; i < this.npoints; i++) {
                if (gr[i] == extrgr)
                    gr[i] = -1; // "hopefully never occurs";
            }
            ready = true;
            for (int i = 0; i < this.npoints; i++) {
                if (gr[i] != -1) { //.equals("hopefully never occurs")) {
                    next = i;
                    ready = false;
                    break;
                }
            }
        }
        short[] res = new short[ngr];
        System.arraycopy(gn, 0, res, 0, ngr);
        return res;
    }


    /**
     * @return one set of moments for each group in this data set
     */
    public FirstMoments[] groupMoments() {
        //----------------------------
        short[] gr = this.groups();
        FirstMoments[] res = new FirstMoments[gr.length];
        for (int g = 0; g < gr.length; g++) {
            res[g] = new FirstMoments(this.dim);
        }
        for (int n = 0; n < this.npoints; n++) {
            int g = -1;
            for (int gg = 0; gg < gr.length; gg++) {
                if (this.group[n] == gr[gg]) {
                    g = gg;
                    break;
                }
            }
            res[g].npoints++;
            res[g].swgt += this.wgt[n];
            for (int i = 0; i < this.dim; i++) {
                res[g].avg[i] += this.wgt[n] * this.pt[n][i];
            }
        }
        for (int g = 0; g < gr.length; g++) {
            for (int d = 0; d < this.dim; d++) {
                res[g].avg[d] /= res[g].swgt;
            }
        }
        for (int n = 0; n < this.npoints; n++) {
            int g = -1;
            for (int gg = 0; gg < gr.length; gg++) {
                if (this.group[n] == gr[gg]) {
                    g = gg;
                    break;
                }
            }
            for (int i = 0; i < this.dim; i++) {
                for (int j = 0; j < this.dim; j++) {
                    res[g].cov[i][j] += this.wgt[n] *
                            (this.pt[n][i] - res[g].avg[i]) *
                            (this.pt[n][j] - res[g].avg[j]);
                }
            }
        }
        for (int g = 0; g < gr.length; g++) {
            for (int i = 0; i < this.dim; i++) {
                for (int j = 0; j < this.dim; j++) {
                    res[g].cov[i][j] /= res[g].swgt;
                }
            }
        }
        return res;
    }

}
