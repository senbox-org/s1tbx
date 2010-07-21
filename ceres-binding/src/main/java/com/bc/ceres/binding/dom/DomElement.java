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

package com.bc.ceres.binding.dom;

import com.bc.ceres.core.runtime.ConfigurationElementBase;

public interface DomElement extends ConfigurationElementBase<DomElement> {
    void setParent(DomElement parent);
    int getChildCount();
    DomElement getChild(int index);
    void setAttribute(String name, String value);
    DomElement createChild(String name);
    void addChild(DomElement childElement);
    void setValue(String value);
    String toXml();
}
