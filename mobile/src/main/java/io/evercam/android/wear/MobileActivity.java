package io.evercam.android.wear;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import io.evercam.android.wear.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import io.evercam.API;
import io.evercam.Camera;
import io.evercam.EvercamException;
import io.evercam.User;


public class MobileActivity extends ActionBarActivity implements MessageApi.MessageListener
{

    private final String TAG = "evercamwear-MobileActivity";
    public final String REQUEST_SNAPSHOT_PATH = "/request/snapshot";
    public final String REQUEST_CAMERA_LIST_PATH = "/request/cameralist";
    public final String MESSAGE_CAMERA_LIST_UPDATED_PATH = "/response/cameralist";
    public final String MESSAGE_SNAPSHOT_UPDATED_PATH = "/response/snapshot";
    public final String MESSAGE_SNAPSHOT_SAVED_PATH = "/response/save/success";
    public final String REQUEST_SAVE_SNAPSHOT_PATH = "/request/savesnapshot";
    private final String SNAPSHOT_FOLDER_NAME = "EvercamSnapshot";
    private GoogleApiClient mGoogleApiClient;
    private TextView welcomeTextView;
    private SharedPreferences sharedPreference;

    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mobile);

        sharedPreference = PreferenceManager.getDefaultSharedPreferences(this);

        welcomeTextView = (TextView) findViewById(R.id.welcome_textview);

        welcomeTextView.setText("Hello, " +  PrefsManager.getUsername(sharedPreference));

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
        if(id == R.id.action_logout)
        {
            PrefsManager.clearAccountDetails(sharedPreference);
            finish();
            startActivity(new Intent(MobileActivity.this, MainActivity.class));
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
            String cameraId = new String(messageEvent.getData());
            new RequestSnapshotTask(cameraId).execute();
        }
        else if(messageEvent.getPath().equals(REQUEST_CAMERA_LIST_PATH))
        {
            new RequestCameraListTask().execute();
        }
        else if(messageEvent.getPath().equals(REQUEST_SAVE_SNAPSHOT_PATH))
        {
            PendingResult<DataItemBuffer> results = Wearable.DataApi.getDataItems(mGoogleApiClient);
            results.setResultCallback(new ResultCallback<DataItemBuffer>() {
                @Override
                public void onResult(DataItemBuffer dataItems) {
                    if (dataItems.getCount() != 0) {
                        for(DataItem dataItem : dataItems)
                        {
                            DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                            final Asset asset = dataMapItem.getDataMap().getAsset("snapshot");

                            new FetchSnapshotTask(asset).execute();
                        }
                    }
                    dataItems.release();
                }
            });
        }
    }

    private class FetchSnapshotTask extends AsyncTask<Void,Void,Boolean>
    {
        private Asset asset;

        public FetchSnapshotTask(Asset asset)
        {
            this.asset = asset;
        }

        @Override
        protected Boolean doInBackground (Void... params)
        {
            if(asset == null)
            {
                Log.d(TAG, "asset is null");
              //  continue;
            }
            else
            {
                Log.d(TAG, "asset is not null");

                final Bitmap bitmap = loadBitmapFromAsset(asset);
                if(bitmap == null)
                {
                    Log.d(TAG, "image is null");
                }
                else
                {
                    Log.d(TAG, "image is not null");
                    String path = save(bitmap);
                    updateGallery(path);
                    return true;
                }
            }
            return false;
        }

        @Override
        protected void onPostExecute (Boolean isSuccess)
        {
            if(isSuccess)
            {
                new SendMessageTask(MESSAGE_SNAPSHOT_SAVED_PATH,null).execute();
            }
        }
    }

    public Bitmap loadBitmapFromAsset (Asset asset)
    {
        if(asset == null)
        {
            throw new IllegalArgumentException("Asset must be non-null");
        }
        ConnectionResult result = mGoogleApiClient.blockingConnect(2000, TimeUnit.MILLISECONDS);
        if(!result.isSuccess())
        {
            return null;
        }
        // convert asset into a file descriptor and block until it's ready
        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(mGoogleApiClient, asset).await().getInputStream();
     //   mGoogleApiClient.disconnect();

        if(assetInputStream == null)
        {
            Log.w(TAG, "Requested an unknown Asset.");
            return null;
        }
        // decode the stream into a bitmap
        return BitmapFactory.decodeStream(assetInputStream);
    }

    private boolean sendImageAndNotifyWearable (Bitmap bitmap)
    {
        Asset asset = createAssetFromBitmap(bitmap);
        PutDataMapRequest dataMap = PutDataMapRequest.create("/image");
        dataMap.getDataMap().putAsset("snapshot", asset);
        PutDataRequest request = dataMap.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, request);

        DataApi.DataItemResult result = pendingResult.await();
        if(result.getStatus().isSuccess())
        {
            Log.d(TAG, "Image asset updated");
            return true;
        }
        else
        {
            Log.d(TAG, "Failed to update asset");
            return false;
        }
    }

    private class UploadSnapshotTask extends AsyncTask<Void,Void,Boolean>
    {
        private Bitmap bitmap;

        public UploadSnapshotTask(Bitmap bitmap)
        {
            this.bitmap = bitmap;
        }

        @Override
        protected Boolean doInBackground (Void... params)
        {
            return sendImageAndNotifyWearable(bitmap);
        }

        @Override
        protected void onPostExecute (Boolean isSuccess)
        {
            if(isSuccess)
            {
               new SendMessageTask(MESSAGE_SNAPSHOT_UPDATED_PATH, null).execute();
            }
        }
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
        private String cameraId;

        public RequestSnapshotTask(String cameraId)
        {
            this.cameraId = cameraId;
        }

        @Override
        protected Bitmap doInBackground (Void... params)
        {
            Bitmap bitmap = null;
            String apiKey = PrefsManager.getUserApiKey(sharedPreference);
            String apiId = PrefsManager.getUserApiId(sharedPreference);

            API.setUserKeyPair(apiKey, apiId);

            try
            {
                InputStream inputStream = Camera.getSnapshotByCameraId(cameraId);
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
               // sendImageAndNotifyWearable(bitmap);
                new UploadSnapshotTask(bitmap).execute();
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
            ArrayList<Camera> onlineCameraArrayList = new ArrayList<Camera>();
            String apiKey = PrefsManager.getUserApiKey(sharedPreference);
            String apiId = PrefsManager.getUserApiId(sharedPreference);
            String username = PrefsManager.getUsername(sharedPreference);

            API.setUserKeyPair(apiKey, apiId);
            try
            {
                ArrayList<Camera> cameraArrayList = User.getCameras(username, true, false);
                if(cameraArrayList.size() > 0)
                {
                    for(int index = 0 ; index < cameraArrayList.size() ; index ++)
                    {
                        Camera camera = cameraArrayList.get(index);
                        if(camera.isOnline())
                        {
                            onlineCameraArrayList.add(camera);
                        }
                    }
                }
            }
            catch (EvercamException e)
            {
                e.printStackTrace();
            }

            return onlineCameraArrayList;
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

    /**
     * Return the snapshot path that get saved.
     */
    public String save(Bitmap snapshotBitmap)
    {
        int i = 0;
        String fileDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                + File.separator + SNAPSHOT_FOLDER_NAME;

        File folder = new File(fileDirectory );
        if (!folder.exists())
        {
            folder.mkdirs();
        }

        if (snapshotBitmap != null)
        {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            snapshotBitmap.compress(Bitmap.CompressFormat.JPEG, 40, bytes);


            String filename = "snapshot" + Integer.toString(i) + ".jpg";

            File f = new File(folder.getPath() + File.separator + filename);
            while (f.exists())
            {
                i++;
                filename = "snapshot" + Integer.toString(i) + ".jpg";
                f = new File(folder.getPath() + File.separator + filename);
            }

            try
            {
                f.createNewFile();
                FileOutputStream fo = new FileOutputStream(f);
                fo.write(bytes.toByteArray());
                fo.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                + File.separator + SNAPSHOT_FOLDER_NAME + "/snapshot" + i + ".jpg";
    }

    /**
     * Update gallery so that the saved snapshot can be viewed in gallery
     */
    public void updateGallery(String path)
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
        {
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES))));
        }
        else
        {
            new SingleMediaScanner(this,path);
        }
    }
}
