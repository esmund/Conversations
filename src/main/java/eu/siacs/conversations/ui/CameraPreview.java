package eu.siacs.conversations.ui;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private SurfaceHolder prHolder;
    private Camera prCamera;
    public List<Camera.Size> prSupportedPreviewSizes;
    private Camera.Size prPreviewSize;

    @SuppressWarnings("deprecation")
    public CameraPreview(Context context, Camera camera) {
        super(context);
        prCamera = camera;

        prSupportedPreviewSizes = prCamera.getParameters().getSupportedPreviewSizes();

        prHolder = getHolder();
        prHolder.addCallback(this);
        prHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }


    public void surfaceCreated(SurfaceHolder holder) {
        try {
            prCamera.setPreviewDisplay(holder);
            prCamera.startPreview();
            Log.d("Camera", "Camera behaviour: prCamera.startPreview();");
        } catch (IOException e) {
            Log.d("Yologram", "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        try {
            prCamera.stopPreview();
            prCamera.release();
            Log.d("Camera", "Camera behaviour: prCamera.release();");
            prCamera = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (prHolder.getSurface() == null) {
            return;
        }

        try {
            prCamera.stopPreview();
            Log.d("Camera", "Camera behaviour:  prCamera.stopPreview();");
        } catch (Exception e) {
        }

        try {
            Camera.Parameters parameters = prCamera.getParameters();
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
            parameters.setPreviewSize(prPreviewSize.width, prPreviewSize.height);

            prCamera.setParameters(parameters);
            prCamera.setPreviewDisplay(prHolder);
            prCamera.startPreview();
            Log.d("Camera", "Camera behaviour:  prCamera.startPreview();");
        } catch (Exception e) {
            Log.d("Yologram", "Error starting camera preview: " + e.getMessage());
        }
    }

    public void refreshCamera() {
        if (prHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            prCamera.stopPreview();
            Log.d("Camera", "Camera behaviour:  refreshCamera() prCamera.stopPreview();");

        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }
        try {
            prCamera.setPreviewDisplay(prHolder);
            prCamera.startPreview();
            Log.d("Camera", "Camera behaviour:  refreshCamera() prCamera.startPreview();");

        } catch (Exception e) {

        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        setMeasuredDimension(width, height);

        if (prSupportedPreviewSizes != null) {
            prPreviewSize =
                    getOptimalPreviewSize(prSupportedPreviewSizes, width, height);
        }
    }

    public Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {

        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;

        if (sizes == null)
            return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;

            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        return optimalSize;
    }
}