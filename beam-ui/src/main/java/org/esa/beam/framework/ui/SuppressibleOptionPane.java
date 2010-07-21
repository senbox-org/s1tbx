/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.beam.framework.ui;

import java.awt.Component;
import java.util.Enumeration;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

import org.esa.beam.util.Guardian;
import org.esa.beam.util.PropertyMap;

public class SuppressibleOptionPane extends JComponent {

    public final static String KEY_PREFIX_DONT_SHOW = ".dontShow";
    public final static String KEY_PREFIX_ENABLED = ".enabled";
    private static final String DONT_SHOW_MESSAGE_TEXT = "Do not show this message anymore."; /*I18N*/
    private static final String DONT_SHOW_QUESTION_TEXT = "Do not show this question anymore."; /*I18N*/

    private final PropertyMap _propertyMap;
    private JCheckBox _checkBox;

    public SuppressibleOptionPane(PropertyMap propertyMap) {
        Guardian.assertNotNull("propertyMap", propertyMap);
        _propertyMap = propertyMap;
    }

    public int showConfirmDialog(final String propertyName, Component parentComponent, Object message) {
        checkValidPropertyName(propertyName);
        if (!isDontShowPropertySet(propertyName)) {
            message = createSuppressibleMessage(message, true);
            final int answer = JOptionPane.showConfirmDialog(parentComponent, message);
            setConfirmResponseProperty(propertyName, answer);
            return answer;
        }
        return JOptionPane.YES_OPTION;
    }

    public int showConfirmDialog(String propertyName, Component parentComponent, Object message,
                                 String title, int optionType) {
        checkValidPropertyName(propertyName);
        if (!isDontShowPropertySet(propertyName)) {
            message = createSuppressibleMessage(message, true);
            final int answer = JOptionPane.showConfirmDialog(parentComponent, message, title, optionType);
            setConfirmResponseProperty(propertyName, answer);
            return answer;
        }
        return JOptionPane.YES_OPTION;
    }

    public int showConfirmDialog(String propertyName, Component parentComponent, Object message,
                                 String title, int optionType, int messageType) {
        checkValidPropertyName(propertyName);
        if (!isDontShowPropertySet(propertyName)) {
            message = createSuppressibleMessage(message, true);
            final int answer = JOptionPane.showConfirmDialog(parentComponent, message, title, optionType, messageType);
            setConfirmResponseProperty(propertyName, answer);
            return answer;
        }
        return JOptionPane.YES_OPTION;
    }

    public int showConfirmDialog(String propertyName, Component parentComponent, Object message,
                                 String title, int optionType, int messageType,
                                 Icon icon) {
        checkValidPropertyName(propertyName);
        if (!isDontShowPropertySet(propertyName)) {
            message = createSuppressibleMessage(message, true);
            final int answer = JOptionPane.showConfirmDialog(parentComponent, message, title, optionType, messageType,
                                                             icon);
            setConfirmResponseProperty(propertyName, answer);
            return answer;
        }
        return JOptionPane.YES_OPTION;
    }


    public int showInternalConfirmDialog(String propertyName, Component parentComponent, Object message) {
        checkValidPropertyName(propertyName);
        if (!isDontShowPropertySet(propertyName)) {
            message = createSuppressibleMessage(message, true);
            final int answer = JOptionPane.showInternalConfirmDialog(parentComponent, message);
            setConfirmResponseProperty(propertyName, answer);
            return answer;
        }
        return JOptionPane.YES_OPTION;
    }

    public int showInternalConfirmDialog(String propertyName, Component parentComponent, Object message,
                                         String title, int optionType) {
        checkValidPropertyName(propertyName);
        if (!isDontShowPropertySet(propertyName)) {
            message = createSuppressibleMessage(message, true);
            final int answer = JOptionPane.showInternalConfirmDialog(parentComponent, message, title, optionType);
            setConfirmResponseProperty(propertyName, answer);
            return answer;
        }
        return JOptionPane.YES_OPTION;
    }

