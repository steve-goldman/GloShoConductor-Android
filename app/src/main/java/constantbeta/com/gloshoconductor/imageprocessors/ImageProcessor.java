package constantbeta.com.gloshoconductor.imageprocessors;

import android.media.Image;

import java.nio.ByteBuffer;

public interface ImageProcessor
{
    ByteBuffer encode(Image image);
}
