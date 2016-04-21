package constantbeta.com.gloshoconductor;


import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

public class CameraFragment extends Fragment implements View.OnClickListener
{
    private static final String TAG = "CameraFragment";

    private final BackgroundThread backgroundThread = new BackgroundThread("CameraBackground");

    private CameraWrapper cameraWrapper;

    private CameraPermissions cameraPermissions;

    private TextureView textureView;

    // package scope
    static CameraFragment newInstance()
    {
        return new CameraFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.ready_to_start_button).setOnClickListener(this);
        textureView = (TextureView)view.findViewById(R.id.texture);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        cameraPermissions = new CameraPermissions(this);
    }

    @Override
    public void onResume()
    {
        Log.d(TAG, "entering onResume");
        super.onResume();
        backgroundThread.start();
        cameraWrapper = new CameraWrapper(this, backgroundThread.handler());
        openCamera();
        Log.d(TAG, "exiting onResume");
    }

    @Override
    public void onPause()
    {
        Log.d(TAG, "entering onPause");
        super.onPause();
        cameraWrapper.closeCamera();
        backgroundThread.stop();
        Log.d(TAG, "exiting onPause");
    }

    @Override
    public void onClick(View v)
    {
        cameraWrapper.takePicture();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (CameraPermissions.REQUEST_CODE == requestCode)
        {
            cameraPermissions.onRequestPermissionsResult(grantResults);
        }
    }

    public TextureView getTextureView()
    {
        return textureView;
    }

    private void openCamera()
    {
        if (cameraPermissions.has())
        {
            cameraWrapper.open();
        }
        else
        {
            cameraPermissions.request();
        }
    }
}
