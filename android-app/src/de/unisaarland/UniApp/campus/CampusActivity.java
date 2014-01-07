package de.unisaarland.UniApp.campus;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;
import de.unisaarland.UniApp.R;
import de.unisaarland.UniApp.bus.BusDetailActivity;
import de.unisaarland.UniApp.bus.model.PointOfInterest;
import de.unisaarland.UniApp.campus.model.CustomMapTileProvider;
import de.unisaarland.UniApp.campus.model.CustomMapTileSupportProvider;
import de.unisaarland.UniApp.campus.uihelper.CustomInfoWindowAdapter;
import de.unisaarland.UniApp.campus.uihelper.PanelButtonListener;
import de.unisaarland.UniApp.campus.uihelper.SearchViewAdapter;
import de.unisaarland.UniApp.database.DatabaseHandler;
import de.unisaarland.UniApp.restaurant.RestaurantActivity;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Shahzad
 * Date: 12/3/13
 * Time: 4:45 PM
 * To change this template use File | Settings | File Templates.
 */

/*
* It implements Location listeners to show the distance of the bus stop from users current location.
* */
public class CampusActivity extends FragmentActivity implements ConnectionCallbacks,OnConnectionFailedListener,
        LocationListener,
        GoogleMap.OnMyLocationButtonClickListener, OnMarkerClickListener,OnInfoWindowClickListener{

    private GoogleMap map;
    private LocationClient locationClient;
    private final int REQUEST_CODE = 5;
    private java.util.HashMap<String,PointOfInterest> poisMap = new java.util.HashMap<String,PointOfInterest>();
    private Location currentLocation;
    private final LatLngBounds UNIVERSITY = new LatLngBounds(
            new LatLng(7.03466212,49.25030771), new LatLng(7.05128056,49.25946299));
    private static final int TIME_INTERVAL = 3000; // 3 seconds

    //////////////location will be updated after every 3 seconds//////////////
    private static final LocationRequest REQUEST = LocationRequest.create()
            .setInterval(TIME_INTERVAL)
            .setFastestInterval(16)    // 16ms = 60fps
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    private String infoBuilding = null;
    private ArrayList<Marker> markers;
    private AutoCompleteTextView search;

    /*
    * Will be called when activity created first time e.g. from scratch and check if intent has any extra information
    * extra information will be set if activity is called from search staff page.
    * */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        savedInstanceState = getIntent().getExtras();
        if(savedInstanceState!=null){
            infoBuilding = savedInstanceState.getString("building");
        }
        setContentView(R.layout.campus_layout);
        // sets the custom navigation bar according to each activity.
        setActionBar();
    }

    /*
    * Will be called when activity created first time after onCreate or when activity comes to the front again or in a pausing state
    * So its better to set all the things needed to use in the activity here if in case anything is released in onPause method
    * */
    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        setUpLocationClient();
        locationClient.connect();
    }

    public void searchItemSelected(PointOfInterest model) {
        ArrayList<PointOfInterest> pois = new ArrayList<PointOfInterest>();
        pois.add(model);
        pinPOIsInArray(pois);
        search.setText("");
        InputMethodManager imm = (InputMethodManager)getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(search.getWindowToken(), 0);
    }

    // get map object and set default parameters e.g mapType and camera position
    // also set the panel listener for changing map type and remove all pins
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (map == null) {
            // Try to obtain the map from the SupportMapFragment.
            map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            markers = new ArrayList<Marker>();
            // Check if we were successful in obtaining the map.
            if (map != null) {
                // set default options of a map which are loaded with the activity
                // like default zoom level and campera position
                map.setMyLocationEnabled(true);
                map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                map.setOnMyLocationButtonClickListener(this);
                map.setOnMarkerClickListener(this);
                map.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                    @Override
                    public void onCameraChange(CameraPosition cameraPosition) {
                        float maxZoom = 18.0f;
                        if (cameraPosition.zoom > maxZoom)
                            map.animateCamera(CameraUpdateFactory.zoomTo(maxZoom));
                    }
                });
                map.setBuildingsEnabled(false);
                map.getUiSettings().setZoomControlsEnabled(false);
                map.setInfoWindowAdapter(new CustomInfoWindowAdapter(this,poisMap));
                map.setOnInfoWindowClickListener(this);
                map.addTileOverlay(new TileOverlayOptions().tileProvider(new CustomMapTileProvider(getResources().getAssets())));
                map.addTileOverlay(new TileOverlayOptions().tileProvider(new CustomMapTileSupportProvider(getResources().getAssets())));
                CameraUpdate upd = CameraUpdateFactory.newLatLngZoom(new LatLng(49.25419, 7.041324), 15);
                map.moveCamera(upd);
                // if info building != null means activity is called from search result details page
                // so it will get the building position from the database and will set the marker there.
                if(infoBuilding != null){
                    DatabaseHandler db = new DatabaseHandler(CampusActivity.this);
                    ArrayList<PointOfInterest> pois = db.getPointsOfInterestForTitle(infoBuilding);
                    db.close();
                    pinPOIsInArray(pois);
                }
            }
        }
        Button panelButton = (Button) findViewById(R.id.panel_button);
        panelButton.setOnClickListener(new PanelButtonListener(this,map,poisMap,markers));
    }

    // return false as i want to open the marker from default implementation i haven't done any specific
    @Override
    public boolean onMarkerClick(com.google.android.gms.maps.model.Marker marker){
        return false;
    }

    /*
    * on clicking the info button it will check if more then 1 action available then it will open a dialog
    * otherwise it will just open the chooser activity for the route
    * and it also has extra check in case of mensa it will open the mensa page.
    * */
    @Override
    public void onInfoWindowClick(final Marker marker) {
        final PointOfInterest p = poisMap.get(marker.getTitle());
        if (p.isCanShowRightCallOut() == 1 && p.getWebsite().length() > 0) {
            AlertDialog.Builder builder1 = new AlertDialog.Builder(CampusActivity.this);
            builder1.setTitle(getString(R.string.action));
            builder1.setMessage(getString(R.string.web_or_route));
            builder1.setCancelable(true);

            builder1.setPositiveButton(getString(R.string.website),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if (p.isCanShowRightCallOut() == 1 && p.getWebsite().length() > 0) {
                                if((marker.getTitle().equals("Mensa") && marker.getSnippet().equals("Restaurant")) ||
                                        marker.getTitle().equals("Mensacafé")){
                                    Intent myIntent = new Intent(CampusActivity.this, RestaurantActivity.class);
                                    myIntent.putExtra("back",getString(R.string.campusText));
                                    CampusActivity.this.startActivity(myIntent);
                                }else{
                                    Intent myIntent = new Intent(CampusActivity.this, BusDetailActivity.class);
                                    myIntent.putExtra("url", p.getWebsite());
                                    myIntent.putExtra("back", getString(R.string.campusText));
                                    CampusActivity.this.startActivity(myIntent);
                                }
                            }
                            dialog.cancel();
                        }
                    });
            builder1.setNegativeButton(getString(R.string.route),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            showRouteIfAvailable(p);
                            dialog.cancel();
                        }
                    });
            AlertDialog alert11 = builder1.create();
            alert11.show();
        } else {
            showRouteIfAvailable(p);
        }
    }

    /*
    * It will show the route in external application from current position to destination
    * if current location is set e.g. using wifi, mobile network or gps otherwise it will display
    * message that please enable at least one location service and will open the settings page on Clicking
    * */
    private void showRouteIfAvailable(PointOfInterest p) {
        if(currentLocation != null){
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://maps.google.com/maps?saddr=" + currentLocation.getLatitude() +
                            "," + currentLocation.getLongitude() + "&daddr=" +
                            p.getLatitude() + "," + p.getLongitude()));
            startActivity(intent);
        }else{
            AlertDialog.Builder currentLocationOptionDialog = new AlertDialog.Builder(CampusActivity.this);
            currentLocationOptionDialog.setTitle(getString(R.string.current_location_not_set));
            currentLocationOptionDialog.setMessage(getString(R.string.enable_location_option));
            currentLocationOptionDialog.setCancelable(true);
            currentLocationOptionDialog.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent viewIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(viewIntent);
                }
            });
            AlertDialog locationOption = currentLocationOptionDialog.create();
            locationOption.show();
        }
    }

    // setup location listener
    private void setUpLocationClient() {
        if (locationClient == null) {
            locationClient = new LocationClient(
                    getApplicationContext(),
                    this,  // ConnectionCallbacks
                    this); // OnConnectionFailedListener
        }
    }

    /**
     * sets the custom navigation bar according to each activity.
     */
    private void setActionBar() {
        ActionBar actionBar = getActionBar();
        // add the custom view to the action bar
        actionBar.setCustomView(R.layout.navigation_bar_layout);
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.LTGRAY));

        TextView backPageText = (TextView) actionBar.getCustomView().findViewById(R.id.page_back_text);
        if(infoBuilding == null) {
            backPageText.setText(R.string.homeText);
        }else{
            backPageText.setText(R.string.info);
        }
        backPageText.setVisibility(View.VISIBLE);
        backPageText.setOnClickListener(new BackButtonClickListener(this));

        ImageButton backButton = (ImageButton) actionBar.getCustomView().findViewById(R.id.back_icon);
        backButton.setVisibility(View.VISIBLE);
        backButton.setOnClickListener(new BackButtonClickListener(this));

        ImageButton rightButton = (ImageButton) actionBar.getCustomView().findViewById(R.id.page_right_icon);
        rightButton.setVisibility(View.VISIBLE);
        rightButton.setBackgroundResource(R.drawable.lines_button);
        rightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(CampusActivity.this, CampusSearchActivity.class);
                CampusActivity.this.startActivityForResult(myIntent,REQUEST_CODE);
            }
        });

        search = (AutoCompleteTextView) actionBar.getCustomView().findViewById(R.id.search_field);
        search.setVisibility(View.VISIBLE);
        search.setHint("Search");

        DatabaseHandler db = new DatabaseHandler(this);

        final ArrayList<PointOfInterest> searchBase = db.getPointsOfInterestPartialMatched();
        db.close();
        SearchViewAdapter adapter =  new SearchViewAdapter(searchBase,this,android.R.layout.simple_dropdown_item_1line,this);
        search.setThreshold(1);
        search.setAdapter(adapter);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
    }

    /*
    * will be called when user selects any point of interest from campusSearchActivity page and will receive
    * a list of PointOfInterests.
    * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_CODE == requestCode && resultCode == RESULT_OK && data.getExtras() != null) {
            ArrayList<Integer> ids = (ArrayList<Integer>) data.getExtras().get("idsArray");
            DatabaseHandler db = new DatabaseHandler(this);
            ArrayList<PointOfInterest> pois = db.getPointsOfInterestForIDs(ids);
            db.close();
            pinPOIsInArray(pois);
        }
    }

    /*
    * add pins to all pointOfInterests in the list and will compute the visible rectangle so that
    * all point of interests are displayed on the screen.
    * */
    private boolean pinPOIsInArray(ArrayList<PointOfInterest> POIs){
        boolean res = false;
        float lati = 0.0f;
        float longi = 0.0f;
        if (POIs.size() > 0) {
            for(PointOfInterest poi : POIs){
                int tempColor = poi.getColor();
                float color = BitmapDescriptorFactory.HUE_RED;

                if(tempColor == 1){
                    color = BitmapDescriptorFactory.HUE_CYAN;
                }else if(tempColor == 2){
                    color = BitmapDescriptorFactory.HUE_GREEN;
                }
                poisMap.put(poi.getTitle(),poi);
               Marker m =map.addMarker(new MarkerOptions()
                       .position(new LatLng(poi.getLatitude(), poi.getLongitude()))
                       .title(poi.getTitle())
                       .snippet(poi.getSubtitle())
                       .icon(BitmapDescriptorFactory.defaultMarker(color)));
                lati = poi.getLatitude();
                longi = poi.getLongitude();
                markers.add(m);
            }
            res = true;
        }


        LatLngBounds.Builder builder = LatLngBounds.builder();
        for(Marker m : markers){
            builder.include(m.getPosition());
        }
        LatLngBounds bounds = builder.build();
        bounds = adjustBoundsForMaxZoomLevel(bounds);
        try{
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 10));
        }catch (Exception e){
            if(lati != 0.0 && longi != 0.0){
            CameraUpdate upd = CameraUpdateFactory.newLatLngZoom(new LatLng(lati, longi),15 );
            map.moveCamera(upd);
        }
            Log.e("MyTag",e.getMessage());
        }
        return res;

    }

    // will adjust the bounds to include markers
    private LatLngBounds adjustBoundsForMaxZoomLevel(LatLngBounds bounds) {
        LatLng sw = bounds.southwest;
        LatLng ne = bounds.northeast;
        double deltaLat = Math.abs(sw.latitude - ne.latitude);
        double deltaLon = Math.abs(sw.longitude - ne.longitude);

        final double zoomN = 0.002; // minimum zoom coefficient
        if (deltaLat < zoomN) {
            sw = new LatLng(sw.latitude - (zoomN - deltaLat / 2), sw.longitude);
            ne = new LatLng(ne.latitude + (zoomN - deltaLat / 2), ne.longitude);
            bounds = new LatLngBounds(sw, ne);
        }
        else if (deltaLon < zoomN) {
            sw = new LatLng(sw.latitude, sw.longitude - (zoomN - deltaLon / 2));
            ne = new LatLng(ne.latitude, ne.longitude + (zoomN - deltaLon / 2));
            bounds = new LatLngBounds(sw, ne);
        }

        return bounds;
    }

    @Override
    public void onConnected(Bundle bundle) {
        locationClient.requestLocationUpdates(
                REQUEST,
                this);
    }

    @Override
    public void onDisconnected() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean onMyLocationButtonClick() {
        return false;
    }

    // custom class to show the back button action using navigation bar and will call the onBack method of activity
    class BackButtonClickListener implements View.OnClickListener{
        final Activity activity;
        public BackButtonClickListener(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onClick(View v) {
            activity.onBackPressed();
        }
    }
}
