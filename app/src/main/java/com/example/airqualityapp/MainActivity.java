package com.example.airqualityapp;

import static com.mapbox.maps.plugin.gestures.GesturesUtils.addOnMapClickListener;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;
import com.mapbox.maps.ViewAnnotationAnchor;
import com.mapbox.maps.ViewAnnotationOptions;
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor;
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.AnnotationPluginImplKt;
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManagerKt;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions;
import com.mapbox.maps.plugin.gestures.OnMapClickListener;
import com.mapbox.maps.viewannotation.ViewAnnotationManager;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.DecimalFormat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_LOCATION = 101;

    int VERY_LOW_COLOR;
    int LOW_COLOR;
    int MEDIUM_COLOR;
    int HIGH_COLOR;
    int VERY_HIGH_COLOR;

    MapView mapView;
    TextView textViewLastUpdateTime, textViewLocation, textViewPM10, textViewPM2_5, textViewSO2, textViewNO2, textViewO3, textViewCO, textViewNH3, textViewNO;
    SearchView searchView;
    CardView infoCardView;

    FloatingActionButton locateBtn, showInfoPopupBtn;
    Button closeCardViewBtn;

    private PointAnnotationManager pointAnnotationManager;
    private PopupWindow popupWindow;
    Bitmap markerBitmap;

    public interface LocationCallback {
        void onLocationReceived(String location);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        VERY_LOW_COLOR = ContextCompat.getColor(this, R.color.green);
        LOW_COLOR = ContextCompat.getColor(this, R.color.light_green);
        MEDIUM_COLOR = ContextCompat.getColor(this, R.color.yellow);
        HIGH_COLOR = ContextCompat.getColor(this, R.color.orange);
        VERY_HIGH_COLOR = ContextCompat.getColor(this, R.color.red);

        mapView = findViewById(R.id.mapView);
        searchView = findViewById(R.id.searchView);
        locateBtn = findViewById(R.id.locateBtn);
        showInfoPopupBtn = findViewById(R.id.showInfoPopupBtn);
        closeCardViewBtn = findViewById(R.id.closeCardViewBtn);

        textViewLastUpdateTime = findViewById(R.id.textViewLastUpdateTime);
        textViewLocation = findViewById(R.id.textViewLocation);
        textViewPM10 = findViewById(R.id.textViewPM10);
        textViewPM2_5 = findViewById(R.id.textViewPM2_5);
        textViewSO2 = findViewById(R.id.textViewSO2);
        textViewNO2 = findViewById(R.id.textViewNO2);
        textViewO3 = findViewById(R.id.textViewO3);
        textViewCO = findViewById(R.id.textViewCO);
        textViewNH3 = findViewById(R.id.textViewNH3);
        textViewNO = findViewById(R.id.textViewNO);

        infoCardView = findViewById(R.id.infoCardView);

        markerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.red_marker);

        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                AnnotationPlugin annotationPlugin = AnnotationPluginImplKt.getAnnotations(mapView);
                pointAnnotationManager = PointAnnotationManagerKt.createPointAnnotationManager(annotationPlugin, mapView);

                Point centreOfEuropePoint = Point.fromLngLat(13, 50);
                mapView.getMapboxMap().setCamera(
                        new CameraOptions.Builder()
                                .center(centreOfEuropePoint)
                                .zoom(3.0)
                                .build());

                addOnMapClickListener(mapView.getMapboxMap(), new OnMapClickListener() {
                    @Override
                    public boolean onMapClick(@NonNull Point point) {
                        DecimalFormat decimalFormat = new DecimalFormat("#.#####");
                        double latitude = Double.parseDouble(decimalFormat.format(point.latitude()));
                        double longitude = Double.parseDouble(decimalFormat.format(point.longitude()));

                        addMarkerToMap(longitude, latitude);

                        getLocationByLngLat(longitude, latitude, new LocationCallback() {
                            @Override
                            public void onLocationReceived(String location) {
                                Toast.makeText(MainActivity.this, location, Toast.LENGTH_SHORT).show();
                                String locationText = "Location: " + location;
                                textViewLocation.setText(locationText);
                            }
                        });

                        callAPI(latitude, longitude);

                        return true;
                    }
                });
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Check if input is not empty or whitespace
                if (!query.trim().isEmpty()) {
//                    Toast.makeText(MainActivity.this, query, Toast.LENGTH_SHORT).show();
                    getLocationLatLng(query);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        locateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "Locating...", Toast.LENGTH_SHORT).show();
                    // Get the current location and place a marker
                    getCurrentLocationAndSetMarker();
                } else {
                    // Request location permissions
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION);
                }
            }
        });

        showInfoPopupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

