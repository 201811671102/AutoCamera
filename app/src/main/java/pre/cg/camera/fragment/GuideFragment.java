package pre.cg.camera.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import pre.cg.camera.R;

public class GuideFragment extends Fragment {
    private static final String TAG = "GuideFragment";

    private Activity activity;
    private View view;
    private ImageView imageView;
    private EditText editText;

    private View.OnClickListener onClickListener;
    private TextWatcher textWatcher;

    public GuideFragment(View.OnClickListener onClickListener,TextWatcher textWatcher) {
        this.onClickListener = onClickListener;
        this.textWatcher = textWatcher;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.guidefragment,container,false);
        return view;
    }
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activity = getActivity();
    }
    @Override
    public void onStart() {
        super.onStart();
        view = getView();
        imageView = view.findViewById(R.id.start_image);
        editText = view.findViewById(R.id.start_time);
        imageView.setOnClickListener(onClickListener);
        editText.addTextChangedListener(textWatcher);
        editText.setVisibility(View.INVISIBLE);
    }

    public void inputTime(){
        editText.setVisibility(View.VISIBLE);
    }
    public void clearInput(){
        editText.setText("");
    }
}
