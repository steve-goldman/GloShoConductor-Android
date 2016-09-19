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
        void onFoundPlayerCountUpdated(int playerCount);
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

    private final LoginMessageFactory loginMessageFactory;

    public WebSocketWrapper(String uri, String installationId, Listener listener)
    {
        this.uri = uri;
        this.listener = listener;
        this.loginMessageFactory = new LoginMessageFactory(installationId);
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

    public synchronized void close()
    {
        if (null != webSocket)
        {
            Log.d(TAG, "closing");
            webSocket.close();
            webSocket = null;
        }
    }

    public synchronized void sendReady()
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

    private synchronized void sendPong()
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

    private static final int TIMESTAMP_BUFFER_SIZE = 8;
    private final ByteBuffer timestampBuffer       = ByteBuffer.allocate(TIMESTAMP_BUFFER_SIZE);

    public synchronized void sendProcessedImage(ByteBuffer bytes)
    {
        if (webSocket == null)
        {
            // in case we just closed but the camera hasn't figured that out yet
            return;
        }

        Log.d(TAG, "sending processed image");

        timestampBuffer.putLong(0, System.currentTimeMillis());
        // note: the library calls the last argument "len" but uses it as
        // the index of one past the last element
        webSocket.send(timestampBuffer.array(), timestampBuffer.arrayOffset(), timestampBuffer.arrayOffset() + TIMESTAMP_BUFFER_SIZE);
        webSocket.send(bytes.array(),           bytes.arrayOffset(),           bytes.arrayOffset() + bytes.remaining());

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

    public void login(Size size, String imageProcessorType, int threshold, int expectedPlayerCount)
    {
        if (webSocket == null)
        {
            // in case we disconnected before logging in
            return;
        }

        Log.d(TAG, "logging in");
        try
        {
            final Message message = loginMessageFactory.create(size,
                                                               imageProcessorType,
                                                               threshold,
                                                               expectedPlayerCount);
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
                else if ("found-player-count-update".equals(messageType))
                {
                    listener.onFoundPlayerCountUpdated(json.getInt("foundPlayerCount"));
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
