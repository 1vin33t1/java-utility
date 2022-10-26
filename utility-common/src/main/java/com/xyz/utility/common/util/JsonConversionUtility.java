package com.xyz.utility.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class JsonConversionUtility {

    private static final ObjectMapper objectMapper;
    private static final Gson gson = new GsonBuilder().create();
    private static final String JSON_PARSING_ERROR = "json parsing error";

    static {
        objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static <T> String convertToJsonSilently(T object) {
        try {
            return convertToJson(object);
        } catch (JsonProcessingException ex) {
            log.error("Error while converting object: {} to json string due to:", object);
            return null;
        }
    }

    public static <T> T convertFromJsonSilently(String jsonString, Class<T> clazz) {
        try {
            return convertFromJson(jsonString, clazz);
        } catch (JsonProcessingException ex) {
            log.error("Error while converting json: {} to object due to:", jsonString);
            return null;
        }
    }

    public static <T> T convertFromJsonSilently(String jsonString, TypeReference<T> typeReference) {
        try {
            return convertFromJson(jsonString, typeReference);
        } catch (JsonProcessingException ex) {
            log.error("Error while converting json: {} to object due to:", jsonString);
            return null;
        }
    }

    public static <T> String convertToJson(T object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

    public static <T> T convertFromJson(String jsonString, Class<T> clazz) throws JsonProcessingException {
        return objectMapper.readValue(jsonString, clazz);
    }

    public static <T> T convertFromJson(String jsonString, TypeReference<T> typeReference) throws JsonProcessingException {
        return objectMapper.readValue(jsonString, typeReference);
    }

    public static <S, R> R convertObject(S source, Class<R> classOfR) {
        return objectMapper.convertValue(source, classOfR);
    }

    public static <S, R> R convertObject(S source, TypeReference<R> typeReference) {
        return objectMapper.convertValue(source, typeReference);
    }

    /**
     * @param source        data to be converted
     * @param typeReference type to be converted to
     * @param <S>
     * @param <R>
     * @return object if converted null otherwise
     */
    public static <S, R> R convertObjectSilently(S source, TypeReference<R> typeReference) {
        try {
            return objectMapper.convertValue(source, typeReference);
        } catch (IllegalArgumentException ex) {
            log.info(JSON_PARSING_ERROR, ex);
            return null;
        }
    }

    public static String jsonFromJavaReflection(Object src) {
        return gson.toJson(src);
    }

    public static String jsonFromJavaReflectionSilently(Object src) {
        try {
            return jsonFromJavaReflection(src);
        } catch (Exception ex) {
            log.info(JSON_PARSING_ERROR, ex);
            return null;
        }
    }

    public static <T> T classFromJsonJavaReflection(String src, Class<T> classOfT) {
        return gson.fromJson(src, classOfT);
    }

    public static <T> T classFromJsonJavaReflectionSilently(String src, Class<T> classOfT) {
        try {
            return classFromJsonJavaReflection(src, classOfT);
        } catch (Exception ex) {
            log.info(JSON_PARSING_ERROR, ex);
            return null;
        }
    }

}
