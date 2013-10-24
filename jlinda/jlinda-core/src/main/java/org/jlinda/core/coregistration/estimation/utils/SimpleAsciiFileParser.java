package org.jlinda.core.coregistration.estimation.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Floats;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Collection of static utilities for reading tab and csv ASCII files
 */
public class SimpleAsciiFileParser {

    private String fileName;
    private LineNumberReader reader;
    private FileInputStream in;

    private final static String REGEX = "[,]|\\s+";

    private int numLines;
    private static Logger logger = LoggerFactory.getLogger(SimpleAsciiFileParser.class.getName());

    public SimpleAsciiFileParser(final String fileName, final int numLines) throws IOException {

        this.fileName = fileName;
        this.numLines = numLines;

        openStream();
        int maxLines = getNumOfLines();
        resetStream();

        if (numLines > maxLines) {
            logger.warn("Number of lines to be parsed larger then total number of lines in the input file.");
            logger.warn("Reading {} lines instead of {}.", maxLines, numLines);
            this.numLines = maxLines;
        }
    }

    public SimpleAsciiFileParser(final String fileName) throws IOException {

        this.fileName = fileName;

        openStream();
        this.numLines = getNumOfLines();
        resetStream();

    }

    public double[][] parseDoubleArray() throws IOException {

        /** declare return array  */
        double[][] data = new double[numLines][];

        /** declare local fields */
        String line;
        int innerCnt;

        /** read through the file */
        try {
            innerCnt = 0;
            while ((line = reader.readLine()) != null && innerCnt < numLines) {
                data[innerCnt] = parseLineToDoubleArray(line);
                innerCnt++;
            }
            reader.close();
        } catch (IOException e) {
            logger.error("Problem reading input file at line {}.", reader.getLineNumber());
            e.printStackTrace();
        }
        logger.info("Input file successfully read.");
        return data;
    }

    public TDoubleArrayList parseDoubleList() throws IOException {

        /** declare return variable */
        TDoubleArrayList data = new TDoubleArrayList();

        /** declare temp variables */
        String line;
        String[] fields;

        int innerCnt;

        /** read through the file */
        try {
            innerCnt = 0;
            while ((line = reader.readLine()) != null && innerCnt < numLines) {
                data.add(parseLineToDoubleArray(line));
                innerCnt++;
            }
            reader.close();
        } catch (IOException e) {
            logger.error("Problem reading input file at line {}.", reader.getLineNumber());
            e.printStackTrace();
        }
        logger.info("Input file successfully read.");
        return data;
    }

    public TIntObjectHashMap<float[]> parseFloatMap() throws IOException {

        /** declare return variable */
        TIntObjectHashMap<float[]> data = new TIntObjectHashMap<>();

        /** declare temp variables */
        String line;
        int innerCnt;

        /** read through the file */
        try {
            innerCnt = 0;
            while ((line = reader.readLine()) != null && innerCnt < numLines) {
                data.put(innerCnt, parseLineToFloatArray(line));
                innerCnt++;
            }
            reader.close();
        } catch (IOException e) {
            logger.error("Problem reading input file at line {}.", reader.getLineNumber());
            e.printStackTrace();
        }
        logger.info("Input file successfully read.");
        return data;
    }

    public TIntObjectHashMap<double[]> parseDoubleMap() throws IOException {

        /** declare return variable */
        TIntObjectHashMap<double[]> data = new TIntObjectHashMap<>();

        /** declare temp variables */
        String line;
        int innerCnt;

        /** read through the file */
        try {
            innerCnt = 0;
            while ((line = reader.readLine()) != null && innerCnt < numLines) {
                data.put(innerCnt, parseLineToDoubleArray(line));
                innerCnt++;
            }
            reader.close();
        } catch (IOException e) {
            logger.error("Problem reading input file at line {}.", reader.getLineNumber());
            e.printStackTrace();
        }
        logger.info("Input file successfully read.");
        return data;
    }

