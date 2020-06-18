/*
 * Copyright (C) 2020 Sensar B.V. http://www.sensar.nl
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
package org.esa.s1tbx.sentinel1.gpf.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Utility class for graphs.
 *
 * @author David A. Monge
 */
public class GraphUtils {

    /**
     * Implements a deep-first search to determine if the graph is connected.
     * @param arcs
     * @param noOfNodes
     * @return
     */
    public static boolean isConnectedGraph(int[][] arcs, int noOfNodes) {
        return isConnectedGraph(Arrays.asList(arcs), noOfNodes);
    }

    /**
     * Implements a deep-first search to determine if the graph is connected.
     * @param arcs
     * @param noOfNodes
     * @return
     */
    public static boolean isConnectedGraph(List<int[]> arcs, int noOfNodes) {
        Set<Integer> visitedNodes = new HashSet<>();

        visitNode(arcs.get(0)[0], arcs, visitedNodes);  // visit source node of first arc

        return visitedNodes.size() == noOfNodes;
    }

    /**
     * Deep-first search.
     * @param node
     * @param arcs
     * @param visitedNodes
     */
    private static void visitNode(int node, List<int[]> arcs, Set<Integer> visitedNodes) {
        if (visitedNodes.contains(node)) {
            return;
        } else {
            // register as visited
            visitedNodes.add(node);

            // visit children
            for (int[] arc : arcs) {
                if (arc[0] == node) {  // for arcs: node -> child
                    visitNode(arc[1], arcs, visitedNodes);  // visit child
                }
                if (arc[1] == node) {  // for arcs: parent -> node
                    visitNode(arc[0], arcs, visitedNodes);  // visit parent
                }
            }
        }
    }
}
