package org.esa.snap.core.jexp.impl;

import org.apache.commons.math3.util.FastMath;
import org.esa.snap.core.jexp.EvalEnv;
import org.esa.snap.core.jexp.EvalException;
import org.esa.snap.core.jexp.Function;
import org.esa.snap.core.jexp.Term;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Common function definitions.
 *
 * @author Norman Fomferra
 */
@SuppressWarnings("unused")
public class Functions {
    
    public static final Function AVG = new AbstractFunction.D("avg", -1) {

        public double evalD(final EvalEnv env, final Term[] args) {
            return ExtMath.mean(env, args);
        }
    };

    public static final Function FNEQ = new AbstractFunction.B("fneq", 2) {
        public boolean evalB(EvalEnv env, Term[] args) throws EvalException {
            final double x1 = args[0].evalD(env);
            final double x2 = args[1].evalD(env);
            return ExtMath.fneq(x1, x2, 1e-6);
        }
    };

    public static final Function FEQ = new AbstractFunction.B("feq", 2) {
        public boolean evalB(EvalEnv env, Term[] args) throws EvalException {
            final double x1 = args[0].evalD(env);
            final double x2 = args[1].evalD(env);
            return ExtMath.feq(x1, x2, 1e-6);
        }
    };

    public static final Function SIN = new AbstractFunction.D("sin", 1) {

        public double evalD(final EvalEnv env, final Term[] args) {
            return FastMath.sin(args[0].evalD(env));
        }
    };

    public static final Function COS = new AbstractFunction.D("cos", 1) {

        public double evalD(final EvalEnv env, final Term[] args) {
            return FastMath.cos(args[0].evalD(env));
        }
    };

    public static final Function TAN = new AbstractFunction.D("tan", 1) {

        public double evalD(final EvalEnv env, final Term[] args) {
            return FastMath.tan(args[0].evalD(env));
        }
    };

    public static final Function ASIN = new AbstractFunction.D("asin", 1) {

        public double evalD(final EvalEnv env, final Term[] args) {
            return FastMath.asin(args[0].evalD(env));
        }
    };

    public static final Function ACOS = new AbstractFunction.D("acos", 1) {

        public double evalD(final EvalEnv env, final Term[] args) {
            return FastMath.acos(args[0].evalD(env));
        }
    };

    public static final Function ATAN = new AbstractFunction.D("atan", 1) {

        public double evalD(final EvalEnv env, final Term[] args) {
            return FastMath.atan(args[0].evalD(env));
        }
    };

    public static final Function ATAN2 = new AbstractFunction.D("atan2", 2) {

        public double evalD(final EvalEnv env, final Term[] args) {
            return Math.atan2(args[0].evalD(env), args[1].evalD(env));
        }
    };

    public static final Function LOG = new AbstractFunction.D("log", 1) {

        public double evalD(final EvalEnv env, final Term[] args) {
            return Math.log(args[0].evalD(env));
        }
    };

    public static final Function LOG10 = new AbstractFunction.D("log10", 1) {
        public double evalD(EvalEnv env, Term[] args) throws EvalException {
            return Math.log10(args[0].evalD(env));
        }
    };

    public static final Function EXP = new AbstractFunction.D("exp", 1) {

        public double evalD(final EvalEnv env, final Term[] args) {
            return FastMath.exp(args[0].evalD(env));
        }
    };

    public static final Function EXP10 = new AbstractFunction.D("exp10", 1) {

        public double evalD(EvalEnv env, Term[] args) throws EvalException {
            return FastMath.pow(10.0, args[0].evalD(env));
        }
    };

    public static final Function SQ = new AbstractFunction.D("sq", 1) {

        public double evalD(final EvalEnv env, final Term[] args) {
            double v = args[0].evalD(env);
            return v * v;
        }
    };

    public static final Function SQRT = new AbstractFunction.D("sqrt", 1) {

        public double evalD(final EvalEnv env, final Term[] args) {
            return Math.sqrt(args[0].evalD(env));
        }
    };

    public static final Function POW = new AbstractFunction.D("pow", 2) {

        public double evalD(final EvalEnv env, final Term[] args) {
            return FastMath.pow(args[0].evalD(env), args[1].evalD(env));
        }
    };

