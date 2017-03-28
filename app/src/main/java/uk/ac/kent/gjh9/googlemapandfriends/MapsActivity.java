package uk.ac.kent.gjh9.googlemapandfriends;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * for the map
 */
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import org.json.JSONException;

import uk.ac.kent.gjh9.googlemapandfriends.exceptions.ServerException;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    WebCaller web_call;

    private String TAG = MapsActivity.class.getSimpleName();

    private ProgressDialog pDialog;

    // URL to get contacts JSON
    private static String[] url = {
        "https://www.cs.kent.ac.uk/people/staff/iau/LocalUsers.php",
        "https://maps.googleapis.com/maps/api/directions/json?origin=51.297285,1.069743&destination=51.297397,1.064704&mode=walking&key=AIzaSyB2GhUJk-kj4WoCElFjv2u4uoFdQ8girZ0"
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web_call = new WebCaller();

        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        new GetUsers().execute();
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
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
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
    }

    /**
     * Async task class to get json by making HTTP call
     */
    private class GetUsers extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(MapsActivity.this);
            pDialog.setMessage("Please wait...");
            pDialog.setCancelable(false);
            pDialog.show();

        }

        @Override
        protected Void doInBackground(Void... arg0) {
            for(int i = url.length; --i >= 0;) {
                // Get JSON data from the url
                String web_data_string = web_call.getWebData(url[i]);

                Log.e(TAG, "Response from url: " + web_data_string);

                if (web_data_string != null) {
                    try {
                        if (web_data_string.contains("error_message")) {
                            throw new ServerException(web_call.getErrorMessage(web_data_string));
                        } else if (url[i].contains("LocalUsers")) {
                            web_call.getUsersLatLon(web_data_string);
                        } else if (url[i].contains("maps.googleapis.com/maps/api/directions"))
                            web_call.getDirections(web_data_string);
                    } catch (final JSONException e) {
                        Log.e(TAG, "Json parsing error: " + e.getMessage());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(),
                                        "Json parsing error: " + e.getMessage(),
                                        Toast.LENGTH_LONG)
                                        .show();
                            }
                        });
                    } catch (ServerException e) {
                        Log.e(TAG, "Server returned an error: " + e.getMessage());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(),
                                        "Something's wrong with the request",
                                        Toast.LENGTH_LONG)
                                        .show();
                            }
                        });
                    }
                } else {
                    Log.e(TAG, "Couldn't get json from server.");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Couldn't get json from server. Check LogCat for possible errors!",
                                    Toast.LENGTH_LONG)
                                    .show();
                        }
                    });
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // Dismiss the progress dialog
            if (pDialog.isShowing())
                pDialog.dismiss();

            // wait until map is ready -- could improve this with a listener
            while(mMap == null);

            for(int i = 0; i < web_call.userList.size(); i++) {
                setMapMarker(web_call.userList.get(i).get("lat"), web_call.userList.get(i).get("lon"), web_call.names.get(i));
            }

            LatLng focus = new LatLng(51.297285, 1.069743);
            mMap.addMarker(new MarkerOptions()
                    .position(focus)
                    .title("Uni of Kent"));

            final String LINE = web_call.getPolyline();

            List<LatLng> decodedPath = PolyUtil.decode(LINE);
            mMap.addPolyline(new PolylineOptions().addAll(decodedPath));

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(focus, 15.5f));
        }

    }
}