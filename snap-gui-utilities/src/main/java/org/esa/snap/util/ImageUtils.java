package org.esa.snap.util;

import javax.swing.*;

/**
 * Created by luis on 08/02/2015.
 */
public class ImageUtils {
    public static ImageIcon esaIcon = LoadIcon("org/esa/snap/icons/esa.png");
    public static ImageIcon rstbIcon = LoadIcon("org/esa/snap/icons/csa.png");
    public static ImageIcon arrayIcon = LoadIcon("org/esa/snap/icons/array_logo.png");
    public static ImageIcon esaPlanetIcon = LoadIcon("org/esa/snap/icons/esa-planet.png");
    public static ImageIcon geoAusIcon = LoadIcon("org/esa/snap/icons/geo_aus.png");

    public static ImageIcon LoadIcon(final String path) {
        final java.net.URL imageURL = ImageUtils.class.getClassLoader().getResource(path);
        if (imageURL == null) return null;
        return new ImageIcon(imageURL);
    }
}
