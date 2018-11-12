package nodo.privatemp3player;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import nodo.privatemp3player.helpers.ExceptionHandler;
import nodo.privatemp3player.helpers.V;
import nodo.privatemp3player.player_main.PlaylistActivity;
import nodo.privatemp3player.preference.InfoActivity;
import nodo.privatemp3player.preference.KeywordActivity;

public class UnlockActivity extends Activity {

    private String readPin = "";
    private String savedPin = "";
    private ImageView[] dots;
    private SharedPreferences pref;
    private TextView txtPinInfo;
    private boolean isFirstRun;
    private boolean need_rewrite_pin;
    private ExceptionHandler exceptionHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unlock);
        exceptionHandler = new ExceptionHandler(this);
        checkPermissions();
    }

    @Override
    protected void onStart() {
        super.onStart();

        pref = getPreferences(MODE_PRIVATE);
        txtPinInfo = findViewById(R.id.txtPinInfo);

        dots = new ImageView[4];
        dots[0] = findViewById(R.id.imgDot0);
        dots[1] = findViewById(R.id.imgDot1);
        dots[2] = findViewById(R.id.imgDot2);
        dots[3] = findViewById(R.id.imgDot3);


        if (pref.contains("pin")) {
            txtPinInfo.setText(R.string.write_you_pin);
            savedPin = pref.getString("pin", "");
        } else {
            txtPinInfo.setText(R.string.write_new_pin);
            isFirstRun = true;
        }


        if (getIntent().hasExtra(V.Extras.change_pin)) {
            Log.d("UnlockActivity", "Change pin mode");
            need_rewrite_pin = true;
            getIntent().removeExtra(V.Extras.change_pin);
        }
    }

    @Override
    protected void onStop() {
        pref = null;
        txtPinInfo = null;
        dots = null;
        isFirstRun = false;
        need_rewrite_pin = false;
        readPin = null;
        savedPin = null;
        super.onStop();
        Log.d("UnlockActivity", "Lifecycle: onStop");

    }

    public void onClickNum(View v) {
        readPin += Integer.parseInt(((Button) v).getText().toString());
        dots[readPin.length() - 1].setImageResource(R.drawable.full_dot_item);
        if (readPin.length() == 4) {
            if (isFirstRun) {
                if (savedPin.length() == 0) {
                    savedPin = readPin;
                    readPin = "";
                    txtPinInfo.setText(R.string.write_pin_again);
                } else {
                    if (savedPin.equals(readPin)) {
                        pref.edit().putString("pin", savedPin).apply();
                        readPin = "";
                        if (need_rewrite_pin)
                            finish();
                        else
                            startActivityForResult(new Intent(this, KeywordActivity.class), V.KEYWORD);
                    } else {
                        txtPinInfo.setText(R.string.wrong_pin_with_repeat);
                        savedPin = "";
                        readPin = "";
                    }
                }
            } else {
                if (savedPin.equals(readPin)) {
                    readPin = "";
                    if (need_rewrite_pin) {
                        savedPin = "";
                        isFirstRun = true;
                        txtPinInfo.setText(R.string.write_new_pin);
                    } else {
                        startActivityForResult(new Intent(this, PlaylistActivity.class), V.PLAYLIST);
                    }
                } else {
                    txtPinInfo.setText(R.string.wrong_pin);
                    readPin = "";
                }
            }
            for (ImageView dot : dots) {
                dot.setImageResource(R.drawable.empty_dot_item);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("UnlockActivity", "onActivityResult: request - " + requestCode + ", result - " + requestCode);
        if (requestCode == V.INFO) {
            startActivityForResult(new Intent(this, PlaylistActivity.class), V.PLAYLIST);
        } else if (resultCode == RESULT_OK && requestCode == V.KEYWORD)
            startActivityForResult(new Intent(this, InfoActivity.class), V.INFO);
        else {
            Log.d("UnlockActivity", "Lifecycle: finish call");
            finishAffinity();
        }

    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnX:
                readPin = "";
                for (int i = 0; i < 4; i++) {
                    dots[i].setImageResource(R.drawable.empty_dot_item);
                }
                break;
            case R.id.btnDel:
                if (readPin.length() != 0) {
                    dots[readPin.length() - 1].setImageResource(R.drawable.empty_dot_item);
                    readPin = readPin.substring(0, readPin.length() - 1);
                }
        }
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_PHONE_STATE}, 1);
        }
    }

    @Override
    protected void onPause() {
        Log.d("UnlockActivity", "Lifecycle: onPause");
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED)
            txtPinInfo.setText(R.string.permission_info);
        if (grantResults[1] != PackageManager.PERMISSION_GRANTED)
            txtPinInfo.setText(R.string.permission_info);
        else
            exceptionHandler.gainStoragePerm();
        if (grantResults[2] != PackageManager.PERMISSION_GRANTED)
            txtPinInfo.setText(String.format("%s%s", getString(R.string.permission_info), getString(R.string.permission_call_info)));
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy() {
        Log.d("UnlockActivity", "Lifecycle: onDestroy");
        super.onDestroy();
    }
}
