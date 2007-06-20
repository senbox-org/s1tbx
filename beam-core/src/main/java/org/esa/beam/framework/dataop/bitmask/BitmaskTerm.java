/*
 * $Id: BitmaskTerm.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.esa.beam.framework.dataop.bitmask;

import java.util.LinkedList;
import java.util.List;

import org.esa.beam.util.StringUtils;

/**
 * The base class for all bit-mask terms. The usual way to create bit-mask terms is the <code>parse</code> method of the
 * bit-mask {@link BitmaskExpressionParser parser}.
 * <p/>
 * <p>Bit-mask terms provide an efficient way to forEachPixel complex bit-mask expressions via the <code>forEachPixel</code>
 * method.
 *
 * @author Norman Fomferra
 * @version $Revision: 1.1.1.1 $ $Date: 2006/09/11 08:16:45 $
 * @see #evaluate(BitmaskTermEvalContext, int)
 * @see BitmaskTermEvalContext
 * @see BitmaskExpressionParser
 */
public abstract class BitmaskTerm {

    /**
     * Constructs a new bit-mask term.
     */
    protected BitmaskTerm() {
    }

    /**
     * Evaluates the bit-mask term in the given context for the current sample index. The context is used to resolve
     * flag dataset references contained in the term and the sample index specifies the current position within the
     * datasets resolved by the context.
     *
     * @param context     the context in which to forEachPixel this term
     * @param sampleIndex the current sample index
     *
     * @return a boolean value as a result of the evaluation
     */
    public abstract boolean evaluate(BitmaskTermEvalContext context, int sampleIndex);

    public abstract boolean equals(Object o);

    public abstract int hashCode();

    public abstract String toString();

    /**
     * Gets the names of all referenced datasets in this term.
     *
     * @return an array containing all referenced dataset names, never <code>null</code>
     */
    public final String[] getReferencedDatasetNames() {
        List nameList = new LinkedList();
        collectReferencedDatasetNames(nameList);
        return (String[]) nameList.toArray(new String[nameList.size()]);
    }

    /**
     * Collects the referenced datasets names into the given list. Called by <code>getReferencedDatasetNames</code>.
     *
     * @see #getReferencedDatasetNames
     */
    protected abstract void collectReferencedDatasetNames(List nameList);

    /**
     * Gets the names of all referenced flags in this term. <p> The flags in the array are returned as strings having
     * the form <code>&lt;dataset-name&gt;.&lt;flag-name&gt;</code>.
     *
     * @return an array containing all referenced flag names, never <code>null</code>
     */
    public final String[] getReferencedFlagNames() {
        List nameList = new LinkedList();
        collectReferencedFlagNames(nameList);
        return (String[]) nameList.toArray(new String[nameList.size()]);
    }

    /**
     * Collects the referenced flags into the given list. Called by <code>getReferencedFlagNames</code>.
     *
     * @see #getReferencedFlagNames
     */
    protected abstract void collectReferencedFlagNames(List nameList);

    /**
     * A flag-referece term comprising a dataset name and a list of flag names.
     */
    public static class FlagReference extends BitmaskTerm {

        /**
         * the name of the referenced dataset
         */
        private String _datasetName;
        /**
         * the names of all referenced flags within the dataset
         */
        private String _flagName;
        /**
         * The current bit-mask evaluation context stored as a result of runtime-optimization
         */
        private BitmaskTermEvalContext _context;
        /**
         * Flag dataset reference created as a result of runtime-optimization
         */
        private FlagDataset _flagDataset;
        /**
         * Flag mask created as a result of runtime-optimization
         */
        private int _flagMask;
        /**
         * constant indicating "flag-mask not yet initialized" state
         */
        private final static int INVALID_MASK = -1;

        /**
         * Constructs a new flag reference for the given dataset name and flag name.
         *
         * @param datasetName the name of the dataset which contains the given flag and provides the samples
         * @param flagName    the name of a flag value contained in the dataset
         */
        public FlagReference(String datasetName, String flagName) {
            _datasetName = datasetName;
            _flagName = flagName;
            _flagMask = INVALID_MASK;
            _flagDataset = null;
        }

        public String getDatasetName() {
            return _datasetName;
        }

        public String getFlagName() {
            return _flagName;
        }

