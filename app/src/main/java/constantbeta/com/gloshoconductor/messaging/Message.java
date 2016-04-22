package constantbeta.com.gloshoconductor.messaging;

import com.koushikdutta.async.http.WebSocket;

import org.json.JSONException;
import org.json.JSONObject;

// package scope -- should only be used by WebSocketWrapper
class Message
{
    private final JSONObject json;

    Message(String messageType) throws JSONException
    {
        json = new JSONObject()
                .put("messageType", messageType)
                .put("timestamp", System.currentTimeMillis());
    }

    Message put(String key, String value) throws JSONException
    {
        json.put(key, value);
        return this;
    }

    Message put(String key, int value) throws JSONException
    {
        json.put(key, value);
        return this;
    }

    void send(WebSocket webSocket)
    {
        webSocket.send(json.toString());
    }
}
