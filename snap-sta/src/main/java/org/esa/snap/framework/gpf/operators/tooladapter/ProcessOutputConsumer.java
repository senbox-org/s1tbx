/*
 * Copyright (C) 2014-2015 CS SI
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
 *  with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.framework.gpf.operators.tooladapter;

/**
 * This interface is used to consume the output of a tool, line by line
 *
 * @author Lucian Barbulescu.
 */
interface ProcessOutputConsumer {
    /**
     * Consume a line of output obtained from a tool.
     *
     * @param line a line of output text.
     */
    public void consumeOutput(String line);
}
