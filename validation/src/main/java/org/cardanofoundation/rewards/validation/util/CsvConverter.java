package org.cardanofoundation.rewards.validation.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class CsvConverter {

    public static void writeObjectToCsvFile(List<HashMap<String, String>> data, String filePath) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));

        if (data.size() == 0) {
            return;
        }

        List<String> keys = data.get(0).keySet().stream().toList();

        for (String key : keys) {
            writer.write(key + ",");
        }

        writer.write("\n");

        for (HashMap<String, String> row : data) {
            for (String key : keys) {
                if (row.get(key) == null) {
                    writer.write(",");
                    continue;
                }
                writer.write(row.get(key) + ",");
            }
            writer.write("\n");
        }

        writer.close();
    }
}
