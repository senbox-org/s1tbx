/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package com.bc.install4j;

import com.install4j.api.beaninfo.ActionBeanInfo;
import com.install4j.api.beaninfo.BeanValidationException;
import com.install4j.api.beaninfo.BeanValidator;
import com.install4j.api.beaninfo.Install4JPropertyDescriptor;
import com.install4j.api.beans.Bean;

/**
 * BeanInfo for com.bc.install4j.ScriptPatcherAction
 */
public class ScriptPatcherActionBeanInfo extends ActionBeanInfo implements BeanValidator {

    private static final String PROPERTY_SCRIPT_DIR_PATH = "scriptDirPath";

    public ScriptPatcherActionBeanInfo() {
        super("Script patcher action",
              "Replaces installer variables in script files",
              "BEAM actions",
              true, false, null,
              com.bc.install4j.ScriptPatcherAction.class);

        addPropertyDescriptor(Install4JPropertyDescriptor.create(PROPERTY_SCRIPT_DIR_PATH,
                                                                 getBeanClass(),
                                                                 "Script directory",
                                                                 "The directory to search for script files (relative path)."));
    }

    @Override
    public void validateBean(Bean bean) throws BeanValidationException {
        checkNotEmpty(PROPERTY_SCRIPT_DIR_PATH, bean);
    }
}
