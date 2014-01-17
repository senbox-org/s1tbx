

package org.esa.nest.utils.clustering;

import org.apache.hadoop.io.Text;
import org.apache.mahout.clustering.AbstractCluster;
import org.apache.mahout.clustering.Cluster;
import org.apache.mahout.clustering.classify.WeightedPropertyVectorWritable;
import org.apache.mahout.clustering.classify.WeightedVectorWritable;
import org.apache.mahout.clustering.iterator.ClusterWritable;
import org.apache.mahout.common.distance.DistanceMeasure;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Implements a {@link ClusterWriter} that outputs in the format used by ClusterDumper in Mahout 0.5
 */
public class ClusterDumperWriter extends AbstractClusterWriter {
  
  private final int subString;
  private final String[] dictionary;
  private final int numTopFeatures;
  
  public ClusterDumperWriter(Writer writer, Map<Integer,List<WeightedVectorWritable>> clusterIdToPoints,
      DistanceMeasure measure, int numTopFeatures, String[] dictionary, int subString) {
    super(writer, clusterIdToPoints, measure);
    this.numTopFeatures = numTopFeatures;
    this.dictionary = dictionary;
    this.subString = subString;
  }
  
  @Override
  public void write(ClusterWritable clusterWritable) throws IOException {
    Cluster cluster = clusterWritable.getValue();
    String fmtStr = cluster.asFormatString(dictionary);
    Writer writer = getWriter();
    if (subString > 0 && fmtStr.length() > subString) {
      writer.write(':');
      writer.write(fmtStr, 0, Math.min(subString, fmtStr.length()));
    } else {
      writer.write(fmtStr);
    }
    
    writer.write('\n');
    
    if (dictionary != null) {
      String topTerms = getTopFeatures(clusterWritable.getValue().getCenter(), dictionary, numTopFeatures);
      writer.write("\tTop Terms: ");
      writer.write(topTerms);
      writer.write('\n');
    }
    
    Map<Integer,List<WeightedVectorWritable>> clusterIdToPoints = getClusterIdToPoints();
    List<WeightedVectorWritable> points = clusterIdToPoints.get(clusterWritable.getValue().getId());
    if (points != null) {
      writer.write("\tWeight : [props - optional]:  Point:\n\t");
      for (Iterator<WeightedVectorWritable> iterator = points.iterator(); iterator.hasNext();) {
        WeightedVectorWritable point = iterator.next();
        writer.write(String.valueOf(point.getWeight()));
        if (point instanceof WeightedPropertyVectorWritable) {
          WeightedPropertyVectorWritable tmp = (WeightedPropertyVectorWritable) point;
          Map<Text,Text> map = tmp.getProperties();
          // map can be null since empty maps when written are returned as null
          writer.write(" : [");
          if (map != null) {
            for (Map.Entry<Text,Text> entry : map.entrySet()) {
              writer.write(entry.getKey().toString());
              writer.write("=");
              writer.write(entry.getValue().toString());
            }
          }
          writer.write("]");
        }
        
        writer.write(": ");
        
        writer.write(AbstractCluster.formatVector(point.getVector(), dictionary));
        if (iterator.hasNext()) {
          writer.write("\n\t");
        }
      }
      writer.write('\n');
    }
  }
}
