package com.bc.ceres.binding.swing.internal;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.binding.swing.Binding;
import com.bc.ceres.binding.swing.BindingContext;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * A binding for a set of {@link javax.swing.AbstractButton} components sharing a multiple-exclusion scope.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class ButtonGroupBinding extends Binding implements ChangeListener {
    private final ButtonGroup buttonGroup;
    private final Map<AbstractButton, Object> buttonToValueMap;
    private final Map<Object, AbstractButton> valueToButtonMap;

    public ButtonGroupBinding(BindingContext context, ButtonGroup buttonGroup, String propertyName, Map<AbstractButton, Object> buttonToValueMap) {
        super(context, propertyName);
        this.buttonGroup = buttonGroup;
        this.buttonToValueMap = buttonToValueMap;
        this.valueToButtonMap = new HashMap<Object, AbstractButton>(buttonToValueMap.size());

        Enumeration<AbstractButton> buttonEnum = buttonGroup.getElements();
        while (buttonEnum.hasMoreElements()) {
            AbstractButton button = buttonEnum.nextElement();
            button.addChangeListener(this);
            valueToButtonMap.put(buttonToValueMap.get(button), button);
        }
    }

    @Override
    protected void adjustComponentImpl() {
        Object value = getValueContainer().getValue(getPropertyName());
        AbstractButton button = valueToButtonMap.get(value);
        if (button != null) {
            button.setSelected(true);
        }
    }

    @Override
    protected JComponent getPrimaryComponent() {
        return buttonGroup.getElements().nextElement();
    }

    public void stateChanged(ChangeEvent event) {
        AbstractButton button = (AbstractButton) event.getSource();
        setPropertyValue(buttonToValueMap.get(button));
    }

    public static Map<AbstractButton, Object> createButtonToValueMap(ButtonGroup buttonGroup, ValueContainer valueContainer, String propertyName) {
        ValueSet valueSet = valueContainer.getValueDescriptor(propertyName).getValueSet();
        if (valueSet == null) {
            throw new IllegalStateException("valueSet == null");
        }
        Object[] items = valueSet.getItems();
        if (buttonGroup.getButtonCount() != items.length) {
            throw new IllegalStateException("buttonGroup.getButtonCount() != items.length");
        }
        Enumeration<AbstractButton> buttonEnum = buttonGroup.getElements();
        HashMap<AbstractButton, Object> buttonToValueMap = new HashMap<AbstractButton, Object>(items.length);
        for (Object item : items) {
            buttonToValueMap.put(buttonEnum.nextElement(), item);
        }
        return buttonToValueMap;
    }

}
