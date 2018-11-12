package nodo.privatemp3player.player_main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import nodo.privatemp3player.R;
import nodo.privatemp3player.explorer.ExplorerActivity;
import nodo.privatemp3player.helpers.V;

public class YourPainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_your_pain);
    }

    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnLoadAfterDel:
                startActivityForResult(new Intent(this, ExplorerActivity.class), V.EXPLORER);
                break;
            case R.id.btnExitDel:
                setResult(RESULT_CANCELED);
                finish();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("YourPainActivity", "onActivityResult: request - " + requestCode + ", result - " + resultCode);
        setResult(resultCode);
        finish();
    }
}
