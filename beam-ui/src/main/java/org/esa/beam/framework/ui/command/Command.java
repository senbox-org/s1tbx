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
package org.esa.beam.framework.ui.command;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.ConfigurableExtension;
import com.bc.ceres.core.runtime.ConfigurationElement;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.StringUtils;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.EventListenerList;
import java.util.EventListener;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

// @todo 1 nf/nf - place class API docu here

/**
 * The <code>Command</code> is a ...
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @version $Revision$  $Date$
 */
public abstract class Command implements ConfigurableExtension {

    private String commandID;
    private final Action action;
    private EventListenerList eventListenerList;
    private static CommandUIFactory commandUIFactory = new DefaultCommandUIFactory();

    /**
     * The key used for storing a large icon for the action, used for toolbar buttons.
     * <p/>
     * <p>Note: Actually this key belongs in the javax.swing.Action interface, but Sun hasn't done this so far (why?).
     */
    public static final String ACTION_KEY_LARGE_ICON = "_largeIcon";
    public static final String ACTION_KEY_SEPARATOR_BEFORE = "_separatorBefore";
    public static final String ACTION_KEY_SEPARATOR_AFTER = "_separatorAfter";
    public static final String ACTION_KEY_PLACE_BEFORE = "_placeBefore";
    public static final String ACTION_KEY_PLACE_AFTER = "_placeAfter";
    public static final String ACTION_KEY_PLACE_CONTEXT_TOP = "_placeAtContextTop";
    public static final String ACTION_KEY_PARENT = "_parent";
    public static final String ACTION_KEY_LOCATION = "_location";
    public static final String ACTION_KEY_CONTEXT = "_context";
    public static final String ACTION_KEY_POPUP_TEXT = "popupText";
    public static final String ACTION_KEY_SORT_CHILDREN = "_sortChildren";
    public static final String HELP_ID_KEY = "_helpId";

    protected Command() {
        action = createAction();
        setCommandID(getClass().getName());
    }

    protected Command(String commandID) {
        this();
        Guardian.assertNotNull(commandID, "commandID");
        setCommandID(commandID);
    }

    public String getCommandID() {
        return commandID;
    }

    public void setCommandID(String commandId) {
        commandID = commandId;
        action.putValue(Action.ACTION_COMMAND_KEY, commandId);
    }

    public Action getAction() {
        return action;
    }

    public boolean isEnabled() {
        return getAction().isEnabled();
    }

    public void setEnabled(boolean enabled) {
        getAction().setEnabled(enabled);
    }

    public String getParent() {
        return (String) getProperty(ACTION_KEY_PARENT);
    }

    public void setParent(String value) {
        setProperty(ACTION_KEY_PARENT, value);
    }

    public String[] getLocations() {
        return (String[]) getProperty(ACTION_KEY_LOCATION);
    }

    public void setLocations(String[] locations) {
        setProperty(ACTION_KEY_LOCATION, locations);
    }

    public boolean containsLocation(String location) {
        return containsProperty(ACTION_KEY_LOCATION, location);
    }

    public String[] getContexts() {
        return (String[]) getProperty(ACTION_KEY_CONTEXT);
    }

    public void setContexts(String[] contexts) {
        setProperty(ACTION_KEY_CONTEXT, contexts);
    }

    public boolean containsContext(String context) {
        return containsProperty(ACTION_KEY_CONTEXT, context);
    }

    public String getText() {
        return (String) getProperty(Action.NAME);
    }

    public void setText(String value) {
        setProperty(Action.NAME, value);
    }

    public String getPopupText() {
        return (String) getProperty(ACTION_KEY_POPUP_TEXT);
    }

    public void setPopupText(String value) {
        setProperty(ACTION_KEY_POPUP_TEXT, value);
    }

    public Boolean getSortChildren() {
        if(getProperty(ACTION_KEY_SORT_CHILDREN) == null) {
            return false;
        }
        return (Boolean) getProperty(ACTION_KEY_SORT_CHILDREN);
    }

