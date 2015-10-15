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
package org.esa.snap.core.util;

import java.beans.PropertyChangeEvent;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Logger;

/**
 * The <code>Debug</code> as it name says is a utility class for debugging. It contains exculisvely static methods and
 * cannot be instantiated.
 * <p>This class provides methods for program assertions as well as tracing capabilities. The tracing output can be
 * redirected to any <code>java.io.XMLCoder</code>. The debbuging capabilities of this class can be disabled at runtime
 * by calling <code>Debug.setEnabled(false)</code>.
 * <p> The methods defined in this class are guaranteed not to throw any exceptions caused by illegal argument passed to
 * them.
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @version $Revision$ $Date$
 * @see AssertionFailure
 */
public class Debug {

    /**
     * Set the DEBUG constant to false, if you want your Java compiler to completely remove method bodies caused by dead
     * code blocks.
     */
    private static final boolean DEBUG = true;
    //private static final boolean DEBUG = false;

    /**
     * If false, debugging will be disabled.
     */
    private static boolean _enabled = false;

    /**
     * The current writer.
     */
    private static PrintWriter _writer = null;

    /**
     * The default writer. Used when current writer not set.
     */
    private static PrintWriter _defaultWriter = null;

    private static final String _tracePrefix = "snap-debug: ";

    private static Logger _logger = null;

    private static boolean _logging;

    public static boolean isLogging() {
        return _logging;
    }

    public static void setLogging(boolean logging) {
        _logging = logging;
    }

    /**
     * Enables the debugging functionality or disables it.
     *
     * @param enabled if true, debugging functionality will be enabled
     */
    public static boolean setEnabled(boolean enabled) {
        boolean oldValue = _enabled;
        _enabled = enabled;
        return oldValue;
    }

    /**
     * Checks whether the debugging functionality is enabled or not.
     *
     * @return true, if so
     */
    public static boolean isEnabled() {
        return DEBUG && _enabled;
    }

    /**
     * Gets the default writer used for debugging output.
     *
     * @return the default writer, or <code>null</code> if debugging is disabled
     */
    public static PrintWriter getDefaultWriter() {
        if (isEnabled() && _defaultWriter == null) {
            _defaultWriter = createPrintWriter(System.out);
        }
        return _defaultWriter;
    }

    /**
     * Gets the current writer that will used for debugging output. If no writer was explicitely set by the user, the
     * default writer is returned.
     *
     * @return the current writer, or <code>null</code> if debugging is disabled
     */
    public static PrintWriter getWriter() {
        if (isEnabled() && _writer == null) {
            _writer = getDefaultWriter();
        }
        return _writer;
    }

    /**
     * Sets the current writer that will be used for debugging output.
     *
     * @param writer the new writer
     */
    public static void setWriter(Writer writer) {
        if (isEnabled() && writer != null) {
            _writer = createPrintWriter(writer);
        }
    }

    /**
     * Sets the current writer for the given stream that will be used for debugging output.
     *
     * @param stream the stream that will be used for debugging output
     */
    public static void setWriter(OutputStream stream) {
        if (isEnabled() && stream != null) {
            _writer = createPrintWriter(stream);
        }
    }

    /**
     * Prints the given message string to the current writer if and only if the debugging class functionality is
     * enabled.
     */
    public static void trace(String message) {
        if (isEnabled() && message != null) {
            PrintWriter w = getWriter();
            w.print(_tracePrefix);
            w.println(message);
            log(message);
        }
    }

    /**
     * Prints the stack trace for the given exeption to the current writer if and only if the debugging class
     * functionality is enabled.
     */
    public static void trace(Throwable exception) {
        if (isEnabled() && exception != null) {
            PrintWriter w = getWriter();
            w.print(_tracePrefix);
            w.println(UtilConstants.MSG_EXCEPTION_OCCURRED);
            exception.printStackTrace(w);
            log(UtilConstants.MSG_EXCEPTION_OCCURRED);
            log(exception);
        }
    }

    /**
     * Prints the given property change event to the current writer if and only if the debugging class functionality is
     * enabled.
     */
    public static void trace(PropertyChangeEvent event) {
        if (isEnabled() && event != null) {
            PrintWriter w = getWriter();
            w.print("property ");
            w.print(event.getPropertyName());
            w.print(" changed from ");
            w.print(event.getOldValue());
            w.print(" to ");
            w.print(event.getNewValue());
            w.println();
        }
    }

