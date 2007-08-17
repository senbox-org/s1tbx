/*
 * $Id: Auxdata.java,v 1.1 2007/03/27 12:51:05 marcoz Exp $
 *
 * Copyright (C) 2007 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.gpf.support;

import com.bc.ceres.core.NullProgressMonitor;
import org.esa.beam.util.ResourceInstaller;
import org.esa.beam.util.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Created by marcoz.
 *
 * @author marcoz
 * @version $Revision: 1.1 $ $Date: 2007/03/27 12:51:05 $
 */
public class Auxdata {

    private final File defaultAuxdataDir;
    private String relativePath;

    /**
     *
     */
    public Auxdata(String symbolicName, String relPath) {
        relativePath = "auxdata" + File.separator + relPath;
        final String auxdirPath = ".beam" + File.separator +
                symbolicName + File.separator +
                relativePath;
        defaultAuxdataDir = new File(SystemUtils.getUserHomeDir(), auxdirPath);
    }

    public File getDefaultAuxdataDir() {
        return defaultAuxdataDir;
    }

    public void installAuxdata(Object operator) throws IOException {
        Class opClass = operator.getClass();
        URL codeSourceUrl = opClass.getProtectionDomain().getCodeSource().getLocation();

        installAuxdata(codeSourceUrl, relativePath);
    }

    private void installAuxdata(URL sourceLocation, String relSourcePath) throws IOException {
        ResourceInstaller resourceInstaller = new ResourceInstaller(sourceLocation, relSourcePath, defaultAuxdataDir);
        resourceInstaller.install(".*", new NullProgressMonitor());
    }
}
