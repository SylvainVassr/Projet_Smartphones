package fr.univ_lr.drone_controller;


import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class Vue1Activity extends AppCompatActivity implements OnMapReadyCallback {

    private Socket socket;
    private BufferedReader reader;

    private static final int SERVERPORT = 55555;
    private static final String SERVER_IP = "10.0.2.2";

    private Button toView2;
    private Button toView3;
    private TextView vitesse;
    private TextView infosDiverses; // Vitesse bateau, vent, direction vent + long et lat bateau
    private GoogleMap gmap;
    ArrayList <LatLng> pts = new ArrayList<>();
    int index = 0;
    int i;
    float lat = 0.0f;
    float lon = 0.0f;
    float directionbateau = 0.0f;
    float vitessebateau = 0.0f;
    float directionvent = 0.0f;
    float vitessevent = 0.0f;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vue1);

        new Thread(new ClientThread()).start();

        this.toView2 = (Button) findViewById(R.id.toView2);
        this.toView3 = (Button) findViewById(R.id.toView3);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView);

        mapFragment.getMapAsync(this);

        this.toView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Vue1Activity.this, Vue2Activity.class);
                startActivity(i);
                finish();
            }
        });

        this.toView3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Vue1Activity.this, Vue3Activity.class);
                startActivity(i);
                finish();
            }
        });

    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.gmap = map;
        LatLng loc = new LatLng(46.1481759,-1.1694211);
        gmap.moveCamera(CameraUpdateFactory.newLatLng(loc));
    }

    public void printLine() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (pts.size() > 1) {
                    for (int i = 0; i < pts.size() - 1; i++) {
                        if (pts.get(i + 1) != null) {
                            //Log.v("listCoord lat: ", String.valueOf(listCoord.get(i).latitude));
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

    float Latitude2Decimal(String lat, String NS) {//trans latitude envoyer par nmea en float
        float med = Float.parseFloat(lat.substring(2)) / 60.0f;
        med += Float.parseFloat(lat.substring(0, 2));
        if (NS.startsWith("S")) {
            med = -med;
        }
        return med;
    }

    float Longitude2Decimal(String lon, String WE) {//trans longitude envoyer par nmea en float
        float med = Float.parseFloat(lon.substring(3)) / 60.0f;
        med += Float.parseFloat(lon.substring(0, 3));
        if (WE.startsWith("W")) {
            med = -med;
        }
        return med;
    }


    void parseur(String line) {//parse les donne
        //System.out.println(line);
        //System.out.println(line.substring(1, 6));
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



                LatLng boatLatLng = new LatLng(lat, lon);


                pts.add(boatLatLng);
                //printLine();

                i++;
            }
            if (line.substring(1, 6).equals("WIMWD")) {

                System.out.println("WIMWD");
                System.out.println("vitesse vent: " + Float.parseFloat(tokens[5]) + "knots, direction du vent: " + Float.parseFloat(tokens[1]) + "°;");
                vitessevent = Float.parseFloat(tokens[5]);
                directionvent = Float.parseFloat(tokens[1]);


            }


            vitesse.setText("" + vitessebateau + " knots");
            infosDiverses.setText("Lat : " + lat + "Long : " + lon +"\n Vitesse vent : " + vitessevent + " knots\n Direction vent : " + directionvent + " °");
        }
        //System.out.println("nmea sentence"+tokens[0]);
    }

    class ClientThread implements Runnable {

        @Override
        public void run() {

            try {
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                socket = new Socket(serverAddr, SERVERPORT);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));//lie les donnees envoyer par nmea simulator
                String line;
                while ((line = reader.readLine()) != null) {//boucle pour lire toute les ligne jusqu'a que nmea arrete de transmetre
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