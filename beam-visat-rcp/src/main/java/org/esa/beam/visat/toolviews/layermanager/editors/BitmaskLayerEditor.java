package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.binding.ValueAccessor;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueModel;
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

        final ValueContainer vc = bindingContext.getValueContainer();
        addBitmaskColorValueModel(vc);
        addBitmaskTransparencyValueModel(vc);
    }

    private void addBitmaskColorValueModel(ValueContainer vc) {
        final ValueModel model = getLayer().getConfiguration().getModel(BitmaskLayerType.PROPERTY_NAME_BITMASK_DEF);
        final ValueAccessor accessor = new BitmaskColorAccessor(model);
        final ValueDescriptor descriptor = new ValueDescriptor("bitmaskColor", Color.class);
        descriptor.setDisplayName("Bitmask colour");
        descriptor.setDefaultConverter();

        vc.addModel(new ValueModel(descriptor, accessor));
    }

    private void addBitmaskTransparencyValueModel(ValueContainer vc) {
        final ValueModel model = getLayer().getConfiguration().getModel(BitmaskLayerType.PROPERTY_NAME_BITMASK_DEF);
        final ValueAccessor accessor = new BitmaskTransparencyAccessor(model);
        final ValueDescriptor descriptor = new ValueDescriptor("bitmaskTransparency", Float.class);
        descriptor.setDisplayName("Bitmask transparency");
        descriptor.setDefaultConverter();

        vc.addModel(new ValueModel(descriptor, accessor));
    }

    private static class BitmaskColorAccessor extends BitmaskPropertyAccessor<Color> {

        BitmaskColorAccessor(ValueModel valueModel) {
            super(valueModel);
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

        BitmaskTransparencyAccessor(ValueModel valueModel) {
            super(valueModel);
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

    private static abstract class BitmaskPropertyAccessor<T> implements ValueAccessor {

        private final ValueModel valueModel;

        protected BitmaskPropertyAccessor(ValueModel valueModel) {
            this.valueModel = valueModel;
        }

        protected abstract T getProperty(BitmaskDef bitmaskDef);

        protected abstract void setProperty(BitmaskDef bitmaskDef, T t);

        @Override
        public final Object getValue() {
            final BitmaskDef bitmaskDef = (BitmaskDef) valueModel.getValue();
            if (bitmaskDef != null) {
                return getProperty(bitmaskDef);
            }
            return null;
        }

        @Override
        public final void setValue(Object value) {
            final BitmaskDef bitmaskDef = (BitmaskDef) valueModel.getValue();
            if (bitmaskDef != null) {
                @SuppressWarnings({"unchecked"})
                final T t = (T) value;
                setProperty(bitmaskDef, t);
            }
        }
    }
}
