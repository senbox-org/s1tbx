package org.esa.snap.core.gpf.descriptor.dependency;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import org.esa.snap.core.gpf.descriptor.OSFamily;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterIO;
import org.esa.snap.core.util.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    private BundleType bundleType;
    private BundleLocation bundleLocation;
    private String windowsURL;
    private String linuxURL;
    private String macosxURL;
    @XStreamOmitField
    private File source;
    private String arguments;
    private File targetLocation;
    private String entryPoint;
    private String updateVariable;
    @XStreamOmitField
    private OSFamily currentOS;

    public Bundle() {
        this.bundleType = BundleType.NONE;
        setCurrentOS();
    }

    public Bundle(BundleType bundleType, File targetLocation) {
        setBundleType(bundleType);
        setTargetLocation(targetLocation);
        setCurrentOS();
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
    }

    /**
     * Returns the type of the bundle location.
     */
    public BundleLocation getLocation() { return this.bundleLocation; }

    /**
     * Sets the type of the bundle location
     * @param value     The bundle location type
     */
    public void setLocation(BundleLocation value) { this.bundleLocation = value; }

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
    public File getTargetLocation() {
        return this.targetLocation;
    }
    /**
     * Sets the target location of the bundle (i.e. the location where the bundle is supposed to be
     * either extracted or installed).
     */
    public void setTargetLocation(File targetLocation) {
        checkEmpty(targetLocation, "targetLocation");
        this.targetLocation = targetLocation;
    }
    /**
     * Returns the download URL for this bundle.
     */
    public String getDownloadURL() {
        switch (getCurrentOS()) {
            case windows:
                return this.windowsURL;
            case linux:
                return this.linuxURL;
            case macosx:
                return this.macosxURL;
            default:
                throw new IllegalArgumentException("OS not supported");
        }
    }
    /**
     * Sets the download URL for this bundle and for the specified operating system family.
     */
    public void setDownloadURL(OSFamily osFamily, String url) {
        switch (osFamily) {
            case windows:
                this.windowsURL = url;
                break;
            case linux:
                this.linuxURL = url;
                break;
            case macosx:
                this.macosxURL = url;
                break;
            case unsupported:
            default:
                throw new IllegalArgumentException("OS not supported");
        }
    }

    /**
     * Sets the URL for the bundle Windows distribution
     */
    public void setWindowsURL(String url) {
        this.windowsURL = url;
        setEntryPoint(lastSegmentFromUrl(url));
    }
    /**
     * Sets the URL for the bundle Linux distribution
     */
    public void setLinuxURL(String url) {
        this.linuxURL = url;
        setEntryPoint(lastSegmentFromUrl(url));
    }
    /**
     * Sets the URL for the bundle MacOSX distribution
     */
    public void setMacosxURL(String url) {
        this.macosxURL = url;
        setEntryPoint(lastSegmentFromUrl(url));
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
                if (this.targetLocation != null && this.entryPoint != null) {
                    Path path = this.targetLocation.toPath().resolve(FileUtils.getFilenameWithoutExtension(this.entryPoint));
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
    public String getArguments() {
        return this.arguments;
    }

    /**
     * Sets the command line arguments for an installer
     */
    public void setArguments(String value) {
        this.arguments = value;
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

    private String lastSegmentFromUrl(String url) {
        URI uri = URI.create(url);
        Path urlPath = Paths.get(uri.getPath());
        return urlPath.getFileName().toString();
    }

    private OSFamily getCurrentOS() {
        if (currentOS == null) {
            setCurrentOS();
        }
        return currentOS;
    }

    private void setCurrentOS() {
        try {
            currentOS = Enum.valueOf(OSFamily.class, ToolAdapterIO.getOsFamily());
        } catch (IllegalArgumentException ignored) {
            currentOS = OSFamily.unsupported;
        }
    }

    private void checkEmpty(Object value, String property) {
        if (this.bundleType != BundleType.NONE && (value == null || (value instanceof String && ((String)value).isEmpty()))) {
            throw new IllegalArgumentException(String.format("Property Bundle.%s cannot be empty", property));
        }
    }
}
