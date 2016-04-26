package constantbeta.com.gloshoconductor.imageprocessors;

import android.media.Image;

public interface ImageProcessor
{
    byte[] encode(Image image);
}
