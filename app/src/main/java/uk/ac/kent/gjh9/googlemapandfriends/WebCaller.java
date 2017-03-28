package uk.ac.kent.gjh9.googlemapandfriends;

/**
 * Created by gavin on 11/03/2017.
 */

import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class WebCaller {

    private static final String TAG = WebCaller.class.getSimpleName();
    ArrayList<HashMap<String, Double>> userList;
    List<String> names;
    String polyline;

    public WebCaller() {
        this.userList = new ArrayList<>();
        this.names = new ArrayList<>();
        this.polyline = null;
    }

    public ArrayList<HashMap<String, Double>> getUserList() {
        return userList;
    }

    public List<String> getNames() {
        return names;
    }

    public String getPolyline() { return polyline; }

    public String getWebData(String reqUrl) {
        String data_string = "";
//        URL url = null;
        HttpURLConnection urlConnection = null;
        InputStream stream_buffer = null;
        try {
            URL url = new URL(reqUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            stream_buffer = new BufferedInputStream(urlConnection.getInputStream());
            data_string = streamToString(stream_buffer);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            // If a page is not loaded, but connection was established get the stream error
            if (urlConnection != null) {
                Log.e(TAG, "getInputStream Error: " + urlConnection.getErrorStream());
            }
        } finally {
            urlConnection.disconnect();
        }
        return data_string;
    }

    /**
     * Parse a string and get the JSON Array of the Users, then for each add the name, longitude
     * latitude into the list and array for access later by the set marker method
     * @param web_data_string
     * @throws JSONException
     */
    public void getUsersLatLon(String web_data_string) throws JSONException {
        JSONObject json_obj = new JSONObject(web_data_string);

        JSONArray users = json_obj.getJSONArray("Users");

        // looping through All Contacts
        for (int i = users.length(); --i >= 0;) {
            JSONObject u = users.getJSONObject(i);

            String name = u.getString("name");
            Double lon = u.getDouble("lon");
            Double lat = u.getDouble("lat");

            HashMap<String, Double> user = new HashMap<>();

            names.add(name);
            user.put("lon", lon);
            user.put("lat", lat);

            userList.add(user);
        }
    }

    /**
     * Parse the JSON string to get the routes -> overview_polyline -> points, which contains
     * the encoded polyline representing a direction between two or more points
     * @param web_data_string
     */
    public void getDirections(String web_data_string) throws JSONException {
        JSONObject json_obj = new JSONObject(web_data_string);

        JSONObject routes = json_obj.getJSONArray("routes").getJSONObject(0);
        polyline = routes.getJSONObject("overview_polyline").getString("points");
    }

    /**
     * When the JSON states an error message from the server
     * we can log and print it for debugging
     */
    public String getErrorMessage(String web_data_string) throws JSONException {
        JSONObject error = new JSONObject(web_data_string);
        return error.getString("error_message");
    }

    /**
     * Make a string from the stream
     * Adapted from Ravi Tamada - http://www.androidhive.info/2012/01/android-json-parsing-tutorial/
     * @param is
     * @return
     */
    private String streamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
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
        return sb.toString();
    }
}