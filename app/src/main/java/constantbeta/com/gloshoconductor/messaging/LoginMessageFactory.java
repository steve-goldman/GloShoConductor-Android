package constantbeta.com.gloshoconductor.messaging;

import android.os.Build;
import android.util.Size;

import org.json.JSONException;

import constantbeta.com.gloshoconductor.BuildConfig;

public class LoginMessageFactory
{
    private final String installationId;

    public LoginMessageFactory(String installationId)
    {
        this.installationId = installationId;
    }

    public Message create(Size size, String imageProcessorType, int expectedPlayerCount) throws JSONException
    {
        return new Message("conductor-login")
                .put("width", size.getWidth())
                .put("height", size.getHeight())
                .put("imageProcessorType", imageProcessorType)
                .put("expectedPlayerCount", expectedPlayerCount)
                // common to all platforms
                .put("id",           installationId)
                .put("version",      BuildConfig.VERSION_NAME)
                .put("build",        BuildConfig.VERSION_CODE)
                .put("platform",     "ANDROID")
                // android details
                .put("sdkVersion",   Build.VERSION.SDK_INT)
                .put("displayId",    Build.DISPLAY)
                .put("product",      Build.PRODUCT)
                .put("device",       Build.DEVICE)
                .put("manufacturer", Build.MANUFACTURER)
                .put("brand",        Build.BRAND)
                .put("model",        Build.MODEL)
                .put("hardware",     Build.HARDWARE);
    }
}
