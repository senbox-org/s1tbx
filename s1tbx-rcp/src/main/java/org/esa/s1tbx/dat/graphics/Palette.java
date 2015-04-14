package org.esa.s1tbx.dat.graphics;

import java.awt.Color;

/**
 * Create color maps
 */
public class Palette {

    // predefined color maps
    public final static Palette[] STANDARD_MAPS =
            {
                    new Palette("Rainbow", new Color[]
                            {Color.blue, Color.cyan, Color.green, Color.yellow, Color.orange, Color.red}),
                    new Palette("Cool", new Color[]
                            {Color.green, Color.blue, new Color(255, 0, 255)}),
                    new Palette("Warm", new Color[]
                            {Color.red, Color.orange, Color.yellow}),
                    new Palette("Thermal", new Color[]
                            {Color.black, Color.red, Color.orange, Color.yellow, Color.green,
                                    Color.blue, new Color(255, 0, 255), Color.white})
            };

    private Color[] colors;
    private final String name;

    public Palette(String name, Color[] colors) {
        this.colors = colors;
        this.name = name;
    }

    public Color[] getColors() {
        return colors;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the color corresponding to the specified value. The
     * argument is assumed to fall between 0 and 1 for interpolation. If
     * <code>val</code> is less than 0 or greater than 1, the start or
     * end colors will be returned, respectively.
     *
     * @param percent the value
     * @return the corresponding color
     */
    public Color lookupColor(final float percent) {
        int length = colors.length - 1;

        if (percent < 0.f)
            return colors[0];
        if (percent >= 1.f)
            return colors[length];

        final int pos = (int) (percent * length);
        final Color s = colors[pos];
        final Color e = colors[pos + 1];
        final float rem = percent * length - pos;
        return new Color(
                s.getRed() + (int) (rem * (e.getRed() - s.getRed())),
                s.getGreen() + (int) (rem * (e.getGreen() - s.getGreen())),
                s.getBlue() + (int) (rem * (e.getBlue() - s.getBlue())));
    }
}