    public TFloatArrayList parseFloatList() throws IOException {

        /** declare return variable */
        TFloatArrayList data = new TFloatArrayList();

        /** declare temp variables */
        String line;
        int innerCnt;

        /** read through the file */
        try {

            innerCnt = 0;
            while ((line = reader.readLine()) != null && innerCnt < numLines) {
                data.add(parseLineToFloatArray(line));
                innerCnt++;
            }
            reader.close();
        } catch (IOException e) {
            logger.error("Problem reading input file at line {}.", reader.getLineNumber());
            e.printStackTrace();
        }
        logger.info("Input file successfully read.");
        return data;
    }

    @Deprecated
    public HashMap<Integer, double[]> parseDoubleMap_GUAVA() throws IOException {

        /** declare return variable */
        ArrayList<Double> tempData = Lists.newArrayList();
        HashMap<Integer, double[]> data = Maps.newHashMap();

        /** declare temp variables */
        String line;
        String[] fields;

        int innerCnt;

        /** read through the file */
        try {

            innerCnt = 0;
            while ((line = reader.readLine()) != null && innerCnt < numLines) {
                fields = line.trim().split(REGEX);
                for (String field : fields) {
                    if (!field.isEmpty()) {
                        tempData.add(Double.parseDouble(field.trim()));
                    }
                    data.put(innerCnt, Doubles.toArray(tempData)); // Doubles of Guava
                }
                tempData.clear();
                innerCnt++;
            }
            reader.close();
        } catch (IOException e) {
            logger.error("Problem reading input file at line {}.", reader.getLineNumber());
            e.printStackTrace();
        }
        logger.info("Input file successfully read.");
        return data;
    }

    @Deprecated
    public HashMap<Integer, float[]> parseFloatMap_GUAVA() throws IOException {

        /** declare return variable */
        ArrayList<Double> tempData = Lists.newArrayList();
        HashMap<Integer, float[]> data = Maps.newHashMap();

        /** declare temp variables */
        String line;
        String[] fields;

        int innerCnt;

        /** read through the file */
        try {

            innerCnt = 0;
            while ((line = reader.readLine()) != null && innerCnt < numLines) {
                fields = line.trim().split(REGEX);
                for (String field : fields) {
                    if (!field.isEmpty()) {
                        tempData.add(Double.parseDouble(field.trim()));
                    }
                    data.put(innerCnt, Floats.toArray(tempData)); // Doubles of Guava
                }
                tempData.clear();
                innerCnt++;
            }
            reader.close();
        } catch (IOException e) {
            logger.error("Problem reading input file at line {}.", reader.getLineNumber());
            e.printStackTrace();
        }
        logger.info("Input file successfully read.");
        return data;
    }

    public float[][] parseFloatArray() throws IOException {

        /** declare array as a data container */
        float[][] data = new float[numLines][];

        /** declare temp variables */
        String line;

        int innerCnt;

        /** read through the file */
        try {
            innerCnt = 0;
            while ((line = reader.readLine()) != null && innerCnt < numLines) {
                data[innerCnt] = parseLineToFloatArray(line);
                innerCnt++;
            }
            reader.close();

        } catch (IOException e) {
            logger.error("Problem reading input file at line {}.", reader.getLineNumber());
            e.printStackTrace();
        }
        logger.info("Input file successfully read.");
        return data;

    }

    /**
     * open file stream
     */
    private void openStream() throws FileNotFoundException {
        in = new FileInputStream(fileName);
        reader = new LineNumberReader(new BufferedReader(new InputStreamReader(in)));
    }

    /**
     * reset to the beginning of file (discard old buffered reader)
     */
    private void resetStream() throws IOException {
        in.getChannel().position(0);
        reader = new LineNumberReader(new BufferedReader(new InputStreamReader(in)));
    }

    /**
     * get max number of lines of input file
     */
    private int getNumOfLines() throws IOException {
        reader.skip(Long.MAX_VALUE);
        return reader.getLineNumber();
    }

