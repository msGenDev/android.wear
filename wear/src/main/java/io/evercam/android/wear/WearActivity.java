package io.evercam.android.wear;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.WatchViewStub;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;


public class WearActivity extends Activity implements WearableListView.ClickListener,
        DataApi.DataListener, MessageApi.MessageListener
{
    public final String REQUEST_SNAPSHOT_PATH = "/request/snapshot";
    public final String REQUEST_CAMERA_LIST_PATH = "/request/cameralist";
    public final String REQUEST_SAVE_SNAPSHOT_PATH = "/request/savesnapshot";
    public final String MESSAGE_CAMERA_LIST_UPDATED_PATH = "/response/cameralist";
    public final String MESSAGE_SNAPSHOT_SAVED_PATH = "/response/save/success";
    private final String TAG = "WearActivity";

    private WearableListView listView;
    private ImageView imageView;
    private ProgressBar progressBar;
    private CircledImageView circledImageView;
    private LinearLayout saveLayout;
    private GoogleApiClient mGoogleApiClient;

    private ArrayList<String> cameraIdList = new ArrayList<String>();
    private ArrayList<String> cameraNameList = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wear);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener()
        {
            @Override
            public void onLayoutInflated(WatchViewStub stub)
            {
                listView = (WearableListView) findViewById(R.id.wearable_list);
                imageView = (ImageView) findViewById(R.id.snapshotImage);
                saveLayout = (LinearLayout) findViewById(R.id.save_layout);
                progressBar = (ProgressBar) findViewById(R.id.progress_bar);
                circledImageView = (CircledImageView) findViewById(R.id.save_button);

                listView.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);

                new SendMessageTask(REQUEST_CAMERA_LIST_PATH, null).execute();
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks()
        {
            @Override
            public void onConnected(Bundle connectionHint)
            {
                Log.d(TAG, "onConnected: " + connectionHint);
                // Now use the data layer API
                Wearable.DataApi.addListener(mGoogleApiClient, WearActivity.this);
                Wearable.MessageApi.addListener(mGoogleApiClient, WearActivity.this);
            }

            @Override
            public void onConnectionSuspended(int cause)
            {
                Log.d(TAG, "onConnectionSuspended: " + cause);
            }
        }).addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener()
        {
            @Override
            public void onConnectionFailed(ConnectionResult result)
            {
                Log.d(TAG, "onConnectionFailed: " + result);
            }
        }).addApi(Wearable.API).build();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        Log.d(TAG, "onStart");
        mGoogleApiClient.connect();
    }

    // WearableListView click listener
    @Override
    public void onClick(WearableListView.ViewHolder v)
    {
        Integer tag = (Integer) v.itemView.getTag();

        TextView txtView = (TextView) v.itemView.findViewById(R.id.name);

        String selectedCameraName = txtView.getText().toString();
        Log.d(TAG, tag + " " + selectedCameraName);
        listView.setVisibility(View.GONE);
        imageView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        new SendMessageTask(REQUEST_SNAPSHOT_PATH, retrieveCameraIdFrom(selectedCameraName)
                .getBytes()).execute();
    }

    private String retrieveCameraIdFrom(String cameraName)
    {
        for(int index = 0; index < cameraNameList.size(); index++)
        {
            if(cameraName.equals(cameraNameList.get(index)))
            {
                return cameraIdList.get(index);
            }
        }
        return "";
    }

    private void showCameraList(ArrayList<String> cameraNameList)
    {
        String[] cameraNameArray = new String[cameraNameList.size()];
        cameraNameArray = cameraNameList.toArray(cameraNameArray);

        progressBar.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);

        listView.setAdapter(new Adapter(WearActivity.this, cameraNameArray));
        listView.setClickListener(WearActivity.this);
    }

    private Collection<String> getNodes()
    {
        Log.d(TAG, "Getting notes");
        HashSet<String> results = new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes
                (mGoogleApiClient).await();
        for(Node node : nodes.getNodes())
        {
            Log.d(TAG, "Node: " + node.getId() + " added!");
            results.add(node.getId());
        }
        return results;
    }

    @Override
    public void onTopEmptyRegionClick()
    {
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents)
    {
        Log.d(TAG, "onDataChanged");
        for(DataEvent event : dataEvents)
        {
            if(event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath
                    ().equals("/image"))
            {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                Asset asset = dataMapItem.getDataMap().getAsset("snapshot");
                if(asset == null)
                {
                    Log.d(TAG, "asset is null");
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
                        this.runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                imageView.setVisibility(View.VISIBLE);
                                progressBar.setVisibility(View.GONE);
                                listView.setVisibility(View.GONE);

                                imageView.setImageBitmap(bitmap);
                                imageView.setOnClickListener(new View.OnClickListener()
                                {
                                    public void onClick(View v)
                                    {
                                        Log.d(TAG, "image view on click");
                                        imageView.setOnClickListener(null);
                                        imageView.setVisibility(View.GONE);
                                        saveLayout.setVisibility(View.VISIBLE);
                                        circledImageView.setOnClickListener(new View
                                                .OnClickListener()
                                        {
                                            @Override
                                            public void onClick(View v)
                                            {
                                                Log.d(TAG, "circled image view on click");
                                                saveLayout.setVisibility(View.GONE);
                                                progressBar.setVisibility(View.VISIBLE);

                                                new SendMessageTask(REQUEST_SAVE_SNAPSHOT_PATH,
                                                        null).executeOnExecutor(AsyncTask
                                                        .THREAD_POOL_EXECUTOR);
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                }
            }
        }
    }

    public Bitmap loadBitmapFromAsset(Asset asset)
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
        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(mGoogleApiClient,
                asset).await().getInputStream();

        if(assetInputStream == null)
        {
            Log.w(TAG, "Requested an unknown Asset.");
            return null;
        }
        // decode the stream into a bitmap
        return BitmapFactory.decodeStream(assetInputStream);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent)
    {
        Log.d(TAG, "onMessageReceived" + messageEvent.getPath() + " " + new String(messageEvent
                .getData()));

        if(messageEvent.getPath().equals(MESSAGE_CAMERA_LIST_UPDATED_PATH))
        {
            Log.d(TAG, "Camera list updated");

            PendingResult<DataItemBuffer> results = Wearable.DataApi.getDataItems(mGoogleApiClient);
            results.setResultCallback(new ResultCallback<DataItemBuffer>()
            {
                @Override
                public void onResult(DataItemBuffer dataItems)
                {
                    if(dataItems.getCount() != 0)
                    {
                        DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItems.get(0));

                        cameraIdList = dataMapItem.getDataMap().getStringArrayList("cameraIdList");
                        cameraNameList = dataMapItem.getDataMap().getStringArrayList
                                ("cameraNameList");

                        Log.d(TAG, cameraIdList.size() + " " + cameraNameList.size());

                        showCameraList(cameraNameList);
                    }
                    dataItems.release();
                }
            });
        }
        else if(messageEvent.getPath().equals(MESSAGE_SNAPSHOT_SAVED_PATH))
        {
            this.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    progressBar.setVisibility(View.GONE);
                    imageView.setVisibility(View.GONE);
                    saveLayout.setVisibility(View.GONE);
                    listView.setVisibility(View.VISIBLE);
                }
            });

            Intent intent = new Intent(WearActivity.this, ConfirmationActivity.class);

            intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                    ConfirmationActivity.SUCCESS_ANIMATION);
            intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, "Succeeded!");
            startActivity(intent);
        }
    }

    private static final class Adapter extends WearableListView.Adapter
    {
        private final Context mContext;
        private final LayoutInflater mInflater;
        private String[] mDataset;

        // Provide a suitable constructor (depends on the kind of dataset)
        public Adapter(Context context, String[] dataset)
        {
            mContext = context;
            mInflater = LayoutInflater.from(context);
            mDataset = dataset;
        }

        // Create new views for list items
        // (invoked by the WearableListView's layout manager)
        @Override
        public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            // Inflate our custom layout for list items
            return new ItemViewHolder(mInflater.inflate(R.layout.list_item, null));
        }

        // Replace the contents of a list item
        // Instead of creating new views, the list tries to recycle existing ones
        // (invoked by the WearableListView's layout manager)
        @Override
        public void onBindViewHolder(WearableListView.ViewHolder holder, int position)
        {
            // retrieve the text view
            ItemViewHolder itemHolder = (ItemViewHolder) holder;
            TextView view = itemHolder.textView;
            // replace text contents
            view.setText(mDataset[position]);
            // replace list item's metadata
            holder.itemView.setTag(position);
        }

        // Return the size of your dataset
        // (invoked by the WearableListView's layout manager)
        @Override
        public int getItemCount()
        {
            return mDataset.length;
        }

        // Provide a reference to the type of views you're using
        public static class ItemViewHolder extends WearableListView.ViewHolder
        {
            private TextView textView;

            public ItemViewHolder(View itemView)
            {
                super(itemView);
                // find the text view within the custom item's layout
                textView = (TextView) itemView.findViewById(R.id.name);
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
        protected Void doInBackground(Void... params)
        {
            Log.d(TAG, "Start send message");
            Collection<String> nodes = getNodes();
            String nodeId = "";
            if(nodes.size() > 0)
            {
                nodeId = nodes.iterator().next();
            }
            Log.d(TAG, "Node id:" + nodeId);
            MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage
                    (mGoogleApiClient, nodeId, PATH, data).await();

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
}
