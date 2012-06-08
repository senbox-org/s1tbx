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
package com.bc.swing.desktop;

import com.jidesoft.swing.JideTabbedPane;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

/**
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 */
public class TabbedDesktopPane extends JPanel {

    private static final long serialVersionUID = -9125146428340482204L;
    
    private JDesktopPane desktopPane;
    private JideTabbedPane tabbedPane;
    private InternalFrameHandler frameListener;
    private JMenu windowMenu;
    private WindowMenuListener windowMenuListener;
    private InternalFrameLayoutManager frameLayoutManager;

    public TabbedDesktopPane() {
        this(new JideTabbedPane(JTabbedPane.TOP,
                             JTabbedPane.SCROLL_TAB_LAYOUT),
             new JDesktopPane());
    }

    public TabbedDesktopPane(JTabbedPane tabbedPane, JDesktopPane desktopPane) {
        super(new BorderLayout());
        this.tabbedPane = (JideTabbedPane) tabbedPane;
        this.desktopPane = desktopPane;
        this.frameListener = new InternalFrameHandler();
        this.windowMenu = null;
        this.windowMenuListener = new WindowMenuListener();
        this.frameLayoutManager = new DefaultInternalFrameLayoutManager();

        this.tabbedPane.setShowCloseButtonOnTab(true);

        initUI();
    }

    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    public JDesktopPane getDesktopPane() {
        return desktopPane;
    }

    public InternalFrameLayoutManager getFrameLayoutManager() {
        return frameLayoutManager;
    }

    public void setFrameLayoutManager(InternalFrameLayoutManager frameLayoutManager) {
        this.frameLayoutManager = frameLayoutManager;
    }

    public JMenu getWindowMenu() {
        return windowMenu;
    }

    public void setWindowMenu(JMenu windowMenu) {
        JMenu windowMenuOld = this.windowMenu;
        if (windowMenuOld != windowMenu) {
            if (windowMenuOld != null) {
                windowMenuOld.removeMenuListener(windowMenuListener);
            }
            this.windowMenu = windowMenu;
            if (this.windowMenu != null) {
                this.windowMenu.addMenuListener(windowMenuListener);
            }
            firePropertyChange("windowMenu", windowMenuOld, this.windowMenu);
        }
    }


    @Override
    protected void addImpl(Component comp, Object constraints, int index) {
        if (comp == tabbedPane || comp == desktopPane) {
            super.addImpl(comp, constraints, index);
        } else {
            throw new IllegalStateException("use addFrame method to add internal frames");
        }
    }

    @Override
    public void remove(int index) {
        final Component comp = getComponent(index);
        if (comp == tabbedPane || comp == desktopPane) {
            super.remove(index);
        } else {
            throw new IllegalStateException("use removeFrame method to remove internal frames");
        }
    }

    public JInternalFrame getSelectedFrame() {
        return desktopPane.getSelectedFrame();
    }

    public JInternalFrame[] getAllFrames() {
        return desktopPane.getAllFrames();
    }

    public JInternalFrame[] getVisibleFrames() {
        final JInternalFrame[] allFrames = getAllFrames();
        return getVisibleFrames(allFrames);
    }

