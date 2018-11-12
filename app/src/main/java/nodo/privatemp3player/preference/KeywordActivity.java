package nodo.privatemp3player.preference;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import nodo.privatemp3player.R;
import nodo.privatemp3player.helpers.Encryptor;
import nodo.privatemp3player.helpers.V;

public class KeywordActivity extends Activity {

    private EditText editKeyword;
    private TextView txtKeywordInfo;
    private SharedPreferences pref;
    private String key = "";
    private boolean rewrite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keyword);

        pref = getSharedPreferences(V.Prefs.common_prefs, Context.MODE_PRIVATE);
        editKeyword = findViewById(R.id.editKeyword);
        txtKeywordInfo = findViewById(R.id.txtKeywordInfo);
        if (pref.contains(V.Prefs.keyword)) {
            rewrite = true;
            editKeyword.setText(pref.getString(V.Prefs.keyword, ""));
            if (pref.getBoolean(V.Prefs.isFullEncrypt, false)) {
                ((RadioGroup) findViewById(R.id.rgrpChooseEncr)).check(R.id.rbtnAll);
            }
        }
    }


    public void onClick(View v) {
        if (v.getId() == R.id.btnOkKeyword) {
            if (rewrite)
                rewriteOld();
            else
                setNew();
        }
    }

    private void setNew() {
        key = editKeyword.getText().toString();
        if (key.length() == 0) {
            txtKeywordInfo.setText(R.string.instruct_0_length);
        } else {
            pref.edit().putString(V.Prefs.keyword, key).apply();
            if (((RadioGroup) findViewById(R.id.rgrpChooseEncr)).getCheckedRadioButtonId() == R.id.rbtnJustName) {
                pref.edit().putBoolean(V.Prefs.isFullEncrypt, false).apply();
            } else {
                pref.edit().putBoolean(V.Prefs.isFullEncrypt, true).apply();
            }
            setResult(RESULT_OK);
            finish();
        }
    }

    private void rewriteOld() {
        boolean isFullFileEncrypt_old = pref.getBoolean(V.Prefs.isFullEncrypt, false), isFullFileEncrypt;
        String key_old = pref.getString(V.Prefs.keyword, "");

        key = editKeyword.getText().toString();
        if (key.length() == 0) {
            txtKeywordInfo.setText(R.string.instruct_0_length);
        } else {
            isFullFileEncrypt = ((RadioGroup) findViewById(R.id.rgrpChooseEncr)).getCheckedRadioButtonId() != R.id.rbtnJustName;
            if (key_old.equals(key) && (isFullFileEncrypt == isFullFileEncrypt_old)) {
                txtKeywordInfo.setText(R.string.instruct_no_changes);
            } else {
                pref.edit().putString(V.Prefs.keyword, key).apply();
                pref.edit().putBoolean(V.Prefs.isFullEncrypt, isFullFileEncrypt).apply();
                new Encryptor(this).prepareOldKeyDecryptingHelpers(key_old, isFullFileEncrypt_old);
                startActivityForResult(new Intent(this, LoadingActivity.class).putExtra(V.Extras.need_reencrypt, true), V.LOADING);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("KeywordActivity", "onActivityResult: request - " + requestCode + ", result - " + resultCode);
        setResult(resultCode);
        finish();
    }
}
