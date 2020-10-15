package pre.cg.camera.activity;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.net.URI;

import pre.cg.camera.R;

public class PictureDetail extends AppCompatActivity {
    private ImageView imageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_detail);

        Intent intent = getIntent();
        Bundle bundle = intent.getBundleExtra("bundle");
        imageView = findViewById(R.id.picture);
        String uri = bundle.getString("uri");
        String date = bundle.getString("date");
        Glide.with(this).load(uri).into(imageView);
    }
}