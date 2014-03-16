package com.baixing.facecontact;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.Camera.PreviewCallback;
import android.location.Location;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    
    int mFlag = 0;
    private byte[] mFrameBuffer;
    private Rect mFaceRect = new Rect();
    
    public CameraPreview(Activity context) {
        super(context);
        mCamera = Camera.open();

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
        int degrees = getCameraDisplayOrientation(context, 0, mCamera);
        mCamera.setDisplayOrientation(degrees);
        mCamera.setPreviewCallback(new PreviewCallback(){

			@Override
			public void onPreviewFrame(byte[] data, Camera camera) {
				// TODO Auto-generated method stub
				mFrameBuffer = data.clone();
			}
        	
        });
        this.setWillNotDraw(false);
        
        mCamera.setFaceDetectionListener(new FaceDetectionListener(){

			@Override
			public void onFaceDetection(final Face[] faces, Camera camera) {
				// TODO Auto-generated method stub
				if(faces == null || faces.length == 0){
					mFaceRect.setEmpty();
					invalidate();
				}
		        if (faces.length > 0){		        	
					// TODO Auto-generated method stub        	        
        	        Rect rec = faces[0].rect;
        	        RectF viewRect = cameraToView(rec, CameraPreview.this);
        	        
        	        mFaceRect.left = (int)viewRect.left;
        	        mFaceRect.right = (int)viewRect.right;
        	        mFaceRect.top = (int)viewRect.top;
        	        mFaceRect.bottom = (int)viewRect.bottom;
        	        invalidate();
        	        if(mFlag <= 10){
        	        	++ mFlag;
        	        	viewRect = cameraToView(rec, getRootView());
        	        	viewRect.left -= 50;
        	        	viewRect.right += 50;
        	        	viewRect.top -= 50;
        	        	viewRect.bottom += 50;
        	        	
	        	        File pictureFile = getOutputMediaFile();
	        	        Camera.Parameters parameters = camera.getParameters();   
	        	        int imageFormat = parameters.getPreviewFormat();
	        	        int w = parameters.getPreviewSize().width;    
	        	        int h = parameters.getPreviewSize().height;  
	        	        Rect rect=new Rect(0,0,w,h);  
	        	        YuvImage yuvImg = new YuvImage(mFrameBuffer,imageFormat,w,h,null);  
	        	        try {    
	        	            ByteArrayOutputStream outputstream = new ByteArrayOutputStream();   
	        	            yuvImg.compressToJpeg(rect, 100, outputstream);    
	        	            Bitmap bmp = BitmapFactory.decodeByteArray(outputstream.toByteArray(), 0, outputstream.size());
							if(bmp != null){
								Bitmap crop = Bitmap.createBitmap(bmp, (int)viewRect.left, (int)viewRect.top, (int)viewRect.width(), (int)viewRect.height());
			        	        try {
			        	            FileOutputStream fos = new FileOutputStream(pictureFile);
			        	            crop.compress(CompressFormat.JPEG, 100, fos);
			        	            fos.close();
			        	        } catch (FileNotFoundException e) {
			        	        } catch (IOException e) {
			        	        }
			        	        crop.recycle();
							}
	        	        }catch(Exception e){
	        	        	e.printStackTrace();
	        	        }
        	        }

		            Log.d("FaceDetection", "face detected: "+ faces.length +
		                    " Face 1 Location X: " + faces[0].rect.centerX() +
		                    "Y: " + faces[0].rect.centerY() );
		        	}
				}
        	
        });
    }
        
    
    private static File getOutputMediaFile(){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                  Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_"+ timeStamp + ".jpg");
        return mediaFile;
    }
    
    private RectF cameraToView(Rect rect, View view){
    	CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(0, info);
    	 Matrix matrix = new Matrix();
    	 // Need mirror for front camera.
    	 boolean mirror = (info.facing == CameraInfo.CAMERA_FACING_FRONT);
    	 matrix.setScale(mirror ? -1 : 1, 1);
    	 
    	 // This is the value for android.hardware.Camera.setDisplayOrientation.
    	 int degrees = getCameraDisplayOrientation((Activity)this.getContext(), 0, mCamera);
    	 matrix.postRotate(degrees);
    	 // Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
    	 // UI coordinates range from (0, 0) to (width, height).
    	 matrix.postScale(view.getWidth() / 2000f, view.getHeight() / 2000f);
    	 matrix.postTranslate(view.getWidth() / 2f, view.getHeight() / 2f);
    	 RectF dst =  new RectF();
    	 RectF src = new RectF();
    	 src.left = rect.left;
    	 src.right = rect.right;
    	 src.top = rect.top;
    	 src.bottom = rect.bottom;
    	 matrix.mapRect(dst, src);
    	 return dst;
    }
  
    
    public static int getCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
        CameraInfo info = new android.hardware.Camera.CameraInfo();
        Camera.getCameraInfo(0, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
//        camera.setDisplayOrientation(result);
    }
    
 
    @Override
    protected void onDraw(Canvas canvas) {
//    	super.onDraw(canvas);
//        canvas.drawRect(area, rectanglePaint);
    	if(mFaceRect != null){
			Paint paint =  new Paint();
			paint.setStyle(Style.STROKE);
			paint.setStrokeWidth(5);
			paint.setARGB(255, 0, 122, 0);
			canvas.drawRect(mFaceRect, paint);

    	}

        Log.w(this.getClass().getName(), "On Draw Called");
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            
            mCamera.setPreviewDisplay(holder);
//        	Parameters params = mCamera.getParameters();
//        	List<Size> sizes = params.getSupportedPreviewSizes();
        	
//        	params.setPreviewSize(sizes.get(1).width, sizes.get(1).height);
//        	mCamera.setParameters(params);

        	mCamera.startPreview();
            
        	mCamera.startFaceDetection();
        } catch (IOException e) {
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
        if (mHolder.getSurface() == null){
          // preview surface does not exist
          return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
          // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            
//        	Parameters params = mCamera.getParameters();
//        	List<Size> sizes = params.getSupportedPreviewSizes();
//        	
//        	params.setPreviewSize(sizes.get(0).width, sizes.get(1).height);
            
            mCamera.startPreview();
            mCamera.startFaceDetection();
        } catch (Exception e){
            
        }
    }
}