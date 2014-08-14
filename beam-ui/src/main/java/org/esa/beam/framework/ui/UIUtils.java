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
package org.esa.beam.framework.ui;

import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.ParamProperties;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.util.ArrayUtils;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.StringUtils;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.text.DecimalFormat;

/**
 * The <code>UIUtils</code> class provides methods frequently used in connection with graphical user interfaces.
 *
 * @author Norman Fomferra
 * @version $Revision: 8407 $  $Date: 2010-02-14 12:58:02 +0100 (So, 14 Feb 2010) $
 */
public class UIUtils {

    public static final String PROPERTY_SOURCE_PRODUCT = "SOURCE_PRODUCT";

    public static final String IMAGE_RESOURCE_PATH = "/org/esa/beam/resources/images/";
    public static final Color COLOR_DARK_RED = new Color(128, 0, 0);
    public static final Color COLOR_DARK_BLUE = new Color(0, 0, 128);
    public static final Color COLOR_DARK_GREEN = new Color(0, 128, 0);

    /**
     * Gets the image icon loaded from the given resource path.
     * <p>Note that this method only works for images found in the classpath of the class loader which loaded this {@link UIUtils} class.
     * If you are not sure, you should better use {@link #getImageURL(String, Class)}.</p>
     *
     * @param resourcePath the resource path
     *
     * @return an image icon loaded from the given resource path or <code>null</code> if it could not be found
     */
    public static ImageIcon loadImageIcon(String resourcePath) {
       return loadImageIcon(resourcePath, UIUtils.class);
    }

    /**
     * Gets the image icon loaded from the given resource path.
     *
     * @param resourcePath the resource path
     * @param callerClass the class which calls this method and therefore provides the class loader for the requested resource
     *
     * @return an image icon loaded from the given resource path or <code>null</code> if it could not be found
     * @since 4.0
     */
    public static ImageIcon loadImageIcon(String resourcePath, Class callerClass) {
        if (StringUtils.isNotNullAndNotEmpty(resourcePath)) {
            URL location = getImageURL(resourcePath, callerClass);
            return (location != null) ? new ImageIcon(location) : null;
        }
        return null;
    }

    /**
     * Gets the location of the given image resource path as an URL.
     * <p>Note that this method only works for images found in the classpath of the class loader which loaded this {@link UIUtils} class.
     * If you are not sure, you should better use {@link #getImageURL(String, Class)}.</p>
     *
     * @param resourcePath the resource path
     *
     * @return an URL representing the given resource path or <code>null</code> if it could not be found
     */
    public static URL getImageURL(String resourcePath) {
        return getImageURL(resourcePath, UIUtils.class);
    }

    /**
     * Gets the location of the given image resource path as an URL.
     *
     * @param resourcePath the resource path
     * @param callerClass the class which calls this method and therefore provides the class loader for the requested resource
     *
     * @return an URL representing the given resource path or <code>null</code> if it could not be found
     * @since 4.0
     */
    public static URL getImageURL(String resourcePath, Class callerClass) {
        String absResourcePath = resourcePath;
        if (!absResourcePath.startsWith("/")) {
            absResourcePath = IMAGE_RESOURCE_PATH + resourcePath;
        }
        return callerClass.getResource(absResourcePath);
    }

    /**
     * Returns the (main) screen's size in pixels.
     */
    public static Dimension getScreenSize() {
        return Toolkit.getDefaultToolkit().getScreenSize();
    }

    /**
     * Returns the (main) screen's width in pixels.
     */
    public static int getScreenWidth() {
        return getScreenSize().width;
    }

    /**
     * Returns the (main) screen's height in pixels.
     */
    public static int getScreenHeight() {
        return getScreenSize().height;
    }


    /**
     * Centers the given component within the screen area.
     * <p/>
     * <p> The method performs the alignment by setting a newly computed location for the component. It does not alter
     * the component's size.
     *
     * @param comp the component whose location is to be altered
     *
     * @throws IllegalArgumentException if the component is <code>null</code>
     */
    public static void centerComponent(Component comp) {
        centerComponent(comp, null);
    }

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
        Dimension screenSize = getScreenSize();

        int x1, y1;

        if (alignComp != null) {
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

    /**
     * Prevent's instantiation.
     */
    private UIUtils() {
    }

    /**
     * Ensures that the popup menue is allways inside the application frame
     */
    public static void showPopup(JPopupMenu popup, MouseEvent event) {
        if (popup == null) {
            return;
        }
        final Component component = event.getComponent();
        final Point point = event.getPoint();
        popup.show(component, point.x, point.y);
    }

    public static Window getRootWindow(Component component) {
        Guardian.assertNotNull("component", component);
        do {
            Component parent = component.getParent();
            if (parent == null && component instanceof Window) {
                return (Window) component;
            }
            component = parent;
        } while (component != null);
        return null;
    }

    public static Border createGroupBorder(String title) {
        return BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                title);
    }

