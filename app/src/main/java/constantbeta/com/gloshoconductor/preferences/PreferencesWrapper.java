package constantbeta.com.gloshoconductor.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Size;

import java.util.UUID;

import constantbeta.com.gloshoconductor.R;

public class PreferencesWrapper
{
    private final Context           context;
    private final SharedPreferences preferences;

    public PreferencesWrapper(Context context, SharedPreferences preferences)
    {
        this.context     = context;
        this.preferences = preferences;
    }

    public String getServerUrl()
    {
        return get(R.string.server_url_key, R.string.server_url_default);
    }

    public Size getResolution()
    {
        return Size.parseSize(get(R.string.resolution_key, R.string.resolution_default));
    }

    public String getImageProcessorType()
    {
        return get(R.string.image_processor_type_key, R.string.image_processor_type_default);
    }

    public int getThreshold()
    {
        return Integer.parseInt(get(R.string.threshold_key, R.string.threshold_default));
    }

    public int getExpectedPlayerCount()
    {
        return Integer.parseInt(get(R.string.expected_player_count_key,
                                    R.string.expected_player_count_default));
    }

    public String getInstallationId()
    {
        String installationId = get(R.string.installation_id_key, null);
        if (installationId == null)
        {
            installationId = UUID.randomUUID().toString();
            put(R.string.installation_id_key, installationId);
        }

        return installationId;
    }

    private String get(int keyId, int defaultId)
    {
        return get(keyId, context.getString(defaultId));
    }

    private String get(int keyId, String defaultValue)
    {
        return preferences.getString(context.getString(keyId), defaultValue);
    }

    private void put(int keyId, String value)
    {
        preferences.edit().putString(context.getString(keyId), value).apply();
    }
}