    /**
     * Returns the integer value of command's mnemonic character.
     *
     * @return the integer value of command's mnemonic character or <code>-1</code> if mnemonic property is
     *         <code>null</code>
     */
    public int getMnemonic() {
        final Object property = getProperty(Action.MNEMONIC_KEY);
        if (property != null) {
            return ((Number) property).intValue();
        }
        return -1;
    }

    public void setMnemonic(int mnemonic) {
        setProperty(Action.MNEMONIC_KEY, mnemonic);
    }

    public KeyStroke getAccelerator() {
        return (KeyStroke) getProperty(Action.ACCELERATOR_KEY);
    }

    public void setAccelerator(KeyStroke accelerator) {
        setProperty(Action.ACCELERATOR_KEY, accelerator);
    }

    public String getShortDescription() {
        return (String) getProperty(Action.SHORT_DESCRIPTION);
    }

    public void setShortDescription(String text) {
        setProperty(Action.SHORT_DESCRIPTION, text);
    }

    public String getLongDescription() {
        return (String) getProperty(Action.LONG_DESCRIPTION);
    }

    public void setLongDescription(String text) {
        setProperty(Action.LONG_DESCRIPTION, text);
    }

    public String getHelpId() {
        return getProperty(HELP_ID_KEY, "");
    }

    public void setHelpId(String id) {
        setProperty(HELP_ID_KEY, id);
    }

    public Icon getSmallIcon() {
        return (Icon) getProperty(Action.SMALL_ICON);
    }

    public void setSmallIcon(Icon icon) {
        setProperty(Action.SMALL_ICON, icon);
    }

    public Icon getLargeIcon() {
        return (Icon) getProperty(ACTION_KEY_LARGE_ICON);
    }

    public void setLargeIcon(Icon icon) {
        setProperty(ACTION_KEY_LARGE_ICON, icon);
    }

    public boolean isSeparatorBefore() {
        return getProperty(ACTION_KEY_SEPARATOR_BEFORE, false);
    }

    public void setSeparatorBefore(boolean separatorBefore) {
        setProperty(ACTION_KEY_SEPARATOR_BEFORE, separatorBefore);
    }

    public boolean isSeparatorAfter() {
        return getProperty(ACTION_KEY_SEPARATOR_AFTER, false);
    }

    public void setSeparatorAfter(boolean separatorAfter) {
        setProperty(ACTION_KEY_SEPARATOR_AFTER, separatorAfter);
    }

    public String getPlaceAfter() {
        return (String) getProperty(ACTION_KEY_PLACE_AFTER);
    }

    public void setPlaceAfter(String placeAfter) {
        setProperty(ACTION_KEY_PLACE_AFTER, placeAfter);
    }

    public String getPlaceBefore() {
        return (String) getProperty(ACTION_KEY_PLACE_BEFORE);
    }

    public void setPlaceBefore(String placeBefore) {
        setProperty(ACTION_KEY_PLACE_BEFORE, placeBefore);
    }

    public boolean isPlaceAtContextTop() {
        return getProperty(ACTION_KEY_PLACE_CONTEXT_TOP, false);
    }

    public void setPlaceAtContextTop(boolean placeAtContextTop) {
        setProperty(ACTION_KEY_PLACE_CONTEXT_TOP, placeAtContextTop);
    }