    /**
     * parse input String line to TDoubleArrayList, uses Trove Collections
     *
     * @param line input string
     */
    private TDoubleArrayList parseLineToDoubleList(String line) {
        TDoubleArrayList list = new TDoubleArrayList();
        String[] fields;
        fields = line.trim().split(REGEX);
        for (String field : fields) {
            if (!field.isEmpty()) {
                list.add(Double.parseDouble(field.trim()));
            }
        }
        return list;
    }

    /**
     * parse input String line to double[], uses Trove Collections
     *
     * @param line input string
     */
    private double[] parseLineToDoubleArray(String line) {
        return parseLineToDoubleList(line).toArray();
    }

    /**
     * parse input String line to TFloatArrayList, uses Trove Collections
     *
     * @param line input string
     */
    private TFloatArrayList parseLineToFloatList(String line) {
        TFloatArrayList list = new TFloatArrayList();
        String[] fields;
        fields = line.trim().split(REGEX);
        for (String field : fields) {
            if (!field.isEmpty()) {
                list.add(Float.parseFloat(field.trim()));
            }
        }
        return list;
    }

    public static void main(String[] args) throws IOException {

//        String[] checks = {"double", "float", "collections"};
        String[] checks = {"collections"};
        String[] numLines = {"full", "1", "20", "150"};

        SimpleAsciiFileParser asciiFileParser;
        double[][] doubleData;
        float[][] floatData;
        TIntObjectHashMap<double[]> doubleMap;
        int cnt;

        String fileName = "/d1/list.1000.txt";

        for (String checkToDo : checks) {

            if (checkToDo.contains("double")) {
                for (String linsToLoad : numLines) {

                    cnt = 0;

                    if (linsToLoad.contains("full")) {
                        asciiFileParser = new SimpleAsciiFileParser(fileName);
                    } else {
                        asciiFileParser = new SimpleAsciiFileParser(fileName, Integer.parseInt(linsToLoad));
                    }

                    doubleData = asciiFileParser.parseDoubleArray();
                    for (double[] aData : doubleData) {
                        System.out.print(cnt++ + ":");
                        System.out.println(ArrayUtils.toString(aData));
                    }
                }

            } else if (checkToDo.contains("collections")) {

                for (String linsToLoad : numLines) {

                    cnt = 0;
                    if (linsToLoad.contains("full")) {
                        asciiFileParser = new SimpleAsciiFileParser(fileName);
                    } else {
                        asciiFileParser = new SimpleAsciiFileParser(fileName, Integer.parseInt(linsToLoad));
                    }

                    doubleMap = asciiFileParser.parseDoubleMap();
                    for (TIntObjectIterator<double[]> iterator = doubleMap.iterator(); iterator.hasNext(); ) {
                        iterator.advance();
                        System.out.print(cnt++ + ":");
                        System.out.println(ArrayUtils.toString(iterator.value()));
                    }

                    // functional loop
                    // doubleMap.forEachValue(new TObjectProcedure<double[]>() {
                    //    int cnt = 0;
                    //    @Override
                    //    public boolean execute(double[] doubles) {
                    //      System.out.print(cnt++ + ":");
                    //      System.out.println(ArrayUtils.toString(doubles));
                    //      return true;
                    //    }
                    // });

                }

            } else if (checkToDo.contains("float")) {

                for (String linsToLoad : numLines) {

                    cnt = 0;

                    if (linsToLoad.contains("full")) {
                        asciiFileParser = new SimpleAsciiFileParser(fileName);
                    } else {
                        asciiFileParser = new SimpleAsciiFileParser(fileName, Integer.parseInt(linsToLoad));
                    }

                    floatData = asciiFileParser.parseFloatArray();
                    for (float[] aData : floatData) {
                        System.out.print(cnt++ + ":");
                        System.out.println(ArrayUtils.toString(aData));
                    }
                }
            }
        }
    }


    /**
     * parse input String line to float[], uses Trove Collections
     *
     * @param line input string
     */
    private float[] parseLineToFloatArray(String line) {
        return parseLineToFloatList(line).toArray();
    }
}