        /**
         * Simply returns
         * <pre>
         *    (dataset.getSampleAt(sampleIndex) & flagMask) == flagMask
         * </pre>
         * where <code>dataset</code> is the dataset referenced by this term and <code>sampleIndex</code> the current
         * position within the dataset. <code>flagMask</code> is the result of a bit-wise-OR operation of all referenced
         * flag values.
         * </p>
         * <p> The method performs a kind of run-time optimization: if the flag dataset isn't resolved yet, it is
         * resolved now and never again within the lifetime of this flag reference..
         *
         * @param context     the context in which to forEachPixel this term
         * @param sampleIndex the current sample index
         *
         * @return a boolean value as a result of the evaluation
         */
        public boolean evaluate(BitmaskTermEvalContext context, int sampleIndex) {
            if (_context != context) {
                resolveReference(context);
            }
            return (_flagDataset.getSampleAt(sampleIndex) & _flagMask) == _flagMask;
        }

        public boolean equals(Object o) {
            return o != null
                    && o.getClass().equals(getClass())
                    && _datasetName.equalsIgnoreCase(((FlagReference) o)._datasetName)
                    && _flagName.equalsIgnoreCase(((FlagReference) o)._flagName);
        }

        public int hashCode() {
            return _datasetName.hashCode() + _flagName.hashCode();
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(_datasetName);
            sb.append('.');
            sb.append(_flagName);
            return sb.toString();
        }

        /**
         * Collects the referenced datasets names into the given list.
         */
        protected void collectReferencedDatasetNames(List nameList) {
            if (!StringUtils.containsIgnoreCase(nameList, _datasetName)) {
                nameList.add(_datasetName);
            }
        }

        /**
         * Collects the referenced flags into the given list.
         */
        protected void collectReferencedFlagNames(List nameList) {
            String flagName = _datasetName + "." + _flagName;
            if (!StringUtils.containsIgnoreCase(nameList, flagName)) {
                nameList.add(flagName);
            }
        }

        private void resolveReference(BitmaskTermEvalContext context) {
            _context = context;
            _flagDataset = context.getFlagDataset(_datasetName);
            _flagMask = _flagDataset.getFlagMask(_flagName);
        }
    }

    /**
     * An abstract base class for unary (1-operand) terms.
     */
    public static abstract class Unary extends BitmaskTerm {

        protected BitmaskTerm _arg;

        /**
         * Constructs an unary term with its only operand.
         *
         * @param arg the operand
         */
        protected Unary(BitmaskTerm arg) {
            _arg = arg;
        }

        public BitmaskTerm getArg() {
            return _arg;
        }

        /**
         * Evaluates this unary term.
         *
         * @param context     the context in which to forEachPixel this term
         * @param sampleIndex the current sample index
         *
         * @return a boolean value as a result of the evaluation
         */
        public abstract boolean evaluate(BitmaskTermEvalContext context, int sampleIndex);
    }

    /**
     * The logical NOT term.
     */
    public static class Not extends Unary {

        /**
         * Constructs a logical NOT term with its only operand.
         *
         * @param arg the operand
         */
        public Not(BitmaskTerm arg) {
            super(arg);
        }

        public int hashCode() {
            return ~_arg.hashCode();
        }

        /**
         * Simply returns
         * <pre>
         *    ! arg.forEachPixel(context, sampleIndex)
         * </pre>
         * where <code>arg</code> is the only argument for this NOT term.
         *
         * @param context     the context in which to forEachPixel this term
         * @param sampleIndex the current sample index
         *
         * @return a boolean value as a result of the evaluation
         */
        public boolean evaluate(BitmaskTermEvalContext context, int sampleIndex) {
            return !_arg.evaluate(context, sampleIndex);
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("NOT");
            sb.append(' ');
            if (_arg instanceof FlagReference) {
                sb.append(_arg.toString());
            } else {
                sb.append('(');
                sb.append(_arg.toString());
                sb.append(')');
            }
            return sb.toString();
        }

        public boolean equals(Object o) {
            return o != null
                   && o.getClass().equals(getClass())
                   && _arg.equals(((Unary) o)._arg);
        }

        /**
         * Collects the referenced datasets names into the given list.
         */
        protected void collectReferencedDatasetNames(List nameList) {
            _arg.collectReferencedDatasetNames(nameList);
        }

        /**
         * Collects the referenced flags into the given list.
         */
        protected void collectReferencedFlagNames(List nameList) {
            _arg.collectReferencedFlagNames(nameList);
        }
    }

    /**
     * An abstract base class for binary (2-operands) terms.
     */
    public static abstract class Binary extends BitmaskTerm {

