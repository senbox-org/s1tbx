package org.esa.snap.gpf.ui;

/**
 * Created by IntelliJ IDEA.
 * User: lveci
 * Date: Feb 13, 2008
 * To change this template use File | Settings | File Templates.
 */
public class UIValidation {

    private State state = State.OK;
    private String msg = "";

    public enum State {OK, ERROR, WARNING}

    public UIValidation(State theState, String theMessage) {
        state = theState;
        msg = theMessage;
    }

    public State getState() {
        return state;
    }

    public String getMsg() {
        return msg;
    }

}
