package uk.ac.kent.gjh9.googlemapandfriends;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.UiSettings;

/**
 * Set some default settings for the view of the map and any buttons wanted.
 *
 * Created by gavin on 21/03/2017.
 */

public class MapMaker extends MapsActivity {

    GoogleMap mMap;

    public MapMaker(GoogleMap mMap) {
        this.mMap = mMap;
    }

    /**
     * Configure the map with buttons and default settings
     */
    protected void mapConfig() {
        UiSettings ui_settings = mMap.getUiSettings();
        ui_settings.setZoomControlsEnabled(true);
        ui_settings.setCompassEnabled(true);
        ui_settings.setMyLocationButtonEnabled(true);
    }
}
