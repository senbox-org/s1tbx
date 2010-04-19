package org.esa.beam.framework.gpf.ws.support;

import org.esa.beam.framework.gpf.ws.OpRunner;

import javax.xml.ws.Endpoint;

public class OpServer {
    public static void main(String args[]) {
        OpRunner opRunner = new DefaultOpRunner();
        Endpoint.publish("http://localhost:8080/OpRunner", opRunner);
    }
}
