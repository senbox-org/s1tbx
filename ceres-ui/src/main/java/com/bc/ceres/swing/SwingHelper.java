package com.bc.ceres.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;

/**
 * Helper class providing some utilities for Swing programming.
 */
public class SwingHelper {

    /**
     * Centers the given component over another component.
     * <p/>
     * <p> The method performs the alignment by setting a newly computed location for the component. It does not alter
     * the component's size.
     *
     * @param comp      the component whose location is to be altered
     * @param alignComp the component used for the alignment of the first component, if <code>null</code> the component
     *                  is ceneterd within the screen area
     *
     * @throws IllegalArgumentException if the component is <code>null</code>
     */
    public static void centerComponent(Component comp, Component alignComp) {

        if (comp == null) {
            throw new IllegalArgumentException("comp must not be null");
        }

        Dimension compSize = comp.getSize();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        int x1, y1;

        if (alignComp != null && !new Rectangle(alignComp.getSize()).isEmpty()) {
            Point alignCompOffs = alignComp.getLocation();
            Dimension alignCompSize = alignComp.getSize();
            x1 = alignCompOffs.x + (alignCompSize.width - compSize.width) / 2;
            y1 = alignCompOffs.y + (alignCompSize.height - compSize.height) / 2;
        } else {
            x1 = (screenSize.width - compSize.width) / 2;
            y1 = (screenSize.height - compSize.height) / 2;
        }

        int x2 = x1 + compSize.width;
        int y2 = y1 + compSize.height;

        if (x2 >= screenSize.width) {
            x1 = screenSize.width - compSize.width - 1;
        }
        if (y2 >= screenSize.height) {
            y1 = screenSize.height - compSize.height - 1;
        }
        if (x1 < 0) {
            x1 = 0;
        }
        if (y1 < 0) {
            y1 = 0;
        }

        comp.setLocation(x1, y1);
    }
}
