package com.bc.ceres.binding.swing.internal;

import com.bc.ceres.binding.swing.Binding;
import com.bc.ceres.binding.swing.ComponentAdapter;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * An abstract binding for a {@link javax.swing.AbstractButton} component.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class AbstractButtonAdapter extends ComponentAdapter implements ActionListener {

    private final AbstractButton button;

    public AbstractButtonAdapter(AbstractButton button) {
        super();
        this.button = button;
    }

    public AbstractButton getButton() {
        return button;
    }

    @Override
    public JComponent getPrimaryComponent() {
        return getButton();
    }

    @Override
    public void bindComponents() {
        button.addActionListener(this);
    }

    @Override
    public void unbindComponents() {
        button.removeActionListener(this);
    }

    @Override
    public void adjustComponents() {
        boolean selected = (Boolean) getBinding().getValue();
        button.setSelected(selected);
    }

    public void actionPerformed(ActionEvent e) {
        getBinding().setValue(getButton().isSelected());
    }
}
