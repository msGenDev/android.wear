package io.evercam.android.wear;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;

public class SingleMediaScanner implements MediaScannerConnection.MediaScannerConnectionClient
{
    MediaScannerConnection connection;
    Context ctxt;
    private String imagepath;

    public SingleMediaScanner(Context ctxt, String url)
    {
        this.ctxt = ctxt;
        startScan(url);
    }

    public void startScan(String url)
    {
        imagepath = url;
        if (connection != null) connection.disconnect();
        connection = new MediaScannerConnection(ctxt, this);
        connection.connect();
    }

    @Override
    public void onMediaScannerConnected()
    {
        try
        {
            connection.scanFile(imagepath, null);
        }
        catch (IllegalStateException e)
        {
        }
    }

    @Override
    public void onScanCompleted(String path, Uri uri)
    {
        connection.disconnect();
    }
}