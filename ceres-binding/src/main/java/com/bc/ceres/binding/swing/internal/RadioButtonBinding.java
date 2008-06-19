package com.bc.ceres.binding.swing.internal;

import com.bc.ceres.binding.swing.internal.AbstractButtonBinding;
import com.bc.ceres.binding.swing.BindingContext;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.JRadioButton;

/**
 * A binding for a {@link javax.swing.JRadioButton} component.
*
* @author Norman Fomferra
* @version $Revision$ $Date$
* @since BEAM 4.2
*/
public class RadioButtonBinding extends AbstractButtonBinding implements ChangeListener {

    public RadioButtonBinding(BindingContext context, String propertyName, JRadioButton radioButton) {
        super(context, propertyName, radioButton);
        radioButton.addChangeListener(this);
    }

    public void stateChanged(ChangeEvent e) {
        adjustProperty();
    }
}
