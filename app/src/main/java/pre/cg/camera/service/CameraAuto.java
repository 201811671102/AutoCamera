package pre.cg.camera.service;


import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import org.litepal.LitePal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import lombok.SneakyThrows;
import pre.cg.camera.pojo.PictureFile;


public class CameraAuto extends Service {
    private static final String TAG = "CameraAuto";
    private boolean autoCamera = false;
    private long autoTime;
    private CameraDevice cameraDevice;
    private CameraDevice.StateCallback cameraDevice_stateCallback;
    private CameraManager cameraManager;
    private CameraCaptureSession cameraCaptureSession;
    private CameraCaptureSession.StateCallback cameraCaptureSession_stateCallback;
    private CameraCaptureSession.CaptureCallback cameraCaptureSession_captureCallback;
    private CaptureRequest.Builder request_build;
    private String cameraId;
    private Size cameraSize;
    private Handler handler;
    private ImageReader imageReader;
    private TextureView textureView;
    private Surface surface;
    private SurfaceTexture surfaceTexture;
    private boolean cameraON = false;
    private int angleSize;
    private PendingIntent pendingIntent;
    private AlarmManager alarmManager;

    public class MyBind extends Binder {
        public void initPreview(String cameraId, Size cameraSize, TextureView textureView) {
            init(cameraId, cameraSize, textureView);
            initChildThread();
            initCameraManage();
            initImageReader();
            initCameraDeviceStateCallbackListener();
            initCameraCaptureSessionStateCallbackListener();
            initCameraCaptureSessionCaptureCallbackListener();
            initTextureViewListener();
            openCamera();
        }

        public void initPictureAuto(long time,int angle) {
            autoCamera = true;
            autoTime = time;
            angleSize = angle;
        }

        public void switchCamera(String mcameraId) {
            cameraId = mcameraId;
            changeCamera();
        }

        public void shutDownCamera(){
            autoCamera = false;
        }

        public boolean cameraState(){return cameraON;}
    }

    private MyBind myBind = new MyBind();

