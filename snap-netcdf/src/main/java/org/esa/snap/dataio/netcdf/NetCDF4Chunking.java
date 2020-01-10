package org.esa.snap.dataio.netcdf;

import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.FileWriter2;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.write.Nc4Chunking;

import java.util.List;

public class NetCDF4Chunking implements Nc4Chunking {

    private static final int MIN_VARIABLE_BYTES = (int) Math.pow(2, 16); // 65K
    private static final int DEFAULT_CHUNKSIZE_BYTES = (int) Math.pow(2, 18); // 256K
    private static final int MIN_CHUNKSIZE_BYTES = (int) Math.pow(2, 13); // 8K
    private static final int DEFLAT_LEVEL = 5;
    private static final boolean IS_SHUFFLE = true;

    private int minVariableSize = MIN_VARIABLE_BYTES;
    private int defaultChunkSize = DEFAULT_CHUNKSIZE_BYTES;
    private int minChunksize = MIN_CHUNKSIZE_BYTES;

    @Override
    public boolean isChunked(Variable v) {
        if (v.isUnlimited()) {
            return true;
        }

        long size = v.getSize() * v.getElementSize();
        return size > minVariableSize;
    }


    @Override
    public long[] computeChunking(Variable v) {

        int[] resultFromAtt = computeChunkingFromAttribute(v);
        if (resultFromAtt != null) {
            return convertToLong(resultFromAtt);
        }

        int maxElements = defaultChunkSize / v.getElementSize();

        // no unlimited dimensions
        if (!v.isUnlimited()) {
            int[] result = fillRightmost(v.getShape(), maxElements);
            return convertToLong(result);
        }

        // unlimited case
        int[] result = computeUnlimitedChunking(v.getDimensions(), v.getElementSize());
        return convertToLong(result);
    }

    private int[] computeChunkingFromAttribute(Variable v) {
        Attribute att = getChunkAttribute(v); // use CHUNK_SIZES attribute if it exists
        if (att != null) {
            int[] result = new int[v.getRank()];
            for (int i = 0; i < v.getRank(); i++) {
                result[i] = att.getNumericValue(i).intValue();
            }
            return result;
        }

        return null;
    }

    // make it easy to test by using dimension list
    private int[] computeUnlimitedChunking(List<Dimension> dims, int elemSize) {
        int maxElements = defaultChunkSize / elemSize;
        int[] result = fillRightmost(convertUnlimitedShape(dims), maxElements);
        long resultSize = new Section(result).computeSize();
        if (resultSize < minChunksize) {
            maxElements = minChunksize / elemSize;
            result = incrUnlimitedShape(dims, result, maxElements);
        }

        return result;
    }

    private int[] fillRightmost(int shape[], int maxElements) {
        // fill up rightmost dimensions first, until maxElements is reached
        FileWriter2.ChunkingIndex index = new FileWriter2.ChunkingIndex(shape);
        return index.computeChunkShape(maxElements);
    }

    private long[] convertToLong(int[] shape) {
        if (shape.length == 0) {
            shape = new int[1];
        }
        long[] result = new long[shape.length];
        for (int i = 0; i < shape.length; i++) {
            result[i] = shape[i] > 0 ? shape[i] : 1; // unlimited dim has 0
        }
        return result;
    }

    private int[] convertUnlimitedShape(List<Dimension> dims) {
        int[] result = new int[dims.size()];
        int count = 0;
        for (Dimension d : dims) {
            result[count++] = (d.isUnlimited()) ? 1 : d.getLength();
        }
        return result;
    }

    private Attribute getChunkAttribute(Variable v) {
        Attribute att = v.findAttribute(CDM.CHUNK_SIZES);
        if (att != null && att.getDataType().isIntegral() && att.getLength() == v.getRank()) {
            return att;
        }
        return null;
    }

    private int[] incrUnlimitedShape(List<Dimension> dims, int[] shape, long maxElements) {
        int countUnlimitedDims = 0;
        for (Dimension d : dims) {
            if (d.isUnlimited()) {
                countUnlimitedDims++;
            }
        }
        long shapeSize = new Section(shape).computeSize(); // shape with unlimited dimensions == 1
        int needFactor = (int) (maxElements / shapeSize);

        // distribute needFactor amongst the n unlimited dimensions
        int need;
        if (countUnlimitedDims <= 1) {
            need = needFactor;
        } else if (countUnlimitedDims == 2) {
            need = (int) Math.sqrt(needFactor);
        } else if (countUnlimitedDims == 3) {
            need = (int) Math.cbrt(needFactor);
        } else {
            // nth root?? hmm roundoff !!
            need = (int) Math.pow(needFactor, 1.0 / countUnlimitedDims);
        }

        int[] result = new int[shape.length];
        int count = 0;
        for (Dimension d : dims) {
            result[count] = (d.isUnlimited()) ? need : shape[count];
            count++;
        }
        return result;
    }

    @Override
    public int getDeflateLevel(Variable v) {
        return DEFLAT_LEVEL;
    }

    @Override
    public boolean isShuffle(Variable v) {
        return IS_SHUFFLE;
    }
}
