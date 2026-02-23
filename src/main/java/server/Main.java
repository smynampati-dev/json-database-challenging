package server;

import com.google.gson.*;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Main {

    private static final String ADDRESS = "127.0.0.1";
    private static final int PORT = 23456;
    private static final String DB_PATH = "server/data/db.json";

    private static final Gson gson = new Gson();

    private static final ReadWriteLock lock = new ReentrantReadWriteLock();
    private static final Lock readLock = lock.readLock();
    private static final Lock writeLock = lock.writeLock();

    private static final ExecutorService executor =
            Executors.newFixedThreadPool(4);

    private static volatile boolean running = true;
    private static ServerSocket serverSocket;

    public static void main(String[] args) {

        System.out.println("Server started!");

        // 🔥 Reset DB at server start (important for Hyperskill tests)
        try {
            File file = new File(DB_PATH);
            file.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("{}");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            serverSocket = new ServerSocket(PORT, 50, InetAddress.getByName(ADDRESS));

            while (running) {
                try {
                    Socket socket = serverSocket.accept();
                    executor.submit(() -> handle(socket));
                } catch (IOException e) {
                    if (!running) break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    private static void handle(Socket socket) {

        try (socket;
             DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {

            String requestJson = input.readUTF();
            JsonObject request = JsonParser.parseString(requestJson).getAsJsonObject();

            String type = request.get("type").getAsString();

            if ("exit".equals(type)) {
                JsonObject response = new JsonObject();
                response.addProperty("response", "OK");
                output.writeUTF(gson.toJson(response));
                running = false;
                serverSocket.close();
                return;
            }

            String response = process(request);
            output.writeUTF(response);

        } catch (IOException ignored) {}
    }

    private static String process(JsonObject request) {

        String type = request.get("type").getAsString();
        JsonObject response = new JsonObject();

        switch (type) {

            case "get":
                readLock.lock();
                try {
                    JsonObject db = readDB();
                    JsonElement result = getNested(db, request.get("key"));

                    if (result == null) {
                        response.addProperty("response", "ERROR");
                        response.addProperty("reason", "No such key");
                    } else {
                        response.addProperty("response", "OK");
                        response.add("value", result);
                    }
                } finally {
                    readLock.unlock();
                }
                break;

            case "set":
                writeLock.lock();
                try {
                    JsonObject db = readDB();
                    setNested(db, request.get("key"), request.get("value"));
                    writeDB(db);
                    response.addProperty("response", "OK");
                } finally {
                    writeLock.unlock();
                }
                break;

            case "delete":
                writeLock.lock();
                try {
                    JsonObject db = readDB();
                    boolean deleted = deleteNested(db, request.get("key"));

                    if (deleted) {
                        writeDB(db);
                        response.addProperty("response", "OK");
                    } else {
                        response.addProperty("response", "ERROR");
                        response.addProperty("reason", "No such key");
                    }
                } finally {
                    writeLock.unlock();
                }
                break;
        }

        return gson.toJson(response);
    }

    // -------- GET nested value --------
    private static JsonElement getNested(JsonObject db, JsonElement keyElement) {

        if (keyElement.isJsonPrimitive()) {
            String key = keyElement.getAsString();
            return db.has(key) ? db.get(key) : null;
        }

        JsonArray keys = keyElement.getAsJsonArray();
        JsonElement current = db;

        for (JsonElement key : keys) {
            if (!current.isJsonObject()) return null;

            JsonObject obj = current.getAsJsonObject();
            String k = key.getAsString();

            if (!obj.has(k)) return null;

            current = obj.get(k);
        }

        return current;
    }

    // -------- SET nested value --------
    private static void setNested(JsonObject db, JsonElement keyElement, JsonElement value) {

        if (keyElement.isJsonPrimitive()) {
            db.add(keyElement.getAsString(), value);
            return;
        }

        JsonArray keys = keyElement.getAsJsonArray();
        JsonObject current = db;

        for (int i = 0; i < keys.size() - 1; i++) {
            String k = keys.get(i).getAsString();

            if (!current.has(k) || !current.get(k).isJsonObject()) {
                current.add(k, new JsonObject());
            }

            current = current.getAsJsonObject(k);
        }

        String lastKey = keys.get(keys.size() - 1).getAsString();
        current.add(lastKey, value);
    }

    // -------- DELETE nested value --------
    private static boolean deleteNested(JsonObject db, JsonElement keyElement) {

        if (keyElement.isJsonPrimitive()) {
            return db.remove(keyElement.getAsString()) != null;
        }

        JsonArray keys = keyElement.getAsJsonArray();
        JsonObject current = db;

        for (int i = 0; i < keys.size() - 1; i++) {
            String k = keys.get(i).getAsString();

            if (!current.has(k) || !current.get(k).isJsonObject()) {
                return false;
            }

            current = current.getAsJsonObject(k);
        }

        String lastKey = keys.get(keys.size() - 1).getAsString();
        return current.remove(lastKey) != null;
    }

    // -------- DB read --------
    private static JsonObject readDB() {
        try {
            String content = Files.readString(Path.of(DB_PATH)).trim();
            if (content.isEmpty()) return new JsonObject();
            return JsonParser.parseString(content).getAsJsonObject();
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    // -------- DB write --------
    private static void writeDB(JsonObject db) {
        try (FileWriter writer = new FileWriter(DB_PATH)) {
            gson.toJson(db, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
