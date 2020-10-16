package pre.cg.camera.activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import org.litepal.LitePal;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pre.cg.camera.R;
import pre.cg.camera.adapter.FragmentAdapter;
import pre.cg.camera.fragment.GuideFragment;
import pre.cg.camera.fragment.MainFragment;
import pre.cg.camera.fragment.PictureFragment;
import pre.cg.camera.pojo.PictureFile;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    /*滑动菜单*/
    private DrawerLayout drawerLayout;
    /*悬浮按钮 跳转照片集页面*/
    private FloatingActionButton floatingActionButton;
    /*滑动菜单_菜单*/
    private NavigationView navigationView;
    /*滑动菜单_菜单_头*/
    private View header;
    /*标题栏*/
    private Toolbar toolbar;
    /*ViewPage*/
    private MainFragment mainFragment;
    private PictureFragment pictureFragment;
    private GuideFragment guideFragment;
    private String autoTime;
    private TextWatcher textWatcher;
    private ViewPager viewPager;
    /*摄像头类型*/
    private Map<Integer,String> camera_id_map = new HashMap<>();
    private String cameraId;
    /*changefragment*/
    private FragmentManager fragmentManager;
    private FrameLayout frameLayout;
    private boolean autoCameraOn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initLayout();
        initSelectCamera();
        /*设置标题栏*/
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.more);
        }
        initFragment();
    }
    /*创建导航栏菜单*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar,menu);
        return true;
    }
    /*过滤导航栏菜单*/
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        /*摄像头类型*/
        initSelectCamera();
        Set<Integer> set = camera_id_map.keySet();
        if (set.size() == 0){
            return false;
        }
        if (!set.contains(R.id.camera_front)){
            menu.findItem(R.id.camera_front).setVisible(false);
        }
        if (!set.contains(R.id.camera_back)){
            menu.findItem(R.id.camera_back).setVisible(false);
        }
        if (!set.contains(R.id.camera_external)){
            menu.findItem(R.id.camera_external).setVisible(false);
        }
       if (mainFragment != null) {
           if (!mainFragment.cameraState()) {
               menu.findItem(R.id.camera).setVisible(false);
           }
       }
       menu.findItem(R.id.behave).setVisible(false);
        return true;
    }
    /*导航栏菜单点击事件*/
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            /*默认返回键 现作为活动菜单滑出键*/
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                break;
            case R.id.camera_front:
                changeCamera(camera_id_map.get(R.id.camera_front));
                break;
            case R.id.camera_back:
                changeCamera(camera_id_map.get(R.id.camera_back));
                break;
            case R.id.camera_external:
                changeCamera(camera_id_map.get(R.id.camera_external));
                break;
            case R.id.clock:
                guideFragment.inputTime();
                showFragment(guideFragment);
                break;
            case R.id.delete:
                List<PictureFile> pictureFiles = pictureFragment.deletePicture();
                for (PictureFile pictureFile : pictureFiles){
                    File file = getExternalFilesDir(pictureFile.getUrl());
                    if (file.exists()){
                        file.delete();
                    }
                    pictureFile.delete();
                }
                break;
            default:
                break;
        }
        return true;
    }
    /*点击事件*/
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.fab:
               AlertDialog.Builder builder = new AlertDialog.Builder(this);
                if (mainFragment.cameraState()){
                    if (autoCameraOn) {
                        builder.setTitle("自动拍照");
                        builder.setMessage("关闭自动拍照");
                        builder.setIcon(R.drawable.alter);
                        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                mainFragment.stopAuto();
                                autoCameraOn = false;
                            }
                        });
                        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        builder.create().show();
                        break;
                    }
                }
                builder.setTitle("自动拍照");
                builder.setMessage("没有开启自动拍照");
                builder.setIcon(R.drawable.alter);
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.create().show();
                break;
            case R.id.start_image:
                hideFragment(guideFragment);
                mainFragment.startPreview(cameraId);
                if (autoTime != null && !autoTime.isEmpty()){
                    mainFragment.startAutoPicture(Long.parseLong(autoTime));
                    autoCameraOn = true;
                }
                toolbar.getMenu().findItem(R.id.camera).setVisible(true);
                autoTime = null;
                hideInput();
                break;
            case R.id.clearAll:
                LitePal.deleteAll("PictureFile");
                AlertDialog.Builder clearAllBuild = new AlertDialog.Builder(this);
                clearAllBuild.setTitle("清空缓存");
                clearAllBuild.setMessage("清空所有文件");
                clearAllBuild.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        File file = new File(getExternalFilesDir("picture/").getPath());
                        clearDir(file);
                    }
                });
                clearAllBuild.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                clearAllBuild.create().show();
            default:
                break;
        }
    }
    /*删除文件*/
    public void  clearDir(File fileDir){
        if (fileDir == null || fileDir.exists() || !fileDir.isDirectory()){
            return;
        }
        for (File file : fileDir.listFiles()){
            if (file.isDirectory()){
                clearDir(file);
            }else{
                file.delete();
            }
        }
        fileDir.delete();
    }
    /*初始化组件*/
    private void  initLayout(){
        drawerLayout = findViewById(R.id.drawerLayout);
        floatingActionButton = findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(this);
        toolbar = findViewById(R.id.main_toolbar);
        /*滑动菜单_菜单*/
        navigationView = findViewById(R.id.nav_navigationView);
        navigationView.setCheckedItem(R.id.main);
        header = navigationView.getHeaderView(0);
        TextView textView = header.findViewById(R.id.picture_path);
        textView.setText("文件路径: "+getExternalFilesDir("picture").getPath());
        Button clearAll = header.findViewById(R.id.clearAll);
        clearAll.setOnClickListener(this::onClick);
        initNavigationViewMenu();
        /*ViewPage*/
        viewPager = findViewById(R.id.view_pager);
            //设置缓存页数
        viewPager.setOffscreenPageLimit(4);
            //设置当前显示页面
        viewPager.setCurrentItem(0);

        /*changeFragment*/
        initAutoTime();
        guideFragment = new GuideFragment(this::onClick,textWatcher);
        frameLayout = findViewById(R.id.change_fragment);
        fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.change_fragment,guideFragment);
        fragmentTransaction.commit();
    }
    /*滑动菜单_菜单_菜单项点击事件*/
    private void initNavigationViewMenu(){
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()){
                    case R.id.main:
                        viewPager.setCurrentItem(0);
                        break;
                    case R.id.pictures:
                        hideFragment(guideFragment);
                        viewPager.setCurrentItem(1);
                        break;
                }
                drawerLayout.closeDrawers();
                return true;
            }
        });
    }
    /* 摄像头类型*/
    private void initSelectCamera() {
        try {
            CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            String[] cameraIdList = cameraManager.getCameraIdList();//获取摄像头id列表
            if (cameraIdList.length == 0) {
                return;
            }
            for (String Id : cameraIdList) {
                //获取相机特征,包含前后摄像头信息，分辨率等
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(Id);
                Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);//获取这个摄像头的面向
                //CameraCharacteristics.LENS_FACING_BACK 后摄像头
                //CameraCharacteristics.LENS_FACING_FRONT 前摄像头
                //CameraCharacteristics.LENS_FACING_EXTERNAL 外部摄像头,比如OTG插入的摄像头
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    camera_id_map.put(R.id.camera_front,Id);
                }else if (facing == CameraCharacteristics.LENS_FACING_BACK){
                    camera_id_map.put(R.id.camera_back,Id);
                }else if (facing == CameraCharacteristics.LENS_FACING_EXTERNAL){
                    camera_id_map.put(R.id.camera_external,Id);
                }
            }
            cameraId = cameraIdList[0];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    /*初始化碎片*/
    private void initFragment(){
        initPermission();
        FragmentAdapter fragmentAdapter = new FragmentAdapter(getSupportFragmentManager());
        mainFragment = new MainFragment();
        pictureFragment = new PictureFragment();
        fragmentAdapter.addFragment(mainFragment);
        fragmentAdapter.addFragment(pictureFragment);
        viewPager.setAdapter(fragmentAdapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                int checkedItem = position == 0 ?R.id.main:R.id.pictures;
                navigationView.setCheckedItem(checkedItem);
                if (position != 0){
                    toolbar.getMenu().findItem(R.id.behave).setVisible(true);
                    toolbar.getMenu().findItem(R.id.camera).setVisible(false);
                    toolbar.getMenu().findItem(R.id.clock).setVisible(false);
                }else{
                    toolbar.getMenu().findItem(R.id.behave).setVisible(false);
                    toolbar.getMenu().findItem(R.id.camera).setVisible(true);
                    toolbar.getMenu().findItem(R.id.clock).setVisible(true);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }
    /*隐藏碎片*/
    private void hideFragment(Fragment fragment){
        if (fragment.isHidden()){
            return;
        }
        viewPager.setVisibility(View.VISIBLE);
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.hide(fragment);
        fragmentTransaction.commit();
        frameLayout.setVisibility(View.INVISIBLE);
    }
    /*显示碎片*/
    private void showFragment(Fragment fragment){
        if (fragment.isHidden()){
            frameLayout.setVisibility(View.VISIBLE);
            viewPager.setVisibility(View.INVISIBLE);
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.show(fragment);
            fragmentTransaction.commit();
        }
    }
    /*切换摄像头*/
    private void changeCamera(String newcameraId){
        if (!guideFragment.isHidden()){
            hideFragment(guideFragment);
        }
        if (mainFragment != null) {
            mainFragment.changeCamera(newcameraId);
        }
    }
    /*请求授权*/
    private void initPermission(){
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.CAMERA},1);
        }
    }
    /*获取定时拍照时间，预览*/
    private void initAutoTime(){
        textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count > 0){
                    autoTime = s.toString();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };
    }
    /*隐藏键盘*/
    private void hideInput(){
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View view = getWindow().peekDecorView();
        if (null != view){
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(),0);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (permissions.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show();
                }else{
                    initPermission();
                }
                break;
            default:
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mainFragment != null){
            mainFragment.stopAuto();
        }
    }

}