    /**
     * Configures this command with the properties (if any) found in the given recource bundle. The resource keys for
     * the corresponding properties are: <p> <ld> <li><code>command.</code><i>command-ID</i><code>.text = <i>display
     * text</i></code></li> <li><code>command.</code><i>command-ID</i><code>.popuptext = <i>display text for popup
     * menu</i></code></li> <li><code>command.</code><i>command-ID</i><code>.mnemonic = <i>mnemonic
     * character</i></code></li> <li><code>command.</code><i>command-ID</i><code>.accelerator = <i>accelerator
     * key</i></code></li> <li><code>command.</code><i>command-ID</i><code>.shortdescr = <i>text</i></code></li>
     * <li><code>command.</code><i>command-ID</i><code>.longdescr = <i>text</i></code></li>
     * <li><code>command.</code><i>command-ID</i><code>.smallicon = <i>image-path</i></code></li>
     * <li><code>command.</code><i>command-ID</i><code>.largeicon = <i>image-path</i></code></li>
     * <li><code>command.</code><i>command-ID</i><code>.parent = <i>command-COMMAND_ID or main-menu-name</i></code></li>
     * <li><code>command.</code><i>command-ID</i><code>.location = <i>location-1</i>, <i>location-2</i>, ...</code></li>
     * <li><code>command.</code><i>command-ID</i><code>.context = <i>context-1</i>, <i>context-2</i>, ...</code></li>
     * <li><code>command.</code><i>command-COMMAND_ID</i><code>.placeBefore = <i>command-ID</i></code></li>
     * <li><code>command.</code><i>command-ID</i><code>.placeAfter = <i>command-COMMAND_ID</i></code></li>
     * <li><code>command.</code><i>command-ID</i><code>.separatorBefore = true <i>or</i> false</code></li>
     * <li><code>command.</code><i>command-ID</i><code>.separatorAfter = true <i>or</i> false</code></li> </ld>
     *
     * @param resourceBundle the resource bundle from which the properties are received
     *
     * @throws IllegalArgumentException if the recource bundle is null
     */
    public void configure(ResourceBundle resourceBundle) {
        Guardian.assertNotNull("resourceBundle", resourceBundle);

        String resString;
        String[] resStrings;
        Icon resIcon;
        Boolean resBoolean;

        resString = getResourceString(resourceBundle, "text");
        if (resString != null && resString.length() > 0) {
            setText(resString);
        }

        resString = getResourceString(resourceBundle, "popuptext");
        if (resString != null && resString.length() > 0) {
            setPopupText(resString);
        }

        resString = getResourceString(resourceBundle, "mnemonic");
        if (resString != null && resString.length() > 0) {
            setMnemonic((int) resString.charAt(0));
        }

        resString = getResourceString(resourceBundle, "accelerator");
        if (resString != null && resString.length() > 0) {
            setAccelerator(KeyStroke.getKeyStroke(resString));
        }

        resString = getResourceString(resourceBundle, "shortDescr");
        if (resString != null) {
            setShortDescription(resString);
        }

        resString = getResourceString(resourceBundle, "longDescr");
        if (resString != null) {
            setLongDescription(resString);
        }

        resIcon = getResourceIcon(resourceBundle, "smallIcon");
        if (resIcon != null) {
            setSmallIcon(resIcon);
        }

        resIcon = getResourceIcon(resourceBundle, "largeIcon");
        if (resIcon != null) {
            setLargeIcon(resIcon);
        }

        resBoolean = getResourceBoolean(resourceBundle, "separatorBefore");
        if (resBoolean != null) {
            setSeparatorBefore(resBoolean);
        }

        resBoolean = getResourceBoolean(resourceBundle, "separatorAfter");
        if (resBoolean != null) {
            setSeparatorAfter(resBoolean);
        }

        resString = getResourceString(resourceBundle, "placeBefore");
        if (resString != null) {
            setProperty(ACTION_KEY_PLACE_BEFORE, resString);
        }

        resString = getResourceString(resourceBundle, "placeAfter");
        if (resString != null) {
            setProperty(ACTION_KEY_PLACE_AFTER, resString);
        }

        resBoolean = getResourceBoolean(resourceBundle, "placeAtContextTop");
        if (resBoolean != null) {
            setPlaceAtContextTop(resBoolean);
        }

        resString = getResourceString(resourceBundle, "parent");
        if (resString != null) {
            setProperty(ACTION_KEY_PARENT, resString);
        }

        resStrings = getResourceStrings(resourceBundle, "location");
        if (resStrings != null) {
            setProperty(ACTION_KEY_LOCATION, resStrings);
        }

        resStrings = getResourceStrings(resourceBundle, "context");
        if (resStrings != null) {
            setProperty(ACTION_KEY_CONTEXT, resStrings);
        }

        resBoolean = getResourceBoolean(resourceBundle, "sortChildren");
        if (resString != null) {
            setProperty(ACTION_KEY_SORT_CHILDREN, resBoolean);
        }

        resString = getResourceString(resourceBundle, "helpId");
        if (resString != null) {
            setProperty(HELP_ID_KEY, resString);
        }
    }

