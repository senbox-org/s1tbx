package org.esa.beam.visat.toolviews.stat;

import com.bc.ceres.binding.Property;
import com.bc.ceres.swing.binding.BindingContext;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import javax.management.Descriptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Norman Fomferra
 */
public class CorrelativeFieldSelectorTest {

    @Test
    public void testExpectedProperties() throws Exception {

        final BindingContext bindingContext = new BindingContext();
        try {
            new CorrelativeFieldSelector(bindingContext);
            fail();
        } catch (IllegalArgumentException expected) {
        }
        bindingContext.getPropertySet().addProperties(Property.create("pointDataSource", ProductNode.class));
        try {
            new CorrelativeFieldSelector(bindingContext);
            fail();
        } catch (IllegalArgumentException expected) {
        }
        bindingContext.getPropertySet().addProperties(Property.create("pointDataSource", VectorDataNode.class));
        try {
            new CorrelativeFieldSelector(bindingContext);
            fail();
        } catch (IllegalArgumentException expected) {
        }
        bindingContext.getPropertySet().addProperties(Property.create("dataField", Descriptor.class));
        try {
            new CorrelativeFieldSelector(bindingContext);
            fail();
        } catch (IllegalArgumentException expected) {
        }
        bindingContext.getPropertySet().addProperties(Property.create("dataField", AttributeDescriptor.class));
        new CorrelativeFieldSelector(bindingContext);
    }

    @Test
    public void testUpdatePointDataSource() throws Exception {
        final BindingContext bindingContext = new BindingContext();
        bindingContext.getPropertySet().addProperties(Property.create("pointDataSource", VectorDataNode.class));
        bindingContext.getPropertySet().addProperties(Property.create("dataField", AttributeDescriptor.class));

        final CorrelativeFieldSelector correlativeFieldSelector = new CorrelativeFieldSelector(bindingContext);
        final Product product = new Product("name", "type", 10, 10);
        product.getVectorDataGroup().add(new VectorDataNode("a", createFeatureType(Geometry.class)));
        product.getVectorDataGroup().add(new VectorDataNode("b", createFeatureType(Point.class)));

        assertEquals(0, correlativeFieldSelector.pointDataSourceList.getItemCount());
        assertEquals(0, correlativeFieldSelector.dataFieldList.getItemCount());

        correlativeFieldSelector.updatePointDataSource(product);

        assertEquals(1, correlativeFieldSelector.pointDataSourceList.getItemCount());
        assertEquals(0, correlativeFieldSelector.dataFieldList.getItemCount());

        correlativeFieldSelector.pointDataSourceProperty.setValue(product.getVectorDataGroup().get("b"));

        assertEquals(2, correlativeFieldSelector.dataFieldList.getItemCount());
    }

    private SimpleFeatureType createFeatureType(Class binding) {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("featureType");
        builder.add("chl", Float.class);
        builder.add("sst", Double.class);
        builder.add("time", ProductData.UTC.class);
        builder.add("point", binding);
        builder.setDefaultGeometry("point");
        return builder.buildFeatureType();
    }
}
