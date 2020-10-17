package pre.cg.camera.adapter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import pre.cg.camera.R;
import pre.cg.camera.activity.PictureDetail;
import pre.cg.camera.pojo.PictureFile;

public class PictureAdapterCheck extends RecyclerView.Adapter<PictureAdapterCheck.PictureHolder> {
    private static final String TAG = "PictureAdapter";

    private List<PictureFile> pictureFileList;
    private Context context;

    public PictureAdapterCheck(@NonNull Context context, List<PictureFile> pictureFileList) {
        super();
        this.context = context;
        this.pictureFileList = pictureFileList;
    }

    @NonNull
    @Override
    public PictureAdapterCheck.PictureHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.picture,parent,false);
        return new PictureHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PictureAdapterCheck.PictureHolder holder, int position) {
        PictureFile pictureFile = pictureFileList.get(position);
        Glide.with(context).load(pictureFile.getUrl()).into(holder.imageView);
        holder.textView.setText(pictureFile.getDate());
        holder.clickView.setVisibility(View.GONE);
        holder.clickView.setImageResource(R.drawable.click);
        holder.imageView.setLongClickable(true);
        holder.imageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ImageView clickView = holder.clickView;
                if (clickView.getVisibility() == View.VISIBLE){
                    clickView.setVisibility(View.GONE);
                    pictureFileList.get(position).setDelete(false);
                }else{
                    clickView.setVisibility(View.VISIBLE);
                    pictureFileList.get(position).setDelete(true);
                }
                return true;
            }
        });
        holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, PictureDetail.class);
                Bundle bundle = new Bundle();
                bundle.putString("uri",pictureFileList.get(position).getUrl());
                bundle.putString("date",pictureFileList.get(position).getDate());
                intent.putExtra("bundle",bundle);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return pictureFileList.size();
    }

    class PictureHolder extends RecyclerView.ViewHolder {
        private ImageView imageView;
        private TextView textView;
        private ImageView clickView;
        public PictureHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.picture_file);
            textView = itemView.findViewById(R.id.picture_date);
            clickView = itemView.findViewById(R.id.click_view);
        }
    }
}
