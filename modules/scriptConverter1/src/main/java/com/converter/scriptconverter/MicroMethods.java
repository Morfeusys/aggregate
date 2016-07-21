package com.converter.scriptconverter;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Created by popov on 19.07.2016.
 */
public class MicroMethods {
    private static Logger log = LoggerFactory.getLogger(MicroMethods.class);

    public static String getExternalIp() throws IOException {
        URL whatismyip = new URL("http://checkip.amazonaws.com");
        BufferedReader in = new BufferedReader(new InputStreamReader(
                whatismyip.openStream()));

        String ip = in.readLine();
        in.close();
        return ip;
    }

    public static String[] getLocation() { // IP geolocation is inherently imprecise. Locations are often near the center of the population. Any location provided by a GeoIP2 database or web service should not be used to identify a particular address or household.

        try {
            // TODO IP location library
            return new String[]{"0", "0"};
        } catch (Exception e) {
            log.error("Can`t determine location\n " + e.getMessage());
            return new String[]{"0", "0"};
        }
    }

    public static String evaluateExpression(String expression) {
        HttpConnection http = new HttpConnection();

        System.out.println("Send Http POST request");
        try {
            http.sendPost("https://zenbot.org/bots/IS1MsuMUvoZoypbn", "{\"text\":\"Hello\",\"vars\":[{\"name\":\"string\",\"value\":\"popov_anton\"}]}");
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return "";
    }

}
