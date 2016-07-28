package constantbeta.com.gloshoconductor.imageprocessors;

import android.media.Image;

import java.nio.ByteBuffer;

public interface ImageProcessor
{
    int THRESHOLD = 224;

    ByteBuffer encode(Image image);
}
