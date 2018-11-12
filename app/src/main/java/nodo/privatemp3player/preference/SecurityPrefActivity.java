package nodo.privatemp3player.preference;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import nodo.privatemp3player.R;
import nodo.privatemp3player.UnlockActivity;
import nodo.privatemp3player.helpers.V;

public class SecurityPrefActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_pref);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnChangeKeyword:
                startActivityForResult(new Intent(this, KeywordActivity.class), V.KEYWORD);
                break;
            case R.id.btnChangePin:
                startActivity(new Intent(this, UnlockActivity.class).putExtra(V.Extras.change_pin, true));
                break;
            case R.id.btnDecryptAll:
                startActivityForResult(new Intent(this, LoadingActivity.class).putExtra(V.Extras.decrypt_all, true), V.LOADING);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("SecurityPrefActivity", "onActivityResult: request - " + requestCode + ", result - " + resultCode);
        if (resultCode == RESULT_OK) {
            setResult(resultCode);
            finish();
        }
    }
}
