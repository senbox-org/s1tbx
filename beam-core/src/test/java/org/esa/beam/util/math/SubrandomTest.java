package org.esa.beam.util.math;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.junit.Ignore;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.StringTokenizer;

import static org.junit.Assert.assertTrue;

/**
 * Tests for using subrandom sequences for sampling 'dummy locations' in SST CCI.
 *
 * @author Ralf Quast
 */
@Ignore
public class SubrandomTest {

    @Ignore
    @Test
    public void testSubrandom() throws Exception {
        final double c = Math.sqrt(2.0) % 1.0;

        assertTrue(c > 0.0 && c < 1.0);
        final Subrandom subrandom = new Subrandom(c, 27182);

        double s;
        double t;

        s = subrandom.nextDouble();
        assertTrue(s > 0.0 && s < 1.0);

        t = subrandom.nextDouble();
        assertTrue(t != s);

        s = t;
        assertTrue(s > 0.0 && s < 1.0);

        t = subrandom.nextDouble();
        assertTrue(t != s);

        s = t;
        assertTrue(s > 0.0 && s < 1.0);

        t = subrandom.nextDouble();
        assertTrue(t != s);

        s = t;
        assertTrue(s > 0.0 && s < 1.0);

        t = subrandom.nextDouble();
        assertTrue(t != s);

        s = t;
        assertTrue(s > 0.0 && s < 1.0);

    }

    @Ignore
    @Test
    public void testSubrandom2D() throws Exception {
        final int h = 400;
        final BufferedImage image = new BufferedImage(h * 2, h, BufferedImage.TYPE_BYTE_GRAY);
        final JLabel label = new JLabel(new ImageIcon(image));

        final JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.getContentPane().add(label);
        frame.setSize(h * 2, h);
        frame.setVisible(true);

        //final double x = Math.sqrt(101.0) % 1.0;
        //final double y = Math.sqrt(17.0) % 1.0;
        //final Subrandom2D subrandom2D = new Subrandom2D(new Point2D.Double(x, y), 27182);
        final Subrandom2D subrandom2D = new SobolSubrandom2D();
        final Graphics2D graphics = image.createGraphics();
        Point2D p;
        int ix;
        int iy;
        for (int i = 0; i < 10000; i++) {
            p = subrandom2D.nextPoint();
            ix = (int) (p.getX() * h);
            iy = (int) (p.getY() * h);
            graphics.draw(new Rectangle(ix, iy, 1, 1));
            graphics.draw(new Rectangle(ix + h, iy, 1, 1));
            label.repaint();

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // ignore
            }
        }

        final File imageFile = new File("subrandom.png");
        ImageIO.write(image, "png", imageFile);
    }

    private static class Subrandom {

        private final double c;
        private double s;

        public Subrandom(double c, long seed) {
            if (c <= 0.0 || c >= 1.0) {
                throw new IllegalArgumentException("Argument c is not in (0, 1).");
            }
            final Random random = new Random(seed);

            this.c = c;
            this.s = random.nextDouble();
        }

        public double nextDouble() {
            return s = (s + c) % 1.0;
        }
    }

    private static class SimpleSubrandom2D implements Subrandom2D {

        private final Point2D.Double c;
        private Point2D.Double s;

        public SimpleSubrandom2D(Point2D.Double c, long seed) {
            if (c.x <= 0.0 || c.x >= 1.0) {
                throw new IllegalArgumentException("Argument c.x is not in (0, 1).");
            }
            if (c.y <= 0.0 || c.y >= 1.0) {
                throw new IllegalArgumentException("Argument c.y is not in (0, 1).");
            }
            final Random random = new Random(seed);

            this.c = new Point2D.Double(c.x, c.y);
            this.s = new Point2D.Double(random.nextDouble(), random.nextDouble());
        }

        @Override
        public Point2D nextPoint() {
            s.x = (s.x + c.x) % 1.0;
            s.y = (s.y + c.y) % 1.0;

            return new Point2D.Double(s.x, s.y);
        }
    }

    private static class SobolSubrandom2D implements Subrandom2D {

        private boolean b;
        private final SobolSequenceGenerator sobolSequenceGenerator;

        public SobolSubrandom2D() throws IOException {
            final File file = new File("/Users/ralf/Downloads/new-joe-kuo-6.21201.txt");
            final FileInputStream is = new FileInputStream(file);
            try {
                sobolSequenceGenerator = new SobolSequenceGenerator(201, is);
            } finally {
                is.close();
            }
        }

        @Override
        public Point2D nextPoint() {
            final double[] doubles = sobolSequenceGenerator.nextVector();

            return new Point2D.Double(doubles[6], doubles[7]);
        }
    }


