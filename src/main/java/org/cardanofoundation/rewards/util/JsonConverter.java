package org.cardanofoundation.rewards.util;

import java.io.*;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonConverter {
    public static <T> ArrayList<T> convertJsonToArrayList(BufferedReader br, Class<T> objectClass) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(
                    br, objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, objectClass));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <T> T readJsonFile(String filePath, Class<T> targetClass) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(new File(filePath), targetClass);
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