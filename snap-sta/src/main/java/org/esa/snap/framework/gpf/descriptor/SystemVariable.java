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
package org.esa.snap.framework.gpf.descriptor;

/**
 * This class encapsulates an environment (or system) variable
 * that can be passed to a tool adapter operator.
 *
 * @author Ramona Manda
 */
public class SystemVariable {
    String key;
    String value;

    SystemVariable() {
        this.key = "";
        this.value = "";
    }

    public SystemVariable(String key, String value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Gets the name of the system variable.
     * @return
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the name of the system variable.
     * @param key
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Gets the value of the system variable
     * @return
     */
    public String getValue() {
        return resolve();
    }

    /**
     * Sets the value of the system variable
     * @param value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     *  Creates a copy of this SystemVariable instance.
     *
     * @return  A copy of this instance
     */
    public SystemVariable createCopy() {
        SystemVariable newVariable = new SystemVariable();
        newVariable.setKey(this.key);
        newVariable.setValue(this.value);
        return newVariable;
    }

    protected String resolve() {
        String existingValue = System.getenv(this.key);
        if (existingValue == null || existingValue.isEmpty()) {
            existingValue = value;
        }
        return existingValue;
    }
}
