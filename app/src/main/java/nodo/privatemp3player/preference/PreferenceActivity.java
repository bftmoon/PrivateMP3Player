package nodo.privatemp3player.preference;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import nodo.privatemp3player.R;
import nodo.privatemp3player.explorer.ExplorerActivity;
import nodo.privatemp3player.helpers.ExceptionHandler;
import nodo.privatemp3player.helpers.V;

public class PreferenceActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preference);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnOpenExplorer:
                startActivityForResult(new Intent(this, ExplorerActivity.class), V.EXPLORER);
                break;
            case R.id.btnSecurity:
                startActivityForResult(new Intent(this, SecurityPrefActivity.class), V.SECURITY_PREF);
                break;
            case R.id.btnInfo:
                startActivity(new Intent(this, InfoActivity.class));
                break;
            case R.id.btnSync:
                // todo: dropbox or mail.cloud sync
                ExceptionHandler eh = new ExceptionHandler(this);
                if (eh.askFileToStorage())
                    Toast.makeText(this, V.app_data_path + "/" + V.asked_error_logs_name, Toast.LENGTH_SHORT).show();
                else if (eh.askFileInStorage())
                    Toast.makeText(this, V.app_data_path + "/" + V.error_logs_name, Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this, R.string.empty_error_files, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("PreferenceActivity", "onActivityResult: request - " + requestCode + ", result - " + resultCode);
        if (resultCode == RESULT_OK) {
            setResult(resultCode);
            finish();
        }
    }
}
