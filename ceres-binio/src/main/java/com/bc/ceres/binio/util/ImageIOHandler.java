package com.bc.ceres.binio.util;

import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.IOHandler;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;

public class ImageIOHandler implements IOHandler {
    private final ImageInputStream imageInputStream;
    private final ImageOutputStream imageOutputStream;

    public ImageIOHandler(ImageInputStream imageInputStream) {
        this.imageInputStream = imageInputStream;
        this.imageOutputStream = null;
    }

    public ImageIOHandler(ImageOutputStream imageOutputStream) {
        this.imageInputStream = imageOutputStream;
        this.imageOutputStream = imageOutputStream;
    }

    @Override
    public void read(DataContext context, byte[] data, long position) throws IOException {
        synchronized (imageInputStream) {
            imageInputStream.seek(position);
            imageInputStream.readFully(data, 0, data.length);
        }
    }

    @Override
    public void write(DataContext context, byte[] data, long position) throws IOException {
        if (imageOutputStream == null) {
            throw new IOException("Read only.");
        }
        synchronized (imageOutputStream) {
            imageOutputStream.seek(position);
            imageOutputStream.write(data);
        }
    }
    
    @Override
    public long getMaxPosition() throws IOException {
        synchronized (imageOutputStream) {
            return imageInputStream.length();
        }
    }
}
