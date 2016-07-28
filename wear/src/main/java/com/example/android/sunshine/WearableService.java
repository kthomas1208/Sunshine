package com.example.android.sunshine;

import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.WearableListenerService;

public class WearableService extends WearableListenerService {

    final String LOG_TAG = WearableService.class.getSimpleName();

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent dataEvent : dataEvents) {
            Log.d(LOG_TAG, "DATA IS HERE: " + dataEvent.toString());
        }
    }
}
