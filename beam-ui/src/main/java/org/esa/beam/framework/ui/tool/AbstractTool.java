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

import org.esa.beam.framework.draw.Drawable;

import javax.swing.event.EventListenerList;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * An abstract implementation of the tool interface.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 * @deprecated since BEAM 4.7, no replacement
 */
@Deprecated
public abstract class AbstractTool implements Tool, Drawable {

    private boolean _active;
    private boolean _enabled;
    private boolean _canceled;
    private boolean _dragging;
    private DrawingEditor _drawingEditor;
    private EventListenerList _listenerList;

    protected AbstractTool() {
    }

    /**
     * Gets a thing that can be drawn while the tool is working.
     * The default implementation simply returns this, because this <code>AbstractTool</code> is a
     * {@link Drawable}.
     * <P>
     * If your tool does not paint anything, override this method and simply return <code>null</code> here.
     *
     * @return always <code>this</code>
     */
    public Drawable getDrawable() {
        return this;
    }

    /**
     * Draws this <code>Drawable</code> on the given <code>Graphics2D</code> drawing surface.
     * The default implementation is left empty. Override this method in order to perform
     * some drawing while the tool executes.
     *
     * @param g2d the graphics context
     */
    public void draw(Graphics2D g2d) {
    }

    /**
     * Activates the tool for the given view. This method is called whenever the user switches to this tool. Use this
     * method to reinitialize a tool.
     * <p/>
     * <p>The default implementation calls <code>setCanceled(false)</code> followed by <code>setActive(true)</code>.
     * <p/>
     * <p>Subclassers should always call <code>super.activate()</code>.
     */
    public void activate() {
        setCanceled(false);
        setActive(true);
    }

    /**
     * Deactivates the tool. This method is called whenever the user switches to another tool. Use this method to do
     * some clean-up when the tool is switched.
     * <p/>
     * <p>The default implementation cancels this tool if it is active and the calls <code>setActive(false)</code>.
     * <p/>
     * <p>Subclassers should always call <code>super.deactivate()</code>.
     */
    public void deactivate() {
        if (isActive()) {
            cancel();
        }
        setActive(false);
    }

    /**
     * An active tool is the currently selected tool in the DrawingView. A tool can be activated/deactivated by calling
     * the activate()/deactivate() method.
     *
     * @return true if the tool is the selected tool in the DrawingView, false otherwise
     *
     * @see #isEnabled
     */
    public boolean isActive() {
        return _active;
    }

    protected void setActive(boolean active) {
        if (_active == active) {
            return;
        }
        _active = active;
        if (_active) {
            fireToolActivated();
        } else {
            fireToolDeactivated();
        }
    }

    /**
     * A tool must be enabled in order to use it and to activate/deactivate it. Typically, the program enables or
     * disables a tool.
     *
     * @see #isActive
     */
    public boolean isEnabled() {
        return _enabled;
    }

    public void setEnabled(boolean enabled) {
        if (_enabled == enabled) {
            return;
        }
        _enabled = enabled;
        if (_enabled) {
            fireToolEnabled();
        } else {
            fireToolDisabled();
        }
    }

    /**
     * This method can be called by a tool, when it finishes its work. Override this method in order implement the
     * actual work of tool, for example, insert a figure in the drawing.
     * <p/>
     * <p>The default implementation simply calls <code>fireToolFinished()</code>. <p>Subclassers should always call
     * <code>super.finish()</code>.
     */
    protected void finish() {
        fireToolFinished();
        if (getDrawingEditor() != null) {
            getDrawingEditor().repaint();
        }
    }

    /**
     * Cancels the tool. This method is called whenever the user switches to another tool while this tool is active. Use
     * this method to do some clean-up when the tool is switched.
     * <p/>
     * <p>The default implementation simply calls <code>setCanceled(false)</code>.
     * <p/>
     * <p>Subclassers should always call <code>super.cancel()</code>.
     */
    public void cancel() {
        setCanceled(true);
        if (getDrawingEditor() != null) {
            getDrawingEditor().repaint();
        }
    }

    /**
     * A tool must be enabled in order to use it and to activate/deactivate it. Typically, the program enables or
     * disables a tool.
     *
     * @see #isActive
     * @return true, if so
     */
    public boolean isCanceled() {
        return _canceled;
    }

