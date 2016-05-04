package constantbeta.com.gloshoconductor.imageprocessors;

import android.media.Image;
import android.util.Log;
import android.util.Size;

import java.nio.ByteBuffer;

// package scope -- instantiate with factory
class ImageProcessorThresholded extends ImageProcessorBase implements ImageProcessor
{
    private static final String TAG        = "IPThresholded";
    private final byte[] bytes;

    private static final int THRESHOLD     = 240;
    private static final byte ON_BYTE      = (byte)0xFF;
    private static final byte OFF_BYTE     = 0;

    ImageProcessorThresholded(Size size)
    {
        super(size);
        bytes = new byte[size.getWidth() * size.getHeight()];
    }

    @Override
    public byte[] encode(Image image)
    {
        Log.d(TAG, "processing");
        final ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        for (int i = 0; i < bytes.length; i++)
        {
            bytes[i] = toInt(buffer.get()) >= THRESHOLD ? ON_BYTE : OFF_BYTE;
        }
        return bytes;
    }

    private int toInt(byte b)
    {
        return b & 0xFF;
    }
}
