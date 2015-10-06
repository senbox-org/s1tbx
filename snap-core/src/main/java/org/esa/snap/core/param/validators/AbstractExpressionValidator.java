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
package org.esa.snap.core.param.validators;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.jexp.EvalEnv;
import org.esa.snap.core.jexp.EvalException;
import org.esa.snap.core.jexp.Function;
import org.esa.snap.core.jexp.Namespace;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.jexp.Parser;
import org.esa.snap.core.jexp.Symbol;
import org.esa.snap.core.jexp.Term;
import org.esa.snap.core.jexp.impl.AbstractFunction;
import org.esa.snap.core.jexp.impl.AbstractSymbol;
import org.esa.snap.core.jexp.impl.DefaultNamespace;
import org.esa.snap.core.jexp.impl.ParserImpl;
import org.esa.snap.core.param.ParamParseException;
import org.esa.snap.core.param.ParamProperties;
import org.esa.snap.core.param.Parameter;
import org.esa.snap.core.util.Debug;

/**
 * Validates boolean and general expressions.
 */
public abstract class AbstractExpressionValidator extends StringValidator {
    public final static String PROPERTY_KEY_SELECTED_PRODUCT = "selectedProduct";
    public final static String PROPERTY_KEY_INPUT_PRODUCTS = "inputProducts";
    public final static String PROPERTY_KEY_PREFERENCES = "preferences";

    protected AbstractExpressionValidator() {
    }

    /**
     * Parses a boolean expression.
     * @param parameter the expression parameter
     * @param text the expression text to be parsed
     * @return the validated text
     * @throws ParamParseException
     */
    @Override
    public Object parse(Parameter parameter, String text) throws ParamParseException {

        Debug.assertTrue(text != null);

        if (isAllowedNullText(parameter, text)) {
            return null;
        }

        Parser parser = getParser(parameter);
        try {
            parser.parse(text);
        } catch (ParseException e) {
            throw new ParamParseException(parameter, e.getMessage());
        }

        // Just return text.
        return text;
    }

    @Override
    public boolean equalValues(Parameter parameter, Object value1, Object value2) {
        return equalValues(false, value1, value2);
    }

    private Parser getParser(Parameter parameter) {
        final ParamProperties properties = parameter.getProperties();
        final Object propertyValue = properties.getPropertyValue(PROPERTY_KEY_SELECTED_PRODUCT);
        final Parser parser;
        if (propertyValue instanceof Product) {
            final Product product = (Product) propertyValue;
            parser = product.createBandArithmeticParser();
        } else {
            parser = createFallbackParser();
        }
        return parser;
    }

    private ParserImpl createFallbackParser() {
        final Namespace fallbackNS = new DummyNamespace();
        final DefaultNamespace defaultNS = new DefaultNamespace(fallbackNS);
        return new ParserImpl(defaultNS, false);
    }

    private static class DummyNamespace implements Namespace {

        public Symbol resolveSymbol(String name) {
            return new DummySymbol(name);
        }

        public Function resolveFunction(String name, Term[] args) {
            return new DummyFunction(name, args);
        }
    }

    private static class DummySymbol extends AbstractSymbol {

        public DummySymbol(String name) {
            super(name, Term.TYPE_D);
        }

        public boolean evalB(EvalEnv env) throws EvalException {
            return false;
        }

        public int evalI(EvalEnv env) throws EvalException {
            return 0;
        }

        public double evalD(EvalEnv env) throws EvalException {
            return 0;
        }

        public String evalS(EvalEnv env) throws EvalException {
            return "0";
        }
    }

    private static class DummyFunction extends AbstractFunction.D {

        public DummyFunction(String name, Term[] args) {
            super(name, args.length);
        }

        public double evalD(EvalEnv env, Term[] args) throws EvalException {
            return 0;
        }
    }
}
