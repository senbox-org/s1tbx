package com.bc.ceres.binding.swing.internal;

import com.bc.ceres.binding.swing.internal.AbstractButtonBinding;
import com.bc.ceres.binding.swing.BindingContext;

import javax.swing.JCheckBox;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * A binding for a {@link javax.swing.JCheckBox} component.
*
* @author Norman Fomferra
* @version $Revision$ $Date$
* @since BEAM 4.2
*/
public class CheckBoxBinding extends AbstractButtonBinding implements ActionListener {

    public CheckBoxBinding(BindingContext context, String propertyName, JCheckBox checkBox) {
        super(context, propertyName, checkBox);
        checkBox.addActionListener(this);
    }

    public void actionPerformed(ActionEvent event) {
        adjustProperty();
    }

}
