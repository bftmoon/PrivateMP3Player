package nodo.privatemp3player.explorer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import nodo.privatemp3player.R;
import nodo.privatemp3player.helpers.V;
import nodo.privatemp3player.preference.LoadingActivity;

public class ExplorerActivity extends Activity {

    private ExplorerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explorer);
        RecyclerView recyclerViewExplorer = findViewById(R.id.recyclerViewExplorer);
        recyclerViewExplorer.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExplorerAdapter(this, (TextView)findViewById(R.id.txtPath));
        recyclerViewExplorer.setAdapter(adapter);
    }

    public void onClick(View v) {
        Log.d("explorer", "Was click");
        switch (v.getId()){
            case R.id.ibtnUndo:
                adapter.undo();
                break;
            case R.id.ibtnOk:
                Log.d("explorer", "Ok was clicked");
                Intent intent = new Intent(this, LoadingActivity.class);
                String[] songfiles = adapter.getChosenFiles();
                if (songfiles != null)
                    intent.putExtra(V.Extras.songpathes, songfiles);
                startActivityForResult(intent, V.LOADING);
                break;
            case R.id.ibtnCancel:
                setResult(RESULT_CANCELED);
                finish();
                break;
            case R.id.ibtnChooseAll:
                adapter.chooseAllInDir();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("ExplorerActivity", "onActivityResult: request - " + requestCode + ", result - " + resultCode);
        setResult(resultCode);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (!adapter.undo())
            super.onBackPressed();
    }
}
