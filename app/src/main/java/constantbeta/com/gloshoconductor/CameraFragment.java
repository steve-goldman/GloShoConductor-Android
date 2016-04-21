package constantbeta.com.gloshoconductor;


import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class CameraFragment extends Fragment implements View.OnClickListener, WebSocketWrapper.Listener
{
    private static final String TAG = "CameraFragment";

    private final BackgroundThread backgroundThread = new BackgroundThread("CameraBackground");

    private final WebSocketWrapper webSocketWrapper = new WebSocketWrapper("ws://192.168.0.8:8080", this);

    private CameraWrapper cameraWrapper;

    private CameraPermissions cameraPermissions;

    private TextureView textureView;
    private TextView connectingTextView;
    private TextView loggingInTextView;
    private Button readyButton;

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
        connectingTextView = (TextView)view.findViewById(R.id.connecting_text_view);
        loggingInTextView = (TextView)view.findViewById(R.id.logging_in_text_view);
        readyButton = (Button)view.findViewById(R.id.ready_to_start_button);
        readyButton.setOnClickListener(this);
        disappearViews();
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
        connectingTextView.setVisibility(View.VISIBLE);
        webSocketWrapper.open();
        Log.d(TAG, "exiting onResume");
    }

    @Override
    public void onPause()
    {
        Log.d(TAG, "entering onPause");
        super.onPause();
        cameraWrapper.closeCamera();
        webSocketWrapper.close();
        backgroundThread.stop();
        disappearViews();
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

    @Override
    public void onConnected()
    {
        Log.d(TAG, "listener notified connected");
        getActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                connectingTextView.setVisibility(View.GONE);
                loggingInTextView.setVisibility(View.VISIBLE);
            }
        });
        webSocketWrapper.login();
    }

    @Override
    public void onLoggedIn()
    {
        Log.d(TAG, "listener notified logged in");
        getActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                loggingInTextView.setVisibility(View.GONE);
                readyButton.setVisibility(View.VISIBLE);
            }
        });
    }

    private void disappearViews()
    {
        for (final View v : new View[]{ connectingTextView, loggingInTextView, readyButton })
        {
            v.setVisibility(View.GONE);
        }
    }
}
