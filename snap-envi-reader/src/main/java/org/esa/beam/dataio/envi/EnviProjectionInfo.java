package org.esa.beam.dataio.envi;

class EnviProjectionInfo {

    public void setProjectionNumber(int projectionNumber) {
        this.projectionNumber = projectionNumber;
    }

    public int getProjectionNumber() {
        return projectionNumber;
    }

    public double[] getParameter() {
        return parameter;
    }

    public void setParameter(double[] parameter) {
        this.parameter = parameter.clone();
    }

    public String getDatum() {
        return datum;
    }

    public void setDatum(String datum) {
        this.datum = datum;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    private int projectionNumber;
    private double[] parameter;
    private String datum;
    private String name;
}
