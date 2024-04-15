package org.cardanofoundation.rewards.validation.util;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonConverter {

    public static <T> T readJsonFile(String filePath, Class<T> targetClass) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        FileInputStream fileInputStream = new FileInputStream(filePath);
        GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream);

        return objectMapper.readValue(gzipInputStream, targetClass);
    }

    public static <T> void writeObjectToJsonFile(T objectToWrite, String filePath) throws IOException {
        File outputFile = new File(filePath);

        if(outputFile.isDirectory()){
            if (!outputFile.exists()) {
                boolean output = outputFile.mkdirs();
                if (!output) {
                    throw new IOException("Failed to create directory: " + outputFile.getAbsolutePath());
                }
            }
        } else {
            if (!outputFile.getAbsoluteFile().getParentFile().exists()) {
                boolean output = outputFile.getAbsoluteFile().getParentFile().mkdirs();
                if (!output) {
                    throw new IOException("Failed to create directory: " + outputFile.getAbsoluteFile().getParentFile());
                }
            }
        }

        FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(fileOutputStream);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(gzipOutputStream, objectToWrite);

        gzipOutputStream.close();
        fileOutputStream.close();
    }
}