package nodo.privatemp3player.preference;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import nodo.privatemp3player.R;

public class InfoActivity extends Activity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
    }

    public void onClick(View v){
        setResult(RESULT_OK);
        finish();
    }
}
