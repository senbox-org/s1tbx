package org.esa.beam.dataio.chris;

/**
 * Class representing the scan line layout for CHRIS images.
 *
 * @author Ralf Quast
 * @version $Revision: 1.2 $ $Date: 2007/04/03 14:05:42 $
 */
class ScanLineLayout {

    final int leadingPixelCount;
    final int imagePixelCount;
    final int trailingPixelCount;

    public ScanLineLayout(int precedingPixelCount, int imagePixelCount, int trailingPixelCount) {
        this.leadingPixelCount = precedingPixelCount;
        this.imagePixelCount = imagePixelCount;
        this.trailingPixelCount = trailingPixelCount;
    }
}
