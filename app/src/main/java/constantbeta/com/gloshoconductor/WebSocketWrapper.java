package constantbeta.com.gloshoconductor;

import android.util.Log;
import android.util.Size;

import com.koushikdutta.async.callback.WritableCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class WebSocketWrapper implements AsyncHttpClient.WebSocketConnectCallback, WebSocket.StringCallback
{
    public interface Listener
    {
        void onConnected();
        void onUnableToConnect();
        void onLoggedIn();
        void onTakePicture();
        void onPictureSent();
    }

    private static final String TAG = "WebSocketWrapper";

    private final String uri;
    private WebSocket webSocket;
    private final Listener listener;

    private boolean loggedIn;

    public WebSocketWrapper(String uri, Listener listener)
    {
        this.uri = uri;
        this.listener = listener;
    }

    public void open()
    {
        if (null != webSocket)
        {
            return;
        }

        Log.d(TAG, "opening");
        AsyncHttpClient.getDefaultInstance().websocket(uri, "glosho-conductor", this);
    }

    public void close()
    {
        loggedIn = false;
        if (null != webSocket)
        {
            Log.d(TAG, "closing");
            webSocket.close();
            webSocket = null;
        }
    }

    public void sendReady()
    {
        Log.d(TAG, "sending ready");
        try
        {
            final Message message = new Message("ready-for-command");
            message.send(webSocket);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    public void sendProcessedImage(byte[] bytes)
    {
        Log.d(TAG, "sending processed image");
        webSocket.send(bytes);
        webSocket.setWriteableCallback(new WritableCallback()
        {
            @Override
            public void onWriteable()
            {
                webSocket.setWriteableCallback(null);
                listener.onPictureSent();
            }
        });
    }

    @Override
    public void onCompleted(Exception ex, WebSocket webSocket)
    {
        Log.d(TAG, "connection completed");
        if (null != ex)
        {
            ex.printStackTrace();
            listener.onUnableToConnect();
            return;
        }

        this.webSocket = webSocket;
        webSocket.setStringCallback(this);

        listener.onConnected();
    }

    void login(Size size, String imageProcessorType)
    {
        Log.d(TAG, "logging in");
        try
        {
            final Message message = new Message("conductor-login")
                    .put("width", size.getWidth())
                    .put("height", size.getHeight())
                    .put("image-processor-type", imageProcessorType);
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
            else if ("take-picture".equals(messageType))
            {
                listener.onTakePicture();
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }
}
