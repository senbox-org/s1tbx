package org.esa.beam.framework.ui.color;

import com.jidesoft.popup.JidePopup;

import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


/**
 * A combo box for color values.
 *
 * @author Norman Fomferra
 * @since SNAP 2.0
 */
public class ColorComboBox extends JComponent {

    public static final String SELECTED_COLOR_PROPERTY = "selectedColor";

    public static final Color TRANSPARENCY = new Color(0, 0, 0, 0);

    private JidePopup popupWindow;
    private ColorLabel colorLabel;
    private Color selectedColor;
    private ColorChooserPanelFactory colorChooserPanelFactory;

    public ColorComboBox() {
        this(Color.WHITE);
    }

    public ColorComboBox(Color color) {
        selectedColor = color;
        colorLabel = new ColorLabel(selectedColor);
        colorLabel.setPreferredSize(new Dimension(16, 16));
        colorLabel.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                showPopupWindow();
            }
        });

        setPreferredSize(new Dimension(60, 20));
        setLayout(new BorderLayout());
        add(colorLabel, BorderLayout.CENTER);
        setFocusable(true);
    }

    public ColorChooserPanelFactory getColorChooserPanelFactory() {
        return colorChooserPanelFactory;
    }

    public void setColorChooserPanelFactory(ColorChooserPanelFactory colorChooserPanelFactory) {
        this.colorChooserPanelFactory = colorChooserPanelFactory;
    }

    public Color getSelectedColor() {
        return selectedColor;
    }

    public void setSelectedColor(Color selectedColor) {
        Color oldValue = this.selectedColor;
        this.selectedColor = selectedColor;
        colorLabel.setColor(selectedColor);
        firePropertyChange(SELECTED_COLOR_PROPERTY, oldValue, this.selectedColor);
    }

    private ColorChooserPanel createColorChooserPanel() {
        if (colorChooserPanelFactory != null) {
            return colorChooserPanelFactory.create(getSelectedColor());
        }
        return new ColorChooserPanel(getSelectedColor());
    }

    private void showPopupWindow() {
        if (popupWindow != null && popupWindow.isShowing()) {
            closePopupWindow();
            return;
        }

        Point location = getLocationOnScreen();
        location.y += getHeight();

        ColorChooserPanel colorChooserPanel = createColorChooserPanel();
        colorChooserPanel.addPropertyChangeListener(ColorChooserPanel.SELECTED_COLOR_PROPERTY, evt -> {
            setSelectedColor(colorChooserPanel.getSelectedColor());
            closePopupWindow();
        });

        popupWindow = new JidePopup();
        popupWindow.setOwner(this);
        popupWindow.getContentPane().add(colorChooserPanel);
        popupWindow.setDefaultFocusComponent(colorChooserPanel);
        popupWindow.setMovable(false);
        popupWindow.setAttachable(false);
        popupWindow.showPopup(location.x, location.y);
    }

    protected void closePopupWindow() {
        if (popupWindow != null) {
            popupWindow.hidePopupImmediately();
            popupWindow = null;
        }
    }
}
