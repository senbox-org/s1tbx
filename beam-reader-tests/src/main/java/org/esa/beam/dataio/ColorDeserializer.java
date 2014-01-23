package org.esa.beam.dataio;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.awt.*;
import java.io.IOException;

/**
 * @author Marco Peters
 */
class ColorDeserializer extends JsonDeserializer<Color> {

    @Override
    public Color deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        final ObjectCodec codec = jp.getCodec();
        final JsonNode node = codec.readTree(jp);
        final String colorText = node.asText();
        final String[] split = colorText.split(",");

        if (split.length == 4) {
            return new Color(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]), Integer.parseInt(split[3]));
        } else {
            return new Color(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
        }
    }
}
