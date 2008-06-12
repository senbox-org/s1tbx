/*
 * $Id: EvalEnv.java,v 1.1.1.1 2006/09/11 08:16:43 norman Exp $
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
 * Represents an application dependant evaluation environment.
 * This interface has no operation. It is up to application
 * how it is to be interpreted.
 *
 * <p>An object of this type is passed to the <code>eval</code>X methods
 * of the <code>{@link com.bc.jexp.Term}</code> class. Special implementations
 * of the <code>{@link com.bc.jexp.Symbol}</code> and <code>{@link com.bc.jexp.Function}</code>
 * interfaces can cast the object to the application specific type in order to perform
 * an application specific evaluation of the <code>Symbol</code> or <code>Function</code>.
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
public interface EvalEnv {

}
