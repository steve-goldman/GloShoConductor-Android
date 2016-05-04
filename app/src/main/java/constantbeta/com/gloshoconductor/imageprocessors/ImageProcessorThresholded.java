package constantbeta.com.gloshoconductor.imageprocessors;

import android.media.Image;
import android.util.Log;
import android.util.Size;

import java.nio.ByteBuffer;

// package scope -- instantiate with factory
class ImageProcessorThresholded extends ImageProcessorBase implements ImageProcessor
{
    private static final String TAG        = "IPThresholded";
    private final ByteBuffer bytes;

    private static final int THRESHOLD     = 240;
    private static final byte ON_BYTE      = (byte)0xFF;
    private static final byte OFF_BYTE     = 0;

    ImageProcessorThresholded(Size size)
    {
        super(size);
        this.bytes = ByteBuffer.allocateDirect(size.getWidth() * size.getHeight());
    }

    @Override
    public ByteBuffer encode(Image image)
    {
        Log.d(TAG, "processing");

        final ByteBuffer srcBuffer      = image.getPlanes()[0].getBuffer();
        final byte[]     destArray      = bytes.array();
        final int        destBaseOffset = bytes.arrayOffset();

        for (int i = 0; i < bytes.capacity() / 8; i++)
        {
            final long l         = srcBuffer.getLong();
            final int baseOffset = destBaseOffset + 8 * i;

            destArray[baseOffset]     = thresholded(l);
            destArray[1 + baseOffset] = thresholded(l >>> 8);
            destArray[2 + baseOffset] = thresholded(l >>> 16);
            destArray[3 + baseOffset] = thresholded(l >>> 24);
            destArray[4 + baseOffset] = thresholded(l >>> 32);
            destArray[5 + baseOffset] = thresholded(l >>> 40);
            destArray[6 + baseOffset] = thresholded(l >>> 48);
            destArray[7 + baseOffset] = thresholded(l >>> 56);
        }

        bytes.position(0).limit(bytes.capacity());
        return bytes;
    }

    private byte thresholded(long b)
    {
        return (b & 0xFF) >= THRESHOLD ? ON_BYTE : OFF_BYTE;
    }
}
