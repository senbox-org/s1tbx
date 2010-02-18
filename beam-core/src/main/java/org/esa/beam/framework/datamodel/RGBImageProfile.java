/*
 * $id$
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.framework.datamodel;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.ConfigurableExtension;
import com.bc.ceres.core.runtime.ConfigurationElement;
import com.bc.jexp.Parser;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Properties;

/**
 * A profile used for the creation of RGB images. The profile comprises the band arithmetic expressions
 * for the computation of red, gree, blue and alpha (optional) channels of the resulting image.
 */
public class RGBImageProfile implements ConfigurableExtension {

    /**
     * The default name for the band providing input for the red image channel.
     */
    public static final String RED_BAND_NAME = "virtual_red";
    /**
     * The default name for the band providing input for the green image channel.
     */
    public static final String GREEN_BAND_NAME = "virtual_green";
    /**
     * The default name for the band providing input for the blue image channel.
     */
    public static final String BLUE_BAND_NAME = "virtual_blue";
    /**
     * The default name for the band providing input for the alpha image channel.
     */
    public static final String ALPHA_BAND_NAME = "virtual_alpha";

    /**
     * An array of 3 strings containing the names for the default red, green and blue bands.
     */
    public static final String[] RGB_BAND_NAMES = new String[]{
            RED_BAND_NAME,
            GREEN_BAND_NAME,
            BLUE_BAND_NAME
    };

    /**
     * An array of 4 strings containing the names for the default red, green, blue and alpha bands.
     */
    public static final String[] RGBA_BAND_NAMES = new String[]{
            RED_BAND_NAME,
            GREEN_BAND_NAME,
            BLUE_BAND_NAME,
            ALPHA_BAND_NAME,
    };

    public static final String FILENAME_EXTENSION = ".rgb";


    public final static String PROPERTY_KEY_NAME = "name";
    public final static String PROPERTY_KEY_RED = "red";
    public final static String PROPERTY_KEY_GREEN = "green";
    public final static String PROPERTY_KEY_BLUE = "blue";
    public final static String PROPERTY_KEY_ALPHA = "alpha";
    public final static String PROPERTY_KEY_INTERNAL = "internal";

    /**
     * Preferences key for RGB profile entries
     */
    public final static String PROPERTY_KEY_PREFIX_RGB_PROFILE = "rgbProfile";

    private final static int R = 0;
    private final static int G = 1;
    private final static int B = 2;
    private final static int A = 3;

    private String name;
    private boolean internal;
    private String[] rgbaExpressions;

    public RGBImageProfile() {
        this("");
    }

    public RGBImageProfile(final String name) {
        this(name, new String[]{"", "", ""});
    }

