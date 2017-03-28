package uk.ac.kent.gjh9.googlemapandfriends;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;
import java.util.List;
import android.support.v4.app.FragmentActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import org.json.JSONException;

import uk.ac.kent.gjh9.googlemapandfriends.exceptions.ServerException;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private WebCaller web_call;

    private String TAG = MapsActivity.class.getSimpleName();

    private ProgressDialog pDialog;
    private Polyline direction_line;

    public MapsActivity() {
        this.web_call = new WebCaller();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        showProgress();
        initMap();
    }

    private void initMap() {
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
        // configure the UI
//        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        UiSettings ui_settings = mMap.getUiSettings();
        ui_settings.setZoomControlsEnabled(true);
        ui_settings.setCompassEnabled(true);
        ui_settings.setMyLocationButtonEnabled(true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(MapsActivity.this, "This app needs your location to work properly", Toast.LENGTH_LONG).show();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            }
        }
        else {
            mMap.setMyLocationEnabled(true);
        }

        // Get data for the map
        String[] url = {
                "https://www.cs.kent.ac.uk/people/staff/iau/LocalUsers.php",
                "https://maps.googleapis.com/maps/api/directions/json?origin=51.297285,1.069743&destination=51.297397,1.064704&mode=walking&key=AIzaSyB2GhUJk-kj4WoCElFjv2u4uoFdQ8girZ0"
        };
        new GetUsers().execute(url[0]);
        new MapDirections().execute(url[1]);
        // Dismiss the progress dialog
        if (pDialog.isShowing())
            pDialog.dismiss();
    }

    /**
     * Add markers to the mMap map using coordinate
     * @param lat the latitude
     * @param lon the longitude
     * @param title the name of the position for the label
     */
    void setMapMarker(double lat, double lon, String title) {
        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(lat, lon))
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
    }

    /**
     * Get the current polyline in use
     */
    Polyline getDirection() { return direction_line; }

    /**
     * Set the new polyline
     */
    void setDirection(Polyline direction) {
        if(direction_line != null)
            direction_line.remove();
        direction_line = direction;
    }

    /**
     * Add the content from the internet: markers, polylines...
     */
    private void addMapContent() {
        for(int i = 0; i < web_call.userList.size(); i++) {
            setMapMarker(web_call.userList.get(i).get("lat"), web_call.userList.get(i).get("lon"), web_call.names.get(i));
        }

        LatLng focus = new LatLng(51.297285, 1.069743);
        mMap.addMarker(new MarkerOptions()
                .position(focus)
                .title("Uni of Kent"));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(focus, 15.5f));
    }

    /**
     * Add polyline to the map
     */
    private void addMapPolyline() {
        final String LINE = web_call.getPolyline();

        List<LatLng> decodedPath = PolyUtil.decode(LINE);
        setDirection( mMap.addPolyline(new PolylineOptions().addAll(decodedPath)) );
    }

    /**
     * Show a progress dialog
     */
    private void showProgress() {
        pDialog = new ProgressDialog(MapsActivity.this);
        pDialog.setMessage("Please wait...");
        pDialog.setCancelable(false);
        pDialog.show();
    }

    /**
     * Async task class to get json by making HTTP call
     */
    private class GetUsers extends AsyncTask<String, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... url) {
            for(int i = url.length; --i >= 0;) {
                // Get JSON data from the url
                String web_data_string = web_call.getWebData(url[i]);
                Log.e(TAG, "Response from url: " + web_data_string);

                if (web_data_string != null) {
                    try {
                        web_call.getUsersLatLon(web_data_string);

                    } catch (final JSONException e) {
                        Log.e(TAG, "Json parsing error: " + e.getMessage());
                    }
                } else {
                    Log.e(TAG, "Couldn't get json from server.");
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            addMapContent();
        }
    }

    private class MapDirections extends AsyncTask<String, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... url) {
                // Get JSON data from the url
                String web_data_string = web_call.getWebData(url[0]);
                Log.e(TAG, "Response from url: " + web_data_string);

                if (web_data_string != null) {
                    try {
                        web_call.getDirections(web_data_string);

                    } catch (final JSONException e) {
                        Log.e(TAG, "Json parsing error: " + e.getMessage());
                    } catch (ServerException e) {
                        Log.e(TAG, "Server returned an error: " + e.getMessage());
                    }
                } else {
                    Log.e(TAG, "Couldn't get json from server.");
                }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            addMapPolyline();
        }
    }
}