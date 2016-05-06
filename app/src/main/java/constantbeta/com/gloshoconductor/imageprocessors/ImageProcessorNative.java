package constantbeta.com.gloshoconductor.imageprocessors;

import java.nio.ByteBuffer;

public class ImageProcessorNative
{
    static
    {
        System.loadLibrary("image-processor");
    }

    // pixels switch to black or white on whether they equal or
    // exceed the given threshold
    public static native void encodeThresholded(ByteBuffer src,
                                                ByteBuffer dest,
                                                int size,
                                                int threshold);

    // same as encodeThreshold except pixels are represented by
    // bits.  dest should be 1/8 the size of src
    public static native void encodeThresholdedBits(ByteBuffer src,
                                                    ByteBuffer dest,
                                                    int size,
                                                    int threshold);

    // series of ints that are the lengths of alternating states,
    // starting with OFF.  ints are encoded with stop-bit encoding
    public static native int encodeThresholdedDeltaDistances(ByteBuffer src,
                                                             ByteBuffer dest,
                                                             int size,
                                                             int threshold);
}
