package org.esa.snap.core.gpf.descriptor.dependency;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import org.esa.snap.core.gpf.descriptor.annotations.Folder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Descriptor class for a dependency bundle.
 * A bundle is a set of binaries of the adapted tool.
 * It can be either in the form of an archive or an executable installer.
 * It is mandatory to specify the location where the bundle will be installed/extracted.
 * A bundle must have an entry point. In the case of an installer/executable, it is the name of the executable.
 * In the case of an archive, it will be the name of the archive.
 * In the case of an uncompressed bundle, it will be the name of the bundle folder.
 *
 * @author  Cosmin Cara
 * @since   5.0.0
 */
@XStreamAlias("dependencyBundle")
public class Bundle {

    private BundleType bundleType;
    @XStreamOmitField
    private File source;
    @Folder
    private File targetLocation;
    private String entryPoint;
    private String arguments;

    public Bundle() {
        this.bundleType = BundleType.NONE;
    }

    public Bundle(BundleType bundleType, File targetLocation, File source) {
        setBundleType(bundleType);
        setTargetLocation(targetLocation);
        setSource(source);
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
     * Returns the source file for this bundle.
     */
    public File getSource() {
        return this.source;
    }

    /**
     * Sets the source for this bundle.
     */
    public void setSource(File source) {
        checkEmpty(source, "source");
        switch (this.bundleType) {
            case ARCHIVE:
                if (!source.isFile() || !source.getName().endsWith(".zip")) {
                    throw new IllegalArgumentException("For achive bundles, the source must be a zip file");
                }
                break;
            case INSTALLER:
                if (!source.isFile()) {
                    throw new IllegalArgumentException("For installer bundles, the source must be a file");
                }
                break;
            default:
                break;
        }
        this.source = source;
        if (this.source != null) {
            setEntryPoint(this.source.getName());
        }
    }

    public boolean isInstalled() {
        boolean installed = false;
        if (this.bundleType != BundleType.NONE) {
            try {
                installed = this.targetLocation != null &&
                        Files.exists(this.targetLocation.toPath()) &&
                        Files.list(this.targetLocation.toPath()).count() > 0;
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

    public void setArguments(String value) {
        this.arguments = value;
    }

    private boolean isFile(String file) {
        return (this.source != null &&
                file != null &&
                Files.isRegularFile(this.source.isDirectory() ?
                        this.source.toPath().resolve(file) :
                        this.source.getParentFile().toPath().resolve(file)));
    }

    private boolean isFolder(String file) {
        return (this.source != null &&
                file != null &&
                Files.isDirectory(this.source.isDirectory() ?
                        this.source.toPath().resolve(file) :
                        this.source.getParentFile().toPath().resolve(file)));
    }

    private void checkEmpty(Object value, String property) {
        if (this.bundleType != BundleType.NONE && (value == null || (value instanceof String && ((String)value).isEmpty()))) {
            throw new IllegalArgumentException(String.format("Property Bundle.%s cannot be empty", property));
        }
    }
}
