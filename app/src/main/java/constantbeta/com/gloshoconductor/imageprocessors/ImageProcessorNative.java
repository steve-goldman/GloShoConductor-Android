package constantbeta.com.gloshoconductor.imageprocessors;

import java.nio.ByteBuffer;

public class ImageProcessorNative
{
    static
    {
        System.loadLibrary("image-processor");
    }

    public static native void encodeThresholded(ByteBuffer src,
                                                ByteBuffer dest,
                                                int size,
                                                int threshold);
}
