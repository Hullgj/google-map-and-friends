package uk.ac.kent.gjh9.googlemapandfriends;

import android.util.Log;
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
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import uk.ac.kent.gjh9.googlemapandfriends.exceptions.ServerException;

/**
 * WebCaller makes a connection to a server over HTTP, parses the data using JSON, and returns
 * strings of the web call. It has two lists for the coordinates and names of the markers, whereas
 * the direction is stored as an encoded polyline. There are several checks for errors in the
 * connection, while parsing the JSON, and any bad responses from the server.
 *
 * Created by gavin on 11/03/2017.
 */

class WebCaller {

    private static final String TAG = WebCaller.class.getSimpleName();
    private ArrayList<HashMap<String, Double>> userList;
    private List<String> names;
    private String polyline;

    WebCaller() {
        this.userList = new ArrayList<>();
        this.names = new ArrayList<>();
        this.polyline = null;
    }

    ArrayList<HashMap<String, Double>> getUserList() {
        return userList;
    }

    List<String> getNames() {
        return names;
    }

    String getPolyline() { return polyline; }

    /**
     * Establish a connection to a given URL to get a stream of data. We convert this stream to a
     * string.
     *
     * @param reqUrl the URL given and to connect to
     * @return a string of the response from the server after converting it from a stream
     */
    String getWebData(String reqUrl) {
        String data_string = null;
        HttpURLConnection urlConnection = null;
        InputStream stream_buffer;
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
            assert urlConnection != null;
            urlConnection.disconnect();
        }
        return data_string;
    }

    /**
     * Determine the type of data received and call the appropriate JSON parser
     */
    int readJSONData(String web_data_string) throws JSONException, ServerException {
        int result = -1;
        JSONObject json_obj = new JSONObject(web_data_string);
        if (web_data_string.contains("error_message")) {
            throw new ServerException(getErrorMessage(json_obj));
        }
        else if(web_data_string.contains("Users")) {
            getUsersLatLon(json_obj);
            result = 0;
        }
        else if(web_data_string.contains("geocoded_waypoints")) {
            getDirections(json_obj);
            result = 1;
        }
        return result;
    }

    /**
     * Parse a string and get the JSON Array of the Users, then for each add the name, longitude
     * latitude into the list and array for access later by the set marker method
     * @throws JSONException when a parsing error occurs
     */
    private void getUsersLatLon(JSONObject json_obj) throws JSONException {

        JSONArray users = json_obj.getJSONArray("Users");

        // looping through All Contacts
        for (int i = users.length(); --i >= 0;) {
            JSONObject u = users.getJSONObject(i);

            String name = u.getString("name");
            Double lon = u.getDouble("lon");
            Double lat = u.getDouble("lat");

            HashMap<String, Double> user = new HashMap<>();
            user.put("lon", lon);
            user.put("lat", lat);
            userList.add(user);

            names.add(name);
        }
    }

    /**
     * Parse the JSON string to get the routes -> overview_polyline -> points, which contains
     * the encoded polyline representing a direction between two or more points
     */
    private void getDirections(JSONObject json_obj) throws JSONException {
        JSONObject routes = json_obj.getJSONArray("routes").getJSONObject(0);
        polyline = routes.getJSONObject("overview_polyline").getString("points");
    }

    /**
     * When the JSON states an error message from the server
     * we can log and print it for debugging
     */
    private String getErrorMessage(JSONObject error) throws JSONException {
        return error.getString("error_message");
    }

    /**
     * Make a string from the stream
     * Adapted from Ravi Tamada - http://www.androidhive.info/2012/01/android-json-parsing-tutorial/
     * @param is the input stream from the web call of the raw JSON data
     * @return a string of the converted stream
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