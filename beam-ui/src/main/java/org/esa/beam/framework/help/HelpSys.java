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
package org.esa.beam.framework.help;

import org.esa.beam.util.Guardian;

import javax.help.DefaultHelpBroker;
import javax.help.HelpBroker;
import javax.help.HelpSet;
import java.awt.Component;
import java.awt.Window;

/**
 * This utility class supports the java help system.
 * <p/>
 * It contains exclusively static methods and can hold a <code>HelpBroker</code> singleton from which this class
 * receives its functionallity.
 * <p/>
 * This class can be used in all components which have a context sensitive help.
 *
 * @author Sabine Embacher
 * @version $Revision$ $Date$
 */
public class HelpSys {

    private static DefaultHelpBroker helpBroker;

    /**
     * Adds the given help set to the current helpset.
     *
     * @param helpSet the help set to be added
     */
    public static void add(HelpSet helpSet) {
        if (!isInitialized()) {
            set(helpSet);
            return;
        }
        HelpSet set = helpBroker.getHelpSet();
        if (set == null) {
            helpBroker.setHelpSet(helpSet);
            return;
        }
        set.add(helpSet);
    }

    /**
     * Gets the held help broker
     *
     * @return the held help broker if there is any, otherways <code>null</code>
     */
    public static HelpBroker getHelpBroker() {
        return helpBroker;
    }

    /**
     * Gets the help set from the held help broker
     *
     * @return the help set from the held help broker if there is any, otherways <code>null</code>
     */
    public static HelpSet getHelpSet() {
        if (isInitialized()) {
            return helpBroker.getHelpSet();
        }
        return null;
    }

    /**
     * Enables help for a component. Convenience method which delegates to the held help broker, if ther is any, an
     * applies the help set from the help broker and the help id to the given component.
     *
     * @param component the component to which the help id should applied
     * @param helpId    the help id to be applied
     *
     * @return <code>true</code> if the help was enabled, otherwise false.
     *
     * @throws IllegalArgumentException if the given component is null
     * @throws IllegalArgumentException if the given help id is null
     * @see HelpBroker#enableHelp(Component, String, HelpSet)
     */
    public static boolean enableHelp(Component component, String helpId) {
        Guardian.assertNotNull("component", component);
        Guardian.assertNotNull("helpId", helpId);
        if (getHelpSet() == null || !isValidID(helpId)) {
            return false;
        }
        getHelpBroker().enableHelp(component, helpId, getHelpSet());
        return true;
    }

    /**
     * Enables the Help key on a component. Convenience method which delegates to the held help broker, if ther is any.
     * an applies the help set from the help broker and the help id to the given component.
     *
     * @param component the component to which the help id should applied
     * @param helpId    the help id to be applied
     *
     * @return <code>true</code> if the help key was enabled, otherwise false.
     *
     * @throws IllegalArgumentException if the given component is null
     * @throws IllegalArgumentException if the given help id is null
     * @see HelpBroker#enableHelpKey(Component, String, HelpSet)
     */
    public static boolean enableHelpKey(Component component, String helpId) {
        Guardian.assertNotNull("component", component);
        Guardian.assertNotNull("helpId", helpId);
        if (getHelpSet() == null || !isValidID(helpId)) {
            return false;
        }
        getHelpBroker().enableHelpKey(component, helpId, getHelpSet());
        return true;
    }

    /**
     * Enables help for a component. Convenience method which delegates to the held help broker, if ther is any. This
     * method sets a component's helpID and HelpSet and adds an ActionListener. When an action is performed it displays
     * the component's helpID and HelpSet in the default viewer.
     *
     * @param component the component to which the help id should applied
     * @param helpId    the help id to be applied
     *
     * @return <code>true</code> if the help was enabled, otherwise false.
     *
     * @throws IllegalArgumentException if the component is not a javax.swing.AbstractButton or a java.awt.Button or the
     *                                  given component is null
     * @see HelpBroker#enableHelpOnButton(Component, String, HelpSet)
     */
    public static boolean enableHelpOnButton(Component component, String helpId) {
        Guardian.assertNotNull("component", component);
        if (getHelpSet() == null || !isValidID(helpId)) {
            return false;
        }
        if (helpId == null || helpId.length() == 0) {
            return false;
        }
        getHelpBroker().enableHelpOnButton(component, helpId, getHelpSet());
        return true;
    }


    public static boolean isValidID(String id) {
        if (getHelpSet() == null || id == null) {
            return false;
        }
        return getHelpSet().getCombinedMap().isValidID(id, getHelpSet());
    }

    /**
     * If the help system is initialised this method shows the given help theme.
     *
     * @param helpId the help theme
     */
    public static void showTheme(String helpId) {
        if (isInitialized()) {
            helpBroker.setCurrentID(helpId);
            helpBroker.setDisplayed(true);
        }
    }

    public static boolean isInitialized() {
        return getHelpBroker() != null;
    }

    /**
     * Disposes the application help system.
     */
    public static void dispose() {
        if (isInitialized()) {
            Window helpWindow = helpBroker.getWindowPresentation().getHelpWindow();
            if (helpWindow != null) {
                helpWindow.dispose();
            }
            helpBroker = null;
        }
    }

    /**
     * Sets the current help set. If the current help broker is not null, the help set will be assigned to it.
     * Otherwise, a help broker will be created from the given help set.
     * <p/>
     * This method can be used to switch to an other helpset.
     *
     * @param helpSet the help set to be set
     */
    private static void set(HelpSet helpSet) {
        if (helpBroker == null) {
            helpBroker = new DefaultHelpBroker(helpSet);
        }
        helpBroker.setHelpSet(helpSet);

    }
}
