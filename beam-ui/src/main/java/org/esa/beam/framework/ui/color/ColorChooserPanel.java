package org.esa.beam.framework.ui.color;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A color chooser panel.
 *
 * @author Norman Fomferra
 * @author Marco Peters
 * @since SNAP 2.0
 */
public class ColorChooserPanel extends JPanel {
    public static final String SELECTED_COLOR_PROPERTY = "selectedColor";
    static final Color TRANSPARENCY = new Color(0, 0, 0, 0);
    private static final int GAP = 2;

    private Color selectedColor;

    public ColorChooserPanel() {
        this(Color.WHITE);
    }

    public ColorChooserPanel(Color selectedColor) {
        super(new BorderLayout(GAP, GAP));
        setBorder(new EmptyBorder(GAP, GAP, GAP, GAP));

        setSelectedColor(selectedColor);

        JButton noneButton = new JButton("None");
        noneButton.addActionListener(e -> {
            setSelectedColor(TRANSPARENCY);
        });

        JButton moreButton = new JButton("More...");
        moreButton.addActionListener(e -> {
            Color color = showMoreColorsDialog();
            if (color != null) {
                setSelectedColor(color);
            }
        });

        add(noneButton, BorderLayout.NORTH);
        add(createColorPicker(), BorderLayout.CENTER);
        add(moreButton, BorderLayout.SOUTH);
        // todo - use colors from popup menu LAF
        setBackground(Color.WHITE);
    }

    public Color getSelectedColor() {
        return selectedColor;
    }

    public void setSelectedColor(Color selectedColor) {
        Color oldValue = this.selectedColor;
        this.selectedColor = selectedColor;
        firePropertyChange(SELECTED_COLOR_PROPERTY, oldValue, this.selectedColor);
    }

    protected JComponent createColorPicker() {

        Color[] colors = {Color.BLACK,
                Color.DARK_GRAY,
                Color.GRAY,
                Color.LIGHT_GRAY,
                Color.WHITE,
                Color.CYAN,
                Color.BLUE,
                Color.MAGENTA,
                Color.YELLOW,
                Color.ORANGE,
                Color.RED,
                Color.PINK,
                Color.GREEN};


        JPanel colorsPanel = new JPanel(new GridLayout(-1, 6, 4, 4));
        colorsPanel.setOpaque(false);
        for (Color color : colors) {
            ColorLabel colorLabel = new ColorLabel(color);
            colorLabel.setHoverEnabled(true);
            colorLabel.setMaximumSize(colorLabel.getPreferredSize());
            colorLabel.setMinimumSize(colorLabel.getPreferredSize());
            colorLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    setSelectedColor(colorLabel.getColor());
                }
            });
            colorsPanel.add(colorLabel);
        }
        return colorsPanel;
    }

    protected Color showMoreColorsDialog() {
        return JColorChooser.showDialog(this, "Select Colour", getSelectedColor());
    }
}
