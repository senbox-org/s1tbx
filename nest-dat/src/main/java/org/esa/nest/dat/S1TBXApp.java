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
package org.esa.nest.dat;

import com.alee.laf.WebLookAndFeel;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.swing.desktop.TabbedDesktopPane;
import com.jidesoft.plaf.LookAndFeelFactory;
import com.jidesoft.swing.JideTabbedPane;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.application.ApplicationDescriptor;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.util.ResourceUtils;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public final class S1TBXApp extends DatApp {
    public S1TBXApp(ApplicationDescriptor applicationDescriptor) {
        super(applicationDescriptor);
    }

    public static S1TBXApp getApp() {
        return (S1TBXApp) VisatApp.getApp();
    }

  /*  protected TabbedDesktopPane createDesktop() {
        final JDesktopPane desktopPane = new JDesktopPane() {
            //final URL imgPath = S1TBXApp.class.getClassLoader().getResource("images/desktop-pane.jpg");
            final URL imgPath = S1TBXApp.class.getClassLoader().getResource("images/azure3.png");
            private final Image image = Toolkit.getDefaultToolkit().getImage(imgPath);

            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
            }
        };

        return new TabbedDesktopPane(new JideTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT), desktopPane);
    }*/

    @Override
    protected boolean initLookAndFeel() {

        try {
            UIManager.installLookAndFeel("WebLookAndFeel", "com.alee.laf.WebLookAndFeel");
        } catch (Throwable e) {
            e.printStackTrace();
        }

        String currentLafClassName = UIManager.getLookAndFeel().getClass().getName();

        String defaultLafClassName = "com.alee.laf.WebLookAndFeel";//getDefaultLookAndFeelClassName();
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


            //printUIProps();

            uiDefaultsInitialized = true;

            return true;
        }
        return false;
    }

    private void printUIProps() {

        try {
            String laf = UIManager.getLookAndFeel().getClass().getName();
            System.out.println(laf);
            System.out.println();

            javax.swing.UIDefaults defaults = UIManager.getDefaults();
            Enumeration newKeys = defaults.keys();

            List<String> list = new ArrayList();
            while (newKeys.hasMoreElements()) {
                Object obj = newKeys.nextElement();
                list.add(obj + " = " + UIManager.get(obj));
            }
            Collections.sort(list);
            for (String s : list) {
                System.out.println(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void initClientUI(ProgressMonitor pm) {
        super.initClientUI(pm);
        try {
            getMainFrame().setIconImage(ResourceUtils.s1Icon.getImage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void loadJideExtension() {
        LookAndFeelFactory.installJideExtension(LookAndFeelFactory.ECLIPSE3X_STYLE);
        // Uncomment this, if we want icons to be displayed on title pane of a DockableFrame
        UIManager.getDefaults().put("DockableFrameTitlePane.showIcon", Boolean.TRUE);
        UIManager.getDefaults().put("SidePane.alwaysShowTabText", Boolean.TRUE);
        UIManager.getDefaults().put("SidePane.orientation", 1);

        UIManager.getDefaults().put("DockableFrame.background", new Color(200, 220, 230));

        UIManager.getDefaults().put("DockableFrame.activeTitleBackground", new Color(200, 220, 220));//new Color(0,127,190));
        UIManager.getDefaults().put("DockableFrame.activeTitleBackground2", new Color(100, 200, 255));//new Color(100,200,200));

        //UIManager.getDefaults().put("JideTabbedPane.showIconOnTab", Boolean.TRUE);
    }

    @Override
    protected String getMainFrameTitle() {
        final String ver = System.getProperty("s1tbx.version");
        return getAppName() + ' ' + ver;
    }

    @Override
    protected ModalDialog createAboutBox() {
        return new DatAboutBox();
    }

}
