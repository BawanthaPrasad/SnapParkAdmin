package com.example.snapparkadmin;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;





public class report extends AppCompatActivity implements OnMapReadyCallback{

    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 102;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private EditText etVehicleNo;
    private Button btnReportV;
    private Button btnReport;
    private ImageView cameraPreview;
    private GoogleMap mMap;
    private Marker selectedMarker;
    private LatLng selectedLatLng;
    private LatLng currentLocation;
    private FusedLocationProviderClient fusedLocationProviderClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        etVehicleNo = findViewById(R.id.etVehicle);
        btnReportV = findViewById(R.id.btnReportV);
        btnReport = findViewById(R.id.btnReport);
        cameraPreview = findViewById(R.id.cameraPreview);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        btnReportV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        btnReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedLatLng == null) {
                    Toast.makeText(report.this, "Please select a location on the map", Toast.LENGTH_SHORT).show();
                    return;
                }

                String vehicleNo = etVehicleNo.getText().toString().trim();
                if (vehicleNo.isEmpty()) {
                    Toast.makeText(report.this, "Please enter vehicle number", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Get current date and time
                Date currentDate = Calendar.getInstance().getTime();

                // Create a Firestore document with the required data
                Map<String, Object> reportData = new HashMap<>();
                reportData.put("vehicleNo", vehicleNo);
                reportData.put("latitude", selectedLatLng.latitude);
                reportData.put("longitude", selectedLatLng.longitude);
                reportData.put("timestamp", currentDate);

                // Add the document to Firestore
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("reports")
                        .add(reportData)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Toast.makeText(report.this, "Report sent successfully", Toast.LENGTH_SHORT).show();
                                // Clear the vehicle number field after sending the report
                                etVehicleNo.setText("");
                                cameraPreview.setImageDrawable(null);
                                // Clear the selected location
                                selectedLatLng = null;
                                if (selectedMarker != null) {
                                    selectedMarker.remove();
                                }
                                // Search for the relevant vehicle number in Firestore
                                db.collection("users")
                                        .whereEqualTo("vehicleNo", vehicleNo)
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                                        String deviceToken = document.getString("deviceToken");
                                                        String email = document.getString("email");
                                                        Toast.makeText(report.this, "User found", Toast.LENGTH_SHORT).show();
                                                        // Send illegal park notification to the relevant device token
                                                        sendNotification(deviceToken);
                                                        sendEmail(email);
                                                    }
                                                } else {
                                                    Log.d(TAG, "Error getting documents: ", task.getException());
                                                }
                                            }
                                        });
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e("Firestore", "Error adding document", e);
                                Toast.makeText(report.this, "Failed to send report", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });



    }

    private void sendNotification(String deviceToken) {
        RemoteMessage.Builder builder = new RemoteMessage.Builder(deviceToken);
        builder.addData("score", "850");
        builder.addData("time", "2:45");

        FirebaseMessaging.getInstance().send(builder.build());
    }

    private void sendEmail(String email) {
        String[] recipients = {email}; // Email address where you want to send the email
        String subject = "SnapPark : Illegal Parking Location";
        String body = "You Have parked on a illegal parking location.Check the Application.";

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_EMAIL, recipients);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);
        intent.setType("message/rfc822"); // Specifies that the intent should only handle email type data
        startActivity(Intent.createChooser(intent, "Choose an email client"));
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Check location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Request location updates
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15)); // Zoom level 15
                }
            });
        } else {
            // Request permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }

        // Set up map click listener
        mMap.setOnMapClickListener(latLng -> {
            // Update marker position
            if (selectedMarker != null) {
                selectedMarker.remove();
            }
            selectedMarker = mMap.addMarker(new MarkerOptions().position(latLng).draggable(true));
            selectedLatLng = latLng; // Update selectedLatLng
        });

        // Set up marker drag listener
        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
                // Do nothing
            }

            @Override
            public void onMarkerDrag(Marker marker) {
                // Do nothing
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                // Update selected marker position
                selectedMarker = marker;
                selectedLatLng = marker.getPosition(); // Update selectedLatLng
            }
        });
    }




    private void dispatchTakePictureIntent() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            // Permission has already been granted
            startCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start the camera
                startCamera();
            } else {
                // Permission denied
                Toast.makeText(this, "Camera permission is required to capture images", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, move camera to current location
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
                        if (location != null) {
                            currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15)); // Zoom level 15
                        }
                    });
                }
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            cameraPreview.setImageBitmap(imageBitmap);
            detectTextFromImage(imageBitmap);
        }
    }

    private void detectTextFromImage(Bitmap imageBitmap) {
        InputImage image = InputImage.fromBitmap(imageBitmap, 0);
        // Create an instance of TextRecognizerOptions and set default options
        TextRecognizerOptions options = TextRecognizerOptions.DEFAULT_OPTIONS;

        // Get TextRecognizer instance with the specified options
        TextRecognizer recognizer = TextRecognition.getClient(options);
        recognizer.process(image)
                .addOnSuccessListener(new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text visionText) {
                        String numberPlateText = extractNumberPlateText(visionText.getText());
                        if (numberPlateText != null) {
                            Log.d("Text Recognition", "Detected number plate text: " + numberPlateText);
                            etVehicleNo.setText(numberPlateText);
                        } else {
                            Log.d("Text Recognition", "No number plate text detected");
                            Toast.makeText(report.this, "No number plate text detected", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("Text Recognition", "Text detection failed: " + e.getMessage());
                        Toast.makeText(report.this, "Text detection failed", Toast.LENGTH_SHORT).show();
                    }
                });

    }
    private String extractNumberPlateText(String fullText) {
        // Regular expression to match number plate formats
        String regex = "[A-Za-z]{1,3}\\s?[-]?\\s?\\d{1,4}"; // Matches formats like "ABC 123", "AB 1234", "ABC-1234"

        // Compile the pattern
        Pattern pattern = Pattern.compile(regex);

        // Create a matcher for the input text
        Matcher matcher = pattern.matcher(fullText);

        // List to store all matched number plates
        List<String> matchedNumberPlates = new ArrayList<>();

        // Find all matches and add them to the list
        while (matcher.find()) {
            matchedNumberPlates.add(matcher.group().replaceAll("\\s", "").replaceAll("-", ""));
        }

        // If no matches found, return null
        if (matchedNumberPlates.isEmpty()) {
            return null;
        }

        // Find the most common match (assuming the correct plate will be detected multiple times)
        return findMostCommonMatch(matchedNumberPlates);
    }

    private String findMostCommonMatch(List<String> matches) {
        Map<String, Integer> frequencyMap = new HashMap<>();

        // Count the frequency of each match
        for (String match : matches) {
            frequencyMap.put(match, frequencyMap.getOrDefault(match, 0) + 1);
        }

        // Find the match with the highest frequency
        String mostCommonMatch = null;
        int maxFrequency = 0;

        for (Map.Entry<String, Integer> entry : frequencyMap.entrySet()) {
            if (entry.getValue() > maxFrequency) {
                mostCommonMatch = entry.getKey();
                maxFrequency = entry.getValue();
            }
        }

        return mostCommonMatch;
    }


}
