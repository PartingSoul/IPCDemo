package com.parting_soul.ipcdemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClick(View view) {
        Class<?> clazz = null;

        switch (view.getId()) {
            case R.id.bt_aidl:
                clazz = AIDLActivity.class;
                break;
            case R.id.bt_messenger:
                clazz = MessengerActivity.class;
                break;
            default:
                break;
        }

        if (clazz != null) {
            Intent intent = new Intent(this, clazz);
            startActivity(intent);
        }

    }
}
