package constantbeta.com.gloshoconductor.imageprocessors;

import android.content.Context;
import android.util.Size;

import constantbeta.com.gloshoconductor.R;

public class ImageProcessorFactory
{
    public static ImageProcessor create(Context context, String type, Size size, int threshold)
    {
        if (getKey(context, R.string.y_plane).equals(type))
        {
            return new ImageProcessorYPlane(size);
        }
        else if (getKey(context, R.string.thresholded).equals(type))
        {
            return new ImageProcessorThresholded(size, threshold);
        }
        else if (getKey(context, R.string.thresholded_bits).equals(type))
        {
            return new ImageProcessorThresholdedBits(size, threshold);
        }
        else if (getKey(context, R.string.thresholded_delta_distances).equals(type))
        {
            return new ImageProcessorThresholdedDeltaDistances(size, threshold);
        }
        throw new IllegalArgumentException("unexpected image processor type: " + type);
    }

    private static String getKey(Context context, int id)
    {
        return context.getResources().getString(id);
    }
}
