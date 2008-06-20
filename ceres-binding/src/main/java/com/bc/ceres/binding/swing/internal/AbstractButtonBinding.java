package com.bc.ceres.binding.swing.internal;

import com.bc.ceres.binding.swing.Binding;
import com.bc.ceres.binding.swing.BindingContext;

import javax.swing.AbstractButton;
import javax.swing.JComponent;

/**
 * An abstract binding for a {@link javax.swing.AbstractButton} component.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public abstract class AbstractButtonBinding extends Binding {

    private final AbstractButton button;

    public AbstractButtonBinding(BindingContext context, String propertyName, AbstractButton button) {
        super(context, propertyName);
        this.button = button;
    }

    public AbstractButton getButton() {
        return button;
    }

    @Override
    protected void adjustComponentsImpl() {
        boolean selected = (Boolean) getValue();
        button.setSelected(selected);
    }

    @Override
    public JComponent getPrimaryComponent() {
        return button;
    }

    protected void adjustProperty() {
        setValue(getButton().isSelected());
    }
}
