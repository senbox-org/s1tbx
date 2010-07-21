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
package org.esa.beam.framework.ui.tool;

import java.util.EventObject;

/**
 * A special event type which is fired by tools to inform tool listeners about state changes of a tool.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 * @deprecated since BEAM 4.7, no replacement
 */
@Deprecated
public class ToolEvent extends EventObject {

    private static final long serialVersionUID = -1684715655211544971L;
    
    public static final int TOOL_ACTIVATED = 0;
    public static final int TOOL_DEACTIVATED = 1;
    public static final int TOOL_ENABLED = 2;
    public static final int TOOL_DISABLED = 3;
    public static final int TOOL_CANCELED = 4;
    public static final int TOOL_FINISHED = 5;

    public static final int TOOL_USER_ID = 10;


    public final int _id;

    /**
     * Constructs a new tool event.
     *
     * @param tool the tool whcih generated this event.
     */
    public ToolEvent(Tool tool, int id) {
        super(tool);
        _id = id;
    }


    /**
     * Returns the tool which generated this event.
     */
    public Tool getTool() {
        return (Tool) getSource();
    }

    /**
     * Gets the event identifier.
     */
    public int getID() {
        return _id;
    }
}
