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

/*
 * MultiplePassGenerator.java
 *
 * Created on 25. April 2007, 14:25
 *
 */

package com.bc.ceres.site;

import com.bc.ceres.core.runtime.Module;
import java.io.IOException;
import java.io.PrintWriter;

/**
 *
 */
public class MultiplePassGenerator implements HtmlGenerator {

    private HtmlGenerator[] delegates;
    
    /** Creates a new instance of MultiplePassGenerator */
    public MultiplePassGenerator(HtmlGenerator[] delegates) {
        this.delegates = delegates;
    }

    public void generate(PrintWriter out, Module[] modules, String version) throws IOException {
        for (HtmlGenerator generator : delegates) {
            generator.generate(out, modules, version);
        }
    }
    
}
