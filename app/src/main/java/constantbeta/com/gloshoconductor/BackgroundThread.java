package constantbeta.com.gloshoconductor;

import android.os.Handler;
import android.os.HandlerThread;

// package scope
class BackgroundThread
{
    private final String name;

    private HandlerThread thread;
    private Handler handler;

    BackgroundThread(String name)
    {
        this.name = name;
    }

    void start()
    {
        thread = new HandlerThread(name);
        thread.start();
        handler = new Handler(thread.getLooper());
    }

    void stop()
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

    Handler handler()
    {
        return handler;
    }

}
