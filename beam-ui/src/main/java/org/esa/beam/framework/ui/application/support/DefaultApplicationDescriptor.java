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

package org.esa.beam.framework.ui.application.support;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.ConfigurableExtension;
import com.bc.ceres.core.runtime.ConfigurationElement;
import com.bc.ceres.core.runtime.Module;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.SingleValueConverterWrapper;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import org.esa.beam.framework.ui.application.ApplicationDescriptor;
import org.esa.beam.util.SystemUtils;

import java.util.ArrayList;

public class DefaultApplicationDescriptor implements ConfigurableExtension, ApplicationDescriptor {

    private String applicationId;
    private String version;
    private String buildId;
    private String buildDate;
    private String copyright;
    private String displayName;
    private String symbolicName;
    private String loggerName;

    @XStreamAlias("resourceBundle")
    private String resourceBundleName;

    @XStreamAlias("frameIcon")
    private String frameIconPath;

    @XStreamAlias("image")
    private String imagePath;

    private ArrayList<ID> excludedActions;
    private ArrayList<ID> excludedToolViews;


    public String getApplicationId() {
        return applicationId;
    }

    public String getCopyright() {
        return copyright;
    }

    public String getResourceBundleName() {
        return resourceBundleName;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getVersion() {
        return version;
    }

    public String getFrameIconPath() {
        return frameIconPath;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String[] getExcludedActions() {
        return toStringArray(excludedActions);
    }

    public String[] getExcludedToolViews() {
        return toStringArray(excludedToolViews);
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public void setResourceBundleName(String resourceBundleName) {
        this.resourceBundleName = resourceBundleName;
    }

    public void setFrameIconPath(String frameIconPath) {
        this.frameIconPath = frameIconPath;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public void setSymbolicName(String symbolicName) {
        this.symbolicName = symbolicName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getBuildId() {
        return buildId;
    }

    public void setBuildId(String buildId) {
        this.buildId = buildId;
    }

    public String getBuildDate() {
        return buildDate;
    }

    public void setBuildDate(String buildDate) {
        this.buildDate = buildDate;
    }

    /**
     * Configures this extension with the supplied configuration data.
     *
     * @param config The configuration data.
     * @throws com.bc.ceres.core.CoreException
     *          if an error occured during configuration.
     */
    public void configure(ConfigurationElement config) throws CoreException {

        if (applicationId == null) {
            throw new CoreException(String.format("Missing configuration element 'applicationId' in element '%s'.", config.getName()));
        }

        Module declaringModule = config.getDeclaringExtension().getDeclaringModule();
        if (displayName == null) {
            displayName = declaringModule.getName();
        }
        if (symbolicName == null) {
            symbolicName = declaringModule.getSymbolicName();
        }
        if (version == null) {
            version = declaringModule.getVersion().toString();
        }
        final String buildIdKey = SystemUtils.getApplicationContextId() + ".build.id";
        if (buildId == null || String.format("${%s}", buildIdKey).equals(buildId)) {
            buildId = System.getProperty(buildIdKey);
        }
        final String buildDateKey = SystemUtils.getApplicationContextId() + ".build.date";
        if (buildDate == null || String.format("${%s}", buildDateKey).equals(buildDate)) {
            buildDate = System.getProperty(buildDateKey);
        }
        if (copyright == null) {
            copyright = declaringModule.getCopyright();
        }
        if (frameIconPath == null) {
            frameIconPath = "/org/esa/beam/resources/images/icons/BeamIcon24.png";
        }
        if (imagePath == null) {
            imagePath = "/org/esa/beam/resources/images/about.jpg";
        }
        if (loggerName == null) {
            loggerName = declaringModule.getSymbolicName();
        }
    }

    private String[] toStringArray(ArrayList<ID> idList) {
        if (idList == null) {
            return new String[0];
        }
        final String[] strings = new String[idList.size()];
        for (int i = 0; i < idList.size(); i++) {
            ID id = idList.get(i);
            strings[i] = id.id;
        }
        return strings;
    }

    @XStreamAlias("id")
    @XStreamConverter(IDC.class)
    public static class ID {
        private String id;

        private ID(String id) {
            this.id = id;
        }
    }

    public static class IDC extends SingleValueConverterWrapper {
        public IDC() {
            super(new IDSVC());
        }
    }


    public static class IDSVC extends AbstractSingleValueConverter {
        @Override
        public boolean canConvert(Class type) {
            return type == ID.class;
        }

        @Override
        public Object fromString(String str) {
            return new ID(str);
        }
    }
}
