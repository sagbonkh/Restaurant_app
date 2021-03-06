package com.carbon.restaurantinspection.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.carbon.restaurantinspection.R;
import com.carbon.restaurantinspection.model.InspectionDetail;
import com.carbon.restaurantinspection.model.InspectionManager;
import com.carbon.restaurantinspection.model.Restaurant;
import com.carbon.restaurantinspection.model.RestaurantManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import static com.carbon.restaurantinspection.model.Favourites.getFavouriteInspectionsList;
import static com.carbon.restaurantinspection.model.Favourites.getFavouriteList;
import static com.carbon.restaurantinspection.ui.MainActivity.isFirstTime;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    public static final String FAVOURITES_CHECKED = "com.carbon.restaurantinspection.ui.filterFragment.favouriteChecked";
    public static final String HAZARD_LEVEL_FROM_FILTER = "com.carbon.restaurantinspection.ui.filterFragment.hazardLevel";
    public static final String NUM_OF_CRITICAL_VIOLATIONS = "com.carbon.restaurantinspection.ui.filterFragment.numOfCrit";
    public static final String GREATER_CHECKED = "com.carbon.restaurantinspection.ui.filterFragment.greaterChecked";
    private Boolean locationPermissionsGranted = false;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Hashtable<LatLng, Integer> markerIcons;
    private Hashtable <Integer, Integer> restaurantIndexHolder;
    private int restaurantIndex;
    static private ClusterManager<MyMarkerClass> CLUSTER_MANAGER;
    private List<MyMarkerClass> myMarkerClassList = new ArrayList<>();
    private RestaurantManager restaurantManager;
    private List<Restaurant> restaurantList;
    private MarkerClusterRenderer renderer;
    private int index = -1;
    public static final String INTENT_NAME = "Map Activity";
    AlertDialog.Builder builderSingle;
    private ArrayList<String> newFavouriteInspections;
    private Toolbar toolbar;
    private ArrayAdapter<String> arrayAdapter;
    private final int FILTER_REQUEST_CODE = 619;
    private boolean favouritesChecked;
    private boolean greaterChecked;
    private int numOfCriticalVioaltionsfromFilter;
    private String hazardLevelFromFilter;
    private InspectionManager inspectionManager;

    public static Intent makeIntent(Context context, int index) {
        Intent intent = new Intent(context, MapActivity.class);
        intent.putExtra(INTENT_NAME, index);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // used rotate tutorial from
        // https://www.tutlane.com/tutorial/android/android-rotate-animations-clockwise-anti-clockwise-with-examples
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        getIntents();
        restaurantManager = RestaurantManager.getInstance(this);
        inspectionManager = InspectionManager.getInstance(this);
        restaurantList = restaurantManager.getRestaurantList();
        markerIcons = new Hashtable<>();
        restaurantIndexHolder = new Hashtable<>();
        toolbar = findViewById(R.id.toolbar);
        getLocationPermission();
        toolbarSetUp();
        updateNewFavouriteRestaurants();
        //Dialog tutorial: https://www.youtube.com/watch?v=0DH2tZjJtm0
        if (!(newFavouriteInspections.isEmpty()) && isFirstTime) {
            isFirstTime = false;
            ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(this, R.style.AlertDialogTheme);
            builderSingle = new AlertDialog.Builder(contextThemeWrapper);
            builderSingle.setView(LayoutInflater.from(this).inflate(R.layout.scrollable_dialog, null));
            showNewFavouriteInspectionsDialog();
        }
        isFirstTime = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (favouritesChecked || hazardLevelFromFilter != null
                || numOfCriticalVioaltionsfromFilter > 0) {
            filterRestaurants();
        }
    }

    private void filterRestaurants() {
        ArrayList<String> filtered;
        if (favouritesChecked && hazardLevelFromFilter != null
                && numOfCriticalVioaltionsfromFilter > 0) {
            filtered = inspectionManager.filter(getFavouriteList(),
                    hazardLevelFromFilter, numOfCriticalVioaltionsfromFilter, greaterChecked);
        } else if (favouritesChecked && hazardLevelFromFilter != null) {
            filtered = inspectionManager.filter(getFavouriteList(),
                    hazardLevelFromFilter);
        } else if (hazardLevelFromFilter != null && numOfCriticalVioaltionsfromFilter > 0) {
            filtered = inspectionManager.filter(hazardLevelFromFilter,
                    numOfCriticalVioaltionsfromFilter, greaterChecked);

        } else if (favouritesChecked && numOfCriticalVioaltionsfromFilter > 0) {
            filtered = inspectionManager.filter(getFavouriteList(),
                    numOfCriticalVioaltionsfromFilter, greaterChecked);
        } else if (favouritesChecked) {
            filtered = getFavouriteList();
        } else if (hazardLevelFromFilter != null) {
            filtered = inspectionManager.filter(hazardLevelFromFilter);
        } else if (numOfCriticalVioaltionsfromFilter > 0) {
            filtered = inspectionManager.filter(numOfCriticalVioaltionsfromFilter, greaterChecked);
        } else {
            filtered = new ArrayList<>();
        }
        ArrayList<Restaurant> filteredList = restaurantManager.filter(filtered);
        CLUSTER_MANAGER.clearItems();
        if (!filteredList.isEmpty()) {
            myMarkerClassList.clear();
            restaurantIndexHolder.clear();
            restaurantIndex = 0;
            clickCluster();
            clickClusterItem();
            setRestaurantMarkers();
            CLUSTER_MANAGER.addItems(myMarkerClassList);
            CLUSTER_MANAGER.cluster();
            CLUSTER_MANAGER.getMarkerCollection().setInfoWindowAdapter(
                    new ExtraInfoWindowAdapter(this));
        }
        CLUSTER_MANAGER.cluster();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.map_search_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.search_icon);
        SearchView searchView = (SearchView) menuItem.getActionView();
        String searchHere = getString(R.string.searchHere);
        searchView.setQueryHint(searchHere);
        if (restaurantManager.getSearchTerm() != null) {
            searchView.setQuery(restaurantManager.getSearchTerm(), false);
            searchView.setIconified(false);
            searchView.clearFocus();
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                if (!s.isEmpty()) {
                    ArrayList<Restaurant> searched = restaurantManager.searchRestaurants(s);
                    CLUSTER_MANAGER.clearItems();
                    if (!searched.isEmpty()) {
                        restaurantIndex = 0;
                        restaurantIndexHolder.clear();
                        myMarkerClassList.clear();
                        setRestaurantMarkers();
                        CLUSTER_MANAGER.addItems(myMarkerClassList);
                        clickCluster();
                        clickClusterItem();
                    }
                    CLUSTER_MANAGER.cluster();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {

                return true;
            }
        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                restaurantIndex = 0;
                restaurantManager.clearSearch();
                CLUSTER_MANAGER.clearItems();
                myMarkerClassList.clear();
                restaurantIndexHolder.clear();
                setRestaurantMarkers();
                CLUSTER_MANAGER.addItems(myMarkerClassList);
                clickCluster();
                clickClusterItem();
                CLUSTER_MANAGER.cluster();
                return false;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.filter){
                   Intent intent = new Intent(MapActivity.this, FilterFragment.class);
                   startActivityForResult(intent, FILTER_REQUEST_CODE);
                   return true;
        } else if (item.getItemId() == R.id.clear_text_icon) {
            restaurantIndex = 0;
            restaurantManager.clearFilter();
            CLUSTER_MANAGER.clearItems();
            myMarkerClassList.clear();
            restaurantIndexHolder.clear();
            setRestaurantMarkers();
            CLUSTER_MANAGER.addItems(myMarkerClassList);
            CLUSTER_MANAGER.cluster();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILTER_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                numOfCriticalVioaltionsfromFilter = data.getIntExtra(
                        NUM_OF_CRITICAL_VIOLATIONS, 0);
                hazardLevelFromFilter = data.getStringExtra(
                        HAZARD_LEVEL_FROM_FILTER);
                favouritesChecked = data.getBooleanExtra(
                        FAVOURITES_CHECKED,
                        false);
                greaterChecked = data.getBooleanExtra(GREATER_CHECKED, false);
            }
        }
    }

    private void toolbarSetUp () {
        toolbarBackButton();
        List<String> restaurantNames = new ArrayList<>();
        int size = restaurantList.size();

        for (int i = 0; i < size; i++) {
            String name = restaurantList.get(i).getName();
            restaurantNames.add(name);
        }

        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                restaurantNames);
    }


    private void getIntents() {
        Intent intent = getIntent();
        index = intent.getIntExtra(INTENT_NAME, -1);
    }

    private void toolbarBackButton () {
        setSupportActionBar(toolbar);
        String activityTitle = getString(R.string.map);
        getSupportActionBar().setTitle(activityTitle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MapActivity.this, RestaurantListActivity.class);
                finish();
                startActivity(intent);
            }
        });
    }

    private void getLocationPermission() {
        // reference Youtuber: CodingWithMitch, Playlist: Google Maps & Google Places Android Course
        //https://www.youtube.com/playlist?list=PLgCYzUzKIBE-vInwQhGSdnbyJ62nixHCt
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                locationPermissionsGranted = true;
                initializeMap();
            } else {
                ActivityCompat.requestPermissions(this, permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this, permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        // reference Youtuber: CodingWithMitch, Playlist: Google Maps & Google Places Android Course
        //https://www.youtube.com/playlist?list=PLgCYzUzKIBE-vInwQhGSdnbyJ62nixHCt
        locationPermissionsGranted = false;

        if (requestCode == 1234){
            if(grantResults.length > 0) {
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        locationPermissionsGranted = false;
                        return;
                    }
                }
                locationPermissionsGranted = true;
                finish();
                startActivity(getIntent());
            }
        }
    }

    private void initializeMap() {
        // reference Youtuber: CodingWithMitch, Playlist: Google Maps & Google Places Android Course
        // https://www.youtube.com/playlist?list=PLgCYzUzKIBE-vInwQhGSdnbyJ62nixHCt
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        // reference Youtuber: CodingWithMitch, Playlist: Google Maps & Google Places Android Course
        // https://www.youtube.com/playlist?list=PLgCYzUzKIBE-vInwQhGSdnbyJ62nixHCt
        // https://www.youtube.com/watch?v=fPFr0So1LmI&list=PLgCYzUzKIBE-vInwQhGSdnbyJ62nixHCt&index=6&t=0s
        this.googleMap = googleMap;

        if (locationPermissionsGranted) {
            CLUSTER_MANAGER = new ClusterManager<>(this, this.googleMap);
            renderer = new MarkerClusterRenderer(this,
                    this.googleMap, CLUSTER_MANAGER);
            CLUSTER_MANAGER.setRenderer(renderer);
            this.googleMap.setOnCameraIdleListener(CLUSTER_MANAGER);
            this.googleMap.setOnMarkerClickListener(CLUSTER_MANAGER);
            this.googleMap.setOnInfoWindowClickListener(CLUSTER_MANAGER);
            this.googleMap.setMyLocationEnabled(true);
            this.googleMap.getUiSettings().setMyLocationButtonEnabled(false);

            if(RestaurantDetailsActivity.latitude == 0 && RestaurantDetailsActivity.longitude == 0){
                getCurrentLocation();
            }

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission
                    .ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission
                    .ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            googleMap.setMyLocationEnabled(true);
            this.googleMap.getUiSettings().setMyLocationButtonEnabled(false);
            clickCluster();
            clickClusterItem();
            setRestaurantMarkers();
            CLUSTER_MANAGER.addItems(myMarkerClassList);
            CLUSTER_MANAGER.cluster();
            CLUSTER_MANAGER.getMarkerCollection().setInfoWindowAdapter(
                    new ExtraInfoWindowAdapter(this));
        }
        if (index > -1) {
            displayRestaurantOnMap();
        }
    }

    private void displayRestaurantOnMap() {
        final Handler handler = new Handler();
        final MyMarkerClass myMarkerClass = myMarkerClassList.get(index);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (renderer.getMarker(myMarkerClass) == null) {
                    LatLng latLng = myMarkerClass.position;
                    moveCamera(latLng, 20f);
                    handler.postDelayed(this, 100);
                } else {
                    renderer.getMarker(myMarkerClass).showInfoWindow();
                }
            }
        };
        handler.post(runnable);
    }

    private void clickClusterItem() {
        CLUSTER_MANAGER.setOnClusterItemInfoWindowClickListener(new ClusterManager.
                OnClusterItemInfoWindowClickListener<MyMarkerClass>() {
            @Override
            public void onClusterItemInfoWindowClick(MyMarkerClass item) {
                int index = restaurantIndexHolder.get(item.getRestaurant_index());
                Intent intent = RestaurantDetailsActivity.makeIntent(MapActivity.this, index);
                startActivity(intent);
            }
        });
    }

    private void clickCluster() {
        CLUSTER_MANAGER.setOnClusterClickListener(new ClusterManager.OnClusterClickListener<MyMarkerClass>() {
            @Override
            public boolean onClusterClick(Cluster<MyMarkerClass> cluster) {
                if (cluster == null) {
                    return false;
                }
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (MyMarkerClass user : cluster.getItems()) {
                    builder.include(user.getPosition());
                }
                LatLngBounds bounds = builder.build();
                try {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        });
    }

    private void getCurrentLocation() {
        // reference Youtuber: CodingWithMitch, Playlist: Google Maps & Google Places Android Course
        // https://www.youtube.com/watch?v=fPFr0So1LmI&list=PLgCYzUzKIBE-vInwQhGSdnbyJ62nixHCt&index=6&t=0s
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if(locationPermissionsGranted) {

            final Task location = fusedLocationProviderClient.getLastLocation();
            location.addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if (task.isSuccessful()) {
                        Location currentLocation = (Location) task.getResult();
                        LatLng myLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                        moveCamera(myLatLng, DEFAULT_ZOOM);

                    } else {
                        Toast.makeText(MapActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void moveCamera(LatLng latLng, float zoom){
        //https://www.youtube.com/watch?v=fPFr0So1LmI&list=PLgCYzUzKIBE-vInwQhGSdnbyJ62nixHCt&index=6&t=0s
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    // gets the Restaurant and Inspection Lists and helps set markers where appropriate

    private void setRestaurantMarkers() {
        restaurantManager = RestaurantManager.getInstance(this);
        restaurantList = restaurantManager.getRestaurantList();

        if(restaurantList != null){
            int numOfRestaurants = restaurantList.size();

            for(int i = 0; i < numOfRestaurants; i++) {
                Restaurant restaurant = restaurantList.get(i);
                String trackingNum = restaurant.getTrackingNumber();

                List<InspectionDetail> inspectionDetailList = inspectionManager
                        .getInspections(trackingNum);

                float latitude = (float) restaurant.getLatitude();
                float longitude = (float) restaurant.getLongitude();
                String name = restaurant.getName();

                if(inspectionDetailList != null) {
                    int size = inspectionDetailList.size();
                    InspectionDetail inspectionDetail = inspectionDetailList.get(size - 1);

                    placeMarker(new LatLng(latitude, longitude), DEFAULT_ZOOM, inspectionDetail,
                            restaurant, i);
                } else {
                    String address = restaurant.getPhysicalAddress();
                    placeMarker(new LatLng(latitude, longitude), DEFAULT_ZOOM, name, address, i);
                }
            }
        }
    }
    /**moves the camera to the location of the chosen restaurant given the restaurant HAS NO
     inspections**/
    private void placeMarker(LatLng latLng, float zoom, String title, String address, int index){
        // googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        if(!title.equals("Current location")) {
            final LatLng latLng1 = new LatLng(latLng.latitude, latLng.longitude);

            myMarkerClassList.add(new MyMarkerClass(latLng1, title, address,
                    R.drawable.safepeg, index));
            markerIcons.put(latLng, 0);
            restaurantIndexHolder.put(index, restaurantIndex);
            restaurantIndex++;
        }
    }

    /** moves the camera to the location of the chosen restaurant given the restaurant HAS an
     inspection **/
    private void placeMarker(LatLng latLng, float zoom, InspectionDetail inspectionDetail,
                             Restaurant restaurant, int index){

        // googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        if(inspectionDetail != null){
            String addressDisplay = getString(R.string.address);
            String hazardLevelDisplay = getString(R.string.hazardLevel);
            String snippet = addressDisplay + " " + restaurant.getPhysicalAddress() + "\n\n" +
                    hazardLevelDisplay + " " + inspectionDetail.getHazardLevel();

            String hazardLevel = inspectionDetail.getHazardLevel();
            int image_id;
            if (hazardLevel.equals("High")) {
                image_id = R.drawable.highpeg;
            } else if (hazardLevel.equals("Moderate")) {
                image_id = R.drawable.midpeg;
            } else {
                image_id = R.drawable.safepeg;
            }

            final LatLng latLng1 = new LatLng(latLng.latitude, latLng.longitude);

            myMarkerClassList.add(new MyMarkerClass(latLng1, restaurant.getName(), snippet,
                    image_id, index));
            markerIcons.put(latLng, image_id);
            restaurantIndexHolder.put(index, restaurantIndex);
            restaurantIndex++;
        }
    }

    /**
     *  Class that creates the pop up display when a marker is clicked
     */
    class ExtraInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        private final View view;

        ExtraInfoWindowAdapter(Context context) {
            view = LayoutInflater.from(context).inflate(R.layout.extra_info_window, null);
        }

        private void rendowWindowText(final Marker marker, View view1) {
            String title = marker.getTitle();
            TextView textView = view1.findViewById(R.id.extra_title);

            if (!title.equals("")) {
                textView.setText(title);
            }

            String snippet = marker.getSnippet();
            TextView textViewSnippet = view1.findViewById(R.id.snippet);

            if (snippet != null && !snippet.equals("")) {
                textViewSnippet.setText(snippet);
            }

            ImageView imageView = view1.findViewById(R.id.hazard_level);

            if (marker.getId() != null && markerIcons != null && markerIcons.size() > 0) {
                int image_id = markerIcons.get(marker.getPosition());
                if (image_id != 0) {
                    if (image_id == R.drawable.safepeg) {
                        imageView.setImageResource(R.drawable.greencheckmark);
                    } else if (image_id == R.drawable.midpeg) {
                        imageView.setImageResource(R.drawable.yellow_caution);
                    } else {
                        imageView.setImageResource(R.drawable.red_skull_crossbones);
                    }
                } else {
                    imageView.setImageResource(R.drawable.error_icon);
                }
            }
        }

        @Override
        public View getInfoWindow(Marker marker) {
            rendowWindowText(marker, view);
            return view;
        }

        @Override
        public View getInfoContents(Marker marker) {
            rendowWindowText(marker, view);
            return null;
        }

    }
    /**
     * Renderer is required for ClusterManager. Particularly to change the marker icon.
     */
    public class MarkerClusterRenderer extends DefaultClusterRenderer<MyMarkerClass> {

        private static final int MARKER_DIMENSION = 90;

        private final IconGenerator iconGenerator;
        private final ImageView markerImageView;
        MarkerClusterRenderer(Context context, GoogleMap map,
                              ClusterManager<MyMarkerClass> clusterManager) {
            super(context, map, clusterManager);
            iconGenerator = new IconGenerator(context);
            markerImageView = new ImageView(context);
            markerImageView.setLayoutParams(new ViewGroup.LayoutParams(MARKER_DIMENSION,
                    MARKER_DIMENSION));
            iconGenerator.setContentView(markerImageView);
        }

        @Override
        protected void onBeforeClusterItemRendered(MapActivity.MyMarkerClass item,
                                                   MarkerOptions markerOptions) {

//          //Bitmap resizing taken from:
//          https://stackoverflow.com/questions/35718103/how-to-specify-the-size-of-the-icon-on-the-marker-in-google-maps-v2-android
            int height = 100;
            int width = 100;
            Bitmap b = BitmapFactory.decodeResource(getResources(), item.getVectorID());
            Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);

            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(smallMarker));
            markerOptions.title(item.getTitle());
            markerOptions.snippet(item.getSnippet());
        }

        @Override
        protected boolean shouldRenderAsCluster(Cluster cluster) {
            return cluster.getSize() > 2;
        }
    }

    /**
     *  Class that imitates a marker. Stores all the information a marker does.
     *  Required for using ClusterManager
     */
    public static class MyMarkerClass implements ClusterItem {

        private final LatLng position;
        private final String title;
        private final String snippet;
        private final int vectorID;
        private final int restaurant_index;

        MyMarkerClass(LatLng position, String title, String snippet, int vectorID,
                      int restaurant_index) {
            this.position = position;
            this.title = title;
            this.snippet = snippet;
            this.vectorID = vectorID;
            this.restaurant_index = restaurant_index;
        }

        @Override
        public LatLng getPosition() {
            return position;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public String getSnippet() {
            return snippet;
        }

        int getVectorID(){
            return vectorID;
        }

        int getRestaurant_index(){
            return restaurant_index;
        }

    }

    //Dialog tutorial:
    //https://stackoverflow.com/questions/15762905/how-can-i-display-a-list-view-in-an-android-alert-dialog
    private void showNewFavouriteInspectionsDialog() {
        String dialogueTitle = getString(R.string.dialogueTitle);
        builderSingle.setTitle(dialogueTitle);



        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, newFavouriteInspections);
        String close = getString(R.string.close);
        builderSingle.setNegativeButton(close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        AlertDialog alert = builderSingle.create();
        alert.show();
    }

    private void updateNewFavouriteRestaurants() {
        newFavouriteInspections = getFavouriteInspectionsList(MapActivity.this);
    }
}