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
package com.bc.swing;

import com.jidesoft.swing.ContentContainer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 * A pane with a title component (a javax.swing.JLabel) and a content component.
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
public class TitledPane extends ContentContainer  { // <JIDE/>

    private static final long serialVersionUID = 6219377186428413528L;

    public static final String ACTIVATED_PROPERTY_NAME = "activated";
// uncomment for font test
//    public static final Font DEFAULT_TITLE_FONT = new Font("Helvetica", Font.BOLD, 11);
    public static final Color DEFAULT_TITLE_BG_ACTIVE = new Color(82, 109, 165);
    public static final Color DEFAULT_TITLE_FG = Color.white;
    public static final Color DEFAULT_TITLE_BG_DEACTIVE = Color.lightGray.darker();

    private JLabel _titleBar;
    private Component _content;
    private boolean _activated;


    public TitledPane() {
        this(" ", null, null);
    }

    public TitledPane(String title) {
        this(title, null, null);
    }

    public TitledPane(String title, JComponent content) {
        this(title, null, content);
    }

    public TitledPane(String title, Icon icon, JComponent content) {
        super();

        setLayout(new BorderLayout());

        _titleBar = new JLabel();
        _titleBar.setLayout(new FlowLayout(FlowLayout.RIGHT, 2, 0));
// <old-UI>
//        _titleBar.setOpaque(true);
//// uncomment for font test
////        _titleBar.setFont(DEFAULT_TITLE_FONT);
//        _titleBar.setBackground(DEFAULT_TITLE_BG_DEACTIVE);      // todo - replace by approp. UIDefault setting
//        _titleBar.setForeground(DEFAULT_TITLE_FG);               // todo - replace by approp. UIDefault setting
// </old-UI>
        _titleBar.setBorder(BorderFactory.createEmptyBorder(1, 3, 1, 1));
        if (icon != null) {
            _titleBar.setIcon(icon);
        }
        if (title != null) {
            _titleBar.setText(title);
        }
        add(_titleBar, BorderLayout.NORTH);

        addMouseListener(new MouseAdapter() {
            /**
             * Invoked when the mouse has been clicked on a component.
             */
            @Override
            public void mouseClicked(MouseEvent e) {
                requestFocus();
            }
        });
        setContent(content);
        setActivated(true);
    }

    public boolean isActivated() {
        return _activated;
    }

    public void setActivated(boolean activated) {
        boolean oldValue = _activated;
        if (oldValue != activated) {
            _activated = activated;
            getTitleBar().setBackground(activated ? DEFAULT_TITLE_BG_ACTIVE : DEFAULT_TITLE_BG_DEACTIVE);
            firePropertyChange(ACTIVATED_PROPERTY_NAME, oldValue, _activated);
        }
    }

    public String getTitle() {
        return getTitleBar().getText();
    }

    public void setTitle(String title) {
        getTitleBar().setText(title);
    }

    public Icon getIcon() {
        return getTitleBar().getIcon();
    }

    public void setIcon(Icon icon) {
        getTitleBar().setIcon(icon);
    }

    public JLabel getTitleBar() {
        return _titleBar;
    }

    public Component getContent() {
        return _content;
    }

    public void setContent(Component content) {
        setContent(content, true);
    }

    protected void setContent(Component content, boolean add) {
        if (_content != content) {
            if (add) {
                removeContent();
                _content = content;
                addContent();
            } else {
                _content = content;
            }
        }
    }

    public static JButton createTitleBarButton(String name, String toolTipText, ActionListener actionListener) {
        ImageIcon imageIcon = null;

        final URL url = TitledPane.class.getResource("/com/bc/swing/icons/" + name + ".gif");
        if (url != null) {
            imageIcon = new ImageIcon(url);
        }

        return createTitleBarButton(imageIcon, name, toolTipText, actionListener);
    }

    public static JButton createTitleBarButton(Icon icon, String name, String toolTipText, ActionListener actionListener) {
        final String text;
        final Dimension iconSize;
        if (icon != null) {
            iconSize = new Dimension(icon.getIconWidth(), icon.getIconHeight());
            text = null;
        } else {
            iconSize = new Dimension(15, 15);
            text = name;
        }
        JButton button = new JButton();
        button.setName(name);
        button.setIcon(icon);
        button.setText(text);
        button.setToolTipText(toolTipText);
        button.addActionListener(actionListener);
        button.setBorder(null);
        button.setBorderPainted(false);
        button.setBackground(DEFAULT_TITLE_BG_ACTIVE);
        button.setMinimumSize(iconSize);
        button.setPreferredSize(iconSize);
        button.setDefaultCapable(false);
        button.setContentAreaFilled(false);
        return button;
    }


    protected boolean isContentInstalled() {
        return getComponentIndex(this, _content) != -1;
    }

    protected void addContent() {
        if (_content != null) {
            add(_content, BorderLayout.CENTER);
        }
    }

    protected void removeContent() {
        if (_content != null) {
            remove(_content);
        }
    }

    protected static int getComponentIndex(Container container, Component component) {
        if (container != null && component != null) {
            Component[] components = container.getComponents();
            for (int i = 0; i < components.length; i++) {
                if (components[i] == component) {
                    return i;
                }
            }
        }
        return -1;
    }


}
