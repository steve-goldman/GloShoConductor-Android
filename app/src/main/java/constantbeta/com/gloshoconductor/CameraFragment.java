package constantbeta.com.gloshoconductor;


import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

public class CameraFragment extends Fragment implements View.OnClickListener, WebSocketWrapper.Listener
{
    private final BackgroundThread backgroundThread = new BackgroundThread("CameraBackground");

    private final WebSocketWrapper webSocketWrapper = new WebSocketWrapper("ws://192.168.0.8:8080", this);

    private CameraWrapper cameraWrapper;

    private CameraPermissions cameraPermissions;

    private TextureView textureView;
    private final ViewStateManager viewStateManager = ViewStateManager.get();

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
        textureView = (TextureView)view.findViewById(R.id.texture);
        viewStateManager.init(view);
        view.findViewById(R.id.ready_to_start_button).setOnClickListener(this);
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
        super.onResume();
        backgroundThread.start();
        cameraWrapper = new CameraWrapper(this, backgroundThread.handler());
        openCamera();
        viewStateManager.setState(ViewStateManager.States.CONNECTING);
        webSocketWrapper.open();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        cameraWrapper.closeCamera();
        webSocketWrapper.close();
        backgroundThread.stop();
    }

    @Override
    public void onClick(View v)
    {
        viewStateManager.setState(ViewStateManager.States.WAITING_FOR_COMMAND);
        webSocketWrapper.sendReady();
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

    @Override
    public void onConnected()
    {
        getActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                viewStateManager.setState(ViewStateManager.States.LOGGING_IN);
            }
        });
        webSocketWrapper.login();
    }

    @Override
    public void onUnableToConnect()
    {
        getActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                viewStateManager.setState(ViewStateManager.States.UNABLE_TO_CONNECT);
            }
        });
    }

    @Override
    public void onLoggedIn()
    {
        getActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                viewStateManager.setState(ViewStateManager.States.READY_TO_START);
            }
        });
    }

    @Override
    public void onTakePicture()
    {
        getActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                viewStateManager.setState(ViewStateManager.States.TAKING_PICTURE);
            }
        });
        cameraWrapper.takePicture();
    }
}