    public static final Function MIN_I = new AbstractFunction.I("min", 2) {

        public int evalI(final EvalEnv env, final Term[] args) {
            return Math.min(args[0].evalI(env), args[1].evalI(env));
        }
    };

    public static final Function MIN_D = new AbstractFunction.D("min", 2) {

        public double evalD(final EvalEnv env, final Term[] args) {
            return Math.min(args[0].evalD(env), args[1].evalD(env));
        }
    };

    public static final Function MAX_I = new AbstractFunction.I("max", 2) {

        public int evalI(final EvalEnv env, final Term[] args) {
            return Math.max(args[0].evalI(env), args[1].evalI(env));
        }
    };

    public static final Function MAX_D = new AbstractFunction.D("max", 2) {

        public double evalD(final EvalEnv env, final Term[] args) {
            return Math.max(args[0].evalD(env), args[1].evalD(env));
        }
    };

    public static final Function FLOOR = new AbstractFunction.D("floor", 1) {
        public double evalD(EvalEnv env, Term[] args) throws EvalException {
            return Math.floor(args[0].evalD(env));
        }
    };

    public static final Function ROUND = new AbstractFunction.D("round", 1) {
        public double evalD(EvalEnv env, Term[] args) throws EvalException {
            return Math.round(args[0].evalD(env));
        }
    };

    public static final Function CEIL = new AbstractFunction.D("ceil", 1) {
        public double evalD(EvalEnv env, Term[] args) throws EvalException {
            return Math.ceil(args[0].evalD(env));
        }
    };

    public static final Function RÌNT = new AbstractFunction.D("rint", 1) {
        public double evalD(EvalEnv env, Term[] args) throws EvalException {
            return Math.rint(args[0].evalD(env));
        }
    };

    public static final Function SIGN_I = new AbstractFunction.I("sign", 1) {

        public int evalI(final EvalEnv env, final Term[] args) {
            return ExtMath.sign(args[0].evalI(env));
        }
    };

    public static final Function SIGN_D = new AbstractFunction.D("sign", 1) {

        public double evalD(final EvalEnv env, final Term[] args) {
            return ExtMath.sign(args[0].evalD(env));
        }
    };

    public static final Function ABS_I = new AbstractFunction.I("abs", 1) {

        public int evalI(final EvalEnv env, final Term[] args) {
            return Math.abs(args[0].evalI(env));
        }
    };

    public static final Function ABS_D = new AbstractFunction.D("abs", 1) {

        public double evalD(final EvalEnv env, final Term[] args) {
            return Math.abs(args[0].evalD(env));
        }
    };

    public static final Function DEG = new AbstractFunction.D("deg", 1) {

        public double evalD(final EvalEnv env, final Term[] args) {
            return Math.toDegrees(args[0].evalD(env));
        }
    };

    public static final Function RAD = new AbstractFunction.D("rad", 1) {

        public double evalD(final EvalEnv env, final Term[] args) {
            return Math.toRadians(args[0].evalD(env));
        }
    };

    public static final Function AMPL = new AbstractFunction.D("ampl", 2) {

        public double evalD(final EvalEnv env, final Term[] args) {
            final double a = args[0].evalD(env);
            final double b = args[1].evalD(env);
            return Math.sqrt(a * a + b * b);
        }
    };

    public static final Function PHASE = new AbstractFunction.D("phase", 2) {

        public double evalD(final EvalEnv env, final Term[] args) {
            final double a = args[0].evalD(env);
            final double b = args[1].evalD(env);
            return Math.atan2(b, a);
        }
    };

    public static final Function FNEQ_EPS = new AbstractFunction.B("fneq", 3) {
        public boolean evalB(EvalEnv env, Term[] args) throws EvalException {
            final double x1 = args[0].evalD(env);
            final double x2 = args[1].evalD(env);
            final double eps = args[2].evalD(env);
            return ExtMath.fneq(x1, x2, eps);
        }
    };

    public static final Function INF = new AbstractFunction.B("inf", 1) {
        public boolean evalB(EvalEnv env, Term[] args) throws EvalException {
            return Double.isInfinite(args[0].evalD(env));
        }
    };

