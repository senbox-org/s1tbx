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

import com.alee.extended.panel.WebAccordion;
import com.alee.extended.panel.WebAccordionStyle;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.visat.VisatApp;
import org.esa.snap.util.ResourceUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * This class pertains to the "about" dialog box for the VISAT application.
 */
class DatAboutBox extends ModalDialog {

    public DatAboutBox() {
        this(new JButton[]{
                new JButton(),
                new JButton(),
        });
    }

    private DatAboutBox(JButton[] others) {
        super(VisatApp.getApp().getMainFrame(), "About " + VisatApp.getApp().getAppName(),
                ModalDialog.ID_OK, others, null);    /*I18N*/

        final JButton creditsButton = others[0];
        creditsButton.setText("Credits...");  /*I18N*/
        creditsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showCreditsDialog();
            }
        });

        final JButton systemButton = others[1];
        systemButton.setText("System Info...");  /*I18N*/
        systemButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showSystemDialog();
            }
        });

        final File homeFolder = ResourceUtils.findHomeFolder();
        final File imgFile = new File(homeFolder, "resource" + File.separator + "images" + File.separator + "sentinel_toolboxes_banner.png");
        final Icon icon = new ImageIcon(imgFile.getAbsolutePath());

        final JLabel imageLabel = new JLabel(icon);
        final JPanel dialogContent = new JPanel(new BorderLayout());
        final String versionText = getVersionHtml();
        final JLabel versionLabel = new JLabel(versionText);

        final JPanel labelPane = new JPanel(new BorderLayout());
        labelPane.add(BorderLayout.NORTH, versionLabel);

        dialogContent.setLayout(new BorderLayout(4, 4));
        dialogContent.add(BorderLayout.NORTH, imageLabel);
        dialogContent.add(BorderLayout.SOUTH, labelPane);

        setContent(dialogContent);
    }

    @Override
    protected void onOther() {
        // override default behaviour by doing nothing
    }

    private void showCreditsDialog() {
        final ModalDialog modalDialog = new ModalDialog(getJDialog(), "Credits", ID_OK, null); /*I18N*/
        final WebAccordion accordion = new WebAccordion(WebAccordionStyle.accordionStyle);
        accordion.setMultiplySelectionAllowed(false);
        accordion.addPane("S1TBX", new JScrollPane(new JLabel(getCreditsHtmlS1TBX())));
        accordion.addPane("S2TBX", new JScrollPane(new JLabel(getCreditsHtmlS2TBX())));
        accordion.addPane("S3TBX", new JScrollPane(new JLabel(getCreditsHtmlS3TBX())));
        accordion.addPane("NEST", new JScrollPane(new JLabel(getCreditsHtmlNEST())));
        accordion.addPane("RSTB", new JScrollPane(new JLabel(getCreditsHtmlRSTB())));
        accordion.addPane("BEAM", new JScrollPane(new JLabel(getCreditsHtmlBEAM())));
        accordion.addPane("Special Thanks", new JScrollPane(new JLabel(getCreditsHtmlSpecialThanks())));
        modalDialog.setContent(accordion);
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
        final String ver = System.getProperty(SystemUtils.getApplicationContextId() + ".version");
        final String pattern =
                "<html>" +
                        "<b>S1TBX</b>" +
                        "<br>(c) Copyright 2014 by Array Systems Computing Inc. and contributors. All rights reserved." +
                        "<br>" +
                        "<b>S2TBX</b>" +
                        "<br>(c) Copyright 2014 by C-S and contributors. All rights reserved." +
                        "<br>" +
                        "<b>S3TBX</b>" +
                        "<br>(c) Copyright 2014 by Brockmann Consult and contributors. All rights reserved." +
                        "<br>" +
                        "<b>NEST</b>" +
                        "<br>(c) Copyright 2007-2014 by Array Systems Computing Inc. and contributors. All rights reserved." +
                        "<br>" +
                        "<b>RSTB</b>" +
                        "<br>(c) Copyright 2010-2014 by Array Systems Computing Inc. and contributors. All rights reserved." +
                        "<br>" +
                        "<b>JLINDA</b>" +
                        "<br>(c) Copyright 2009-2014 by PPO.labs and contributors. All rights reserved." +
                        "<br>" +
                        "<b>BEAM </b>" +
                        "<br>(c) Copyright 2002-2014 by Brockmann Consult and contributors. All rights reserved." +
                        "<br>" +
                        "<br>This program has been developed under contract to ESA (ESRIN)." +
                        "<br>" +
                        "<br>This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License" +
                        "<br>as published by the Free Software Foundation. This program is distributed in the hope it will be useful, but WITHOUT ANY" +
                        "<br>WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE." +
                        "<br>See the GNU General Public License for more details." +
                        "<br>" +
                        "<br>This product includes software developed by Unidata and NCSA. Visit http://www.unidata.ucar.edu/ and http://hdf.ncsa.uiuc.edu/" +
                        "</html>";
        return pattern; /*I18N*/
    }

    private static String getCreditsHtmlS1TBX() {
        return "<html>" +
                "<center><u><b>The S1TBX team</b></u></center>" +
                "<b>Array Systems Computing</b>:" +
                "<table border=0>" +
                "<tr><td>" +
                "&nbsp;&nbsp;<b>Rajesh Jha</b><br>" +
                "&nbsp;&nbsp;<b>Luis Veci</b><br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Jun Lu</b><br>" +
                "&nbsp;&nbsp;<b>Cecilia Wong</b> <br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Serge Stankovic</b><br>" +
                "&nbsp;&nbsp;<b>Andrew Taylor</b><br>" +
                "</td></tr>" +
                "</table>" +
                "<table border=0>" +
                "<tr><td>" +
                "<b>German Aerospace Center</b> (DLR):" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Pau Prats-Iraola</b><br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Rolf Scheiber</b><br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Marc Rodriguez-Cassola</b><br>" +
                "</td></tr>" +
                "</table>" +
                "<table border=0>" +
                "<tr><td>" +
                "<b>Ocean Data Lab</b> (ODL):" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Fabrice Collard</b><br>" +
                "</td></tr>" +
                "</table>" +
                "<table border=0>" +
                "<tr><td>" +
                "<b>European Space Agency</b> (ESA) ESRIN:<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Marcus Engdahl</b><br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Michael Foumelis</b><br>" +
                "</td></tr>" +
                "</table>" +
                "</html>"; /*I18N*/
    }

    private static String getCreditsHtmlS2TBX() {
        return "<html>" +
                "<center><u><b>The S2TBX team</b></u></center>" +
                "<b>C-S</b>:" +
                "<table border=0>" +
                "<tr><td>" +
                "&nbsp;&nbsp;<b>Olivier Thepaut</b><br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Nicolas Ducoin</b><br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Julien Malik</b><br>" +
                "</td></tr>" +
                "</table>" +
                "<table border=0>" +
                "<tr><td>" +
                "<b>European Space Agency</b> (ESA) ESRIN:<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Ferran Gascon</b><br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Fabrizio Ramoino</b><br>" +
                "</td></tr>" +
                "</table>" +
                "</html>"; /*I18N*/
    }

    private static String getCreditsHtmlS3TBX() {
        return "<html>" +
                "<center><u><b>The S3TBX team</b></u></center>" +
                "<b>Brockmann Consult</b>:" +
                "<table border=0>" +
                "<tr><td>" +
                "&nbsp;&nbsp;<b>Tom Block</b><br>" +
                "&nbsp;&nbsp;<b>Carsten Brockmann</b><br>" +
                "&nbsp;&nbsp;<b>Sabine Embacher</b><br>" +
                "&nbsp;&nbsp;<b>Olga Faber</b><br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Tonio Fincke</b><br>" +
                "&nbsp;&nbsp;<b>Norman Fomferra</b><br>" +
                "&nbsp;&nbsp;<b>Uwe Kramer</b><br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Des Murphy</b><br>" +
                "&nbsp;&nbsp;<b>Michael Paperin</b><br>" +
                "&nbsp;&nbsp;<b>Marco Peters</b><br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Ralf Quast</b><br>" +
                "&nbsp;&nbsp;<b>Kerstin Stelzer</b><br>" +
                "&nbsp;&nbsp;<b>Marco Zuhlke</b><br>" +
                "</td></tr>" +
                "</table>" +
                "<table border=0>" +
                "<tr><td>" +
                "<b>European Space Agency</b> (ESA) ESRIN:<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Peter Regner</b><br>" +
                "</td></tr>" +
                "</table>" +
                "</html>"; /*I18N*/
    }

    private static String getCreditsHtmlNEST() {
        return "<html>" +
                "<center><u><b>The NEST team</b></u></center>" +
                "<b>Array Systems Computing</b>:" +
                "<table border=0>" +
                "<tr><td>" +
                "&nbsp;&nbsp;<b>Rajesh Jha</b> (project manager)<br>" +
                "&nbsp;&nbsp;<b>Luis Veci</b> (software lead)<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Jun Lu</b> (scientist/developer)<br>" +
                "&nbsp;&nbsp;<b>Shengli Dai</b> (scientist)<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Iris Buchan</b> (quality assurance)<br>" +
                "&nbsp;&nbsp;<b>Andrew Taylor</b> (IT support)<br>" +
                "</td></tr>" +
                "</table>" +
                "<table border=0>" +
                "<tr><td>" +
                "<b>TU Delft/PPO.Labs</b>:" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Ramon Hanssen</b> (project manager)<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Petar Marinkovic</b> (software lead/scientist)<br>" +
                "</td></tr>" +
                "</table>" +
                "<table border=0>" +
                "<tr><td>" +
                "<b>European Space Agency</b> (ESA) ESRIN:<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Marcus Engdahl</b> (technical officer)<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Andrea Minchella</b> (scientist)<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Romain Husson</b> (scientist)<br>" +
                "</td></tr>" +
                "</table>" +
                "</html>"; /*I18N*/
    }

    private static String getCreditsHtmlRSTB() {
        return "<html>" +
                "<center><u><b>The RSTB team</b></u></center>" +
                "<b>Array Systems Computing</b>:" +
                "<table border=0>" +
                "<tr><td>" +
                "&nbsp;&nbsp;<b>Rajesh Jha</b> (RSTB PM)<br>" +
                "&nbsp;&nbsp;<b>Ali Mahmoodi</b> (ASMERS PM)<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Luis Veci</b> (software lead)<br>" +
                "&nbsp;&nbsp;<b>Jun Lu</b> (scientist/developer)<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Cecilia Wong</b> (developer)<br>" +
                "&nbsp;&nbsp;<b>Roberta Manners</b> (scientist)<br>" +
                "</td></tr>" +
                "</table>" +
                "<table border=0>" +
                "<tr><td>" +
                "<b>Canadian Space Agency</b> (CSA):<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Stephane Chalifoux</b> (project authority)<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Robert Saint-Jean</b> (technical authority)<br>" +
                "</td></tr>" +
                "</table>" +
                "<table border=0>" +
                "<tr><td>" +
                "<b>Canada Centre for Mapping and Earth Observation</b> (CCMEO):<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Francois Charboneau</b> (technical authority)<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b></b><br>" +
                "</td></tr>" +
                "</table>" +
                "<table border=0>" +
                "<tr><td>" +
                "<b>Canadian Forest Service</b> (CFS):<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Hao Chen</b> (senior physical scientist)<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>David Hill</b> (analyst)<br>" +
                "</td></tr>" +
                "</table>" +
                "<table border=0>" +
                "<tr><td>" +
                "<b>Geological Survey of Canada</b> (GSC):<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Paul Fraser</b> (remote sensing specialist)<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Dustin Whalen</b> (physical scientist)<br>" +
                "</td></tr>" +
                "</table>" +
                "<table border=0>" +
                "<tr><td>" +
                "<b>Agriculture and Agri-Food Canada</b> (AAFC):<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Heather McNairn</b><br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Amine Merzouki</b><br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Catherine Champagne</b><br>" +
                "</td></tr>" +
                "</table>" +
                "</html>"; /*I18N*/
    }

    private static String getCreditsHtmlBEAM() {
        return "<html>" +
                "<center><u><b>The BEAM team</b></u></center>" +
                "<b>Brockmann Consult</b>:" +
                "<table border=0>" +
                "<tr><td>" +
                "&nbsp;&nbsp;<b>Tom Block</b> (programming)<br>" +
                "&nbsp;&nbsp;<b>Carsten Brockmann</b> (quality control)<br>" +
                "&nbsp;&nbsp;<b>Sabine Embacher</b> (programming)<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Olga Faber</b> (testing)<br>" +
                "&nbsp;&nbsp;<b>Norman Fomferra</b> (project lead)<br>" +
                "&nbsp;&nbsp;<b>Uwe Kramer</b> (Mac OS X porting)<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Des Murphy</b> (contract management)<br>" +
                "&nbsp;&nbsp;<b>Michael Paperin</b> (web development)<br>" +
                "&nbsp;&nbsp;<b>Marco Peters</b> (programming)<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Ralf Quast</b> (programming)<br>" +
                "&nbsp;&nbsp;<b>Kerstin Stelzer</b> (quality control)<br>" +
                "&nbsp;&nbsp;<b>Marco Zuhlke</b> (programming)<br>" +
                "</td></tr>" +
                "</table>" +
                "<table border=0>" +
                "<tr><td>" +
                "<b>European Space Agency</b> (ESA) ESRIN:<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Peter Regner</b> (technical officer)<br>" +
                "</td></tr>" +
                "</table>" +
                "</html>"; /*I18N*/
    }

    private static String getCreditsHtmlSpecialThanks() {
        return "<html>" +
                "Special contributions made by:" +
                "<table border=0>" +
                "<tr><td>" +
                "&nbsp;&nbsp;<b>Jason Fritz</b> (Cosmo-Skymed Calibration)<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Emanuella Boros</b> (Summer of Code)<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Christoph Sperl</b> (Catalysts)<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Max König</b> (NPolar)<br>" +
                "</td></tr>" +
                "</table>" +

                "<hr>Special thanks to:" +
                "<table border=0>" +
                "<tr><td>" +
                "&nbsp;&nbsp;Yves Louis Desnos<br>" +
                "&nbsp;&nbsp;Yann Denis<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;Pier Georgio Marchetti<br>" +
                "&nbsp;&nbsp;Steven Delwart<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;Medhavy Thankappan<br>" +
                "&nbsp;&nbsp;Mark Williams<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;Joe Buckley<br>" +
                "&nbsp;&nbsp;David Goodenough<br>" +
                "</td></tr>" +
                "</table>" +

                "<hr>The developers would also like to say thank you for <b>NASA WorldWind</b>, <b>IntelliJ</b>, <b>JProfiler</b>, <b>Install4J</b>, and" +
                "<br>all companies and organisations supporting the open-source idea and Earth Observation." +
                "</html>"; /*I18N*/
    }

    private static Object[][] getSystemInfo() {

        final List<Object[]> data = new ArrayList<Object[]>();

        Properties sysProps = null;
        try {
            sysProps = System.getProperties();
        } catch (RuntimeException e) {
        }
        if (sysProps != null) {
            final String[] names = new String[sysProps.size()];
            final Enumeration<?> e = sysProps.propertyNames();
            for (int i = 0; i < names.length; i++) {
                names[i] = (String) e.nextElement();
            }
            Arrays.sort(names);
            for (String name : names) {
                final String value = sysProps.getProperty(name);
                data.add(new Object[]{name, value});
            }
        }

        final Object[][] dataArray = new Object[data.size()][2];
        for (int i = 0; i < dataArray.length; i++) {
            dataArray[i] = data.get(i);
        }
        return dataArray;
    }
}
