package constantbeta.com.gloshoconductor.imageprocessors;

import android.media.Image;
import android.util.Log;
import android.util.Size;

import java.nio.ByteBuffer;

// package scope -- instantiate with factory
class ImageProcessorThresholdedBits extends ImageProcessorBase implements ImageProcessor
{
    private static final String TAG        = "IPThresholdedBits";
    private final byte[] bytes;

    private static final int THRESHOLD     = 240;

    ImageProcessorThresholdedBits(Size size)
    {
        super(size);
        this.bytes = new byte[size.getWidth() * size.getHeight() / 8];
    }

    @Override
    public byte[] encode(Image image)
    {
        Log.d(TAG, "processing");
        final ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        for (int i = 0; i < bytes.length; i++)
        {
            byte b = 0;
            if (isOn(buffer.get()))
            {
                b |= 0b0000_0001;
            }
            if (isOn(buffer.get()))
            {
                b |= 0b0000_0010;
            }
            if (isOn(buffer.get()))
            {
                b |= 0b0000_0100;
            }
            if (isOn(buffer.get()))
            {
                b |= 0b0000_1000;
            }
            if (isOn(buffer.get()))
            {
                b |= 0b0001_0000;
            }
            if (isOn(buffer.get()))
            {
                b |= 0b0010_0000;
            }
            if (isOn(buffer.get()))
            {
                b |= 0b0100_0000;
            }
            if (isOn(buffer.get()))
            {
                b |= 0b1000_0000;
            }
            bytes[i] = b;
        }
        return bytes;
    }

    private boolean isOn(byte b)
    {
        return (b & 0xFF) >= THRESHOLD;
    }
}