        protected BitmaskTerm _arg1;
        protected BitmaskTerm _arg2;

        /**
         * Constructs a binary term with its two operands.
         *
         * @param arg1 the left-hand operand
         * @param arg2 the right-hand operand
         */
        protected Binary(BitmaskTerm arg1, BitmaskTerm arg2) {
            _arg1 = arg1;
            _arg2 = arg2;
        }


        public BitmaskTerm getArg1() {
            return _arg1;
        }

        public BitmaskTerm getArg2() {
            return _arg2;
        }

        /**
         * Evaluates this binary term.
         *
         * @param context     the context in which to forEachPixel this term
         * @param sampleIndex the current sample index
         *
         * @return a boolean value as a result of the evaluation
         */
        public abstract boolean evaluate(BitmaskTermEvalContext context, int sampleIndex);

        public boolean equals(Object o) {
            return o != null
                   && o.getClass().equals(getClass())
                   && _arg1.equals(((Binary) o)._arg1)
                   && _arg2.equals(((Binary) o)._arg2);
        }

        /**
         * Collects the referenced datasets names into the given list.
         */
        protected void collectReferencedDatasetNames(List nameList) {
            _arg1.collectReferencedDatasetNames(nameList);
            _arg2.collectReferencedDatasetNames(nameList);
        }

        /**
         * Collects the referenced flags into the given list.
         */
        protected void collectReferencedFlagNames(List nameList) {
            _arg1.collectReferencedFlagNames(nameList);
            _arg2.collectReferencedFlagNames(nameList);
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            if (_arg1 instanceof FlagReference) {
                sb.append(_arg1.toString());
            } else {
                sb.append('(');
                sb.append(_arg1.toString());
                sb.append(')');
            }
            sb.append(' ');
            sb.append(getOpName());
            sb.append(' ');
            if (_arg2 instanceof FlagReference) {
                sb.append(_arg2.toString());
            } else {
                sb.append('(');
                sb.append(_arg2.toString());
                sb.append(')');
            }
            return sb.toString();
        }

        protected abstract String getOpName();
    }

    /**
     * The logical OR term.
     */
    public static class Or extends Binary {

        /**
         * Constructs a logical OR term with its two operands.
         *
         * @param arg1 the left-hand operand
         * @param arg2 the right-hand operand
         */
        public Or(BitmaskTerm arg1, BitmaskTerm arg2) {
            super(arg1, arg2);
        }

        public int hashCode() {
            return _arg1.hashCode() + _arg2.hashCode();
        }

        /**
         * Simply returns
         * <pre>
         *    arg1.forEachPixel(context, sampleIndex) || arg2.forEachPixel(context, sampleIndex)
         * </pre>
         * where <code>arg1</code> is the left-hand operand and <code>arg2</code> the right-hand operand of this logical
         * OR term.
         *
         * @param context     the context in which to forEachPixel this term
         * @param sampleIndex the current sample index
         *
         * @return a boolean value as a result of the evaluation
         */
        public boolean evaluate(BitmaskTermEvalContext context, int sampleIndex) {
            return _arg1.evaluate(context, sampleIndex) || _arg2.evaluate(context, sampleIndex);
        }

        protected String getOpName() {
            return "OR";
        }
    }

    /**
     * The logical AND term.
     */
    public static class And extends Binary {

        /**
         * Constructs a logical AND term with its two operands.
         *
         * @param arg1 the left-hand operand
         * @param arg2 the right-hand operand
         */
        public And(BitmaskTerm arg1, BitmaskTerm arg2) {
            super(arg1, arg2);
        }

        public int hashCode() {
            return _arg1.hashCode() * _arg2.hashCode();
        }

        /**
         * Simply returns
         * <pre>
         *    arg1.forEachPixel(context, sampleIndex) && arg2.forEachPixel(context, sampleIndex)
         * </pre>
         * where <code>arg1</code> is the left-hand operand and <code>arg2</code> the right-hand operand of this logical
         * OR term.
         *
         * @param context     the context in which to forEachPixel this term
         * @param sampleIndex the current sample index
         *
         * @return a boolean value as a result of the evaluation
         */
        public boolean evaluate(BitmaskTermEvalContext context, int sampleIndex) {
            boolean w = _arg1.evaluate(context, sampleIndex);
            boolean toa = _arg2.evaluate(context, sampleIndex);
            return w && toa;
        }

        protected String getOpName() {
            return "AND";
        }
    }
}
