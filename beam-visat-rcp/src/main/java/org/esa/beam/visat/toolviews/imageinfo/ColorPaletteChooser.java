package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.math.Range;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Vector;

class ColorPaletteChooser extends JComboBox<ColorPaletteChooser.ColorPaletteWrapper> {

    private final String DERIVED_FROM = "derived from";
    private final String UNNAMED = "unnamed";
    private boolean discreteDisplay;
    private boolean log10Display;

    public ColorPaletteChooser() {
        super(getPalettes());
        setRenderer(createPaletteRenderer());
        setEditable(false);
    }

    public void removeUserDefinedPalette() {
        final String name = getItemAt(0).name;
        if (UNNAMED.equals(name) || name.startsWith(DERIVED_FROM)) {
            removeItemAt(0);
        }
    }

    public ColorPaletteDef getSelectedColorPaletteDefinition() {
        final int selectedIndex = getSelectedIndex();
        final ComboBoxModel<ColorPaletteWrapper> model = getModel();
        final ColorPaletteWrapper colorPaletteWrapper = model.getElementAt(selectedIndex);
        final ColorPaletteDef cpd = colorPaletteWrapper.cpd;
        cpd.getFirstPoint().setLabel(colorPaletteWrapper.name);
        return cpd;
    }

    public void setSelectedColorPaletteDefinition(ColorPaletteDef cpd) {
        removeUserDefinedPalette();
        final ComboBoxModel<ColorPaletteWrapper> model = getModel();
        for (int i = 0; i < model.getSize(); i++) {
            if (model.getElementAt(i).cpd.equals(cpd)) {
                setSelectedIndex(i);
                return;
            }
        }
        setUserDefinedPalette(cpd);
    }

    private void setUserDefinedPalette(ColorPaletteDef userPalette) {
        final String suffix = userPalette.getFirstPoint().getLabel();
        final String name;
        if (suffix != null && suffix.trim().length() > 0) {
            name = DERIVED_FROM + " " + suffix.trim();
        } else {
            name = UNNAMED;
        }
        final ColorPaletteWrapper item = new ColorPaletteWrapper(name, userPalette);
        insertItemAt(item, 0);
        setSelectedIndex(0);
    }

    private static Vector<ColorPaletteWrapper> getPalettes() {
        final List<ColorPaletteDef> defList = ColorPalettesManager.getColorPaletteDefList();
        final Vector<ColorPaletteWrapper> paletteWrappers = new Vector<>();
        for (ColorPaletteDef colorPaletteDef : defList) {
            final String nameFor = getNameForWithoutExtension(colorPaletteDef);
            paletteWrappers.add(new ColorPaletteWrapper(nameFor, colorPaletteDef));
        }
        return paletteWrappers;
    }

    private static String getNameForWithoutExtension(ColorPaletteDef colorPaletteDef) {
        final String nameFor = ColorPalettesManager.getNameFor(colorPaletteDef);
        if (nameFor.toLowerCase().endsWith(".cpd")) {
            return nameFor.substring(0, nameFor.length() - 4);
        } else {
            return nameFor;
        }
    }

    private ListCellRenderer<ColorPaletteWrapper> createPaletteRenderer() {
        return new ListCellRenderer<ColorPaletteWrapper>() {
            @Override
            public Component getListCellRendererComponent(JList<? extends ColorPaletteWrapper> list, ColorPaletteWrapper value, int index, boolean isSelected, boolean cellHasFocus) {
                final Font font = getFont();
                final Font smaller = font.deriveFont(font.getSize() * 0.85f);

                final JLabel nameComp = new JLabel(value.name);
                nameComp.setFont(smaller);

                final ColorPaletteDef cpd = value.cpd;
                final JLabel rampComp = new JLabel(" ") {
                    @Override
                    public void paint(Graphics g) {
                        super.paint(g);
                        drawPalette((Graphics2D) g, cpd, g.getClipBounds().getSize());
                    }
                };

                final JPanel palettePanel = new JPanel(new BorderLayout(0, 2));
                palettePanel.add(nameComp, BorderLayout.NORTH);
                palettePanel.add(rampComp, BorderLayout.CENTER);

                return palettePanel;
            }
        };
    }

    private void drawPalette(Graphics2D g2, ColorPaletteDef colorPaletteDef, Dimension paletteDim) {
        final int width = paletteDim.width;
        final int height = paletteDim.height;

        final ColorPaletteDef cpdCopy = colorPaletteDef.createDeepCopy();
        cpdCopy.setDiscrete(discreteDisplay);
        cpdCopy.setNumColors(width);
        final ImageInfo imageInfo = new ImageInfo(cpdCopy);
        imageInfo.setLogScaled(log10Display);

        Color[] colorPalette = ImageManager.createColorPalette(imageInfo);

        g2.setStroke(new BasicStroke(1.0f));

        for (int x = 0; x < width; x++) {
            g2.setColor(colorPalette[x]);
            g2.drawLine(x, 0, x, height);
        }
    }

    public void setLog10Display(boolean log10Display) {
        this.log10Display = log10Display;
        repaint();
    }

    public void setDiscreteDisplay(boolean discreteDisplay) {
        this.discreteDisplay = discreteDisplay;
        repaint();
    }

    public Range getRangeFromFile() {
        final ComboBoxModel<ColorPaletteWrapper> model = getModel();
        final int selectedIndex = getSelectedIndex();
        final ColorPaletteWrapper paletteWrapper = model.getElementAt(selectedIndex);
        String name = paletteWrapper.name;
        final ColorPaletteDef cpd;
        if (name.startsWith(DERIVED_FROM)) {
            name = name.substring(DERIVED_FROM.length() + 2, name.length() - 1).trim();
            cpd = findColorPalette(name);
        } else {
            cpd = paletteWrapper.cpd;
        }
        return new Range(cpd.getMinDisplaySample(), cpd.getMaxDisplaySample());
    }

    private ColorPaletteDef findColorPalette(String name) {
        final ComboBoxModel<ColorPaletteWrapper> model = getModel();
        for (int i = 0; i < model.getSize(); i++) {
            final ColorPaletteWrapper paletteWrapper = model.getElementAt(i);
            if (paletteWrapper.name.equals(name)) {
                return paletteWrapper.cpd;
            }
        }
        return null;
    }

    public static final class ColorPaletteWrapper {

        public final String name;

        public final ColorPaletteDef cpd;

        private ColorPaletteWrapper(String name, ColorPaletteDef cpd) {
            this.name = name;
            this.cpd = cpd;
        }
    }
}