    public static final Function NAN = new AbstractFunction.B("nan", 1) {
        public boolean evalB(EvalEnv env, Term[] args) throws EvalException {
            return Double.isNaN(args[0].evalD(env));
        }
    };

    public static final Function DISTANCE = new AbstractFunction.D("distance", -1) {

        public double evalD(final EvalEnv env, final Term[] args) {
            double sqrSum = 0.0;
            final int n = args.length / 2;
            for (int i = 0; i < n; i++) {
                final double v = args[i + n].evalD(env) - args[i].evalD(env);
                sqrSum += v * v;
            }
            return Math.sqrt(sqrSum);
        }
    };

    public static final Function FEQ_EPS = new AbstractFunction.B("feq", 3) {
        public boolean evalB(EvalEnv env, Term[] args) throws EvalException {
            final double x1 = args[0].evalD(env);
            final double x2 = args[1].evalD(env);
            final double eps = args[2].evalD(env);
            return ExtMath.feq(x1, x2, eps);
        }
    };

    public static final AbstractFunction.D STDDEV = new AbstractFunction.D("stddev", -1) {

        public double evalD(final EvalEnv env, final Term[] args) {
            double mean = ExtMath.mean(env, args);
            return Math.sqrt(ExtMath.mean2(env, args) - (mean * mean));
        }
    };

    public static final AbstractFunction.D COEF_VAR = new AbstractFunction.D("coef_var", -1) {

        public double evalD(final EvalEnv env, final Term[] args) {
            final double m2 = ExtMath.mean2(env, args);
            return Math.sqrt(ExtMath.mean4(env, args) - (m2 * m2)) / m2;
        }
    };

    private static final Random RANDOM = new Random();

    public static final Function RANDOM_UNIFORM = new AbstractFunction.D("random_uniform", 0) {
        @Override
        public double evalD(EvalEnv env, Term[] args) throws EvalException {
            return RANDOM.nextDouble();
        }
    };
    public static final Function RANDOM_GAUSSIAN = new AbstractFunction.D("random_gaussian", 0) {
        @Override
        public double evalD(EvalEnv env, Term[] args) throws EvalException {
            return RANDOM.nextGaussian();
        }
    };
    public static final Function SINH = new AbstractFunction.D("sinh", 1) {
        @Override
        public double evalD(EvalEnv env, Term[] args) throws EvalException {
            return Math.sinh(args[0].evalD(env));
        }
    };
    public static final Function COSH = new AbstractFunction.D("cosh", 1) {
        @Override
        public double evalD(EvalEnv env, Term[] args) throws EvalException {
            return Math.cosh(args[0].evalD(env));
        }
    };
    public static final Function TANH = new AbstractFunction.D("tanh", 1) {
        @Override
        public double evalD(EvalEnv env, Term[] args) throws EvalException {
            return Math.tanh(args[0].evalD(env));
        }
    };
    public static final Function SECH = new AbstractFunction.D("sech", 1) {
        @Override
        public double evalD(EvalEnv env, Term[] args) throws EvalException {
            return ExtMath.sech(args[0].evalD(env));
        }
    };
    public static final Function COSECH = new AbstractFunction.D("cosech", 1) {
        @Override
        public double evalD(EvalEnv env, Term[] args) throws EvalException {
            return ExtMath.cosech(args[0].evalD(env));
        }
    };

    public static final Function BIT_SET = new AbstractFunction.B("bit_set", 2) {
        @Override
        public boolean evalB(EvalEnv env, Term[] args) throws EvalException {
            final int value = args[0].evalI(env);
            final int bitIndex = args[1].evalI(env);
            return (value & (1L << bitIndex)) != 0;
        }
    };


    public static List<Function> getAll() {
        Field[] declaredFields = Functions.class.getDeclaredFields();
        ArrayList<Function> functions = new ArrayList<>();
        for (Field declaredField : declaredFields) {
            if (Function.class.isAssignableFrom(declaredField.getType())) {
                try {
                    functions.add((Function) declaredField.get(null));
                } catch (IllegalAccessException e) {
                    // ?
                }
            }
        }
        return functions;
    }


}
