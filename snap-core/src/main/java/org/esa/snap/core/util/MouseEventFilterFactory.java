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
package org.esa.snap.core.util;

import javax.swing.Timer;
import javax.swing.event.MouseInputListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

//@todo 1 se/** - add (more) class documentation

public final class MouseEventFilterFactory {

    private static int _timeout = 300; //MouseEventTimeout in milli seconds

    /**
     * Sets the timeout which is used by the filters created by the factory methods.
     *
     * @param timeout the timeout time in milli seconds.
     *
     * @throws IllegalArgumentException if the given timeout time is smaller then 1.
     */
    public static void setTimeout(int timeout) throws IllegalArgumentException {
        Guardian.assertGreaterThan("timeout", timeout, 0);
        _timeout = timeout;
    }

    /**
     * Wrapp the given MouseInputListener with a MouseInputFilter
     *
     * @param mouseInputListener the listener to wrapp
     *
     * @return the wrapped listener or null if the given listener is null
     */
    public static MouseInputListener createFilter(MouseInputListener mouseInputListener) {
        if (mouseInputListener != null) {
            return new MouseInputFilter(_timeout, mouseInputListener);
        }
        return null;
    }

    /**
     * Wrapp the given MouseInputListener with a MouseInputFilter
     *
     * @param mouseListener the listener to wrapp
     *
     * @return the wrapped listener or null if the given listener is null
     */
    public static MouseListener createFilter(MouseListener mouseListener) {
        if (mouseListener != null) {
            return new MouseEventFilter(_timeout, mouseListener);
        }
        return null;
    }

    private static class MouseInputFilter implements MouseInputListener {

        private final MouseInputListener _listener;
        private MouseEventFilter _mouseEventFilter;

        public MouseInputFilter(int timeout, MouseInputListener listener) {
            _mouseEventFilter = new MouseEventFilter(timeout, listener);
            _listener = listener;
        }

        public void mouseClicked(MouseEvent e) {
            _mouseEventFilter.mouseClicked(e);
        }

        public void mousePressed(MouseEvent e) {
            _mouseEventFilter.mousePressed(e);
        }

        public void mouseReleased(MouseEvent e) {
            _mouseEventFilter.mouseReleased(e);
        }

        public void mouseEntered(MouseEvent e) {
            _mouseEventFilter.mouseEntered(e);
        }

        public void mouseExited(MouseEvent e) {
            _mouseEventFilter.mouseExited(e);
        }

        public void mouseDragged(MouseEvent e) {
            if (_listener != null) {
                _listener.mouseDragged(e);
            }
        }

        public void mouseMoved(MouseEvent e) {
            if (_listener != null) {
                _listener.mouseMoved(e);
            }
        }
    }

    private static class MouseEventFilter implements MouseListener {

        private final int _timeout;
        private final MouseListener _listener;
        private MouseEvent _clickEvent;
        private Timer _clickTimer;
//        private MouseEvent _pressedEvent;
//        private Timer _pressedTimer;
//        private MouseEvent _releasedEvent;
//        private Timer _releasedTimer;

        public MouseEventFilter(int timeout, MouseListener listener) {
            _timeout = timeout;
            _listener = listener;
        }

        public void mouseClicked(MouseEvent e) {
            _clickEvent = e;
            startClickTimer();
        }

        public void mousePressed(MouseEvent e) {
//            _pressedEvent = e;
//            startPressedTimer();
            if (_listener != null) {
                _listener.mousePressed(e);
            }
        }

        public void mouseReleased(MouseEvent e) {
//            _releasedEvent = e;
//            startReleasedTimer();
            if (_listener != null) {
                _listener.mouseReleased(e);
            }
        }

        public void mouseEntered(MouseEvent e) {
            if (_listener != null) {
                _listener.mouseEntered(e);
            }
        }

        public void mouseExited(MouseEvent e) {
            if (_listener != null) {
                _listener.mouseExited(e);
            }
        }

        private void startClickTimer() {
            if (_clickTimer == null) {
                _clickTimer = new Timer(_timeout, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        _clickTimer.stop();
                        if (_listener != null && _clickEvent != null) {
                            _listener.mouseClicked(_clickEvent);
                            _clickEvent = null;
                        }
                    }
                });
            }
            if (!_clickTimer.isRunning()) {
                _clickTimer.start();
            }
        }

//        private void startPressedTimer() {
//            if (_pressedTimer == null) {
//                _pressedTimer = new Timer(_timeout, new ActionListener() {
//                    public void actionPerformed(ActionEvent e) {
//                        _pressedTimer.stop();
//                        if (_listener != null && _pressedEvent != null) {
//                            _listener.mousePressed(_pressedEvent);
//                            _pressedEvent = null;
//                        }
//                    }
//                });
//            }
//            if (!_pressedTimer.isRunning()) {
//                _pressedTimer.start();
//            }
//        }
//
//        private void startReleasedTimer() {
//            if (_releasedTimer == null) {
//                _releasedTimer = new Timer(_timeout, new ActionListener() {
//                    public void actionPerformed(ActionEvent e) {
//                        _releasedTimer.stop();
//                        if (_listener != null && _releasedEvent != null) {
//                            _listener.mouseReleased(_releasedEvent);
//                            _releasedEvent = null;
//                        }
//                    }
//                });
//            }
//            if (!_releasedTimer.isRunning()) {
//                _releasedTimer.start();
//            }
//        }
    }
}
