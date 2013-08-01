package org.esa.beam.dataio;

class ExpectedPixel {
    private int x;
    private int y;
    private float value;

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    float getValue() {
        return value;
    }

    void setValue(float value) {
        this.value = value;
    }
}