    protected void setCanceled(boolean canceled) {
        if (_canceled == canceled) {
            return;
        }
        _canceled = canceled;
        if (_canceled) {
            fireToolCanceled();
        }
    }

    public boolean isDragging() {
        return _dragging;
    }

    protected void setDragging(boolean dragging) {
        _dragging = dragging;
    }

    /**
     * Gets the editor for this tool. The editor is the image display on which this tool acts.
     *
     * @return the editor, can be <code>null</code>
     */
    public DrawingEditor getDrawingEditor() {
        return _drawingEditor;
    }

    /**
     * Sets the editor for this tool. The editor is the image display on which this tool acts.
     *
     * @param editor the editor, can be <code>null</code>
     */
    public void setDrawingEditor(DrawingEditor editor) {
        DrawingEditor oldEditor = _drawingEditor;
        if (oldEditor == editor) {
            return;
        }
        _drawingEditor = editor;
        if (_drawingEditor == null && isEnabled()) {
            setEnabled(false);
        }
    }

    /**
     * Gets the cursor for this tool. The method simply returns <code>null</code>.
     *
     * @return the cursor for this tool or <code>null</code> if this tool does not have a special cursor.
     */
    public Cursor getCursor() {
        return null;
    }

    /**
     * Invoked when a key has been pressed.
     * @param e The tool input event.
     */
    public void keyPressed(ToolInputEvent e) {
    }

    /**
     * Invoked when a key has been released.
     * @param e The tool input event.
     */
    public void keyReleased(ToolInputEvent e) {
    }

    /**
     * Invoked when a key has been typed. This event occurs when a key press is followed by a key release.
     * @param e The tool input event.
     */
    public void keyTyped(ToolInputEvent e) {
    }

    /**
     * Invoked when the mouse has been clicked on a component.
     * @param e The tool input event.
     */
    public void mouseClicked(ToolInputEvent e) {
    }

    /**
     * Invoked when a mouse button is pressed on a component and then dragged.  Mouse drag events will continue to be
     * delivered to the component where the first originated until the mouse button is released (regardless of whether
     * the mouse position is within the bounds of the component).
     * @param e The tool input event.
     */
    public void mouseDragged(ToolInputEvent e) {
    }

    /**
     * Invoked when the mouse enters a component.
     * @param e The tool input event.
     */
    public void mouseEntered(ToolInputEvent e) {
    }

    /**
     * Invoked when the mouse exits a component.
     * @param e The tool input event.
     */
    public void mouseExited(ToolInputEvent e) {
    }

    /**
     * Invoked when the mouse button has been moved on a component (with no buttons no down).
     * @param e The tool input event.
     */
    public void mouseMoved(ToolInputEvent e) {
    }

    /**
     * Invoked when a mouse button has been pressed on a component.
     * @param e The tool input event.
     */
    public void mousePressed(ToolInputEvent e) {
    }

    /**
     * Invoked when a mouse button has been released on a component.
     * @param e The tool input event.
     */
    public void mouseReleased(ToolInputEvent e) {
    }

    protected static boolean isSingleLeftClick(ToolInputEvent e) {
        final boolean b = isLeftMouseButtonDown(e)
                          && e.getMouseEvent().getClickCount() == 1;
//        System.out.println("isSingleLeftClick = " + b);
        return b;
    }

    protected static boolean isDoubleLeftClick(ToolInputEvent e) {
        final boolean b = isLeftMouseButtonDown(e)
                          && e.getMouseEvent().getClickCount() > 1;
//        System.out.println("isDoubleLeftClick = " + b);
        return b;
    }

//    protected static boolean isLeftMouseButtonReleased(ToolInputEvent e) {
//        return (e.getMouseEvent().getModifiers() & MouseEvent.BUTTON1_MASK) == 0;
//    }

    protected static boolean isLeftMouseButtonDown(ToolInputEvent e) {
        final boolean b = (e.getMouseEvent().getModifiers() & MouseEvent.BUTTON1_MASK) != 0;
//        System.out.println("isLeftMouseButtonDown = " + b);
        return b;
    }

    protected static boolean isShiftKeyDown(ToolInputEvent e) {
        return (e.getMouseEvent().getModifiers() & MouseEvent.SHIFT_MASK) != 0;
    }

