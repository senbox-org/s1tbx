/*
 * $Id: ParamExceptionHandler.java,v 1.1.1.1 2006/09/11 08:16:46 norman Exp $
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
package org.esa.beam.framework.param;

/**
 * The <code>ParamExceptionHandler</code> are an alternative way to handle parameter exceptions. Multiple methods in the
 * <code>Parameter</code> class accept a <code>ParamExceptionHandler</code> argument for this purpose.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 * @see Parameter
 */
public interface ParamExceptionHandler {

    /**
     * Notifies a client if an exeption occured on a <code>Parameter</code>.
     *
     * @param e the exception
     *
     * @return <code>true</code> if the exception was handled successfully, <code>false</code> otherwise
     */
    boolean handleParamException(ParamException e);
}