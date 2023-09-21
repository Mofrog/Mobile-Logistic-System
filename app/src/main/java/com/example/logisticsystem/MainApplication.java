package com.example.logisticsystem;

import android.app.Application;
import com.yandex.mapkit.MapKitFactory;

public class MainApplication extends Application {
    private final String MAPKIT_API_KEY = "2d74dbb1-81d7-4f8d-8194-9d5c6a539d8d";

    @Override
    public void onCreate() {
        super.onCreate();
        // Set the api key before calling initialize on MapKitFactory.
        MapKitFactory.setApiKey(MAPKIT_API_KEY);
    }
}