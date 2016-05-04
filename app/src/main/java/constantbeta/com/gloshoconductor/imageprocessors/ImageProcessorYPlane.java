package constantbeta.com.gloshoconductor.imageprocessors;

import android.media.Image;
import android.util.Log;
import android.util.Size;

import java.nio.ByteBuffer;

// package scope -- instantiate with factory
class ImageProcessorYPlane extends ImageProcessorBase implements ImageProcessor
{
    private static final String TAG = "IPYPlane";
    private final byte[] bytes;

    ImageProcessorYPlane(Size size)
    {
        super(size);
        bytes = new byte[size.getWidth() * size.getHeight()];
    }

    @Override
    public byte[] encode(Image image)
    {
        Log.d(TAG, "processing");
        final ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        buffer.get(bytes);
        return bytes;
    }
}
