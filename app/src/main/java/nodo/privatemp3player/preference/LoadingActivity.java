package nodo.privatemp3player.preference;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import nodo.privatemp3player.helpers.Encryptor;
import nodo.privatemp3player.R;
import nodo.privatemp3player.helpers.V;

public class LoadingActivity extends Activity implements View.OnClickListener {

    private Button btnNext;
    private TextView txtProgress;
    private ImageView bongocatImg;
    private AnimationDrawable bongocatAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        btnNext = findViewById(R.id.btnNext);
        txtProgress = findViewById(R.id.txtProgress);
        txtProgress.setText("");
        bongocatImg = findViewById(R.id.imgBongocat);
        bongocatImg.setBackgroundResource(R.drawable.bongocat_animation);
        bongocatAnim = (AnimationDrawable) bongocatImg.getBackground();
        bongocatAnim.start();
        Intent intent = getIntent();
        if (intent.hasExtra(V.Extras.songpathes)) {
            btnNext.setEnabled(false);
            new ConvertNewTask().execute(intent.getStringArrayExtra(V.Extras.songpathes));
        } else if (intent.hasExtra(V.Extras.need_reencrypt)) {
            btnNext.setEnabled(false);
            new ConvertOldTask().execute();
        } else if (intent.hasExtra(V.Extras.decrypt_all)) {
            btnNext.setEnabled(false);
            intent.removeExtra(V.Extras.decrypt_all);
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.ask_decrypting))
                    .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new DecryptTask().execute();
                        }
                    })
                    .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            txtProgress.setText(R.string.relax);
                            btnNext.setEnabled(true);
                        }
                    })
                    .create()
                    .show();
        } else {
            txtProgress.setText(R.string.relax);
        }
    }

    @Override
    protected void onDestroy() {
        Log.d("Desrtoys", "LoadingActivity");
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        setResult(RESULT_OK);
        finish();
    }

    @SuppressLint("StaticFieldLeak")
    class ConvertNewTask extends AsyncTask<String, Integer, Void> {

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            btnNext.setEnabled(true);
            bongocatAnim.stop();
            bongocatImg.setBackgroundResource(R.mipmap.bongocat_0);
            getSharedPreferences(V.Prefs.common_prefs, Context.MODE_PRIVATE)
                    .edit().putBoolean(V.Prefs.need_update, true).apply();
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onProgressUpdate(Integer[] values) {
            super.onProgressUpdate(values);
            if (Objects.equals(values[0], values[1]))
                txtProgress.setText(getString(R.string.cobvering_info) + values[0] + "/" + values[1] + getString(R.string.done_info));
            else
                txtProgress.setText(getString(R.string.cobvering_info) + values[0] + "/" + values[1]);
        }

        @Override
        protected Void doInBackground(String[] filesPathes) {
            Encryptor encryptor = new Encryptor(getApplicationContext());
            for (int i = 0; i < filesPathes.length; i++) {
                try {
                    encryptor.encryptFile(filesPathes[i]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Integer[] val = {i + 1, filesPathes.length};
                publishProgress(val);
            }
            return null;
        }
    }

    @SuppressLint("StaticFieldLeak")
    class ConvertOldTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            btnNext.setEnabled(true);
            bongocatAnim.stop();
            bongocatImg.setBackgroundResource(R.mipmap.bongocat_0);
            getSharedPreferences(V.Prefs.common_prefs, Context.MODE_PRIVATE)
                    .edit().putBoolean(V.Prefs.need_update, true).apply();
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onProgressUpdate(Integer[] values) {
            super.onProgressUpdate(values);
            if (values[0] == -1) {
                txtProgress.setText(R.string.converting_no_files);
            } else {
                if (Objects.equals(values[0], values[1]))
                    txtProgress.setText(getString(R.string.cobvering_info) + values[0] + "/" + values[1] + getString(R.string.done_info));
                else
                    txtProgress.setText(getString(R.string.cobvering_info) + values[0] + "/" + values[1]);
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Encryptor encr = new Encryptor(getApplicationContext());
            String[] filenames = new File(V.app_songs_path).list();

            if (filenames != null && filenames.length != 0) {
                for (int i = 0, len = filenames.length; i < len; i++) {
                    encr.reencryptFile(filenames[i]);
                    publishProgress(i + 1, len);
                }
                encr.removeOldKeyDecryptingHelpers();
            } else {
                publishProgress(-1);
            }
            return null;
        }
    }

    @SuppressLint("StaticFieldLeak")
    class DecryptTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            btnNext.setEnabled(true);
            bongocatAnim.stop();
            bongocatImg.setBackgroundResource(R.mipmap.bongocat_0);
            getSharedPreferences(V.Prefs.common_prefs, Context.MODE_PRIVATE)
                    .edit().putBoolean(V.Prefs.need_update, true).apply();
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onProgressUpdate(Integer[] values) {
            super.onProgressUpdate(values);
            if (values[0] == -1) {
                txtProgress.setText(R.string.decrypting_no_files);
            } else {
                if (Objects.equals(values[0], values[1]))
                    txtProgress.setText(getString(R.string.decrypting_files) + values[0] + "/" + values[1] + getString(R.string.decrypting_done) + "\n" + V.decrypted_folder_path);
                else
                    txtProgress.setText(getString(R.string.decrypting_files) + values[0] + "/" + values[1]);
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Encryptor encr = new Encryptor(getApplicationContext());
            String[] filenames = new File(V.app_songs_path).list();
            new File(V.decrypted_folder_path).mkdir();
            if (filenames != null && filenames.length != 0) {
                for (int i = 0, len = filenames.length; i < len; i++) {
                    new File(encr.decryptFile(V.app_songs_path + "/" + filenames[i]))
                            .renameTo(new File(V.decrypted_folder_path + "/" + encr.decryptFileName(filenames[i])));
                    publishProgress(i + 1, len);
                }
            } else {
                publishProgress(-1);
            }
            return null;
        }
    }
}
