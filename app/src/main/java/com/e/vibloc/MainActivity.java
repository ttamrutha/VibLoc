package com.e.vibloc;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;

import com.google.android.material.snackbar.Snackbar;
import com.skyfishjy.library.RippleBackground;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    CoordinatorLayout coordinatorLayout;
    Button button_start, button_stop;
     RippleBackground rippleBackground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        coordinatorLayout = findViewById(R.id.coordinatorLayout);

        if(!checkPermission()){
            requestPermission();
        }

        button_start = findViewById(R.id.button_start);
        button_stop = findViewById(R.id.button_stop);

        button_start.setOnClickListener(this);
        button_stop.setOnClickListener(this);
        rippleBackground=(RippleBackground)findViewById(R.id.content);

    }

    public  boolean checkPermission() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            return false;
        }
        return true;
    }

    public  void requestPermission() {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Snackbar.make(coordinatorLayout, "Location access permission granted",
                            Snackbar.LENGTH_SHORT).show();
                }
        }
    }

    @Override
    public void onClick(View v) {
        if(v == button_start){
            if(!AccelerometerService.isServiceStarted)
            {
                rippleBackground.startRippleAnimation();
                AccelerometerService.context = this;
                startService(new Intent(this,AccelerometerService.class));
                //Toast.makeText(this, "Service Started", Toast.LENGTH_SHORT).show();
            }

        }
        else{
            rippleBackground.stopRippleAnimation();

            stopService(new Intent(this,AccelerometerService.class));
            //Toast.makeText(this, "Service Stoped", Toast.LENGTH_SHORT).show();
        }
    }
}
