package org.esa.beam.visat.toolviews.cbir;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

/**
 * Listener to allow for iPhone like drag scrolling of a Component within a JScrollPane.
 *
 * @author Greg Cope
 *         <p/>
 *         <p/>
 *         <p/>
 *         This program is free software: you can redistribute it and/or modify
 *         <p/>
 *         it under the terms of the GNU General Public License as published by
 *         <p/>
 *         the Free Software Foundation, either version 3 of the License, or
 *         <p/>
 *         (at your option) any later version.
 *         <p/>
 *         <p/>
 *         <p/>
 *         This program is distributed in the hope that it will be useful,
 *         <p/>
 *         but WITHOUT ANY WARRANTY; without even the implied warranty of
 *         <p/>
 *         MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *         <p/>
 *         GNU General Public License for more details.
 *         <p/>
 *         <p/>
 *         <p/>
 *         You should have received a copy of the GNU General Public License
 *         <p/>
 *         along with this program.  If not, see //www.gnu.org/licenses/>.
 */
public class DragScrollListener implements MouseListener, MouseMotionListener {

    //flags used to turn on/off draggable scrolling directions
    private static final int DRAGABLE_HORIZONTAL_SCROLL_BAR = 1;
    private static final int DRAGABLE_VERTICAL_SCROLL_BAR = 2;

    //defines the intensity of automatic scrolling.
    private int scrollingIntensity = 10;

    //value used to decrease scrolling intensity during animation
    private double damping = 0.05;

    //indicates the number of milliseconds between animation updates.
    private int animationSpeed = 20;

    //Animation timer
    private javax.swing.Timer animationTimer = null;

    //the time of the last mouse drag event
    private long lastDragTime = 0;
    private Point lastDragPoint = null;

    //animation rates
    private double pixelsPerMSX;
    private double pixelsPerMSY;

    //flag which defines the draggable scroll directions
    //private int scrollBarMask = DRAGABLE_HORIZONTAL_SCROLL_BAR | DRAGABLE_VERTICAL_SCROLL_BAR;
    private int scrollBarMask = DRAGABLE_HORIZONTAL_SCROLL_BAR;

    //the draggable component
    private final Component draggableComponent;

    //the JScrollPane containing the component
    private JScrollPane scroller = null;

    //the default cursor
    private Cursor defaultCursor;

    //List of drag speeds used to calculate animation speed
    //Uses the Point2D class to represent speeds rather than locations
    private java.util.List<Point2D> dragSpeeds = new ArrayList<Point2D>();

    public DragScrollListener(Component c) {

        draggableComponent = c;
        defaultCursor = draggableComponent.getCursor();

        draggableComponent.addPropertyChangeListener(new PropertyChangeListener() {


            @Override

            public void propertyChange(PropertyChangeEvent arg0) {

                setScroller();
                defaultCursor = draggableComponent.getCursor();
            }

        });

        setScroller();
    }

    private void setScroller() {

        Component c = getParentScroller(draggableComponent);
        if (c != null) {
            scroller = (JScrollPane) c;
        } else {
            scroller = null;
        }
    }

    /**
     * Sets the Draggable elements - the Horizontal or Vertical Direction. One
     * <p/>
     * can use a bitmasked 'or' (HORIZONTAL_SCROLL_BAR | VERTICAL_SCROLL_BAR ).
     *
     * @param mask One of HORIZONTAL_SCROLL_BAR, VERTICAL_SCROLL_BAR, or HORIZONTAL_SCROLL_BAR | VERTICAL_SCROLL_BAR
     */
    public void setDraggableElements(int mask) {
        scrollBarMask = mask;
    }

    /**
     * Sets the scrolling intensity - the default value being 5. Note, that this has an
     * <p/>
     * inverse relationship to intensity (1 has the biggest difference, higher numbers having
     * <p/>
     * less impact).
     *
     * @param intensity The new intensity value (Note the inverse relationship).
     */
    public void setScrollingIntensity(int intensity) {
        scrollingIntensity = intensity;
    }

    /**
     * Sets how frequently the animation will occur in milliseconds. Default
     * <p/>
     * value is 30 milliseconds. 60+ will get a bit flickery.
     *
     * @param timing The timing, in milliseconds.
     */
    public void setAnimationTiming(int timing) {
        animationSpeed = timing;
    }

    /**
     * Sets the animation damping.
     *
     * @param damping The new value
     */
    public void setDamping(double damping) {
        this.damping = damping;
    }

    /**
     * Empty implementation
     */
    public void mouseEntered(MouseEvent e) {
    }

    /**
     * Empty implementation
     */
    public void mouseExited(MouseEvent e) {
    }

    /**
     * Mouse pressed implementation
     */
    public void mousePressed(MouseEvent e) {

        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }

        draggableComponent.setCursor(new Cursor(Cursor.MOVE_CURSOR));

        dragSpeeds.clear();

