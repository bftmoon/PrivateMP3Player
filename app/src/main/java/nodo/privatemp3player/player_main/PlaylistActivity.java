package nodo.privatemp3player.player_main;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.SearchView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

import nodo.privatemp3player.helpers.V;
import nodo.privatemp3player.helpers.Encryptor;
import nodo.privatemp3player.R;
import nodo.privatemp3player.explorer.ExplorerActivity;
import nodo.privatemp3player.preference.PreferenceActivity;

public class PlaylistActivity extends Activity implements View.OnTouchListener {


    private PlaylistAdapter adapter;
    private Intent playIntent;
    private MusicService musicSrv;
    private boolean isServiceBinded;
    private int screenSize;
    private float x1;

    private final BroadcastReceiver songnumReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("PlaylistActivity", "Receiving: Set songnum catch");
            adapter.setSongnum(intent.getIntExtra("songnum", 0));
        }
    };

    private final BroadcastReceiver finishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("PlaylistActivity", "Receiving, Lifecycle: finishing catch");
            finish();
        }
    };

    private final ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("PlaylistActivity", "Lifecycle-Service: connected");
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicSrv = binder.getService();
            musicSrv.focusPlaylistChanged(true);
            getEncryptorToService();

            if (musicSrv.emptySongnames()) {
                if (musicSrv.createSongList()) {
                    if (adapter.isEmptySongnames()) {
                        adapter.setSongnames(new Encryptor(getApplicationContext()).decryptAllNames(musicSrv.getSongnames()));
                    }
                } else {
                    startAlertDialog();
                }
            } else {
                SharedPreferences pref = getSharedPreferences(V.Prefs.common_prefs, Context.MODE_PRIVATE);
                if (pref.contains("need_update")) {
                    updateSongnames();
                    pref.edit().remove("need_update").apply();
                }
                if (adapter.isEmptySongnames()) {
                    adapter.setSongnames(new Encryptor(getApplicationContext()).decryptAllNames(musicSrv.getSongnames()));
                }
                adapter.setSongnum(musicSrv.getCurrentSongNum());
            }
            isServiceBinded = true;
        }


        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("PlaylistActivity", "Lyfecycle-Service: disconnected");
            musicSrv.focusPlaylistChanged(false);
            isServiceBinded = false;
        }
    };

    private void startAlertDialog() {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.ask_go_explorer))
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        goToExplorer();
                    }
                })
                .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getApplicationContext(), getString(R.string.empty), Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenSize = metrics.widthPixels;

        adapter = new PlaylistAdapter(new OnPlaylistClickListener() {
            @Override
            public void onSongItemClick(int songnum) {
                try {
                    musicSrv.setSong(songnum);
                } catch (IOException e) {
                    Log.e("PlaylistActivity", "Wrong songnum using: " + e);
                }
            }

            @Override
            public void onSongItemLongClick() {
                findViewById(R.id.ibtnDelete).setVisibility(View.VISIBLE);
            }
        }, this);

        RecyclerView recyclerPlaylist = findViewById(R.id.recyclerPlaylist);
        recyclerPlaylist.setLayoutManager(new LinearLayoutManager(this));
        recyclerPlaylist.setAdapter(adapter);
        recyclerPlaylist.setOnTouchListener(this);
        Log.d("PlaylistActivity", "Lifecycle: onCreate");
    }

    private void goToExplorer() {
        startActivityForResult(new Intent(this, ExplorerActivity.class), V.EXPLORER);
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.d("PlaylistActivity", "Lifecycle: onStart");
        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            startService(playIntent);
            bindService(playIntent, musicConnection, 0);
        } else if (!isServiceBinded) {
            bindService(playIntent, musicConnection, 0);
        }
    }

    @Override
    protected void onResume() {
        Log.d("PlaylistActivity", "Lifecycle: onResume");
        super.onResume();
//        if (isServiceBinded) {
//            musicSrv.focusPlaylistChanged(true);
//        }
        LocalBroadcastManager.getInstance(this).registerReceiver(songnumReceiver, new IntentFilter("songnum-updater"));
        LocalBroadcastManager.getInstance(this).registerReceiver(finishReceiver, new IntentFilter("finishing-playlist"));
    }

    @Override
    protected void onPause() {
        Log.d("PlaylistActivity", "Lifecycle: onPause");
        if (musicSrv != null) {
            musicSrv.focusPlaylistChanged(false);
            if (isServiceBinded) {
                Log.d("PlaylistActivity", "Lifecycle: unbound");
                unbindService(musicConnection);
                isServiceBinded = false;
            }
        }
        super.onPause();
    }

    private void getEncryptorToService() {
        musicSrv.setEncryptor(new Encryptor(this));
    }

    @Override
    protected void onStop() {
        Log.d("PlaylistActivity", "Lifecycle: onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d("PlaylistActivity", "Lifecycle: onDestroy");
        if (musicSrv != null) {
            if (!musicSrv.isPlaying()) {
                Log.d("PlaylistActivity", "Lifecycle-Service: Stop service onDestroy without music");
                if (playIntent != null) {
                    unbindService(musicConnection);
                    stopService(playIntent); // Service don't need without music play
                }
            }
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.playlist_menu, menu);
        SearchView menuSearch = (SearchView) menu.findItem(R.id.menuSearch).getActionView();
        menuSearch.setImeOptions(EditorInfo.IME_ACTION_DONE);
        menuSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return false;
            }
        });
        menuSearch.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                adapter.cancelSearchMode();
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuSettings:
                musicSrv.focusPlaylistChanged(false);
                startActivityForResult(new Intent(this, PreferenceActivity.class), V.PREFERENCE);
                return true;
            case R.id.menuUpdate:
                updateSongnames();
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                if (((x1 - event.getX()) > screenSize / 5f) && adapter.wasSongChosen()) {
                    musicSrv.focusPlaylistChanged(false);
                    Intent intent = new Intent(this, SongPlayActivity.class);
                    intent.putExtra("encrypted_filename", musicSrv.getEncryptedCurrentSongname())
                            .putExtra("duration", musicSrv.getDuration())
                            .putExtra("currTime", musicSrv.getCurrentTime());
                    if (musicSrv.isPlaying()) intent.putExtra("isPlaying", true);
                    if (musicSrv.isLoop()) intent.putExtra("isLoop", true);
                    if (musicSrv.isRand()) intent.putExtra("isRand", true);
                    startActivityForResult(intent, V.SONGPLAY);
                }
        }
        return false;
    }

    public void onClick(View v) {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.ask_deleting))
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ArrayList<Integer> delThisSogs = adapter.removeSongnumsForDeleting();
                        if (delThisSogs.isEmpty())
                            Toast.makeText(getApplicationContext(), R.string.empty_delete_list, Toast.LENGTH_SHORT).show();
                        else
                            musicSrv.removeSongs(delThisSogs);
                        if (musicSrv.emptySongnames()) {
                            stopService(playIntent);
                            playIntent = null;
                            startActivityForResult(new Intent(getApplicationContext(), YourPainActivity.class), V.YOUR_PAIN);
                        }
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        adapter.cancelDelMode();
                    }
                })
                .show();
        v.setVisibility(View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("PlaylistActivity", "onActivityResult: request - " + requestCode + ", result - " + resultCode);
        switch (requestCode) {
            case V.SONGPLAY:
                if (resultCode == RESULT_OK)
                    finish();
                else if (data != null)
                    adapter.setSongnum(data.getIntExtra("songnum", 0));
                break;
            case V.YOUR_PAIN:
                if (resultCode != RESULT_OK)
                    finish();
            default:
                if (resultCode == RESULT_OK)
                    updateSongnames();
        }
    }

    private void updateSongnames() {
        if (musicSrv.createSongList()) {
            adapter.setSongnames(new Encryptor(getApplicationContext()).decryptAllNames(musicSrv.getSongnames()));
        } else {
            startAlertDialog();
        }
    }

    @Override
    public void onBackPressed() {
        if (adapter.isDelMode()) {
            Log.d("PlaylistActivity", "Delete: Cancel del by back button");
            adapter.cancelDelMode();
            findViewById(R.id.ibtnDelete).setVisibility(View.GONE);
            return;
        }
        if (adapter.isSearchMode()) {
            Log.d("PlaylistActivity", "Search: Cancel search by back button");
            adapter.cancelSearchMode();
        }
        super.onBackPressed();
    }
}
