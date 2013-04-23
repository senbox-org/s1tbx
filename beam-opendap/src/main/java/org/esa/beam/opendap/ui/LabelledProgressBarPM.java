/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.opendap.ui;

import com.bc.ceres.core.ProgressMonitor;

/**
 * Progress monitor that features and updates two labels and a progress bar.
 *
 * @author Tonio Fincke
 * @author Thomas Storm
 */
public interface LabelledProgressBarPM extends ProgressMonitor {

    void setPreMessage(String preMessageText);

    void setPostMessage(String postMessageText);

    int getTotalWork();

    int getCurrentWork();

    void setTooltip(String tooltip);
}
