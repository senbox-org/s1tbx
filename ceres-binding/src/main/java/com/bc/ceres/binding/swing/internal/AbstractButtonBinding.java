package com.bc.ceres.binding.swing.internal;

import com.bc.ceres.binding.swing.Binding;
import com.bc.ceres.binding.swing.BindingContext;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * An abstract binding for a {@link javax.swing.AbstractButton} component.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class AbstractButtonBinding extends Binding implements ActionListener {

    private final AbstractButton button;

    public AbstractButtonBinding(BindingContext context, String propertyName, AbstractButton button) {
        super(context, propertyName);
        this.button = button;
        this.button.addActionListener(this);
    }

    public AbstractButton getButton() {
        return button;
    }

    @Override
    protected void doAdjustComponents() {
        boolean selected = (Boolean) getValue();
        button.setSelected(selected);
    }

    @Override
    public JComponent getPrimaryComponent() {
        return getButton();
    }

    public void actionPerformed(ActionEvent e) {
        setValue(getButton().isSelected());
    }
}
