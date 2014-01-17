
package org.esa.nest.utils.clustering;

import java.io.Closeable;
import java.io.IOException;

import org.apache.mahout.clustering.iterator.ClusterWritable;

/**
 * Writes out clusters
 */
public interface ClusterWriter extends Closeable {

  /**
   * Write all values in the Iterable to the output
   *
   * @param iterable The {@link Iterable} to loop over
   * @return the number of docs written
   * @throws java.io.IOException if there was a problem writing
   */
  long write(Iterable<ClusterWritable> iterable) throws IOException;

  /**
   * Write out a Cluster
   */
  void write(ClusterWritable clusterWritable) throws IOException;

  /**
   * Write the first {@code maxDocs} to the output.
   *
   * @param iterable The {@link Iterable} to loop over
   * @param maxDocs  the maximum number of docs to write
   * @return The number of docs written
   * @throws IOException if there was a problem writing
   */
  long write(Iterable<ClusterWritable> iterable, long maxDocs) throws IOException;
}