    /**
     * Causes this command to fire the 'check status' event to all of its listeners.
     */
    public void updateState() {
        updateState(new CommandEvent(this, null, null, null));
        fireUpdateState();
    }

    /**
     * Creates an appropriate menu item for this command.
     */
    public abstract JMenuItem createMenuItem();

    /**
     * Creates an appropriate tool bar button for this command.
     */
    public abstract AbstractButton createToolBarButton();

    /**
     * Creates an appropriate action instance for this command.
     */
    protected abstract Action createAction();

    protected Object getProperty(String key) {
        return getAction().getValue(key);
    }

    protected void setProperty(String key, Object value) {
        getAction().putValue(key, value);
    }

    protected boolean containsProperty(String key, Object testValue) {
        if (testValue == null) {
            return false;
        }
        Object value = getProperty(key);
        if (value == null) {
            return false;
        }
        if (value instanceof String && testValue instanceof String) {
            return ((String) value).equalsIgnoreCase((String) testValue);
        } else if (value instanceof String[] && testValue instanceof String) {
            return StringUtils.containsIgnoreCase((String[]) value, (String) testValue);
        } else {
            return value.equals(testValue);
        }
    }

    protected String getProperty(String key, String defaultValue) {
        Object value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return (String) value;
    }

    protected String[] getProperty(String key, String[] defaultValues) {
        Object value = getProperty(key);
        if (value == null || !(value instanceof String[])) {
            return defaultValues;
        }
        return (String[]) value;
    }

    protected boolean getProperty(String key, boolean defaultValue) {
        Object value = getProperty(key);
        if (value == null || !(value instanceof Boolean)) {
            return defaultValue;
        }
        return (Boolean) value;
    }

    protected void setProperty(String key, boolean value) {
        setProperty(key, value ? Boolean.TRUE : Boolean.FALSE);
    }


    protected EventListenerList getEventListenerList() {
        return eventListenerList;
    }

    /**
     * Adds a command event listener.
     *
     * @param t the type of the listener to be added
     * @param l the command listener
     */
    protected void addEventListener(Class t, EventListener l) {
        if (l != null) {
            if (eventListenerList == null) {
                eventListenerList = new EventListenerList();
            }
            eventListenerList.add(t, l);
        }
    }

    /**
     * Removes a command event listener.
     *
     * @param t the type of the listener to be removed
     * @param l the command listener
     */
    protected void removeEventListener(Class t, EventListener l) {
        if (l != null && eventListenerList != null) {
            eventListenerList.remove(t, l);
        }
    }


