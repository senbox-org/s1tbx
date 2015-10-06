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

package org.esa.snap.core.gpf.graph;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.esa.snap.core.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * A list of {@link NodeSource}s.
 */
public class SourceList {

    private List<NodeSource> sourceList = new ArrayList<>();

    /**
     * Gets all {@link NodeSource}s.
     *
     * @return the {@link NodeSource}s
     */
    public NodeSource[] getSources() {
        return sourceList.toArray(new NodeSource[sourceList.size()]);
    }

    /**
     * Gets the {@link NodeSource} at the given index.
     *
     * @param index the index
     *
     * @return the {@link NodeSource}
     */
    public NodeSource getSource(int index) {
        return sourceList.get(index);
    }

    /**
     * Adds a {@link NodeSource} to this list.
     *
     * @param source the {@link NodeSource}
     */
    public void addSource(NodeSource source) {
        final String sourceId = source.getSourceNodeId();  //check by source id, not name
        for (NodeSource nodeSource : sourceList) {
            if (nodeSource.getSourceNodeId().equals(sourceId)) {
                throw new IllegalArgumentException("duplicated source node id");
            }
        }
        sourceList.add(source);
    }

    /**
     * Removes a {@link NodeSource} from this list.
     *
     * @param source the {@link NodeSource}
    */
    public void removeSource(NodeSource source) {
        sourceList.remove(source);
    }
    
    public static class Converter implements com.thoughtworks.xstream.converters.Converter {

        @Override
        public boolean canConvert(Class aClass) {
            return SourceList.class.equals(aClass);
        }

        @Override
        public void marshal(Object object, HierarchicalStreamWriter hierarchicalStreamWriter,
                            MarshallingContext marshallingContext) {
            SourceList sourceList = (SourceList) object;
            NodeSource[] sources = sourceList.getSources();
            for (NodeSource source : sources) {
                hierarchicalStreamWriter.startNode(source.getName());
                hierarchicalStreamWriter.addAttribute("refid", source.getSourceNodeId());
                hierarchicalStreamWriter.endNode();
            }
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader hierarchicalStreamReader,
                                UnmarshallingContext unmarshallingContext) {
            SourceList sourceList = new SourceList();
            while (hierarchicalStreamReader.hasMoreChildren()) {
                hierarchicalStreamReader.moveDown();
                String name = hierarchicalStreamReader.getNodeName();
                if ("sourceProducts".equals(name)) {
                    final String sourceNodeIdsString = hierarchicalStreamReader.getValue().trim();
                    if (StringUtils.isNotNullAndNotEmpty(sourceNodeIdsString)) {
                        final String[] sourceNodeIds = StringUtils.csvToArray(sourceNodeIdsString);
                        for (int i = 0; i < sourceNodeIds.length; i++) {
                            String sourceNodeId = sourceNodeIds[i];
                            sourceList.addSource(new NodeSource(String.format("%s%d", name, i), sourceNodeId));
                        }
                    }

                } else {
                    String refid = hierarchicalStreamReader.getAttribute("refid");
                    if (refid == null) {
                        refid = hierarchicalStreamReader.getValue().trim();
                    }
                    sourceList.addSource(new NodeSource(name, refid));
                }
                hierarchicalStreamReader.moveUp();
            }
            return sourceList;
        }
    }
}
