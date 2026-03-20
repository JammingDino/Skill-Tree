package com.jd_skill_tree.skills;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;

/**
 * A custom GSON TypeAdapter for Minecraft's ResourceLocation class.
 * This teaches GSON how to read a simple string from JSON and turn it into an ResourceLocation,
 * and how to write an ResourceLocation back out as a string.
 */
public class IdentifierAdapter extends TypeAdapter<ResourceLocation> {

    @Override
    public void write(JsonWriter out, ResourceLocation value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(value.toString());
        }
    }

    @Override
    public ResourceLocation read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        return new ResourceLocation(in.nextString());
    }
}