package pre.cg.camera.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;

import pre.cg.camera.R;
import pre.cg.camera.adapter.PictureAdapterCheck;
import pre.cg.camera.pojo.PictureFile;

public class PictureFragment extends Fragment{
    private static final String TAG = "PictureFragment";
    private View view;
    private Activity activity;
    private List<PictureFile> pictureFileList;
    private PictureAdapterCheck pictureAdapterCheck;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private GridLayoutManager gridLayoutManager;
    private int lastItemPosition;
    private Handler handler;
    private HandlerThread handlerThread;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        initData();
        return inflater.inflate(R.layout.picturefragment,container,false);
    }

    @SuppressLint("ResourceAsColor")
    @Override
    public void onStart() {
        super.onStart();
        view = getView();
        activity = getActivity();
        initChildThread();
        initLayout();
        initRecycler();
        initSwipeRefresh();
        recyclerMore();
    }

    @Override
    public void onResume() {
        super.onResume();
        clearDelte();
    }

    /*初始化子线程*/
    private void initChildThread(){
        handlerThread = new HandlerThread(activity.getPackageName());
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }
    /*初始化组件*/
    private void initLayout(){
        recyclerView = view.findViewById(R.id.picture_recycle);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh_picture);
    }
    /*数据绑定*/
    private void initRecycler(){
        gridLayoutManager = new GridLayoutManager(getContext(),2);
        recyclerView.setLayoutManager(gridLayoutManager);
        initData();
        recyclerView.setAdapter(pictureAdapterCheck);
    //    pictureAdapterCheck.notifyDataSetChanged();
    }
    /*获取（追加）数据*/
    public void initData(){
        if (recyclerView == null){
            return;
        }
        int offset = 1;
        if (pictureFileList == null) {
            pictureFileList = new ArrayList<>();
        }else{
            offset = pictureFileList.size();
        }
        LitePal.beginTransaction();
        List<PictureFile> pictureFiles = LitePal.order("id asc").limit(20).offset(offset).find(PictureFile.class,true);
        if (pictureFiles.size() != 0) {
            LitePal.setTransactionSuccessful();
        }
        LitePal.endTransaction();
        pictureFileList.addAll(pictureFiles);
        if (pictureAdapterCheck != null){
            activity.runOnUiThread(()->{
                pictureAdapterCheck.notifyDataSetChanged();
            });
        }else{
            pictureAdapterCheck = new PictureAdapterCheck(getContext(),pictureFileList);
        }
    }
    /*下拉刷新*/
    @SuppressLint("ResourceAsColor")
    private void initSwipeRefresh(){
        swipeRefreshLayout.setColorSchemeColors(R.color.colorAccent);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                pictureFileList.clear();
                initData();
                swipeRefreshLayout.setRefreshing(false);
            }

        });
    }
    /*上拉加载*/
    private void recyclerMore(){
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                // 在newState为滑到底部时
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (lastItemPosition == pictureAdapterCheck.getItemCount()){
                         handler.postDelayed(()->{
                            initData();
                         },500);
                    }
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                lastItemPosition = gridLayoutManager.findLastVisibleItemPosition()+1;
            }
        });
    }
    /*获取删除item*/
    public List<PictureFile> deletePicture(){
        List<PictureFile> pictureFiles = new ArrayList<>();
        for (int i=0;i<pictureFileList.size();){
            if (pictureFileList.get(i).isDelete()){
                int j = pictureFileList.get(i).delete();
                pictureFiles.add(pictureFileList.get(i));
                pictureFileList.remove(i);
                pictureAdapterCheck.notifyItemRemoved(i);
            }else{
                i++;
            }
        }
        pictureAdapterCheck.notifyDataSetChanged();
        return pictureFiles;
    }
    /*清空删除记录*/
    private void clearDelte(){
        for (PictureFile pictureFile : pictureFileList){
            pictureFile.setDelete(false);
        }
    }
}