    /**
     * Calls <code>traceMemoryUsage(null)</code>.
     *
     * @see #traceMemoryUsage(String)
     */
    public static void traceMemoryUsage() {
        traceMemoryUsage(null);
    }

    /**
     * Prints the ammount of free memory using the given label string to the current writer if and only if the debugging
     * class functionality is enabled.
     *
     * @param label an optional label, can be null
     */
    public static void traceMemoryUsage(String label) {
        if (isEnabled()) {
            String message = createMemoryUsageMessage(label);
            PrintWriter w = getWriter();
            w.print(_tracePrefix);
            w.println(message);
            log(message);
        }
    }

    private static String createMemoryUsageMessage(String label) {
        final StringBuffer sb = new StringBuffer(128);
        long freeMem = Runtime.getRuntime().freeMemory();
        long totalMem = Runtime.getRuntime().totalMemory();
        long usedMem = totalMem - freeMem;
        float accuracy = 0.1F;
        float mbFactor = (1.0F / accuracy) * (1.0F / 1024.0F / 1024.0F);
        sb.append("total memory: ");
        sb.append(accuracy * Math.round(mbFactor * totalMem));
        sb.append(" MB, in use: ");
        sb.append(accuracy * Math.round(mbFactor * usedMem));
        sb.append(" MB");
        if (label != null) {
            sb.append(": label <");
            sb.append(label);
            sb.append(">");
        }
        return sb.toString();
    }

    /**
     * Prints a 'method not implemented' message using the given class and method name string to the current writer if
     * and only if the debugging class functionality is enabled.
     *
     * @param clazz      the method's class
     * @param methodName the method's name
     */
    public static void traceMethodNotImplemented(Class clazz, String methodName) {
        if (isEnabled()) {
            PrintWriter w = getWriter();
            w.print(_tracePrefix);
            w.print(UtilConstants.MSG_METHOD_NOT_IMPLEMENTED);
            if (methodName != null) {
                if (clazz != null) {
                    w.print(clazz.getName());
                } else {
                    w.print("<unknown class>");
                }
                w.print(".");
                w.print(methodName);
                w.print("()");
                w.println();
            }
        }
    }

    /**
     * If the <code>condition</code> is NOT true, and the debugging functionality is enabled, this method throws an
     * <code>AssertionFailure</code> in order to signal that an internal program post- or pre-condition failed.
     * <p> Use this method whenever a valid state must be ensured within your sourcecode in order to safely continue the
     * program.
     * <p> For example, the method can be used to ensure valid arguments passed to private and protected methods.
     *
     * @param condition the assert condition
     *
     * @throws AssertionFailure if <code>condition</code> evaluates to false and the debugging functionality is enabled
     */
    public static void assertTrue(boolean condition) throws AssertionFailure {
        if (isEnabled()) {
            if (!condition) {
                handleAssertionFailed(null);
            }
        }
    }

    /**
     * If the <code>condition</code> is NOT true, and the debugging functionality is enabled, this method throws an
     * <code>AssertionFailure</code> in order to signal that an internal program post- or pre-condition failed. The
     * <code>AssertionFailure</code> will be created with the given error message string.
     * <p> Use this method whenever a valid state must be ensured within your sourcecode in order to safely continue the
     * program.
     * <p> For example, the method can be used to ensure valid arguments passed to private and protected methods.
     *
     * @param condition the assert condition
     * @param message   an error message
     *
     * @throws AssertionFailure if <code>condition</code> evaluates to false and the debugging functionality is enabled
     */
    public static void assertTrue(boolean condition, String message) throws AssertionFailure {
        if (isEnabled()) {
            if (!condition) {
                handleAssertionFailed(message);
            }
        }
    }


    /**
     * If the given object is null, and the debugging functionality is enabled, this method throws an
     * <code>AssertionFailure</code> in order to signal that an internal program post- or pre-condition failed.
     * <p> Use this method whenever a valid state must be ensured within your sourcecode in order to safely continue the
     * program.
     * <p> For example, the method can be used to ensure valid arguments passed to private and protected methods.
     *
     * @param object the object to test for non-null condition
     *
     * @throws AssertionFailure if <code>object</code> is null and the debugging functionality is enabled
     */
    public static void assertNotNull(Object object) throws AssertionFailure {
        if (isEnabled()) {
            if (object == null) {
                handleAssertionFailed(UtilConstants.MSG_OBJECT_NULL);
            }
        }
    }

