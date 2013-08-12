package org.esa.beam.dataio;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.awt.Color;
import java.io.IOException;

/**
 * @author Marco Peters
 */
public class ColorSerializer extends JsonSerializer<Color> {

    @Override
    public void serialize(Color value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        final StringBuilder sb = new StringBuilder();
        sb.append(value.getRed());
        sb.append(",");
        sb.append(value.getGreen());
        sb.append(",");
        sb.append(value.getBlue());
        if(value.getAlpha() != 255) {
            sb.append(",");
            sb.append(value.getAlpha());

        }
        jgen.writeString(sb.toString());
    }
}
