package org.esa.beam.binning;

/**
 * @author Norman Fomferra
 */
public class TemporalDataPeriod implements DataPeriod {
      private static final double EPS = 1.0 / (60.0 * 60.0 * 1000); // 1 ms

      private final double startTime;
      private final int duration;

      /**
       * @param startTime   Binning period's start time in MJD.
       * @param duration    The binning period's duration in days.
       */
      public TemporalDataPeriod(double startTime, int duration) {
          this.startTime = startTime;
          this.duration = duration;
      }

      @Override
      public double getStartTime() {
          return startTime;
      }

      @Override
      public int getDuration() {
          return duration;
      }

      @Override
      public int getObservationMembership(double lon, double time) {

          final double h = 24.0 * (time - startTime);

          if (h - EPS < 0) {
              // pixel is attached to data-period (p-1)
              return -1;
          } else if (h + EPS > 24.0 * duration) {
              // pixel is attached to data-period (p+1)
              return +1;
          } else {
              // pixel is attached to data-period (p)
              return 0;
          }
      }
}