        lastDragPoint = e.getPoint();
    }

    /**
     * Mouse released implementation. This determines if further animation
     * <p/>
     * is necessary and launches the appropriate times.
     */
    public void mouseReleased(MouseEvent e) {

        draggableComponent.setCursor(defaultCursor);

        if (scroller == null) {
            return;
        }

        //make sure the mouse ended in a dragging event
        long durationSinceLastDrag = System.currentTimeMillis() - lastDragTime;

        if (durationSinceLastDrag > 20) {
            return;
        }

        //get average speed for last few drags
        pixelsPerMSX = 0;
        pixelsPerMSY = 0;

        int j = 0;
        for (int i = dragSpeeds.size() - 1; i >= 0 && i > dragSpeeds.size() - 6; i--, j++) {
            pixelsPerMSX += dragSpeeds.get(i).getX();
            pixelsPerMSY += dragSpeeds.get(i).getY();
        }

        pixelsPerMSX /= -(double) j;
        pixelsPerMSY /= -(double) j;

        //start the timer
        if (Math.abs(pixelsPerMSX) > 0 || Math.abs(pixelsPerMSY) > 0) {
            animationTimer = new javax.swing.Timer(animationSpeed, new ScrollAnimator());
            animationTimer.start();
        }
    }

    /**
     * Empty implementation
     */

    public void mouseClicked(MouseEvent e) {
    }

    /**
     * MouseDragged implementation. Sets up timing and frame animation.
     */
    public void mouseDragged(MouseEvent e) {

        if (scroller == null) {
            return;
        }

        final Point p = e.getPoint();
        final int diffx = p.x - lastDragPoint.x;
        final int diffy = p.y - lastDragPoint.y;
        lastDragPoint = e.getPoint();

        //scroll the x axis
        if ((scrollBarMask & DRAGABLE_HORIZONTAL_SCROLL_BAR) != 0) {
            getHorizontalScrollBar().setValue(getHorizontalScrollBar().getValue() - diffx);
        }

        //the Scrolling affects mouse locations - offset the last drag point to compensate
        lastDragPoint.x = lastDragPoint.x - diffx;

        //scroll the y axis
        if ((scrollBarMask & DRAGABLE_VERTICAL_SCROLL_BAR) != 0) {
            getVerticalScrollBar().setValue(getVerticalScrollBar().getValue() - diffy);
        }

        //the Scrolling affects mouse locations - offset the last drag point to compensate
        lastDragPoint.y = lastDragPoint.y - diffy;

        //add a drag speed
        dragSpeeds.add(new Point2D.Double((e.getPoint().x - lastDragPoint.x), (e.getPoint().y - lastDragPoint.y)));

        lastDragTime = System.currentTimeMillis();
    }

    /**
     * Empty
     */
    public void mouseMoved(MouseEvent e) {
    }

    /**
     * Private inner class which accomplishes the animation.
     *
     * @author Greg Cope
     */
    private class ScrollAnimator implements ActionListener {
        /**
         * Performs the animation through the setting of the JScrollBar values.
         */
        public void actionPerformed(ActionEvent e) {
            //damp the scrolling intensity
            pixelsPerMSX -= pixelsPerMSX * damping;
            pixelsPerMSY -= pixelsPerMSY * damping;

            //check to see if timer should stop.
            if (Math.abs(pixelsPerMSX) < 0.01 && Math.abs(pixelsPerMSY) < 0.01) {
                animationTimer.stop();
                return;
            }

            //calculate new X value
            int nValX = getHorizontalScrollBar().getValue() + (int) (pixelsPerMSX * scrollingIntensity);
            int nValY = getVerticalScrollBar().getValue() + (int) (pixelsPerMSY * scrollingIntensity);

            //Deal with out of scroll bounds
            if (nValX <= 0) {
                nValX = 0;
            } else if (nValX >= getHorizontalScrollBar().getMaximum()) {
                nValX = getHorizontalScrollBar().getMaximum();
            }

            if (nValY <= 0) {
                nValY = 0;
            } else if (nValY >= getVerticalScrollBar().getMaximum()) {
                nValY = getVerticalScrollBar().getMaximum();
            }

            //Check again to see if timer should stop
            if ((nValX == 0 || nValX == getHorizontalScrollBar().getMaximum()) && Math.abs(pixelsPerMSY) < 1) {
                animationTimer.stop();
                return;
            }

            if ((nValY == 0 || nValY == getVerticalScrollBar().getMaximum()) && Math.abs(pixelsPerMSX) < 1) {
                animationTimer.stop();
                return;
            }

            //Set new values
            if ((scrollBarMask & DRAGABLE_HORIZONTAL_SCROLL_BAR) != 0) {
                getHorizontalScrollBar().setValue(nValX);
            }

            if ((scrollBarMask & DRAGABLE_VERTICAL_SCROLL_BAR) != 0) {
                getVerticalScrollBar().setValue(nValY);
            }
        }
    }

    /**
     * Utility to retrieve the Horizontal Scroll Bar.
     *
     * @return
     */
    private JScrollBar getHorizontalScrollBar() {
        return scroller.getHorizontalScrollBar();
    }

    /**
     * Utility to retrieve the Vertical Scroll Bar
     *
     * @return
     */
    private JScrollBar getVerticalScrollBar() {
        return scroller.getVerticalScrollBar();
    }

    /**
     * @param c
     * @return
     */
    private Component getParentScroller(Component c) {

        Container parent = c.getParent();
        if (parent != null && parent instanceof Component) {

            Component parentC = (Component) parent;
            if (parentC instanceof JScrollPane) {
                return parentC;
            } else {
                return getParentScroller(parentC);
            }
        }

        return null;
    }
}