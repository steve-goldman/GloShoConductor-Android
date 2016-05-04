package constantbeta.com.gloshoconductor.imageprocessors;

import android.util.Size;

public class ImageProcessorFactory
{
    public class Types
    {
        public static final String YPlane                    = "y-plane";
        public static final String Thresholded               = "thresholded";
        public static final String ThresholdedBits           = "thresholded-bits";
        public static final String ThresholdedDeltaDistances = "thresholded-delta-distances";
    }

    public static ImageProcessor create(String type, Size size)
    {
        if (Types.YPlane.equals(type))
        {
            return new ImageProcessorYPlane(size);
        }
        else if (Types.Thresholded.equals(type))
        {
            return new ImageProcessorThresholded(size);
        }
        else if (Types.ThresholdedBits.equals(type))
        {
            return new ImageProcessorThresholdedBits(size);
        }
        else if (Types.ThresholdedDeltaDistances.equals(type))
        {
            return new ImageProcessorThresholdedDeltaDistances(size);
        }
        throw new IllegalArgumentException("unexpected image processor type: " + type);
    }
}
