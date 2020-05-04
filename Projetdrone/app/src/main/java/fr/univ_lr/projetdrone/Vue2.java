package fr.univ_lr.projetdrone;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class Vue2 extends AppCompatActivity implements OnMapReadyCallback, SensorEventListener {

    private GoogleMap gmap;
    private double xOrigin,yOrigin;
    private LatLng location;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vue2);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        Button toView1 = (Button) findViewById(R.id.toView1);
        Button toView3 = (Button) findViewById(R.id.toView3);
        Button home = (Button) findViewById(R.id.buttonHome);
        Button urgence = (Button) findViewById(R.id.buttonEmergency);

        this.xOrigin = 7.2;
        this.yOrigin = -0.35;

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView2);

        mapFragment.getMapAsync(this);

        toView1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Vue2.this, Vue1.class);
                startActivity(i);
                finish();
            }
        });

        toView3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Vue2.this, Vue3.class);
                startActivity(i);
                finish();
            }
        });

        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast toast = Toast.makeText(getApplicationContext(), "Home button pushed", Toast.LENGTH_SHORT); toast.show();
                updateLocation(new LatLng(46.1481759,-1.1694211));
            }
        });

        urgence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast toast = Toast.makeText(getApplicationContext(), "Emergency button pushed", Toast.LENGTH_SHORT); toast.show();
            }
        });

    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.gmap = map;

        LatLng loc = new LatLng(46.1481759,-1.1694211);
        this.gmap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc,15));

        this.location = loc;
        gmap.addMarker(new MarkerOptions().position(this.location));

        // Gestionnaire de capteurs, dans ce cas si, l'Acceleromètre
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        assert sensorManager != null;
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);

    }

    /**
     * Fonction appelée à la fin du traitement du capteur afin de modifier la position du drone sur la carte en fonction
     * du nouvel attribut this.location
     */
    private void updateLocation(LatLng loc) {
        BitmapDrawable bitmapdraw=(BitmapDrawable) getResources().getDrawable(R.drawable.boat);
        Bitmap b=bitmapdraw.getBitmap();
        Bitmap smallMarker = Bitmap.createScaledBitmap(b, 80, 80, false);

        this.location = loc;
        gmap.clear();
        gmap.addMarker(new MarkerOptions().position(location).icon(BitmapDescriptorFactory.fromBitmap(smallMarker)));

    }

    /**
     * Fonction appelée lors d'un changement détecté sur les capteurs
     * @param event contient les coordonnées à la suite du changement sur le capteur
     *              event.values[0] > axe X
     *              event.values[1] > axe Y
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        double tolerance = 1;   // valeur de tolérance pour ne pas prendre en compte les micros-mouvements du capteur

        double deplacement = 0.0003;    // valeur qui sera ajouté à la longitude ou à la latitude pour effectuer le déplacement

        LatLng newLoc = this.location;
        double newX = event.values[0];
        double newY = event.values[1];

        if(Math.abs( this.xOrigin - newX ) > tolerance) {
            if(newX > this.xOrigin) {
                newLoc = new LatLng(newLoc.longitude - deplacement, newLoc.latitude);  // RECULE
            }
            else {
                newLoc = new LatLng(newLoc.longitude + deplacement , newLoc.latitude);  // AVANCE
            }
        }

        if(Math.abs( this.yOrigin - newY ) > tolerance) {
            if(newY > this.yOrigin) {
                newLoc = new LatLng(newLoc.longitude,newLoc.latitude + deplacement);  // DROITE
            }
            else {
                newLoc = new LatLng(newLoc.longitude,newLoc.latitude - deplacement);  // GAUCHE
            }
        }

        updateLocation(newLoc); // mise à jour de la position du marker

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
