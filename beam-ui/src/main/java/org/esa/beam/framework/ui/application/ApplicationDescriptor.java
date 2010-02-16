package org.esa.beam.framework.ui.application;

/**
 * Provides descriptive informtion for applications.
 * @see org.esa.beam.framework.ui.BasicApp
 */
public interface ApplicationDescriptor {
    String getApplicationId();

    String getSymbolicName();

    String getDisplayName();

    String getVersion();

    String getFrameIconPath();

    String getImagePath();

    String getResourceBundleName();

    String getCopyright();

    String[] getExcludedActions();

    String[] getExcludedToolViews();
}