    public static Frame getRootFrame(Component component) {
        Guardian.assertNotNull("component", component);
        final Window window = getRootWindow(component);
        return (window instanceof Frame) ? (Frame) window : null;
    }

    public static JFrame getRootJFrame(Component component) {
        Guardian.assertNotNull("component", component);
        final Window window = getRootWindow(component);
        return (window instanceof JFrame) ? (JFrame) window : null;
    }

    public static Cursor setRootFrameWaitCursor(Component component) {
        return setRootFrameCursor(component, Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    public static Cursor setRootFrameDefaultCursor(Component component) {
        return setRootFrameCursor(component, Cursor.getDefaultCursor());
    }

    public static Cursor setRootFrameCursor(Component component, Cursor newCursor) {
        Guardian.assertNotNull("component", component);
        Frame frame = getRootFrame(component);
        if (frame == null && component instanceof Frame) {
            frame = (Frame) component;
        }
        Cursor oldCursor = null;
        if (frame != null) {
            oldCursor = frame.getCursor();
            if (newCursor != null) {
                frame.setCursor(newCursor);
            } else {
                frame.setCursor(Cursor.getDefaultCursor());
            }
        }
        return oldCursor;
    }

    public static JMenu findMenu(JMenuBar menuBar, String name, boolean deepSearch) {
        int n = menuBar.getMenuCount();
        for (int i = 0; i < n; i++) {
            JMenu menu = menuBar.getMenu(i);
            if (name.equals(menu.getName())) {
                return menu;
            }
        }
        if (deepSearch) {
            for (int i = 0; i < n; i++) {
                JMenu menu = menuBar.getMenu(i);
                JMenu subMenu = findSubMenu(menu.getPopupMenu(), name);
                if (subMenu != null) {
                    return subMenu;
                }
            }
        }
        return null;
    }

    public static int findMenuPosition(JMenuBar menuBar, String name) {
        int n = menuBar.getMenuCount();
        for (int i = 0; i < n; i++) {
            JMenu menu = menuBar.getMenu(i);
            if (name.equals(menu.getName())) {
                return i;
            }
        }
        return -1;
    }

    public static int findMenuItemPosition(JPopupMenu popupMenu, String name) {
        int n = popupMenu.getComponentCount();
        for (int i = 0; i < n; i++) {
            Component c = popupMenu.getComponent(i);
            if (c instanceof JMenuItem) {
                JMenuItem menuItem = (JMenuItem) c;
                if (name.equals(menuItem.getName())) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static JMenu findSubMenu(JPopupMenu popupMenu, String name) {
        int n = popupMenu.getComponentCount();
        for (int i = 0; i < n; i++) {
            Component c = popupMenu.getComponent(i);
            if (c instanceof JMenu) {
                JMenu subMenu = (JMenu) c;
                if (name.equals(subMenu.getName())) {
                    return subMenu;
                }
                subMenu = findSubMenu(subMenu.getPopupMenu(), name);
                if (subMenu != null) {
                    return subMenu;
                }
            }
        }
        return null;
    }

    public static String getUniqueFrameTitle(final JInternalFrame[] frames, final String titleBase) {
        if (frames.length == 0) {
            return titleBase;
        }
        String[] titles = new String[frames.length];
        for (int i = 0; i < frames.length; i++) {
            JInternalFrame frame = frames[i];
            titles[i] = frame.getTitle();
        }
        if (!ArrayUtils.isMemberOf(titleBase, titles)) {
            return titleBase;
        }
        for (int i = 0; i < frames.length; i++) {
            final String title = titleBase + " (" + (i + 2) + ")";
            if (!ArrayUtils.isMemberOf(title, titles)) {
                return title;
            }
        }
        return titleBase + " (" + (frames.length + 1) + ")";
    }

    public static JSpinner createSpinner(final Parameter param, final Number spinnerStep, final String formatPattern) {
        final Number v = (Number) param.getValue();
        final ParamProperties properties = param.getProperties();
        final Comparable min = (Comparable) properties.getMinValue();
        final Comparable max = (Comparable) properties.getMaxValue();
        final Double bigStep = spinnerStep.doubleValue() * 10;
        final JSpinner spinner = createSpinner(v, min, max, spinnerStep, bigStep, formatPattern);
        spinner.setName(properties.getLabel());
        spinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                param.setValue(spinner.getValue(), null);
            }
        });
        param.addParamChangeListener(new ParamChangeListener() {
            public void parameterValueChanged(ParamChangeEvent event) {
                spinner.setValue(param.getValue());
            }
        });
        param.getEditor().getEditorComponent().addPropertyChangeListener("enabled", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                spinner.setEnabled(((Boolean) evt.getNewValue()).booleanValue());
            }
        });

        properties.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (ParamProperties.MAXVALUE_KEY.equals(evt.getPropertyName())) {
                    ((SpinnerNumberModel) spinner.getModel()).setMaximum((Comparable) properties.getMaxValue());
                }
                if (ParamProperties.MINVALUE_KEY.equals(evt.getPropertyName())) {
                    ((SpinnerNumberModel) spinner.getModel()).setMinimum((Comparable) properties.getMinValue());
                }
            }
        });

        final JSpinner.NumberEditor editor = ((JSpinner.NumberEditor) spinner.getEditor());
        final JFormattedTextField textField = editor.getTextField();
        textField.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                textField.selectAll();
            }

            public void focusLost(FocusEvent e) {
            }
        });

        return spinner;
    }

    public static JSpinner createDoubleSpinner(final double value, final double rangeMinimum,
                                               final double rangeMaximum,
                                               final double stepSize, final double bigStepSize,
                                               final String formatPattern) {

        final SpinnerNumberModel numberModel = new SpinnerNumberModel(value, rangeMinimum, rangeMaximum, stepSize);
        final JSpinner spinner = new JSpinner(numberModel);
        final JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.NumberEditor) {
            JSpinner.NumberEditor numberEditor = (JSpinner.NumberEditor) editor;
            final DecimalFormat format = numberEditor.getFormat();
            format.applyPattern(formatPattern);
            numberEditor.getTextField().setColumns(8);
        }

        final Double sss = stepSize;
        final Double bss = bigStepSize;
        final String bigDec = "dec++";
        final String bigInc = "inc++";

        final InputMap inputMap = spinner.getInputMap();
        final ActionMap actionMap = spinner.getActionMap();

        // big increase with "PAGE_UP" key
        inputMap.put(KeyStroke.getKeyStroke("PAGE_UP"), bigInc);
        actionMap.put(bigInc, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                numberModel.setStepSize(bss);
                numberModel.setValue(numberModel.getNextValue());
                numberModel.setStepSize(sss);
            }
        });

        // big decrease with "PAGE_UP" key
        inputMap.put(KeyStroke.getKeyStroke("PAGE_DOWN"), bigDec);
        actionMap.put(bigDec, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                numberModel.setStepSize(bss);
                numberModel.setValue(numberModel.getPreviousValue());
                numberModel.setStepSize(sss);
            }
        });

        return spinner;
    }

    public static JSpinner createSpinner(final Number value, final Comparable rangeMinimum, final Comparable rangeMaximum,
                                         final Number stepSize, final Number bigStepSize, final String formatPattern) {

        final SpinnerNumberModel numberModel = new SpinnerNumberModel(value, rangeMinimum, rangeMaximum, stepSize);
        final JSpinner spinner = new JSpinner(numberModel);
        final JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.NumberEditor) {
            JSpinner.NumberEditor numberEditor = (JSpinner.NumberEditor) editor;
            final DecimalFormat format = numberEditor.getFormat();
            format.applyPattern(formatPattern);
            numberEditor.getTextField().setColumns(8);
        }
        spinner.setValue(0);
        spinner.setValue(value);

        final String bigDec = "dec++";
        final String bigInc = "inc++";

        final InputMap inputMap = spinner.getInputMap();
        final ActionMap actionMap = spinner.getActionMap();

        // big increase with "PAGE_UP" key
        inputMap.put(KeyStroke.getKeyStroke("PAGE_UP"), bigInc);
        actionMap.put(bigInc, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                numberModel.setStepSize(bigStepSize);
                numberModel.setValue(numberModel.getNextValue());
                numberModel.setStepSize(stepSize);
            }
        });

        // big decrease with "PAGE_UP" key
        inputMap.put(KeyStroke.getKeyStroke("PAGE_DOWN"), bigDec);
        actionMap.put(bigDec, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                numberModel.setStepSize(bigStepSize);
                numberModel.setValue(numberModel.getPreviousValue());
                numberModel.setStepSize(stepSize);
            }
        });

        return spinner;
    }

}
