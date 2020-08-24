package org.esa.s1tbx.stac.support;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.simple.JSONObject;

public class JSONSupport {

    public static String prettyPrint(final JSONObject json) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            return gson.toJson(json);
        } catch (Exception e) {
            System.out.println("Unable to pretty print " + json.toJSONString());
            return json.toJSONString();
        }
    }
}