    protected static boolean isControlKeyDown(ToolInputEvent e) {
        return (e.getMouseEvent().getModifiers() & MouseEvent.CTRL_MASK) != 0;
    }

    protected static boolean isAltKeyDown(ToolInputEvent e) {
        return (e.getMouseEvent().getModifiers() & MouseEvent.ALT_MASK) != 0;
    }

    /**
     * Adds a new tool listener to this tool.
     *
     * @param listener the new listener to be added
     */
    public void addToolListener(ToolListener listener) {
        if (listener != null) {
            if (_listenerList == null) {
                _listenerList = new EventListenerList();
            }
            _listenerList.add(ToolListener.class, listener);
        }
    }

    /**
     * Removes an existsing tool listener from this tool.
     *
     * @param listener the existsing listener to be removed
     */
    public void removeToolListener(ToolListener listener) {
        if (listener != null && _listenerList != null) {
            _listenerList.remove(ToolListener.class, listener);
        }
    }


    protected void fireToolActivated() {
        fireToolEvent(ToolEvent.TOOL_ACTIVATED);
    }

    protected void fireToolDeactivated() {
        fireToolEvent(ToolEvent.TOOL_DEACTIVATED);
    }

    protected void fireToolEnabled() {
        fireToolEvent(ToolEvent.TOOL_ENABLED);
    }

    protected void fireToolDisabled() {
        fireToolEvent(ToolEvent.TOOL_DISABLED);
    }

    protected void fireToolCanceled() {
        fireToolEvent(ToolEvent.TOOL_CANCELED);
    }

    protected void fireToolFinished() {
        fireToolEvent(ToolEvent.TOOL_FINISHED);
    }

    private void fireToolEvent(int eventID) {
        if (_listenerList == null) {
            return;
        }
        ToolEvent event = null;
        // Guaranteed to return a non-null array
        Object[] listeners = _listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ToolListener.class) {
                // Lazily create the event:
                if (event == null) {
                    event = new ToolEvent(this, eventID);
                }
                fireToolEvent((ToolListener) listeners[i + 1], event);
            }
        }
    }

    private void fireToolEvent(ToolListener toolListener, ToolEvent event) {
        switch (event.getID()) {
        case ToolEvent.TOOL_ACTIVATED:
            toolListener.toolActivated(event);
            break;
        case ToolEvent.TOOL_DEACTIVATED:
            toolListener.toolDeactivated(event);
            break;
        case ToolEvent.TOOL_ENABLED:
            toolListener.toolEnabled(event);
            break;
        case ToolEvent.TOOL_DISABLED:
            toolListener.toolDisabled(event);
            break;
        case ToolEvent.TOOL_CANCELED:
            toolListener.toolCanceled(event);
            break;
        case ToolEvent.TOOL_FINISHED:
            toolListener.toolFinished(event);
            break;
        }
    }

    public void handleEvent(ToolInputEvent e) {
        MouseEvent me = e.getMouseEvent();
        if (me != null) {
            if (me.getID() == MouseEvent.MOUSE_ENTERED) {
                mouseEntered(e);
            } else if (me.getID() == MouseEvent.MOUSE_EXITED) {
                mouseExited(e);
            } else if (me.getID() == MouseEvent.MOUSE_MOVED) {
                mouseMoved(e);
            } else if (me.getID() == MouseEvent.MOUSE_DRAGGED) {
                mouseDragged(e);
            } else if (me.getID() == MouseEvent.MOUSE_RELEASED) {
                mouseReleased(e);
            } else if (me.getID() == MouseEvent.MOUSE_PRESSED) {
                mousePressed(e);
            } else if (me.getID() == MouseEvent.MOUSE_CLICKED) {
                mouseClicked(e);
            }
        }
        KeyEvent ke = e.getKeyEvent();
        if (ke != null) {
            if (ke.getID() == KeyEvent.KEY_PRESSED) {
                keyPressed(e);
            } else if (ke.getID() == KeyEvent.KEY_RELEASED) {
                keyReleased(e);
            } else if (ke.getID() == KeyEvent.KEY_TYPED) {
                keyTyped(e);
            }
        }
    }
}
