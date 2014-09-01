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

package org.esa.beam.framework.ui.application;

/**
 * Provides descriptive information for applications.
 * @see org.esa.beam.framework.ui.BasicApp
 */
public interface ApplicationDescriptor {
    String getApplicationId();

    String getSymbolicName();

    String getDisplayName();

    String getVersion();

    String getBuildId();

    String getBuildDate();

    String getFrameIconPaths();

    String getAboutImagePath();

    String getResourceBundleName();

    String getCopyright();

    String[] getExcludedActions();

    String[] getExcludedActionGroups();

    String[] getExcludedToolViews();
}
