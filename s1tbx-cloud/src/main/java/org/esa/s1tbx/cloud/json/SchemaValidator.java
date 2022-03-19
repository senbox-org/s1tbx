/*
 * Copyright (C) 2021 SkyWatch Space Applications Inc. https://www.skywatch.com
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
package org.esa.s1tbx.cloud.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.json.simple.JSONObject;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Set;

public class SchemaValidator {

    private static final JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);

    public SchemaValidator() {
    }

    public void validate(final String schemaPath, final JSONObject json) throws Exception {
        validate(schemaPath, json, false);
    }

    public void validate(final String schemaPath, final JSONObject json, final boolean silent) throws Exception {
        final JsonSchema schema = getJsonSchemaFromClasspath(schemaPath);
        final JsonNode node = getJsonNodeFromStringContent(json.toJSONString());
        final Set<ValidationMessage> errors = schema.validate(node);
        String allErrors = "";

        if(!errors.isEmpty()) {
            System.out.println();
            System.out.println(schemaPath);
            System.out.println(json.toJSONString());

            for (ValidationMessage msg : errors) {
                System.out.println(msg);
                allErrors += msg +"\n";
            }
        }

        if(!silent && !allErrors.isEmpty()) {
            throw new Exception(errors.size() + " schema validation errors found \n" + allErrors);
        }
    }

    protected JsonNode getJsonNodeFromClasspath(String name) throws Exception {
        InputStream is1 = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);

        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(is1);
    }

    protected JsonNode getJsonNodeFromStringContent(String content) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(content);
    }

    protected JsonNode getJsonNodeFromUrl(String url) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(new URL(url));
    }

    protected JsonSchema getJsonSchemaFromClasspath(String name) {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
        return schemaFactory.getSchema(is);
    }

    protected JsonSchema getJsonSchemaFromStringContent(String schemaContent) {
        return schemaFactory.getSchema(schemaContent);
    }

    protected JsonSchema getJsonSchemaFromUrl(String url) throws Exception {
        return schemaFactory.getSchema(new URI(url));
    }

    protected JsonSchema getJsonSchemaFromJsonNode(JsonNode jsonNode) {
        return schemaFactory.getSchema(jsonNode);
    }
}
