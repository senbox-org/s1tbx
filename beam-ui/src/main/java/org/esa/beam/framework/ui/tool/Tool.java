/*
 * $Id: Tool.java,v 1.1 2006/10/10 14:47:38 norman Exp $
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
package org.esa.beam.framework.ui.tool;

import java.awt.Cursor;

import org.esa.beam.framework.draw.Drawable;

/**
 * A tool is used to let a user operate on a, usually graphical, view.
 * <p/>
 * <p>Views with an active tool delegate all mouse and keyboard input to the active tool.
 * <p/>
 * <p>The use can activate and deactivate a tool by pressing the associated tool button. In a real-world application,
 * tools are usually mutually exclusive - only one tool can be activated at the same time.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 */
public interface Tool {

    /**
     * An active tool is the currently selected tool in the DrawingView. A tool can be activated/deactivated by calling
     * the activate()/deactivate() method.
     *
     * @return true if the tool is the selected tool in the DrawingView, false otherwise
     *
     * @see #isEnabled
     */
    boolean isActive();

    /**
     * Activates the tool for the given view. This method is called whenever the user switches to this tool. Use this
     * method to reinitialize a tool. Note, a valid view must be present in order for the tool to accept activation.
     */
    void activate();

    /**
     * Deactivates the tool. This method is called whenever the user switches to another tool. Use this method to do
     * some clean-up when the tool is switched.
     * <p/>
     * <p> Subclassers should always call <code>super.deactivate()</code>.
     */
    void deactivate();

    /**
     * Cancels the tool. This method is called whenever the user switches to another tool while this tool is active. Use
     * this method to do some clean-up when the tool is switched.
     * <p/>
     * <p> Subclassers should always call <code>super.cancel()</code>.
     */
    void cancel();

    /**
     * A tool must be enabled in order to use it and to activate/deactivate it. Typically, the program enables or
     * disables a tool.
     *
     * @see #isActive
     */
    boolean isEnabled();

    /**
     * Sets the enabled state of this tool.
     *
     * @param enabled the enabled state
     */
    void setEnabled(boolean enabled);


    /**
     * Determines whether oer not this tool is currently dragging.
     */
    boolean isDragging();

    /**
     * Gets the cursor for this tool.
     *
     * @return the cursor for this tool or <code>null</code> if this tool does not have a special cursor.
     */
    Cursor getCursor();

    /**
     * Gets the editor for this tool. The editor is the image display on which this tool acts.
     *
     * @return the editor, can be <code>null</code>
     */
    DrawingEditor getDrawingEditor();

    /**
     * Sets the editor for this tool. The editor is the image display on which this tool acts.
     *
     * @param drawingEditor the editor, can be <code>null</code>
     */
    void setDrawingEditor(DrawingEditor drawingEditor);

    /**
     * Adds a new tool listener to this tool.
     *
     * @param listener the new listener to be added
     */
    void addToolListener(ToolListener listener);

    /**
     * Removes an existsing tool listener from this tool.
     *
     * @param listener the existsing listener to be removed
     */
    void removeToolListener(ToolListener listener);

    /**
     * Handles a tool input event.
     * @param toolInputEvent a tool input event
     */
    void handleEvent(ToolInputEvent toolInputEvent);

    /**
     * Gets a thing that can be drawn while the tool is working.
     * @return the drawable, or null if this tool cannot be drawn
     */
    Drawable getDrawable();
}

