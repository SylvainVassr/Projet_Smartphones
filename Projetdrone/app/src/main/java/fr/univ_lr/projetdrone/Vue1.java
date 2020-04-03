package fr.univ_lr.projetdrone;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

;

public class Vue1 extends AppCompatActivity implements OnMapReadyCallback {

    private Socket socket;

    private static final int SERVERPORT = 55555;
    private static final String SERVER_IP = "10.0.2.2";

    private Button versVue2;
    private Button versVue3;
    private GoogleMap gmap;
    ArrayList <LatLng> pts = new ArrayList<>();
    float lat = 0.0f;
    float lon = 0.0f;
    float directionbateau = 0.0f;
    float vitessebateau = 0.0f;
    float directionvent = 0.0f;
    float vitessevent = 0.0f;
    int i=0;
    TextView txt_lat;
    TextView txt_lon;
    TextView txt_vtsb;
    TextView txt_vtsv;
    TextView txt_dirv;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vue1);

        new Thread(new ClientThread()).start();

        this.versVue2 = (Button) findViewById(R.id.versVue2);
        this.versVue3 = (Button) findViewById(R.id.versVue3);
        this.txt_lat = (TextView) findViewById(R.id.txt_lat);
        this.txt_lon = (TextView) findViewById(R.id.txt_lon);
        this.txt_vtsb = (TextView) findViewById(R.id.vitesse);
        this.txt_vtsv = (TextView) findViewById(R.id.txt_vtsv);
        this.txt_dirv = (TextView) findViewById(R.id.txt_dirv);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView);

        mapFragment.getMapAsync(this);

        this.versVue2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Vue1.this, Vue2.class);
                startActivity(i);
                finish();
            }
        });

        this.versVue3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Vue1.this, Vue3.class);
                startActivity(i);
                finish();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.gmap = map;

        LatLng loc = new LatLng(46.1481759,-1.1694211);
        this.gmap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc,15));    }

    public void printLine() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (pts.size() > 1) {
                    for (int i = 0; i < pts.size() - 1; i++) {
                        if (pts.get(i + 1) != null) {
                            LatLng pt1 = pts.get(i);
                            LatLng pt2 = pts.get(i + 1);

                            gmap.addPolyline(new PolylineOptions().add(pt1, pt2).width(5).color(Color.BLACK));
                            gmap.moveCamera(CameraUpdateFactory.newLatLng(pt2));
                        }
                    }
                    gmap.addMarker(new MarkerOptions().position(pts.get(0)).title("Point de départ"));
                }
            }
        });
    }

    float Latitude2Decimal(String lat, String NS) {//transmission latitude envoyé par nmea en float
        float med = Float.parseFloat(lat.substring(2)) / 60.0f;
        med += Float.parseFloat(lat.substring(0, 2));
        if (NS.startsWith("S")) {
            med = -med;
        }
        return med;
    }

    float Longitude2Decimal(String lon, String WE) {//transmission longitude envoyé par nmea en float
        float med = Float.parseFloat(lon.substring(3)) / 60.0f;
        med += Float.parseFloat(lon.substring(0, 3));
        if (WE.startsWith("W")) {
            med = -med;
        }
        return med;
    }


    void parseur(String line) {//parse les donne
        if (line.startsWith("$")) {
            String[] tokens = line.split(",");
            String type = tokens[0];

            if (line.substring(1, 6).equals("GPRMC")) {

                System.out.println("GPRMC");
                System.out.println("lat: " + Latitude2Decimal(tokens[3], tokens[4]) + ", long: " + Longitude2Decimal(tokens[3], tokens[4]) + ", vitesse: " + Float.parseFloat(tokens[7]) + "knots, direction: " + Float.parseFloat(tokens[8]) + "°;");
                lat = Latitude2Decimal(tokens[3], tokens[4]);
                lon = Longitude2Decimal(tokens[5], tokens[6]);
                vitessebateau = Float.parseFloat(tokens[7]);
                directionbateau = Float.parseFloat(tokens[8]);

                txt_lat.setText("Latitude :" + lat);
                txt_lon.setText("Longitude :" + lon);
                txt_vtsb.setText("Vitesse du bateau :" + vitessebateau + " knots");

                LatLng boatLatLng = new LatLng(lat, lon);


                pts.add(boatLatLng);
                printLine();

                i++;
            }
            if (line.substring(1, 6).equals("WIMWD")) {

                System.out.println("WIMWD");
                System.out.println("vitesse vent: " + Float.parseFloat(tokens[5]) + "knots, direction du vent: " + Float.parseFloat(tokens[1]) + "°;");
                vitessevent = Float.parseFloat(tokens[5]);
                directionvent = Float.parseFloat(tokens[1]);

                txt_vtsv.setText("Vitesse du vent : " + vitessevent + " knots");
                txt_dirv.setText("Direction du vent : " + directionvent + " °");


            }

        }
    }

    class ClientThread implements Runnable {

        @Override
        public void run() {

            try {
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                socket = new Socket(serverAddr, SERVERPORT);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));//lie les donnees envoyé par nmea simulator
                String line;
                while ((line = reader.readLine()) != null) {//boucle pour lire toute les lignes jusqu'a que nmea arrete de transmetre
                    //parse les donnees
                    parseur(line);
                }
            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        }
    }
}