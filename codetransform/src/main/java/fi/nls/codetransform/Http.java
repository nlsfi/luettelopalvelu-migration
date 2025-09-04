package fi.nls.codetransform;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class Http {

    public static int head(String url) throws Exception {
        URL u = new URI(url).toURL();
        HttpURLConnection c = (HttpURLConnection) u.openConnection();
        c.setRequestMethod("HEAD");
        c.setRequestProperty("Accept", "application/json");
        return c.getResponseCode();
    }

    public static byte[] get(String url) throws Exception {
        URL u = new URI(url).toURL();
        HttpURLConnection c = (HttpURLConnection) u.openConnection();
        c.setRequestProperty("Accept", "application/json");
        try (InputStream in = c.getInputStream()) {
            return in.readAllBytes();
        } catch (Exception ex) {
            try (InputStream err = c.getErrorStream()) {
                System.err.write(err.readAllBytes());
            } catch (Exception ignore) { }
            throw ex;
        }
    }

    public static byte[] post(String url, byte[] jsonPayload, String bearerToken) throws Exception {
        URL u = new URI(url).toURL();
        HttpURLConnection c = (HttpURLConnection) u.openConnection();
        c.setDoOutput(true);
        c.setRequestMethod("POST");
        c.setRequestProperty("Authorization", "Bearer " + bearerToken);
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("Content-Type", "application/json");
        c.setRequestProperty("Content-Length", "" + jsonPayload.length);
        try (OutputStream out = c.getOutputStream()) {
            out.write(jsonPayload);
        }
        try (InputStream in = c.getInputStream()) {
            return in.readAllBytes();
        } catch (Exception ex) {
            try (InputStream err = c.getErrorStream()) {
                System.err.write(err.readAllBytes());
            } catch (Exception ignore) { }
            throw ex;
        }
    }

    public static boolean delete(String url, String bearerToken) throws Exception {
        URL u = new URI(url).toURL();
        HttpURLConnection c = (HttpURLConnection) u.openConnection();
        c.setRequestMethod("DELETE");
        c.setRequestProperty("Authorization", "Bearer " + bearerToken);
        int responseCode = c.getResponseCode();
        boolean isOk = responseCode / 100 == 2;
        if (isOk) {
            try (InputStream in = c.getInputStream()) {
                in.readAllBytes();
            }
        } else {
            try (InputStream err = c.getErrorStream()) {
                System.err.write(err.readAllBytes());
            } catch (Exception ignore) { }
        }
        return isOk;
    }

}

