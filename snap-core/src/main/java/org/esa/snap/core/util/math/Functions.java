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

package org.esa.snap.core.util.math;

final class Functions {

    //////////////////////////////////////////////////////
    // Frequently used f(x) functions
    //////////////////////////////////////////////////////

    static final class FX_X4 implements FX {

        public final double f(double x) {
            return (x * x * x * x);
        }
        public String getCCodeExpr() {
            return "pow(x, 4)";
        }
    }

    static final class FX_X3 implements FX {

        public final double f(double x) {
            return (x * x * x);
        }
        public String getCCodeExpr() {
            return "pow(x, 3)";
        }
    }

    static final class FX_X2 implements FX {

        public final double f(double x) {
            return (x * x);
        }
        public String getCCodeExpr() {
            return "pow(x, 2)";
        }
    }

    static final class FX_X implements FX {

        public final double f(double x) {
            return x;
        }
        public String getCCodeExpr() {
            return "x";
        }
    }

    static final class FX_1 implements FX {

        public final double f(double x) {
            return 1.0;
        }
        public String getCCodeExpr() {
            return "1";
        }
    }

    //////////////////////////////////////////////////////
    // Frequently used f(x,y) functions
    //////////////////////////////////////////////////////

    static final class FXY_X4Y4 implements FXY {
        public final double f(double x, double y) {
            return (x * x * x * x) * (y * y * y * y);
        }
        public String getCCodeExpr() {
            return "pow(x, 4) * pow(y, 4)";
        }
    }

    static final class FXY_X4Y3 implements FXY {
        public final double f(double x, double y) {
            return (x * x * x * x) * (y * y * y);
        }
        public String getCCodeExpr() {
            return "pow(x, 4) * pow(y, 3)";
        }
    }

    static final class FXY_X3Y4 implements FXY {
        public final double f(double x, double y) {
            return (x * x * x) * (y * y * y * y);
        }
        public String getCCodeExpr() {
            return "pow(x, 3) * pow(y, 4)";
        }
    }

    static final class FXY_X4Y2 implements FXY {
        public final double f(double x, double y) {
            return (x * x * x * x) * (y * y);
        }
        public String getCCodeExpr() {
            return "pow(x, 4) * pow(y, 2)";
        }
    }

    static final class FXY_X2Y4 implements FXY {
        public final double f(double x, double y) {
            return (x * x) * (y * y * y * y);
        }
        public String getCCodeExpr() {
            return "pow(x, 2) * pow(y, 4)";
        }
    }

    static final class FXY_X4Y implements FXY {
        public final double f(double x, double y) {
            return (x * x * x * x) * (y);
        }
        public String getCCodeExpr() {
            return "pow(x, 4) * y";
        }
    }

    static final class FXY_XY4 implements FXY {
        public final double f(double x, double y) {
            return (x) * (y * y * y * y);
        }
        public String getCCodeExpr() {
            return "x * pow(y, 4)";
        }
    }

    static final class FXY_X4 implements FXY {
        public final double f(double x, double y) {
            return (x * x * x * x);
        }
        public String getCCodeExpr() {
            return "pow(x, 4)";
        }
    }

    static final class FXY_Y4 implements FXY {
        public final double f(double x, double y) {
            return (y * y * y * y);
        }
        public String getCCodeExpr() {
            return "pow(y, 4)";
        }
    }

    static final class FXY_X3Y3 implements FXY {

        public final double f(double x, double y) {
            return (x * x * x) * (y * y * y);
        }
        public String getCCodeExpr() {
            return "pow(x, 3) * pow(y, 3)";
        }
    }

    static final class FXY_X3Y2 implements FXY {

        public final double f(double x, double y) {
            return (x * x * x) * (y * y);
        }
        public String getCCodeExpr() {
            return "pow(x, 3) * pow(y, 2)";
        }
    }

    static final class FXY_X2Y3 implements FXY {

        public final double f(double x, double y) {
            return (x * x) * (y * y * y);
        }
        public String getCCodeExpr() {
            return "pow(x, 2) * pow(y, 3)";
        }
    }

    static final class FXY_X3Y implements FXY {

        public final double f(double x, double y) {
            return (x * x * x) * (y);
        }
        public String getCCodeExpr() {
            return "pow(x, 3) * y";
        }
    }

    static final class FXY_XY3 implements FXY {

        public final double f(double x, double y) {
            return (x) * (y * y * y);
        }
        public String getCCodeExpr() {
            return "x * pow(y, 3)";
        }
    }

    static final class FXY_X3 implements FXY {

        public final double f(double x, double y) {
            return (x * x * x);
        }
        public String getCCodeExpr() {
            return "pow(x, 3)";
        }
    }

    static final class FXY_Y3 implements FXY {

        public final double f(double x, double y) {
            return (y * y * y);
        }
        public String getCCodeExpr() {
            return "pow(y, 3)";
        }
    }

    static final class FXY_X2Y2 implements FXY {

        public final double f(double x, double y) {
            return (x * x) * (y * y);
        }
        public String getCCodeExpr() {
            return "pow(x, 2) * pow(y, 2)";
        }
    }

    static final class FXY_X2Y implements FXY {

        public final double f(double x, double y) {
            return (x * x) * (y);
        }
        public String getCCodeExpr() {
            return "pow(x, 2) * y";
        }
    }

    static final class FXY_XY2 implements FXY {

        public final double f(double x, double y) {
            return (x) * (y * y);
        }
        public String getCCodeExpr() {
            return "x * pow(y, 2)";
        }
    }

    static final class FXY_X2 implements FXY {

        public final double f(double x, double y) {
            return (x) * (x);
        }

        public String getCCodeExpr() {
            return "pow(x, 2)";
        }
    }

    static final class FXY_XY implements FXY {

        public final double f(double x, double y) {
            return (x) * (y);
        }
        public String getCCodeExpr() {
            return "x * y";
        }
    }

    static final class FXY_Y2 implements FXY {

        public final double f(double x, double y) {
            return (y * y);
        }
        public String getCCodeExpr() {
            return "pow(y, 2)";
        }
    }

    static final class FXY_X implements FXY {

        public final double f(double x, double y) {
            return (x);
        }
        public String getCCodeExpr() {
            return "x";
        }
    }

    static final class FXY_Y implements FXY {

        public final double f(double x, double y) {
            return (y);
        }
        public String getCCodeExpr() {
            return "y";
        }
    }

    static final class FXY_1 implements FXY {

        public final double f(double x, double y) {
            return 1.0;
        }

        public String getCCodeExpr() {
            return "1";
        }
    }
}
