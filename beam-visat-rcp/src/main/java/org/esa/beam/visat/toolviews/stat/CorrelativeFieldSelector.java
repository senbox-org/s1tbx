package org.esa.beam.visat.toolviews.stat;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.core.Assert;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Norman Fomferra
 */
class CorrelativeFieldSelector {

    final JLabel pointDataSourceLabel;
    final JComboBox pointDataSourceList;
    final JLabel dataFieldLabel;
    final JComboBox dataFieldList;

    final Property pointDataSourceProperty;
    final Property dataFieldProperty;

    final static String NULL_NAME = " ";

    CorrelativeFieldSelector(BindingContext bindingContext) {
        Assert.argument(bindingContext.getPropertySet().getProperty("pointDataSource") != null, "bindingContext");
        Assert.argument(bindingContext.getPropertySet().getProperty("dataField") != null, "bindingContext");
        Assert.argument(bindingContext.getPropertySet().getProperty("pointDataSource").getType().equals(VectorDataNode.class), "bindingContext");
        Assert.argument(bindingContext.getPropertySet().getProperty("dataField").getType().equals(AttributeDescriptor.class), "bindingContext");

        pointDataSourceLabel = new JLabel("Point data source:");
        pointDataSourceList = new JComboBox();
        dataFieldLabel = new JLabel("Data field:");
        dataFieldList = new JComboBox();

        pointDataSourceList.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) {
                    this.setText(((VectorDataNode) value).getName());
                }
                return this;
            }
        });

        dataFieldList.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) {
                    this.setText(((AttributeDescriptor) value).getName().getLocalPart());
                }
                return this;
            }
        });


        bindingContext.bind("pointDataSource", pointDataSourceList);
        pointDataSourceProperty = bindingContext.getPropertySet().getProperty("pointDataSource");
        dataFieldProperty = bindingContext.getPropertySet().getProperty("dataField");
        pointDataSourceProperty.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateDataField();
            }
        });
        bindingContext.getBinding("pointDataSource").addComponent(pointDataSourceLabel);
        bindingContext.bind("dataField", dataFieldList);
        bindingContext.getBinding("dataField").addComponent(dataFieldLabel);
    }


    public void updatePointDataSource(Product product) {
        if (product != null) {
            final Class pointClass = com.vividsolutions.jts.geom.Point.class;
            final ProductNodeGroup<VectorDataNode> vectorDataGroup = product.getVectorDataGroup();
            final List<VectorDataNode> vectorDataNodes = new ArrayList<VectorDataNode>();
            for (VectorDataNode vectorDataNode : vectorDataGroup.toArray(new VectorDataNode[vectorDataGroup.getNodeCount()])) {
                final GeometryDescriptor geometryDescriptor = vectorDataNode.getFeatureType().getGeometryDescriptor();
                if (geometryDescriptor != null &&
                        pointClass.isAssignableFrom(geometryDescriptor.getType().getBinding())) {
                    vectorDataNodes.add(vectorDataNode);
                }
            }
            final ValueSet valueSet = new ValueSet(vectorDataNodes.toArray());
            pointDataSourceProperty.getDescriptor().setValueSet(valueSet);
        } else {
            pointDataSourceProperty.getDescriptor().setValueSet(null);
            dataFieldProperty.getDescriptor().setValueSet(null);
            try {
                pointDataSourceProperty.setValue(null);
                dataFieldProperty.setValue(null);
            } catch (ValidationException ignore) {
            }
        }
    }

    public void updateDataField() {
        if (pointDataSourceProperty.getValue() != null) {
            final List<AttributeDescriptor> attributeDescriptors = ((VectorDataNode) pointDataSourceProperty.getValue()).getFeatureType().getAttributeDescriptors();
            final List<AttributeDescriptor> result = new ArrayList<AttributeDescriptor>();
            result.add(new NullAttributeDescriptor());
            for (AttributeDescriptor attributeDescriptor : attributeDescriptors) {
                if (Number.class.isAssignableFrom(attributeDescriptor.getType().getBinding())) {
                    result.add(attributeDescriptor);
                }
            }
            dataFieldProperty.getDescriptor().setValueSet(new ValueSet(result.toArray()));
        } else {
            dataFieldProperty.getDescriptor().setValueSet(null);
            try {
                dataFieldProperty.setValue(null);
            } catch (ValidationException ignore) {
            }
        }
    }

    public void tryToSelectPointDataSource(VectorDataNode pointDataSource) {
                pointDataSourceList.setSelectedItem(pointDataSource);
    }

    public void tryToSelectDataField(AttributeDescriptor dataField) {
                dataFieldList.setSelectedItem(dataField);
    }

    private class NullAttributeDescriptor implements AttributeDescriptor {

        @Override
        public AttributeType getType() {
            return null;
        }

        @Override
        public String getLocalName() {
            return NULL_NAME;
        }

        @Override
        public Object getDefaultValue() {
            return null;
        }

        @Override
        public Name getName() {
            return new NameImpl(NULL_NAME);
        }

        @Override
        public int getMinOccurs() {
            return 0;
        }

        @Override
        public int getMaxOccurs() {
            return 0;
        }

        @Override
        public boolean isNillable() {
            return true;
        }

        @Override
        public Map<Object, Object> getUserData() {
            return null;
        }
    }
}
