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
package org.esa.snap.core.util.math;

import org.apache.commons.math3.util.FastMath;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Performance tests for FastMath.
 * Not enabled by default, as the class does not end in Test.
 * <p>
 * Invoke by running<br/>
 * {@code mvn test -Dtest=org.esa.snap.util.math.FastMathPerformance}<br/>
 * or by running<br/>
 * {@code mvn test -Dtest=org.esa.snap.util.math.FastMathPerformance -DargLine="-DtestRuns=1234 -server"}<br/>
 */

public class FastMathPerformance {
    private static final int RUNS = Integer.parseInt(System.getProperty("testRuns", "10000000"));
    private static final double F1 = 1d / RUNS;

    private static List<Double> results = new ArrayList<>();

    // Header format
    private static final String FMT_HDR = "%-5s %13s %13s %13s Runs=%d Java %s (%s) %s (%s)";
    // Detail format
    private static final String FMT_DTL = "%-5s %6d %6.1f %6d %6.4f %6d %6.4f";

    @BeforeClass
    public static void header() {
        Locale.setDefault(Locale.ENGLISH);
        System.out.println(String.format(FMT_HDR,
                                         "Name",  "Math", "StrictMath", "FastMath", RUNS,
                                         System.getProperty("java.version"),
                                         System.getProperty("java.runtime.version", "?"),
                                         System.getProperty("java.vm.name"),
                                         System.getProperty("java.vm.version")
        ));
    }

    private static void report(String name, double result, long strictMathTime, long fastMathTime, long mathTime) {
        long unitTime = mathTime;

        System.out.println(String.format(FMT_DTL,
                                         name,
                                         mathTime / RUNS, (double) mathTime / unitTime,
                                         strictMathTime / RUNS, (double) strictMathTime / unitTime,
                                         fastMathTime / RUNS, (double) fastMathTime / unitTime
        ));


        // Keep HotSpot honest
        results.add(result);
    }

    @Test
    @Ignore
    public void test() {
        for (int i = 0; i < 10; ++i) {
            testLog();
            testLog10();
            testLog1p();
            testPow();
            testPowFromIntToInt();
            testExp();
            testSin();
            testAsin();
            testCos();
            testAcos();
            testTan();
            testAtan();
            testAtan2();
            testHypot();
            testCbrt();
            testSqrt();
            testCosh();
            testSinh();
            testTanh();
            testExpm1();
            testAbs();
        }

        System.out.println("#results = " + results.size());
    }

    public void testLog() {
        System.gc();
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            x += StrictMath.log(Math.PI + i/* 1.0 + i/1e9 */);
        long strictMath = System.nanoTime() - time;

        System.gc();
        double y = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            y += FastMath.log(Math.PI + i/* 1.0 + i/1e9 */);
        long fastTime = System.nanoTime() - time;

        System.gc();
        double z = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            z += Math.log(Math.PI + i/* 1.0 + i/1e9 */);
        long mathTime = System.nanoTime() - time;

        report("log", x + y + z, strictMath, fastTime, mathTime);
    }

    public void testLog10() {
        System.gc();
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            x += StrictMath.log10(Math.PI + i/* 1.0 + i/1e9 */);
        long strictMath = System.nanoTime() - time;

        System.gc();
        double y = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            y += FastMath.log10(Math.PI + i/* 1.0 + i/1e9 */);
        long fastTime = System.nanoTime() - time;

        System.gc();
        double z = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            z += Math.log10(Math.PI + i/* 1.0 + i/1e9 */);
        long mathTime = System.nanoTime() - time;

        report("log10", x + y + z, strictMath, fastTime, mathTime);
    }

    public void testLog1p() {
        System.gc();
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            x += StrictMath.log1p(Math.PI + i/* 1.0 + i/1e9 */);
        long strictMath = System.nanoTime() - time;

        System.gc();
        double y = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            y += FastMath.log1p(Math.PI + i/* 1.0 + i/1e9 */);
        long fastTime = System.nanoTime() - time;

        System.gc();
        double z = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            z += Math.log1p(Math.PI + i/* 1.0 + i/1e9 */);
        long mathTime = System.nanoTime() - time;

        report("log1p", x + y + z, strictMath, fastTime, mathTime);
    }

