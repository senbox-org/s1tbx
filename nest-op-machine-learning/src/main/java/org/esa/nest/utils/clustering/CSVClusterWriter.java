
package org.esa.nest.utils.clustering;

import org.apache.mahout.clustering.Cluster;
import org.apache.mahout.clustering.classify.WeightedVectorWritable;
import org.apache.mahout.clustering.iterator.ClusterWritable;
import org.apache.mahout.common.distance.DistanceMeasure;
import org.apache.mahout.math.NamedVector;
import org.apache.mahout.math.Vector;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Format is adjacency style as put forth at http://gephi.org/users/supported-graph-formats/csv-format/, the centroid
 * is the first element and all the rest of the row are the points in that cluster
 *
 **/
public class CSVClusterWriter extends AbstractClusterWriter {

  private static final Pattern VEC_PATTERN = Pattern.compile("\\{|\\:|\\,|\\}");

  public CSVClusterWriter(Writer writer, Map<Integer, List<WeightedVectorWritable>> clusterIdToPoints,
      DistanceMeasure measure) {
    super(writer, clusterIdToPoints, measure);
  }

  @Override
  public void write(ClusterWritable clusterWritable) throws IOException {
    StringBuilder line = new StringBuilder();
    Cluster cluster = clusterWritable.getValue();
    line.append(cluster.getId());
    List<WeightedVectorWritable> points = getClusterIdToPoints().get(cluster.getId());
    if (points != null) {
      for (WeightedVectorWritable point : points) {
        Vector theVec = point.getVector();
        line.append(',');
        if (theVec instanceof NamedVector) {
          line.append(((NamedVector)theVec).getName());
        } else {
          String vecStr = theVec.asFormatString();
          //do some basic manipulations for display
          vecStr = VEC_PATTERN.matcher(vecStr).replaceAll("_");
          line.append(vecStr);
        }
      }
      getWriter().append(line).append("\n");
    }
  }
}