    public void addFrame(final JInternalFrame internalFrame) {
        if (!internalFrame.isVisible()) {
            internalFrame.setVisible(true);
        }
        internalFrame.addInternalFrameListener(frameListener);
        desktopPane.add(internalFrame);
        addTabFor(internalFrame);
        if (internalFrame.isSelected()) {
            selectTabFor(internalFrame);
        } else {
            try {
                internalFrame.setSelected(true);
            } catch (PropertyVetoException ignored) {
            }
        }

        internalFrame.addPropertyChangeListener(JInternalFrame.TITLE_PROPERTY, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                final InternalFrameProxy placeHolder = getPlaceHolderFor(internalFrame);
                final int index = tabbedPane.indexOfComponent(placeHolder);
                if (index > -1) {
                    tabbedPane.setTitleAt(index, internalFrame.getTitle());
                }
            }
        });
    }

    public void closeFrame(JInternalFrame internalFrame) {
        internalFrame.removeInternalFrameListener(frameListener);
        try {
            internalFrame.setClosed(true);
        } catch (PropertyVetoException e) {
            internalFrame.dispose();
        }
        removeTabFor(internalFrame);
        desktopPane.remove(internalFrame);
    }

    public void closeAllFrames() {
        final JInternalFrame[] allFrames = getAllFrames();
        for (int i = allFrames.length - 1; i >= 0; --i) {
            closeFrame(allFrames[i]);
        }
    }

    public void moveFrameToVisible(final JInternalFrame internalFrame) {
        frameLayoutManager.moveFrameToVisible(desktopPane, internalFrame);
    }

    public void cascadeFrames() {
        frameLayoutManager.cascadeFrames(desktopPane, reverseFrames(getVisibleFrames()));
    }

    public void tileFramesEvenly() {
        frameLayoutManager.tileFramesEvenly(desktopPane, sortFrames(getVisibleFrames()));
    }

    public void tileFramesHorizontally() {
        frameLayoutManager.tileFramesHorizontally(desktopPane, sortFrames(getVisibleFrames()));
    }

    public void tileFramesVertically() {
        frameLayoutManager.tileFramesVertically(desktopPane, sortFrames(getVisibleFrames()));
    }

    private void initUI() {
        initTabbedPaneMaxHeight();
        updateTabbedPaneVisibility();

        tabbedPane.addChangeListener(new TabbedPaneChangeHandler());
        tabbedPane.addMouseListener(new PopupMenuHandler());
// <old-UI>
//        desktopPane.setBackground(BACKGROUND_COLOR);
// </old-UI>
        tabbedPane.setBackground(desktopPane.getBackground());

// <JIDE>
        tabbedPane.setCloseAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JInternalFrame selectedFrame = getSelectedFrame();
                if(selectedFrame != null) {
                    closeFrame(selectedFrame);
                }
            }
        });
        tabbedPane.setShowCloseButton(true);
