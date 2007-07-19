package com.bc.install4j;

import com.install4j.api.beaninfo.*;
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

    public void validateBean(Bean bean) throws BeanValidationException {
        checkNotEmpty(PROPERTY_SCRIPT_DIR_PATH, bean);
    }
}
