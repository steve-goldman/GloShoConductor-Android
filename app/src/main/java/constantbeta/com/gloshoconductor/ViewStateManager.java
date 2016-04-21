package constantbeta.com.gloshoconductor;

import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViewStateManager
{
    public class States
    {
        public static final int CONNECTING = 1;
        public static final int LOGGING_IN = 2;
        public static final int READY_TO_START = 3;
    }

    private static ViewStateManager instance = new ViewStateManager();

    public static ViewStateManager get()
    {
        return instance;
    }

    private ViewStateManager()
    {
    }

    public void init(View view)
    {
        connectingTextView.view = view.findViewById(R.id.connecting_text_view);
        loggingInTextView.view = view.findViewById(R.id.logging_in_text_view);
        readyButton.view = view.findViewById(R.id.ready_to_start_button);
        disappearAll();
    }

    public void setState(int state)
    {
        if (OnViews.containsKey(state))
        {
            disappearAll();
            for (ViewHolder viewHolder : OnViews.get(state))
            {
                viewHolder.view.setVisibility(View.VISIBLE);
            }
        }
        else
        {
            Log.e(TAG, "invalid state: " + state);
        }
    }

    private static class ViewHolder
    {
        ViewHolder()
        {
            allViewHolders.add(this);
        }

        View view;
    }

    private static final String TAG = "ViewStateManager";
    private static final List<ViewHolder> allViewHolders = new ArrayList<>();
    private static final ViewHolder connectingTextView = new ViewHolder();
    private static final ViewHolder loggingInTextView = new ViewHolder();
    private static final ViewHolder readyButton = new ViewHolder();

    private static final Map<Integer, ViewHolder[]> OnViews = new HashMap<>();
    static
    {
        OnViews.put(States.CONNECTING, new ViewHolder[] { connectingTextView });
        OnViews.put(States.LOGGING_IN, new ViewHolder[] { loggingInTextView });
        OnViews.put(States.READY_TO_START, new ViewHolder[] { readyButton });
    }

    private void disappearAll()
    {
        for (ViewHolder viewHolder : allViewHolders)
        {
            viewHolder.view.setVisibility(View.GONE);
        }
    }
}
