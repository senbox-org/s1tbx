/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
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
