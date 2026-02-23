package client;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    private static final String ADDRESS = "127.0.0.1";
    private static final int PORT = 23456;

    private static final Gson gson = new Gson();

    private static class Args {

        @Parameter(names = "-t")
        String type;

        @Parameter(names = "-k")
        String key;

        @Parameter(names = "-v")
        String value;

        @Parameter(names = "-in")
        String fileName;
    }

    public static void main(String[] args) {

        Args arguments = new Args();
        JCommander.newBuilder()
                .addObject(arguments)
                .build()
                .parse(args);

        System.out.println("Client started!");

        try (Socket socket =
                     new Socket(InetAddress.getByName(ADDRESS), PORT);
             DataInputStream input =
                     new DataInputStream(socket.getInputStream());
             DataOutputStream output =
                     new DataOutputStream(socket.getOutputStream())) {

            String requestJson;

            if (arguments.fileName != null) {

                String path = System.getProperty("user.dir")
                        + "/src/client/data/" + arguments.fileName;

                requestJson = Files.readString(Path.of(path));

            } else {

                JsonObject request = new JsonObject();
                request.addProperty("type", arguments.type);

                if (!"exit".equals(arguments.type)) {
                    request.addProperty("key", arguments.key);
                }

                if ("set".equals(arguments.type)) {
                    request.addProperty("value", arguments.value);
                }

                requestJson = gson.toJson(request);
            }

            output.writeUTF(requestJson);
            System.out.println("Sent: " + requestJson);

            String response = input.readUTF();
            System.out.println("Received: " + response);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
