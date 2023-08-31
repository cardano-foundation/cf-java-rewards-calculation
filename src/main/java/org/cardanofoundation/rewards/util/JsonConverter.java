package org.cardanofoundation.rewards.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonConverter {
  public static <T> ArrayList<T> convertJsonToArrayList(BufferedReader br, Class<T> clazz) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      return objectMapper.readValue(
          br, objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, clazz));
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static <T> ArrayList<T> convertFileJsonToArrayList(String filePath, Class<T> clazz) {
    BufferedReader br;
    try {
      br = new BufferedReader(new FileReader(filePath));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      return objectMapper.readValue(
          br, objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, clazz));
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
