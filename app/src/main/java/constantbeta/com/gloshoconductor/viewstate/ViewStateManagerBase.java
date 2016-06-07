package constantbeta.com.gloshoconductor.viewstate;

import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract class ViewStateManagerBase
{
    private static final String TAG = "ViewStateManagerBase";

    private int state = -1;

    public void setState(int state)
    {
        if (VisibleViews.containsKey(state))
        {
            disappearAll();
            for (ViewHolder viewHolder : VisibleViews.get(state))
            {
                viewHolder.view.setVisibility(View.VISIBLE);
            }

            this.state = state;
        }
        else
        {
            Log.e(TAG, "invalid state: " + state);
        }
    }

    public int getState()
    {
        return state;
    }

    public abstract void init(View view);

    protected void disappearAll()
    {
        for (ViewHolder viewHolder : allViewHolders)
        {
            viewHolder.view.setVisibility(View.GONE);
        }
    }

    protected static class ViewHolder
    {
        ViewHolder()
        {
            allViewHolders.add(this);
        }

        View view;
    }

    protected static void setVisibleViews(int state, ViewHolder[] views)
    {
        VisibleViews.put(state, views);
    }

    private static final Map<Integer, ViewHolder[]> VisibleViews = new HashMap<>();
    private static final List<ViewHolder> allViewHolders = new ArrayList<>();
}
