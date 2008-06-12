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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import com.bc.swing.TitledPane;
/**
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
public class DockablePane extends JPanel implements DockableComponent {

    private static final long serialVersionUID = -7929784460684679243L;
    
    public static final String FLOATING_COMPONENT_FACTORY_PROPERTY_NAME = "floatingComponentFactory";
    public static final String DOCKED_PROPERTY_NAME = "docked";

    private TitledPane _titledPane;
    private FloatingComponentFactory _floatingComponentFactory;
    private FloatingComponent _floatingComponent;
    private boolean _docked;
    private boolean _closable;
    private Window _ownerWindow;
    private Container _parentContainer;
    private Rectangle _bounds;
    private Object _constraints;
    private int _componentIndex;
    private Vector _windowListeners;

    public DockablePane(String title,
                        Icon icon,
                        JComponent content,
                        boolean closable) {
        this(title, icon, content, null, closable);
    }

    public DockablePane(String title,
                        Icon icon,
                        JComponent content,
                        Object constraints,
                        boolean closable) {
        this(title, icon, content, constraints, -1, closable);
    }

    public DockablePane(String title,
                        Icon icon,
                        JComponent content,
                        int componentIndex,
                        boolean closable) {
        this(title, icon, content, null, componentIndex, closable);
    }

    private DockablePane(String title,
                         Icon icon,
                         JComponent content,
                         Object constraints,
                         int componentIndex,
                         boolean closable) {
        _constraints = constraints;
        _componentIndex = componentIndex;
        _floatingComponentFactory = FloatingWindow.getFactory();
        _docked = true;
        _closable = closable;

        setBorder(new EmptyBorder(2, 2, 2, 2));
        setLayout(new BorderLayout());
        _titledPane = new TitledPane(title, icon, content);

        final AbstractButton floatButton = createFloatButton();
        final JLabel titleBar = _titledPane.getTitleBar();
        titleBar.add(floatButton);

        if (_closable) {
            _titledPane.getTitleBar().add(createHideButton());
        }
        add(_titledPane, BorderLayout.CENTER);
    }

    public boolean isDocked() {
        return _docked;
    }

    public void setDocked(boolean docked) {
        boolean wasDocked = _docked;
        if (wasDocked != docked) {
            if (wasDocked) {
                openFloatingComponent();
            } else {
                closeFloatingComponent();
            }
            _docked = docked;
            firePropertyChange(DOCKED_PROPERTY_NAME, wasDocked, _docked);
        }
    }

    public FloatingComponentFactory getFloatingComponentFactory() {
        return _floatingComponentFactory;
    }

    public void setFloatingComponentFactory(FloatingComponentFactory floatingComponentFactory) {
        FloatingComponentFactory oldValue = _floatingComponentFactory;
        if (oldValue != floatingComponentFactory) {
            _floatingComponentFactory = floatingComponentFactory;
            firePropertyChange(FLOATING_COMPONENT_FACTORY_PROPERTY_NAME, oldValue, _floatingComponentFactory);
        }
    }

    public Icon getIcon() {
        return _titledPane.getIcon();
    }

    public String getTitle() {
        return _titledPane.getTitle();
    }

    public Component getContent() {
        if (_floatingComponent != null) {
            return _floatingComponent.getContent();
        } else {
            return _titledPane.getContent();
        }
    }

    public void setContent(Component content) {
        if (_floatingComponent != null) {
            _floatingComponent.setContent(content);
        } else {
            _titledPane.setContent(content);
        }
    }

    public void addWindowListener(WindowListener l) {
        if (_windowListeners == null) {
            _windowListeners = new Vector();
        }
        _windowListeners.add(l);
        if (!_docked && _floatingComponent != null) {
            _floatingComponent.addWindowListener(l);
        }
    }

    public void removeWindowListener(WindowListener l) {
        if (_windowListeners == null) {
            return;
        }
        _windowListeners.remove(l);
        if (!_docked && _floatingComponent != null) {
            _floatingComponent.removeWindowListener(l);
        }
    }

    /////////////////////////////////////////////////////////////////////
    // Implementation helpers

    private void addThisToParent() {
        setVisible(true);
        if (_parentContainer != null) {
            if (_parentContainer instanceof JTabbedPane) {
                addThisToTabbedPaneParent();
            } else {
                addThisToNonTabbedPaneParent();
            }
            _parentContainer.setVisible(true);
            _parentContainer.invalidate();
            _parentContainer.validate();
            _parentContainer.repaint();
        }
    }

    private void addThisToTabbedPaneParent() {
        JTabbedPane tabbedPane = (JTabbedPane) _parentContainer;
        int tabIndex = -1;
        String tabTitle = getTitle();
        Icon tabIcon = getIcon();
        String tabTip = null;

        if (_constraints instanceof TabInfo) {
            TabInfo tabInfo = (TabInfo) _constraints;
            tabIndex = tabInfo.getIndex();
            tabTitle = tabInfo.getTitle();
            tabIcon = tabInfo.getIcon();
            tabTip = tabInfo.getTip();
        } else if (_constraints instanceof Integer) {
            tabIndex = (Integer) _constraints;
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
        if (_constraints == null && _componentIndex >= 0) {
            int componentIndex = _componentIndex;
            if (componentIndex > getComponentCount()) {
                componentIndex = getComponentCount();
            }
            _parentContainer.add(this, componentIndex);
        } else {
            _parentContainer.add(this, _constraints);
        }
    }

    private void removeThisFromParent() {
        _parentContainer = getParent();
        _ownerWindow = SwingUtilities.windowForComponent(this);
        if (_bounds == null) {
            _bounds = new Rectangle(getLocationOnScreen(), getSize());
        }
        setVisible(false);
        if (_parentContainer != null) {
            if (_parentContainer instanceof JTabbedPane) {
                removeThisFromTabbedPaneParent();
            } else {
                removeThisFromNonTabbedPaneParent();
            }
            if (_parentContainer.getComponentCount() == 0) {
                _parentContainer.setVisible(false);
            }
            _parentContainer.invalidate();
            _parentContainer.validate();
            _parentContainer.repaint();
        }
    }

    private void removeThisFromTabbedPaneParent() {
        final JTabbedPane tabbedPane = (JTabbedPane) _parentContainer;
        final TabInfo tabbedPaneInfo = createTabInfo(tabbedPane);
        _constraints = tabbedPaneInfo;
        tabbedPane.removeTabAt(tabbedPaneInfo.getIndex());
    }

    private void removeThisFromNonTabbedPaneParent() {
        _componentIndex = getComponentIndex(_parentContainer, this);
        _parentContainer.remove(this);
    }

    private void openFloatingComponent() {
        removeThisFromParent();

        Component content = _titledPane.getContent();
        _titledPane.setContent(null);

        _floatingComponent = _floatingComponentFactory.createFloatingComponent(_ownerWindow);
        if (_floatingComponent instanceof FloatingWindow) {
            FloatingWindow floatingWindow = (FloatingWindow) _floatingComponent;
            floatingWindow.setClosable(_closable);
        }
        _floatingComponent.setOriginator(this);
        _floatingComponent.setIcon(_titledPane.getIcon());
        _floatingComponent.setTitle(_titledPane.getTitle());
        _floatingComponent.setContent(content);
        _floatingComponent.setBounds(_bounds);
        if (_windowListeners != null) {
            for (int i = 0; i < _windowListeners.size(); i++) {
                _floatingComponent.addWindowListener((WindowListener) _windowListeners.get(i));
            }
        }
        _floatingComponent.show();
    }

    private void closeFloatingComponent() {
        Component content = _floatingComponent.getContent();
        _bounds = _floatingComponent.getBounds();
        _floatingComponent.close();
        if (_windowListeners != null) {
            for (int i = 0; i < _windowListeners.size(); i++) {
                _floatingComponent.removeWindowListener((WindowListener) _windowListeners.get(i));
            }
        }
        _floatingComponent = null;
        _titledPane.setContent(content);

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
