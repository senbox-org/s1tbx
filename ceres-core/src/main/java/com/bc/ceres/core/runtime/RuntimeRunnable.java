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

package com.bc.ceres.core.runtime;

import com.bc.ceres.core.ProgressMonitor;

/**
 * A runnable which is executed by the runtime.
 * <p>
 * This interface may be implemented by clients.
 */
public interface RuntimeRunnable {

    /**
     * Executes client code.
     * <p>
     * If this RuntimeRunnable is an application launched by the Ceres runtime,
     * the <code>argument</code> parameter can safely be casted to a <code>String[]</code>.
     * This array contains all command-line arguments passed to the application.
     *
     * @param argument the argument passed to the RuntimeRunnable, which may be null.
     * @param pm a progress monitor which may be used by the client code
     * @throws Exception if any error occurs in the client code
     */
    void run(Object argument, ProgressMonitor pm) throws Exception;
}
