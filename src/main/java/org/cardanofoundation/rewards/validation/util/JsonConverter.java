package org.cardanofoundation.rewards.validation.util;

import java.io.*;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonConverter {

    public static <T> T readJsonFile(String filePath, Class<T> targetClass) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(new File(filePath), targetClass);
    }

    public static <T> void writeObjectToJsonFile(T objectToWrite, String filePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        File outputFile = new File(filePath);
        objectMapper.writeValue(outputFile, objectToWrite);
    }


    public static <T> ArrayList<T> convertFileJsonToArrayList(String filePath, Class<T> objectClass) {
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(filePath));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(
                    br, objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, objectClass));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}