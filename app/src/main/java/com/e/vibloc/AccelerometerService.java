package com.e.vibloc;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class AccelerometerService extends Service implements GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks, LocationListener, SensorEventListener, MqttCallback {
    final String TAG = AccelerometerService.class.getSimpleName();
    public static Context context;

    private static final int UPDATE_INTERVAL = 2000;
    private static final int FASTEST_INTERVAL = 1000;
    private static final int DISPLACEMENT = 100;

    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;

    public static double latitude, longitude;
    public static boolean isServiceStarted = false;
    double x,y,z,fallThreshold;

    private SensorManager sensorManager;
    private Sensor sensor;

    private MqttAndroidClient client;
    boolean isConnected;

    public static final String MQTT_PORT = "1883";
    //public static final String MQTT_IP = "18.188.213.251";
    public static final String MQTT_IP = "broker.hivemq.com";
    public static final String TOPIC = "demo";

    @SuppressLint("RestrictedApi")
    public AccelerometerService() {
        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(context, "tcp://"+MQTT_IP+":"+MQTT_PORT,
                clientId);
        Log.e(TAG,client.getServerURI());

        googleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        // Toast.makeText(context, "GoogleApiClient Built", Toast.LENGTH_SHORT).show();
        locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setSmallestDisplacement(DISPLACEMENT);
        if (googleApiClient != null) {
            googleApiClient.connect();
            // Toast.makeText(context, "Requested for GoogleApiClient connection", Toast.LENGTH_SHORT).show();
        }
        isServiceStarted = true;

       sensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
       sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

       sensorManager.registerListener(this,sensor, SensorManager.SENSOR_DELAY_NORMAL);

       connect();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isServiceStarted = true;
        Toast.makeText(this, "Service Started", Toast.LENGTH_SHORT).show();
        return super.onStartCommand(intent, START_FLAG_RETRY, startId);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(context, "Failed to Connect to GoogelApiClient", Toast.LENGTH_SHORT).show();
        googleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Toast.makeText(context, "Connected to GoogelApiClient", Toast.LENGTH_SHORT).show();
        sendLocationRequest();
        Toast.makeText(context, "Location Request Sent", Toast.LENGTH_SHORT).show();
    }

    private void sendLocationRequest() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        googleApiClient.connect();
        Toast.makeText(context, "Suspended Connection to GoogelApiClient", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        Toast.makeText(context, latitude+","+longitude, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
         x = event.values[0];
         y = event.values[1];
         z = event.values[2];

         fallThreshold = Math.sqrt((double) (((x * x) + (y * y) + (z * z))));

         if(fallThreshold < 1.0){
             Toast.makeText(context, ""+fallThreshold, Toast.LENGTH_SHORT).show();
             String msg = latitude+","+longitude+","+fallThreshold;
             publishStatusToMqtt(TOPIC,msg,false);
         }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void connectionLost(Throwable cause) {

    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

    private void connect(){
        if (client != null){
            try {
                IMqttToken token = client.connect();
                token.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        // We are connected
                        Log.d(TAG, "onSuccess");
                        isConnected = true;
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        // Something went wrong e.g. connection timeout or firewall problems
                        Log.d(TAG, "onFailure "+exception);
                        isConnected = false;
                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    private void publishStatusToMqtt(String topic, String msg, boolean isRetained) {
        if(isConnected){
            byte[] encodedPayload = new byte[0];
            try {
                if(!msg.equals("")) {
                    encodedPayload = msg.getBytes("UTF-8");
                }
                MqttMessage message = new MqttMessage(encodedPayload);
                message.setRetained(isRetained);
                client.publish(topic, message);
                Toast.makeText(context, "Message Sent "+msg, Toast.LENGTH_SHORT).show();
            } catch (UnsupportedEncodingException | MqttException e) {
                Log.e(TAG,e.getMessage()+"");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(googleApiClient != null){
            googleApiClient.disconnect();
        }
        sensorManager.unregisterListener(this);
        isServiceStarted = false;
        Toast.makeText(AccelerometerService.this, "service stopped", Toast.LENGTH_SHORT).show();
    }
}

