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
package com.bc.swing.dock;

import com.bc.swing.TitledPane;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import java.util.Vector;

/**
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
public class DockablePane extends JPanel implements DockableComponent {

    private static final long serialVersionUID = -7929784460684679243L;
    
    public static final String FLOATING_COMPONENT_FACTORY_PROPERTY_NAME = "floatingComponentFactory";
    public static final String DOCKED_PROPERTY_NAME = "docked";

    private TitledPane titledPane;
    private FloatingComponentFactory floatingComponentFactory;
    private FloatingComponent floatingComponent;
    private boolean docked;
    private boolean closable;
    private Window ownerWindow;
    private Rectangle bounds;
    private Object constraints;
    private int componentIndex;
    private Vector windowListeners;

    public DockablePane(String title,
                        Icon icon,
                        JComponent content,
                        boolean closable, final FloatingComponentFactory factory) {
        this(title, icon, content, null, closable, factory);
    }

    public DockablePane(String title,
                        Icon icon,
                        JComponent content,
                        Object constraints,
                        boolean closable, final FloatingComponentFactory factory) {
        this(title, icon, content, constraints, -1, closable, factory);
    }

    public DockablePane(String title,
                        Icon icon,
                        JComponent content,
                        int componentIndex,
                        boolean closable, final FloatingComponentFactory factory) {
        this(title, icon, content, null, componentIndex, closable, factory);
    }

    private DockablePane(String title,
                         Icon icon,
                         JComponent content,
                         Object constraints,
                         int componentIndex,
                         boolean closable, final FloatingComponentFactory factory) {
        this.constraints = constraints;
        this.componentIndex = componentIndex;
        floatingComponentFactory = factory;
        docked = true;
        this.closable = closable;

        setBorder(new EmptyBorder(2, 2, 2, 2));
        setLayout(new BorderLayout());
        titledPane = new TitledPane(title, icon, content);

        final AbstractButton floatButton = createFloatButton();
        final JLabel titleBar = titledPane.getTitleBar();
        titleBar.add(floatButton);

        if (this.closable) {
            titledPane.getTitleBar().add(createHideButton());
        }
        add(titledPane, BorderLayout.CENTER);
    }


    public void setShown(boolean show) {
        final boolean wasShowing = isContentShown();
        if (!wasShowing && show) {
            addThisToParent();
        }
        if (wasShowing && !show){
            if(!docked) {
                setDocked(true);
            }
            removeThisFromParent();
        }
    }

    public boolean isContentShown() {
        return getContent().isShowing();
    }

    public boolean isDocked() {
        return docked;
    }

    public void setDocked(boolean docked) {
        boolean wasDocked = this.docked;
        if (wasDocked != docked) {
            if (wasDocked) {
                openFloatingComponent();
            } else {
                closeFloatingComponent();
            }
            this.docked = docked;
            firePropertyChange(DOCKED_PROPERTY_NAME, wasDocked, this.docked);
        }
    }

    public FloatingComponentFactory getFloatingComponentFactory() {
        return floatingComponentFactory;
    }

    public void setFloatingComponentFactory(FloatingComponentFactory floatingComponentFactory) {
        FloatingComponentFactory oldValue = this.floatingComponentFactory;
        if (oldValue != floatingComponentFactory) {
            this.floatingComponentFactory = floatingComponentFactory;
            firePropertyChange(FLOATING_COMPONENT_FACTORY_PROPERTY_NAME, oldValue, this.floatingComponentFactory);
        }
    }

    public Icon getIcon() {
        return titledPane.getIcon();
    }

    public String getTitle() {
        return titledPane.getTitle();
    }

    public Component getContent() {
        if (floatingComponent != null) {
            return floatingComponent.getContent();
        } else {
            return titledPane.getContent();
        }
    }

    public void setContent(Component content) {
        if (floatingComponent != null) {
            floatingComponent.setContent(content);
        } else {
            titledPane.setContent(content);
        }
    }

    public void addWindowListener(WindowListener l) {
        if (windowListeners == null) {
            windowListeners = new Vector();
        }
        windowListeners.add(l);
        if (!docked && floatingComponent != null) {
            floatingComponent.addWindowListener(l);
        }
    }

    public void removeWindowListener(WindowListener l) {
        if (windowListeners == null) {
            return;
        }
        windowListeners.remove(l);
        if (!docked && floatingComponent != null) {
            floatingComponent.removeWindowListener(l);
        }
    }

    /////////////////////////////////////////////////////////////////////
    // Implementation helpers

    private void addThisToParent() {
        setVisible(true);
    }

    private void removeThisFromParent() {
        ownerWindow = SwingUtilities.windowForComponent(this);
        if (bounds == null) { // store the size for the floating window which may be shown next
            bounds = new Rectangle(getLocationOnScreen(), getSize());
        }
        setVisible(false);
    }

    private void openFloatingComponent() {
        removeThisFromParent();

        Component content = titledPane.getContent();
        titledPane.setContent(null);

        floatingComponent = floatingComponentFactory.createFloatingComponent(ownerWindow);
        if (floatingComponent instanceof FloatingWindow) {
            FloatingWindow floatingWindow = (FloatingWindow) floatingComponent;
            floatingWindow.setClosable(closable);
        }
        floatingComponent.setOriginator(this);
        floatingComponent.setIcon(titledPane.getIcon());
        floatingComponent.setTitle(titledPane.getTitle());
        floatingComponent.setContent(content);
        if(bounds != null) {
            floatingComponent.setBounds(bounds);
        }
        if (windowListeners != null) {
            for (Object windowListener : windowListeners) {
                floatingComponent.addWindowListener((WindowListener) windowListener);
            }
        }
        floatingComponent.show();
    }

    private void closeFloatingComponent() {
        Component content = floatingComponent.getContent();
        bounds = floatingComponent.getBounds();
        floatingComponent.close();
        if (windowListeners != null) {
            for (Object windowListener : windowListeners) {
                floatingComponent.removeWindowListener((WindowListener) windowListener);
            }
        }
        floatingComponent = null;
        titledPane.setContent(content);

        addThisToParent();
    }

    protected AbstractButton createFloatButton() {
        return TitledPane.createTitleBarButton("undock", "Float", new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setDocked(false);
            }
        });
    }

    protected AbstractButton createHideButton() {
        return TitledPane.createTitleBarButton("close", "Close", new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                removeThisFromParent();
            }
        });
    }

    public static int getComponentIndex(Container container, Component component) {
        final int n = container.getComponentCount();
        for (int i = 0; i < n; i++) {
            if (container.getComponent(i) == component) {
                return i;
            }
        }
        return -1;
    }

}
