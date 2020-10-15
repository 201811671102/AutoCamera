package pre.cg.camera;

import android.app.Application;
import android.content.Context;

import org.litepal.LitePal;

public class MyApplication extends Application {
    private static final MyApplication myApplication = new MyApplication();
    public static MyApplication getInstance(){
        return myApplication;
    }
    public MyApplication(){};

    private Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        this.context = getApplicationContext();
        LitePal.initialize(context);
    }
}