    /**
     * If the given String is null or empty, and the debugging functionality is enabled, this method throws an
     * <code>AssertionFailure</code> in order to signal that an internal program post- or pre-condition failed.
     * <p> Use this method whenever a valid state must be ensured within your sourcecode in order to safely continue the
     * program.
     * <p> For example, the method can be used to ensure valid arguments passed to private and protected methods.
     *
     * @param string the String to test for non-null and not empty condition
     *
     * @throws AssertionFailure if <code>String</code> is null or empty and the debugging functionality is enabled
     */
    public static void assertNotNullOrEmpty(String string) throws AssertionFailure {
        if (isEnabled()) {
            if (string == null || string.length() < 1) {
                handleAssertionFailed(UtilConstants.MSG_STRING_NULL_OR_EMPTY);
            }
        }
    }

    /**
     * Delegates <code>Debug.trace()</code> calls to the system logger.
     *
     * @param message the message to be logged.
     */
    protected static void log(String message) {
        if (isEnabled()) {
            if (isLogging()) {
                if (_logger == null) {
                    _logger = SystemUtils.LOG;
                }
                _logger.finest(message);
            }
        }
    }

    /**
     * Delegates <code>Debug.trace()</code> calls to the system logger.
     *
     * @param exception the exception to be logged.
     */
    protected static void log(Throwable exception) {
        if (isEnabled()) {
            if (isLogging()) {
                final StringWriter stringWriter = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(stringWriter);
                exception.printStackTrace(printWriter);
                log(stringWriter.getBuffer().toString());
            }
        }
    }

    /**
     * Implementation for the assertion-failed handler.
     *
     * @throws AssertionFailure always if debugging is enabled
     */
    private static void handleAssertionFailed(String message) throws AssertionFailure {
        if (isEnabled()) {
            /*
             * @todo 3 nf/nf - Place dialog box here, asking the user whether to continue,
             * debug, or cancel...
             */
            try {
                // this Exception allows me to get the StackTrace Information
                throw new Exception();
            } catch (Exception e) {
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                printWriter.print(_tracePrefix);
                printWriter.print("assertion failed: ");
                if (message != null) {
                    printWriter.print(message);
                    printWriter.print(": ");
                }
                printWriter.println();
                e.printStackTrace(printWriter);
                final String messageToTrace = stringWriter.getBuffer().toString();
                printWriter.close();
                printWriter = null;
                stringWriter = null;

                PrintWriter w = getWriter();
                w.print(messageToTrace);
                log(messageToTrace);
                throw new AssertionFailure(message);
            }
        }
    }

    /**
     * Creates a new trace writer by wrapping the given writer.
     */
    private static PrintWriter createPrintWriter(Writer writer) {
        return new PrintWriter(writer, true);
    }

    /**
     * Creates a new trace writer by wrapping the given output stream.
     */
    private static PrintWriter createPrintWriter(OutputStream stream) {
        return createPrintWriter(new OutputStreamWriter(stream));
    }

    /**
     * Protected constructor. Used to prevent object instatiation from this class.
     */
    protected Debug() {
    }

    public static void trace(String name, double[][] v) {
        int m = v.length;
        StringBuffer sb = new StringBuffer(m * 8 + 8);
        for (int i = 0; i < m; i++) {
            sb.setLength(0);
            sb.append(name);
            sb.append("[");
            sb.append(i);
            sb.append("] = {");
            int n = v[i].length;
            for (int j = 0; j < n; j++) {
                if (j > 0) {
                    sb.append(", ");
                }
                sb.append(v[i][j]);
            }
            sb.append("}");
            trace(sb.toString());
        }
    }

    public static void trace(String name, double[] v) {
        int n = v.length;
        StringBuffer sb = new StringBuffer(n * 8 + 8);
        sb.append(name);
        sb.append(" = {");
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(v[i]);
        }
        sb.append("}");
        trace(sb.toString());
    }
}
