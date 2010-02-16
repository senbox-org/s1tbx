package org.esa.beam.framework.dataop.resamp;


final class CubicConvolutionResampling implements Resampling {

    public String getName() {
        return "CUBIC_CONVOLUTION";
    }

    public final Index createIndex() {
        return new Index(4, 4);
    }

    public final void computeIndex(final float x,
                                   final float y,
                                   final int width,
                                   final int height,
                                   final Index index) {
        index.x = x;
        index.y = y;
        index.width = width;
        index.height = height;

        final int i0 = (int) Math.floor(x);
        final int j0 = (int) Math.floor(y);

        float di = x - (i0 + 0.5f);
        float dj = y - (j0 + 0.5f);

        index.i0 = i0;
        index.j0 = j0;

        final int iMax = width - 1;
        if (di >= 0) {
            index.i[0] = Index.crop(i0 - 1, iMax);
            index.i[1] = Index.crop(i0 - 0, iMax);
            index.i[2] = Index.crop(i0 + 1, iMax);
            index.i[3] = Index.crop(i0 + 2, iMax);
        } else {
            index.i[0] = Index.crop(i0 - 2, iMax);
            index.i[1] = Index.crop(i0 - 1, iMax);
            index.i[2] = Index.crop(i0 + 0, iMax);
            index.i[3] = Index.crop(i0 + 1, iMax);
            di += 1;
        }

        final int jMax = height - 1;
        if (dj >= 0) {
            index.j[0] = Index.crop(j0 - 1, jMax);
            index.j[1] = Index.crop(j0 - 0, jMax);
            index.j[2] = Index.crop(j0 + 1, jMax);
            index.j[3] = Index.crop(j0 + 2, jMax);
        } else {
            index.j[0] = Index.crop(j0 - 2, jMax);
            index.j[1] = Index.crop(j0 - 1, jMax);
            index.j[2] = Index.crop(j0 + 0, jMax);
            index.j[3] = Index.crop(j0 + 1, jMax);
            dj += 1;
        }

        index.ki[0] = f1(1 + di);
        index.ki[1] = f2(di);
        index.ki[2] = f2(1 - di);
        index.ki[3] = f1(2 - di);

        index.kj[0] = f1(1 + dj);
        index.kj[1] = f2(dj);
        index.kj[2] = f2(1 - dj);
        index.kj[3] = f1(2 - dj);

    }

    public final float resample(final Raster raster,
                                final Index index) throws Exception {

        final int i1 = index.i[0];
        final int i2 = index.i[1];
        final int i3 = index.i[2];
        final int i4 = index.i[3];

        final int j1 = index.j[0];
        final int j2 = index.j[1];
        final int j3 = index.j[2];
        final int j4 = index.j[3];

        final float ki1 = index.ki[0];
        final float ki2 = index.ki[1];
        final float ki3 = index.ki[2];
        final float ki4 = index.ki[3];

        final float kj1 = index.kj[0];
        final float kj2 = index.kj[1];
        final float kj3 = index.kj[2];
        final float kj4 = index.kj[3];

        final float z11 = raster.getSample(i1, j1);
        final float z12 = raster.getSample(i2, j1);
        final float z13 = raster.getSample(i3, j1);
        final float z14 = raster.getSample(i4, j1);

        final float z21 = raster.getSample(i1, j2);
        final float z22 = raster.getSample(i2, j2);
        final float z23 = raster.getSample(i3, j2);
        final float z24 = raster.getSample(i4, j2);

        final float z31 = raster.getSample(i1, j3);
        final float z32 = raster.getSample(i2, j3);
        final float z33 = raster.getSample(i3, j3);
        final float z34 = raster.getSample(i4, j3);

        final float z41 = raster.getSample(i1, j4);
        final float z42 = raster.getSample(i2, j4);
        final float z43 = raster.getSample(i3, j4);
        final float z44 = raster.getSample(i4, j4);

        if (Float.isNaN(z11) || Float.isNaN(z12) || Float.isNaN(z13) || Float.isNaN(z14) ||
            Float.isNaN(z21) || Float.isNaN(z22) || Float.isNaN(z23) || Float.isNaN(z24) ||
            Float.isNaN(z31) || Float.isNaN(z32) || Float.isNaN(z33) || Float.isNaN(z34) ||
            Float.isNaN(z41) || Float.isNaN(z42) || Float.isNaN(z43) || Float.isNaN(z44)) {
            return raster.getSample(index.i0, index.j0);
        }

        // interpolate along 4 lines first
        final float z1 = z11 * ki1 + z12 * ki2 + z13 * ki3 + z14 * ki4;
        final float z2 = z21 * ki1 + z22 * ki2 + z23 * ki3 + z24 * ki4;
        final float z3 = z31 * ki1 + z32 * ki2 + z33 * ki3 + z34 * ki4;
        final float z4 = z41 * ki1 + z42 * ki2 + z43 * ki3 + z44 * ki4;

        // then along a single column
        return z1 * kj1 + z2 * kj2 + z3 * kj3 + z4 * kj4;
    }

    private static float f1(final float t) {
        return 4f - 8f * (t) + 5f * (t * t) - (t * t * t);
    }

    private static float f2(final float t) {
        return 1f - 2f * (t * t) + (t * t * t);
    }

    @Override
    public String toString() {
        return "Cubic convolution resampling";
    }
}
