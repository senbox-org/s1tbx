/*
 * $Id: CeosTestHelper.java,v 1.1 2006/09/14 09:15:56 marcop Exp $
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
package org.esa.beam.dataio.ceos;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class CeosTestHelper {

    public static void writeBlanks(final ImageOutputStream ios, final int numBlanks) throws IOException {
        final char[] chars = new char[numBlanks];
        Arrays.fill(chars, ' ');
        ios.writeBytes(new String(chars));
    }
}
