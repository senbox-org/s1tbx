/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.core.param;

import org.esa.snap.core.param.editors.BooleanEditor;
import org.esa.snap.core.param.editors.ColorEditor;
import org.esa.snap.core.param.editors.ComboBoxEditor;
import org.esa.snap.core.param.editors.FileEditor;
import org.esa.snap.core.param.editors.ListEditor;
import org.esa.snap.core.param.editors.TextFieldEditor;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.Guardian;

import java.awt.Color;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * The <code>ParamEditorFactory</code> has a single static method which is used to create instances of a
 * <code>Parameter</code>'s editor.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 * @see Parameter
 * @see ParamProperties
 * @see ParamEditor
 * @see ParamValidator
 */
public final class ParamEditorFactory {


    /**
     * Creates a parameter editor instance for the given parameter.
     *
     * @param parameter the parameter
     * @return the parameter editor, never null
     */
    public static ParamEditor createParamEditor(Parameter parameter) {
        Guardian.assertNotNull("parameter", parameter);
        ParamProperties paramProps = parameter.getProperties();
        Debug.assertNotNull(paramProps);

        ParamEditor editor = null;

        // Step 1: Try to create an editor from the 'editorClass' property
        //
        Class editorClass = paramProps.getEditorClass();
        if (editorClass != null) {
            Constructor editorConstructor = null;
            try {
                editorConstructor = editorClass.getConstructor(Parameter.class);
            } catch (NoSuchMethodException e) {
                Debug.trace(e);
            } catch (SecurityException e) {
                Debug.trace(e);
            }
            if (editorConstructor != null) {
                try {
                    editor = (ParamEditor) editorConstructor.newInstance(parameter);
                } catch (InstantiationException e) {
                    Debug.trace(e);
                } catch (IllegalAccessException e) {
                    Debug.trace(e);
                } catch (IllegalArgumentException e) {
                    Debug.trace(e);
                } catch (InvocationTargetException e) {
                    Debug.trace(e);
                }
            }
        }
        if (editor != null) {
            return editor;
        }

        // Step 2: Create a default editor based on the parameter's type info
        //
        if (parameter.isTypeOf(Boolean.class)) {
            editor = new BooleanEditor(parameter);
        } else if (parameter.isTypeOf(Color.class)) {
            editor = new ColorEditor(parameter);
        } else if (parameter.isTypeOf(File.class)) {
            editor = new FileEditor(parameter);
        } else if (paramProps.getValueSet() != null
                   && paramProps.getValueSet().length > 0) {
            if (parameter.isTypeOf(String[].class)) {
                editor = new ListEditor(parameter);
            } else {
                editor = new ComboBoxEditor(parameter);
            }
        }

        // The last choice works always: a text field!
        if (editor == null) {
            editor = new TextFieldEditor(parameter);
        }

        return editor;
    }

}