/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

    /**
     * Implementation of a Sobol sequence.
     * <p/>
     * A Sobol sequence is a low-discrepancy sequence with the property that for all values of N,
     * its subsequence (x1, ... xN) has a low discrepancy. It can be used to generate pseudo-random
     * points in a space S, which are equi-distributed.
     * <p/>
     * The implementation already comes with support for up to 1000 dimensions with direction numbers
     * calculated from <a href="http://web.maths.unsw.edu.au/~fkuo/sobol/">Stephen Joe and Frances Kuo</a>.
     * <p/>
     * The generator supports two modes:
     * <ul>
     * <li>sequential generation of points: {@link #nextVector()}</li>
     * <li>random access to the i-th point in the sequence: {@link #skipTo(int)}</li>
     * </ul>
     *
     * @version $Id: SobolSequenceGenerator.html 885258 2013-11-03 02:46:49Z tn $
     * @see <a href="http://en.wikipedia.org/wiki/Sobol_sequence">Sobol sequence (Wikipedia)</a>
     * @see <a href="http://web.maths.unsw.edu.au/~fkuo/sobol/">Sobol sequence direction numbers</a>
     * @since 3.3
     */
    private static class SobolSequenceGenerator {

        /**
         * The number of bits to use.
         */
        private static final int BITS = 52;

        /**
         * The scaling factor.
         */
        private static final double SCALE = (double) (1L << BITS); //Math.pow(2, BITS);

        /**
         * The maximum supported space dimension.
         */
        private static final int MAX_DIMENSION = 1000;

        /**
         * The resource containing the direction numbers.
         */
        private static final String RESOURCE_NAME = "/assets/org/apache/commons/math3/random/new-joe-kuo-6.1000"; // TODO - add resource file

        /**
         * Character set for file input.
         */
        private static final String FILE_CHARSET = "US-ASCII";

        /**
         * Space dimension.
         */
        private final int dimension;

        /**
         * The current index in the sequence.
         */
        private int count = 0;

        /**
         * The direction vector for each component.
         */
        private final long[][] direction;

        /**
         * The current state.
         */
        private final long[] x;

        /**
         * Construct a new Sobol sequence generator for the given space dimension.
         *
         * @param dimension the space dimension
         */
        public SobolSequenceGenerator(final int dimension) {
            if (dimension < 1 || dimension > MAX_DIMENSION) {
                throw new IllegalArgumentException("dimension < 1 || dimension > MAX_DIMENSION"); // TODO - message
            }

            // initialize the other dimensions with direction numbers from a resource
            final InputStream is = getClass().getResourceAsStream(RESOURCE_NAME);
            if (is == null) {
                throw new IllegalStateException("The internal resource file could not be read.");
            }

            this.dimension = dimension;

            // init data structures
            direction = new long[dimension][BITS + 1];
            x = new long[dimension];

            try {
                initFromStream(is);
            } catch (IOException e) {
                // the internal resource file could not be read; should not happen
                throw new IllegalStateException("The internal resource file could not be read.");
            } catch (NoSuchElementException e) {
                // the internal resource file could not be parsed; should not happen
                throw new IllegalStateException("The internal resource file could not be parsed.");
            } catch (NumberFormatException e) {
                // the internal resource file could not be parsed; should not happen
                throw new IllegalStateException("The internal resource file could not be parsed.");
            } finally {
                try {
                    is.close();
                } catch (IOException e) { // NOPMD
                    // ignore
                }
            }
        }

        /**
         * Construct a new Sobol sequence generator for the given space dimension with
         * direction vectors loaded from the given stream.
         * <p/>
         * The expected format is identical to the files available from
         * <a href="http://web.maths.unsw.edu.au/~fkuo/sobol/">Stephen Joe and Frances Kuo</a>.
         * The first line will be ignored as it is assumed to contain only the column headers.
         * The columns are:
         * <ul>
         * <li>d: the dimension</li>
         * <li>s: the degree of the primitive polynomial</li>
         * <li>a: the number representing the coefficients</li>
         * <li>m: the list of initial direction numbers</li>
         * </ul>
         * Example:
         * <pre>
         * d       s       a       m_i
         * 2       1       0       1
         * 3       2       1       1 3
         * </pre>
         * <p/>
         * The input stream <i>must</i> be an ASCII text containing one valid direction vector per line.
         *
         * @param dimension the space dimension
         * @param is        the stream to read the direction vectors from
         *
         * @throws IOException if an error occurs while reading from the input stream
         */
        public SobolSequenceGenerator(final int dimension, final InputStream is) throws IOException {

            if (dimension < 1) {
                throw new IllegalArgumentException("dimension < 1"); // TODO - message
            }

            this.dimension = dimension;

            // init data structures
            direction = new long[dimension][BITS + 1];
            x = new long[dimension];

            // initialize the other dimensions with direction numbers from the stream
            int lastDimension = initFromStream(is);
            if (lastDimension < dimension) {
                throw new IllegalArgumentException("lastDimension < dimension"); // TODO - message
            }
        }

        /**
         * Load the direction vector for each dimension from the given stream.
         * <p/>
         * The input stream <i>must</i> be an ASCII text containing one
         * valid direction vector per line.
         *
         * @param is the input stream to read the direction vector from
         *
         * @return the last dimension that has been read from the input stream
         *
         * @throws IOException if the stream could not be read
         */
        private int initFromStream(final InputStream is) throws IOException {

            // special case: dimension 1 -> use unit initialization
            for (int i = 1; i <= BITS; i++) {
                direction[0][i] = 1l << (BITS - i);
            }

            final Charset charset = Charset.forName(FILE_CHARSET);
            final BufferedReader reader = new BufferedReader(new InputStreamReader(is, charset));
            int dim = -1;

            try {
                // ignore first line
                reader.readLine();

                int lineNumber = 2;
                int index = 1;
                String line;
                while ((line = reader.readLine()) != null) {
                    StringTokenizer st = new StringTokenizer(line, " ");
                    try {
                        dim = Integer.parseInt(st.nextToken());
                        if (dim >= 2 && dim <= dimension) { // we have found the right dimension
                            final int s = Integer.parseInt(st.nextToken());
                            final int a = Integer.parseInt(st.nextToken());
                            final int[] m = new int[s + 1];
                            for (int i = 1; i <= s; i++) {
                                m[i] = Integer.parseInt(st.nextToken());
                            }
                            initDirectionVector(index++, a, m);
                        }

                        if (dim > dimension) {
                            return dim;
                        }
                    } catch (NoSuchElementException e) {
                        throw new NoSuchElementException("Could not parse line '" + line + "' in line number " + lineNumber);
                    } catch (NumberFormatException e) {
                        throw new NoSuchElementException("Could not parse line '" + line + "' in line number " + lineNumber);
                    }
                    lineNumber++;
                }
            } finally {
                reader.close();
            }

            return dim;
        }

        /**
         * Calculate the direction numbers from the given polynomial.
         *
         * @param d the dimension, zero-based
         * @param a the coefficients of the primitive polynomial
         * @param m the initial direction numbers
         */
        private void initDirectionVector(final int d, final int a, final int[] m) {
            final int s = m.length - 1;
            for (int i = 1; i <= s; i++) {
                direction[d][i] = ((long) m[i]) << (BITS - i);
            }
            for (int i = s + 1; i <= BITS; i++) {
                direction[d][i] = direction[d][i - s] ^ (direction[d][i - s] >> s);
                for (int k = 1; k <= s - 1; k++) {
                    direction[d][i] ^= ((a >> (s - 1 - k)) & 1) * direction[d][i - k];
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public double[] nextVector() {
            final double[] v = new double[dimension];
            if (count == 0) {
                count++;
                return v;
            }

            // find the index c of the rightmost 0
            int c = 1;
            int value = count - 1;
            while ((value & 1) == 1) {
                value >>= 1;
                c++;
            }

            for (int i = 0; i < dimension; i++) {
                x[i] = x[i] ^ direction[i][c];
                v[i] = (double) x[i] / SCALE;
            }
            count++;
            return v;
        }

        /**
         * Skip to the i-th point in the Sobol sequence.
         * <p/>
         * This operation can be performed in O(1).
         *
         * @param index the index in the sequence to skip to
         *
         * @return the i-th point in the Sobol sequence
         */
        public double[] skipTo(final int index) {
            if (index == 0) {
                // reset x vector
                Arrays.fill(x, 0);
            } else {
                final int i = index - 1;
                final long grayCode = i ^ (i >> 1); // compute the gray code of i = i XOR floor(i / 2)
                for (int j = 0; j < dimension; j++) {
                    long result = 0;
                    for (int k = 1; k <= BITS; k++) {
                        final long shift = grayCode >> (k - 1);
                        if (shift == 0) {
                            // stop, as all remaining bits will be zero
                            break;
                        }
                        // the k-th bit of i
                        final long ik = shift & 1;
                        result ^= ik * direction[j][k];
                    }
                    x[j] = result;
                }
            }
            count = index;
            return nextVector();
        }

        /**
         * Returns the index i of the next point in the Sobol sequence that will be returned
         * by calling {@link #nextVector()}.
         *
         * @return the index of the next point
         */
        public int getNextIndex() {
            return count;
        }

    }


    private static interface Subrandom2D {

        Point2D nextPoint();
    }
}