    /**
     * Notify all listeners that have registered interest for notification on the 'update status' event type.  The event
     * instance is lazily created using the parameters passed into the fire method.
     */
    protected void fireUpdateState() {
        if (getEventListenerList() == null) {
            return;
        }
        // Guaranteed to return a non-null array
        Object[] listeners = getEventListenerList().getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        CommandEvent commandEvent = null;
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == CommandListener.class
                || listeners[i] == CommandStateListener.class) {
                // Lazily create the event:
                if (commandEvent == null) {
                    commandEvent = new CommandEvent(this, null, null, null);
                }
                ((CommandStateListener) listeners[i + 1]).updateState(commandEvent);
            }
        }
    }

    /**
     * Gets the key for the given command command-ID and command property name name as used in the resource bundle for
     * this application.
     */
    protected String createResourceKey(String commandPropertyName) {
        StringBuffer sb = new StringBuffer();
        sb.append("command.");
        sb.append(getCommandID());
        sb.append(".");
        sb.append(commandPropertyName);
        return sb.toString();
    }

    /**
     * Gets the resource string for the given command command-ID and command property name.
     */
    protected String getResourceString(ResourceBundle resourceBundle, String commandPropertyName) {
        String key = createResourceKey(commandPropertyName);
        try {
            return resourceBundle.getString(key);
        } catch (MissingResourceException e) {
            //Debug.trace("missing value for recource key '" + key + "'");
            return null;
        }
    }

    /**
     * Gets the resource strings for the given command command-ID and command property name.
     */
    protected String[] getResourceStrings(ResourceBundle resourceBundle, String commandPropertyName) {
        String value = getResourceString(resourceBundle, commandPropertyName);
        if (value == null) {
            return null;
        }
        return StringUtils.split(value, new char[]{','}, true);
    }

    /**
     * Gets the resource strings for the given command command-ID and command property name.
     */
    protected String[] getResourceStringArray(ResourceBundle resourceBundle, String commandPropertyName) {
        String key = createResourceKey(commandPropertyName);
        try {
            return resourceBundle.getStringArray(key);
        } catch (MissingResourceException e) {
            //Debug.trace("missing value for recource key '" + key + "'");
            return null;
        }
    }

    /**
     * Gets the boolean resource value for the given command command-ID and command property name.
     */
    protected Boolean getResourceBoolean(ResourceBundle resourceBundle, String commandPropertyName) {
        String value = getResourceString(resourceBundle, commandPropertyName);
        if (value != null) {
            return Boolean.valueOf(value);
        }
        return null;
    }

    protected Icon getResourceIcon(ResourceBundle resourceBundle, String commandPropertyName) {
        String value = getResourceString(resourceBundle, commandPropertyName);
        if (value != null) {
            try {
                return UIUtils.loadImageIcon(value);
            } catch (RuntimeException e) {
                Debug.trace(
                        "failed to load icon for recource entry '" + createResourceKey(
                                commandPropertyName) + "=" + value + "'");
            }
        }
        return null;
    }

    protected boolean containsPropertyValue(String key, Object value) {
        if (value == null) {
            return false;
        }
        Object oldValue = getProperty(key);
        if (oldValue == null) {
            return false;
        } else if (oldValue instanceof List) {
            List valueList = (List) oldValue;
            return valueList.contains(value);
        } else {
            return oldValue.equals(value);
        }
    }

    @Override
    public String toString() {
        return getCommandID();
    }

    public static CommandUIFactory getCommandUIFactory() {
        return commandUIFactory;
    }

    public static void setCommandUIFactory(CommandUIFactory commandUIFactory) {
        Guardian.assertNotNull("commandUIFactory", commandUIFactory);
        Command.commandUIFactory = commandUIFactory;
    }

    /**
     * Lets an action update its component tree (if any) since the Java look-and-feel has changed.
     * <p/>
     * <p>If a plug-in uses top-level containers such as dialogs or frames, implementors of this method should invoke
     * <code>SwingUtilities.updateComponentTreeUI()</code> on such containers.
     * <p/>
     * <p>The default implementation does nothing.</p>
     */
    public void updateComponentTreeUI() {
    }

    /**
     * Called when a command should update its state.
     * <p> This method can contain some code which analyzes the underlying element and makes a decision whether
     * this item or group should be made visible/invisible or enabled/disabled etc.</p>
     *
     * @param event the command event
     */
    public void updateState(final CommandEvent event) {

    }

    public void configure(ConfigurationElement config) throws CoreException {

        String resString;
        String[] resStrings;
        Icon resIcon;
        Boolean resBoolean;

        resString = getValue(config, "id", null);
        if (resString != null) {
            setCommandID(resString);
        }

        resString = getConfigString(config, "text");
        if (resString != null && resString.length() > 0) {
            setText(resString);
        }

        resString = getConfigString(config, "popuptext");
        if (resString != null && resString.length() > 0) {
            setPopupText(resString);
        }

        resString = getConfigString(config, "mnemonic");
        if (resString != null && resString.length() > 0) {
            setMnemonic((int) resString.charAt(0));
        }

        resString = getConfigString(config, "accelerator");
        if (resString != null && resString.length() > 0) {
            setAccelerator(KeyStroke.getKeyStroke(resString));
        }

        resString = getConfigString(config, "shortDescr");
        if (resString != null) {
            setShortDescription(resString);
        }

        resString = getConfigString(config, "longDescr");
        if (resString != null) {
            setLongDescription(resString);
        }

        resIcon = getConfigIcon(config, "smallIcon");
        if (resIcon != null) {
            setSmallIcon(resIcon);
        }

        resIcon = getConfigIcon(config, "largeIcon");
        if (resIcon != null) {
            setLargeIcon(resIcon);
        }

        resBoolean = getConfigBoolean(config, "separatorBefore");
        if (resBoolean != null) {
            setSeparatorBefore(resBoolean);
        }

        resBoolean = getConfigBoolean(config, "separatorAfter");
        if (resBoolean != null) {
            setSeparatorAfter(resBoolean);
        }

        resString = getConfigString(config, "placeBefore");
        if (resString != null) {
            setProperty(ACTION_KEY_PLACE_BEFORE, resString);
        }

        resString = getConfigString(config, "placeAfter");
        if (resString != null) {
            setProperty(ACTION_KEY_PLACE_AFTER, resString);
        }

        resBoolean = getConfigBoolean(config, "placeAtContextTop");
        if (resBoolean != null) {
            setPlaceAtContextTop(resBoolean);
        }

        resString = getConfigString(config, "parent");
        if (resString != null) {
            setProperty(ACTION_KEY_PARENT, resString);
        }

        resStrings = getConfigStrings(config, "location");
        if (resStrings != null) {
            setProperty(ACTION_KEY_LOCATION, resStrings);
        }

        resStrings = getConfigStrings(config, "context");
        if (resStrings != null) {
            setProperty(ACTION_KEY_CONTEXT, resStrings);
        }

        resBoolean = getConfigBoolean(config, "sortChildren");
        if (resBoolean != null) {
            setProperty(ACTION_KEY_SORT_CHILDREN, resBoolean);
        }

        resString = getConfigString(config, "helpId");
        if (resString != null) {
            setProperty(HELP_ID_KEY, resString);
        }
    }

    protected String getValue(ConfigurationElement config, String elementName, String defaultValue) {
        String value = null;
        ConfigurationElement child = config.getChild(elementName);
        if (child != null) {
            value = child.getValue();
        }
        return value != null ? value : defaultValue;
    }


    /**
     * Gets the resource string for the given command command-ID and command property name.
     */
    protected String getConfigString(ConfigurationElement config, String elementName) {
        String value = null;
        ConfigurationElement child = config.getChild(elementName);
        if (child != null) {
            value = child.getValue();
        }
        return value;
    }

    /**
     * Gets the resource strings for the given command command-ID and command property name.
     */
    protected String[] getConfigStrings(ConfigurationElement config, String elementName) {
        String value = getConfigString(config, elementName);
        if (value == null) {
            return null;
        }
        return StringUtils.split(value, new char[]{','}, true);
    }


    /**
     * Gets the boolean resource value for the given command command-ID and command property name.
     */
    protected Boolean getConfigBoolean(ConfigurationElement config, String elementName) {
        String value = getConfigString(config, elementName);
        if (value != null) {
            return Boolean.valueOf(value);
        }
        return null;
    }

    protected Icon getConfigIcon(ConfigurationElement config, String elementName) {
        String value = getConfigString(config, elementName);
        if (value != null) {
            try {
                return UIUtils.loadImageIcon(value, getClass());
            } catch (RuntimeException e) {
                // todo - handle missing icon here
                e.printStackTrace();
            }
        }
        return null;
    }
}