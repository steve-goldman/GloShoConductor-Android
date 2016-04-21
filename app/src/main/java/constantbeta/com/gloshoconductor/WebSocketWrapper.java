package constantbeta.com.gloshoconductor;

import android.util.Log;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

// package scope
class WebSocketWrapper implements AsyncHttpClient.WebSocketConnectCallback, WebSocket.StringCallback
{
    interface Listener
    {
        void onConnected();
        void onLoggedIn();
    }

    private static final String TAG = "WebSocketWrapper";

    private final String uri;
    private WebSocket webSocket;
    private final Listener listener;

    private boolean loggedIn;

    WebSocketWrapper(String uri, Listener listener)
    {
        this.uri = uri;
        this.listener = listener;
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
        loggedIn = false;
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
        webSocket.setStringCallback(this);

        listener.onConnected();
    }

    void login()
    {
        Log.d(TAG, "logging in");
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
    public void onStringAvailable(String message)
    {
        Log.d(TAG, "received: " + message);
        try
        {
            final JSONObject json = new JSONObject(new JSONTokener(message));
            final String messageType = json.getString("messageType");

            if ("conductor-logged-in".equals(messageType))
            {
                loggedIn = true;
                listener.onLoggedIn();
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }
}
