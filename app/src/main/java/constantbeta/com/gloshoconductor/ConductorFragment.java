package constantbeta.com.gloshoconductor;


import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.Image;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import constantbeta.com.gloshoconductor.camera.CameraPermissions;
import constantbeta.com.gloshoconductor.camera.CameraWrapper;
import constantbeta.com.gloshoconductor.imageprocessors.ImageProcessor;
import constantbeta.com.gloshoconductor.imageprocessors.ImageProcessorFactory;
import constantbeta.com.gloshoconductor.messaging.WebSocketWrapper;
import constantbeta.com.gloshoconductor.viewstate.ViewStateManager;

public class ConductorFragment extends Fragment implements WebSocketWrapper.Listener, CameraWrapper.Listener
{
    @SuppressWarnings("FieldCanBeLocal")
    private final String TAG = "fragment";

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

    private SharedPreferences prefs;
    private static final String SERVER_URL_KEY            = "serverUrl";
    private static final String RESOLUTION_POSITION_KEY   = "selectedResolution";
    private static final String EXPECTED_PLAYER_COUNT_KEY = "expectedPlayerCount";

    private EditText serverUrlEditText;

    private final AtomicBoolean isConnected = new AtomicBoolean();

    private Spinner resolutionSpinner;
    private Size    imageSize;

    private EditText expectedPlayerCountEditText;

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
        imageProcessorType = getString(R.string.image_processor_type);
        textureView = (TextureView)view.findViewById(R.id.texture);
        serverUrlEditText = (EditText)view.findViewById(R.id.server_url_edit_text);
        prefs = getActivity().getSharedPreferences("GloShoConductor", Context.MODE_PRIVATE);
        serverUrlEditText.setText(prefs.getString(SERVER_URL_KEY, getString(R.string.server_url)));
        setupResolutionsSpinner(view);
        setupExpectedPlayerCountEditText(view);
        viewStateManager.init(view);
        view.findViewById(R.id.ready_to_start_button).setOnClickListener(readyToStartClickListener);
        view.findViewById(R.id.reconnect_button).setOnClickListener(reconnectClickListener);
        view.findViewById(R.id.disconnect_button).setOnClickListener(disconnectClickListener);
    }

    private void setupResolutionsSpinner(final View view)
    {
        resolutionSpinner = (Spinner)view.findViewById(R.id.resolution_spinner);
        Size[]   sizes    = CameraWrapper.getImageSizes(getActivity());
        String[] strs     = new String[sizes.length];
        for (int i = 0; i < sizes.length; i++)
        {
            strs[i] = sizes[sizes.length - i - 1].toString();
        }
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(getActivity(), android.R.layout.simple_spinner_item, strs);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        resolutionSpinner.setAdapter(adapter);

        int cachedPosition = prefs.getInt(RESOLUTION_POSITION_KEY, 0);
        if (cachedPosition >= resolutionSpinner.getAdapter().getCount())
        {
            cachedPosition = 0;
        }
        resolutionSpinner.setSelection(cachedPosition);

        updateImageSize();
        resolutionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                resolutionSelected();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
            }
        });
    }

    private void resolutionSelected()
    {
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(RESOLUTION_POSITION_KEY, resolutionSpinner.getSelectedItemPosition());
        editor.apply();

        updateImageSize();
        cameraWrapper.close();
        closeWebSocketWrapper();
        openCamera();
    }

    private void updateImageSize()
    {
        imageSize = Size.parseSize((String)resolutionSpinner.getSelectedItem());
        Log.d(TAG, "set image size: " + imageSize.toString());
    }

    private void setupExpectedPlayerCountEditText(View view)
    {
        expectedPlayerCountEditText = (EditText) view.findViewById(R.id.expected_player_count_edit_text);
        expectedPlayerCountEditText.setText(String.valueOf(prefs.getInt(EXPECTED_PLAYER_COUNT_KEY,
                                                                        100)));
        expectedPlayerCountEditText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
            }

            @Override
            public void afterTextChanged(Editable s)
            {
                Log.d(TAG, "text changed");
                final SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(EXPECTED_PLAYER_COUNT_KEY, getExpectedPlayerCount());
                editor.apply();
            }
        });
    }

    private int getExpectedPlayerCount()
    {
        return Integer.parseInt(expectedPlayerCountEditText.getText().toString());
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
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putString(SERVER_URL_KEY, serverUrlEditText.getText().toString());
            editor.apply();
            final InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            //noinspection ConstantConditions
            imm.hideSoftInputFromWindow(serverUrlEditText.getWindowToken(), 0);
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
            cameraWrapper = new CameraWrapper(this, this, backgroundThread.handler(), imageSize);
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
                webSocketWrapper.login(cameraWrapper.getImageSize(), imageProcessorType, getExpectedPlayerCount());
            }
        }, getResources().getInteger(R.integer.login_delay));

    }

    @Override
    public void onDisconnected()
    {
        // TODO
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
        cameraWrapper.stopTakingPictures();
        takingPictures = false;
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
        imageProcessor = ImageProcessorFactory.create(imageProcessorType, cameraWrapper.getImageSize());
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
        webSocketWrapper = new WebSocketWrapper(serverUrlEditText.getText().toString(), this);
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
}
