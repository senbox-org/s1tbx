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
import org.esa.beam.util.StringUtils;
import org.esa.nest.util.ResourceUtils;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;

/**
 * This action emails a problem to Array
 *
 */
public class ReportABugAction extends ExecCommand {
    private static final String PR_EMAIL = "mailto:nest_pr@array.ca";

    /**
     * Invoked when a command action is performed.
     *
     * @param event the command event.
     */
    @Override
    public void actionPerformed(CommandEvent event) {

        final String contextID = ResourceUtils.getContextID();
        final String ver = System.getProperty(contextID+".version");
        String email = System.getProperty(contextID+".contact_email");
        if(email == null || email.isEmpty())
            email = PR_EMAIL;

        final Desktop desktop = Desktop.getDesktop();
        final String mail = email + "?subject="+contextID+ver+"-Problem-Report&body=Description:%0A%0A%0A%0A";
        final String sysInfo = getSystemInfo();

        final String longmail = mail + "SysInfo:%0A" + sysInfo.substring(0, Math.min(1800, sysInfo.length()));

        try {
            desktop.mail(URI.create(longmail));
        } catch (Exception e) {
            e.printStackTrace();
            try {
                desktop.mail(URI.create(mail));
            } catch(IOException ex) {
                //
            }
        }
    }

    private static String getSystemInfo() {

        String sysInfoStr="";
        Properties sysProps = null;
        try {
            sysProps = System.getProperties();
        } catch (RuntimeException e) {
            //
        }
        if (sysProps != null) {
            String[] names = new String[sysProps.size()];
            Enumeration<?> e = sysProps.propertyNames();
            for (int i = 0; i < names.length; i++) {
                names[i] = (String) e.nextElement();
            }
            Arrays.sort(names);
            for (String name : names) {
                if(name.equals("java.class.path") || name.equals("java.library.path") || name.contains("vendor"))
                    continue;

                if(name.startsWith("beam") || name.startsWith("nest") || name.startsWith("ceres")
                   || name.startsWith("os") || name.startsWith("java")) {
                    
                    String value = sysProps.getProperty(name);
                    sysInfoStr += name + "=" + value + "%0A";
                }
            }
        }

        return StringUtils.createValidName(sysInfoStr.trim(),
                new char[]{'_', '-', '.','=','/','@','~','%','*','(',')','+','!','#','$','^','&',':','<','>','|'},
                //new char[]{'_', '-', '.', '%','=' },
                '_');
    }
}