/*
 * $id$
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.framework.datamodel;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.ConfigurableExtension;
import com.bc.ceres.core.runtime.ConfigurationElement;
import com.bc.jexp.Parser;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.math.Range;
import org.esa.beam.util.io.FileUtils;

import java.io.*;
import java.net.URL;
import java.util.Properties;
import java.util.Arrays;

/**
 * A profile used for the creation of RGB images. The profile comprises the band arithmetic expressions
 * for the computation of red, gree, blue and alpha (optional) channels of the resulting image.
 */
public class RGBImageProfile implements Cloneable, ConfigurableExtension {

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
    private String[] expressions;
    // todo - include in module and DIMAP XML (nf/mp - 26.06.2008)
    private Range[] sampleDisplayRanges;
    // todo - include in module and DIMAP XML (nf/mp - 26.06.2008)
    private double[] sampleDisplayGammas;

    public RGBImageProfile() {
        this("");
    }

    public RGBImageProfile(final String name) {
        this(name, new String[]{"", "", ""});
    }

    public RGBImageProfile(final String name, String[] expressions) {
        Guardian.assertTrue("name != null",
                            name != null);
        Guardian.assertTrue("rgbaExpressions != null",
                            expressions != null);
        Guardian.assertTrue("rgbaExpressions.length == 3 || rgbaExpressions.length == 4",
                            expressions.length == 3 || expressions.length == 4);

        this.name = name;
        this.expressions = new String[4];
        this.sampleDisplayGammas = new double[]{1, 1, 1, 1};
        this.sampleDisplayRanges = new Range[4];
        setExpressions(expressions);
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

    public String[] getExpressions() {
        return expressions.clone();
    }

    public void setExpressions(String[] expressions) {
        setExpression(R, expressions[R]);
        setExpression(G, expressions[G]);
        setExpression(B, expressions[B]);
        setExpression(A, expressions.length > 3 ? expressions[A] : "");
    }

    @Deprecated
    public boolean equalExpressions(RGBImageProfile profile) {
        return Arrays.equals(this.expressions, profile.expressions);
    }

    @Deprecated
    public boolean equalExpressions(String[] expressions) {
        return Arrays.equals(this.expressions, expressions);
    }

    /**
     * Tests if one of the R,G,B expressions are non-empty strings.
     *
     * @return true, if so
     */
    public boolean isValid() {
        return !(getRedExpression().equals("") && getGreenExpression().equals("") && getBlueExpression().equals(""));
    }

    @Deprecated
    public String[] getRgbExpressions() {
        return getExpressions();
    }

    @Deprecated
    public void setRgbExpressions(String[] rgbExpressions) {
        setExpressions(rgbExpressions);
    }

    @Deprecated
    public String[] getRgbaExpressions() {
        return expressions.clone();
    }

    @Deprecated
    public void setRgbaExpressions(String[] rgbaExpressions) {
        setExpressions(rgbaExpressions);
    }

    public String getExpression(int index) {
        return expressions[index];
    }

    public void setExpression(int index, String expression) {
        expressions[index] = checkAndTrim(expression);
    }

    @Deprecated
    public String getRedExpression() {
        return getExpression(R);
    }

    @Deprecated
    public void setRedExpression(String expression) {
        setExpression(R, expression);
    }

    @Deprecated
    public String getGreenExpression() {
        return getExpression(G);
    }

    @Deprecated
    public void setGreenExpression(String expression) {
        setExpression(G, expression);
    }

    @Deprecated
    public String getBlueExpression() {
        return getExpression(B);
    }

    @Deprecated
    public void setBlueExpression(String expression) {
        setExpression(B, expression);
    }

    @Deprecated
    public String getAlphaExpression() {
        return getExpression(A);
    }

    @Deprecated
    public void setAlphaExpression(String expression) {
        setExpression(A, expression);
    }

    public boolean hasAlpha() {
        return !getExpression(A).equals("");
    }

    public boolean isSampleDisplayGammaActive(int index) {
        double gamma = getSampleDisplayGamma(index);
        return Math.abs(gamma - 1.0) < 1e-8;
    }

    public double getSampleDisplayGamma(int index) {
        return sampleDisplayGammas[index];
    }

    public void setSampleDisplayGamma(int index, double gamma) {
        Assert.argument(gamma >= 0.0, "gamma");
        this.sampleDisplayGammas[index] = gamma;
    }

    public Range getSampleDisplayRange(int index) {
        Range range = sampleDisplayRanges[index];
        return range != null ? new Range(range.getMin(), range.getMax()) : null;
    }

    public void setSampleDisplayRange(int index, Range range) {
        this.sampleDisplayRanges[index] = range != null ? new Range(range.getMin(), range.getMax()) : null;
    }

    /**
     * Tests whether this profile is applicable to the given product. With other words, the method tests
     * if an RGB image can be created from the given product.
     *
     * @param product the product
     * @return true, if so
     */
    public boolean isApplicableTo(final Product product) {
        boolean allEmpty = true;
        for (final String expression : expressions) {
            if (!"".equals(expression)) {
                allEmpty = false;
            }
        }
        if (allEmpty) {
            return false;
        }

        Guardian.assertNotNull("product", product);
        final Parser parser = product.createBandArithmeticParser();
        for (final String expression : expressions) {
            if (!"".equals(expression) && !product.isCompatibleBandArithmeticExpression(expression, parser)) {
                return false;
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
            final String[] bandNames = allBandNames[i];
            for (String bandName : bandNames) {
                if (rgbaExpressions[i].equals("")) {
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
     * @return the profile, never null
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
     * @return the profile, never null
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


    /**
     * Creates and returns a copy of this object.
     *
     * @return a copy of this object
     */
    @Override
    public final Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RGBImageProfile that = (RGBImageProfile) o;

        if (internal != that.internal) {
            return false;
        }
        if (!Arrays.equals(expressions, that.expressions)) {
            return false;
        }
        if (!Arrays.equals(sampleDisplayGammas, that.sampleDisplayGammas)) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if (!Arrays.equals(sampleDisplayRanges, that.sampleDisplayRanges)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (name != null ? name.hashCode() : 0);
        result = 31 * result + (internal ? 1 : 0);
        result = 31 * result + (expressions != null ? Arrays.hashCode(expressions) : 0);
        result = 31 * result + (sampleDisplayRanges != null ? Arrays.hashCode(sampleDisplayRanges) : 0);
        result = 31 * result + (sampleDisplayGammas != null ? Arrays.hashCode(sampleDisplayGammas) : 0);
        return result;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[" +
                "name=" + name + ", " +
                "r=" + expressions[0] + ", " +
                "g=" + expressions[1] + ", " +
                "b=" + expressions[2] + ", " +
                "a=" + expressions[3] +
                "]";
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
        expressions[R] = getChildValue(config, "red");
        expressions[G] = getChildValue(config, "green");
        expressions[B] = getChildValue(config, "blue");

        ConfigurationElement child = config.getChild("alpha");
        String alpha = null;
        if (child != null) {
            alpha = child.getValue();
        }
        if (alpha == null) {
            alpha = "";
        }
        expressions[A] = alpha;
    }

    private static String getChildValue(ConfigurationElement config, String childName) throws CoreException {
        ConfigurationElement child = config.getChild(childName);
        if (child != null) {
            return child.getValue();
        } else {
            throw new CoreException("Configuration element [" + childName + "] does not exist");
        }
    }

    private String checkAndTrim(String expression) {
        Assert.notNull(expression,  "expression");
        return expression.trim();
    }

}
