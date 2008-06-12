/*
 * $Id: ScrollableView.java,v 1.1.1.1 2006/09/11 08:16:43 norman Exp $
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
package com.bc.view;

/**
 * Represents a scrollable view for usage with a {@link com.bc.swing.ViewPane}.
 * <p>
 * This interface shall be implemented by a {@link javax.swing.JComponent} in order to allow scrolling and zooming.
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
public interface ScrollableView  {
    /**
     * Gets the view model.
     * @return the view model, never null
     */
    ViewModel getViewModel();

    /**
     * Sets the view model.
     * @param  viewModel the view model, never null
     */
    void setViewModel(ViewModel viewModel);
}
