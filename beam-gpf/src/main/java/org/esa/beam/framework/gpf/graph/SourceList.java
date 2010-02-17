package org.esa.beam.framework.gpf.graph;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.esa.beam.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * A list of {@link NodeSource}s.
 */
public class SourceList {

    private List<NodeSource> sourceList = new ArrayList<NodeSource>();

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
        sourceList.add(source);
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
                    if(StringUtils.isNotNullAndNotEmpty(sourceNodeIdsString)) {
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