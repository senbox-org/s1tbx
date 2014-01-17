/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.nest.supervised.tree;

/**
 *
 * @author emmab Called each time an instance has been classified
 */
public interface PredictionCallback {

    /**
     * called when an instance has been classified
     *
     * @param treeId tree that classified the instance
     * @param instanceId classified instance
     * @param prediction predicted label
     */
    void prediction(int treeId, int instanceId, int prediction);
}
