package org.esa.nest.base.util.sgd;

import com.google.common.base.Preconditions;
import com.google.common.io.Closeables;

import java.io.BufferedReader;

/**
 * Uses the same logic as TrainLogistic and RunLogistic for finding an input, but instead
 * of processing the input, this class just prints the input to standard out.
 */
public final class PrintResourceOrFile {

  private PrintResourceOrFile() {
  }

  public static void main(String[] args) throws Exception {
    Preconditions.checkArgument(args.length == 1, "Must have a single argument that names a file or resource.");
    BufferedReader in = TrainLogistic.open(args[0]);
    try {
      String line;
      while ((line = in.readLine()) != null) {
        System.out.println(line);
      }
    } finally {
      Closeables.close(in, true);
    }
  }
}
