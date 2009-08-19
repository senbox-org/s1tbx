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
package com.bc.ceres.binding.swing.internal;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.swing.ComponentAdapter;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

/**
 * A binding for a {@link JTextComponent} component.
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class TextComponentAdapter extends ComponentAdapter implements DocumentListener {

    private final JTextComponent textComponent;
    private boolean isAdjustingValue; 

    public TextComponentAdapter(JTextComponent textComponent) {
        super();
        this.textComponent = textComponent;
    }

    @Override
    public JComponent[] getComponents() {
        return new JComponent[]{textComponent};
    }

    @Override
    public void bindComponents() {
        textComponent.getDocument().addDocumentListener(this);
        textComponent.setInputVerifier(createInputVerifier());
    }

    @Override
    public void unbindComponents() {
        textComponent.getDocument().removeDocumentListener(this);
//        textArea.setInputVerifier(null);
    }

    @Override
    public void adjustComponents() {
        final ValueContainer valueContainer = getBinding().getContext().getValueContainer();
        final ValueModel model = valueContainer.getModel(getBinding().getPropertyName());
        if (!isAdjustingValue) {
            if (model != null) {
                textComponent.setText(model.getValueAsText());
            } else {
                textComponent.setText("");
            }
        }
    }

    boolean adjustValue() {
        try {
            if (!isAdjustingValue) {
                final ValueContainer valueContainer = getBinding().getContext().getValueContainer();
                final ValueModel model = valueContainer.getModel(getBinding().getPropertyName());
                isAdjustingValue = true;
                model.setValueFromText(textComponent.getText());
                isAdjustingValue = false;
            }
            return true;
        } catch (ValidationException e) {
            handleError(e);
            return false;
        } catch (ConversionException e) {
            handleError(e);
            return false;
        }
    }

    public InputVerifier createInputVerifier() {
        return new TextComponentVerifier(this);
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        adjustValue();
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        adjustValue();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        adjustValue();
    }
}
