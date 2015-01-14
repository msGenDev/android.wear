package io.evercam.android.wear;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;

public class SingleMediaScanner implements MediaScannerConnection.MediaScannerConnectionClient
{
    private MediaScannerConnection connection;
    private Context context;
    private String imagepath;

    public SingleMediaScanner(Context context, String url)
    {
        this.context = context;
        startScan(url);
    }

    public void startScan(String url)
    {
        imagepath = url;
        if(connection != null) connection.disconnect();
        connection = new MediaScannerConnection(context, this);
        connection.connect();
    }

    @Override
    public void onMediaScannerConnected()
    {
        try
        {
            connection.scanFile(imagepath, null);
        }
        catch(IllegalStateException e)
        {
        }
    }

    @Override
    public void onScanCompleted(String path, Uri uri)
    {
        connection.disconnect();
    }
}