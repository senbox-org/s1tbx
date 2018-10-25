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
package org.esa.snap.core.gpf.operators.tooladapter;

import java.io.File;

/**
 * Holder interface for tool adapter constants.
 *
 * @author Lucian Barbulescu.
 */
public interface ToolAdapterConstants {

    String OPERATOR_NAMESPACE = "org.esa.snap.core.gpf.operators.tooladapter.";
    String DESCRIPTOR_FILE = "META-INF" + File.separator + "descriptor.xml";
    /**
     * The id of the tool's source product.
     */
    String TOOL_SOURCE_PRODUCT_ID = "sourceProduct";
    String TOOL_TARGET_PRODUCT_ID = "targetProduct";
    String TOOL_SOURCE_PRODUCT_FILE = "sourceProductFile";
    String TOOL_TARGET_PRODUCT_FILE = "targetProductFile";
    /**
     * The id of the tool's target file as it is used in the descriptor.
     */
    String OPERATOR_GENERATED_NAME_SEPARATOR = "_";
    String OPERATOR_TEMP_FILES_SEPARATOR = "_";
    String TOOL_VELO_TEMPLATE_SUFIX = "-template.vm";

    String TEMPLATE_PARAM_MASK = "TemplateParameter";
    String TEMPLATE_BEFORE_MASK = "TemplateBeforeExecution";
    String TEMPLATE_AFTER_MASK = "TemplateAfterExecution";
    String REGULAR_PARAM_MASK = "RegularParameter";
    String FOLDER_PARAM_MASK = "FolderParameter";

    String DEFAULT_PARAM_NAME = "DefaultParameter";
    String MAIN_TOOL_FILE_LOCATION = "mainToolFileLocation";
    String WORKING_DIR = "workingDir";
    String TEMPLATE_TYPE = "templateType";
    String PROGRESS_PATTERN = "progressPattern";
    String ERROR_PATTERN = "errorPattern";
    String STEP_PATTERN = "stepPattern";
    String MENU_LOCATION = "menuLocation";
    String DESCRIPTION = "description";
    String AUTHORS = "authors";
    String COPYRIGHT = "copyright";
    String VERSION = "version";
    String LABEL = "label";
    String NAME = "name";
    String ALIAS = "alias";
    String NOT_NULL = "NotNull";
    String NOT_EMPTY = "NotEmpty";
    String HANDLE_OUTPUT = "isHandlingOutputName";
    String PROCESSING_WRITER = "processingWriter";
    String PREPROCESSOR_EXTERNAL_TOOL = "preprocessorExternalTool";
    String USER_MODULE_PATH = "user.module.path";
    String BUNDLE = "bundle";
}
