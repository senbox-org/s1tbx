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

    @XStreamAlias("frameIcons")
    private String frameIconPaths;

    @XStreamAlias("aboutImage")
    private String aboutImagePath;

    @SuppressWarnings("UnusedDeclaration")
    private ArrayList<ID> excludedActions;
    @SuppressWarnings("UnusedDeclaration")
    private ArrayList<ID> excludedActionGroups;
    @SuppressWarnings("UnusedDeclaration")
    private ArrayList<ID> excludedToolViews;

    @Override
    public String getApplicationId() {
        return applicationId;
    }

    @Override
    public String getCopyright() {
        return copyright;
    }

    @Override
    public String getResourceBundleName() {
        return resourceBundleName;
    }

    @Override
    public String getSymbolicName() {
        return symbolicName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getFrameIconPaths() {
        return frameIconPaths;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getAboutImagePath() {
        return aboutImagePath;
    }

    @Override
    public String[] getExcludedActions() {
        return toStringArray(excludedActions);
    }

    @Override
    public String[] getExcludedActionGroups() {
        return toStringArray(excludedActionGroups);
    }

    @Override
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

    public void setFrameIconPaths(String frameIconPaths) {
        this.frameIconPaths = frameIconPaths;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    public void setAboutImagePath(String aboutImagePath) {
        this.aboutImagePath = aboutImagePath;
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

    @Override
    public String getBuildId() {
        return buildId;
    }

    public void setBuildId(String buildId) {
        this.buildId = buildId;
    }

    @Override
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
     *          if an error occurred during configuration.
     */
    @Override
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
        final String versionKey = SystemUtils.getApplicationContextId() + ".version";
        if (version == null || String.format("${%s}", versionKey).equals(version)) {
            version = System.getProperty(versionKey);
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
        if (frameIconPaths == null) {
            frameIconPaths = "/org/esa/beam/resources/images/icons/BeamIcon24.png";
        }
        if (aboutImagePath == null) {
            aboutImagePath = "/org/esa/beam/resources/images/about.jpg";
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