    public int showInternalConfirmDialog(String propertyName, Component parentComponent, Object message,
                                         String title, int optionType,
                                         int messageType) {
        checkValidPropertyName(propertyName);
        if (!isDontShowPropertySet(propertyName)) {
            message = createSuppressibleMessage(message, true);
            final int answer = JOptionPane.showInternalConfirmDialog(parentComponent, message, title, optionType,
                                                                     messageType);
            setConfirmResponseProperty(propertyName, answer);
            return answer;
        }
        return JOptionPane.YES_OPTION;
    }

    public int showInternalConfirmDialog(String propertyName, Component parentComponent, Object message,
                                         String title, int optionType,
                                         int messageType, Icon icon) {
        checkValidPropertyName(propertyName);
        if (!isDontShowPropertySet(propertyName)) {
            message = createSuppressibleMessage(message, true);
            final int answer = JOptionPane.showInternalConfirmDialog(parentComponent, message, title, optionType, messageType,
                                                                     icon);
            setConfirmResponseProperty(propertyName, answer);
            return answer;
        }
        return JOptionPane.YES_OPTION;
    }

    public void showMessageDialog(String propertyName, Component parentComponent, Object message) {
        checkValidPropertyName(propertyName);
        if (!isDontShowPropertySet(propertyName)) {
            message = createSuppressibleMessage(message, false);
            JOptionPane.showMessageDialog(parentComponent, message);
            setDontShowProperty(propertyName);
        }
    }

    public void showMessageDialog(String propertyName, Component parentComponent, Object message,
                                  String title, int messageType) {
        checkValidPropertyName(propertyName);
        if (!isDontShowPropertySet(propertyName)) {
            message = createSuppressibleMessage(message, false);
            JOptionPane.showMessageDialog(parentComponent, message, title, messageType);
            setDontShowProperty(propertyName);
        }
    }

    public void showMessageDialog(String propertyName, Component parentComponent, Object message,
                                  String title, int messageType, Icon icon) {
        checkValidPropertyName(propertyName);
        if (!isDontShowPropertySet(propertyName)) {
            message = createSuppressibleMessage(message, false);
            JOptionPane.showMessageDialog(parentComponent, message, title, messageType, icon);
            setDontShowProperty(propertyName);
        }
    }

    public void showInternalMessageDialog(String propertyName, Component parentComponent, Object message) {
        checkValidPropertyName(propertyName);
        if (!isDontShowPropertySet(propertyName)) {
            message = createSuppressibleMessage(message, false);
            JOptionPane.showInternalMessageDialog(parentComponent, message);
            setDontShowProperty(propertyName);
        }
    }

    public void showInternalMessageDialog(String propertyName, Component parentComponent, Object message,
                                          String title, int messageType) {
        checkValidPropertyName(propertyName);
        if (!isDontShowPropertySet(propertyName)) {
            message = createSuppressibleMessage(message, false);
            JOptionPane.showInternalMessageDialog(parentComponent, message, title, messageType);
            setDontShowProperty(propertyName);
        }
    }

    public void showInternalMessageDialog(String propertyName, Component parentComponent, Object message,
                                          String title, int messageType,
                                          Icon icon) {
        checkValidPropertyName(propertyName);
        if (!isDontShowPropertySet(propertyName)) {
            message = createSuppressibleMessage(message, false);
            JOptionPane.showInternalMessageDialog(parentComponent, message, title, messageType, icon);
            setDontShowProperty(propertyName);
        }
    }

    public int showInternalOptionDialog(String propertyName, Component parentComponent, Object message,
                                        String title, int optionType,
                                        int messageType, Icon icon,
                                        Object[] options, Object initialValue) {
        checkValidPropertyName(propertyName);
        if (!isDontShowPropertySet(propertyName)) {
            message = createSuppressibleMessage(message, false);
            final int answer = JOptionPane.showInternalOptionDialog(parentComponent, message, title, optionType,
                                                                    messageType, icon, options, initialValue);
            setDontShowProperty(propertyName);
            return answer;
        }
        return JOptionPane.YES_OPTION;
    }


