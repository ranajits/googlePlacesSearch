package in.focalworks.mapsapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.style.CharacterStyle;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks, OnMapReadyCallback,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

   private GoogleMap mMap;
    private Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;
    private static final String TAG = MainActivity.class.getSimpleName();
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    // Location updates intervals in sec
    private static int UPDATE_INTERVAL = 10000; // 10 sec
    private static int FATEST_INTERVAL = 5000; // 5 sec
    private static int DISPLACEMENT = 10; // 10 meters
    private LocationRequest mLocationRequest;
    MarkerOptions markerOptions;
    LatLng latLng;
    AutoCompleteTextView edtlocationSearch;
    boolean isDropDownSelected = false;
   // private PlaceAutocompleteFragment autocompleteFragment;
    private int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    private PlaceArrayAdapter mPlaceArrayAdapter;
    //private static final LatLngBounds BOUNDS_MOUNTAIN_VIEW = new LatLngBounds(new LatLng(37.398160, -122.180831), new LatLng(37.430610, -121.972090));
    private static final CharacterStyle STYLE_BOLD = new StyleSpan(Typeface.BOLD);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // First we need to check availability of play services
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        edtlocationSearch = (AutoCompleteTextView) findViewById(R.id.editText);

        if (checkPlayServices()) {
            // Building the GoogleApi client
            buildGoogleApiClient();
        }


        edtlocationSearch.setThreshold(2);
        edtlocationSearch.setOnItemClickListener(mAutocompleteClickListener);
        mPlaceArrayAdapter = new PlaceArrayAdapter(this, android.R.layout.simple_list_item_1, null, null);
        edtlocationSearch.setAdapter(mPlaceArrayAdapter);



    }


    private AdapterView.OnItemClickListener mAutocompleteClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final PlaceArrayAdapter.PlaceAutocomplete item = mPlaceArrayAdapter.getItem(position);
            final String placeId = String.valueOf(item.placeId);
            Log.i("Location", "Selected: " + item.description);
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
            Log.i("Location", "Fetching details for ID: " + item.placeId);
        }
    };
    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback
            = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                Log.e("Location", "Place query did not complete. Error: " +
                        places.getStatus().toString());
                return;
            }
            // Selecting the first object buffer.
            final Place place = places.get(0);

            Log.i("name", place.getName().toString());
            Log.i("coordinates", place.getLatLng().toString());

            mMap.animateCamera(CameraUpdateFactory.newLatLng(place.getLatLng()));
            markerOptions = new MarkerOptions();
            markerOptions.position(place.getLatLng());
            markerOptions.title(place.getName().toString());
            //edtlocationSearch.setText(""+ addressText);
            mMap.addMarker(markerOptions);


        }
    };


    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }



    /**
     * get location
     */
    private void getCurrentLoc() {
        Log.i(TAG, "getCurrentLoc");
        mLastLocation = LocationServices.FusedLocationApi
                .getLastLocation(mGoogleApiClient);

        if (mLastLocation != null) {
            double latitude = mLastLocation.getLatitude();
            double longitude = mLastLocation.getLongitude();

            //lblLocation.setText(latitude + ", " + longitude);
            Log.i(TAG, "getCurrentLoc " + latitude + ", " + longitude);
            LatLng currLocation = new LatLng(latitude, longitude);
            mMap.addMarker(new MarkerOptions().position(currLocation).title("Mumbai"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currLocation));
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
        }

    }

    /**
     * Method to verify google play services on the device
     *
     */
    private boolean checkPlayServices() {
        Log.i(TAG, "checkPlayServices");
        int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
                //GooglePlayServicesUtil                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }


    /**
     * Creating google api client object
     * */
    protected synchronized void buildGoogleApiClient() {

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }


    /**
     * Google api callback methods
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }


    @Override
    public void onConnected(Bundle arg0) {
        Log.i(TAG, "Connected");
        // Once connected with google api, get the location
        createLocationRequest();
        startLocationUpdates();
        mPlaceArrayAdapter.setGoogleApiClient(mGoogleApiClient);
    }


    @Override
    public void onConnectionSuspended(int arg0) {
        Log.i(TAG, "connection Suspended");
        mGoogleApiClient.connect();
    }


    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        Toast.makeText(getApplicationContext(), "Location changed!",
                Toast.LENGTH_SHORT).show();
        // Displaying the new location on UI
        getCurrentLoc();
    }


    /**
     * Starting the location updates
     * */
    protected void startLocationUpdates() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "No permissions granted!",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);

    }

    /**
     * Stopping location updates
     */
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }


    /**
     * Creating location request object
     *
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT); // 10 meters
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap=googleMap;
    }





    public class PlaceArrayAdapter extends ArrayAdapter<PlaceArrayAdapter.PlaceAutocomplete> implements Filterable {
        private static final String TAG = "PlaceArrayAdapter";
        private GoogleApiClient mGoogleApiClient;
        private AutocompleteFilter mPlaceFilter;
        private LatLngBounds mBounds;
        private ArrayList<PlaceAutocomplete> mResultList;

        /**
         * Constructor
         *
         * @param context  Context
         * @param resource Layout resource
         * @param bounds   Used to specify the search bounds
         * @param filter   Used to specify place types
         */
        public PlaceArrayAdapter(Context context, int resource, LatLngBounds bounds, AutocompleteFilter filter) {
            super(context, resource);
            mBounds = bounds;
            mPlaceFilter = filter;
        }

        public void setGoogleApiClient(GoogleApiClient googleApiClient) {
            if (googleApiClient == null || !googleApiClient.isConnected()) {
                mGoogleApiClient = null;
            } else {
                mGoogleApiClient = googleApiClient;
            }
        }

        @Override
        public int getCount() {
            return mResultList.size();
        }

        @Override
        public PlaceAutocomplete getItem(int position) {
            return mResultList.get(position);
        }

        private ArrayList<PlaceAutocomplete> getPredictions(CharSequence constraint) {
            if (mGoogleApiClient != null) {
                Log.i(TAG, "Executing autocomplete query for: " + constraint);
                PendingResult<AutocompletePredictionBuffer> results =
                        Places.GeoDataApi
                                .getAutocompletePredictions(mGoogleApiClient, constraint.toString(),
                                        mBounds, mPlaceFilter);
                // Wait for predictions, set the timeout.
                AutocompletePredictionBuffer autocompletePredictions = results
                        .await(60, TimeUnit.SECONDS);
                final Status status = autocompletePredictions.getStatus();
                if (!status.isSuccess()) {
                    Toast.makeText(getContext(), "Error: " + status.toString(),
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error getting place predictions: " + status
                            .toString());
                    autocompletePredictions.release();
                    return null;
                }

                Log.i(TAG, "Query completed. Received " + autocompletePredictions.getCount()
                        + " predictions.");
                Iterator<AutocompletePrediction> iterator = autocompletePredictions.iterator();
                ArrayList resultList = new ArrayList<>(autocompletePredictions.getCount());
                while (iterator.hasNext()) {

                    AutocompletePrediction prediction = iterator.next();
                    final CharSequence primaryText = prediction.getPrimaryText(new CharacterStyle() {
                        @Override
                        public void updateDrawState(TextPaint tp) {

                        }
                    });
                    Log.i(TAG, "Query completed. Received " + prediction.getPlaceId()+ "  "+ prediction.getPrimaryText(STYLE_BOLD));
                    resultList.add(new PlaceAutocomplete(prediction.getPlaceId(),      prediction.getFullText(STYLE_BOLD)));
                }
                // Buffer release
                autocompletePredictions.release();
                return resultList;
            }
            Log.e(TAG, "Google API client is not connected.");
            return null;
        }

        @Override
        public Filter getFilter() {
            Filter filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    if (constraint != null) {
                        // Query the autocomplete API for the entered constraint
                        mResultList = getPredictions(constraint);
                        if (mResultList != null) {
                            // Results
                            results.values = mResultList;
                            results.count = mResultList.size();
                        }
                    }
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    if (results != null && results.count > 0) {
                        // The API returned at least one result, update the data.
                        notifyDataSetChanged();
                    } else {
                        // The API did not return any results, invalidate the data set.
                        notifyDataSetInvalidated();
                    }
                }
            };
            return filter;
        }

        class PlaceAutocomplete {

            public CharSequence placeId;
            public CharSequence description;

            PlaceAutocomplete(CharSequence placeId, CharSequence description) {
                this.placeId = placeId;
                this.description = description;
            }

            @Override
            public String toString() {
                return description.toString();
            }
        }
    }



    // An AsyncTask class for accessing the GeoCoding Web Service
    private class GeocoderTask extends AsyncTask<Void, Void, List<Address>> {

        String location="";
        Geocoder geocoder ;
        List<Address> addresses = null;
        List<String>addressTextList;
        ArrayAdapter<String> adapter;
        public GeocoderTask(String location) {
            this.location=location;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
            addresses = new ArrayList<>();
            addressTextList = new ArrayList<>();
        }

        @Override
        protected List<Address> doInBackground(Void... locationName) {
            // Creating an instance of Geocoder class

            try {
                if(geocoder.isPresent()){
                    addresses = geocoder.getFromLocationName ( location,3);
                    Log.i(TAG, "geocoder present "  + addresses.size());
                }else {
                    Log.i(TAG, "geocoder not present");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return addresses;
        }

        @Override
        protected void onPostExecute(final List<Address> addresses) {
            if(addresses==null || addresses.size()==0){
                Toast.makeText(getBaseContext(), "No Location found", Toast.LENGTH_SHORT).show();
                edtlocationSearch.dismissDropDown();
            }else {

                addressTextList= new ArrayList<>();
                addressTextList.clear();
                // Clears all the existing markers on the map
                mMap.clear();
                for(int i=0;i<addresses.size();i++) {

                    Address address = (Address) addresses.get(i);
                    // Creating an instance of GeoPoint, to display in Google Map
                    latLng = new LatLng(address.getLatitude(), address.getLongitude());

                    String addressText = String.format("%s %s",
                            address.getMaxAddressLineIndex() > 0 ? address.getAddressLine(0) : "",
                            address.getCountryName());
                    addressTextList.add(addressText);
                    Log.i(TAG, "new location: " + addressText + " laylong : " + address.getLatitude() + " " + address.getLongitude());
                    markerOptions = new MarkerOptions();
                    markerOptions.position(latLng);
                    markerOptions.title(addressText);
                    //edtlocationSearch.setText(""+ addressText);
                    mMap.addMarker(markerOptions);

                    // Locate the first location
                    // if(i==0)
                    // mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                }
                if(adapter!=null){
                    adapter.clear();
                    adapter.notifyDataSetChanged();
                }
                adapter = new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_selectable_list_item, addressTextList);
                //Set the number of characters the user must type before the drop down list is shown
                edtlocationSearch.setThreshold(1);
                //   edtlocationSearch.dismissDropDown();
                Log.i(TAG, " adapter getCount(): "+ adapter.getCount());
                edtlocationSearch.setAdapter(null);
                // edtlocationSearch.
                adapter.notifyDataSetChanged();
                edtlocationSearch.setAdapter(adapter);
                if(isDropDownSelected){
                    isDropDownSelected=false;
                    edtlocationSearch.dismissDropDown();
                    Log.i(TAG, " dismissing dropdown"+isDropDownSelected );

                }else {
                    edtlocationSearch.showDropDown();
                    Log.i(TAG, " showing dropdown"+isDropDownSelected );
                }
                edtlocationSearch.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        isDropDownSelected=true;
                        Log.i(TAG, "clicked on : "+ addressTextList.get(position));
                        edtlocationSearch.setText(""+ addressTextList.get(position));
                        latLng = new LatLng(addresses.get(position).getLatitude(), addresses.get(position).getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));


                    }
                });
            }




        }
    }





}
