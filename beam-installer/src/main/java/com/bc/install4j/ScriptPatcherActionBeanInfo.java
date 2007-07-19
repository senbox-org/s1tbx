package com.bc.install4j;

import com.install4j.api.beaninfo.*;
import com.install4j.api.beans.Bean;
import com.bc.install4j.ScriptPatcherAction;

/**
 * BeanInfo for com.bc.install4j.ScriptPatcherAction
 */
public class ScriptPatcherActionBeanInfo extends ActionBeanInfo implements BeanValidator {

    private static final String PROPERTY_SCRIPT_DIR_PATH = "scriptDirPath";
    private static final String PROPERTY_WRITING_LOG = "writingLog";
    private static final String PROPERTY_LOG_FILENAME = "logFilename";

    public ScriptPatcherActionBeanInfo() {
        super("Script patcher action",
              "Replaces installer variables in script files",
              "File operations",
              false, true, null,
              ScriptPatcherAction.class);

        addPropertyDescriptor(Install4JPropertyDescriptor.create(PROPERTY_SCRIPT_DIR_PATH,
                                                                 getBeanClass(),
                                                                 "Script directory",
                                                                 "The directory to search for script files (relative path)."));
        addPropertyDescriptor(Install4JPropertyDescriptor.create(PROPERTY_WRITING_LOG, getBeanClass(),
                                                                 "Writing log file",
                                                                 "Whether or not to write a log file."));
        addPropertyDescriptor(Install4JPropertyDescriptor.create(PROPERTY_LOG_FILENAME, getBeanClass(),
                                                                 "Log filename",
                                                                 "The name of the log file."));
    }

    public void validateBean(Bean bean) throws BeanValidationException {
        checkNotEmpty(PROPERTY_SCRIPT_DIR_PATH, bean);
        checkNotEmpty(PROPERTY_LOG_FILENAME, bean);
    }
}
