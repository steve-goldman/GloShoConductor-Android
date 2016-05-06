package constantbeta.com.gloshoconductor.imageprocessors;

import android.util.Size;

// package scope -- instantiate via subclasses
abstract class ImageProcessorBase
{
    private final Size size;

    ImageProcessorBase(Size size)
    {
        this.size = size;
    }

    protected int numImagePixels()
    {
        return size.getWidth() * size.getHeight();
    }
}
