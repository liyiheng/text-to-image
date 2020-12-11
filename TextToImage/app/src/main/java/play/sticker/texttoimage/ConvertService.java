package play.sticker.texttoimage;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import play.sticker.texttoimage.util.BaseAccessibilityService;
import play.sticker.texttoimage.util.GifEncoder;

public class ConvertService extends BaseAccessibilityService {

    @Override
    public void onAccessibilityEvent(final AccessibilityEvent event) {
    }

    public boolean convertContent(AccessibilityNodeInfo rootNode) {
        int count = rootNode.getChildCount();
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo nodeInfo = rootNode.getChild(i);
            if (nodeInfo == null) {
                continue;
            }
            if ("android.widget.EditText".contentEquals(nodeInfo.getClassName())) {
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                String text = nodeInfo.getText().toString();
                String path = generate(text);
                Bundle arguments = new Bundle();
                arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                        AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);
                arguments.putBoolean(AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN, true);
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                ClipData clip = ClipData.newPlainText("label", path);
                clipboardManager.setPrimaryClip(clip);
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                Toast.makeText(getApplicationContext(), path, Toast.LENGTH_SHORT).show();
                return true;
            }
            if (convertContent(nodeInfo)) {
                return true;
            }
        }
        return false;
    }


    private String saveBitmap(Bitmap bitmap) {
        GifEncoder gifEncoder = new GifEncoder();
        FileOutputStream fos;
        try {
            File root = Environment.getExternalStorageDirectory();
            File file = new File(root, "text_to_img.png");
            fos = new FileOutputStream(file);
            if (App.getInstance().isGif()) {
                gifEncoder.start(fos);
                gifEncoder.addFrame(bitmap);
                gifEncoder.finish();
            } else {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
            }
            // MediaScannerConnection.scanFile(getApplicationContext(),
            //         new String[]{file.getAbsolutePath()},
            //         new String[]{"image/*"}, null);
            return file.getAbsolutePath();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return null;
    }

    private static List<String> splitStringBySize(String str, int size) {
        ArrayList<String> split = new ArrayList<>();
        for (int i = 0; i <= str.length() / size; i++) {
            String substring = str.substring(i * size, Math.min((i + 1) * size, str.length()));
            if (substring.isEmpty()) {
                continue;
            }
            split.add(substring);
        }
        return split;
    }

    private String generate(String text) {
        StringBuilder sb = new StringBuilder();
        List<String> lines = splitStringBySize(text,
                App.getInstance().isGif() ?
                        6
                        :
                        text.length() < 6 ? 3 : 6);
        // String.join needs min api 26
        for (int i = 0; i < lines.size(); i++) {
            if (i != 0) {
                sb.append('\n');
            }
            sb.append(lines.get(i));
        }
        // Context wrappedContext = new ContextThemeWrapper(this, R.style.Theme_AppCompat);
        LayoutInflater inflater = LayoutInflater.from(this);
        TextView tv = ((TextView) inflater.inflate(R.layout.text_view, null));
        tv.setSingleLine(false);
        tv.setText(sb);
        tv.setTypeface(App.getInstance().getTypeface());
        Bitmap bitmap = convertViewToBitmap(tv);
        return saveBitmap(bitmap);
    }

    private Bitmap convertViewToBitmap(View view) {
        view.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        view.draw(canvas);
        return bitmap;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onInterrupt() {
        super.onInterrupt();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(EmptyEvent event) {
        boolean a = convertContent(getRootInActiveWindow());
    }
}