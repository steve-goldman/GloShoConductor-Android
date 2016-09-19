package constantbeta.com.gloshoconductor.preferences;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Size;

import constantbeta.com.gloshoconductor.R;
import constantbeta.com.gloshoconductor.camera.CameraWrapper;

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        findPreference(getString(R.string.threshold_key)).setOnPreferenceChangeListener(this);
        populateResolutionEntries();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue)
    {
        return validateThreshold(newValue);
    }

    private boolean validateThreshold(Object newValue)
    {
        try
        {
            int threshold = Integer.parseInt((String) newValue);
            return threshold >= 0 && threshold < 256;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private void populateResolutionEntries()
    {
        ListPreference resolutionPreference
                = (ListPreference) findPreference(getString(R.string.resolution_key));

        String[] resolutions = getResolutionStrings(CameraWrapper.getImageSizes(getActivity()));

        resolutionPreference.setEntries(resolutions);
        resolutionPreference.setEntryValues(resolutions);
    }

    private String[] getResolutionStrings(Size[] sizes)
    {
        String[] strings = new String[sizes.length];

        for (int i = 0; i < sizes.length; i++)
        {
            strings[i] = sizes[sizes.length - i - 1].toString();
        }

        return strings;
    }
}
