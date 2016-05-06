package constantbeta.com.gloshoconductor.imageprocessors;

import android.media.Image;
import android.util.Log;
import android.util.Size;

import java.nio.ByteBuffer;

// package scope -- instantiate with factory
class ImageProcessorThresholdedBits extends ImageProcessorBase implements ImageProcessor
{
    private static final String TAG        = "IPThresholdedBits";
    private final ByteBuffer    bytes;

    private static final int THRESHOLD     = 240;

    ImageProcessorThresholdedBits(Size size)
    {
        super(size);
        this.bytes = ByteBuffer.allocateDirect(numImagePixels() / 8);
    }

    @Override
    public ByteBuffer encode(Image image)
    {
        Log.d(TAG, "processing");

        final ByteBuffer srcBuffer = image.getPlanes()[0].getBuffer();
        ImageProcessorNative.encodeThresholdedBits(srcBuffer, bytes, numImagePixels(), THRESHOLD);

        return bytes;
    }
}
