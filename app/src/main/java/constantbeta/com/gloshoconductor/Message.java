package constantbeta.com.gloshoconductor;

import com.koushikdutta.async.http.WebSocket;

import org.json.JSONException;
import org.json.JSONObject;

public class Message
{
    private final JSONObject json;

    public Message(String messageType) throws JSONException
    {
        json = new JSONObject()
                .put("messageType", messageType)
                .put("timestamp", System.currentTimeMillis());
    }

    public Message put(String key, String value) throws JSONException
    {
        json.put(key, value);
        return this;
    }

    public void send(WebSocket webSocket)
    {
        webSocket.send(json.toString());
    }
}