//        tabbedPane.setTabShape(JideTabbedPane.SHAPE_BOX);
//        tabbedPane.setTabResizeMode(JideTabbedPane.RESIZE_MODE_FIXED);
// </JIDE>

        add(tabbedPane, BorderLayout.NORTH);
        add(desktopPane, BorderLayout.CENTER);
    }

    private void initTabbedPaneMaxHeight() {
//        tabbedPane.addTab("X", new InternalFrameProxy(null));
//        Dimension preferredSize = tabbedPane.getPreferredSize();
//        tabbedPane.removeTabAt(0);
//
//        if (preferredSize != null && preferredSize.height > 0) {
//            tabbedPane.setPreferredSize(new Dimension(preferredSize.width, preferredSize.height));
//            final Dimension maximumSize = tabbedPane.getMaximumSize();
//            if (maximumSize != null) {
//                tabbedPane.setMaximumSize(new Dimension(maximumSize.width, preferredSize.height));
//            }
//        }
    }

    private void updateTabbedPaneVisibility() {
        tabbedPane.setVisible(tabbedPane.getTabCount() > 0);
    }

    private void addTabFor(JInternalFrame internalFrame) {
        final InternalFrameProxy placeHolder = getPlaceHolderFor(internalFrame);
        if (placeHolder == null) {
            tabbedPane.addTab(internalFrame.getTitle(), new InternalFrameProxy(internalFrame));
            updateTabbedPaneVisibility();
        }
    }

    private void removeTabFor(JInternalFrame internalFrame) {
        final InternalFrameProxy placeHolder = getPlaceHolderFor(internalFrame);
        if (placeHolder != null) {
            tabbedPane.remove(placeHolder);
            updateTabbedPaneVisibility();
        }
    }

    private void selectTabFor(JInternalFrame internalFrame) {
        final InternalFrameProxy placeHolder = getPlaceHolderFor(internalFrame);
        if (placeHolder != null) {
            tabbedPane.setSelectedComponent(placeHolder);
        }
    }

    private InternalFrameProxy getPlaceHolderFor(JInternalFrame internalFrame) {
        int n = tabbedPane.getComponentCount();
        for (int i = 0; i < n; i++) {
            Component component = tabbedPane.getComponent(i);
            if (component instanceof InternalFrameProxy) {
                InternalFrameProxy placeHolder = (InternalFrameProxy) component;
                if (placeHolder.getInternalFrame() == internalFrame) {
                    return placeHolder;
                }
            }
        }
        return null;
    }


    private void updateWindowMenu() {
        if (windowMenu == null) {
            return;
        }

        windowMenu.removeAll();

        final JInternalFrame[] allFrames = getAllFrames();
        sortFrames(allFrames);
        final JInternalFrame[] visibleFrames = getVisibleFrames(allFrames);

        final JMenuItem cascadeMenuItem = new JMenuItem("Cascade"); /*I18N*/
        cascadeMenuItem.setMnemonic('C'); /*I18N*/
        cascadeMenuItem.setEnabled(visibleFrames.length > 1);
        cascadeMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cascadeFrames();
            }
        });
        windowMenu.add(cascadeMenuItem);

        final JMenuItem tileEMenuItem = new JMenuItem("Tile Evenly"); /*I18N*/
        tileEMenuItem.setMnemonic('E'); /*I18N*/
        tileEMenuItem.setEnabled(visibleFrames.length > 1);
        tileEMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tileFramesEvenly();
            }
        });
        windowMenu.add(tileEMenuItem);

        final JMenuItem tileHMenuItem = new JMenuItem("Tile Horizontally"); /*I18N*/
        tileHMenuItem.setMnemonic('H'); /*I18N*/
        tileHMenuItem.setEnabled(visibleFrames.length > 1);
        tileHMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tileFramesHorizontally();
            }
        });
        windowMenu.add(tileHMenuItem);

        final JMenuItem tileVMenuItem = new JMenuItem("Tile Vertically"); /*I18N*/
        tileVMenuItem.setMnemonic('V'); /*I18N*/
        tileVMenuItem.setEnabled(visibleFrames.length > 1);
        tileVMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tileFramesVertically();
            }
        });
        windowMenu.add(tileVMenuItem);

        windowMenu.addSeparator();

        final JMenuItem closeAllMenuItem = new JMenuItem("Close All");/*I18N*/
        closeAllMenuItem.setMnemonic('A'); /*I18N*/
        closeAllMenuItem.setEnabled(allFrames.length > 0);
        closeAllMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeAllFrames();
            }
        });
        windowMenu.add(closeAllMenuItem);

        if (allFrames.length > 0) {
            windowMenu.addSeparator();

            for (int i = 0; i < allFrames.length; i++) {
                final JInternalFrame frame = allFrames[i];
                int windowNo = i + 1;
                final char mnemonic;
                if (windowNo >= 10) {
                    mnemonic = (char) ('A' + (windowNo - 10));
                } else {
                    mnemonic = (char) ('0' + windowNo);
                }
                final JCheckBoxMenuItem selectMenuItem = new JCheckBoxMenuItem(mnemonic + " " + frame.getTitle());
                selectMenuItem.setSelected(frame == getSelectedFrame());
                selectMenuItem.setMnemonic(mnemonic);
                selectMenuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            frame.setSelected(true);
                        } catch (PropertyVetoException ignored) {
                        }
                    }
                });
                windowMenu.add(selectMenuItem);
            }
        }
    }

    private JInternalFrame[] sortFrames(final JInternalFrame[] frames) {
        Arrays.sort(frames, new FrameComparator());
        return frames;
    }

    private JInternalFrame[] reverseFrames(JInternalFrame[] visibleFrames) {
        final int n = visibleFrames.length;
        for (int i = 0; i < n / 2; i++) {
            final int j = n - 1 - i;
            JInternalFrame frame = visibleFrames[j];
            visibleFrames[j] = visibleFrames[i];
            visibleFrames[i] = frame;
        }
        return visibleFrames;
    }

    private JInternalFrame[] getVisibleFrames(final JInternalFrame[] frames) {
        ArrayList<JInternalFrame> list = new ArrayList<JInternalFrame>();
        for (JInternalFrame frame : frames) {
            if (frame.isVisible() && !frame.isIcon()) {
                list.add(frame);
            }
        }
        return list.toArray(new JInternalFrame[list.size()]);
    }

    private static class InternalFrameProxy extends JComponent {

        private static final long serialVersionUID = 1961531463465924691L;
        
        private final JInternalFrame internalFrame;

        public InternalFrameProxy(JInternalFrame internalFrame) {
            setVisible(false);
            setSize(0, 0);
            setPreferredSize(getSize());
            setMaximumSize(getSize());
            this.internalFrame = internalFrame;
        }

        public JInternalFrame getInternalFrame() {
            return internalFrame;
        }
    }

    private class InternalFrameHandler extends InternalFrameAdapter {

        @Override
        public void internalFrameClosed(InternalFrameEvent e) {
            removeTabFor(e.getInternalFrame());
        }

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final JInternalFrame internalFrame = e.getInternalFrame();
            selectTabFor(internalFrame);
            moveFrameToVisible(internalFrame);
        }
    }

    private class TabbedPaneChangeHandler implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            final Component selectedComponent = tabbedPane.getSelectedComponent();
            if (selectedComponent instanceof InternalFrameProxy) {
                InternalFrameProxy placeHolder = (InternalFrameProxy) selectedComponent;
                if (placeHolder.getInternalFrame().isIcon()) {
                    try {
                        placeHolder.getInternalFrame().setIcon(false);
                    } catch (PropertyVetoException ignored) {
                    }
                }
                if (!placeHolder.getInternalFrame().isSelected()) {
                    try {
                        placeHolder.getInternalFrame().setSelected(true);
                    } catch (PropertyVetoException ignored) {
                    }
                }
            }
            updateTabbedPaneVisibility();
        }
    }

    private class WindowMenuListener implements MenuListener {

        @Override
        public void menuSelected(MenuEvent e) {
            updateWindowMenu();
        }

        @Override
        public void menuDeselected(MenuEvent e) {
        }

        @Override
        public void menuCanceled(MenuEvent e) {
        }
    }

    private static class FrameComparator implements Comparator<JInternalFrame> {

        @Override
        public int compare(JInternalFrame f1, JInternalFrame f2) {
            if (f1.getTitle() != null && f2.getTitle() != null) {
                return f1.getTitle().compareTo(f2.getTitle());
            } else if (f1.getTitle() != null) {
                return 1;
            } else if (f2.getTitle() != null) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    private class PopupMenuHandler extends MouseAdapter {

        /**
         * Invoked when a mouse button has been released on a component.
         */
        @Override
        public void mouseReleased(MouseEvent e) {
            // check: Context menu does not work anymore!!!
            if (e.isPopupTrigger()) {
                final int index = tabbedPane.indexAtLocation(e.getX(), e.getY());
                if (index >= 0) {
                    final Component component = tabbedPane.getComponentAt(index);
                    if (component instanceof InternalFrameProxy) {
                        final InternalFrameProxy internalFrameProxy = (InternalFrameProxy) component;
                        final JInternalFrame thisFrame = internalFrameProxy.getInternalFrame();
                        showPopupMenu(thisFrame, e);
                    }
                }
            }
        }

        private void showPopupMenu(final JInternalFrame thisFrame, MouseEvent e) {
            final JPopupMenu popupMenu = new JPopupMenu();

            final JMenuItem closeMenuItem = new JMenuItem("Close");
            closeMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    closeFrame(thisFrame);
                }
            });
            popupMenu.add(closeMenuItem);

            if (tabbedPane.getTabCount() > 1) {
                final JMenuItem closeAllButThisMenuItem = new JMenuItem("Close All But This");
                closeAllButThisMenuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        final JInternalFrame[] allFrames = getAllFrames();
                        for (JInternalFrame otherFrame : allFrames) {
                            if (otherFrame != thisFrame) {
                                closeFrame(otherFrame);
                            }
                        }
                    }
                });
                popupMenu.add(closeAllButThisMenuItem);

                final JMenuItem closeAllMenuItem = new JMenuItem("Close All");
                closeAllMenuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        closeAllFrames();
                    }
                });
                popupMenu.add(closeAllMenuItem);
            }

            popupMenu.show(tabbedPane, e.getX(), e.getY());
        }
    }
}
