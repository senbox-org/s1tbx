/*
 *
 *  * Copyright (C) 2016 CS ROMANIA
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  *  with this program; if not, see http://www.gnu.org/licenses/
 *
 */
package org.esa.snap.core.gpf.descriptor.dependency;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import org.esa.snap.core.gpf.descriptor.OSFamily;
import org.esa.snap.core.gpf.descriptor.TemplateParameterDescriptor;
import org.esa.snap.core.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.core.gpf.descriptor.ToolParameterDescriptor;
import org.esa.snap.core.gpf.descriptor.template.MemoryTemplate;
import org.esa.snap.core.gpf.descriptor.template.Template;
import org.esa.snap.core.gpf.descriptor.template.TemplateEngine;
import org.esa.snap.core.gpf.descriptor.template.TemplateException;
import org.esa.snap.core.gpf.descriptor.template.TemplateType;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterConstants;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterIO;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterOp;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Descriptor class for a dependency bundle.
 * A bundle is a set of binaries of the adapted tool.
 * It can be either in the form of an archive or an executable installer.
 * It is mandatory to specify the location where the bundle will be installed/extracted.
 * A bundle must have an entry point. In the case of an installer/executable, it is the name of the executable.
 * In the case of an archive, it will be the name of the archive.
 * The source of the bundle can be either local (i.e. from the local filesystem)
 * or remote (i.e. from an URL)
 *
 * @author  Cosmin Cara
 */
@XStreamAlias("dependencyBundle")
public class Bundle {

    @XStreamOmitField
    private ToolAdapterOperatorDescriptor parent;
    private BundleType bundleType;
    private BundleLocation bundleLocation;
    private String downloadURL;
    //@XStreamOmitField
    private File source;
    private TemplateParameterDescriptor templateparameter;
    private String targetLocation;
    private String entryPoint;
    private String updateVariable;
    @XStreamOmitField
    private OSFamily operatingSystem;

    private static Bundle initializeBundle(OSFamily osFamily) {
        Bundle bundle = new Bundle(new ToolAdapterOperatorDescriptor("bundle", ToolAdapterOp.class),
                        BundleType.NONE,
                        SystemUtils.getAuxDataPath().toString());
        bundle.setOS(osFamily);
        return bundle;
    }

    public static OSFamily getCurrentOS() {
        return Enum.valueOf(OSFamily.class, ToolAdapterIO.getOsFamily());
    }

    public Bundle() {
        this.bundleType = BundleType.NONE;
        this.bundleLocation = BundleLocation.REMOTE;
        initializeBundle(OSFamily.windows);
        initializeBundle(OSFamily.linux);
        initializeBundle(OSFamily.macosx);
    }

    public Bundle(ToolAdapterOperatorDescriptor descriptor, BundleType bundleType, String targetLocation) {
        this.bundleType = BundleType.NONE;
        this.parent = descriptor;
        this.bundleLocation = BundleLocation.REMOTE;
        setBundleType(bundleType);
        setTargetLocation(targetLocation);
        this.operatingSystem = Bundle.getCurrentOS();
    }

    public Bundle(Bundle original) {
        this.parent = original.parent;
        this.bundleType = original.bundleType;
        this.bundleLocation = original.bundleLocation;
        this.downloadURL = original.downloadURL;
        this.source = original.source;
        this.templateparameter = original.templateparameter;
        this.targetLocation = original.targetLocation != null ?
                original.targetLocation.toString().replace("\\", "/") :
                null;
        this.entryPoint = original.entryPoint;
        this.updateVariable = original.updateVariable;
        this.operatingSystem = original.operatingSystem;
    }

    public ToolAdapterOperatorDescriptor getParent() { return this.parent; }

    public void setParent(ToolAdapterOperatorDescriptor descriptor) {
        this.parent = descriptor;
    }

    /**
     * Returns the type of the bundle provided with the adapter.
     * Can be one of {@link BundleType} members.
     */
    public BundleType getBundleType() {
        return this.bundleType;
    }
    /**
     * Sets the bundle type of the adapter.
     * @param bundleType    The type of the bundle (see {@link BundleType})
     */
    public void setBundleType(BundleType bundleType) {
        this.bundleType = bundleType;
        if (this.bundleType != BundleType.NONE) {
            initArgumentsParameter();
        }
    }

    /**
     * Returns the type of the bundle location.
     */
    public BundleLocation getLocation() { return this.bundleLocation; }

    /**
     * Sets the type of the bundle location
     * @param value     The bundle location type
     */
    public void setLocation(BundleLocation value) {
        this.bundleLocation = value;
        if(this.bundleLocation.equals(BundleLocation.REMOTE)){
            setSource(null);
        } else {
            setDownloadURL(null);
        }
    }

    /**
     * Returns the local source of the bundle
     */
    public File getSource() { return this.source; }

