package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.binding.swing.ValueEditorRegistry;
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

        ValueDescriptor vd0 = new ValueDescriptor(FigureLayer.PROPERTY_NAME_SHAPE_OUTLINED, Boolean.class);
        vd0.setDefaultValue(FigureLayer.DEFAULT_SHAPE_OUTLINED);
        vd0.setDisplayName("Outline shape");
        vd0.setDefaultConverter();
        addValueDescriptor(vd0);

        ValueDescriptor vd1 = new ValueDescriptor(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_COLOR, Color.class);
        vd1.setDefaultValue(FigureLayer.DEFAULT_SHAPE_OUTL_COLOR);
        vd1.setDisplayName("Shape outline colour");
        vd1.setDefaultConverter();
        addValueDescriptor(vd1);

        ValueDescriptor vd2 = new ValueDescriptor(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_WIDTH, Double.class);
        vd2.setDefaultValue(FigureLayer.DEFAULT_SHAPE_OUTL_WIDTH);
        vd2.setDisplayName("Shape outline width");
        vd2.setDefaultConverter();
        addValueDescriptor(vd2);

        final ValueEditorRegistry valueEditorRegistry = ValueEditorRegistry.getInstance();

        ValueDescriptor vd3 = new ValueDescriptor(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_TRANSPARENCY, Double.class);
        vd3.setDefaultValue(FigureLayer.DEFAULT_SHAPE_OUTL_TRANSPARENCY);
        vd3.setDisplayName("Shape outline transparency");
        vd3.setValueRange(new ValueRange(0, 0.95));
        vd3.setDefaultConverter();
        vd3.setProperty("valueEditor", valueEditorRegistry.getValueEditor(RangeEditor.class.getName()));
        addValueDescriptor(vd3);


        ValueDescriptor vd4 = new ValueDescriptor(FigureLayer.PROPERTY_NAME_SHAPE_FILLED, Boolean.class);
        vd4.setDefaultValue(FigureLayer.DEFAULT_SHAPE_FILLED);
        vd4.setDisplayName("Fill shape");
        vd4.setDefaultConverter();
        addValueDescriptor(vd4);

        ValueDescriptor vd5 = new ValueDescriptor(FigureLayer.PROPERTY_NAME_SHAPE_FILL_COLOR, Color.class);
        vd5.setDefaultValue(FigureLayer.DEFAULT_SHAPE_FILL_COLOR);
        vd5.setDisplayName("Shape fill colour");
        vd5.setDefaultConverter();
        addValueDescriptor(vd5);

        ValueDescriptor vd6 = new ValueDescriptor(FigureLayer.PROPERTY_NAME_SHAPE_FILL_TRANSPARENCY, Double.class);
        vd6.setDefaultValue(FigureLayer.DEFAULT_SHAPE_FILL_TRANSPARENCY);
        vd6.setDisplayName("Shape fill transparency");
        vd6.setValueRange(new ValueRange(0, 0.95));
        vd6.setDefaultConverter();
        vd6.setProperty("valueEditor", valueEditorRegistry.getValueEditor(RangeEditor.class.getName()));
        addValueDescriptor(vd6);

        boolean outlined = (Boolean) bindingContext.getValueContainer().getValue(
                FigureLayer.PROPERTY_NAME_SHAPE_OUTLINED);
        bindingContext.bindEnabledState(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_COLOR, outlined,
                                        FigureLayer.PROPERTY_NAME_SHAPE_OUTLINED, outlined);
        bindingContext.bindEnabledState(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_TRANSPARENCY, outlined,
                                        FigureLayer.PROPERTY_NAME_SHAPE_OUTLINED, outlined);
        bindingContext.bindEnabledState(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_WIDTH, outlined,
                                        FigureLayer.PROPERTY_NAME_SHAPE_OUTLINED, outlined);

        boolean filled = (Boolean) bindingContext.getValueContainer().getValue(FigureLayer.PROPERTY_NAME_SHAPE_FILLED);
        bindingContext.bindEnabledState(FigureLayer.PROPERTY_NAME_SHAPE_FILL_COLOR, filled,
                                        FigureLayer.PROPERTY_NAME_SHAPE_FILLED, filled);
        bindingContext.bindEnabledState(FigureLayer.PROPERTY_NAME_SHAPE_FILL_TRANSPARENCY, filled,
                                        FigureLayer.PROPERTY_NAME_SHAPE_FILLED, filled);

    }

}