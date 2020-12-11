package play.sticker.texttoimage;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.tbruyelle.rxpermissions3.RxPermissions;
import com.yhao.floatwindow.FloatWindow;
import com.yhao.floatwindow.MoveType;
import com.yhao.floatwindow.Screen;

import org.greenrobot.eventbus.EventBus;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.start).setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
        ((RadioGroup) findViewById(R.id.radio_group)).setOnCheckedChangeListener((group, checkedId) -> {
            App.getInstance().setGif(checkedId == R.id.radio_gif);
        });

        // View view = getLayoutInflater().inflate(R.layout.layout, null);
        ImageView imageView = new ImageView(getApplicationContext());
        imageView.setImageResource(R.mipmap.ic_launcher);
        imageView.setOnClickListener(this);
        FloatWindow
                .with(getApplicationContext())
                .setView(imageView)
                .setWidth(100)                               //设置控件宽高
                .setHeight(Screen.width, 0.2f)
                .setX(100)                                   //设置控件初始位置
                .setY(Screen.height, 0.3f)
                .setDesktopShow(true)                        //桌面显示
                .setMoveType(MoveType.active)
                .build();
        // Must be done during an initialization phase like onCreate
        rxPermissions
                .request(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(granted -> {
                    if (!granted) { // Always true pre-M
                    }
                });
    }

    final RxPermissions rxPermissions = new RxPermissions(this); // where this is an Activity or Fragment instance

    @Override
    public void onClick(View v) {
        EventBus.getDefault().post(new EmptyEvent());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FloatWindow.destroy();
    }
}