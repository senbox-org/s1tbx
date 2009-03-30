/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.binding.swing.Binding;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.glayer.Layer;

import org.esa.beam.glayer.GraticuleLayer;
import org.esa.beam.visat.toolviews.layermanager.LayerEditor;

import java.awt.Color;

import javax.swing.JComponent;

/**
 * Editor for graticule layer.
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class GraticuleLayerEditor implements LayerEditor {

    private final ValueDescriptorLayerEditor delegate;
    
    public GraticuleLayerEditor() {
        delegate = new ValueDescriptorLayerEditor(createVDs());
    }
    
    @Override
    public JComponent createControl() {
        return delegate.createControl();
    }

    @Override
    public void updateControl(Layer selectedLayer) {
        delegate.updateControl(selectedLayer);
        BindingContext bindingContext = delegate.getBindingContext();
        
        boolean resAuto = (Boolean) bindingContext.getValueContainer().getValue(GraticuleLayer.PROPERTY_NAME_RES_AUTO);
        setEditorEnableState(GraticuleLayer.PROPERTY_NAME_RES_PIXELS, resAuto);
        setEditorEnableState(GraticuleLayer.PROPERTY_NAME_RES_LAT, !resAuto);
        setEditorEnableState(GraticuleLayer.PROPERTY_NAME_RES_LON, !resAuto);
        
        boolean textEnabled = (Boolean) bindingContext.getValueContainer().getValue(GraticuleLayer.PROPERTY_NAME_TEXT_ENABLED);
        setEditorEnableState(GraticuleLayer.PROPERTY_NAME_TEXT_FG_COLOR, textEnabled);
        setEditorEnableState(GraticuleLayer.PROPERTY_NAME_TEXT_BG_COLOR, textEnabled);
        setEditorEnableState(GraticuleLayer.PROPERTY_NAME_TEXT_BG_TRANSPARENCY, textEnabled);
    }
    
    private void setEditorEnableState(String propertyBame, boolean enabled) {
        Binding pixelBinding = delegate.getBindingContext().getBinding(propertyBame);
        JComponent[] components = pixelBinding.getComponents();
        for (JComponent component : components) {
            component.setEnabled(enabled);
        }
    }
    
    private static ValueDescriptor[] createVDs() {
        ValueDescriptor[] vds = new ValueDescriptor[11];
        
        vds[0] = new ValueDescriptor(GraticuleLayer.PROPERTY_NAME_RES_AUTO, Boolean.class);
        vds[0].setDefaultValue(GraticuleLayer.DEFAULT_RES_AUTO);
        vds[0].setDisplayName("Compute latitude and longitude steps");
        vds[0].setDefaultConverter();
        
        vds[1] = new ValueDescriptor(GraticuleLayer.PROPERTY_NAME_RES_PIXELS, Integer.class);
        vds[1].setDefaultValue(GraticuleLayer.DEFAULT_RES_PIXELS);
        vds[1].setValueRange(new ValueRange(16, 512));
        vds[1].setDisplayName("Average grid size in pixels");
        vds[1].setDefaultConverter();
        
        vds[2] = new ValueDescriptor(GraticuleLayer.PROPERTY_NAME_RES_LAT, Double.class);
        vds[2].setDefaultValue(GraticuleLayer.DEFAULT_RES_LAT);
        vds[2].setValueRange(new ValueRange(0.01, 90.00));
        vds[2].setDisplayName("Latitude step (dec. degree)");
        vds[2].setDefaultConverter();
        
        vds[3] = new ValueDescriptor(GraticuleLayer.PROPERTY_NAME_RES_LON, Double.class);
        vds[3].setDefaultValue(GraticuleLayer.DEFAULT_RES_LON);
        vds[3].setValueRange(new ValueRange(0.01, 180.00));
        vds[3].setDisplayName("Longitude step (dec. degree)");
        vds[3].setDefaultConverter();
        
        vds[4] = new ValueDescriptor(GraticuleLayer.PROPERTY_NAME_LINE_COLOR, Color.class);
        vds[4].setDefaultValue(GraticuleLayer.DEFAULT_LINE_COLOR);
        vds[4].setDisplayName("Line colour");
        vds[4].setDefaultConverter();
        
        vds[5] = new ValueDescriptor(GraticuleLayer.PROPERTY_NAME_LINE_WIDTH, Double.class);
        vds[5].setDefaultValue(GraticuleLayer.DEFAULT_LINE_WIDTH);
        vds[5].setDisplayName("Line width");
        vds[5].setDefaultConverter();
        
        vds[6] = new ValueDescriptor(GraticuleLayer.PROPERTY_NAME_LINE_TRANSPARENCY, Double.class);
        vds[6].setDefaultValue(GraticuleLayer.DEFAULT_LINE_TRANSPARENCY);
        vds[6].setValueRange(new ValueRange(0, 1));
        vds[6].setDisplayName("Line transparency");
        vds[6].setDefaultConverter();
        
        vds[7] = new ValueDescriptor(GraticuleLayer.PROPERTY_NAME_TEXT_ENABLED, Boolean.class);
        vds[7].setDefaultValue(GraticuleLayer.DEFAULT_TEXT_ENABLED);
        vds[7].setDisplayName("Show text labels");
        vds[7].setDefaultConverter();
        
        vds[8] = new ValueDescriptor(GraticuleLayer.PROPERTY_NAME_TEXT_FG_COLOR, Color.class);
        vds[8].setDefaultValue(GraticuleLayer.DEFAULT_TEXT_FG_COLOR);
        vds[8].setDisplayName("Text foreground colour");
        vds[8].setDefaultConverter();
        
        vds[9] = new ValueDescriptor(GraticuleLayer.PROPERTY_NAME_TEXT_BG_COLOR, Color.class);
        vds[9].setDefaultValue(GraticuleLayer.DEFAULT_TEXT_BG_COLOR);
        vds[9].setDisplayName("Text background colour");
        vds[9].setDefaultConverter();
        
        vds[10] = new ValueDescriptor(GraticuleLayer.PROPERTY_NAME_TEXT_BG_TRANSPARENCY, Double.class);
        vds[10].setDefaultValue(GraticuleLayer.DEFAULT_TEXT_BG_TRANSPARENCY);
        vds[10].setValueRange(new ValueRange(0, 1));
        vds[10].setDisplayName("Text background transparency");
        vds[10].setDefaultConverter();
        return vds;
    }
}
