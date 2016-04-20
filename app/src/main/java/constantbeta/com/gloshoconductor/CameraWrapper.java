package constantbeta.com.gloshoconductor;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

// package scope
class CameraWrapper
{
    private static final String TAG = "CameraWrapper";

    private final CameraFragment fragment;
    private final Handler backgroundHandler;
    private final Semaphore cameraLock = new Semaphore(1);

    private CameraManager manager;
    private String cameraId;
    private ImageReader imageReader;

    private CameraCaptureSession captureSession;
    private CameraDevice device;
    private Size previewSize;

    CameraWrapper(CameraFragment fragment, Handler backgroundHandler)
    {
        this.fragment = fragment;
        this.backgroundHandler = backgroundHandler;
    }

    void openCamera()
    {
        Log.d(TAG, "entering openCamera");
        final Activity activity = fragment.getActivity();
        manager = (CameraManager)activity.getSystemService(Context.CAMERA_SERVICE);

        setupOutputs(backgroundHandler);
        configureTransform(fragment.getTextureView().getWidth(), fragment.getTextureView().getHeight());

        try
        {
            if (!cameraLock.tryAcquire(2500, TimeUnit.MILLISECONDS))
            {
                throw new RuntimeException("timed out locking camera opening");
            }
            manager.openCamera(cameraId, stateCallback, backgroundHandler);
        }
        catch (CameraAccessException | SecurityException e)
        {
            e.printStackTrace();
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException("interrupted while trying to lock camera opening");
        }
        Log.d(TAG, "exiting openCamera");
    }

    void closeCamera()
    {
        Log.d(TAG, "entering closeCamera");
        try
        {
            cameraLock.acquire();
            if (null != captureSession)
            {
                captureSession.close();
                captureSession = null;
            }
            if (null != device)
            {
                device.close();
                device = null;
            }
            if (null != imageReader)
            {
                imageReader.close();
                imageReader = null;
            }
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException("interrupted while trying to lock camera closing");
        }
        finally
        {
            cameraLock.release();
        }
        Log.d(TAG, "exiting closeCamera");
    }

    private void setupOutputs(Handler backgroundHandler)
    {
        Log.d(TAG, "entering setupOutputs");
        try
        {
            for (final String cameraId : manager.getCameraIdList())
            {
                final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing == null || facing == CameraCharacteristics.LENS_FACING_FRONT)
                {
                    continue;
                }

                final StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null)
                {
                    continue;
                }

                final Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.YUV_420_888)),
                        new CompareSizesByArea());

                imageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.YUV_420_888, 1);
                imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener()
                {
                    @Override
                    public void onImageAvailable(ImageReader reader)
                    {
                        // TODO -- post something to the background handler
                    }
                }, backgroundHandler);

                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
                int displayRotation = fragment.getActivity().getWindowManager().getDefaultDisplay().getRotation();
                int sensorOrientation =
                        characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (sensorOrientation == 90 || sensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (sensorOrientation == 0 || sensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                }

                final int width = fragment.getTextureView().getWidth();
                final int height = fragment.getTextureView().getHeight();

                Point displaySize = new Point();
                fragment.getActivity().getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                if (swappedDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                                                 rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                                                 maxPreviewHeight, largest);

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                /*
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    fragment.getTextureView().setAspectRatio(
                            mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }
                */
                this.cameraId = cameraId;

                Log.d(TAG, "exiting setupOutputs success");
                return;
            }
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
        Log.d(TAG, "exiting setupOutputs failure");
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight)
    {
        final Activity activity = fragment.getActivity();
        if (null == fragment.getTextureView() || null == previewSize || null == activity)
        {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation)
        {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / previewSize.getHeight(),
                    (float) viewWidth / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        else if (Surface.ROTATION_180 == rotation)
        {
            matrix.postRotate(180, centerX, centerY);
        }
        fragment.getTextureView().setTransform(matrix);
    }

    private void createCameraPreviewSession()
    {
        try
        {
            final SurfaceTexture texture = fragment.getTextureView().getSurfaceTexture();
            assert texture != null;

            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

            final Surface surface = new Surface(texture);
            final CaptureRequest.Builder previewRequestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);

            device.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()), new CameraCaptureSession.StateCallback()
                {
                    @Override
                    public void onConfigured(CameraCaptureSession captureSession)
                    {
                        // check if camera is already closed
                        if (null == device)
                        {
                            return;
                        }

                        // start displaying the preview
                        CameraWrapper.this.captureSession = captureSession;
                        try
                        {
                            // TODO -- autofocus?

                            final CaptureRequest previewRequest = previewRequestBuilder.build();
                            captureSession.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler);
                        }
                        catch (CameraAccessException e)
                        {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onConfigureFailed(CameraCaptureSession session)
                    {

                    }
                }, null);
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }

    private final CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback()
    {
        private void process(CaptureResult result)
        {
            // TODO
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result)
        {
            process(result);
        }
    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback()
    {
        @Override
        public void onOpened(CameraDevice device)
        {
            cameraLock.release();
            CameraWrapper.this.device = device;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(CameraDevice device)
        {
            cameraLock.release();
            device.close();
            CameraWrapper.this.device = null;
        }

        @Override
        public void onError(CameraDevice device, int error)
        {
            cameraLock.release();
            device.close();
            CameraWrapper.this.device = null;
            final Activity activity = fragment.getActivity();
            if (null != activity)
            {
                activity.finish();
            }
        }
    };

    static class CompareSizesByArea implements Comparator<Size>
    {
        @Override
        public int compare(Size lhs, Size rhs)
        {
            // cast to ensure the multiplications won't overflow
            return Long.signum((long)lhs.getWidth() * lhs.getHeight() - (long)rhs.getWidth() * rhs.getHeight());
        }
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio)
    {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w)
            {
                if (option.getWidth() >= textureViewWidth && option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                }
                else
                {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0)
        {
            return Collections.min(bigEnough, new CompareSizesByArea());
        }
        else if (notBigEnough.size() > 0)
        {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        }
        else
        {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }
}
