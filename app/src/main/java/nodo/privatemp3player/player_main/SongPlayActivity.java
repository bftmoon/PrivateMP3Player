package nodo.privatemp3player.player_main;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;

import nodo.privatemp3player.R;
import nodo.privatemp3player.helpers.Encryptor;
import nodo.privatemp3player.helpers.V;

public class SongPlayActivity extends Activity implements SeekBar.OnSeekBarChangeListener {

    private Intent playIntent;
    private MusicService musicSrv;
    private TextView txtFilename, txtSongNameArtist, txtCurrTime, txtDuration;
    private SeekBar sbarTime;
    private ImageView imgAlbum;
    private int screenWidth;
    private Encryptor encr;
    private boolean isServiceBinded;
    private float x1; // onTouch coord

    private final Handler timerHandler = new Handler();
    private final Thread currTimeShower = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                sbarTime.setProgress(musicSrv.getCurrentTime());
                txtCurrTime.setText(makeFormattedTime(musicSrv.getCurrentTime()));
            } catch (NullPointerException e) {
                sbarTime.setProgress(0);
                txtCurrTime.setText(makeFormattedTime(0));
            } catch (IllegalStateException e) {
                sbarTime.setProgress(0);
                txtCurrTime.setText("0");
            }
            timerHandler.postDelayed(this, 100);
        }
    });

    private final ServiceConnection musicConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(final ComponentName name, IBinder service) {
            Log.d("SongPlayActivity", "Lifecycle-Service: connected");
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicSrv = binder.getService();
            musicSrv.focusSongplayChanged(true);
            isServiceBinded = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBinded = false;
            Log.d("SongPlayActivity", "Lifecycle-Service: disconnected");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_play);
        Log.d("SongPlayActivity", "Lifecycle: onCreate");
        txtFilename = findViewById(R.id.txtFileName);
        txtSongNameArtist = findViewById(R.id.txtSongNameAuthor);
        txtCurrTime = findViewById(R.id.txtCurrTime);
        txtDuration = findViewById(R.id.txtDuration);
        sbarTime = findViewById(R.id.sbarTime);
        sbarTime.setOnSeekBarChangeListener(this);
        imgAlbum = findViewById(R.id.imgAlbum);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenWidth = metrics.widthPixels;
        encr = new Encryptor(this);
        setMetadata(getIntent());
        currTimeShower.start(); // it should be here
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("SongPlayActivity", "Lifecycle: onStart");
        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            startService(playIntent);
        }
        if (!isServiceBinded) {
            bindService(playIntent, musicConnection, 0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("SongPlayActivity", "Lifecycle: onResume");
        LocalBroadcastManager.getInstance(this).registerReceiver(metadataReceiver, new IntentFilter("metadata-updater"));
        LocalBroadcastManager.getInstance(this).registerReceiver(finishReceiver, new IntentFilter("finishing-songplay"));
        timerHandler.postDelayed(currTimeShower, 0);
    }

    private void finishApp() {
        setResult(RESULT_OK);
        finish();
    }

    private final BroadcastReceiver finishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("SongPlayActivity", "Lifecycle-Service: finish command catch");
            finishApp();
        }
    };

    private final BroadcastReceiver metadataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("SongPlayActivity", "Lifecycle-Service: metadata catch");
            setMetadata(intent);
        }
    };

    private void setMetadata(Intent intent) {
        String filename = intent.getStringExtra("encrypted_filename");
        int songDuration = intent.getIntExtra("duration", 0);
        txtDuration.setText(makeFormattedTime(songDuration));
        sbarTime.setMax(songDuration);
        SongMetadata metadata = encr.decryptMetadata(V.app_songs_path + "/" + filename);
        txtSongNameArtist.setText(metadata.getTitleAndArtist());
        if (metadata.hasPicture())
            imgAlbum.setImageBitmap(metadata.getPicture());
        else
            imgAlbum.setImageResource(R.mipmap.notes);
        txtFilename.setText(encr.decryptFileName(filename));
        if (intent.hasExtra("currTime")) {
            int currTime = intent.getIntExtra("currTime", 0);
            txtCurrTime.setText(makeFormattedTime(currTime));
            sbarTime.setProgress(currTime);
        }
        if (intent.hasExtra("isPlaying")) {
            ((ImageButton) findViewById(R.id.ibtnPlayPause)).setImageResource(R.mipmap.icon_pause);
        } else {
            ((ImageButton) findViewById(R.id.ibtnPlayPause)).setImageResource(R.mipmap.icon_play);
        }
        if (intent.hasExtra("isLoop")) {
            ((ImageButton) findViewById(R.id.ibtnLoop)).setImageResource(R.mipmap.loop_on);
        }
        if (intent.hasExtra("isRand")) {
            ((ImageButton) findViewById(R.id.ibtnRand)).setImageResource(R.mipmap.icon_rand_on);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(currTimeShower);
        Log.d("SongPlayActivity", "Lifecycle: onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("SongPlayActivity", "Lifecycle: onStop, isServiceBinded - " + isServiceBinded);
        if (musicSrv != null && isServiceBinded) {
            musicSrv.focusSongplayChanged(false);
            unbindService(musicConnection);
            Log.d("SongPlayActivity", "Lifecycle-Service: unbound");
            isServiceBinded = false;
        }
    }

    @Override
    protected void onDestroy() {
        Log.d("SongPlayActivity", "Lifecycle: onDestroy");
        super.onDestroy();
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ibtnPlayPause:
                if (musicSrv.playOrPauseSong()) {
                    ((ImageButton) v).setImageResource(R.mipmap.icon_play);
                    timerHandler.removeCallbacks(currTimeShower);
                } else {
                    ((ImageButton) v).setImageResource(R.mipmap.icon_pause);
                    timerHandler.postDelayed(currTimeShower, 0);
                }
                break;
            case R.id.ibtnNext:
                try {
                    musicSrv.nextOrPrevSong(true);
                } catch (IOException e) {
                    Log.e("SongPlayActivity", "Next not found");
                }
                break;
            case R.id.ibtnPrev:
                try {
                    musicSrv.nextOrPrevSong(false);
                } catch (IOException e) {
                    Log.e("SongPlayActivity", "Previous not found");
                }
                break;
            case R.id.ibtnRand:
                if (musicSrv.randMode())
                    ((ImageButton) v).setImageResource(R.mipmap.icon_rand_on);
                else
                    ((ImageButton) v).setImageResource(R.mipmap.icon_rand_off);
                break;
            case R.id.ibtnLoop:
                if (musicSrv.loopMode())
                    ((ImageButton) v).setImageResource(R.mipmap.loop_on);
                else
                    ((ImageButton) v).setImageResource(R.mipmap.loop);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            musicSrv.setCurrentTime(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    private String makeFormattedTime(int millis) {
        String res = "";
        millis /= 1000;
        if (millis >= 60) {
            res += millis / 60 + ":";
            millis %= 60;
        } else {
            res += "00:";
        }
        res += millis < 10 ? "0" + millis : millis;
        return res;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                if ((event.getX() - x1) > screenWidth / 5f) {
                    musicSrv.focusSongplayChanged(false);
                    setResult(RESULT_CANCELED, new Intent().putExtra("songnum", musicSrv.getCurrentSongNum()));
                    finish();
                }
        }
        return false;
    }
}
