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
package org.esa.beam.visat;

import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.application.ApplicationDescriptor;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * This class pertains to the "about" dialog box for the application.
 */
public class VisatAboutBox extends ModalDialog {

    public VisatAboutBox() {
        this(new JButton[]{
//                new JButton(),
                new JButton(),
        });
    }

    private VisatAboutBox(JButton[] others) {
        super(VisatApp.getApp().getMainFrame(), String.format("About %s", VisatApp.getApp().getAppName()),
              ModalDialog.ID_OK, others, null);    /*I18N*/
// TODO - the credits info is not up to date, so I commented it out. We shall put this info on the BEAM home page ASAP! (nf)
//        JButton creditsButton = others[0];
//        creditsButton.setText("Credits...");  /*I18N*/
//        creditsButton.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                showCreditsDialog();
//            }
//        });
//
//        JButton systemButton = others[1];
         JButton systemButton = others[0];
        systemButton.setText("System Info...");  /*I18N*/
        systemButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showSystemDialog();
            }
        });

        final ApplicationDescriptor applicationDescriptor = VisatApp.getApp().getApplicationDescriptor();
        String imagePath = applicationDescriptor.getImagePath();
        Icon imageIcon = null;
        if (imagePath != null) {
            URL resource = VisatApp.getApp().getClass().getResource(imagePath);
            if (resource != null) {
                imageIcon = new ImageIcon(resource);
            }
        }

        JLabel imageLabel = new JLabel(imageIcon);
        JPanel dialogContent = new JPanel(new BorderLayout());
        String versionText = getVersionHtml();
        JLabel versionLabel = new JLabel(versionText);

        JPanel labelPane = new JPanel(new BorderLayout());
        labelPane.add(BorderLayout.NORTH, versionLabel);

        dialogContent.setLayout(new BorderLayout(4, 4));
        dialogContent.add(BorderLayout.WEST, imageLabel);
        dialogContent.add(BorderLayout.EAST, labelPane);

        setContent(dialogContent);
    }

    @Override
    protected void onOther() {
        // override default behaviour by doing nothing
    }

    private void showCreditsDialog() {
        final ModalDialog modalDialog = new ModalDialog(getJDialog(), "Credits", ID_OK, null); /*I18N*/
        final String credits = getCreditsHtml();
        final JLabel creditsPane = new JLabel(credits); /*I18N*/
        modalDialog.setContent(creditsPane);
        modalDialog.show();
    }


    private void showSystemDialog() {
        final ModalDialog modalDialog = new ModalDialog(getJDialog(), "System Info", ID_OK, null);
        final Object[][] sysInfo = getSystemInfo();
        final JTable sysTable = new JTable(sysInfo, new String[]{"Property", "Value"}); /*I18N*/
        final JScrollPane systemScroll = new JScrollPane(sysTable);
        systemScroll.setPreferredSize(new Dimension(400, 400));
        modalDialog.setContent(systemScroll);
        modalDialog.show();
    }

    private static String getVersionHtml() {
        // todo - load text from resource
        final String pattern = "<html>" +
                               "<b>{0} Version {1}</b>" +
                               "<br><b>{2}</b>" +
                               "<br>" +
                               "<br>This software is based on the BEAM toolbox." +
                               "<br>(c) Copyright 2002-2010 by Brockmann Consult and contributors." +
                               "<br>Visit http://www.brockmann-consult.de/beam/" +
                               "<br>BEAM is developed under contract to ESA (ESRIN)." +
                               "<br>Visit http://envisat.esa.int/services/" +
                               "<br>" +
                               "<br>BEAM is free software; you can redistribute it and/or modify it" +
                               "<br>under the terms of the GNU General Public License as published by the" +
                               "<br>Free Software Foundation. This program is distributed in the hope it will be" +
                               "<br>useful, but WITHOUT ANY WARRANTY; without even the implied warranty" +
                               "<br>of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE." +
                               "<br>See the GNU General Public License for more details." +
                               "</html>";
        return MessageFormat.format(pattern,
                                    VisatApp.getApp().getAppName(),
                                    VisatApp.getApp().getAppVersion(),
                                    VisatApp.getApp().getAppCopyright());
    }

    private static String getCreditsHtml() {
        // todo - load text from resource
        return
                "<html>" +
                "<br>Special thanks for their BEAM contributions goes to" +
                "<br>&nbsp;&nbsp;<b>Max Aulinger</b> for the implementation of the ROI pixel export," +
                "<br>&nbsp;&nbsp;<b>Christian Berwanger</b> for the LANDSAT TM reader," +
                "<br>&nbsp;&nbsp;<b>Marc Bouvet</b> from ESRIN for the GETASSE30 elevation model," +
                "<br>&nbsp;&nbsp;<b>Roland Doerffer</b> from GKSS for the valuable ideas and promoting BEAM," +
                "<br>&nbsp;&nbsp;<b>Jim Gower</b> from Fisheries and Oceans Canada for the FLH/MCI algorithm," +
                "<br>&nbsp;&nbsp;<b>Tom Lancester</b> from InfoTerra for the implementation of the flux conserving binning," +
                "<br>&nbsp;&nbsp;<b>Rene Preusker</b> from FU Berlin for the neural nets used in the MERIS cloud detection," +
                "<br>&nbsp;&nbsp;<b>Peter Regner</b> from ESRIN for his enthusiasm in 'his' project," +
                "<br>&nbsp;&nbsp;<b>Serge Riazanoff</b> from VisioTerra for the development of the orthorectification algorithm," +
                "<br>&nbsp;&nbsp;<b>Mike Rast</b> from ESTEC for promoting BEAM and training users," +
                "<br>&nbsp;&nbsp;<b>Helmut Schiller</b> from GKSS for his advice on tricky mathematical problems," +
                "<br>&nbsp;&nbsp;and all the other people who helped us making this software (better)." +
                "<br><hr>" +
                "<br>The BEAM developers would also like to say thank you to" +
                "<br>&nbsp;&nbsp;<b>Sun</b> for the beautiful programming language they have invented," +
                "<br>&nbsp;&nbsp;<b>JetBrains</b> for IntelliJ IDEA, the best Java IDE in the world," +
                "<br>&nbsp;&nbsp;<b>Eclipse.org</b> for the second best Java IDE in the world," +
                "<br>&nbsp;&nbsp;<b>JIDE Software</b> for providing to us an open source licenses for their docking framework," +
                "<br>&nbsp;&nbsp;<b>Atlassian</b> for providing to us open source licenses of JIRA and Confluence," +
                "<br>&nbsp;&nbsp;<b>LifeRay</b> for their great portlet server and CMS," +
                "<br>&nbsp;&nbsp;the <b>GeoTools</b> team for developer support and their great geo-referencing API," +
                "<br>&nbsp;&nbsp;the <b>JFreeChart</b> open source project," +
                "<br>&nbsp;&nbsp;all companies and organisations supporting the open-source idea." +
                "<br><hr>" +
                "<br>The BEAM team at Brockmann Consult is:" +
                "<table border=0>" +
                "<tr><td>" +
                "&nbsp;&nbsp;<b>Tom Block</b> (programming)<br>" +
                "&nbsp;&nbsp;<b>Carsten Brockmann</b> (quality control)<br>" +
                "&nbsp;&nbsp;<b>Sabine Embacher</b> (programming)<br>" +
                "&nbsp;&nbsp;<b>Olga Faber</b> (testing)<br>" +
                "&nbsp;&nbsp;<b>Norman Fomferra</b> (project lead)<br>" +
                "&nbsp;&nbsp;<b>Uwe Krämer</b> (Mac OS X porting)<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Des Murphy</b> (contract management)<br>" +
                "&nbsp;&nbsp;<b>Michael Paperin</b> (web development)<br>" +
                "&nbsp;&nbsp;<b>Marco Peters</b> (programming)<br>" +
                "&nbsp;&nbsp;<b>Ralf Quast</b> (programming)<br>" +
                "&nbsp;&nbsp;<b>Kerstin Stelzer</b> (quality control)<br>" +
                "&nbsp;&nbsp;<b>Marco Zühlke</b> (programming)<br>" +
                "</td></tr>" +
                "</table>" +
                "<hr>" +
                "</html>"; /*I18N*/
    }

    private static Object[][] getSystemInfo() {

        List<Object[]> data = new ArrayList<Object[]>();

        Properties sysProps = null;
        try {
            sysProps = System.getProperties();
        } catch (RuntimeException e) {
            //ignore
        }
        if (sysProps != null) {
            String[] names = new String[sysProps.size()];
            Enumeration<?> e = sysProps.propertyNames();
            for (int i = 0; i < names.length; i++) {
                names[i] = (String) e.nextElement();
            }
            Arrays.sort(names);
            for (String name : names) {
                String value = sysProps.getProperty(name);
                data.add(new Object[]{name, value});
            }
        }

        Object[][] dataArray = new Object[data.size()][2];
        for (int i = 0; i < dataArray.length; i++) {
            dataArray[i] = data.get(i);
        }
        return dataArray;
    }
}
