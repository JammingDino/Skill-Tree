package com.jd_skill_tree.skills;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.minecraft.util.Identifier;

import java.io.IOException;

/**
 * A custom GSON TypeAdapter for Minecraft's Identifier class.
 * This teaches GSON how to read a simple string from JSON and turn it into an Identifier,
 * and how to write an Identifier back out as a string.
 */
public class IdentifierAdapter extends TypeAdapter<Identifier> {

    @Override
    public void write(JsonWriter out, Identifier value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(value.toString());
        }
    }

    @Override
    public Identifier read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        return new Identifier(in.nextString());
    }
}