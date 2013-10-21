/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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

import org.esa.beam.framework.ui.ModalDialog;
import org.esa.nest.util.ResourceUtils;

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
        super(DatApp.getApp().getMainFrame(), "About "+DatApp.getApp().getAppName(),
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
        final File imgFile = new File(homeFolder, "res"+File.separator+"nest_splash.png");
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
        final String ver = System.getProperty(ResourceUtils.getContextID()+".version");
        final String pattern =
                "<html>" +
                "<b>NEST  Version "+ver+"</b>" +
                "<br>(c) Copyright 2007-2013 by Array Systems Computing Inc. and contributors. All rights reserved." +
                "<br>Visit http://www.array.ca/nest" +
                "<br>" +
                "<b>JLINDA</b>" +
                "<br>(c) Copyright 2009-2013 by PPO.labs and contributors. All rights reserved." +
                "<br>" +
                "<b>BEAM </b>" +
                "<br>(c) Copyright 2002-2013 by Brockmann Consult and contributors. All rights reserved." +
                "<br>Visit http://www.brockmann-consult.de/beam/" +
                "<br>" +
                "<br>This program has been developed under contract to ESA (ESRIN)." +
                "<br>Visit http://envisat.esa.int/services/" +
                "<br>" +
                "<br>This program is free software; you can redistribute it and/or modify it" +
                "<br>under the terms of the GNU General Public License as published by the" +
                "<br>Free Software Foundation. This program is distributed in the hope it will be" +
                "<br>useful, but WITHOUT ANY WARRANTY; without even the implied warranty" +
                "<br>of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE." +
                "<br>See the GNU General Public License for more details." +
                "<br>" +
                "<br>This product includes software developed by Unidata and NCSA" +
                "<br>Visit http://www.unidata.ucar.edu/ and http://hdf.ncsa.uiuc.edu/" +
                "</html>";
        return pattern; /*I18N*/
    }

    private static String getCreditsHtml() {
        return "<html>" +
                "<hr><center><u><b>The NEST team</b></u></center>"+
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
                "<b>TU Delft/PPO.Labs</b>:" +
                "<table border=0>" +
                "<tr><td>" +
                "&nbsp;&nbsp;<b>Ramon Hanssen</b> (project manager)<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Petar Marinkovic</b> (software lead/scientist)<br>" +
                "</td></tr>" +
                "</table>" +
                "<b>European Space Agency</b> (ESA) ESRIN:" +
                "<table border=0>" +
                "<tr><td>" +
                "&nbsp;&nbsp;<b>Marcus Engdahl</b> (technical officer)<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Andrea Minchella</b> (scientist)<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Romain Husson</b> (scientist)<br>" +
                "</td></tr>" +
                "</table>" +

                "<hr><center><u><b>The RSTB team</b></u></center>"+
                "<b>Array Systems Computing</b>:" +
                "<table border=0>" +
                "<tr><td>" +
                "&nbsp;&nbsp;<b>Rajesh Jha</b> (project manager)<br>" +
                "&nbsp;&nbsp;<b>Luis Veci</b> (software lead)<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Jun Lu</b> (scientist/developer)<br>" +
                "&nbsp;&nbsp;<b>Cecilia Wong</b> (developer)<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Roberta Manners</b> (scientist)<br>" +
                "&nbsp;&nbsp;<b></b><br>" +
                "</td></tr>" +
                "</table>" +
                "<b>Canadian Space Agency</b> (CSA):" +
                "<table border=0>" +
                "<tr><td>" +
                "&nbsp;&nbsp;<b>Stéphane Chalifoux</b> (project authority)<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Robert Saint-Jean</b> (technical authority)<br>" +
                "</td></tr>" +
                "</table>" +
                "<b>Canadian Centre for Remote Sensing</b> (CCRS):" +
                "<table border=0>" +
                "<tr><td>" +
                "&nbsp;&nbsp;<b>Francois Charboneau</b> (technical authority)<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b></b><br>" +
                "</td></tr>" +
                "</table>" +
                "<b>Canadian Forest Service</b> (CFS):" +
                "<table border=0>" +
                "<tr><td>" +
                "&nbsp;&nbsp;<b>Hao Chen</b> (research associate)<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>David Hill</b> (Analyst)<br>" +
                "</td></tr>" +
                "</table>" +
                "<b>Geological Survey of Canada</b> (GSC):" +
                "<table border=0>" +
                "<tr><td>" +
                "&nbsp;&nbsp;<b>Paul Fraser</b> (remote sensing specialist)<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Dustin Whalen</b> (physical scientist)<br>" +
                "</td></tr>" +
                "</table>" +
                "<b>Agriculture and Agri-Food Canada</b> (AAFC):" +
                "<table border=0>" +
                "<tr><td>" +
                "&nbsp;&nbsp;<b>Heather McNairn</b><br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Amine Merzouki</b><br>" +
                "</td></tr>" +
                "</table>" +

                "<hr><center><u><b>The BEAM team</b></u></center>" +
                "<b>Brockmann Consult</b>:" +
                "<table border=0>" +
                "<tr><td>" +
                "&nbsp;&nbsp;<b>Tom Block</b> (programming)<br>" +
                "&nbsp;&nbsp;<b>Carsten Brockmann</b> (quality control)<br>" +
                "&nbsp;&nbsp;<b>Sabine Embacher</b> (programming)<br>" +
                "&nbsp;&nbsp;<b>Olga Faber</b> (testing)<br>" +
                "&nbsp;&nbsp;<b>Norman Fomferra</b> (project lead)<br>" +
                "&nbsp;&nbsp;<b>Uwe Kramer</b> (Mac OS X porting)<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;<b>Des Murphy</b> (contract management)<br>" +
                "&nbsp;&nbsp;<b>Michael Paperin</b> (web development)<br>" +
                "&nbsp;&nbsp;<b>Marco Peters</b> (programming)<br>" +
                "&nbsp;&nbsp;<b>Ralf Quast</b> (programming)<br>" +
                "&nbsp;&nbsp;<b>Kerstin Stelzer</b> (quality control)<br>" +
                "&nbsp;&nbsp;<b>Marco Zuhlke</b> (programming)<br>" +
                "</td></tr>" +
                "</table>" +
                "<b>European Space Agency</b> (ESA) ESRIN:" +
                "<table border=0>" +
                "<tr><td>" +
                "&nbsp;&nbsp;<b>Peter Regner</b> (technical officer)<br>" +
                "</td></tr>" +
                "</table>" +

                "<hr>Special contributions made by:" +
                "<table border=0>" +
                "<tr><td>" +
                "&nbsp;&nbsp;<b>Jason Fritz</b> (Cosmo-Skymed Calibration)<br>" +
                "</td>" +
                "<td>" +
                "&nbsp;&nbsp;<b>Emanuella Boros</b> (Summer of Code)<br>" +
                "</td></tr>" +
                "</table>" +

                "<hr>Special thanks to:" +
                "<table border=0>" +
                "<tr><td>" +
                "&nbsp;&nbsp;Yves Louis Desnos<br>" +
                "&nbsp;&nbsp;Yann Denis<br>" +
                "&nbsp;&nbsp;Pier Georgio Marchetti<br>" +
                "&nbsp;&nbsp;Steven Delwart<br>" +
                "</td><td>" +
                "&nbsp;&nbsp;Medhavy Thankappan<br>" +
                "&nbsp;&nbsp;Mark Williams<br>" +
                "&nbsp;&nbsp;Joe Buckley<br>" +
                "&nbsp;&nbsp;David Goodenough<br>" +
                "</td></tr>" +
                "</table>" +

                "<hr>The developers would also like to say thank you to" +
                "<br>&nbsp;&nbsp;All researchers contributing to the world of Earth Observation." +
                "<br>&nbsp;&nbsp;<b>NASA</b> for the wonderful WorldWind Java SDK," +
                "<br>&nbsp;&nbsp;<b>IntelliJ</b> for the best IDE in the world," +
                "<br>&nbsp;&nbsp;<b>JIDE Software</b> for a great docking framework," +
                "<br>&nbsp;&nbsp;<b>Bitrock</b> for a cross platform installer," +
                "<br>&nbsp;&nbsp;All companies and organisations supporting the open-source idea." +
                "<br>" +
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
