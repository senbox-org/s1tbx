package org.esa.beam.dataio.chris;

/**
 * Class representing the scan line layout for CHRIS images.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
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
