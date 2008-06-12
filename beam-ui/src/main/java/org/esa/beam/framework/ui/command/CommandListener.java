/*
 * $Id: CommandListener.java,v 1.1 2006/10/10 14:47:36 norman Exp $
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
package org.esa.beam.framework.ui.command;

// @todo 1 nf/nf - place class API docu here

/**
 * The <code>CommandListener</code> is a ...
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 */
public interface CommandListener extends CommandStateListener {

    /**
     * Invoked when a command action is performed.
     *
     * @param event the command event
     */
    void actionPerformed(CommandEvent event);
}
