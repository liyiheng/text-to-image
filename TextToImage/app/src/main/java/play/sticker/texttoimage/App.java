package play.sticker.texttoimage;

import android.app.Application;
import android.graphics.Typeface;

public class App extends Application {
    private static App instance;
    private Typeface typeface;
    private boolean gif;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        typeface = Typeface.createFromAsset(instance.getAssets(), "fonts/YingZhangKaiShu-2.ttf");
    }

    public static App getInstance() {
        return instance;
    }

    public Typeface getTypeface() {
        return typeface;
    }

    public boolean isGif() {
        return gif;
    }

    public void setGif(boolean gif) {
        this.gif = gif;
    }
}
