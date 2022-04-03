package com.mego.fizoalarm.pojo;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mego.fizoalarm.pojo.challenges.Challenge;

import java.lang.reflect.Type;

public class ChallengeConfigJsonAdapter implements JsonSerializer<Challenge>, JsonDeserializer<Challenge> {

    @Override
    public JsonElement serialize(Challenge src, Type typeOfSrc, JsonSerializationContext context) {

        try {
            return context.serialize( src , Class.forName(src.getClassFullyQualifiedName()));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("UNKNOWN class to serialize");
        }

    }

    @Override
    public Challenge deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

        String classToBuild = json.getAsJsonObject().get("classFullyQualifiedName").getAsString();

        try {
            return context.deserialize(json, Class.forName(classToBuild) );
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("UNKNOWN class to deserialize");
        }


    }

}