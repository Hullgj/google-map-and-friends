package uk.ac.kent.gjh9.googlemapandfriends;

import android.graphics.Color;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;
import java.util.List;

/**
 * Control and add markers to the map. This class gets data from WebCaller including the coordinates
 * of the markers, and the polyline of the direction between two clicked markers.
 *
 * Created by gavin on 21/03/2017.
 */

public class MapMarkers extends MapsActivity {

    private WebCaller web_call;
    private GoogleMap mMap;
    private Polyline direction_line;

    public MapMarkers(WebCaller web_call, GoogleMap mMap) {
        this.web_call = web_call;
        this.mMap = mMap;
    }
    /**
     * Add markers to the mMap map using coordinates, title, and make them azure blue.
     * @param lat the latitude
     * @param lon the longitude
     * @param title the name of the position for the label
     */
    protected void setMapMarker(double lat, double lon, String title) {
        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(lat, lon))
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
    }

    /**
     * Add the content from the internet: markers, polylines...
     */
    protected void addMapMarkers() {

        for(int i = 0; i < web_call.getUserList().size(); i++) {
            setMapMarker(web_call.getUserList().get(i).get("lat"), web_call.getUserList().get(i).get("lon"), web_call.getNames().get(i));
        }

        LatLng focus = new LatLng(51.297285, 1.069743);
        mMap.addMarker(new MarkerOptions()
                .position(focus)
                .title("Senate Building\nUni of Kent"));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(focus, 15.5f));
    }

    /**
     * Set the new polyline
     */
    protected void setDirection(Polyline direction) {
        if(direction_line != null)
            direction_line.remove();
        direction_line = direction;
    }

    /**
     * Add polyline to the map
     */
    protected void addMapPolyline() {
        final String map_polyline = web_call.getPolyline();
        if(map_polyline != null) {
            List<LatLng> decodedPath = PolyUtil.decode(map_polyline);
            setDirection( mMap.addPolyline(new PolylineOptions().addAll(decodedPath)
                    .color(Color.MAGENTA)
            ) );
        }
    }
}
