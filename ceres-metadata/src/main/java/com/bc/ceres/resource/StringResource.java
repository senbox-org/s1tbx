/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package com.bc.ceres.resource;


/**
 * A [@link Resource} reading its content from a @{link String}.
 */
public class StringResource extends Resource {

    private final String stringContent;

    public StringResource(String path, String stringContent) {
        this(path, stringContent, null);
    }

    public StringResource(String path, String stringContent, Resource origin) {
        super(path, origin);
        this.stringContent = stringContent;
    }

    @Override
    protected String read() {
        return stringContent;
    }

}
