package constantbeta.com.gloshoconductor.viewstate;

import android.view.View;
import android.widget.TextView;

import constantbeta.com.gloshoconductor.R;

public class ViewStateManager extends ViewStateManagerBase
{
    public class States
    {
        public static final int CONNECTING            = 1;
        public static final int UNABLE_TO_CONNECT     = 2;
        public static final int LOGGING_IN            = 3;
        public static final int READY_TO_START        = 4;
        public static final int WAITING_TO_START      = 5;
        public static final int STARTING_IN           = 6;
        public static final int WAITING_FOR_COMMAND   = 7;
        public static final int TAKING_PICTURE        = 8;
        public static final int SENDING_PICTURE       = 9;
    }

    private static ViewStateManager instance = new ViewStateManager();

    public static ViewStateManager get()
    {
        return instance;
    }

    private ViewStateManager()
    {
    }

    private static final ViewHolder connectingTextView        = new ViewHolder();
    private static final ViewHolder unableToConnectTextView   = new ViewHolder();
    private static final ViewHolder loggingInTextView         = new ViewHolder();
    private static final ViewHolder readyButton               = new ViewHolder();
    private static final ViewHolder waitingToStartTextView    = new ViewHolder();
    private static final ViewHolder startingInTextView        = new ViewHolder();
    private static final ViewHolder waitingForCommandTextView = new ViewHolder();
    private static final ViewHolder takingPictureTextView     = new ViewHolder();
    private static final ViewHolder sendingPictureTextView    = new ViewHolder();
    private static final ViewHolder playerCountTextView       = new ViewHolder();

    static
    {
        setVisibleViews(States.CONNECTING,          new ViewHolder[] { connectingTextView });
        setVisibleViews(States.UNABLE_TO_CONNECT,   new ViewHolder[] { unableToConnectTextView });
        setVisibleViews(States.LOGGING_IN,          new ViewHolder[] { loggingInTextView });
        setVisibleViews(States.READY_TO_START,      new ViewHolder[] { readyButton, playerCountTextView });
        setVisibleViews(States.WAITING_TO_START,    new ViewHolder[] { waitingToStartTextView });
        setVisibleViews(States.STARTING_IN,         new ViewHolder[] { startingInTextView });
        setVisibleViews(States.WAITING_FOR_COMMAND, new ViewHolder[] { waitingForCommandTextView });
        setVisibleViews(States.TAKING_PICTURE,      new ViewHolder[] { takingPictureTextView });
        setVisibleViews(States.SENDING_PICTURE,     new ViewHolder[] { sendingPictureTextView });
    }

    @Override
    public void init(View view)
    {
        connectingTextView.view        = view.findViewById(R.id.connecting_text_view);
        unableToConnectTextView.view   = view.findViewById(R.id.unable_to_connect_text_view);
        loggingInTextView.view         = view.findViewById(R.id.logging_in_text_view);
        readyButton.view               = view.findViewById(R.id.ready_to_start_button);
        waitingToStartTextView.view    = view.findViewById(R.id.waiting_to_start_text_view);
        startingInTextView.view        = view.findViewById(R.id.starting_in_text_view);
        waitingForCommandTextView.view = view.findViewById(R.id.waiting_for_command_text_view);
        takingPictureTextView.view     = view.findViewById(R.id.taking_picture_text_view);
        sendingPictureTextView.view    = view.findViewById(R.id.sending_picture_text_view);
        playerCountTextView.view       = view.findViewById(R.id.player_count_text_view);
        disappearAll();
    }

    public void setPlayerCount(int playerCount)
    {
        ((TextView)playerCountTextView.view).setText(
                playerCount + (playerCount == 1 ? " player" : " players"));
    }

    public void setStartingInSeconds(int seconds)
    {
        ((TextView)startingInTextView.view).setText(
                "Starting in " + seconds + (seconds == 1 ? " second" : " seconds"));
    }
}