    public void testPow() {
        System.gc();
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            x += StrictMath.pow(Math.PI + i * F1, i * F1);
        long strictTime = System.nanoTime() - time;

        System.gc();
        double y = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            y += FastMath.pow(Math.PI + i * F1, i * F1);
        long fastTime = System.nanoTime() - time;

        System.gc();
        double z = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            z += Math.pow(Math.PI + i * F1, i * F1);
        long mathTime = System.nanoTime() - time;
        report("pow", x + y + z, strictTime, fastTime, mathTime);
    }

    public void testPowFromIntToInt() {
        System.gc();
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            x += StrictMath.pow(i, i);
        long strictTime = System.nanoTime() - time;

        System.gc();
        double y = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            y += FastMath.pow(i, i);
        long fastTime = System.nanoTime() - time;

        System.gc();
        double z = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            z += Math.pow(i, i);
        long mathTime = System.nanoTime() - time;
        report("powII", x + y + z, strictTime, fastTime, mathTime);
    }

    public void testExp() {
        System.gc();
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            x += StrictMath.exp(i * F1);
        long strictTime = System.nanoTime() - time;

        System.gc();
        double y = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            y += FastMath.exp(i * F1);
        long fastTime = System.nanoTime() - time;

        System.gc();
        double z = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            z += Math.exp(i * F1);
        long mathTime = System.nanoTime() - time;

        report("exp", x + y + z, strictTime, fastTime, mathTime);
    }

    public void testSin() {
        System.gc();
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            x += StrictMath.sin(i * F1);
        long strictTime = System.nanoTime() - time;

        System.gc();
        double y = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            y += FastMath.sin(i * F1);
        long fastTime = System.nanoTime() - time;

        System.gc();
        double z = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            z += Math.sin(i * F1);
        long mathTime = System.nanoTime() - time;

        report("sin", x + y + z, strictTime, fastTime, mathTime);
    }

    public void testAsin() {
        System.gc();
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            x += StrictMath.asin(i / 10000000.0);
        long strictTime = System.nanoTime() - time;

        System.gc();
        double y = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            y += FastMath.asin(i / 10000000.0);
        long fastTime = System.nanoTime() - time;

        System.gc();
        double z = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            z += Math.asin(i / 10000000.0);
        long mathTime = System.nanoTime() - time;

        report("asin", x + y + z, strictTime, fastTime, mathTime);
    }

    public void testCos() {
        System.gc();
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            x += StrictMath.cos(i * F1);
        long strictTime = System.nanoTime() - time;

        System.gc();
        double y = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            y += FastMath.cos(i * F1);
        long fastTime = System.nanoTime() - time;

        System.gc();
        double z = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            z += Math.cos(i * F1);
        long mathTime = System.nanoTime() - time;

        report("cos", x + y + z, strictTime, fastTime, mathTime);
    }

    public void testAcos() {
        System.gc();
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            x += StrictMath.acos(i / 10000000.0);
        long strictTime = System.nanoTime() - time;

        System.gc();
        double y = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            y += FastMath.acos(i / 10000000.0);
        long fastTime = System.nanoTime() - time;

        System.gc();
        double z = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            z += Math.acos(i / 10000000.0);
        long mathTime = System.nanoTime() - time;
        report("acos", x + y + z, strictTime, fastTime, mathTime);
    }

    public void testTan() {
        System.gc();
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            x += StrictMath.tan(i * F1);
        long strictTime = System.nanoTime() - time;

        System.gc();
        double y = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            y += FastMath.tan(i * F1);
        long fastTime = System.nanoTime() - time;

        System.gc();
        double z = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            z += Math.tan(i * F1);
        long mathTime = System.nanoTime() - time;

        report("tan", x + y + z, strictTime, fastTime, mathTime);
    }

    public void testAtan() {
        System.gc();
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            x += StrictMath.atan(i * F1);
        long strictTime = System.nanoTime() - time;

        System.gc();
        double y = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            y += FastMath.atan(i * F1);
        long fastTime = System.nanoTime() - time;

        System.gc();
        double z = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            z += Math.atan(i * F1);
        long mathTime = System.nanoTime() - time;

        report("atan", x + y + z, strictTime, fastTime, mathTime);
    }

