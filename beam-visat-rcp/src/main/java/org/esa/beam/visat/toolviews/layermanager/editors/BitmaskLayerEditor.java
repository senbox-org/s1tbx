package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyAccessor;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.swing.BindingContext;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.glayer.BitmaskLayerType;

import java.awt.Color;

/**
 * Editor for bitmask layers.
 *
 * @author Ralf Quast
 * @version $ Revision: $ $ Date: $
 * @since BEAM 4.6
 */
public class BitmaskLayerEditor extends ImageLayerEditor {

    @Override
    protected void initializeBinding(AppContext appContext, BindingContext bindingContext) {
        super.initializeBinding(appContext, bindingContext);

        final PropertySet vc = bindingContext.getPropertySet();
        addBitmaskColorValueModel(vc);
        addBitmaskTransparencyValueModel(vc);
    }

    private void addBitmaskColorValueModel(PropertySet vc) {
        final Property model = getLayer().getConfiguration().getProperty(BitmaskLayerType.PROPERTY_NAME_BITMASK_DEF);
        final PropertyAccessor accessor = new BitmaskColorAccessor(model);
        final PropertyDescriptor descriptor = new PropertyDescriptor("bitmaskColor", Color.class);
        descriptor.setDisplayName("Bitmask colour");
        descriptor.setDefaultConverter();

        vc.addProperty(new Property(descriptor, accessor));
    }

    private void addBitmaskTransparencyValueModel(PropertySet vc) {
        final Property model = getLayer().getConfiguration().getProperty(BitmaskLayerType.PROPERTY_NAME_BITMASK_DEF);
        final PropertyAccessor accessor = new BitmaskTransparencyAccessor(model);
        final PropertyDescriptor descriptor = new PropertyDescriptor("bitmaskTransparency", Float.class);
        descriptor.setDisplayName("Bitmask transparency");
        descriptor.setDefaultConverter();

        vc.addProperty(new Property(descriptor, accessor));
    }

    private static class BitmaskColorAccessor extends BitmaskPropertyAccessor<Color> {

        BitmaskColorAccessor(Property property) {
            super(property);
        }

        @Override
        protected Color getProperty(BitmaskDef bitmaskDef) {
            return bitmaskDef.getColor();
        }

        @Override
        protected void setProperty(BitmaskDef bitmaskDef, Color color) {
            bitmaskDef.setColor(color);
        }
    }

    private static class BitmaskTransparencyAccessor extends BitmaskPropertyAccessor<Float> {

        BitmaskTransparencyAccessor(Property property) {
            super(property);
        }

        @Override
        protected Float getProperty(BitmaskDef bitmaskDef) {
            return bitmaskDef.getTransparency();
        }

        @Override
        protected void setProperty(BitmaskDef bitmaskDef, Float f) {
            bitmaskDef.setTransparency(f);
        }
    }

    private static abstract class BitmaskPropertyAccessor<T> implements PropertyAccessor {

        private final Property property;

        protected BitmaskPropertyAccessor(Property property) {
            this.property = property;
        }

        protected abstract T getProperty(BitmaskDef bitmaskDef);

        protected abstract void setProperty(BitmaskDef bitmaskDef, T t);

        @Override
        public final Object getValue() {
            final BitmaskDef bitmaskDef = (BitmaskDef) property.getValue();
            if (bitmaskDef != null) {
                return getProperty(bitmaskDef);
            }
            return null;
        }

        @Override
        public final void setValue(Object value) {
            final BitmaskDef bitmaskDef = (BitmaskDef) property.getValue();
            if (bitmaskDef != null) {
                @SuppressWarnings({"unchecked"})
                final T t = (T) value;
                setProperty(bitmaskDef, t);
            }
        }
    }
}
