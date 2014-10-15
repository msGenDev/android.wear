//package com.example.liutingdu.testwear;
//
//import android.net.Uri;
//import android.util.Log;
//
//import com.google.android.gms.common.ConnectionResult;
//import com.google.android.gms.common.api.GoogleApiClient;
//import com.google.android.gms.common.data.FreezableUtils;
//import com.google.android.gms.wearable.DataEvent;
//import com.google.android.gms.wearable.DataEventBuffer;
//import com.google.android.gms.wearable.MessageEvent;
//import com.google.android.gms.wearable.Wearable;
//import com.google.android.gms.wearable.WearableListenerService;
//
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//
//public class DataLayerListenerService extends WearableListenerService {
//
//    private static final String TAG = "DataLayerListenerService";
////    private static final String START_ACTIVITY_PATH = "/start-activity";
////    private static final String DATA_ITEM_RECEIVED_PATH = "/data-item-received";
//    public final String REQUEST_SNAPSHOT_PATH = "/request/snapshot";
//
//    @Override
//    public void onDataChanged(DataEventBuffer dataEvents) {
//        if (Log.isLoggable(TAG, Log.DEBUG)) {
//            Log.d(TAG, "onDataChanged: " + dataEvents);
//        }
//        }
//
//
//    @Override
//    public void onMessageReceived(MessageEvent messageEvent) {
//        Log.d(TAG, "onMessageReceived");
//        if (messageEvent.getPath().equals(REQUEST_SNAPSHOT_PATH)) {
//            Log.d(TAG,new String(messageEvent.getData()));
//        }
//    }
//    }