    public CameraAuto() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return myBind;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        long time = SystemClock.elapsedRealtime() + autoTime*1000;
        Intent intent1 = new Intent(this, CameraAuto.class);
        pendingIntent = PendingIntent.getService(this, 0, intent1, 0);
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, time, pendingIntent);
        if (autoCamera) {
            takePicture();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @SneakyThrows
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (imageReader != null){
            imageReader.close();
            imageReader = null;
        }
        closeCamera();
        alarmManager.cancel(pendingIntent);
        //textureView.getSurfaceTextureListener().onSurfaceTextureDestroyed(textureView.getSurfaceTexture());
        cameraDevice_stateCallback = null;
        cameraCaptureSession_captureCallback = null;
        cameraCaptureSession_stateCallback = null;
        cameraManager = null;
    }

    /*初始化参数*/
    private void init(String cameraId, Size cameraSize, TextureView textureView) {
        this.cameraId = cameraId;
        this.cameraSize = cameraSize;
        this.textureView = textureView;
    }

    /*初始化子线程*/
    private void initChildThread() {
        HandlerThread handlerThread = new HandlerThread("faceCamera");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    /*初始化相机管理*/
    private void initCameraManage() {
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
    }

    /*初始化照片*/
    private void initImageReader() {
        imageReader = ImageReader.newInstance(cameraSize.getWidth(), cameraSize.getHeight(), ImageFormat.JPEG, 2);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();
                ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[byteBuffer.remaining()];
                byteBuffer.get(bytes);
                try {
                    /*开启事务*/
                    LitePal.beginTransaction();
                    StringBuilder path = new StringBuilder();
                    LocalDateTime localDateTime = LocalDateTime.now();
                    path.append("picture/");
                    path.append(localDateTime.getYear() + "/");
                    path.append(localDateTime.getMonthValue() + "/");
                    path.append(localDateTime.getDayOfMonth());

                    File filepath = new File(getExternalFilesDir(path.toString()).getPath());
                    if (!filepath.exists()) {
                        filepath.mkdirs();
                    }
                    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss");
                    String datetime = dateTimeFormatter.format(localDateTime).toString();
                    File file = new File(filepath, datetime + ".jpg");
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    fileOutputStream.write(bytes);
                    fileOutputStream.flush();
                    fileOutputStream.close();
                    image.close();
                    if (file.exists()) {
                        PictureFile pictureFile = new PictureFile();
                        pictureFile.setUrl(file.getPath());
                        pictureFile.setDate(datetime);
                        if (pictureFile.save()) {
                            LitePal.setTransactionSuccessful();
                        }
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    LitePal.endTransaction();
                }
            }
        }, handler);

    }

    /*初始化预览*/
    private void initTextureViewListener() {
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

            }
        });
    }

    /*初始化相机监听*/
    private void initCameraDeviceStateCallbackListener() {
        cameraDevice_stateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                //相机开启
                cameraDevice = camera;
                try {
                    SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
                    surfaceTexture.setDefaultBufferSize(cameraSize.getWidth(), cameraSize.getHeight());
                    surface = new Surface(surfaceTexture);
                    request_build = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    request_build.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, getRange());
                    request_build.set(CaptureRequest.CONTROL_AE_LOCK, false);
                    request_build.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
                    request_build.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF);//关闭自动对焦
                    request_build.addTarget(surface);
                    cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()), cameraCaptureSession_stateCallback, handler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
                cameraON = true;
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                cameraON = false;
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                cameraON = false;
            }
        };
    }

    /*相机状态监听*/
    private void initCameraCaptureSessionStateCallbackListener() {
        cameraCaptureSession_stateCallback = new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                cameraCaptureSession = session;
                try {
                    cameraCaptureSession.setRepeatingRequest(request_build.build(), cameraCaptureSession_captureCallback, handler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
                cameraON = true;
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                cameraON = false;
            }
        };
    }

    /*相机数据监听*/
    private void initCameraCaptureSessionCaptureCallbackListener() {
        cameraCaptureSession_captureCallback = new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                super.onCaptureStarted(session, request, timestamp, frameNumber);
                //获取开始
            }

            @Override
            public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
                super.onCaptureProgressed(session, request, partialResult);
                //获取中
            }

            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);
                //获取结束
            }

            @Override
            public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                super.onCaptureFailed(session, request, failure);
                //获取失败
                cameraON = false;
            }
        };
    }

    /*打开相机*/
    private void openCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            cameraManager.openCamera(cameraId, cameraDevice_stateCallback, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    /*拍照*/
    private void takePicture() {
        CaptureRequest.Builder captureRequestBuilder = null;
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            request_build.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, getRange());//This line of code is used for adjusting the fps range and fixing the dark preview
            request_build.set(CaptureRequest.CONTROL_AE_LOCK, false);
            request_build.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            request_build.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF);//关闭自动对焦
            request_build.set(CaptureRequest.JPEG_ORIENTATION, angleSize);
            Surface surface = imageReader.getSurface();
            captureRequestBuilder.addTarget(surface);
            CaptureRequest request = captureRequestBuilder.build();
            cameraCaptureSession.capture(request, null, handler); //获取拍照
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    /**
     * 获取曝光范围
     *
     * @return
     */
    private Range<Integer> getRange() {
        CameraCharacteristics chars = null;
        try {
            chars = cameraManager.getCameraCharacteristics(cameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Range<Integer>[] ranges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);

        Range<Integer> result = null;

        for (Range<Integer> range : ranges) {
            //帧率不能太低，大于10
            if (range.getLower() < 10)
                continue;
            if (result == null)
                result = range;
                //FPS下限小于15，弱光时能保证足够曝光时间，提高亮度。range范围跨度越大越好，光源足够时FPS较高，预览更流畅，光源不够时FPS较低，亮度更好。
            else if (range.getLower() <= 15 && (range.getUpper() - range.getLower()) > (result.getUpper() - result.getLower()))
                result = range;
        }
        return result;
    }
    /*关闭摄像头，释放资源*/
    private void closeCamera(){
        if(autoCamera){
            autoCamera = false;
        }
        if (cameraCaptureSession != null){
            try {
                cameraCaptureSession.stopRepeating();
                cameraCaptureSession.abortCaptures();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
        if (cameraDevice != null){
            cameraDevice.close();
            cameraDevice = null;
        }
        cameraManager = null;
        if (handler != null){
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }  if (request_build != null) {
            request_build.removeTarget(surface);
            request_build = null;
        }
        if (surface != null) {
            surface.release();
            surface = null;
        }
        if (surfaceTexture != null) {
            surfaceTexture.release();
            surfaceTexture = null;
        }
    }
    /*切换摄像头*/
    private void changeCamera(){
       closeCamera();
       initChildThread();
       initCameraManage();
       initImageReader();
       if (!textureView.isAvailable()){
           initTextureViewListener();
       }else{
           openCamera();
       }
    }

}
