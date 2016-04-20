package constantbeta.com.gloshoconductor;


import android.app.Activity;
import android.os.Bundle;

public class CameraActivity extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        if (null == savedInstanceState)
        {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, CameraFragment.newInstance())
                    .commit();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        // workaround for a bug that was crashing when the error dialog comes up
    }
}
