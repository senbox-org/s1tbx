package org.esa.nest.base.util.sgd;

final class SGDInfo {

  private double averageLL;
  private double averageCorrect;
  private double step;
  private int[] bumps = {1, 2, 5};

  double getAverageLL() {
    return averageLL;
  }

  void setAverageLL(double averageLL) {
    this.averageLL = averageLL;
  }

  double getAverageCorrect() {
    return averageCorrect;
  }

  void setAverageCorrect(double averageCorrect) {
    this.averageCorrect = averageCorrect;
  }

  double getStep() {
    return step;
  }

  void setStep(double step) {
    this.step = step;
  }

  int[] getBumps() {
    return bumps;
  }

  void setBumps(int[] bumps) {
    this.bumps = bumps;
  }

}