    /**
     * Delegates to <code>JOptionPane</code> if the dialog for given property key should displayed or returns
     * <code>JOptionPane.OK_OPTION</code>
     *
     * @param propertyName    the property key
     * @param parentComponent for <code>JOptionPane</code> delegation
     * @param message         for <code>JOptionPane</code> delegation
     * @param title           for <code>JOptionPane</code> delegation
     * @param optionType      for <code>JOptionPane</code> delegation
     * @param messageType     for <code>JOptionPane</code> delegation
     * @param icon            for <code>JOptionPane</code> delegation
     * @param options         for <code>JOptionPane</code> delegation
     * @param initialValue    for <code>JOptionPane</code> delegation
     *
     * @return the <code>JOptionPane</code> result or <code>JOptionPane.OK_OPTION</code> if the dialog should not be
     *         displayed.
     *
     * @see JOptionPane#showOptionDialog(Component, Object, String, int, int, Icon, Object[], Object)
     * @see JOptionPane#OK_OPTION
     */
    public int showOptionDialog(final String propertyName, Component parentComponent, Object message,
                                String title, int optionType, int messageType,
                                Icon icon, Object[] options, Object initialValue) {
        checkValidPropertyName(propertyName);
        if (isDontShowPropertySet(propertyName)) {
            message = createSuppressibleMessage(message, false);
            final int answer = JOptionPane.showOptionDialog(parentComponent, message, title,
                                                            optionType, messageType, icon,
                                                            options, initialValue);
            setDontShowProperty(propertyName);
            return answer;
        }
        return JOptionPane.YES_OPTION;
    }

    public boolean areDialogsSuppressed() {
        Enumeration propertyKeys = _propertyMap.getPropertyKeys();
        while (propertyKeys.hasMoreElements()) {
            String key = (String) propertyKeys.nextElement();
            if (isDontShowPropertyName(key) && _propertyMap.getPropertyBool(key)) {
                return true;
            }
        }
        return false;
    }

    public void unSuppressDialogs() {
        Enumeration propertyKeys = _propertyMap.getPropertyKeys();
        while (propertyKeys.hasMoreElements()) {
            String key = (String) propertyKeys.nextElement();
            if (isDontShowPropertyName(key)) {
                _propertyMap.setPropertyBool(key, false);
            }
        }
    }

    private Object createSuppressibleMessage(Object message, boolean question) {
        if (message == null) {
            return message;
        }

        if (_checkBox == null) {
            _checkBox = new JCheckBox();            
        }

        _checkBox.setText(question ? DONT_SHOW_QUESTION_TEXT : DONT_SHOW_MESSAGE_TEXT);
        _checkBox.setSelected(false);

        Object[] newMessage;
        if (message instanceof Object[]) {
            final int length = ((Object[])message).length;
            newMessage = new Object[length + 2];
            System.arraycopy(message, 0, newMessage, 0, length);
        } else if (message instanceof Component || message instanceof Icon) {
            newMessage = new Object[3];
            newMessage[0] = message;
        } else {
            newMessage = new Object[3];
            newMessage[0] = message.toString();
        }
        newMessage[newMessage.length - 2] = " ";
        newMessage[newMessage.length - 1] = _checkBox;

        return newMessage;
    }

    private static void checkValidPropertyName(final String propertyName) {
        Guardian.assertNotNullOrEmpty("propertyName", propertyName);
    }

    private void setConfirmResponseProperty(String propertyName, final int answer) {
        setDontShowProperty(propertyName);
        if (answer == JOptionPane.YES_OPTION) {
            _propertyMap.setPropertyBool(propertyName, true);
        } else if (answer == JOptionPane.NO_OPTION) {
            _propertyMap.setPropertyBool(propertyName, false);
        }
    }

    private boolean isDontShowPropertySet(String propertyName) {
        return _propertyMap.getPropertyBool(createDontShowPropertyName(propertyName), false);
    }

    private void setDontShowProperty(String propertyName) {
        _propertyMap.setPropertyBool(createDontShowPropertyName(propertyName), _checkBox.isSelected());
    }

    private static String createDontShowPropertyName(String propertyName) {
        if (propertyName.endsWith(KEY_PREFIX_ENABLED)) {
             return propertyName.substring(0, propertyName.lastIndexOf('.')) + KEY_PREFIX_DONT_SHOW;
        }
        return propertyName + KEY_PREFIX_DONT_SHOW;
    }

    private static boolean isDontShowPropertyName(String propertyName) {
        return propertyName.endsWith(KEY_PREFIX_DONT_SHOW);
    }

}
