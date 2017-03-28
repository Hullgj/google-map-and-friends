package uk.ac.kent.gjh9.googlemapandfriends;

/**
 * Created by gavin on 11/03/2017.
 *
 */

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class WebCall {

    private static final String TAG = WebCall.class.getSimpleName();

    public WebCall() {
    }

    /**
     * Make a HTTP connection to the url, call streamtostring to
     * modify the data for processing and close the connection
     * @param reqUrl
     * @return
     */
    public String downloadData(String reqUrl) {
        String response = null;
        URL url = null;
        try {
            url = new URL(reqUrl);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        // open connection to url
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            // read the response
            InputStream in = new BufferedInputStream(connection.getInputStream());
            response = streamToString(in);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            connection.disconnect();
        }
        return response;
    }

    /**
     * Take a stream and convert it into a string
     * Adapted from Ravi Tamada - http://www.androidhive.info/2012/01/android-json-parsing-tutorial/
     * @param is the input stream
     * @return a string
     */
    private String streamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder str_builder = new StringBuilder();

        String line = "";
        try {
            while ((line = reader.readLine()) != null) {
                str_builder.append(line).append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return str_builder.toString();
    }
}