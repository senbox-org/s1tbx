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

package org.esa.snap.core.jexp;


/**
 * The abstract <code>Term</code> class is an in-memory representation of an
 * element within an arbitrary expression tree. The class defines a number of
 * concrete <code>Term</code> implementations each representing either an
 * an atomic leave (number constant, symbol reference) or a node
 * (e.g. binary operator, function call) within an expression tree.
 * <p> Instances of this class are normally created using an expression parser
 * which implements the <code>{@link org.esa.snap.core.jexp.Parser}</code> interface.
 * The <code>{@link org.esa.snap.core.jexp.impl.ParserImpl}</code> class provides a default
 * implementation of such a parser.
 *
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
public abstract class Term {
    // Term precedences for decompilation
    private static final int Assign_PRE = 990;
    private static final int Cond_PRE = 890;
    private static final int OrB_PRE = 790;
    private static final int AndB_PRE = 780;
    private static final int Comp_PRE = 690;
    private static final int OrI_PRE = 590;
    private static final int XOrI_PRE = 580;
    private static final int AndI_PRE = 570;
    private static final int Add_PRE = 490;
    private static final int Mul_PRE = 390;
    private static final int Unary_PRE = 290;
    //private static final int Postfix_PRE = 190;
    private static final int Primary_PRE = 0;

    /**
     * The ID for the <code>boolean</code> type.
     */
    public final static int TYPE_B = 1;
    /**
     * The ID for the <code>int</code> type.
     */
    public final static int TYPE_I = 2;
    /**
     * The ID for the <code>double</code> type.
     */
    public final static int TYPE_D = 3;
    /**
     * The ID for the <code>string</code> type.
     */
    public final static int TYPE_S = 4;

    /**
     * The empty term array.
     */
    private final static Term[] _EMPTY_TERM_ARRAY = new Term[0];

    /**
     * Gets the term's "natural" return type.
     *
     * @return the type, should always be one of the <code>TYPE_</code>X constants
     * defined in this class.
     */
    public abstract int getRetType();

    /**
     * Evaluates this term to a <code>boolean</code> value.
     *
     * @param context the application dependant environment.
     * @return a <code>boolean</code> value
     * @throws EvalException if the evaluation fails
     */
    public abstract boolean evalB(EvalEnv context);

    /**
     * Evaluates this term to an <code>int</code> value.
     *
     * @param env the application dependant environment.
     * @return an <code>int</code> value
     * @throws EvalException if the evaluation fails
     */
    public abstract int evalI(EvalEnv env);

    /**
     * Evaluates this term to a <code>double</code> value.
     *
     * @param env the application dependant environment.
     * @return a <code>double</code> value
     * @throws EvalException if the evaluation fails
     */
    public abstract double evalD(EvalEnv env);

    /**
     * Visitor support.
     *
     * @param visitor A visitor.
     */
    public abstract <T> T accept(TermVisitor<T> visitor);

    /**
     * Evaluates this term to a <code>String</code> value.
     * The default implementation simply returns the value of {@link #toString()}.
     *
     * @param env the application dependant environment.
     * @return a <code>String</code> value
     * @throws EvalException if the evaluation fails
     */
    public String evalS(EvalEnv env) {
        return toString();
    }

    /**
     * Returns an array of terms which are children of this term.
     *
     * @return an array of terms, never <code>null</code> but can be empty
     */
    public Term[] getChildren() {
        return _EMPTY_TERM_ARRAY;
    }

    /**
     * Returns a string representation of this term which
     * can be used for debugging.
     */
    @Override
    public abstract String toString();

    /**
     * Tests whether or not this term "naturally" returns a <code>boolean</code>.
     *
     * @return <code>true</code> if so
     */
    public final boolean isB() {
        return getRetType() == TYPE_B;
    }

    /**
     * Tests whether or not this term "naturally" returns an <code>int</code>.
     *
     * @return <code>true</code> if so
     */
    public final boolean isI() {
        return getRetType() == TYPE_I;
    }

    /**
     * Tests whether or not this term "naturally" returns a <code>double</code>.
     *
     * @return <code>true</code> if so
     */
    public final boolean isD() {
        return getRetType() == TYPE_D;
    }

    /**
     * Tests whether or not this term "naturally" returns a numeric value.
     *
     * @return <code>true</code> if so
     */
    public final boolean isN() {
        return isI() || isD();
    }

    /**
     * Tests whether or not this term returns a <code>String</code>.
     *
     * @return <code>true</code> if so
     */
    public final boolean isS() {
        return getRetType() == TYPE_S;
    }

    /**
     * Converts an <code>int</code> to a <code>boolean</code>.
     *
     * @param value the value to be converted
     * @return the conversion result, which is <code>value != 0</code>.
     */
    public static boolean toB(final int value) {
        return value != 0;
    }

    /**
     * Converts a <code>double</code> to a <code>boolean</code>.
     *
     * @param value the value to be converted
     * @return the conversion result, which is <code>value != 0.0</code>.
     */
    public static boolean toB(final double value) {
        return value != 0.0;
    }

    /**
     * Converts a <code>boolean</code> to an <code>int</code>.
     *
     * @param value the value to be converted
     * @return the conversion result, which is <code>value ? 1 : 0</code>.
     */
    public static int toI(final boolean value) {
        return value ? 1 : 0;
    }

    /**
     * Converts a <code>double</code> to an <code>int</code>.
     *
     * @param value the value to be converted
     * @return the conversion result, which is <code>(int) value</code>.
     */
    public static int toI(final double value) {
        return (int) value;
    }

    /**
     * Converts a <code>boolean</code> to an <code>double</code>.
     *
     * @param value the value to be converted
     * @return the conversion result, which is <code>value ? 1.0 : 0.0</code>.
     */
    public static double toD(final boolean value) {
        return value ? 1.0 : 0.0;
    }

    /**
     * Converts a <code>boolean</code> to a <code>String</code>.
     *
     * @param value the value to be converted
     * @return the conversion result, which is <code>value ? 1.0 : 0.0</code>.
     */
    public static String toS(final boolean value) {
        return Boolean.toString(value);
    }

    /**
     * Converts a <code>integer</code> to a <code>String</code>.
     *
     * @param value the value to be converted
     * @return the conversion result, which is <code>value ? 1.0 : 0.0</code>.
     */
    public static String toS(final int value) {
        return Integer.toString(value);
    }

    /**
     * Converts a <code>double</code> to a <code>String</code>.
     *
     * @param value the value to be converted
     * @return the conversion result, which is <code>value ? 1.0 : 0.0</code>.
     */
    public static String toS(final double value) {
        return Double.toString(value);
    }

    /**
     * @return {@code true}, if this term evaluates to the same constant value regardless of any {@link EvalEnv},
     * even {@code null}.
     */
    public abstract boolean isConst();


    public abstract int compare(Term other);

    private static String getParamString(final String name, final Term[] args) {
        final StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append('(');
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(args[i].toString());
        }
        sb.append(')');
        return sb.toString();
    }

    public abstract int pre();

    private static int numType(Term t) {
        return t.isD() ? TYPE_D : TYPE_I;
    }

    private static int numType(Term t1, Term t2) {
        return t1.isD() || t2.isD() ? TYPE_D : TYPE_I;
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * A boolean constant, e.g. <code>true</code> or <code>false</code>.
     */
    public static abstract class Const extends Term {

        @Override
        public boolean evalB(final EvalEnv env) {
            return toB();
        }

        protected abstract boolean toB();

        @Override
        public int evalI(final EvalEnv env) {
            return toI();
        }

        protected abstract int toI();

        @Override
        public double evalD(final EvalEnv env) {
            return toD();
        }

        protected abstract double toD();

        @Override
        public String evalS(final EvalEnv env) {
            return toS();
        }

        protected abstract String toS();

        @Override
        public final boolean isConst() {
            return true;
        }

        @Override
        public final int compare(Term other) {
            if (other instanceof ConstB) {
                return toI() - ((ConstB) other).toI();
            } else if (other instanceof ConstI) {
                return toI() - ((ConstI) other).toI();
            } else if (other instanceof ConstD) {
                double delta = toD() - ((ConstD) other).toD();
                return delta == 0.0 ? 0 : delta < 0 ? -1 : +1;
            } else if (other instanceof ConstS) {
                return toS().compareTo(((ConstS) other).toS());
            }
            return -1;
        }

        @Override
        public int pre() {
            return Primary_PRE;
        }
    }

    /**
     * A boolean constant, e.g. <code>true</code> or <code>false</code>.
     */
    public static final class ConstB extends Const {

        public static final ConstB FALSE = new ConstB(false);
        public static final ConstB TRUE = new ConstB(true);

        private final boolean value;

        public ConstB(final boolean value) {
            this.value = value;
        }

        public boolean getValue() {
            return value;
        }

        @Override
        public int getRetType() {
            return TYPE_B;
        }

        @Override
        protected boolean toB() {
            return value;
        }

        @Override
        protected int toI() {
            return toI(value);
        }

        @Override
        protected double toD() {
            return toD(value);
        }

        @Override
        protected String toS() {
            return toS(value);
        }

        @Override
        public String toString() {
            return toS();
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }

    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * An integer constant, e.g. <code>6325</code> or <code>054</code> (octal)
     * or <code>0x49AF</code> (hex).
     */
    public static final class ConstI extends Const {

        public static final ConstI ZERO = new ConstI(0);
        public static final ConstI ONE = new ConstI(1);
        public static final ConstI TWO = new ConstI(2);

        private final int value;

        public static Term.ConstI lookup(int value) {
            if (value == 0) {
                return Term.ConstI.ZERO;
            } else if (value == 1) {
                return Term.ConstI.ONE;
            } else if (value == 2) {
                return Term.ConstI.TWO;
            }
            return null;
        }

        public ConstI(final int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        @Override
        public int getRetType() {
            return TYPE_I;
        }

        @Override
        protected boolean toB() {
            return toB(value);
        }

        @Override
        protected int toI() {
            return value;
        }

        @Override
        protected double toD() {
            return value;
        }

        @Override
        protected String toS() {
            return toS(value);
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * A floating point constant, e.g. <code>2.97665</code> or
     * <code>1.4e-12</code>.
     */
    public static final class ConstD extends Const {

        public static final ConstD NAN = new ConstD(Double.NaN);
        public static final ConstD ZERO = new ConstD(0.0);
        public static final ConstD ONE = new ConstD(1.0);
        public static final ConstD TWO = new ConstD(2.0);
        public static final ConstD HALF = new ConstD(0.5);
        public static final ConstD PI = new ConstD(Math.PI);
        public static final ConstD E = new ConstD(Math.E);

        private final double value;

        public static Term.ConstD lookup(double value) {
            if (Double.isNaN(value)) {
                return Term.ConstD.NAN;
            } else if (eq(value, 0.0)) {
                return Term.ConstD.ZERO;
            } else if (eq(value, 0.5)) {
                return Term.ConstD.HALF;
            } else if (eq(value , 1.0)) {
                return Term.ConstD.ONE;
            } else if (eq(value , 2.0)) {
                return Term.ConstD.TWO;
            } else if (eq(value, Math.PI)) {
                return Term.ConstD.PI;
            } else if (eq(value, Math.E)) {
                return Term.ConstD.E;
            }
            return null;
        }

        public ConstD(final double value) {
            this.value = value;
        }

        public double getValue() {
            return value;
        }

        @Override
        public int getRetType() {
            return TYPE_D;
        }

        @Override
        protected boolean toB() {
            return toB(value);
        }

        @Override
        protected int toI() {
            return toI(value);
        }

        @Override
        protected double toD() {
            return value;
        }

        @Override
        protected String toS() {
            return toS(value);
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }

        /**
         * Special comparison only valid for numbers whose exponent is -1 to +1.
         */
        public static boolean eq(double v1, double v2) {
            return Math.abs(v1 - v2) <= 1e-15;
        }
    }

    public static class ConstS extends Const {
        private final String value;

        public ConstS(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public int getRetType() {
            return TYPE_S;
        }

        @Override
        protected boolean toB() {
            if (value.equalsIgnoreCase("true")) {
                return true;
            }
            if (value.equalsIgnoreCase("false")) {
                return false;
            }
            throw new EvalException("Cannot convert '" + value + "' to boolean.");
        }

        @Override
        protected int toI() {
            try {
                return Integer.valueOf(value);
            } catch (NumberFormatException e) {
                throw new EvalException("Cannot convert '" + value + "' to int.");
            }
        }

        @Override
        protected double toD() {
            try {
                return Double.valueOf(value);
            } catch (NumberFormatException e) {
                throw new EvalException("Cannot convert '" + value + "' to double.");
            }
        }

        @Override
        protected String toS() {
            return value;
        }

        @Override
        public String toString() {
            return "\"" + value + "\"";
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * A reference to a <code>{@link Symbol}</code>.
     */
    public static final class Ref extends Term {

        protected final Symbol symbol;

        public Ref(final Symbol symbol) {
            this.symbol = symbol;
        }

        public Symbol getSymbol() {
            return symbol;
        }

        public Variable getVariable() {
            return (symbol instanceof Variable) ? (Variable) symbol : null;
        }

        @Override
        public int getRetType() {
            return symbol.getRetType();
        }

        @Override
        public boolean evalB(final EvalEnv env) {
            return symbol.evalB(env);
        }

        @Override
        public int evalI(final EvalEnv env) {
            return symbol.evalI(env);
        }

        @Override
        public double evalD(final EvalEnv env) {
            return symbol.evalD(env);
        }

        @Override
        public String evalS(EvalEnv env) {
            return symbol.evalS(env);
        }

        @Override
        public String toString() {
            return symbol.getName();
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean isConst() {
            return symbol.isConst();
        }

        @Override
        public int compare(Term other) {
            if (other instanceof Const) {
                return 1;
            }
            if (other instanceof Ref) {
                return symbol.getName().compareTo(((Ref) other).symbol.getName());
            }
            return -1;
        }

        @Override
        public int pre() {
            return Primary_PRE;
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * A call of a (or reference to a) <code>{@link Function}</code>.
     */
    public static final class Call extends Term {

        protected final Function function;
        protected final Term[] args;

        public Call(final Function function, final Term... args) {
            this.function = function;
            this.args = args;
        }

        @Override
        public int getRetType() {
            return function.getRetType();
        }

        public Function getFunction() {
            return function;
        }

        public int getArgCount() {
            return args.length;
        }

        public Term getArg() {
            return args[0];
        }

        public Term getArg(int index) {
            return args[index];
        }

        public Term[] getArgs() {
            return args;
        }

        @Override
        public boolean evalB(final EvalEnv env) {
            return function.evalB(env, args);
        }

        @Override
        public int evalI(final EvalEnv env) {
            return function.evalI(env, args);
        }

        @Override
        public double evalD(final EvalEnv env) {
            return function.evalD(env, args);
        }

        @Override
        public Term[] getChildren() {
            return getArgs();
        }

        @Override
        public String toString() {
            return getParamString(function.getName(), args);
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean isConst() {
            return function.isConst(args);
        }

        @Override
        public int compare(Term other) {
            if (other instanceof Const) {
                return 1;
            }
            if (other instanceof Ref) {
                return 1;
            }
            if (other instanceof Call) {
                Call otherCall = (Call) other;
                int i = function.getName().compareTo(otherCall.function.getName());
                if (i != 0) {
                    return i;
                }
                i = getArgCount() - otherCall.getArgCount();
                if (i != 0) {
                    return i;
                }
                for (int j = 0; j < getArgCount(); j++) {
                    i = getArg(j).compare(otherCall.getArg(j));
                    if (i != 0) {
                        return i;
                    }
                }
                return 0;
            }
            return -1;
        }

        @Override
        public int pre() {
            return Primary_PRE;
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * An abstract operation.
     */
    public static abstract class Op extends Term {

        protected final String name;
        protected final int type;
        protected final Term[] args;

        protected Op(final String name, final int type, final Term[] args) {
            this.name = name.intern();
            this.type = type;
            this.args = args;
        }

        public String getName() {
            return name;
        }

        @Override
        public int getRetType() {
            return type;
        }

        public int getArgCount() {
            return args.length;
        }

        public Term getArg() {
            return args[0];
        }

        public Term getArg(int index) {
            return args[index];
        }

        public Term[] getArgs() {
            return args;
        }

        @Override
        public Term[] getChildren() {
            return getArgs();
        }

        @Override
        public String toString() {
            return getParamString(name, args);
        }

        @Override
        public boolean isConst() {
            for (Term arg : args) {
                if (!arg.isConst()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int compare(Term other) {
            if (other instanceof Const) {
                return 1;
            }
            if (other instanceof Ref) {
                return 1;
            }
            if (other instanceof Call) {
                return 1;
            }
            if (other instanceof Op) {
                Op otherOp = (Op) other;
                int i = getName().compareTo(otherOp.getName());
                if (i != 0) {
                    return i;
                }
                i = getArgCount() - otherOp.getArgCount();
                if (i != 0) {
                    return i;
                }
                for (int j = 0; j < getArgCount(); j++) {
                    i = getArg(j).compare(otherOp.getArg(j));
                    if (i != 0) {
                        return i;
                    }
                }
                return 0;
            }
            return -1;
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * An abstract unary (= 1 operand) operation.
     */
    public static abstract class Unary extends Op {

        protected final Term arg;

        protected Unary(final String name, final int type, final Term arg) {
            super(name, type, new Term[]{arg});
            this.arg = arg;
        }

        @Override
        public final int pre() {
            return Unary_PRE;
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * An abstract unary (= 1 operand) operation with return type of
     * <code>boolean</code>.
     */
    public static abstract class UnaryB extends Unary {

        protected UnaryB(final String name, final Term arg) {
            super(name, TYPE_B, arg);
        }

        @Override
        public int evalI(final EvalEnv env) {
            return evalB(env) ? 1 : 0;
        }

        @Override
        public double evalD(final EvalEnv env) {
            return evalB(env) ? 1.0 : 0.0;
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * An abstract unary (= 1 operand) operation with return type of
     * <code>int</code>.
     */
    public static abstract class UnaryI extends Unary {

        protected UnaryI(final String name, final Term arg) {
            super(name, TYPE_I, arg);
        }

        @Override
        public boolean evalB(final EvalEnv env) {
            return toB(evalI(env));
        }

        @Override
        public double evalD(final EvalEnv env) {
            return evalI(env);
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * An abstract unary (= 1 operand) operation with a numeric return type.
     */
    public static abstract class UnaryN extends Unary {

        protected UnaryN(final String name, final int type, final Term arg) {
            super(name, type, arg);
        }

        @Override
        public boolean evalB(final EvalEnv env) {
            return toB(evalD(env));
        }
    }

    /////////////////////////////////////////////////////////////////////////

    public static abstract class Binary extends Op {

        protected final Term arg1;
        protected final Term arg2;

        protected Binary(final String name, final int type, final Term arg1, final Term arg2) {
            super(name, type, new Term[]{arg1, arg2});
            this.arg1 = arg1;
            this.arg2 = arg2;
        }

    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * An abstract binary (= 2 operands) operation.
     */
    public static abstract class BinaryB extends Binary {

        protected BinaryB(final String name, final Term arg1, final Term arg2) {
            super(name, TYPE_B, arg1, arg2);
        }

        @Override
        public int evalI(final EvalEnv env) {
            //throw new EvalException("illegal operation");
            return evalB(env) ? 1 : 0;
        }

        @Override
        public double evalD(final EvalEnv env) {
            //throw new EvalException("illegal operation");
            return evalB(env) ? 1.0 : 0.0;
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * An abstract binary (= 2 operands) operation with a return type
     * of <code>int</code>.
     */
    public static abstract class BinaryI extends Binary {

        protected BinaryI(final String name, final Term arg1, final Term arg2) {
            super(name, TYPE_I, arg1, arg2);
        }

        @Override
        public boolean evalB(final EvalEnv env) {
            return toB(evalI(env));
        }

        @Override
        public double evalD(final EvalEnv env) {
            return evalI(env);
        }
    }

    /**
     * An abstract unary (= 1 operand) operation.
     */
    /**
     * An abstract binary (= 2 operands) operation with a numeric return type.
     */
    public static abstract class BinaryN extends Binary {

        protected BinaryN(final String name, final int type, final Term arg1, final Term arg2) {
            super(name, type, arg1, arg2);
        }

        @Override
        public boolean evalB(final EvalEnv env) {
            return type == TYPE_I ? toB(evalI(env)) : toB(evalD(env));
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * The conditional operation
     * <blockquote>
     * <i>b-term</i> <code>?</code> <i>term</i> <code>:</code> <i>term</i>
     * </blockquote>
     */
    public static final class Cond extends Op {

        protected final Term arg1;
        protected final Term arg2;
        protected final Term arg3;

        public Cond(final int type, final Term arg1, final Term arg2, final Term arg3) {
            super("Cond", type, new Term[]{arg1, arg2, arg3});
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.arg3 = arg3;
        }

        @Override
        public boolean evalB(final EvalEnv env) {
            return arg1.evalB(env) ? arg2.evalB(env) : arg3.evalB(env);
        }

        @Override
        public int evalI(final EvalEnv env) {
            return arg1.evalB(env) ? arg2.evalI(env) : arg3.evalI(env);
        }

        @Override
        public double evalD(final EvalEnv env) {
            return arg1.evalB(env) ? arg2.evalD(env) : arg3.evalD(env);
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public int pre() {
            return Cond_PRE;
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * The assignment operation
     * <blockquote>
     * <i>variable-ref-term</i> <code>=</code> <i>term</i>
     * </blockquote>
     */
    public static final class Assign extends Binary {

        public Assign(final Term arg1, final Term arg2) {
            super("Assign", arg1.getRetType(), arg1, arg2);
        }

        @Override
        public boolean evalB(final EvalEnv context) {
            throw new EvalException("not implemented");
        }

        @Override
        public int evalI(final EvalEnv env) {
            throw new EvalException("not implemented");
        }

        @Override
        public double evalD(final EvalEnv env) {
            throw new EvalException("not implemented");
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public int pre() {
            return Assign_PRE;
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * The logical NOT operation
     * <blockquote>
     * <code>!</code> <i>b-term</i>
     * </blockquote>
     */
    public static final class NotB extends UnaryB {

        public NotB(final Term arg) {
            super("NotB", arg);
        }

        @Override
        public boolean evalB(final EvalEnv env) {
            return !arg.evalB(env);
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }

    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * The logical AND operation:
     * <blockquote>
     * <i>b-term</i> <code>&amp;&amp;</code> <i>b-term</i>
     * </blockquote>
     */
    public static final class AndB extends BinaryB {

        public AndB(final Term arg1, final Term arg2) {
            super("AndB", arg1, arg2);
        }

        @Override
        public boolean evalB(final EvalEnv env) {
            return arg1.evalB(env) && arg2.evalB(env);
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public int pre() {
            return AndB_PRE;
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * The logical OR operation:
     * <blockquote>
     * <i>b-term</i> <code>||</code> <i>b-term</i>
     * </blockquote>
     */
    public static final class OrB extends BinaryB {

        public OrB(final Term arg1, final Term arg2) {
            super("OrB", arg1, arg2);
        }

        @Override
        public boolean evalB(final EvalEnv env) {
            return arg1.evalB(env) || arg2.evalB(env);
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public int pre() {
            return OrB_PRE;
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * The bitwise NOT operation:
     * <blockquote>
     * <code>~</code> <i>i-term</i>
     * </blockquote>
     */
    public static final class NotI extends UnaryI {

        public NotI(final Term arg) {
            super("NotI", arg);
        }

        @Override
        public int evalI(final EvalEnv env) {
            return ~arg.evalI(env);
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * The bitwise XOR operation:
     * <blockquote>
     * <i>i-term</i> <code>^</code> <i>i-term</i>
     * </blockquote>
     */
    public static final class XOrI extends BinaryI {

        public XOrI(final Term arg1, final Term arg2) {
            super("XOrI", arg1, arg2);
        }

        @Override
        public int evalI(final EvalEnv env) {
            return arg1.evalI(env) ^ arg2.evalI(env);
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public int pre() {
            return XOrI_PRE;
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * The bitwise AND operation:
     * <blockquote>
     * <i>i-term</i> <code>&amp;</code> <i>i-term</i>
     * </blockquote>
     */
    public static final class AndI extends BinaryI {

        public AndI(final Term arg1, final Term arg2) {
            super("AndI", arg1, arg2);
        }

        @Override
        public int evalI(final EvalEnv env) {
            return arg1.evalI(env) & arg2.evalI(env);
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public int pre() {
            return AndI_PRE;
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * The bitwise OR operation:
     * <blockquote>
     * <i>i-term</i> <code>&brvbar;</code> <i>i-term</i>
     * </blockquote>
     */
    public static final class OrI extends BinaryI {

        public OrI(final Term arg1, final Term arg2) {
            super("OrI", arg1, arg2);
        }

        @Override
        public int evalI(final EvalEnv env) {
            return arg1.evalI(env) | arg2.evalI(env);
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public int pre() {
            return OrI_PRE;
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * The numerical NEG operation:
     * <blockquote>
     * <code>-</code> <i>d-term</i>
     * </blockquote>
     */
    public static final class Neg extends UnaryN {

        public Neg(final Term arg) {
            this(numType(arg), arg);
        }

        public Neg(final int type, final Term arg) {
            super("Neg", type, arg);
        }

        @Override
        public int evalI(final EvalEnv env) {
            return -arg.evalI(env);
        }

        @Override
        public double evalD(final EvalEnv env) {
            return -arg.evalD(env);
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /////////////////////////////////////////////////////////////////////////

    /**
     * The numerical ADD operation:
     * <blockquote>
     * <i>n-term</i> <code>+</code> <i>n-term</i>
     * </blockquote>
     */
    public static final class Add extends BinaryN {

        public Add(final Term arg1, final Term arg2) {
            this(numType(arg1, arg2), arg1, arg2);
        }

        public Add(final int type, final Term arg1, final Term arg2) {
            super("Add", type, arg1, arg2);
        }

        @Override
        public int evalI(final EvalEnv env) {
            return arg1.evalI(env) + arg2.evalI(env);
        }

        @Override
        public double evalD(final EvalEnv env) {
            return arg1.evalD(env) + arg2.evalD(env);
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public int pre() {
            return Add_PRE;
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * The numerical SUB operation:
     * <blockquote>
     * <i>n-term</i> <code>-</code> <i>n-term</i>
     * </blockquote>
     */
    public static final class Sub extends BinaryN {

        public Sub(final Term arg1, final Term arg2) {
            this(numType(arg1, arg2), arg1, arg2);
        }

        public Sub(final int type, final Term arg1, final Term arg2) {
            super("Sub", type, arg1, arg2);
        }

        @Override
        public int evalI(final EvalEnv env) {
            return arg1.evalI(env) - arg2.evalI(env);
        }

        @Override
        public double evalD(final EvalEnv env) {
            return arg1.evalD(env) - arg2.evalD(env);
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public int pre() {
            return Add_PRE;
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * The numerical MUL operation:
     * <blockquote>
     * <i>n-term</i> <code>*</code> <i>n-term</i>
     * </blockquote>
     */
    public static final class Mul extends BinaryN {

        public Mul(final Term arg1, final Term arg2) {
            this(numType(arg1, arg2), arg1, arg2);
        }

        public Mul(final int type, final Term arg1, final Term arg2) {
            super("Mul", type, arg1, arg2);
        }

        @Override
        public int evalI(final EvalEnv env) {
            return arg1.evalI(env) * arg2.evalI(env);
        }

        @Override
        public double evalD(final EvalEnv env) {
            return arg1.evalD(env) * arg2.evalD(env);
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public int pre() {
            return Mul_PRE;
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * The numerical DIV operation:
     * <blockquote>
     * <i>n-term</i> <code>/</code> <i>n-term</i>
     * </blockquote>
     */
    public static final class Div extends BinaryN {

        public Div(final Term arg1, final Term arg2) {
            this(numType(arg1, arg2), arg1, arg2);
        }

        public Div(final int type, final Term arg1, final Term arg2) {
            super("Div", type, arg1, arg2);
        }

        @Override
        public int evalI(final EvalEnv env) {
            return arg1.evalI(env) / arg2.evalI(env);
        }

        @Override
        public double evalD(final EvalEnv env) {
            return arg1.evalD(env) / arg2.evalD(env);
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public int pre() {
            return Mul_PRE;
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * The numerical MOD (modulo) operation:
     * <blockquote>
     * <i>n-term</i> <code>%</code> <i>n-term</i>
     * </blockquote>
     */
    public static final class Mod extends BinaryN {

        public Mod(final Term arg1, final Term arg2) {
            this(numType(arg1, arg2), arg1, arg2);
        }

        public Mod(final int type, final Term arg1, final Term arg2) {
            super("Mod", type, arg1, arg2);
        }

        @Override
        public int evalI(final EvalEnv env) {
            return arg1.evalI(env) % arg2.evalI(env);
        }

        @Override
        public double evalD(final EvalEnv env) {
            return arg1.evalD(env) % arg2.evalD(env);
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public int pre() {
            return Mul_PRE;
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * The boolean EQ operation:
     * <blockquote>
     * <i>b-term</i> <code>==</code> <i>b-term</i>
     * </blockquote>
     */
    public static final class EqB extends BinaryB {

        public EqB(final Term arg1, final Term arg2) {
            super("EqB", arg1, arg2);
        }

        @Override
        public boolean evalB(final EvalEnv env) {
            return arg1.evalB(env) == arg2.evalB(env);
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public int pre() {
            return Comp_PRE;
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * The integer EQ operation:
     * <blockquote>
     * <i>i-term</i> <code>==</code> <i>i-term</i>
     * </blockquote>
     */
    public static final class EqI extends BinaryB {

        public EqI(final Term arg1, final Term arg2) {
            super("EqI", arg1, arg2);
        }

        @Override
        public boolean evalB(final EvalEnv env) {
            return arg1.evalI(env) == arg2.evalI(env);
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public int pre() {
            return Comp_PRE;
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * The double EQ operation:
     * <blockquote>
     * <i>d-term</i> <code>==</code> <i>d-term</i>
     * </blockquote>
     */
    public static final class EqD extends BinaryB {

        public EqD(final Term arg1, final Term arg2) {
            super("EqD", arg1, arg2);
        }

        @Override
        public boolean evalB(final EvalEnv env) {
            return arg1.evalD(env) == arg2.evalD(env);
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public int pre() {
            return Comp_PRE;
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * The boolean NEQ operation:
     * <blockquote>
     * <i>b-term</i> <code>!=</code> <i>b-term</i>
     * </blockquote>
     */
    public static final class NEqB extends BinaryB {

        public NEqB(final Term arg1, final Term arg2) {
            super("NEqB", arg1, arg2);
        }

        @Override
        public boolean evalB(final EvalEnv env) {
            return arg1.evalB(env) != arg2.evalB(env);
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public int pre() {
            return Comp_PRE;
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * The integer NEQ operation:
     * <blockquote>
     * <i>i-term</i> <code>!=</code> <i>i-term</i>
     * </blockquote>
     */
    public static final class NEqI extends BinaryB {

        public NEqI(final Term arg1, final Term arg2) {
            super("NEqI", arg1, arg2);
        }

        @Override
        public boolean evalB(final EvalEnv env) {
            return arg1.evalI(env) != arg2.evalI(env);
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public int pre() {
            return Comp_PRE;
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * The double NEQ operation:
     * <blockquote>
     * <i>d-term</i> <code>!=</code> <i>d-term</i>
     * </blockquote>
     */
    public static final class NEqD extends BinaryB {

        public NEqD(final Term arg1, final Term arg2) {
            super("NEqD", arg1, arg2);
        }

        @Override
        public boolean evalB(final EvalEnv env) {
            return arg1.evalD(env) != arg2.evalD(env);
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public int pre() {
            return Comp_PRE;
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * The integer LT operation:
     * <blockquote>
     * <i>i-term</i> <code>&lt;</code> <i>i-term</i>
     * </blockquote>
     */
    public static final class LtI extends BinaryB {

        public LtI(final Term arg1, final Term arg2) {
            super("LtI", arg1, arg2);
        }

        @Override
        public boolean evalB(final EvalEnv env) {
            return arg1.evalI(env) < arg2.evalI(env);
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public int pre() {
            return Comp_PRE;
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * The double LT operation:
     * <blockquote>
     * <i>d-term</i> <code>&lt;</code> <i>d-term</i>
     * </blockquote>
     */
    public static final class LtD extends BinaryB {

        public LtD(final Term arg1, final Term arg2) {
            super("LtD", arg1, arg2);
        }

        @Override
        public boolean evalB(final EvalEnv env) {
            return arg1.evalD(env) < arg2.evalD(env);
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public int pre() {
            return Comp_PRE;
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * The integer LE operation:
     * <blockquote>
     * <i>i-term</i> <code>&lt;=</code> <i>i-term</i>
     * </blockquote>
     */
    public static final class LeI extends BinaryB {

        public LeI(final Term arg1, final Term arg2) {
            super("LeI", arg1, arg2);
        }

        @Override
        public boolean evalB(final EvalEnv env) {
            return arg1.evalI(env) <= arg2.evalI(env);
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public int pre() {
            return Comp_PRE;
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * The double LE operation:
     * <blockquote>
     * <i>d-term</i> <code>&lt;=</code> <i>d-term</i>
     * </blockquote>
     */
    public static final class LeD extends BinaryB {

        public LeD(final Term arg1, final Term arg2) {
            super("LeD", arg1, arg2);
        }

        @Override
        public boolean evalB(final EvalEnv env) {
            return arg1.evalD(env) <= arg2.evalD(env);
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public int pre() {
            return Comp_PRE;
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * The integer GT operation:
     * <blockquote>
     * <i>i-term</i> <code>&gt;</code> <i>i-term</i>
     * </blockquote>
     */
    public static final class GtI extends BinaryB {

        public GtI(final Term arg1, final Term arg2) {
            super("GtI", arg1, arg2);
        }

        @Override
        public boolean evalB(final EvalEnv env) {
            return arg1.evalI(env) > arg2.evalI(env);
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public int pre() {
            return Comp_PRE;
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * The double GT operation:
     * <blockquote>
     * <i>d-term</i> <code>&gt;</code> <i>d-term</i>
     * </blockquote>
     */
    public static final class GtD extends BinaryB {

        public GtD(final Term arg1, final Term arg2) {
            super("GtD", arg1, arg2);
        }

        @Override
        public boolean evalB(final EvalEnv env) {
            return arg1.evalD(env) > arg2.evalD(env);
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public int pre() {
            return Comp_PRE;
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * The integer GE operation:
     * <blockquote>
     * <i>i-term</i> <code>&gt;=</code> <i>i-term</i>
     * </blockquote>
     */
    public static final class GeI extends BinaryB {

        public GeI(final Term arg1, final Term arg2) {
            super("GeI", arg1, arg2);
        }

        @Override
        public boolean evalB(final EvalEnv env) {
            return arg1.evalI(env) >= arg2.evalI(env);
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public int pre() {
            return Comp_PRE;
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * The double GE operation:
     * <blockquote>
     * <i>d-term</i> <code>&gt;=</code> <i>d-term</i>
     * </blockquote>
     */
    public static final class GeD extends BinaryB {

        public GeD(final Term arg1, final Term arg2) {
            super("GeD", arg1, arg2);
        }

        @Override
        public boolean evalB(final EvalEnv env) {
            return arg1.evalD(env) >= arg2.evalD(env);
        }

        @Override
        public <T> T accept(TermVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public int pre() {
            return Comp_PRE;
        }
    }
}
