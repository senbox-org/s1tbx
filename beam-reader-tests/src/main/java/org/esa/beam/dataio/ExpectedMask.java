package org.esa.beam.dataio;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.esa.beam.framework.datamodel.Mask;

import java.awt.Color;

/**
 * @author Marco Peters
 */
class ExpectedMask {
    @JsonProperty(required = true)
    private String name;
    @JsonProperty()
    private Class<? extends Mask.ImageType> type;
    @JsonProperty()
    @JsonSerialize(using = ColorSerializer.class)
    @JsonDeserialize(using = ColorDeserializer.class)
    private Color color;
    @JsonProperty()
    private String description;


    ExpectedMask() {
    }

    ExpectedMask(Mask mask) {
        this(mask.getName(), mask.getImageType().getClass(), mask.getImageColor(), mask.getDescription());
    }

    ExpectedMask(String name, Class<? extends Mask.ImageType> type, Color color, String description) {
        this();
        this.name = name;
        this.type = type;
        this.color = color;
        this.description = description;
    }

    String getName() {
        return name;
    }

    Class<? extends Mask.ImageType> getType() {
        return type;
    }

    Color getColor() {
        return color;
    }

    String getDescription() {
        return description;
    }
}