    public RGBImageProfile(final String name, String[] rgbaExpressions) {
        Guardian.assertTrue("name != null",
                            name != null);
        Guardian.assertTrue("rgbaExpressions != null",
                            rgbaExpressions != null);
        Guardian.assertTrue("rgbaExpressions.length == 3 || rgbaExpressions.length == 4",
                            rgbaExpressions.length == 3 || rgbaExpressions.length == 4);

        this.name = name;
        this.rgbaExpressions = new String[4];
        this.rgbaExpressions[R] = rgbaExpressions[R];
        this.rgbaExpressions[G] = rgbaExpressions[G];
        this.rgbaExpressions[B] = rgbaExpressions[B];
        this.rgbaExpressions[A] = rgbaExpressions.length == 4 ? rgbaExpressions[A] : "";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(final boolean internal) {
        this.internal = internal;
    }

    public boolean equalExpressions(RGBImageProfile profile) {
        return equalExpressions(profile.rgbaExpressions);
    }

    public boolean equalExpressions(String[] rgbaExpressions) {
        for (int i = 0; i < this.rgbaExpressions.length; i++) {
            if (!this.rgbaExpressions[i].equals(rgbaExpressions[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tests if one of the R,G,B expressions are non-empty strings.
     *
     * @return true, if so
     */
    public boolean isValid() {
        return !(getRedExpression().equals("") && getGreenExpression().equals("") && getBlueExpression().equals(""));
    }

    public String[] getRgbExpressions() {
        String[] copy = new String[rgbaExpressions.length - 1];
        copy[R] = rgbaExpressions[R];
        copy[G] = rgbaExpressions[G];
        copy[B] = rgbaExpressions[B];
        return copy;
    }

    public void setRgbExpressions(String[] rgbExpressions) {
        rgbaExpressions[R] = rgbExpressions[R];
        rgbaExpressions[G] = rgbExpressions[G];
        rgbaExpressions[B] = rgbExpressions[B];
    }

    public String[] getRgbaExpressions() {
        return (String[]) rgbaExpressions.clone();
    }

    public void setRgbaExpressions(String[] rgbaExpressions) {
        this.rgbaExpressions[R] = rgbaExpressions[R];
        this.rgbaExpressions[G] = rgbaExpressions[G];
        this.rgbaExpressions[B] = rgbaExpressions[B];
        this.rgbaExpressions[A] = rgbaExpressions[A];
    }

    public String getRedExpression() {
        return rgbaExpressions[R];
    }

    public void setRedExpression(String expression) {
        rgbaExpressions[R] = checkAndTrimExpressionArgument(expression);
    }

    public String getGreenExpression() {
        return rgbaExpressions[G];
    }

    public void setGreenExpression(String expression) {
        rgbaExpressions[G] = checkAndTrimExpressionArgument(expression);
    }

    public String getBlueExpression() {
        return rgbaExpressions[B];
    }

    public void setBlueExpression(String expression) {
        rgbaExpressions[B] = checkAndTrimExpressionArgument(expression);
    }

    public String getAlphaExpression() {
        return rgbaExpressions[A];
    }

    public void setAlphaExpression(String expression) {
        rgbaExpressions[A] = checkAndTrimExpressionArgument(expression);
    }

    public boolean hasAlpha() {
        return !getAlphaExpression().equals("");
    }

    /**
     * Tests whether this profile is applicable to the given product. With other words, the method tests
     * if an RGB image can be created from the given product.
     *
     * @param product the product
     *
     * @return true, if so
     */
    public boolean isApplicableTo(final Product product) {
        Guardian.assertNotNull("product", product);
        if (!isValid()) {
            return false;
        }
        final Parser parser = product.createBandArithmeticParser();
        final String[] expressions = getRgbExpressions();
        for (int i = 0; i < expressions.length; i++) {
            final String expression = expressions[i];
            if (!expression.equals("")) {
                if (!product.isCompatibleBandArithmeticExpression(expression, parser)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static RGBImageProfile getCurrentProfile(final Product product) {
        RGBImageProfile profile = new RGBImageProfile("Current Profile");
        String[] rgbaExpressions = new String[]{"", "", "", ""};
        final String[] rBandNames = new String[]{RED_BAND_NAME, PROPERTY_KEY_RED, "r"};
        final String[] gBandNames = new String[]{GREEN_BAND_NAME, PROPERTY_KEY_GREEN, "g"};
        final String[] bBandNames = new String[]{BLUE_BAND_NAME, PROPERTY_KEY_BLUE, "b"};
        final String[] aBandNames = new String[]{ALPHA_BAND_NAME, PROPERTY_KEY_ALPHA, "a"};
        final String[][] allBandNames = new String[][]{rBandNames, gBandNames, bBandNames, aBandNames};
        for (int i = 0; i < allBandNames.length; i++) {
            final String[] names = allBandNames[i];
            for (int j = 0; j < names.length; j++) {
                if (rgbaExpressions[i].equals("")) {
                    final String bandName = names[j];
                    final Band band = product.getBand(bandName);
                    if (band != null) {
                        if (band instanceof VirtualBand) {
                            rgbaExpressions[i] = ((VirtualBand) band).getExpression();
                        } else {
                            rgbaExpressions[i] = band.getName();
                        }
                    }
                }
            }
        }
        profile.setRgbExpressions(rgbaExpressions);
        return profile;
    }

    /**
     * Loads a profile from the given file using the Java properties file format
     *
     * @param file the file
     *
     * @return the profile, never null
     *
     * @throws IOException if an I/O error occurs
     * @see #setProperties(java.util.Properties)
     */
    public static RGBImageProfile loadProfile(final File file) throws IOException {
        Properties properties = new Properties();
        final InputStream inStream = new FileInputStream(file);
        try {
            properties.load(inStream);
        } finally {
            inStream.close();
        }
        final String defaultName = FileUtils.getFilenameWithoutExtension(file);
        final RGBImageProfile profile = new RGBImageProfile(defaultName);
        profile.setProperties(properties);
        return profile;
    }

    /**
     * Loads a profile from the given url using the Java properties file format
     *
     * @param url the url
     *
     * @return the profile, never null
     *
     * @throws IOException if an I/O error occurs
     * @see #setProperties(java.util.Properties)
     */
    public static RGBImageProfile loadProfile(final URL url) throws IOException {
        Properties properties = new Properties();
        final InputStream inStream = url.openStream();
        try {
            properties.load(inStream);
        } finally {
            inStream.close();
        }
        String urlExtForm = url.toExternalForm();
        int lastPathSeperatorIndex = urlExtForm.lastIndexOf('/');
        int extensionDotIndex = urlExtForm.lastIndexOf('.');
        final String defaultName = urlExtForm.substring(lastPathSeperatorIndex + 1, extensionDotIndex);
        final RGBImageProfile profile = new RGBImageProfile(defaultName);
        profile.setProperties(properties);
        return profile;
    }

    /**
     * Stores this profile in the given file using the Java properties file format
     *
     * @param file the file
     *
     * @throws IOException if an I/O error occurs
     * @see #getProperties(java.util.Properties)
     */
    public void store(final File file) throws IOException {
        final OutputStream outStream = new FileOutputStream(file);
        try {
            final Properties properties = new Properties();
            getProperties(properties);
            properties.store(outStream, "RGB-Image Profile");
        } finally {
            outStream.close();
        }
    }

    /**
     * Sets profile properties and accoringly sets them in the given property map.
     *
     * @param properties the property map which receives the properties of this profiles
     */
    public void getProperties(final Properties properties) {
        properties.put(PROPERTY_KEY_RED, getRedExpression());
        properties.put(PROPERTY_KEY_GREEN, getGreenExpression());
        properties.put(PROPERTY_KEY_BLUE, getBlueExpression());
        if (!getAlphaExpression().equals("")) {
            properties.put(PROPERTY_KEY_ALPHA, getAlphaExpression());
        } else {
            properties.remove(PROPERTY_KEY_ALPHA);
        }
        properties.put(PROPERTY_KEY_NAME, getName());
        if (isInternal()) {
            properties.put(PROPERTY_KEY_INTERNAL, isInternal() ? "true" : "false");
        } else {
            properties.remove(PROPERTY_KEY_INTERNAL);
        }
    }

    /**
     * Sets profile properties from the given property map.
     *
     * @param properties the property map which provides the properties for this profiles
     */
    public void setProperties(Properties properties) {
        final String name = properties.getProperty(PROPERTY_KEY_NAME);
        final String[] rgbaExpressions = new String[]{
                getProperty(properties, new String[]{PROPERTY_KEY_RED, "r"}, ""),
                getProperty(properties, new String[]{PROPERTY_KEY_GREEN, "g"}, ""),
                getProperty(properties, new String[]{PROPERTY_KEY_BLUE, "b"}, ""),
                getProperty(properties, new String[]{PROPERTY_KEY_ALPHA, "a"}, "")
        };
        final boolean internal = Boolean.parseBoolean(properties.getProperty(PROPERTY_KEY_INTERNAL, "false"));

        if (name != null) {
            setName(name);
        }
        setInternal(internal);
        setRgbaExpressions(rgbaExpressions);
    }

    public static void storeRgbaExpressions(final Product product, final String[] rgbaExpressions) {
        for (int i = 0; i < RGBImageProfile.RGBA_BAND_NAMES.length; i++) {
            final String rgbBandName = RGBImageProfile.RGBA_BAND_NAMES[i];
            final String rgbaExpression = rgbaExpressions[i];
            final Band rgbBand = product.getBand(rgbBandName);
            final boolean expressionIsEmpty = rgbaExpression.equals("");
            final boolean alphaChannel = i == 3;

            if (rgbBand != null) { // band already exists
                if (rgbBand instanceof VirtualBand) {
                    VirtualBand virtualBand = (VirtualBand) rgbBand;
                    virtualBand.setExpression(rgbaExpression);
                } else {
                    product.removeBand(rgbBand);
                    product.addBand(new VirtualBand(rgbBandName,
                                                    ProductData.TYPE_FLOAT32,
                                                    product.getSceneRasterWidth(),
                                                    product.getSceneRasterHeight(),
                                                    expressionIsEmpty ? "0" : rgbaExpression));
                }
            } else { // band does not exist
                if (!alphaChannel || !expressionIsEmpty) { // don't add empty alpha channels
                    product.addBand(new VirtualBand(rgbBandName,
                                                    ProductData.TYPE_FLOAT32,
                                                    product.getSceneRasterWidth(),
                                                    product.getSceneRasterHeight(),
                                                    expressionIsEmpty ? "0" : rgbaExpression));
                }
            }
        }
    }

    @Override
    public int hashCode() {
        return getRedExpression().hashCode() +
               getGreenExpression().hashCode() +
               getBlueExpression().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof RGBImageProfile) {
            RGBImageProfile profile = (RGBImageProfile) obj;
            return getName().equals(profile.getName()) && equalExpressions(profile);
        }
        return false;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[" +
               "name=" + name + ", " +
               "r=" + rgbaExpressions[0] + ", " +
               "g=" + rgbaExpressions[1] + ", " +
               "b=" + rgbaExpressions[2] + ", " +
               "a=" + rgbaExpressions[3] +
               "]";
    }


    private static String checkAndTrimExpressionArgument(String expression) {
        if (expression == null) {
            throw new IllegalArgumentException("expression is null");
        }
        return expression.trim();
    }

    private static String getProperty(Properties properties, String[] keys, String defaultValue) {
        String value = null;
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            value = properties.getProperty(key);
            if (value != null) {
                break;
            }
        }
        return value != null ? value : defaultValue;
    }

    public void configure(ConfigurationElement config) throws CoreException {

        name = getChildValue(config, "name");
        internal = true;
        rgbaExpressions[R] = getChildValue(config, "red");
        rgbaExpressions[G] = getChildValue(config, "green");
        rgbaExpressions[B] = getChildValue(config, "blue");

        ConfigurationElement child = config.getChild("alpha");
        String alpha = null;
        if (child != null) {
            alpha = child.getValue();
        }
        if (alpha == null) {
            alpha = "";
        }
        rgbaExpressions[A] = alpha;
    }

    private static String getChildValue(ConfigurationElement config, String childName) throws CoreException {
        ConfigurationElement child = config.getChild(childName);
        if (child != null) {
            return child.getValue();
        } else {
            throw new CoreException("Configuration element [" + childName + "] does not exist");
        }
    }
}
