package constantbeta.com.gloshoconductor.imageprocessors;

import android.media.Image;
import android.util.Log;
import android.util.Size;

import java.nio.ByteBuffer;

// package scope -- instantiate with factory
class ImageProcessorThresholdedDeltaDistances extends ImageProcessorBase implements ImageProcessor
{
    private static final      String TAG        = "IPDeltaDistances";
    private final ByteBuffer  bytes;

    private static final int  THRESHOLD    = 240;

    ImageProcessorThresholdedDeltaDistances(Size size)
    {
        super(size);
        this.bytes = ByteBuffer.allocateDirect(size.getWidth() * size.getHeight());
    }

    private void writeCount(int count)
    {
        while (count > 127)
        {
            bytes.put((byte)(0x80 | (count & 0x7F)));
            count >>>= 7;
        }
        bytes.put((byte)count);
    }

    @Override
    public ByteBuffer encode(Image image)
    {
        Log.d(TAG, "processing");

        final ByteBuffer buffer = image.getPlanes()[0].getBuffer();

        bytes.clear();

        boolean curIsOn  = false;
        int     curCount = 0;

        while (buffer.hasRemaining())
        {
            final boolean isOn = isOn(buffer.get());
            if (!curIsOn && isOn)
            {
                writeCount(curCount);
                curIsOn = true;
                curCount = 1;
            }
            else if (curIsOn && !isOn)
            {
                writeCount(curCount);
                curIsOn = false;
                curCount = 1;
            }
            else
            {
                curCount++;
            }
        }
        writeCount(curCount);

        bytes.flip();
        return bytes;
    }

    private boolean isOn(byte b)
    {
        return (b & 0xFF) >= THRESHOLD;
    }
}
