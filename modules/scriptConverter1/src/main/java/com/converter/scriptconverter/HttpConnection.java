package com.converter.scriptconverter;


import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HttpConnection {

    public static void main(String[] args) throws Exception {

        HttpConnection http = new HttpConnection();

        System.out.println("Send Http POST request");
        System.out.println(http.sendPost("https://zenbot.org/api/IS1MsuMUvoZoypbn?text=Hello", "{\"text\":\"Hello\",\"vars\":[{\"name\":\"string\",\"value\":\"popov_anton\"}]}").toJSONString());


    }

    public static JSONObject sendPost(String urlString, String json) {
        String output = null;
        try {

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            String input = json;

            OutputStream os = conn.getOutputStream();
            os.write(input.getBytes());
            os.flush();

            if (conn.getResponseCode() != HttpURLConnection.HTTP_CREATED) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));

            System.out.println("Output from Server .... \n");
            while ((output = br.readLine()) != null) {
                System.out.println(output);
            }

            conn.disconnect();

        } catch (MalformedURLException e) {

            e.printStackTrace();

        } catch (IOException e) {

            e.printStackTrace();

        }
        JSONParser parser = new JSONParser();
        JSONObject response = new JSONObject();
        try {
            response = (JSONObject) parser.parse(output);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return response;
    }
}
