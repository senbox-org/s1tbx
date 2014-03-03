package org.esa.pfa.ordering;

/**
* @author Norman Fomferra
*/
public class ProductOrder {
    public enum State {
        WAITING,
        REQUEST_SUBMITTED,
        DOWNLOADING,
        DOWNLOADED,
    }

    final String productName;
    State state;
    int progress;

    public ProductOrder(String productName) {
        this(productName, State.WAITING, 0);
    }

    public ProductOrder(String productName, State state, int progress) {
        this.productName = productName;
        this.state = state;
        this.progress = progress;
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
