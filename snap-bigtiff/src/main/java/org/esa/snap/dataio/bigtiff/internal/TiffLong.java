package org.esa.snap.dataio.bigtiff.internal;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;

public class TiffLong extends TiffValue {

    private final long longValue;

    public TiffLong(long longValue) {
        this.longValue = longValue;
    }

    public long getValue() {
        return longValue;
    }

    @Override
    public int getSizeInBytes() {
        return Long.BYTES;
    }

    @Override
    public void write(ImageOutputStream ios) throws IOException {
        ios.writeLongs(new long[]{longValue}, 0, 1);
    }
}
