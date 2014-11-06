/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap;

import com.alee.laf.WebLookAndFeel;
import com.bc.ceres.core.ProgressMonitor;
import com.jidesoft.plaf.LookAndFeelFactory;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.application.ApplicationDescriptor;
import org.esa.beam.visat.VisatApp;

import javax.swing.UIManager;

public final class SNAPApp extends VisatApp {

    public SNAPApp(ApplicationDescriptor applicationDescriptor) {
        super(applicationDescriptor);
    }


    @Override
    protected boolean initLookAndFeel() {

        try {
            UIManager.installLookAndFeel("WebLookAndFeel", "com.alee.laf.WebLookAndFeel");
        } catch (Throwable e) {
            e.printStackTrace();
        }

        String currentLafClassName = UIManager.getLookAndFeel().getClass().getName();

        String defaultLafClassName = "com.alee.laf.WebLookAndFeel";
        String newLafClassName = getPreferences().getPropertyString(PROPERTY_KEY_APP_UI_LAF, defaultLafClassName);
        // This should fix a problem occurring in JIDE 3.3.5 with the GTKLookAndFeel (nf, 2012-03-02)
        if (newLafClassName.equals("com.sun.java.swing.plaf.gtk.GTKLookAndFeel")) {
            newLafClassName = defaultLafClassName;
        }
        if (!uiDefaultsInitialized || !currentLafClassName.equals(newLafClassName)) {
            try {
                UIManager.setLookAndFeel(newLafClassName);
                getPreferences().setPropertyString(PROPERTY_KEY_APP_UI_LAF, newLafClassName);

                if ("WebLookAndFeel".equals(newLafClassName)) {
                    // Enabling dialog decoration
                    WebLookAndFeel.setDecorateAllWindows(true);
                }
            } catch (Throwable ignored) {
                // ignore
            }
            try {
                loadJideExtension();
            } catch (Throwable ignored) {
                // ignore
            }

            uiDefaultsInitialized = true;

            return true;
        }
        return false;
    }

    @Override
    protected void initClientUI(ProgressMonitor pm) {
        super.initClientUI(pm);
       // getMainFrame().setIconImage(ResourceUtils.s1Icon.getImage());
    }

    protected void loadJideExtension() {
        LookAndFeelFactory.installJideExtension(LookAndFeelFactory.EXTENSION_STYLE_ECLIPSE);

        UIManager.getDefaults().put("DockableFrameTitlePane.showIcon", Boolean.TRUE);
        UIManager.getDefaults().put("SidePane.alwaysShowTabText", Boolean.TRUE);
        UIManager.getDefaults().put("SidePane.orientation", 1);
    }

    @Override
    protected String getMainFrameTitle() {
        final String ver = System.getProperty("snap.version");
        return getAppName() + ' ' + ver;
    }

    @Override
    protected ModalDialog createAboutBox() {
        //todo
        return null;
    }

}
