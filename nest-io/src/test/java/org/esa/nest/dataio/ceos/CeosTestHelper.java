/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.ceos;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision: 1.1 $ $Date: 2011-06-30 15:41:47 $
 */
public class CeosTestHelper {

    public static void writeBlanks(final ImageOutputStream ios, final int numBlanks) throws IOException {
        final char[] chars = new char[numBlanks];
        Arrays.fill(chars, ' ');
        ios.writeBytes(new String(chars));
    }
}
