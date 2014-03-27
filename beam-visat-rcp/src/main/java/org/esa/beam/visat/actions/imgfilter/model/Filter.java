package org.esa.beam.visat.actions.imgfilter.model;

import com.bc.ceres.core.Assert;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.SingleValueConverter;

import java.awt.*;
import java.util.*;
import java.util.List;

import static java.lang.Math.*;

/**
 * @author Norman
 */
public class Filter {

    public enum Operation {
        CONVOLVE,
        MIN,
        MAX,
        MEAN,
        MEDIAN,
        STDDEV,
        ERODE,
        DILATE,
        OPEN,
        CLOSE
    }

    String name;
    String shorthand;
    Operation operation;
    boolean editable;
    HashSet<String> tags;
    double[] kernelElements;
    int kernelWidth;
    int kernelHeight;
    double kernelQuotient;
    int kernelOffsetX;
    int kernelOffsetY;

    transient List<Listener> listeners;
    private static int id = 1;

    public static Filter create(int kernelSize, String... tags) {
        return create(Operation.CONVOLVE, kernelSize, tags);
    }

    public static Filter create(Operation operation, int kernelSize, String... tags) {
        int id = Filter.id++;
        String name = System.getProperty("user.name") + id;
        String shorthand = "my" + id;
        return new Filter(name, shorthand, operation, kernelSize, kernelSize, null, 1.0, tags);
    }

    public Filter(String name, String shorthand, int kernelWidth, int kernelHeight, double[] kernelElements, double kernelQuotient, String... tags) {
        this(name, shorthand, Operation.CONVOLVE, kernelWidth, kernelHeight, kernelElements, kernelQuotient, tags);
    }

    public Filter(String name, String shorthand, Operation operation, int kernelWidth, int kernelHeight, String... tags) {
        this(name, shorthand, operation, kernelWidth, kernelHeight, null, 1.0, tags);
    }

    public Filter(String name, String shorthand, Operation operation, int kernelWidth, int kernelHeight, double[] kernelElements, double kernelQuotient, String... tags) {
        Assert.notNull(name, "name");
        Assert.notNull(shorthand, "shorthand");
        Assert.notNull(operation, "operation");
        Assert.argument(kernelWidth > 0, "kernelWidth");
        Assert.argument(kernelHeight > 0, "kernelHeight");
        Assert.argument(Math.abs(kernelQuotient) > 0.0, "kernelQuotient");
        this.name = name;
        this.shorthand = shorthand;
        this.operation = operation;
        this.tags = new HashSet<>();
        this.tags.addAll(Arrays.asList(tags));
        this.kernelWidth = kernelWidth;
        this.kernelHeight = kernelHeight;
        this.kernelQuotient = kernelQuotient;
        this.kernelOffsetX = kernelWidth / 2;
        this.kernelOffsetY = kernelHeight / 2;

        if (kernelElements != null) {
            Assert.argument(kernelElements.length == kernelWidth * kernelHeight, "kernelElements");
            this.kernelElements = kernelElements.clone();
        } else {
            this.kernelElements = new double[kernelWidth * kernelHeight];
            if (operation == Operation.CONVOLVE) {
                this.kernelElements[kernelOffsetY * kernelWidth + kernelOffsetX] = 1.0;
            } else {
                Arrays.fill(this.kernelElements, 1.0);
            }
        }

        this.listeners = new ArrayList<>();
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (!name.equals(this.name)) {
            this.name = name;
            notifyChange();
        }
    }

    public String getShorthand() {
        return shorthand;
    }

