package cag;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Main {
    
    public static Ribbon instance;
    public static Handler exceptionHandler = new Handler();
    
    public static void main(String[] args) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler(exceptionHandler);

        instance = new Ribbon(new URI("wss://tetr.io/ribbon"));
    }

    public static void tetrioStats(String nickname) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL("https://ch.tetr.io/api/users/" + nickname.toLowerCase()).openConnection();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            connection.setRequestProperty("User-Agent", "asdf i dont know");

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
            int idk;
            try {
                while ((idk = reader.read()) != -1) {
                    sb.append((char) idk);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            String jsonString = sb.toString();
            Gson gson = new Gson();
            Map<?, ?> map = gson.fromJson(jsonString, Map.class);
            Map<?, ?> data = (Map<?, ?>) map.get("data");
            Map<?, ?> user = (Map<?, ?>) data.get("user");
            Map<?, ?> league = (Map<?, ?>) user.get("league");
            System.out.println("nickname: " + user.get("username"));
            System.out.println("country: " + user.get("country"));
            System.out.println("rank: " + league.get("rank") + ", " + league.get("rating") + "TR");
            System.out.println("glicko: " + league.get("glicko") + "±" + league.get("rd"));
            System.out.println(league.get("apm") + "APM " + league.get("pps") + "PPS " + league.get("vs") + "VS");
        }).start();
    }
    
    public static String getFromApi(String url) {/*
        return await (await fetch(API_BASE + url, {
            method: "GET",
            headers: {
                "Authorization": "Bearer " + process.env.TOKEN
            }
        })).json();*/
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
    
}
