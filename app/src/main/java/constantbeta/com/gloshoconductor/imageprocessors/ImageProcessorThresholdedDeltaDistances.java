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

    ImageProcessorThresholdedDeltaDistances(Size size)
    {
        super(size);
        this.bytes = ByteBuffer.allocateDirect(numImagePixels());
    }

    @Override
    public ByteBuffer encode(Image image)
    {
        Log.d(TAG, "processing");

        final ByteBuffer srcBuffer = image.getPlanes()[0].getBuffer();
        bytes.limit(ImageProcessorNative.encodeThresholdedDeltaDistances(srcBuffer, bytes, numImagePixels(), THRESHOLD));
        return bytes;
    }
}
