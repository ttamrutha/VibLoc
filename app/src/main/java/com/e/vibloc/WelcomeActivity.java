package com.e.vibloc;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                isLoggedIn();
            }
        },3000);
    }

    private void isLoggedIn(){
        startActivity(new Intent(WelcomeActivity.this,MainActivity.class));
        finish();
    }
}
