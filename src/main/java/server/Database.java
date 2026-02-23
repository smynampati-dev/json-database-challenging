package server;

import java.util.HashMap;
import java.util.Map;

public class Database {

    private final Map<String, String> storage = new HashMap<>();

    public String set(String key, String value) {
        storage.put(key, value);
        return "OK";
    }

    public String get(String key) {
        if (!storage.containsKey(key)) {
            return "ERROR";
        }
        return storage.get(key);
    }

    public String delete(String key) {
        if (!storage.containsKey(key)) {
            return "ERROR";
        }
        storage.remove(key);
        return "OK";
    }

    public int size() {
        return storage.size();
    }
}
