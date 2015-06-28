package org.esa.snap.framework.gpf.descriptor;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import org.esa.snap.framework.gpf.operators.tooladapter.ToolAdapterIO;

import java.util.HashMap;
import java.util.Map;

/**
 * This class encapsulates a property that has OS-dependent values.
 *
 * @author Cosmin Cara
 */
@XStreamAlias("property")
public class OSDependentProperty {

    @XStreamOmitField
    private volatile Map<OS, String> values;
    @XStreamOmitField
    private OS currentOS;
    String name;
    String windows;
    String linux;
    String macosx;

    public enum OS {
        windows,
        linux,
        macosx,
        unsupported
    }

    public OSDependentProperty() {
        initialize();
    }

    public OSDependentProperty(String name, String value) {
        this();
        this.name = name;
        setValue(value);
    }
    /**
     * Gets the property name
     */
    public String getName() { return name; }
    /**
     * Sets the property name
     */
    public void setName(String name) { this.name = name; }
    /**
     * Gets the property value for the current OS
     */
    public String getValue() {
        if (values == null) {
            initialize();
        }
        return values.getOrDefault(currentOS, null);
    }

    /**
     * Sets the property value for the current OS
     */
    public void setValue(String value) {
        values.put(currentOS, value);
        switch (currentOS) {
            case windows:
                this.windows = value;
                break;
            case linux:
                this.linux = value;
                break;
            case macosx:
                this.macosx = value;
                break;
        }
    }
    /**
     * Gets the property value for Windows
     */
    public String getWindows() { return windows == null ? values.get(OS.windows) : windows; }
    /**
     * Sets the property value for Windows
     */
    public void setWindows(String value) {
        windows = value;
        values.put(OS.windows, value);
    }
    /**
     * Gets the property value for Linux
     */
    public String getLinux() {
        return linux == null ? values.get(OS.linux) : linux;
    }
    /**
     * Sets the property value for Linux
     */
    public void setLinux(String value) {
        linux = value;
        values.put(OS.linux, value);
    }
    /**
     * Gets the property value for MacOSX
     */
    public String getMacosx() { return macosx == null ? values.get(OS.macosx) : macosx; }
    /**
     * Sets the property value for MacOSX
     */
    public void setMacosx(String value) {
        macosx = value;
        values.put(OS.macosx, value);
    }
    /**
     * Creates a copy of this instance.
     */
    public OSDependentProperty createCopy() {
        OSDependentProperty copy = new OSDependentProperty();
        copy.setName(this.getName());
        copy.setWindows(this.getWindows());
        copy.setLinux(this.getLinux());
        copy.setMacosx(this.getMacosx());
        return copy;
    }

    private void initialize() {
        values = new HashMap<>();
        try {
            currentOS = Enum.valueOf(OS.class, ToolAdapterIO.getOsFamily());
        } catch (IllegalArgumentException ignored) {
            currentOS = OS.unsupported;
        }
        values.keySet().stream().filter(key -> key != currentOS).forEach(key -> {
            switch (key) {
                case windows:
                    values.put(key, windows == null ? "" : windows);
                    break;
                case linux:
                    values.put(key, linux == null ? "" : linux);
                    break;
                case macosx:
                    values.put(key, macosx == null ? "" : macosx);
                    break;
                case unsupported:
                    values.put(key, "");
                    break;
            }
        });
    }
}
