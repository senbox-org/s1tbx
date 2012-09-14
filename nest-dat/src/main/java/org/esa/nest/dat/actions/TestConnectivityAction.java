/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat.actions;

import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.util.Settings;
import org.esa.nest.util.ftpUtils;

/**
 * This action test if FTP is working
 *
 */
public class TestConnectivityAction extends ExecCommand {

    @Override
    public void actionPerformed(CommandEvent event) {
        final String remoteFTPSRTM = Settings.instance().get("DEM/srtm3GeoTiffDEM_FTP");
        final String remotePathSRTM = ftpUtils.getPathFromSettings("DEM/srtm3GeoTiffDEM_remotePath");
        final String delftFTP = Settings.instance().get("OrbitFiles/delftFTP");
        final String delftFTPPath = Settings.instance().get("OrbitFiles/delftFTP_ERS2_precise_remotePath");

        boolean failed = false;
        String msg1 = "Connection to FTP "+ remoteFTPSRTM + remotePathSRTM;
        if(ftpUtils.testFTP(remoteFTPSRTM, remotePathSRTM)) {
            msg1 += " PASSED";
        } else {
            msg1 += " FAILED";
            failed = true;
        }

        String msg2 = "Connection to FTP "+ delftFTP + delftFTPPath;
        if(ftpUtils.testFTP(delftFTP, delftFTPPath)) {
            msg2 += " PASSED";
        } else {
            msg2 += " FAILED";
            failed = true;
        }

        String msg = msg1 +"\n" +msg2;
        if(failed) {
            msg += "\n\nPlease verify that all paths are correct in your $NEST_HOME/config/settings.xml";
            msg += "\nAlso verify that FTP is not blocked by your firewall.";
        }
        VisatApp.getApp().showInfoDialog(msg, null);
    }
}