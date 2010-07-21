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

package com.bc.ceres.compiler;

import javax.tools.SimpleJavaFileObject;
import java.net.URI;

/**
 * A file object used to represent Java source coming from a string.
 */
public class Code extends SimpleJavaFileObject {
    /**
     * The source code of this "file".
     */
    private final String className;

    /**
     * The source code of this "file".
     */
    private final String code;

    /**
     * Constructs a new JavaCode file object.
     *
     * @param className the fully qualified class name
     * @param code      the source code for the compilation unit represented by this file object
     */
    public Code(String className, String code) {
        super(toURI(className), Kind.SOURCE);
        this.className = className;
        this.code = code;
    }

    public String getClassName() {
        return className;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return code;
    }

    private static URI toURI(String className) {
        return URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension);
    }
}