    public void testAtan2() {
        System.gc();
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            x += StrictMath.atan2(i * F1, i * F1);
        long strictTime = System.nanoTime() - time;

        System.gc();
        double y = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            y += FastMath.atan2(i * F1, i * F1);
        long fastTime = System.nanoTime() - time;

        System.gc();
        double z = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            z += Math.atan2(i * F1, i * F1);
        long mathTime = System.nanoTime() - time;

        report("atan2", x + y + z, strictTime, fastTime, mathTime);
    }

    public void testHypot() {
        System.gc();
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            x += StrictMath.hypot(i * F1, i * F1);
        long strictTime = System.nanoTime() - time;

        System.gc();
        double y = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            y += FastMath.hypot(i * F1, i * F1);
        long fastTime = System.nanoTime() - time;

        System.gc();
        double z = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            z += Math.hypot(i * F1, i * F1);
        long mathTime = System.nanoTime() - time;

        report("hypot", x + y + z, strictTime, fastTime, mathTime);
    }

    public void testCbrt() {
        System.gc();
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            x += StrictMath.cbrt(i * F1);
        long strictTime = System.nanoTime() - time;

        System.gc();
        double y = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            y += FastMath.cbrt(i * F1);
        long fastTime = System.nanoTime() - time;

        System.gc();
        double z = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            z += Math.cbrt(i * F1);
        long mathTime = System.nanoTime() - time;

        report("cbrt", x + y + z, strictTime, fastTime, mathTime);
    }

    public void testSqrt() {
        System.gc();
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            x += StrictMath.sqrt(i * F1);
        long strictTime = System.nanoTime() - time;

        System.gc();
        double y = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            y += FastMath.sqrt(i * F1);
        long fastTime = System.nanoTime() - time;

        System.gc();
        double z = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            z += Math.sqrt(i * F1);
        long mathTime = System.nanoTime() - time;

        report("sqrt", x + y + z, strictTime, fastTime, mathTime);
    }

    public void testCosh() {
        System.gc();
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            x += StrictMath.cosh(i * F1);
        long strictTime = System.nanoTime() - time;

        System.gc();
        double y = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            y += FastMath.cosh(i * F1);
        long fastTime = System.nanoTime() - time;

        System.gc();
        double z = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            z += Math.cosh(i * F1);
        long mathTime = System.nanoTime() - time;

        report("cosh", x + y + z, strictTime, fastTime, mathTime);
    }

    public void testSinh() {
        System.gc();
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            x += StrictMath.sinh(i * F1);
        long strictTime = System.nanoTime() - time;

        System.gc();
        double y = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            y += FastMath.sinh(i * F1);
        long fastTime = System.nanoTime() - time;

        System.gc();
        double z = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            z += Math.sinh(i * F1);
        long mathTime = System.nanoTime() - time;

        report("sinh", x + y + z, strictTime, fastTime, mathTime);
    }

    public void testTanh() {
        System.gc();
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            x += StrictMath.tanh(i * F1);
        long strictTime = System.nanoTime() - time;

        System.gc();
        double y = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            y += FastMath.tanh(i * F1);
        long fastTime = System.nanoTime() - time;

        System.gc();
        double z = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            z += Math.tanh(i * F1);
        long mathTime = System.nanoTime() - time;

        report("tanh", x + y + z, strictTime, fastTime, mathTime);
    }

    public void testExpm1() {
        System.gc();
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            x += StrictMath.expm1(-i * F1);
        long strictTime = System.nanoTime() - time;

        System.gc();
        double y = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            y += FastMath.expm1(-i * F1);
        long fastTime = System.nanoTime() - time;

        System.gc();
        double z = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            z += Math.expm1(-i * F1);
        long mathTime = System.nanoTime() - time;
        report("expm1", x + y + z, strictTime, fastTime, mathTime);
    }

    public void testAbs() {
        System.gc();
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            x += StrictMath.abs(i * (1 - 0.5 * RUNS));
        long strictTime = System.nanoTime() - time;

        System.gc();
        double y = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            y += FastMath.abs(i * (1 - 0.5 * RUNS));
        long fastTime = System.nanoTime() - time;

        System.gc();
        double z = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++)
            x += Math.abs(i * (1 - 0.5 * RUNS));
        long mathTime = System.nanoTime() - time;

        report("abs", x + y + z, strictTime, fastTime, mathTime);
    }
}
