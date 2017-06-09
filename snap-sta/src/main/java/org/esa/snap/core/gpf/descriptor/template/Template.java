/*
 *
 *  * Copyright (C) 2016 CS ROMANIA
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  *  with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.snap.core.gpf.descriptor.template;

import java.io.File;
import java.io.IOException;

/**
 * @author Cosmin Cara
 */
public interface Template {

    String getName();
    void setName(String value);
    void associateWith(TemplateEngine engine) throws TemplateException;
    String getContents() throws IOException;
    void setContents(String text, boolean shouldParse) throws TemplateException;
    TemplateType getType();
    void setType(TemplateType value);
    boolean isInMemory();
    Template copy() throws IOException;
    void save() throws IOException;
    File getPath();

}
