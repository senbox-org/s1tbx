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
 * The private implementation package for the {@code binio} API.
 * The package hosts the diverse implementations of the {@link com.bc.ceres.binio.CompoundData} and {@link com.bc.ceres.binio.SequenceData} interfaces.
 *
 * <p>The {@code CompoundData} implementations are:
 * <table>
 * <tr>
 *    <th>Compound instance</th>
 *    <th>All members have known size</th>
 * </tr>
 * <tr>
 *    <td>{@link FixCompound}</td>
 *    <td>Yes</td>
 * </tr>
 * <tr>
 *    <td>{@link VarCompound}</td>
 *    <td>Yes</td>
 * </tr>
 * </table>
 *
 *
 * <p>The {@code SequenceData} implementations are:
 * <table>
 * <tr>
 *    <th>Sequence instance</th>
 *    <th>Element type has known size</th>
 *    <th>Element type is simple</th>
 *    <th>Element count is known</th>
 * </tr>
 * <tr>
 *    <td>{@link FixSequenceOfSimples}</td>
 *    <td>Yes</td>
 *    <td>Yes</td>
 *    <td>Yes</td>
 * </tr>
 * <tr>
 *    <td>{@link VarSequenceOfSimples}</td>
 *    <td>Yes</td>
 *    <td>Yes</td>
 *    <td>No</td>
 * </tr>
 * <tr>
 *    <td>{@link FixSequenceOfFixCollections}</td>
 *    <td>Yes</td>
 *    <td>No</td>
 *    <td>Yes</td>
 * </tr>
 * <tr>
 *    <td>{@link VarSequenceOfFixCollections}</td>
 *    <td>Yes</td>
 *    <td>No</td>
 *    <td>No</td>
 * </tr>
 * <tr>
 *    <td>{@link FixSequenceOfVarCollections}</td>
 *    <td>No</td>
 *    <td>n.a.</td>
 *    <td>Yes</td>
 * </tr>
 * <tr>
 *    <td>{@code VarSequenceOfVarCollections}: not supported</td>
 *    <td>No</td>
 *    <td>n.a.</td>
 *    <td>No</td>
 * </tr>
 * </table>
 */
package com.bc.ceres.binio.internal;