//                showPopup();
                showDialog();
            }
        });

        closeCardViewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                infoCardView.setVisibility(View.GONE);
            }
        });
    }

    private void showDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.info_popup_layout);

        // Set the dialog window properties
        if (dialog.getWindow() != null) {
            Window window = dialog.getWindow();

            // Set the dialog to match parent width
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            // Set gravity to top
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.TOP; // Position the dialog at the top
            window.setAttributes(params);

            // Remove the black background
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // Optional: Remove dim effect behind the dialog
//            params.dimAmount = 0.0f;
            window.setAttributes(params);
        }

        // Prevent the dialog from closing when touching outside
        dialog.setCanceledOnTouchOutside(false);

        // Set up the close button inside the dialog
        Button closeButton = dialog.findViewById(R.id.closePopupBtn);
        closeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }


    private void showPopup() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.info_popup_layout, null);

        // Initialize the PopupWindow
        popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                false
        );

        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Set an elevation if you are using API 21+ for shadow effect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popupWindow.setElevation(10);
        }

        // Ensure it does not close when clicking outside
        popupWindow.setOutsideTouchable(false);

        // Prevent the popup from being dismissed on back button press
        popupWindow.setFocusable(true);
        popupWindow.setTouchable(true);

        // Show the popup at the center of the screen
        popupWindow.showAtLocation(findViewById(R.id.main), Gravity.CENTER, 0, 0);

        Button closeButton = popupView.findViewById(R.id.closePopupBtn);
        closeButton.setOnClickListener(v -> popupWindow.dismiss());
    }


    private void getCurrentLocationAndSetMarker() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Check if location services are enabled
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Location services are turned off. Please turn them on.", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();

                        // Add marker and update camera
                        Point point = Point.fromLngLat(longitude, latitude);
                        addMarkerToMap(point.longitude(), point.latitude());

                        getLocationByLngLat(longitude, latitude, new LocationCallback() {
                            @Override
                            public void onLocationReceived(String location) {
                                Toast.makeText(MainActivity.this, location, Toast.LENGTH_SHORT).show();
                                String locationText = "Location: " + location;
                                textViewLocation.setText(locationText);
                            }
                        });

                        callAPI(latitude, longitude);

                        // Remove updates to avoid multiple calls
                        locationManager.removeUpdates(this);
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {}

                    @Override
                    public void onProviderEnabled(@NonNull String provider) {}

                    @Override
                    public void onProviderDisabled(@NonNull String provider) {}
                });
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    // Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocationAndSetMarker(); // Permission granted, get the current location
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void addMarkerToMap(double longitude, double latitude) {
        pointAnnotationManager.deleteAll();

        // Create a Point for the marker location
        Point point = Point.fromLngLat(longitude, latitude);

        // Create marker options
        PointAnnotationOptions pointAnnotationOptions = new PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(markerBitmap);

        // Add the marker to the map
        pointAnnotationManager.create(pointAnnotationOptions);

        // Center the map on the marker
        mapView.getMapboxMap().setCamera(
                new CameraOptions.Builder()
                        .center(Point.fromLngLat(point.longitude(), point.latitude() - 2.5))
//                        .center(point)
                        .zoom(5.0)
                        .build());

        Toast.makeText(MainActivity.this, "Latitude: " + latitude + "\n" + "Longitude: " + longitude, Toast.LENGTH_SHORT).show();
    }

    private void getLocationLatLng(String location){
        String apiKey = "pk.eyJ1IjoiZmVyZGVrIiwiYSI6ImNtM2c4aGRobTAxdWwycnNhaG9xajQ2c24ifQ.-QJgFtrEf7z6iorAU_KJjQ";
        String apiUrl = "https://api.mapbox.com/search/geocode/v6/forward?q=" + location + "&access_token=" + apiKey;
//        Log.e("String", apiUrl);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, apiUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray featuresArray = response.getJSONArray("features");
                    JSONObject firstFeature = featuresArray.getJSONObject(0);
                    JSONObject geometry = firstFeature.getJSONObject("geometry");
                    JSONArray coordinates = geometry.getJSONArray("coordinates");

                    double longitude = coordinates.getDouble(0);
                    double latitude = coordinates.getDouble(1);

                    addMarkerToMap(longitude, latitude);

                    getLocationByLngLat(longitude, latitude, new LocationCallback() {
                        @Override
                        public void onLocationReceived(String location) {
                            Toast.makeText(MainActivity.this, location, Toast.LENGTH_SHORT).show();
                            String locationText = "Location: " + location;
                            textViewLocation.setText(locationText);
                        }
                    });

                    callAPI(latitude, longitude);

                } catch (Exception e) {
                    Log.e("myError", e.toString());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, error.toString(), Toast.LENGTH_LONG).show();
                Log.e("myError", error.toString());
            }
        });
        Volley.newRequestQueue(this).add(jsonObjectRequest);
    }

    private void getLocationByLngLat(double longitude, double latitude, LocationCallback callback) {
        String apiKey = "pk.eyJ1IjoiZmVyZGVrIiwiYSI6ImNtM2c4aGRobTAxdWwycnNhaG9xajQ2c24ifQ.-QJgFtrEf7z6iorAU_KJjQ";
        String apiUrl = "https://api.mapbox.com/search/geocode/v6/reverse?longitude=" + longitude + "&latitude=" + latitude + "&access_token=" + apiKey;
//        Log.e("String", apiUrl);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, apiUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray featuresArray = response.getJSONArray("features");
                    JSONObject firstFeature = featuresArray.getJSONObject(0);
                    JSONObject properties = firstFeature.getJSONObject("properties");
                    JSONObject context = properties.getJSONObject("context");

                    JSONObject placeObject = context.has("place") && !context.isNull("place") ? context.getJSONObject("place") : null;
                    JSONObject regionObject = context.has("region") && !context.isNull("region") ? context.getJSONObject("region") : null;
                    JSONObject countryObject = context.has("country") && !context.isNull("country") ? context.getJSONObject("country") : null;

                    String place = (placeObject != null && placeObject.has("name") && !placeObject.isNull("name")) ? placeObject.getString("name") : "";
                    String region = (regionObject != null && regionObject.has("name") && !regionObject.isNull("name")) ? regionObject.getString("name") : "";
                    String country = (countryObject != null && countryObject.has("name") && !countryObject.isNull("name")) ? countryObject.getString("name") : "";

                    String locationText = "";
                    if (!place.isEmpty()) {
                        locationText += place;
                    }
                    if (!region.isEmpty()) {
                        locationText += (locationText.isEmpty() ? "" : ", ") + region;
                    }
                    if (!country.isEmpty()) {
                        locationText += (locationText.isEmpty() ? "" : ", ") + country;
                    }

                    // Pass the result to the callback
                    callback.onLocationReceived(locationText);

                } catch (Exception e) {
                    Log.e("myError", e.toString());
                    callback.onLocationReceived("");  // return empty if there's an error
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, error.toString(), Toast.LENGTH_LONG).show();
                Log.e("myError", error.toString());
                callback.onLocationReceived("");  // return empty on error
            }
        });

        Volley.newRequestQueue(this).add(jsonObjectRequest);
    }

    private void changeTextViewBackgroundBasedOnValuePM10(double value, TextView textView) {
        if (value <= 20) {
            textView.setBackground(new ColorDrawable(VERY_LOW_COLOR));
        } else if (value <= 40) {
            textView.setBackground(new ColorDrawable(LOW_COLOR));
        } else if (value <= 50) {
            textView.setBackground(new ColorDrawable(MEDIUM_COLOR));
        } else if (value <= 100) {
            textView.setBackground(new ColorDrawable(HIGH_COLOR));
        } else {
            textView.setBackground(new ColorDrawable(VERY_HIGH_COLOR));
        }
    }

    private void changeTextViewBackgroundBasedOnValuePM2_5(double value, TextView textView) {
        if (value <= 10) {
            textView.setBackground(new ColorDrawable(VERY_LOW_COLOR));
        } else if (value <= 20) {
            textView.setBackground(new ColorDrawable(LOW_COLOR));
        } else if (value <= 25) {
            textView.setBackground(new ColorDrawable(MEDIUM_COLOR));
        } else if (value <= 50) {
            textView.setBackground(new ColorDrawable(HIGH_COLOR));
        } else {
            textView.setBackground(new ColorDrawable(VERY_HIGH_COLOR));
        }
    }

    private void changeTextViewBackgroundBasedOnValueSO2(double value, TextView textView) {
        if (value <= 100) {
            textView.setBackground(new ColorDrawable(VERY_LOW_COLOR));
        } else if (value <= 200) {
            textView.setBackground(new ColorDrawable(LOW_COLOR));
        } else if (value <= 350) {
            textView.setBackground(new ColorDrawable(MEDIUM_COLOR));
        } else if (value <= 500) {
            textView.setBackground(new ColorDrawable(HIGH_COLOR));
        } else {
            textView.setBackground(new ColorDrawable(VERY_HIGH_COLOR));
        }
    }

    private void changeTextViewBackgroundBasedOnValueNO2(double value, TextView textView) {
        if (value <= 40) {
            textView.setBackground(new ColorDrawable(VERY_LOW_COLOR));
        } else if (value <= 90) {
            textView.setBackground(new ColorDrawable(LOW_COLOR));
        } else if (value <= 120) {
            textView.setBackground(new ColorDrawable(MEDIUM_COLOR));
        } else if (value <= 230) {
            textView.setBackground(new ColorDrawable(HIGH_COLOR));
        } else {
            textView.setBackground(new ColorDrawable(VERY_HIGH_COLOR));
        }
    }

    private void changeTextViewBackgroundBasedOnValueO3(double value, TextView textView) {
        if (value <= 50) {
            textView.setBackground(new ColorDrawable(VERY_LOW_COLOR));
        } else if (value <= 100) {
            textView.setBackground(new ColorDrawable(LOW_COLOR));
        } else if (value <= 130) {
            textView.setBackground(new ColorDrawable(MEDIUM_COLOR));
        } else if (value <= 240) {
            textView.setBackground(new ColorDrawable(HIGH_COLOR));
        } else {
            textView.setBackground(new ColorDrawable(VERY_HIGH_COLOR));
        }
    }

    private String convertTimestampToDateString(long timestamp) {
        // Multiply by 1000 to convert to milliseconds
        Date date = new Date(timestamp * 1000L);

        // Format Date as a readable string
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }

    private void callAPI(double latitude, double longitude){
        String apiKey = "f404bd5533b639ddc1bd855225cbffe5";
        String apiUrl = "https://api.openweathermap.org/data/2.5/air_pollution?lat=" + latitude + "&lon=" + longitude + "&appid=" + apiKey;
//        Log.e("String", apiUrl);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, apiUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray listArray = response.getJSONArray("list");
                    JSONObject firstItem = listArray.getJSONObject(0);
                    JSONObject components = firstItem.getJSONObject("components");
                    long timestamp = firstItem.getLong("dt");

                    double pm10 = components.getDouble("pm10");
                    double pm2_5 = components.getDouble("pm2_5");
                    double so2 = components.getDouble("so2");
                    double no2 = components.getDouble("no2");
                    double o3 = components.getDouble("o3");
                    double co = components.getDouble("co");
                    double nh3 = components.getDouble("nh3");
                    double no = components.getDouble("no");

                    String lastUpdateTimeText = "Updated: " + convertTimestampToDateString(timestamp);
                    textViewLastUpdateTime.setText(lastUpdateTimeText);

                    changeTextViewBackgroundBasedOnValuePM10(pm10, textViewPM10);
                    changeTextViewBackgroundBasedOnValuePM2_5(pm2_5, textViewPM2_5);
                    changeTextViewBackgroundBasedOnValueSO2(so2, textViewSO2);
                    changeTextViewBackgroundBasedOnValueNO2(no2, textViewNO2);
                    changeTextViewBackgroundBasedOnValueO3(o3, textViewO3);

                    textViewPM10.setText(Html.fromHtml("PM<sub>10</sub>: " + pm10 + " μg/m<sup>3</sup>", Html.FROM_HTML_MODE_COMPACT));
                    textViewPM2_5.setText(Html.fromHtml("PM<sub>2.5</sub>: " + pm2_5 + " μg/m<sup>3</sup>", Html.FROM_HTML_MODE_COMPACT));
                    textViewSO2.setText(Html.fromHtml("SO<sub>2</sub>: " + so2 + " μg/m<sup>3</sup>", Html.FROM_HTML_MODE_COMPACT));
                    textViewNO2.setText(Html.fromHtml("NO<sub>2</sub>: " + no2 + " μg/m<sup>3</sup>", Html.FROM_HTML_MODE_COMPACT));
                    textViewO3.setText(Html.fromHtml("O<sub>3</sub>: " + o3 + " μg/m<sup>3</sup>", Html.FROM_HTML_MODE_COMPACT));
                    textViewCO.setText(Html.fromHtml("CO: " + co + " μg/m<sup>3</sup>", Html.FROM_HTML_MODE_COMPACT));
                    textViewNH3.setText(Html.fromHtml("NH<sub>3</sub>: " + nh3 + " μg/m<sup>3</sup>", Html.FROM_HTML_MODE_COMPACT));
                    textViewNO.setText(Html.fromHtml("NO: " + no + " μg/m<sup>3</sup>", Html.FROM_HTML_MODE_COMPACT));

                    infoCardView.setVisibility(View.VISIBLE);

                } catch (Exception e) {
                    Log.e("myError", e.toString());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, error.toString(), Toast.LENGTH_LONG).show();
                Log.e("myError", error.toString());
            }
        });
        Volley.newRequestQueue(this).add(jsonObjectRequest);
    }
}