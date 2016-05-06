package constantbeta.com.gloshoconductor.imageprocessors;

import android.media.Image;
import android.util.Log;
import android.util.Size;

import java.nio.ByteBuffer;

// package scope -- instantiate with factory
class ImageProcessorThresholded extends ImageProcessorBase implements ImageProcessor
{
    static
    {
        System.loadLibrary("image-processor-thresholded");
    }

    private native void encode(ByteBuffer src, ByteBuffer dest, int size, int threshold);

    private static final String TAG        = "IPThresholded";
    private final ByteBuffer    bytes;
    private final int           size;

    private static final int    THRESHOLD  = 240;

    ImageProcessorThresholded(Size size)
    {
        super(size);
        this.size = size.getWidth() * size.getHeight();
        this.bytes = ByteBuffer.allocateDirect(this.size);
    }

    @Override
    public ByteBuffer encode(Image image)
    {
        Log.d(TAG, "processing");

        final ByteBuffer srcBuffer = image.getPlanes()[0].getBuffer();
        encode(srcBuffer, bytes, size, THRESHOLD);

        return bytes;
    }
}
