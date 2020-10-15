package pre.cg.camera.fragment;



import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;



import java.util.List;

import lombok.SneakyThrows;
import pre.cg.camera.service.CameraAuto;
import pre.cg.camera.R;

public class MainFragment extends Fragment {
    private static final String TAG = "MainFragment";
    private Activity activity;
    private Context fragmentcontext;
    private Intent intent;
    /*预览界面*/
    private TextureView textureView;
    private CameraManager cameraManager;
    private String cameraId;
    private Size cameraSize;
    boolean activityON = false;
    private CameraAuto.MyBind myBind;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @SneakyThrows
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            myBind = (CameraAuto.MyBind) service;
            myBind.initPreview(cameraId, cameraSize, textureView);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.mainfragment,container,false);
        return view;
    }
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activity = getActivity();
        fragmentcontext = context;
    }
    @Override
    public void onStart() {
        super.onStart();
        textureView = getView().findViewById(R.id.textureview);
    }
    /*初始化管理*/
    private void initCameraManage(){
        cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
    }
    /**
     * 初始化计算适合当前屏幕分辨率的拍照分辨率
     * @return
     */
    private void initHandlerMatchingSize() {
        try {
            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] sizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            int deviceWidth = displayMetrics.widthPixels;
            int deviceHeigh = displayMetrics.heightPixels;
            for (int j = 1; j < 81; j++) {
                for (int i = 0; i < sizes.length; i++) {
                    Size itemSize = sizes[i];
                    if (itemSize.getHeight() < (deviceWidth + j * 5) && itemSize.getHeight() > (deviceWidth - j * 5)) {
                        if (cameraSize != null) { //如果之前已经找到一个匹配的宽度
                            if (Math.abs(deviceHeigh-itemSize.getWidth()) < Math.abs(deviceHeigh - cameraSize.getWidth())){ //求绝对值算出最接近设备高度的尺寸
                                cameraSize = itemSize;
                                continue;
                            }
                        }else {
                            cameraSize = itemSize;
                        }

                    }
                }

            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    /*开启预览*/
    public void startPreview(String cameraId){
        ActivityManager activityManager = (ActivityManager) fragmentcontext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> runningServiceInfos = activityManager.getRunningServices(Integer.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo runningServiceInfo : runningServiceInfos){
            if (runningServiceInfo.service.getClassName().equals("pre.cg.camera.service.CameraAuto"));{
                activityON = runningServiceInfo.started;
                break;
            }
        }
        intent = new Intent(activity, CameraAuto.class);
        if (activityON) {
//            activity.unbindService(serviceConnection);
            activity.stopService(intent);
        }
        this.cameraId = cameraId;
        initCameraManage();
        initHandlerMatchingSize();
        activity.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        activity.startService(intent);
    }
    /*开启定时拍照*/
    public void startAutoPicture(long autoTime){
        if (myBind != null) {
            try {
                int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
                int angle = getJpegOrientation(cameraManager.getCameraCharacteristics(cameraId), rotation);
                myBind.initPictureAuto(autoTime,angle);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }
    /*切换摄像头*/
    public void changeCamera(String cameraId){
        if (myBind!=null){
            myBind.switchCamera(cameraId);
        }
    }
    /*暂停拍照*/
    public void stopAuto(){
        if (myBind!=null) {
            myBind.shutDownCamera();
        }
    }
    /*相机状态*/
    public boolean cameraState(){
        if (textureView.isAvailable() && myBind != null){
            return myBind.cameraState();
        }
        return false;
    }
    /**
     * 官方提供的JPEG图片方向算法
     * @param c
     * @param deviceOrientation
     * @return
     */
    private int getJpegOrientation(CameraCharacteristics c, int deviceOrientation) {
        if (deviceOrientation == OrientationEventListener.ORIENTATION_UNKNOWN){
            return 0;
        }
        int sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION);//获取传感器方向

        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90;

        // Reverse device orientation for front-facing cameras
        boolean facingFront = c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;//判断摄像头面向
        if (facingFront) {
            deviceOrientation = -deviceOrientation;
        }

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        int jpegOrientation = (sensorOrientation + deviceOrientation + 360) % 360;

        return jpegOrientation;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        cameraManager = null;
        activity.unbindService(serviceConnection);
    }
}
