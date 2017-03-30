package uk.ac.kent.gjh9.googlemapandfriends;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import java.util.ArrayList;
import java.util.List;
import android.support.v4.app.FragmentActivity;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import org.json.JSONException;
import uk.ac.kent.gjh9.googlemapandfriends.exceptions.ServerException;

/**
 * The main activity of the android maps application controlling the classes, views, and asyntask.
 * The Google map starts first, initialising the features and components, when the map is ready
 * the MapMaker and MapMarker classes are instantiated. We then make a spinner to enable changing
 * between different views of the map.
 *
 * After initialization, we make a call to WebCaller with the URL containing the markers, which are
 * placed on the map. Finally, we set a click listener on any marker.
 *
 * When a marker is clicked, the info window shows with the title, and we log one marker click in
 * waypoint_marker. On another click of a marker we build a URL with the coordinates of both clicked
 * markers, the transport method, and the Google API key - sending this to Google Directions. Then
 * we parse the response to get a polyline that we plot on the map, showing the route betweeen
 * markers.
 *
 * Created by gavin on 11/03/2017.
 */

public class MapsActivity extends FragmentActivity implements
        GoogleMap.OnMarkerClickListener,
        OnMapReadyCallback,
        AdapterView.OnItemSelectedListener {

    private GoogleMap mMap;
    private WebCaller web_call;
    private MapMarkers map_markers;
    private String TAG = MapsActivity.class.getSimpleName();
    private ProgressDialog pDialog;
    private List<LatLng> waypoint_marker = new ArrayList<>();

    public MapsActivity() {
        this.web_call = new WebCaller();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        initMap();
    }

    /**
     * Initialize the map in asynchronous mode
     */
    protected void initMap() {
        if(mMap == null) {
            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }
    }

    /**
     * Configure the map, asking for location permissions
     * Afterwards we start the asynchronous task of getting the content for the map
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        MapMaker map_maker = new MapMaker(mMap);
        this.map_markers = new MapMarkers(web_call, mMap);
        // configure the UI
        map_maker.mapConfig();
        spinnerConfig();

        // Get data for the map
        String[] url = {
                "https://www.cs.kent.ac.uk/people/staff/iau/LocalUsers.php"
        };
        new WebCallProcessor().execute(url[0]);

        // Set a listener for marker click.
        mMap.setOnMarkerClickListener(this);
    }

    /**
     * Configure the spinner that allows a choice of map view
     */
    private void spinnerConfig() {
        Spinner map_type = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.map_type, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        map_type.setAdapter(adapter);

        map_type.setOnItemSelectedListener(this);
    }

    /**
     * When the user clicks on a marker check if less than two map_markers have been clicked, and store
     * the latitude and longitude in waypoint_marker field. When the waypoint_field has two sets
     * of lat and lng, call the directional polyline method to draw the line
     * @param marker the marker the user has clicked on
     * @return false always to preserve the default behaviour of clicking on a marker -> show label
     */
    @Override
    public boolean onMarkerClick(Marker marker) {
        if(waypoint_marker.size() >= 2 || waypoint_marker == null)
            waypoint_marker = new ArrayList<>();

        waypoint_marker.add(marker.getPosition());

        if(waypoint_marker.size() == 2) {
            String google_directions_url = "https://maps.googleapis.com/maps/api/directions/json?origin=" +
                    waypoint_marker.get(0).latitude +
                    "," +
                    waypoint_marker.get(0).longitude +
                    "&destination=" +
                    waypoint_marker.get(1).latitude +
                    "," +
                    waypoint_marker.get(1).longitude +
                    "&mode=walking&key=AIzaSyB2GhUJk-kj4WoCElFjv2u4uoFdQ8girZ0";
            new WebCallProcessor().execute(google_directions_url);
        }
        return false;
    }

    /**
     * Show a progress dialog
     */
    private void showProgress(boolean toggle) {
        // Dismiss the progress dialog
        if (pDialog == null)
            pDialog = new ProgressDialog(MapsActivity.this);

        else if(pDialog.isShowing())
            pDialog.dismiss();

        if(toggle) {
            pDialog.setMessage("Please wait...");
            pDialog.setCancelable(false);
            pDialog.show();
        }
    }

    /**
     * Change the view of the map based on the selected button in the spinner.
     * @param parent the parent container of the app
     * @param view the main view of the app
     * @param position the position of the selected option in the spinner
     * @param id the ID of the spinner
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String map_type = parent.getItemAtPosition(position).toString();
        switch(map_type) {
            case "Normal": mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL); break;
            case "Hybrid": mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID); break;
            case "Satellite": mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE); break;
            case "Terrain": mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN); break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    /**
     * Async task class to get json by making HTTP call
     */
    private class WebCallProcessor extends AsyncTask<String, Integer, Integer> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgress(true);
        }

        @Override
        protected Integer doInBackground(String... url) {
            int result = 0;
            // Get JSON data from the url
            String web_data_string = web_call.getWebData(url[0]);
            Log.e(TAG, "Response from url: " + web_data_string);

            if (web_data_string != null) {
                try {
                    result = web_call.readJSONData(web_data_string);

                } catch (final JSONException e) {
                    Log.e(TAG, "Json parsing error: " + e.getMessage());
                } catch (ServerException e) {
                    Log.e(TAG, "Server returned an error: " + e.getMessage());
                }
            } else {
                Log.e(TAG, "Couldn't get json from server.");
            }
            return result;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            if(result == 0)
                map_markers.addMapMarkers();
            else if(result == 1)
                map_markers.addMapPolyline();
            showProgress(false);
        }
    }
}