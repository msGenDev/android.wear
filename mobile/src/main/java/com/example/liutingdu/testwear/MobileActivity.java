package com.example.liutingdu.testwear;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import io.evercam.API;
import io.evercam.Camera;
import io.evercam.EvercamException;
import io.evercam.User;


public class MobileActivity extends ActionBarActivity implements MessageApi.MessageListener
{

    private final String TAG = "MobileActivity";
    public final String REQUEST_SNAPSHOT_PATH = "/request/snapshot";
    public final String REQUEST_CAMERA_LIST_PATH = "/request/cameralist";
    public final String MESSAGE_CAMERA_LIST_UPDATED_PATH = "/response/cameralist";
    GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mobile);
        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks()
        {
            @Override
            public void onConnected (Bundle connectionHint)
            {
                Log.d(TAG, "onConnected: " + connectionHint);
                // Now you can use the data layer API
                Wearable.MessageApi.addListener(mGoogleApiClient, MobileActivity.this);
            }

            @Override
            public void onConnectionSuspended (int cause)
            {
                Log.d(TAG, "onConnectionSuspended: " + cause);
            }
        }).addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener()
        {
            @Override
            public void onConnectionFailed (ConnectionResult result)
            {
                Log.d(TAG, "onConnectionFailed: " + result);
            }
        }).addApi(Wearable.API).build();
    }

    @Override
    protected void onStart ()
    {
        super.onStart();
        Log.d(TAG, "onStart");
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop ()
    {
        if(null != mGoogleApiClient && mGoogleApiClient.isConnected())
        {
            Wearable.MessageApi.removeListener(mGoogleApiClient, MobileActivity.this);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }


    @Override
    public boolean onCreateOptionsMenu (Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.mobile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if(id == R.id.action_settings)
        {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMessageReceived (MessageEvent messageEvent)
    {
        Log.d(TAG, "onMessageReceived" + messageEvent.getPath() + " " + new String(messageEvent.getData()));
        if(messageEvent.getPath().equals(REQUEST_SNAPSHOT_PATH))
        {
            new RequestSnapshotTask().execute();
        }
        else if(messageEvent.getPath().equals(REQUEST_CAMERA_LIST_PATH))
        {
            Log.d(TAG, "Message received");
            new RequestCameraListTask().execute();
        }
    }

    private void sendImageAndNotifyWearable (Bitmap bitmap)
    {
        Asset asset = createAssetFromBitmap(bitmap);
        PutDataMapRequest dataMap = PutDataMapRequest.create("/image");
        dataMap.getDataMap().putAsset("snapshot", asset);
        PutDataRequest request = dataMap.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, request);
    }

    private void sendCameraListAndNotifyWearable(ArrayList<Camera> cameraArrayList)
    {
        PutDataMapRequest dataMap = PutDataMapRequest.create("/cameralist");

        DataMap map = new DataMap();
     //   map.putInt("count",cameraArrayList.size());

        ArrayList<String> cameraIdList = new ArrayList<String>();
        ArrayList<String> cameraNameList = new ArrayList<String>();

        for(int index = 0; index < cameraArrayList.size(); index ++)
        {
            try
            {
          //      map.putString(String.valueOf(index), cameraArrayList.get(index).getName());
                cameraIdList.add(cameraArrayList.get(index).getId());
                cameraNameList.add(cameraArrayList.get(index).getName());
            }
            catch (EvercamException e)
            {
                Log.e(TAG, e.toString());
                continue;
            }
        }

        map.putStringArrayList("cameraIdList", cameraIdList);
        map.putStringArrayList("cameraNameList", cameraNameList);

        dataMap.getDataMap().putAll(map);
        PutDataRequest request = dataMap.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                .putDataItem(mGoogleApiClient, request);
        Log.d(TAG, "Data uploaded as data item");

        //Notify wearable by sending a message, because if put same data to
        //the data map, the data api won't get called on the watch.
        new SendMessageTask(MESSAGE_CAMERA_LIST_UPDATED_PATH, null).execute();
    }

    private static Asset createAssetFromBitmap (Bitmap bitmap)
    {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }

    private class RequestSnapshotTask extends AsyncTask<Void, Void, Bitmap>
    {
        @Override
        protected Bitmap doInBackground (Void... params)
        {

            Bitmap bitmap = null;
            API.setUserKeyPair("83d44e31781b88c0c73bded662be28bc", "614f4cb6");
            try
            {
                InputStream inputStream = Camera.getSnapshotByCameraId("wayra-office");
                bitmap = BitmapFactory.decodeStream(inputStream);
            }
            catch (EvercamException e)
            {
                e.printStackTrace();
            }

            return bitmap;
        }

        @Override
        protected void onPostExecute (Bitmap bitmap)
        {
            if(bitmap != null)
            {
                Log.d(TAG, "Image not null");
                sendImageAndNotifyWearable(bitmap);
            }
            else
            {
                Log.d(TAG, "Image is null");
            }
        }
    }

    private class RequestCameraListTask extends AsyncTask<Void, Void, ArrayList<Camera>>
    {
        @Override
        protected ArrayList<Camera> doInBackground (Void... params)
        {
            ArrayList<Camera> cameraArrayList = new ArrayList<Camera>();
            API.setUserKeyPair("83d44e31781b88c0c73bded662be28bc", "614f4cb6");
            try
            {
                cameraArrayList = User.getCameras("liutingd", true, false);
            }
            catch (EvercamException e)
            {
                e.printStackTrace();
            }

            return cameraArrayList;
        }

        @Override
        protected void onPostExecute (ArrayList<Camera> cameraArrayList)
        {
            if(cameraArrayList.size() > 0)
            {
                Log.d(TAG, "Send camera list");
                sendCameraListAndNotifyWearable(cameraArrayList);
            }
            else
            {
                Log.d(TAG, "Camera list size is 0");
            }
        }
    }

    private class SendMessageTask extends AsyncTask<Void, Void, Void>
    {
        private final String PATH;
        private final byte[] data;

        public SendMessageTask(String path, byte[] data)
        {
            PATH = path;
            this.data = data;
        }

        @Override
        protected Void doInBackground (Void... params)
        {
            Log.d(TAG, "Start send message");
            String nodeId = getNodes().iterator().next();
            MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, PATH, data).await();

            if(!result.getStatus().isSuccess())
            {
                Log.d(TAG, "ERROR: failed to send Message: " + result.getStatus());
            }
            else
            {
                Log.d(TAG, "Message sent!! " + result.getStatus());
            }
            return null;
        }
    }

    private Collection<String> getNodes ()
    {
        Log.d(TAG, "Getting notes");
        HashSet<String> results = new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (Node node : nodes.getNodes())
        {
            results.add(node.getId());
        }
        return results;
    }
}
