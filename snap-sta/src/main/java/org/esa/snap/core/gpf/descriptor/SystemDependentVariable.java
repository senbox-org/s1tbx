package org.esa.snap.core.gpf.descriptor;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterIO;

import java.util.HashMap;
import java.util.Map;

/**
 * This class encapsulates a property that has OS-dependent values.
 *
 * @author Cosmin Cara
 */
@XStreamAlias("osvariable")
public class SystemDependentVariable extends SystemVariable {
    @XStreamOmitField
    private volatile Map<OSFamily, String> values;
    @XStreamOmitField
    private OSFamily currentOS;
    private String windows;
    private String linux;
    private String macosx;
    private boolean isTransient;

    private SystemDependentVariable(String key, String value, Map<OSFamily, String> values, OSFamily currentOS,
                                   String windows, String linux, String macosx, boolean isTransient) {
        super(key, value);
        this.currentOS = currentOS;
        this.windows = windows;
        this.linux = linux;
        this.macosx = macosx;
        this.isTransient = isTransient;
        if (values == null) {
            initialize();
        } else {
            this.values = new HashMap<>();
            this.values.putAll(values);
        }
    }

    public SystemDependentVariable(String key, String value) {
        super(key, value);
        initialize();
        setValue(value);
        isTransient = false;
    }

    /**
     * Sets the property value for the current OS
     */
    @Override
    public String getValue() {
        if (values == null) {
            initialize();
        }
        this.value = getCurrentOSValue();
        String retVal = resolve();
        return (retVal != null && !retVal.isEmpty()) ? retVal : values.getOrDefault(currentOS, null);
    }
    /**
     * Gets the property value for the current OS
     */
    @Override
    public void setValue(String value) {
        values.put(currentOS, value);
        this.value = value;

        if (!isTransient && value != null && !value.isEmpty() && this.isShared) {
            ToolAdapterIO.saveVariable(this.key, value);
        }
    }

    @Override
    public SystemVariable createCopy() {
        return new SystemDependentVariable(this.key, this.value, this.values, this.currentOS,
                                           this.windows, this.linux, this.macosx, this.isTransient);
    }

    /**
     * Gets the property value for Windows
     */
    public String getWindows() { return windows == null ? values.get(OSFamily.windows) : windows; }
    /**
     * Sets the property value for Windows
     */
    public void setWindows(String value) {
        this.windows = value;
        if (currentOS == OSFamily.windows) {
            setValue(value);
        }
    }
    /**
     * Gets the property value for Linux
     */
    public String getLinux() {
        return linux == null ? values.get(OSFamily.linux) : linux;
    }
    /**
     * Sets the property value for Linux
     */
    public void setLinux(String value) {
        this.linux = value;
        if (currentOS == OSFamily.linux) {
            setValue(value);
        }
    }
    /**
     * Gets the property value for MacOSX
     */
    public String getMacosx() { return macosx == null ? values.get(OSFamily.macosx) : macosx; }
    /**
     * Sets the property value for MacOSX
     */
    public void setMacosx(String value) {
        this.macosx = value;
        if (currentOS == OSFamily.macosx) {
            setValue(value);
        }
    }

    public void setTransient(boolean value) { this.isTransient = value; }

    public String getCurrentOSValue(){
        return values.get(currentOS);
    }

    private void initialize() {
        values = new HashMap<>();
        values.put(OSFamily.windows, windows == null ? "" : windows);
        values.put(OSFamily.linux, linux == null ? "" : linux);
        values.put(OSFamily.macosx, macosx == null ? "" : macosx);
        values.put(OSFamily.unsupported, "");
        try {
            currentOS = Enum.valueOf(OSFamily.class, ToolAdapterIO.getOsFamily());
        } catch (IllegalArgumentException ignored) {
            currentOS = OSFamily.unsupported;
        }
        //values.put(currentOS, resolve());
    }
}
