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
package org.esa.snap.core.datamodel;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.ConfigurableExtension;
import com.bc.ceres.core.runtime.ConfigurationElement;
import org.esa.snap.core.dataop.barithm.BandArithmetic;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
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


    public static final String PROPERTY_KEY_NAME = "name";
    public static final String PROPERTY_KEY_RED = "red";
    public static final String PROPERTY_KEY_GREEN = "green";
    public static final String PROPERTY_KEY_BLUE = "blue";
    public static final String PROPERTY_KEY_ALPHA = "alpha";
    public static final String PROPERTY_KEY_INTERNAL = "internal";

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
    private String[] pattern;

    public RGBImageProfile() {
        this("");
    }

    public RGBImageProfile(final String name) {
        this(name, new String[]{"", "", ""}, null);
    }

    public RGBImageProfile(final String name, String[] rgbaExpressions) {
        this(name, rgbaExpressions, null);
    }

    /**
     * Creates a new RGB profile.
     * In the pattern you can simply use '*' for a sequence of characters or use '?' for a single character.
     * For Example:
     * {@code new String[]{ "MER_*_2*", "MER_*_2*", ""}}
     *
     * @param name The name of the profile
     * @param rgbaExpressions the expressions for the RGBA channels. Only RGB expressions are mandatory, the one for the alpha
     *                        channel can be missing
     * @param pattern Pattern to check if this profile can be applied to a certain product. Three patterns need to be provided.
     *                1. Will be matched against the product type
     *                2. Will be matched against the product name
     *                3. Will be matched against the description of the product
     */
    public RGBImageProfile(final String name, String[] rgbaExpressions, String pattern[]) {
        Assert.argument(name != null, "name != null");
        Assert.argument(rgbaExpressions != null, "rgbaExpressions != null");
        Assert.argument(rgbaExpressions.length == 3 || rgbaExpressions.length == 4,
                        "rgbaExpressions.length == 3 || rgbaExpressions.length == 4");
        if (pattern != null) {
            Assert.argument(pattern.length == 3, "pattern.length == 3");
        }

        this.name = name;
        this.rgbaExpressions = new String[4];
        this.rgbaExpressions[R] = rgbaExpressions[R];
        this.rgbaExpressions[G] = rgbaExpressions[G];
        this.rgbaExpressions[B] = rgbaExpressions[B];
        this.rgbaExpressions[A] = rgbaExpressions.length == 4 ? rgbaExpressions[A] : "";
        this.pattern = pattern;
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
        return rgbaExpressions.clone();
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

    public String[] getPattern() {
        return pattern;
    }

    public void setPattern(String[] pattern) {
        this.pattern = pattern;
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
        final String[] expressions = getRgbExpressions();
        for (final String expression : expressions) {
            if (!expression.equals("")) {
                if (!product.isCompatibleBandArithmeticExpression(expression)) {
                    return false;
                }
            }
        }

        //check if raster size is the same in all the expressions
        try {
            if(!BandArithmetic.areRastersEqualInSize(product, expressions)) {
                return false;
            }
        } catch (ParseException e) {
            return false;
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
            for (final String currentBandName : names) {
                if (rgbaExpressions[i].equals("")) {
                    final Band band = product.getBand(currentBandName);
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
        try (InputStream inStream = new FileInputStream(file)) {
            properties.load(inStream);
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
        try (InputStream inStream = url.openStream()) {
            properties.load(inStream);
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
        try (OutputStream outStream = new FileOutputStream(file)) {
            final Properties properties = new Properties();
            getProperties(properties);
            properties.store(outStream, "RGB-Image Profile");
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
    @SuppressWarnings("LocalVariableHidesMemberVariable")
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
        storeRgbaExpressions(product, rgbaExpressions, RGBImageProfile.RGBA_BAND_NAMES);    
    }

    public static void storeRgbaExpressions(final Product product, final String[] rgbaExpressions, final String[] bandNames) {
        for (int i = 0; i < bandNames.length; i++) {
            final String rgbBandName = bandNames[i];
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
            return getName().equals(profile.getName()) && equalExpressions(profile) &&
                   Arrays.equals(getPattern(), profile.getPattern());
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
        for (String key : keys) {
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
        ConfigurationElement patternConfig = config.getChild("pattern");
        if (patternConfig != null) {
            pattern = new String[3];
            ConfigurationElement productType = patternConfig.getChild("productType");
            ConfigurationElement productName = patternConfig.getChild("productName");
            ConfigurationElement productDesc = patternConfig.getChild("productDesc");
            if (productType != null) {
                pattern[0] = productType.getValue();
            }
            if (productName != null) {
                pattern[1] = productName.getValue();
            }
            if (productDesc != null) {
                pattern[2] = productDesc.getValue();
            }
        }
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
