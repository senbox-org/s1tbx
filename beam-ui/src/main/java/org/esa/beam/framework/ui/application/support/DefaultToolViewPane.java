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

package org.esa.beam.framework.ui.application.support;

import com.jidesoft.docking.DockContext;
import com.jidesoft.docking.DockableFrame;
import com.jidesoft.docking.event.DockableFrameAdapter;
import com.jidesoft.docking.event.DockableFrameEvent;
import org.esa.beam.framework.ui.application.PageComponent;
import org.esa.beam.framework.ui.application.ToolViewDescriptor;
import org.esa.beam.util.Debug;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;

/**
 * Uses a {@link DockableFrame} as control.
 */
public class DefaultToolViewPane extends AbstractPageComponentPane {
    private DockableFrame dockableFrame;
    private boolean pageComponentControlCreated;

    public DefaultToolViewPane(PageComponent pageComponent) {
        super(pageComponent);
    }

    @Override
    protected JComponent createControl() {
        dockableFrame = new DockableFrame();
        dockableFrame.setKey(getPageComponent().getId());
        configureControl(true);
        dockableFrame.addDockableFrameListener(new DockableFrameHandler());
        nameComponent(dockableFrame, "Pane");
        return dockableFrame;
    }

    @Override
    protected void pageComponentChanged(PropertyChangeEvent evt) {
        configureControl(false);
    }

    private void configureControl(boolean init) {
        ToolViewDescriptor toolViewDescriptor = (ToolViewDescriptor) getPageComponent().getDescriptor();

        dockableFrame.setTitle(toolViewDescriptor.getTitle());
        dockableFrame.setTabTitle(toolViewDescriptor.getTabTitle());
        dockableFrame.setFrameIcon(toolViewDescriptor.getSmallIcon());
        dockableFrame.setToolTipText(toolViewDescriptor.getDescription());

        if (init) {
            if (toolViewDescriptor.getFloatingBounds() != null) {
                dockableFrame.setUndockedBounds(toolViewDescriptor.getFloatingBounds());
            }

            if (toolViewDescriptor.getDockedWidth() > 0) {
                dockableFrame.getContext().setDockedWidth(toolViewDescriptor.getDockedWidth());
            }
            if (toolViewDescriptor.getDockedHeight() > 0) {
                dockableFrame.getContext().setDockedHeight(toolViewDescriptor.getDockedHeight());
            }
            if (toolViewDescriptor.getInitIndex() >= 0) {
                dockableFrame.getContext().setInitIndex(toolViewDescriptor.getInitIndex());
            }
            if (toolViewDescriptor.getInitSide() != null) {
                dockableFrame.getContext().setInitSide(toJideSide(toolViewDescriptor.getInitSide()));
            }
            if (toolViewDescriptor.getInitState() != null) {
                dockableFrame.getContext().setInitMode(toJideMode(toolViewDescriptor.getInitState()));
            }
        }
    }

//    private ToolViewDescriptor.State toState(int jideState) {
//        if (jideState == DockContext.STATE_FRAMEDOCKED) {
//            return ToolViewDescriptor.State.DOCKED;
//        } else if (jideState == DockContext.STATE_FLOATING) {
//            return ToolViewDescriptor.State.FLOATING;
//        } else if (jideState == DockContext.STATE_AUTOHIDE) {
//            return ToolViewDescriptor.State.ICONIFIED;
//        } else if (jideState == DockContext.STATE_AUTOHIDE_SHOWING) {
//            return ToolViewDescriptor.State.ICONIFIED_SHOWING;
//        } else if (jideState == DockContext.STATE_HIDDEN) {
//            return ToolViewDescriptor.State.HIDDEN;
//        }
//        return ToolViewDescriptor.State.UNKNOWN;
//    }

    private int toJideMode(ToolViewDescriptor.State state) {
        if (state == ToolViewDescriptor.State.DOCKED) {
            return DockContext.STATE_FRAMEDOCKED;
        } else if (state == ToolViewDescriptor.State.FLOATING) {
            return DockContext.STATE_FLOATING;
        } else if (state == ToolViewDescriptor.State.ICONIFIED) {
            return DockContext.STATE_AUTOHIDE;
        } else if (state == ToolViewDescriptor.State.ICONIFIED_SHOWING) {
            return DockContext.STATE_AUTOHIDE_SHOWING;
        } else if (state == ToolViewDescriptor.State.HIDDEN) {
            return DockContext.STATE_HIDDEN;
        }
        throw new IllegalStateException("unhandled " + ToolViewDescriptor.State.class);
    }

//    private ToolViewDescriptor.DockSide toSide(int jideSide) {
//        if (jideSide == DockContext.DOCK_SIDE_ALL) {
//            return ToolViewDescriptor.DockSide.ALL;
//        } else if (jideSide == DockContext.DOCK_SIDE_CENTER) {
//            return ToolViewDescriptor.DockSide.CENTER;
//        } else if (jideSide == DockContext.DOCK_SIDE_WEST) {
//            return ToolViewDescriptor.DockSide.WEST;
//        } else if (jideSide == DockContext.DOCK_SIDE_EAST) {
//            return ToolViewDescriptor.DockSide.EAST;
//        } else if (jideSide == DockContext.DOCK_SIDE_NORTH) {
//            return ToolViewDescriptor.DockSide.NORTH;
//        } else if (jideSide == DockContext.DOCK_SIDE_SOUTH) {
//            return ToolViewDescriptor.DockSide.SOUTH;
//        }
//        return ToolViewDescriptor.DockSide.UNKNOWN;
//    }

