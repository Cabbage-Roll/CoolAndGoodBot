package cag;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static Ribbon chatInstance;
    public static Map<String, Ribbon> roomInstances;
    public static final Handler exceptionHandler = new Handler();

    public static void main(String[] args) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler(exceptionHandler);

        chatInstance = new Ribbon(new URI("wss://tetr.io/ribbon"));
    }

    public static String getFromApi(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL("https://tetr.io/api/" + url).openConnection();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        connection.setRequestProperty("User-Agent", "Java");
        try {
            connection.setRequestMethod("GET");
        } catch (ProtocolException e) {
            e.printStackTrace();
        }
        connection.setRequestProperty("Authorization", "Bearer " + Secret.userToken);
        try {
            connection.connect();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        StringBuilder sb = new StringBuilder();
        int character;
        try {
            while ((character = reader.read()) != -1) {
                sb.append((char) character);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    public static String getFromCh(String url) {
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) new URL("https://ch.tetr.io/api/" + url).openConnection();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        connection.setRequestProperty("User-Agent", "Java");
        try {
            connection.setRequestMethod("GET");
        } catch (ProtocolException e) {
            e.printStackTrace();
        }

        try {
            connection.connect();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        StringBuilder sb = new StringBuilder();
        int character;
        try {
            while ((character = reader.read()) != -1) {
                sb.append((char) character);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

}
