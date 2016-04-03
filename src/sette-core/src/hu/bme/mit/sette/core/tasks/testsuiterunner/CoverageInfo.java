package hu.bme.mit.sette.core.tasks.testsuiterunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.apache.commons.lang3.tuple.Triple;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NonNull;

public final class CoverageInfo {
    // FIXME insane data structure
    // key: source file
    // value: tuple of FULLY, PARTLY and NOT covered line numbers sets (green, yellow, red)
    public final Map<String, Triple<SortedSet<Integer>, SortedSet<Integer>, SortedSet<Integer>>> data;

    public CoverageInfo() {
        this.data = new HashMap<>();
    }

    public CoverageInfo(
            @NonNull Map<String, Triple<SortedSet<Integer>, SortedSet<Integer>, SortedSet<Integer>>> data) {
        this.data = data;
    }

    public String toJsonString() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, List<SortedSet<Integer>>> jsonData = new HashMap<>();

            for (String filename : data.keySet()) {
                Triple<SortedSet<Integer>, SortedSet<Integer>, SortedSet<Integer>> tuple = data
                        .get(filename);
                List<SortedSet<Integer>> tupleList = Arrays.asList(tuple.getLeft(),
                        tuple.getMiddle(),
                        tuple.getRight());
                jsonData.put(filename, tupleList);
            }

            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonData);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Cannot convert to JSON: " + this, ex);
        }
    }

    public static CoverageInfo fromJsonString(String jsonString) throws IOException {
        try {
            ObjectMapper mapper = new ObjectMapper();

            Map<String, List<SortedSet<Integer>>> jsonData = mapper.readValue(jsonString,
                    new TypeReference<Map<String, List<SortedSet<Integer>>>>() {
                        // type reference for jackson
                    });

            Map<String, Triple<SortedSet<Integer>, SortedSet<Integer>, SortedSet<Integer>>> data = new HashMap<>();

            for (String filename : jsonData.keySet()) {
                List<SortedSet<Integer>> tupleList = jsonData.get(filename);

                if (tupleList.size() != 3) {
                    throw new RuntimeException(
                            "A tuple list was found with != 3 elements: " + jsonString);
                }

                Triple<SortedSet<Integer>, SortedSet<Integer>, SortedSet<Integer>> tuple = Triple
                        .of(tupleList.get(0), tupleList.get(1), tupleList.get(2));
                data.put(filename, tuple);
            }

            return new CoverageInfo(data);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Cannot parse JSON: " + jsonString);
        }
    }
}
