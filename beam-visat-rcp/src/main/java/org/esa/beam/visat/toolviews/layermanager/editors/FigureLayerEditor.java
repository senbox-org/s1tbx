package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.binding.swing.PropertyEditorRegistry;
import com.bc.ceres.binding.swing.internal.RangeEditor;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.glayer.FigureLayer;

import java.awt.Color;

/**
 * @author Marco Peters
 * @version $ Revision: $ Date: $
 * @since BEAM 4.6
 */
public class FigureLayerEditor extends AbstractBindingLayerEditor {

    @Override
    protected void initializeBinding(AppContext appContext, final BindingContext bindingContext) {

        PropertyDescriptor vd0 = new PropertyDescriptor(FigureLayer.PROPERTY_NAME_SHAPE_OUTLINED, Boolean.class);
        vd0.setDefaultValue(FigureLayer.DEFAULT_SHAPE_OUTLINED);
        vd0.setDisplayName("Outline shape");
        vd0.setDefaultConverter();
        addValueDescriptor(vd0);

        PropertyDescriptor vd1 = new PropertyDescriptor(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_COLOR, Color.class);
        vd1.setDefaultValue(FigureLayer.DEFAULT_SHAPE_OUTL_COLOR);
        vd1.setDisplayName("Outline colour");
        vd1.setDefaultConverter();
        addValueDescriptor(vd1);

        PropertyDescriptor vd2 = new PropertyDescriptor(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_WIDTH, Double.class);
        vd2.setDefaultValue(FigureLayer.DEFAULT_SHAPE_OUTL_WIDTH);
        vd2.setDisplayName("Outline width");
        vd2.setDefaultConverter();
        addValueDescriptor(vd2);

        final PropertyEditorRegistry propertyEditorRegistry = PropertyEditorRegistry.getInstance();

        PropertyDescriptor vd3 = new PropertyDescriptor(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_TRANSPARENCY, Double.class);
        vd3.setDefaultValue(FigureLayer.DEFAULT_SHAPE_OUTL_TRANSPARENCY);
        vd3.setDisplayName("Outline transparency");
        vd3.setValueRange(new ValueRange(0, 0.95));
        vd3.setDefaultConverter();
        vd3.setAttribute("valueEditor", propertyEditorRegistry.getValueEditor(RangeEditor.class.getName()));
        addValueDescriptor(vd3);


        PropertyDescriptor vd4 = new PropertyDescriptor(FigureLayer.PROPERTY_NAME_SHAPE_FILLED, Boolean.class);
        vd4.setDefaultValue(FigureLayer.DEFAULT_SHAPE_FILLED);
        vd4.setDisplayName("Fill shape");
        vd4.setDefaultConverter();
        addValueDescriptor(vd4);

        PropertyDescriptor vd5 = new PropertyDescriptor(FigureLayer.PROPERTY_NAME_SHAPE_FILL_COLOR, Color.class);
        vd5.setDefaultValue(FigureLayer.DEFAULT_SHAPE_FILL_COLOR);
        vd5.setDisplayName("Fill colour");
        vd5.setDefaultConverter();
        addValueDescriptor(vd5);

        PropertyDescriptor vd6 = new PropertyDescriptor(FigureLayer.PROPERTY_NAME_SHAPE_FILL_TRANSPARENCY, Double.class);
        vd6.setDefaultValue(FigureLayer.DEFAULT_SHAPE_FILL_TRANSPARENCY);
        vd6.setDisplayName("Fill transparency");
        vd6.setValueRange(new ValueRange(0, 0.95));
        vd6.setDefaultConverter();
        vd6.setAttribute("valueEditor", propertyEditorRegistry.getValueEditor(RangeEditor.class.getName()));
        addValueDescriptor(vd6);

        boolean outlined = (Boolean) bindingContext.getPropertySet().getValue(
                FigureLayer.PROPERTY_NAME_SHAPE_OUTLINED);
        bindingContext.bindEnabledState(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_COLOR, outlined,
                                        FigureLayer.PROPERTY_NAME_SHAPE_OUTLINED, outlined);
        bindingContext.bindEnabledState(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_TRANSPARENCY, outlined,
                                        FigureLayer.PROPERTY_NAME_SHAPE_OUTLINED, outlined);
        bindingContext.bindEnabledState(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_WIDTH, outlined,
                                        FigureLayer.PROPERTY_NAME_SHAPE_OUTLINED, outlined);

        boolean filled = (Boolean) bindingContext.getPropertySet().getValue(FigureLayer.PROPERTY_NAME_SHAPE_FILLED);
        bindingContext.bindEnabledState(FigureLayer.PROPERTY_NAME_SHAPE_FILL_COLOR, filled,
                                        FigureLayer.PROPERTY_NAME_SHAPE_FILLED, filled);
        bindingContext.bindEnabledState(FigureLayer.PROPERTY_NAME_SHAPE_FILL_TRANSPARENCY, filled,
                                        FigureLayer.PROPERTY_NAME_SHAPE_FILLED, filled);

    }

}