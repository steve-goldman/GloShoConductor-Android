package constantbeta.com.gloshoconductor.imageprocessors;

import android.util.Size;

// package scope -- instantiate via subclasses
abstract class ImageProcessorBase
{
    protected final Size size;

    ImageProcessorBase(Size size)
    {
        this.size = size;
    }
}