    /**
     * Sets the local source of the bundle
     * @param value     The file representing an archive or an installer
     */
    public void setSource(File value) {
        this.source = value;
        if (value != null) {
            setEntryPoint(value.getName());
        }
    }
    /**
     * Returns the target location of the bundle (i.e. the location where the bundle is supposed to be
     * either extracted or installed).
     */
    public String getTargetLocation() {
        return this.targetLocation;
    }
    /**
     * Sets the target location of the bundle (i.e. the location where the bundle is supposed to be
     * either extracted or installed).
     */
    public void setTargetLocation(String targetLocation) {
        checkEmpty(targetLocation, "targetLocation");
        this.targetLocation = targetLocation;
        /*if (this.targetLocation != null) {
            setEntryPoint(Paths.get(this.targetLocation).getFileName().toString());
        }*/
    }
    /**
     * Returns the download URL for this bundle.
     */
    public String getDownloadURL() { return this.downloadURL; }
    /**
     * Sets the download URL for this bundle.
     */
    public void setDownloadURL(String url) {
        this.downloadURL = url;
        if(url != null && url.length() > 0) {
            setEntryPoint(lastSegmentFromUrl(url));
        }
    }
    /**
     * Checks if the bundle location is local or remote.
     */
    public boolean isLocal() {
        return this.bundleLocation == BundleLocation.LOCAL;
    }

    /**
     * Checks if the bundle binaries have been installed.
     */
    public boolean isInstalled() {
        boolean installed = false;
        if (this.bundleType != BundleType.NONE) {
            try {
                if (this.targetLocation != null && this.entryPoint != null && this.parent != null) {
                    Path path = this.parent.resolveVariables(this.targetLocation).toPath()
                                    .resolve(FileUtils.getFilenameWithoutExtension(this.entryPoint));
                    installed = Files.exists(path) && Files.list(path).count() > 0;
                }
            } catch (IOException ignored) { }
        }
        return installed;
    }
    /**
     * Returns the entry point of the bundle.
     */
    public String getEntryPoint() {
        return this.entryPoint;
    }
    /**
     * Sets the entry point of the bundle.
     */
    private void setEntryPoint(String entryPoint) {
        this.entryPoint = entryPoint;
    }
    /**
     * Returns the command line arguments for an installer, if any
     */
    public TemplateParameterDescriptor getArgumentsParameter() {
        initArgumentsParameter();
        return this.templateparameter;
    }

    /**
     * Sets the command line arguments for an installer
     */
    public void setArgumentsParameter(TemplateParameterDescriptor value) {
        this.templateparameter = value;
    }

    /**
     * Gets the name of the System Variable to be updated after bundle installation.
     * This variable will have as value the location in which the bundle was installed.
     */
    public String getUpdateVariable() { return this.updateVariable; }

    /**
     * Sets the name of the System Variable to be updated after bundle installation.
     */
    public void setUpdateVariable(String name) { this.updateVariable = name; }

    public OSFamily getOS() { return operatingSystem; }

    public void setOS(OSFamily operatingSystem) { this.operatingSystem = operatingSystem; }

    public String getCommandLine() {
        String cmdLine = null;
        if (this.templateparameter != null && this.parent != null) {
            try {
                TemplateEngine templateEngine = this.parent.getTemplateEngine();
                this.templateparameter.setTemplateEngine(templateEngine);
                Map<String, Object> params = new HashMap<String, Object>() {{
                    put("targetLocation", Bundle.this.parent.resolveVariables(targetLocation));
                }};
                cmdLine = templateEngine.execute(this.templateparameter.getTemplate(), params);
            } catch (TemplateException e) {
                SystemUtils.LOG.warning(e.getMessage());
            }
        }
        return cmdLine == null || "null".equals(cmdLine) ? "" : cmdLine;
    }

    String[] getCommandLineArguments() {
        String[] args = null;
        String output = getCommandLine();
        if (!StringUtils.isNullOrEmpty(output)) {
            args = output.replace("\r","")
                    .replace("\t","").split("\n");
            for (int i = 0; i < args.length; i++) {
                args[i] = args[i].trim();
            }
        }
        return args;
    }

    private void initArgumentsParameter() {
        if (this.templateparameter == null || this.templateparameter.getName() == null) {
            this.templateparameter = new TemplateParameterDescriptor("arguments", File.class);
            this.templateparameter.setParameterType(ToolAdapterConstants.TEMPLATE_PARAM_MASK);
            Template template = new MemoryTemplate(TemplateType.VELOCITY);
            this.templateparameter.setOutputFile(new File(template.getName()));
            ToolParameterDescriptor parameterDescriptor = new ToolParameterDescriptor("targetLocation", File.class);
            parameterDescriptor.setDefaultValue(String.valueOf(this.targetLocation));
            this.templateparameter.addParameterDescriptor(parameterDescriptor);
            if (this.parent != null) {
                try {
                    TemplateEngine templateEngine = this.parent.getTemplateEngine();
                    template.associateWith(templateEngine);
                    this.templateparameter.setTemplateEngine(templateEngine);
                    this.templateparameter.setTemplate(template);
                } catch (TemplateException e) {
                    SystemUtils.LOG.warning(e.getMessage());
                }
            }
        }
    }

    private String lastSegmentFromUrl(String url) {
        URI uri = URI.create(url);
        Path urlPath = Paths.get(uri.getPath());
        return urlPath.getFileName().toString();
    }

    private void checkEmpty(Object value, String property) {
        if (this.bundleType != BundleType.NONE && (value == null || (value instanceof String && ((String)value).isEmpty()))) {
            throw new IllegalArgumentException(String.format("Property Bundle.%s cannot be empty", property));
        }
    }
}
