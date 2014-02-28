package org.esa.pfa.ordering;

/**
* @author Norman Fomferra
*/
public class ProductOrder {
    public enum State {
        SUBMITTED,
        DOWNLOADING,
        DOWNLOADED,
    }

    final String productName;
    State state;
    int progress;

    public ProductOrder(String productName) {
        this.productName = productName;
        state = State.SUBMITTED;
        progress = 0;
    }

    public String getProductName() {
        return productName;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }
}
