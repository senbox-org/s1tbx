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
package org.esa.snap.core.util.logging;

import org.esa.snap.core.util.SystemUtils;

import java.util.logging.Logger;

/**
 * This class is the central manager class for logging. It exposes a set of convenience methods for the initialisation
 * and configuration of the logging framework.
 */
@Deprecated
public class BeamLogManager {

    /**
     * Gets the SNAP system logger ("org.esa.snap").
     *
     * @return the system logger
     * @deprecated use SystemUtils#LOG
     */
    @Deprecated
    public static Logger getSystemLogger() {
        return SystemUtils.LOG;
    }
}
