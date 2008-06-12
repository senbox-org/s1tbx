/*
 * $Id: EvalException.java,v 1.1.1.1 2006/09/11 08:16:43 norman Exp $
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

package com.bc.jexp;


/**
 * An exception that can be thrown during the evaluation of a
 * <code>{@link com.bc.jexp.Term}</code>.
 *
 * <p> A method is not required to declare in its <code>throws</code>
 * clause any subclasses of <code>EvalException</code> since it is derived from
 * <code>java.lang.RuntimeException</code>.
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
public class EvalException extends RuntimeException {

    private static final long serialVersionUID = 1321063032856719846L;

    /**
     * Constructs a <code>EvalException</code> with no detail message.
     */
    public EvalException() {
        super();
    }

    /**
     * Constructs a <code>EvalException</code> with the specified
     * detail message.
     *
     * @param message the detail message.
     */
    public EvalException(final String message) {
        super(message);
    }


    public EvalException(String message, Throwable cause) {
        super(message, cause);
    }
}
