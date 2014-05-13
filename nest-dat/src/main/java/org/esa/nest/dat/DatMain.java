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

import com.jidesoft.utils.Lm;
import org.esa.beam.framework.ui.application.ApplicationDescriptor;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.VisatMain;

public class DatMain extends VisatMain {

    @Override
    protected void verifyJideLicense() {
        Lm.verifyLicense("Array", "NEST", "YxhjAbyq0epTKa6vcSTdNx8O1gLVRbL");
    }

    @Override
    protected VisatApp createApplication(ApplicationDescriptor applicationDescriptor) {
        return new DatApp(applicationDescriptor);
    }
}