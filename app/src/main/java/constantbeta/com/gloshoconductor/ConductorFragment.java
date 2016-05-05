package constantbeta.com.gloshoconductor;


import android.app.Fragment;
import android.media.Image;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import java.util.Timer;
import java.util.TimerTask;

import constantbeta.com.gloshoconductor.camera.CameraPermissions;
import constantbeta.com.gloshoconductor.camera.CameraWrapper;
import constantbeta.com.gloshoconductor.imageprocessors.ImageProcessor;
import constantbeta.com.gloshoconductor.imageprocessors.ImageProcessorFactory;
import constantbeta.com.gloshoconductor.messaging.WebSocketWrapper;
import constantbeta.com.gloshoconductor.viewstate.ViewStateManager;

public class ConductorFragment extends Fragment implements View.OnClickListener, WebSocketWrapper.Listener, CameraWrapper.Listener
{
    private final BackgroundThread backgroundThread = new BackgroundThread("CameraBackground");

    private WebSocketWrapper webSocketWrapper;

    private final Timer timer = new Timer();

    private CameraWrapper cameraWrapper;

    private CameraPermissions cameraPermissions;

    private TextureView textureView;
    private final ViewStateManager viewStateManager = ViewStateManager.get();

    private String imageProcessorType;
    private ImageProcessor imageProcessor;

    private boolean takingPictures;

    // package scope
    static ConductorFragment newInstance()
    {
        return new ConductorFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_conductor, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        webSocketWrapper = new WebSocketWrapper(getString(R.string.server_uri), this);
        imageProcessorType = getString(R.string.image_processor_type);
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
        cameraWrapper = new CameraWrapper(this, this, backgroundThread.handler());
        openCamera();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        cameraWrapper.close();
        webSocketWrapper.close();
        backgroundThread.stop();
    }

    @Override
    public void onClick(View v)
    {
        setViewState(ViewStateManager.States.WAITING_TO_START);
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
        setViewState(ViewStateManager.States.LOGGING_IN);
        timer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                webSocketWrapper.login(cameraWrapper.getImageSize(), imageProcessorType);
            }
        }, getResources().getInteger(R.integer.login_delay));

    }

    @Override
    public void onUnableToConnect()
    {
        setViewState(ViewStateManager.States.UNABLE_TO_CONNECT);
    }

    @Override
    public void onLoggedIn()
    {
        setViewState((ViewStateManager.States.READY_TO_START));
    }

    @Override
    public void onTakePicture()
    {
        setViewState(ViewStateManager.States.TAKING_PICTURE);
        cameraWrapper.takePicture();
    }

    @Override
    public void onStartTakingPictures()
    {
        setViewState(ViewStateManager.States.TAKING_PICTURE);
        takingPictures = true;
        cameraWrapper.startTakingPictures();
    }

    @Override
    public void onStopTakingPictures()
    {
        cameraWrapper.stopTakingPictures();
        takingPictures = false;
        setViewState(ViewStateManager.States.DONE);
    }

    @Override
    public void onPictureSent()
    {
        if (takingPictures)
        {
            setViewState(ViewStateManager.States.TAKING_PICTURE);
        }
        else
        {
            setViewState(ViewStateManager.States.WAITING_FOR_COMMAND);
        }
    }

    @Override
    public void onPlayerCountUpdated(final int playerCount)
    {
        getActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                viewStateManager.setPlayerCount(playerCount);
            }
        });
    }

    @Override
    public void onStartingIn(int seconds)
    {
        setStartingInSeconds(seconds);
    }

    @Override
    public void onRunning()
    {
        setViewState(ViewStateManager.States.WAITING_FOR_COMMAND);
    }

    @Override
    public void onDone()
    {
        setViewState(ViewStateManager.States.DONE);
    }

    @Override
    public void onCameraOpened()
    {
        imageProcessor = ImageProcessorFactory.create(imageProcessorType, cameraWrapper.getImageSize());
        setViewState(ViewStateManager.States.CONNECTING);
        timer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                webSocketWrapper.open();
            }
        }, getResources().getInteger(R.integer.connect_delay));
    }

    @Override
    public void onPictureTaken(Image image)
    {
        setViewState(ViewStateManager.States.SENDING_PICTURE);
        webSocketWrapper.sendProcessedImage(imageProcessor.encode(image));
    }

    private void setViewState(final int state)
    {
        getActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                viewStateManager.setState(state);
            }
        });
    }

    private void setStartingInSeconds(final int seconds)
    {
        getActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                viewStateManager.setStartingInSeconds(seconds);
                viewStateManager.setState(ViewStateManager.States.STARTING_IN);
            }
        });
    }
}
