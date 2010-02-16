/*
 * $Id: ConfigurationShemaElement.java,v 1.1 2007/03/23 15:39:40 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.bc.ceres.core.runtime;

/**
 * A configuration element of an extension point (shema).
 * <p/>
 * This interface is not intended to be implemented by clients.</p>
 */
public interface ConfigurationSchemaElement extends ConfigurationElementBase<ConfigurationSchemaElement> {

    /**
     * Gets the declaring extension point, if this is an element of an extension point configuration (the shema).
     *
     * @return The declaring extension point, or {@code null} if this is not a shema element.
     */
    ExtensionPoint getDeclaringExtensionPoint();
}