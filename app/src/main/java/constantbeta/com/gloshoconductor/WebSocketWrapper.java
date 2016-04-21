package constantbeta.com.gloshoconductor;

import android.util.Log;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import org.json.JSONException;

// package scope
class WebSocketWrapper implements AsyncHttpClient.WebSocketConnectCallback, WebSocket.StringCallback
{
    private static final String TAG = "WebSocketWrapper";

    private final String uri;
    private WebSocket webSocket;

    WebSocketWrapper(String uri)
    {
        this.uri = uri;
    }

    void open()
    {
        if (null != webSocket)
        {
            return;
        }

        Log.d(TAG, "opening");
        AsyncHttpClient.getDefaultInstance().websocket(uri, "glosho-conductor", this);
    }

    void close()
    {
        if (null != webSocket)
        {
            Log.d(TAG, "closing");
            webSocket.close();
            webSocket = null;
        }
    }

    @Override
    public void onCompleted(Exception ex, WebSocket webSocket)
    {
        Log.d(TAG, "connection completed");
        if (null != ex)
        {
            ex.printStackTrace();
            return;
        }

        this.webSocket = webSocket;

        try
        {
            final Message message = new Message("conductor-login");
            message.send(webSocket);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onStringAvailable(String s)
    {
        Log.d(TAG, "received: " + s);
    }
}
