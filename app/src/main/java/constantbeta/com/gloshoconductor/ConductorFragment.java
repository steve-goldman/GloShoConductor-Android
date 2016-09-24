package constantbeta.com.gloshoconductor;


import android.app.Fragment;
import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import constantbeta.com.gloshoconductor.camera.CameraPermissions;
import constantbeta.com.gloshoconductor.camera.CameraWrapper;
import constantbeta.com.gloshoconductor.imageprocessors.ImageProcessor;
import constantbeta.com.gloshoconductor.imageprocessors.ImageProcessorFactory;
import constantbeta.com.gloshoconductor.messaging.WebSocketWrapper;
import constantbeta.com.gloshoconductor.preferences.PreferencesWrapper;
import constantbeta.com.gloshoconductor.preferences.SettingsActivity;
import constantbeta.com.gloshoconductor.viewstate.ViewStateManager;

public class ConductorFragment extends Fragment implements WebSocketWrapper.Listener, CameraWrapper.Listener
{
    private final BackgroundThread backgroundThread = new BackgroundThread("CameraBackground");

    private WebSocketWrapper webSocketWrapper;

    private final Timer timer = new Timer();

    private CameraWrapper cameraWrapper;

    private CameraPermissions cameraPermissions;

    private TextureView textureView;
    private final ViewStateManager viewStateManager = ViewStateManager.get();

    private ImageProcessor imageProcessor;

    private boolean takingPictures;

    private PreferencesWrapper prefs;

    private final AtomicBoolean isConnected = new AtomicBoolean();

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
        textureView = (TextureView)view.findViewById(R.id.texture);
        prefs = new PreferencesWrapper(getActivity(),
                                       PreferenceManager.getDefaultSharedPreferences(getActivity()));
        viewStateManager.init(view);
        view.findViewById(R.id.ready_to_start_button).setOnClickListener(readyToStartClickListener);
        view.findViewById(R.id.reconnect_button).setOnClickListener(reconnectClickListener);
        view.findViewById(R.id.disconnect_button).setOnClickListener(disconnectClickListener);
        view.findViewById(R.id.settings_button).setOnClickListener(settingsClickListener);
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
        openCamera();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        cameraWrapper.close();
        closeWebSocketWrapper();
        backgroundThread.stop();
    }

    private void closeWebSocketWrapper()
    {
        if (null != webSocketWrapper)
        {
            isConnected.set(false);
            webSocketWrapper.close();
            stopTakingPictures();
        }
    }

    private final View.OnClickListener readyToStartClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            setViewState(ViewStateManager.States.WAITING_TO_START);
            webSocketWrapper.sendReady();
        }
    };

    private final View.OnClickListener reconnectClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            connect();
        }
    };

    private final View.OnClickListener disconnectClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            isConnected.set(false);
            webSocketWrapper.close();
            setViewState(ViewStateManager.States.NOT_CONNECTED);
        }
    };

    private final View.OnClickListener settingsClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
        }
    };

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
            cameraWrapper = new CameraWrapper(this, this,
                                              backgroundThread.handler(),
                                              prefs.getResolution());
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
                webSocketWrapper.login(cameraWrapper.getImageSize(),
                                       prefs.getImageProcessorType(),
                                       prefs.getThreshold(),
                                       prefs.getExpectedPlayerCount());
            }
        }, getResources().getInteger(R.integer.login_delay));

    }

    @Override
    public void onDisconnected()
    {
        setViewState(ViewStateManager.States.NOT_CONNECTED);
        stopTakingPictures();
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
        isConnected.set(true);
    }

    @Override
    public void onUnableToLogIn()
    {
        setViewState(ViewStateManager.States.UNABLE_TO_LOGIN);
        webSocketWrapper.close();
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
        setViewState(ViewStateManager.States.TAKING_PICTURES);
        takingPictures = true;
        cameraWrapper.startTakingPictures();
    }

    @Override
    public void onStopTakingPictures()
    {
        stopTakingPictures();
        setViewState(ViewStateManager.States.DONE);
    }

    @Override
    public void onPictureSent()
    {
        if (!takingPictures)
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
    public void onFoundPlayerCountUpdated(final int foundPlayerCount)
    {
        getActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                viewStateManager.setFoundPlayerCount(foundPlayerCount);
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
        imageProcessor = ImageProcessorFactory.create(getActivity(),
                                                      prefs.getImageProcessorType(),
                                                      cameraWrapper.getImageSize(),
                                                      prefs.getThreshold());
        connect();
    }

    @Override
    public void onPictureTaken(Image image)
    {
        if (isConnected.get())
        {
            if (!takingPictures)
            {
                setViewState(ViewStateManager.States.SENDING_PICTURE);
            }
            webSocketWrapper.sendProcessedImage(imageProcessor.encode(image));
        }
    }

    private void connect()
    {
        setViewState(ViewStateManager.States.CONNECTING);
        webSocketWrapper = new WebSocketWrapper(prefs.getServerUrl(),
                                                prefs.getInstallationId(),
                                                this);
        timer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                webSocketWrapper.open();
            }
        }, getResources().getInteger(R.integer.connect_delay));
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

    private void stopTakingPictures()
    {
        cameraWrapper.stopTakingPictures();
        takingPictures = false;
    }
}
