package constantbeta.com.gloshoconductor.imageprocessors;

import android.media.Image;
import android.util.Log;
import android.util.Size;

import java.nio.ByteBuffer;

// package scope -- instantiate with factory
class ImageProcessorThresholdedBits extends ImageProcessorBase implements ImageProcessor
{
    private static final String TAG        = "IPThresholdedBits";
    private final ByteBuffer bytes;

    private static final int THRESHOLD     = 240;
    private static final byte ON_BYTE      = (byte)0xFF;
    private static final byte OFF_BYTE     = 0;

    ImageProcessorThresholdedBits(Size size)
    {
        super(size);
        this.bytes = ByteBuffer.allocateDirect(size.getWidth() * size.getHeight() / 8);
    }

    @Override
    public ByteBuffer encode(Image image)
    {
        Log.d(TAG, "processing");

        final ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        final byte[] array      = bytes.array();
        final int    baseOffset = bytes.arrayOffset();

        for (int i = 0; i < bytes.capacity(); i++)
        {
            final long l = buffer.getLong();
            final byte b = (byte)((thresholded(l) & 0b0000_0001) |
                    (thresholded(l >>>  8) & 0b0000_0010) |
                    (thresholded(l >>> 16) & 0b0000_0100) |
                    (thresholded(l >>> 24) & 0b0000_1000) |
                    (thresholded(l >>> 32) & 0b0001_0000) |
                    (thresholded(l >>> 40) & 0b0010_0000) |
                    (thresholded(l >>> 48) & 0b0100_0000) |
                    (thresholded(l >>> 56) & 0b1000_0000));

            array[baseOffset + i] = b;
        }

        bytes.position(0).limit(bytes.capacity());
        return bytes;
    }

    private byte thresholded(long b)
    {
        return (b & 0xFF) >= THRESHOLD ? ON_BYTE : OFF_BYTE;
    }
}
