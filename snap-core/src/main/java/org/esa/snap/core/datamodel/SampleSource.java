package org.esa.snap.core.datamodel;

/**
 * Represents a general source of sample values.
 *
 * @author Ralf Quast
 */
interface SampleSource {

    int getSample(int x, int y);

    double getSampleDouble(int x, int y);
}
