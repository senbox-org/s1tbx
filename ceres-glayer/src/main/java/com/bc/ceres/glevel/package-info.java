/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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
 * Adds multi-resolution / image pyramid capabilities to JAI.
 *
 * The framework has been designed taking into account the following requirements:
 * <ul>
 *   <li>A multi-resolution image ({@link com.bc.ceres.glevel.MultiLevelImage}) shall manage its lower-resolution instances such that
 *       the same lower-resolution image instance is returned for the same level.
 *   </li>
 *   <li>It should be possible to add the multi-resolution capability to any existing {@code RenderedImage} (see {@link com.bc.ceres.glevel.support.DefaultMultiLevelSource}).
 *   </li>
 *   <li>Classes implementing the multi-resolution capability may use any JAI {@code OpImage} DAG to produce its tiles.
 *        It should be easy to implement the multi-resolution capability (see {@link com.bc.ceres.glevel.support.DefaultMultiLevelImage}). Tile computation
 *        shall then directly take into account the resolution level.
 *   </li>
 * </ul>
 *
 * @author Norman Fomferra
 */
package com.bc.ceres.glevel;

