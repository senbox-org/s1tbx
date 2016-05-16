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
    private volatile Map<OS, String> values;
    @XStreamOmitField
    private OS currentOS;
    String windows;
    String linux;
    String macosx;
    private boolean isTransient;

    enum OS {
        windows,
        linux,
        macosx,
        unsupported
    }

    public SystemDependentVariable() {
        super();
        initialize();
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
        SystemDependentVariable copy = new SystemDependentVariable();
        copy.setKey(this.getKey());
        copy.setShared(this.isShared());
        copy.setValue(this.getValue());
        copy.setWindows(this.getWindows());
        copy.setLinux(this.getLinux());
        copy.setMacosx(this.getMacosx());
        return copy;
    }

    /**
     * Gets the property value for Windows
     */
    public String getWindows() { return windows == null ? values.get(OS.windows) : windows; }
    /**
     * Sets the property value for Windows
     */
    public void setWindows(String value) {
        this.windows = value;
        if (currentOS == OS.windows) {
            setValue(value);
        }
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
        this.linux = value;
        if (currentOS == OS.linux) {
            setValue(value);
        }
    }
    /**
     * Gets the property value for MacOSX
     */
    public String getMacosx() { return macosx == null ? values.get(OS.macosx) : macosx; }
    /**
     * Sets the property value for MacOSX
     */
    public void setMacosx(String value) {
        this.macosx = value;
        if (currentOS == OS.macosx) {
            setValue(value);
        }
    }

    public void setTransient(boolean value) { this.isTransient = value; }

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
        values.put(currentOS, resolve());
    }

    public String getCurrentOSValue(){
        return values.get(currentOS);
    }
}
