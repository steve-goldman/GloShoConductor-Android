package constantbeta.com.gloshoconductor.imageprocessors;

import android.util.Size;

public class ImageProcessorFactory
{
    public class Types
    {
        public static final String YPlane = "y-plane";
    }

    public static ImageProcessor create(String type, Size size)
    {
        if (Types.YPlane.equals(type))
        {
            return new ImageProcessorYPlane(size);
        }
        throw new IllegalArgumentException("unexpected image processor type: " + type);
    }
}
