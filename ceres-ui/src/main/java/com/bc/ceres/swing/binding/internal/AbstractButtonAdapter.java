package com.bc.ceres.swing.binding.internal;

import com.bc.ceres.swing.binding.ComponentAdapter;

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
    public JComponent[] getComponents() {
        return new JComponent[]{button};
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
        boolean selected = (Boolean) getBinding().getPropertyValue();
        button.setSelected(selected);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        getBinding().setPropertyValue(getButton().isSelected());
    }
}
