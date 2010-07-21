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

import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideToggleButton;
import org.esa.beam.framework.ui.UIUtils;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.FilteredImageSource;
import java.awt.image.RGBImageFilter;
import java.util.EventObject;

//import org.esa.beam.framework.ui.UIUtils;

/**
 * The <code>ToolButtonFactory</code> can be used to create tool bar buttons which have a consistent look and feel.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 */
public class ToolButtonFactory {

    public static final Color SELECTED_BORDER_COLOR = new Color(8, 36, 107);
    private static final Color SELECTED_BACKGROUND_COLOR = new Color(130, 146, 185);
    private static final Color ROLLOVER_BACKGROUND_COLOR = new Color(181, 190, 214);
    private static final int BUTTON_MIN_SIZE = 16;

    private static ImageIcon _separatorIcon;

    public static AbstractButton createButton(Icon icon, boolean toggle) {
        AbstractButton button = createButton(toggle);
        button.setIcon(icon);
        configure(button);
        return button;
    }

    public static AbstractButton createButton(Action action, boolean toggle) {
        AbstractButton button = createButton(toggle);
        setButtonName(button, action);
        button.setAction(action);
        configure(button);
        return button;
    }

    private static AbstractButton createButton(boolean toggle) {
        JideButton button;
        if (toggle) {
            button = new JideToggleButton(); // <JIDE/>
        } else {
            button = new JideButton(); // <JIDE/>
        }
        return button;
    }

    private static void configure(AbstractButton button) {

        RolloverButtonEventListener l = new RolloverButtonEventListener();
        button.addMouseListener(l);
        button.addItemListener(l);

        if (button.getAction() != null) {
            if (button.getIcon() != null) {
                button.putClientProperty("hideActionText", Boolean.TRUE);
            }
            Object largeIcon = button.getAction().getValue("_largeIcon");
            if (largeIcon instanceof Icon) {
                button.setIcon((Icon) largeIcon);
            }
        }

        Icon icon = button.getIcon();
        int minWidth = BUTTON_MIN_SIZE;
        int minHeight = BUTTON_MIN_SIZE;
        if (icon != null) {
            button.setText(null);
            minWidth = Math.max(icon.getIconWidth(), BUTTON_MIN_SIZE);
            minHeight = Math.max(icon.getIconHeight(), BUTTON_MIN_SIZE);
            if (icon instanceof ImageIcon) {
                button.setRolloverIcon(createRolloverIcon((ImageIcon) icon));
            }
        } else {
            button.setText("[?]");
        }
        final int space = 3;
        Dimension prefSize = new Dimension(minWidth + space, minHeight + space);
        Dimension minSize = new Dimension(minWidth, minHeight);
        Dimension maxSize = new Dimension(minWidth + space, minHeight + space);
        button.setPreferredSize(prefSize);
        button.setMaximumSize(maxSize);
        button.setMinimumSize(minSize);

    }

    public static JComponent createToolBarSeparator() {
        if (_separatorIcon == null) {
            _separatorIcon = UIUtils.loadImageIcon("icons/Separator24.gif");
        }
        return new JLabel(_separatorIcon);
    }


    public static ImageIcon createRolloverIcon(ImageIcon imageIcon) {
        return new ImageIcon(createRolloverImage(imageIcon.getImage()));
    }


    private static Image createRolloverImage(Image image) {
        return Toolkit.getDefaultToolkit().createImage(new FilteredImageSource(image.getSource(),
                                                                               new BrightBlueFilter()));
    }

    private static class BrightBlueFilter extends RGBImageFilter {

        public BrightBlueFilter() {
            canFilterIndexColorModel = true;
        }

        @Override
        public int filterRGB(int x, int y, int rgb) {
            int a = (rgb & 0xff000000) >> 24;
            int r = (rgb & 0x00ff0000) >> 16;
            int g = (rgb & 0x0000ff00) >> 8;
            int b = rgb & 0x000000ff;
            int i = (r + g + b) / 3;
            r = g = i;
            b = 255;
            return a << 24 | r << 16 | g << 8 | b;
        }
    }

    private static class RolloverButtonEventListener extends MouseAdapter implements ItemListener {

        public RolloverButtonEventListener() {
        }

        public AbstractButton getButton(EventObject e) {
            return (AbstractButton) e.getSource();

        }

        /**
         * Invoked when a mouse button has been pressed on a component.
         */
        @Override
        public void mousePressed(MouseEvent e) {
            setSelectedState(getButton(e));
        }

        /**
         * Invoked when a mouse button has been released on a component.
         */
        @Override
        public void mouseReleased(MouseEvent e) {
            setDefaultState(getButton(e));
        }

        /**
         * Invoked when the mouse enters a component.
         */
        @Override
        public void mouseEntered(MouseEvent e) {
            setRolloverStateState(getButton(e));
        }

        /**
         * Invoked when the mouse exits a component.
         */
        @Override
        public void mouseExited(MouseEvent e) {
            setDefaultState(getButton(e));
        }

        private void setDefaultState(AbstractButton b) {
            if (b.isSelected()) {
                setSelectedState(b);
            } else {
                setNormalState(b);
            }
        }

        private void setNormalState(AbstractButton b) {
            b.setBorderPainted(false);
//            b.setForeground(getDefaultForeground());
            b.setBackground(getDefaultBackground());
        }

        private void setSelectedState(AbstractButton b) {
            if (b.isEnabled()) {
                b.setBorderPainted(true);
                b.setBackground(SELECTED_BACKGROUND_COLOR);
            } else {
                b.setBorderPainted(false);
                b.setBackground(getDefaultBackground().darker());
            }
        }

        private void setRolloverStateState(AbstractButton b) {
            if (b.isEnabled()) {
                b.setBorderPainted(true);
                b.setBackground(ROLLOVER_BACKGROUND_COLOR);
            }
        }

        /**
         * Invoked when an item has been selected or deselected. The code written for this method performs the
         * operations that need to occur when an item is selected (or deselected).
         */
        public void itemStateChanged(ItemEvent e) {
            setDefaultState((AbstractButton) e.getSource());
        }

        private Color getDefaultBackground() {
            Color color = null;
            String[] keys = new String[]{"Button.background", "Label.background", "Panel.background"};
            for (String key : keys) {
                color = UIManager.getLookAndFeel().getDefaults().getColor(key);
            }
            if (color == null) {
                color = new Color(238, 238, 238);
            }
            return color;
        }
    }


    private static void setButtonName(AbstractButton button, Action action) {
        if (button.getName() == null) {
            String name = null;

            Object value = action.getValue(Action.ACTION_COMMAND_KEY);
            if (value != null) {
                name = value.toString();
            } else {
                value = action.getValue(Action.NAME);
                if (value != null) {
                    name = value.toString();
                }
            }
            if (name != null) {
                button.setName(name);
            }
        }
    }

}
