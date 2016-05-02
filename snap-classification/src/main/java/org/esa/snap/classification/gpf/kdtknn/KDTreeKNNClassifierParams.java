
/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.classification.gpf.kdtknn;

import net.sf.javaml.core.Dataset;

import java.io.Serializable;

/**
 * To encapsulate all parameters required to create an KDTreeKNNClassifier object
 */
public class KDTreeKNNClassifierParams implements Serializable {

    private int numNeighbours;
    private Dataset trainDataset;

    KDTreeKNNClassifierParams(final int numNeighbours, final Dataset trainDataset) {
        this.numNeighbours = numNeighbours;
        this.trainDataset = trainDataset;
    }

    public int getNumNeighbours() {
        return numNeighbours;
    }

    public Dataset getTrainDataset() {
        return trainDataset;
    }
}
