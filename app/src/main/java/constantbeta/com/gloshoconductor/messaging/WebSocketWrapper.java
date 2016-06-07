package constantbeta.com.gloshoconductor.messaging;

import android.util.Log;
import android.util.Size;

import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.WritableCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.nio.ByteBuffer;

public class WebSocketWrapper
{
    public interface Listener
    {
        void onConnected();
        void onDisconnected();
        void onUnableToConnect();
        void onLoggedIn();
        void onUnableToLogIn();
        void onPlayerCountUpdated(int playerCount);
        void onStartingIn(int seconds);
        void onRunning();
        void onTakePicture();
        void onStartTakingPictures();
        void onStopTakingPictures();
        void onPictureSent();
        void onDone();
    }

    private static final String TAG = "WebSocketWrapper";

    private final String uri;
    private WebSocket webSocket;
    private final Listener listener;

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
        AsyncHttpClient.getDefaultInstance().websocket(uri, "glosho-conductor", connectedCallback);
    }

    public void close()
    {
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
            final Message message = new Message("ready-to-start");
            message.send(webSocket);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    private void sendPong()
    {
        Log.d(TAG, "sending pong");
        try
        {
            final Message message = new Message("pong");
            message.send(webSocket);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    public void sendProcessedImage(ByteBuffer bytes)
    {
        Log.d(TAG, "sending processed image");
        // note: the library calls the last argument "len" but uses it as
        // the index of one past the last element
        webSocket.send(bytes.array(), bytes.arrayOffset(), bytes.remaining() + bytes.arrayOffset());
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

    private final AsyncHttpClient.WebSocketConnectCallback connectedCallback = new AsyncHttpClient.WebSocketConnectCallback()
    {
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

            WebSocketWrapper.this.webSocket = webSocket;
            webSocket.setStringCallback(stringCallback);
            webSocket.setClosedCallback(disconnectedCallback);

            listener.onConnected();
        }
    };

    public void login(Size size, String imageProcessorType)
    {
        Log.d(TAG, "logging in");
        try
        {
            final Message message = new Message("conductor-login")
                    .put("width", size.getWidth())
                    .put("height", size.getHeight())
                    .put("imageProcessorType", imageProcessorType);
            message.send(webSocket);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    private final WebSocket.StringCallback stringCallback = new WebSocket.StringCallback()
    {
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
                    listener.onLoggedIn();
                }
                if ("conductor-login-error".equals(messageType))
                {
                    listener.onUnableToLogIn();
                }
                else if ("take-picture".equals(messageType))
                {
                    listener.onTakePicture();
                }
                else if ("start-taking-pictures".equals(messageType))
                {
                    listener.onStartTakingPictures();
                }
                else if ("stop-taking-pictures".equals(messageType))
                {
                    listener.onStopTakingPictures();
                }
                else if ("player-count-update".equals(messageType))
                {
                    listener.onPlayerCountUpdated(json.getInt("playerCount"));
                }
                else if ("starting-in".equals(messageType))
                {
                    listener.onStartingIn(json.getInt("seconds"));
                }
                else if ("running".equals(messageType))
                {
                    listener.onRunning();
                }
                else if ("done".equals(messageType))
                {
                    listener.onDone();
                }
                else if ("ping".equals(messageType))
                {
                    sendPong();
                }
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
        }
    };

    private final CompletedCallback disconnectedCallback = new CompletedCallback()
    {
        @Override
        public void onCompleted(Exception ex)
        {
            listener.onDisconnected();
        }
    };
}
