/*
 * Copyright (C) 2014-2015 CS-SI (foss-contact@thor.si.c-s.fr)
 * Copyright (C) 2014-2015 CS-Romania (office@c-s.ro)
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

package org.esa.snap.dataio;

import com.sun.media.imageio.stream.FileChannelImageInputStream;
import com.sun.media.imageioimpl.stream.ChannelImageInputStreamSpi;

import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

/**
 * Wrapper over <code>ChannelImageInputStreamSpi</code>.
 * This is used to create a mapped file I/O (using NIO) instead of the "classic" FileImageInputStream.
 *
 * @author Cosmin Cara
 */
public class FileImageInputStreamSpi extends ChannelImageInputStreamSpi {

    @Override
    public ImageInputStream createInputStreamInstance(Object input, boolean useCache, File cacheDir) throws IOException {
        if (!File.class.isInstance(input))
            throw new IllegalArgumentException("This SPI accepts only java.io.File");
        ImageInputStream stream = null;
        File inputFile = (File) input;
        // We need to make sure the underlying channel is closed, because it may hold a reference to our file and
        // the respective Java classes do not close it.
        //return super.createInputStreamInstance(new RandomAccessFile(inputFile.getAbsolutePath(), "r").getChannel(), useCache, cacheDir);
//        FileChannel channel = new RandomAccessFile(inputFile.getAbsolutePath(), "r").getChannel();
        FileChannel channel = FileChannel.open(inputFile.toPath(), StandardOpenOption.READ);
        if (useCache) {
            stream = new FileCacheImageInputStream(Channels.newInputStream(channel), cacheDir) {
                @Override
                public void close() throws IOException {
                    channel.close();
                    super.close();
                }
            };
        } else {
            stream = new FileChannelImageInputStream(channel) {
                @Override
                public void close() throws IOException {
                    channel.close();
                    super.close();
                }
            };
        }

        return stream;
    }

    @Override
    public Class<?> getInputClass() {
        return File.class;
    }
}