    private int toJideSide(ToolViewDescriptor.DockSide dockSide) {
        if (dockSide == ToolViewDescriptor.DockSide.ALL) {
            return DockContext.DOCK_SIDE_ALL;
        } else if (dockSide == ToolViewDescriptor.DockSide.CENTER) {
            return DockContext.DOCK_SIDE_CENTER;
        } else if (dockSide == ToolViewDescriptor.DockSide.WEST) {
            return DockContext.DOCK_SIDE_WEST;
        } else if (dockSide == ToolViewDescriptor.DockSide.EAST) {
            return DockContext.DOCK_SIDE_EAST;
        } else if (dockSide == ToolViewDescriptor.DockSide.NORTH) {
            return DockContext.DOCK_SIDE_NORTH;
        } else if (dockSide == ToolViewDescriptor.DockSide.SOUTH) {
            return DockContext.DOCK_SIDE_SOUTH;
        }
        throw new IllegalStateException("unhandled " + ToolViewDescriptor.DockSide.class);
    }

    private void ensurePageComponentControlCreated() {
        if (!pageComponentControlCreated) {
            Debug.trace("Creating control for page component " + getPageComponent().getId());
            JComponent pageComponentControl;
            try {
                pageComponentControl = getPageComponent().getControl();
            } catch (Throwable e) {
                e.printStackTrace();
                // todo - delegate to application exception handler service
                String message = "An internal error occurred.\n " +
                        "Not able to create user interface control for\n" +
                        "page component '" + getPageComponent().getDescriptor().getTitle() + "'.";
                JOptionPane.showMessageDialog(getPageComponent().getContext().getPage().getWindow(),
                                              message, "Internal Error",
                                              JOptionPane.ERROR_MESSAGE);
                pageComponentControl = new JLabel(message);
            }
            if (pageComponentControl.getName() == null) {
                nameComponent(pageComponentControl, "Control");        
            }
            dockableFrame.getContentPane().add(pageComponentControl, BorderLayout.CENTER);
            pageComponentControlCreated = true;
            getPageComponent().componentOpened();
        }
    }

    private class DockableFrameHandler extends DockableFrameAdapter {

        public DockableFrameHandler() {
        }

        @Override
        public void dockableFrameAdded(DockableFrameEvent dockableFrameEvent) {
            Debug.trace("dockableFrameEvent = " + dockableFrameEvent);
        }

        @Override
        public void dockableFrameRemoved(DockableFrameEvent dockableFrameEvent) {
            Debug.trace("dockableFrameEvent = " + dockableFrameEvent);
        }

        @Override
        public void dockableFrameShown(DockableFrameEvent dockableFrameEvent) {
            Debug.trace("dockableFrameEvent = " + dockableFrameEvent);
            ensurePageComponentControlCreated();
            getPageComponent().componentShown();
        }

        @Override
        public void dockableFrameHidden(DockableFrameEvent dockableFrameEvent) {
            Debug.trace("dockableFrameEvent = " + dockableFrameEvent);
            if (pageComponentControlCreated) {
                getPageComponent().componentHidden();
            }
        }

        @Override
        public void dockableFrameActivated(DockableFrameEvent dockableFrameEvent) {
            Debug.trace("dockableFrameEvent = " + dockableFrameEvent);
            ensurePageComponentControlCreated();
        }

        @Override
        public void dockableFrameDeactivated(DockableFrameEvent dockableFrameEvent) {
            Debug.trace("dockableFrameEvent = " + dockableFrameEvent);
        }

        @Override
        public void dockableFrameDocked(DockableFrameEvent dockableFrameEvent) {
            Debug.trace("dockableFrameEvent = " + dockableFrameEvent);
        }

        @Override
        public void dockableFrameFloating(DockableFrameEvent dockableFrameEvent) {
            Debug.trace("dockableFrameEvent = " + dockableFrameEvent);
            ensurePageComponentControlCreated();
        }

        @Override
        public void dockableFrameAutohidden(DockableFrameEvent dockableFrameEvent) {
            Debug.trace("dockableFrameEvent = " + dockableFrameEvent);
        }

        @Override
        public void dockableFrameAutohideShowing(DockableFrameEvent dockableFrameEvent) {
            Debug.trace("dockableFrameEvent = " + dockableFrameEvent);
            ensurePageComponentControlCreated();
        }

        @Override
        public void dockableFrameTabShown(DockableFrameEvent dockableFrameEvent) {
            Debug.trace("dockableFrameEvent = " + dockableFrameEvent);
        }

        @Override
        public void dockableFrameTabHidden(DockableFrameEvent dockableFrameEvent) {
            Debug.trace("dockableFrameEvent = " + dockableFrameEvent);
        }

        @Override
        public void dockableFrameMaximized(DockableFrameEvent dockableFrameEvent) {
            Debug.trace("dockableFrameEvent = " + dockableFrameEvent);
            ensurePageComponentControlCreated();
        }

        @Override
        public void dockableFrameRestored(DockableFrameEvent dockableFrameEvent) {
            Debug.trace("dockableFrameEvent = " + dockableFrameEvent);
            ensurePageComponentControlCreated();
        }
    }
}