    public void setShorthand(String shorthand) {
        if (!shorthand.equals(this.shorthand)) {
            this.shorthand = shorthand;
            notifyChange();
        }
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        if (operation != this.operation) {
            this.operation = operation;
            notifyChange();
        }
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        if (editable != this.editable) {
            this.editable = editable;
            notifyChange();
        }
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(String... tags) {
        setTags(new HashSet<>(Arrays.asList(tags)));
    }

    public void setTags(HashSet<String> tags) {
        if (!tags.equals(this.tags)) {
            this.tags = tags;
            notifyChange();
        }
    }

    public double getKernelElement(int i, int j) {
        return getKernelElement(j * kernelWidth + i);
    }

    public double getKernelElement(int index) {
        return kernelElements[index];
    }

    public void setKernelElement(int i, int j, double value) {
        setKernelElement(j * kernelWidth + i, value);
    }

    public void setKernelElement(int index, double value) {
        if (value != kernelElements[index]) {
            kernelElements[index] = value;
            notifyChange();
        }
    }

    public double[] getKernelElements() {
        return kernelElements;
    }

    public void setKernelElements(double[] kernelElements) {
        if (!Arrays.equals(kernelElements, this.kernelElements)) {
            if (kernelElements.length != kernelWidth * kernelHeight) {
                throw new IllegalArgumentException("kernelElements.length != kernelWidth * kernelHeight");
            }
            this.kernelElements = kernelElements;
            notifyChange();
        }
    }

    public int getKernelWidth() {
        return kernelWidth;
    }

    public int getKernelHeight() {
        return kernelHeight;
    }

    public void setKernelSize(int width, int height) {
        if (width != this.kernelWidth || height != this.kernelHeight) {
            int oldWidth = this.kernelWidth;
            int oldHeight = this.kernelHeight;
            double[] oldElements = kernelElements;
            double[] elements = new double[width * height];
            for (int j = 0; j < height; j++) {
                for (int i = 0; i < width; i++) {
                    int oldI = (i * oldWidth) / width;
                    int oldJ = (j * oldHeight) / height;
                    int oldIndex = oldJ * oldWidth + oldI;
                    int newIndex = j * width + i;
                    elements[newIndex] = oldElements[oldIndex];
                }
            }
            this.kernelWidth = width;
            this.kernelHeight = height;
            this.kernelOffsetX = width / 2;
            this.kernelOffsetY = height / 2;
            this.kernelElements = elements;
            notifyChange();
        }
    }

    public void fill(FillFunction fillFunction) {
        int w = kernelWidth;
        int h = kernelHeight;
        double sum = 0;
        double x0 = -0.5 * (w - 1);
        double y0 = -0.5 * (h - 1);
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                double x = x0 + i;
                double y = y0 + j;
                double v = fillFunction.compute(x, y, w, h);
                this.kernelElements[j * w + i] = v;
                sum += v;
            }
        }
        this.kernelQuotient = sum;
        notifyChange();
    }

    public void fillRectangle(double fillValue) {
        Arrays.fill(kernelElements, fillValue);
        this.kernelQuotient = kernelWidth * kernelHeight * fillValue;
        notifyChange();
    }

    public void fillEllipse(final double fillValue) {
        fill(new FillFunction() {
            @Override
            public double compute(double x, double y, int w, int h) {
                double a = 0.5 * w;
                double b = 0.5 * h;
                double r = (x / a) * (x / a) + (y / b) * (y / b);
                return r < 1.0 ? fillValue : 0;
            }
        });
    }


    public void fillGaussian() {
        fill(new FillFunction() {
            @Override
            public double compute(double x, double y, int w, int h) {
                double sigX = 0.2 * w;
                double sigY = 0.2 * h;
                double r = (x / sigX) * (x / sigX) + (y / sigY) * (y / sigY);
                double z = exp(-0.5 * r);
                return floor((round(2 * (1 + min(w, h)) * z)));
            }
        });
    }

    public void fillLaplacian() {
        fill(new FillFunction() {
            @Override
            public double compute(double x, double y, int w, int h) {
                double sigX = 0.15 * w;
                double sigY = 0.15 * h;
                double r = (x / sigX) * (x / sigX) + (y / sigY) * (y / sigY);
                double z = (r - 1.0) * exp(-0.5 * r);
                return floor((round(2 * (1 + min(w, h)) * z)));
            }
        });
    }

    public void fillRandom() {
        fill(new FillFunction() {
            @Override
            public double compute(double x, double y, int w, int h) {
                double range = min(w, h);
                return round(range * (2 * Math.random() - 1));
            }
        });
    }

    public double getKernelQuotient() {
        return kernelQuotient;
    }

    public void setKernelQuotient(double kernelQuotient) {
        this.kernelQuotient = kernelQuotient;
        notifyChange();
    }

    public int getKernelOffsetX() {
        return kernelOffsetX;
    }

    public int getKernelOffsetY() {
        return kernelOffsetY;
    }

    public void setKernelOffset(int kernelOffsetX, int kernelOffsetY) {
        if (kernelOffsetX != this.kernelOffsetX || kernelOffsetY != this.kernelOffsetY) {
            this.kernelOffsetX = kernelOffsetX;
            this.kernelOffsetY = kernelOffsetY;
            notifyChange();
        }
    }

    public String getKernelElementsAsText() {
        return formatKernelElements(kernelElements, new Dimension(kernelWidth, kernelHeight), "\t");
    }

    public void setKernelElementsFromText(String text) {
        Dimension dim = new Dimension();
        double[] kernelElements = parseKernelElementsFromText(text, dim);
        if (kernelElements != null) {
            setKernelSize(dim.width, dim.height);
            setKernelElements(kernelElements);
        }
    }

    protected static double[] parseKernelElementsFromText(String text, Dimension dim) {
        String[][] tokens = tokenizeKernelElements(text);
        if (tokens == null) {
            return null;
        }
        int height = tokens.length;
        int width = tokens[0].length;
        double[] kernelElements = new double[width * height];
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                try {
                    kernelElements[j * width + i] = Double.parseDouble(tokens[j][i]);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        if (dim != null) {
            dim.width = width;
            dim.height = height;
        }
        return kernelElements;
    }

    public static String[][] tokenizeKernelElements(String text) {
        String[] lines = text.split("\n");
        int height = lines.length;
        int width = -1;
        String[][] strings = new String[height][];
        for (int j = 0; j < height; j++) {
            String line = lines[j];
            String[] cells = line.split(",|;|\\s+");
            if (width == -1) {
                width = cells.length;
            } else if (width != cells.length) {
                return null;
            }
            strings[j] = cells;
        }
        return strings;
    }

    public static String formatKernelElements(double[] kernelElements, Dimension dim, String sep) {
        StringBuilder text = new StringBuilder();
        for (int j = 0; j < dim.height; j++) {
            for (int i = 0; i < dim.width; i++) {
                if (i != 0) {
                    text.append(sep);
                }
                double v = kernelElements[j * dim.width + i];
                if (v == (int) v) {
                    text.append((int) v);
                } else {
                    text.append(v);
                }
            }
            if (j < dim.height - 1) {
                text.append('\n');
            }
        }
        return text.toString();
    }

    public static XStream createXStream() {
        final XStream xStream = new XStream();
        xStream.alias("filter", Filter.class);
        xStream.registerLocalConverter(Filter.class, "kernelElements", new SingleValueConverter() {
            @Override
            public String toString(Object o) {
                double[] o1 = (double[]) o;
                // todo - find out how to obtain width, height
                return Filter.formatKernelElements(o1, new Dimension(o1.length, 1), ",");
            }

            @Override
            public Object fromString(String s) {
                return Filter.parseKernelElementsFromText(s, null);
            }

            @Override
            public boolean canConvert(Class aClass) {
                return aClass.equals(double[].class);
            }
        });
        return xStream;
    }

    @SuppressWarnings("UnusedDeclaration")
    private Object readResolve() {
        if (tags == null) {
            tags = new HashSet<>();
        }
        if (listeners == null) {
            listeners = new ArrayList<>();
        }
        return this;
    }

    @Override
    public String toString() {
        return name;
    }

    public static boolean isKernelDataText(String text) {
        String[][] strings = tokenizeKernelElements(text);
        return strings != null;
    }

    public void notifyChange() {
        for (Listener listener : listeners) {
            listener.filterModelChanged(this);
        }
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public interface Listener {
        void filterModelChanged(Filter filter);
    }

    interface FillFunction {
        double compute(double x, double y, int w, int h);
    }
}
