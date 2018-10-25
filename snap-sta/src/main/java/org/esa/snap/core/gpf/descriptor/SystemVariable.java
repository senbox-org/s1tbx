/*
 * Copyright (C) 2014-2015 CS SI
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
 *  with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.core.gpf.descriptor;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.esa.snap.core.gpf.operators.tooladapter.LookupReference;
import org.esa.snap.core.gpf.operators.tooladapter.LookupWithDefaultReference;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterIO;
import org.esa.snap.runtime.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * This class encapsulates an environment (or system) variable
 * that can be passed to a tool adapter operator.
 *
 * @author Ramona Manda
 */
@XStreamAlias("variable")
public class SystemVariable {
    private static final List<LookupReference> lookupReferences;

    static {
        lookupReferences =  new ArrayList<>();
        addLookupReference(System::getenv);
        addLookupReference(System::getProperty);
        final Preferences preferences = Config.instance("s2tbx").preferences();
        addLookupReference(preferences::get);
    }

    public static void addLookupReference(LookupReference reference) {
        lookupReferences.add(reference);
    }

    public static void addLookupReference(LookupWithDefaultReference reference) {
        lookupReferences.add(reference);
    }


    String key;
    String value;
    boolean isShared;

    private SystemVariable() {
        this.key = "";
        this.value = "";
        this.isShared = false;
    }

    public SystemVariable(String key, String value) {
        this.key = key;
        this.value = value;
        this.isShared = false;
    }

    /**
     * Gets the name of the system variable.
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the name of the system variable.
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Gets the value of the system variable
     */
    public String getValue() {
        return resolve();
    }

    /**
     * Sets the value of the system variable
     */
    public void setValue(String value) {
        this.value = value;
        if (this.value != null && !this.value.isEmpty() && this.isShared) {
            ToolAdapterIO.saveVariable(this.key, this.value);
        }
    }

    /**
     * Returns <code>true</code> if the variable is intended to be shared with other adapters.
     */
    public boolean isShared() { return this.isShared; }

    /*
     * Sets the shared status of the variable.
     */
    public void setShared(boolean value) { this.isShared = value; }

    /**
     *  Creates a copy of this SystemVariable instance.
     *
     * @return  A copy of this instance
     */
    public SystemVariable createCopy() {
        SystemVariable newVariable = new SystemVariable();
        newVariable.setKey(this.key);
        newVariable.setValue(this.value);
        newVariable.setShared(this.isShared);
        return newVariable;
    }

    protected String resolve() {
        // first: STA shared variables
        if (this.value == null || this.value.isEmpty()) {
            this.value = ToolAdapterIO.getVariableValue(this.key, null, this.isShared);
        }
        // second: registered lookups
        for (LookupReference reference : lookupReferences) {
            if (this.value == null || this.value.isEmpty()) {
                this.value = reference.apply(this.key);
            } else {
                break;
            }
        }
        return this.value;
    }
}
