/*
 * $Id: DockablePane.java,v 1.1 2006/10/10 14:47:35 norman Exp $
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
package com.bc.swing.dock;

import com.bc.swing.TitledPane;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.Window;
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
    private Container parentContainer;
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
        if (parentContainer != null) {
            if (parentContainer instanceof JTabbedPane) {
                addThisToTabbedPaneParent();
            } else {
                addThisToNonTabbedPaneParent();
            }
            parentContainer.setVisible(true);
            parentContainer.invalidate();
            parentContainer.validate();
            parentContainer.repaint();
        }
    }

    private void addThisToTabbedPaneParent() {
        JTabbedPane tabbedPane = (JTabbedPane) parentContainer;
        int tabIndex = -1;
        String tabTitle = getTitle();
        Icon tabIcon = getIcon();
        String tabTip = null;

        if (constraints instanceof TabInfo) {
            TabInfo tabInfo = (TabInfo) constraints;
            tabIndex = tabInfo.getIndex();
            tabTitle = tabInfo.getTitle();
            tabIcon = tabInfo.getIcon();
            tabTip = tabInfo.getTip();
        } else if (constraints instanceof Integer) {
            tabIndex = (Integer) constraints;
        }

        if (tabIndex >= 0 && tabIndex <= tabbedPane.getTabCount()) {
            tabbedPane.insertTab(tabTitle, tabIcon, this, tabTip, tabIndex);
        } else {
            tabbedPane.addTab(tabTitle, tabIcon, this, tabTip);
            tabIndex = tabbedPane.getTabCount() - 1;
        }

        tabbedPane.setSelectedIndex(tabIndex);
    }

    private void addThisToNonTabbedPaneParent() {
        if (constraints == null && componentIndex >= 0) {
            int componentIdx = componentIndex;
            if (componentIdx > parentContainer.getComponentCount()) {
                componentIdx = parentContainer.getComponentCount();
            }
            parentContainer.add(this, componentIdx);
        } else {
            parentContainer.add(this, constraints);
        }
    }

    private void removeThisFromParent() {
        parentContainer = getParent();
        ownerWindow = SwingUtilities.windowForComponent(this);
        if (bounds == null) {
            bounds = new Rectangle(getLocationOnScreen(), getSize());
        }
        setVisible(false);
        if (parentContainer != null) {
            if (parentContainer instanceof JTabbedPane) {
                removeThisFromTabbedPaneParent();
            } else {
                removeThisFromNonTabbedPaneParent();
            }
            if (parentContainer.getComponentCount() == 0) {
                parentContainer.setVisible(false);
            }
            parentContainer.invalidate();
            parentContainer.validate();
            parentContainer.repaint();
        }
    }

    private void removeThisFromTabbedPaneParent() {
        final JTabbedPane tabbedPane = (JTabbedPane) parentContainer;
        final TabInfo tabbedPaneInfo = createTabInfo(tabbedPane);
        constraints = tabbedPaneInfo;
        tabbedPane.removeTabAt(tabbedPaneInfo.getIndex());
    }

    private void removeThisFromNonTabbedPaneParent() {
        componentIndex = getComponentIndex(parentContainer, this);
        parentContainer.remove(this);
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
        floatingComponent.setBounds(bounds);
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
        return TitledPane.createTitleBarButton("hide", "Hide", new ActionListener() {

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

    private TabInfo createTabInfo(JTabbedPane tabbedPane) {
        final int index = tabbedPane.indexOfComponent(this);
        String title = tabbedPane.getTitleAt(index);
        if (title == null || title.length() == 0) {
            title = getTitle();
        }
        Icon icon = tabbedPane.getIconAt(index);
        if (icon == null) {
            icon = getIcon();
        }
        final String toolTipText = tabbedPane.getToolTipTextAt(index);
        return new TabInfo(index, title, icon, toolTipText);
    }

    public static class TabInfo {

        private String title;
        private Icon icon;
        private String tip;
        private int index;

        public TabInfo(int index, String title, Icon icon, String toolTipText) {
            this.title = title;
            this.icon = icon;
            this.index = index;
            this.tip = toolTipText;
        }

        public Icon getIcon() {
            return icon;
        }

        public void setIcon(Icon icon) {
            this.icon = icon;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getTip() {
            return tip;
        }

        public void setTip(String tip) {
            this.tip = tip;
        }
    }
}
