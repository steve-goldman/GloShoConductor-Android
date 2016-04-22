package constantbeta.com.gloshoconductor;

import android.os.Handler;
import android.os.HandlerThread;

public class BackgroundThread
{
    private final String name;

    private HandlerThread thread;
    private Handler handler;

    public BackgroundThread(String name)
    {
        this.name = name;
    }

    public void start()
    {
        thread = new HandlerThread(name);
        thread.start();
        handler = new Handler(thread.getLooper());
    }

    public void stop()
    {
        thread.quitSafely();
        try
        {
            thread.join();
            thread = null;
            handler = null;
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    public Handler handler()
    {
        return handler;
    }

}
