package fr.univ_lr.projetdrone;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;

public class Vue3 extends AppCompatActivity implements OnMapReadyCallback {

    private Button toView1;
    private Button toView2;
    private GoogleMap gmap;
    private ArrayList<LatLng> waypoints;
    private TextView ptCoords;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vue3);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        this.toView1 = (Button) findViewById(R.id.toView1);
        this.toView2 = (Button) findViewById(R.id.toView2);
        this.waypoints = new ArrayList<>();
        this.ptCoords = (TextView) findViewById(R.id.textCoordsPoint);

        Button clear = (Button) findViewById(R.id.buttonClear);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView3);
        mapFragment.getMapAsync(this);

        this.toView1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Vue3.this, Vue1.class);
                startActivity(i);
                finish();
            }
        });
        this.toView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Vue3.this, Vue2.class);
                startActivity(i);
                finish();
            }
        });

        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clear();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.gmap = map;
        LatLng loc = new LatLng(46.1481759, -1.1694211);
        this.gmap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 15));

        // Gère l'ajout de waypoints(markers) sur la carte pour le tracé
        this.gmap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                gmap.addMarker(new MarkerOptions().position(point));
                addPoint(point);
            }
        });
    }

    /**
     * Ajoute 'point' dans l'ArrayList de points, et change le texte du TextView, affichant les coordonnées du nouveau point.
     *
     * @param point, le point à ajouter dans l'ArrayList de waypoints
     */
    private void addPoint(LatLng point) {
        this.waypoints.add(point);
        double longitude = point.longitude;
        double latitude = point.latitude;
        this.ptCoords.setText(String.format("Longitude :\n %s\nLatitude :\n %s", longitude, latitude));
        drawLines();
    }

    /**
     * Parcours l'ArrayList de points et affiche un tracé entre les points i et i+1, i+1 et i+2, etc...
     * Puis lance la transformation de ces points en trames NMEA.
     */
    private void drawLines() {

        for (int i = 0; i < this.waypoints.size() - 1; i++) {
            LatLng pt1 = this.waypoints.get(i);
            LatLng pt2 = this.waypoints.get(i + 1);

            this.gmap.addPolyline(new PolylineOptions()
                    .add(pt1, pt2)
                    .width(5)
                    .color(Color.BLACK));
        }

        transformIntoNMEA(this.waypoints.get(this.waypoints.size() - 1));
    }

    /**
     * Vide la carte de tout points et/ou tracés.
     */
    private void clear() {
        this.gmap.clear();
        this.waypoints = new ArrayList<>();
        this.ptCoords.setText("");
    }

    /**
     * Transforme les waypoints de l'utilisateur par des trames NMEA
     */

    private void transformIntoNMEA(LatLng point) {


        String type = "GPRMC";
        Date d = new Date();
        SimpleDateFormat f = new SimpleDateFormat("HHmmss");
        String s = f.format(d);

        String time = s;

        String lat = convertIntoDMS(point.latitude);
        String latIndic;
        if (point.latitude < 0)
            latIndic = "S";
        else
            latIndic = "N";

        String lon = convertIntoDMS(point.longitude);
        String lonIndic;
        if (point.longitude < 0)
            lonIndic = "E";
        else
            lonIndic = "W";

        String speed = "30";
        String sum = "E";

        String trame = String.format("%s,,%s,%s,%s,%s,%s,,090420,,%s", time, lat, latIndic, lon, lonIndic, speed, sum);
        // date 09 avril 2020

        trame = ("$") + type + "," + trame + ("*") + getChecksum(trame);

        Log.d("NMEA", trame);
    }


    /**
     * Génère un code hexadecimal qui validera la trame NMEA
     *
     * @param in La chaîne de caractères à valider en CRC
     * @return String sous forme hexadécimale
     */
    private static String getChecksum(String in) {
        int checksum = 0;

        int end = in.indexOf('*');
        if (end == -1)
            end = in.length();
        for (int i = 0; i < end; i++) {
            checksum = checksum ^ in.charAt(i);
        }
        String hex = Integer.toHexString(checksum);
        if (hex.length() == 1)
            hex = "0" + hex;
        return hex.toUpperCase();
    }

    /**
     * Transforme des coordonnées sous format latitude/longitude en format DMS
     * Exemple : 40.7600000,-73.984000040°      --->     45' 36.000" N  73° 59' 2.400" W
     *
     * @param coord, les coordonnées à convertir en format DMS
     * @return
     */
    private static String convertIntoDMS(double coordonnees) {
        double coord = Math.abs(coordonnees); // SECURITE
        int coordInteger = (int) coord; // renommer degre
        double coordDecimal = coord - coordInteger;
        String partieEntierePreCalcul;

        if (coordInteger < 10) // on rajoute un 0 pour faire 08
            partieEntierePreCalcul = "0" + coordInteger;
        else // > 10, pas besoin de 0
            partieEntierePreCalcul = String.valueOf(coordInteger);

        coordDecimal *= 60;

        int coordInteger2 = (int) coordDecimal; // minutes en int
        double coordDecimal2 = coordDecimal - coordInteger2;

        // minutes en string
        String partieDecimalePostCalcul = Double.toString(coordDecimal2); // partieDecimalePostCalcul en minutes

        String mmmm = String.valueOf(partieDecimalePostCalcul.charAt(2)) +
                partieDecimalePostCalcul.charAt(3) +
                partieDecimalePostCalcul.charAt(4) +
                partieDecimalePostCalcul.charAt(5);

        String partieEntierePostCalcul;

        if (coordInteger2 < 10) // on rajoute un 0 pour faire 08
            partieEntierePostCalcul = "0" + coordInteger2;
        else // > 10, pas besoin de 0
            partieEntierePostCalcul = String.valueOf(coordInteger2);

        String ddmm = "";

        if (coordonnees < 0)
            ddmm = "-" + partieEntierePreCalcul + partieEntierePostCalcul;
        else
            ddmm = partieEntierePreCalcul + partieEntierePostCalcul;


        return ddmm + "." + mmmm;

    }

}