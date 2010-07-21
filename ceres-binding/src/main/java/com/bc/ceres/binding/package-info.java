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

/**
 * The main purpose of this package is to provide a simple data binding framework for binding
 * Java objects to a GUI or a DOM.
 * <p>Basically it also provides support for the <i>Property List</i> and <i>Value Object</i>
 * design patterns (see {@link PropertyContainer} and {@link Property}).</p>
 * <p>This package and its sub-packages currently form a self-standing API which is
 * not used by <i>Ceres</i> itself.</p>
 * <p>An important entry point into this API may be the {@link PropertyContainer} class, which
 * is used to manage a collection {@link Property}s whose values may originate from
 * diverse sources.</p>
 *
 * @since Ceres 0.6
 */
package com.bc.ceres.binding;
