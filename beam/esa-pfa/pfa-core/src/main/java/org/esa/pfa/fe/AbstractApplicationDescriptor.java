/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.pfa.fe;

import java.awt.*;

/**

 */
public abstract class AbstractApplicationDescriptor implements PFAApplicationDescriptor {

    private String name;

    protected AbstractApplicationDescriptor(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Gets the width and height of the patch segmentation.
     *
     * @return the  dimension
     */
    public abstract Dimension getPatchDimension();

}
