package nodo.privatemp3player.player_main;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import nodo.privatemp3player.helpers.Encryptor;
import nodo.privatemp3player.R;
import nodo.privatemp3player.helpers.V;

public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener {

    enum PlaybackStatus {PLAYING, PAUSED}

    private MediaPlayer mediaPlayer;
    private final ArrayList<String> songnames = new ArrayList<>();
    private Encryptor encr;
    private int currentSongNum = -1, savedTime;
    private final IBinder musicBind = new MusicBinder();
    private boolean isRand, hasAudioFocus, songplayActFocused, playlistActFocused;
    private AudioManager audioManager;
    private MediaSessionManager mediaSessionManager;
    private MediaSession mediaSession;
    private MediaController.TransportControls transportControls;

    private static final String ACTION_PLAY = "privateMP3player.ACTION_PLAY";
    private static final String ACTION_PAUSE = "privateMP3player.ACTION_PAUSE";
    private static final String ACTION_PREVIOUS = "privateMP3player.ACTION_PREVIOUS";
    private static final String ACTION_NEXT = "privateMP3player.ACTION_NEXT";
    private static final String ACTION_STOP = "privateMP3player.ACTION_STOP";
    private static final String ACTION_DESTROY_SERVICE = "privateMP3player.ACTION_DESTROY_SERVICE";


    class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        requestAudioFocus();
        if (!hasAudioFocus) {
            stopSelf();
        }
        initMediaPlayer();
        createSongList();
        registerBecomingNoisyReceiver();
        initMediaSession();
        Log.d("MusicService", "Lifecycle: onCreate");
    }

    private void initMediaSession() {
        Log.d("MusicService", "Begin initMediaSession");
        if (mediaSessionManager != null) return;
        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        mediaSession = new MediaSession(getApplicationContext(), "PrivateMP3Player");
        transportControls = mediaSession.getController().getTransportControls();
        mediaSession.setActive(true); //ready to receive media commands
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS | MediaSession.FLAG_HANDLES_MEDIA_BUTTONS);
        mediaSession.setCallback(new MediaSession.Callback() {

            @Override
            public void onPlay() {
                Log.d("MusicService", "Notification: call Play");
                super.onPlay();
                playOrPauseSong();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onPause() {
                super.onPause();
                Log.d("MusicService", "Notification: call Pause");
                playOrPauseSong();
                buildNotification(PlaybackStatus.PAUSED);
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                Log.d("MusicService", "Notification: call Next");
                try {
                    nextOrPrevSong(true);
                } catch (IOException e) {
                    Log.e("MusicService", "Next song not found");
                }
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                Log.d("MusicService", "Notification: call Previous");
                try {
                    nextOrPrevSong(false);
                } catch (IOException e) {
                    Log.e("MusicService", "Prev song not found");
                }
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onStop() {
                super.onStop();
                removeNotification();
                prepareForDestroy();
                Log.d("MusicService", "Stopping by notification");
                stopSelf();
            }
        });
    }

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIncomingActions(intent);
        startForeground(1,
                new Notification.Builder(this)
                        .setStyle(new Notification.MediaStyle()
                                .setMediaSession(mediaSession.getSessionToken())
                                .setShowActionsInCompactView(0, 1, 2, 3))
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                        .setContentTitle("Nothing interesting")
                        .addAction(new Notification.Action(android.R.drawable.ic_media_previous, "previous", playbackAction(3)))
                        .addAction(new Notification.Action(android.R.drawable.ic_media_pause, "pause", playbackAction(0)))
                        .addAction(new Notification.Action(android.R.drawable.ic_media_next, "next", playbackAction(2)))
                        .addAction(android.R.drawable.ic_menu_close_clear_cancel, "destroy service", playbackAction(4))
                        .build());
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("MusicService", "Lifecycle: onBind");
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("MusicService", "Lifecycle: onUnbind");
        return false;
    }

    private void prepareForDestroy() {
        Log.d("MusicService", "Preparing for destroy");
        mediaPlayer.stop();
        mediaPlayer.release();
        removeAudioFocus();
        removeNotification();
        finishActivities();
        stopSelf();
    }


    @Override
    public void onDestroy() {
        Log.d("MusicService", "Lifecycle: onDestroy");
        unregisterReceiver(becomingNoisyReceiver);
        super.onDestroy();
    }


    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.w("MusicService", "MediaPlayer onError call - ");
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.w("MusicService", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.w("MusicService", "MEDIA ERROR SERVER DIED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.w("MusicService", "MEDIA ERROR UNKNOWN " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_IO:
                Log.w("MusicService", "MEDIA ERROR IO " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_MALFORMED:
                Log.w("MusicService", "MEDIA ERROR MALFORMED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                Log.w("MusicService", "MEDIA ERROR TIMED_OUT " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                Log.w("MusicService", "MEDIA ERROR UNSUPPORTED " + extra);
                break;
        }
        return false;
    }


    public void setEncryptor(Encryptor encr) {
        this.encr = encr;
        Log.d("MusicService", "Encryptor added");
    }

    /////////////////////  Work with Notification ///////////////////////

    private void buildNotification(PlaybackStatus playbackStatus) {
        Log.d("MusicService", "Notification: building");
        int notificationAction = android.R.drawable.ic_media_play;
        PendingIntent play_pauseAction = null;

        if (playbackStatus == PlaybackStatus.PLAYING) {
            notificationAction = android.R.drawable.ic_media_pause;
            play_pauseAction = playbackAction(1);
        } else if (playbackStatus == PlaybackStatus.PAUSED) {
            notificationAction = android.R.drawable.ic_media_play;
            play_pauseAction = playbackAction(0);
        }

        Notification.Builder notificationBuilder = new Notification.Builder(this)
                .setStyle(new Notification.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2, 3))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentTitle("Nothing interesting")
                .addAction(new Notification.Action(android.R.drawable.ic_media_previous, "previous", playbackAction(3)))
                .addAction(new Notification.Action(notificationAction, "pause", play_pauseAction))
                .addAction(new Notification.Action(android.R.drawable.ic_media_next, "next", playbackAction(2)))
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "destroy service", playbackAction(4));
        Log.d("MusicService", "Notification: Getting system service");
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(1, notificationBuilder.build());
    }

    private void removeNotification() {
        Log.d("MusicService", "Notification: Removing");
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(1);
    }

    private PendingIntent playbackAction(int actionNumber) {
        Intent playbackAction = new Intent(this, MusicService.class);
        switch (actionNumber) {
            case 0:
                playbackAction.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 1:
                playbackAction.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 2:
                playbackAction.setAction(ACTION_NEXT);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 3:
                playbackAction.setAction(ACTION_PREVIOUS);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 4:
                playbackAction.setAction(ACTION_DESTROY_SERVICE);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        return null;
    }

    private void handleIncomingActions(Intent playbackAction) {
        Log.d("MusicService", "Handling incoming action - ");
        if (playbackAction == null || playbackAction.getAction() == null) {
            Log.d("MusicService", "Nothing to do");
            return;
        }

        String actionString = playbackAction.getAction();
        Log.d("MusicService", actionString);
        if (actionString.equalsIgnoreCase(ACTION_PLAY)) {
            transportControls.play();
        } else if (actionString.equalsIgnoreCase(ACTION_PAUSE)) {
            transportControls.pause();
        } else if (actionString.equalsIgnoreCase(ACTION_NEXT)) {
            transportControls.skipToNext();
        } else if (actionString.equalsIgnoreCase(ACTION_PREVIOUS)) {
            transportControls.skipToPrevious();
        } else if (actionString.equalsIgnoreCase(ACTION_STOP)) {
            transportControls.stop();
        } else if (actionString.equalsIgnoreCase(ACTION_DESTROY_SERVICE)) {
            transportControls.stop();
            stopSelf();
        }
    }

    ///////////////// Work with player - main /////////////

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d("MusicService", "Receiving: onPrepared, to songplay - " + songplayActFocused + ", to playlist - " + playlistActFocused);
        if (songplayActFocused)
            sendMetadata();
        else if (playlistActFocused)
            sendSongnum();
        if (savedTime == 0)
            mp.start();
        else {
            mp.seekTo(savedTime);
            savedTime = 0;
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mediaPlayer.reset();
        if (isRand) {
            currentSongNum = new Random(System.currentTimeMillis()).nextInt(songnames.size());
            try {
                playDecryptedSong();
            } catch (IOException e) {
                Log.e("MusicService", "Random song not found: " + e);
            }
        } else {
            try {
                nextOrPrevSong(true);
            } catch (IOException e) {
                Log.e("MusicService", "Next song not found: " + e);
            }
        }
    }

    public boolean playOrPauseSong() {
        if (!hasAudioFocus)
            requestAudioFocus();
        if (isPlaying()) {
            Log.d("MusicService", "Pause song");
            mediaPlayer.pause();
            buildNotification(PlaybackStatus.PAUSED);
            return true;
        }
        Log.d("MusicService", "Resume song");
        mediaPlayer.start();
        buildNotification(PlaybackStatus.PLAYING);
        return false;
    }

    public void nextOrPrevSong(boolean isNext) throws IOException {
        Log.d("MusicService", "Next or prev song: ");
        if (!hasAudioFocus)
            requestAudioFocus();
        if (isPlaying()) mediaPlayer.pause();
        mediaPlayer.reset();
        if (isNext) {
            Log.d("MusicService", "Next");
            currentSongNum++;
            if (currentSongNum >= songnames.size())
                currentSongNum = 0;
        } else {
            Log.d("MusicService", "Prev");
            if (currentSongNum == 0)
                currentSongNum = songnames.size() - 1;
            else
                currentSongNum--;
        }
        playDecryptedSong();
    }


    public void setSong(int songIndex) throws IOException {
        Log.d("MusicService", "Set song");
        if (!hasAudioFocus) {
            requestAudioFocus();
            currentSongNum = songIndex;
            playDecryptedSong();
        } else {
            if (currentSongNum == songIndex) {
                playOrPauseSong();
            } else {
                if (isPlaying()) mediaPlayer.pause();
                mediaPlayer.reset();
                currentSongNum = songIndex;
                playDecryptedSong();
            }
        }
    }

    public boolean isPlaying() {
        try {
            return mediaPlayer.isPlaying();
        } catch (IllegalStateException e) {
            return false;
        }

    }

    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    private void playDecryptedSong() throws IOException {
        Log.d("MusicService", "Begin decrypt song");
        String nextSong = songnames.get(currentSongNum);
        mediaPlayer.setDataSource(encr.decryptFile(V.app_songs_path + "/" + nextSong));
        mediaPlayer.prepareAsync();
        buildNotification(PlaybackStatus.PLAYING);

    }

    //////////////// Work with mods ////////////////////

    public boolean randMode() {
        Log.d("MusicService", "RandMode changing");
        isRand = !isRand;
        return isRand;
    }

    public boolean loopMode() {
        boolean isLoop = !mediaPlayer.isLooping();
        mediaPlayer.setLooping(isLoop);
        return isLoop;
    }

    public boolean isRand() {
        return isRand;
    }

    public boolean isLoop() {
        return mediaPlayer.isLooping();
    }

    ////////////////Work with AudioFocus /////////////////

    private final BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("MusicService", "Headphones: Call Pause by headphones off (ACTION_AUDIO_BECOMING_NOISY)");
            mediaPlayer.pause();
            buildNotification(PlaybackStatus.PAUSED);
        }
    };

    private void registerBecomingNoisyReceiver() {
        Log.d("MusicService", "Headphones: Registering BecomingNoisyReceiver (getting audio focus)");
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }

    private void removeAudioFocus() {
        hasAudioFocus = !(AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager.abandonAudioFocus(this));
        Log.d("MusicService", "AudioFocus: Removing AudioFocus, hasAudioFocus - " + hasAudioFocus);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                Log.d("MusicService", "AudioFocus: AUDIOFOCUS_GAIN");
                // resume playback
                if (hasAudioFocus) {
                    if (mediaPlayer.isPlaying())
                        mediaPlayer.setVolume(1.0f, 1.0f);
                } else {
                    requestAudioFocus();
                    if (savedTime != 0) {
                        Log.d("MusicService", "AudioFocus: repeating of song and time");
                        try {
                            mediaPlayer.reset();
                            playDecryptedSong();
                        } catch (IOException e) {
                            Log.e("MusgainicService", "AudioFocus: Wrong song num on request AudioFocus");
                        }
                    }
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                Log.d("MusicService", "AudioFocus: AUDIOFOCUS_LOSS");
                // Lost focus for an unbounded amount of time
                if (mediaPlayer.isPlaying()) {
                    savedTime = mediaPlayer.getCurrentPosition();
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                buildNotification(PlaybackStatus.PAUSED);
                hasAudioFocus = false;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                Log.d("MusicService", "AudioFocus: AUDIOFOCUS_LOSS_TRANSIENT");
                // Lost focus for a short time
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                buildNotification(PlaybackStatus.PAUSED);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                Log.d("MusicService", "AudioFocus: AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                // Lost focus for a short time, but it's ok to keep playing
                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.7f, 0.7f);
                break;
        }
    }

    private void requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
        Log.d("MusicService", "AudioFocus: requestAudioFocus, hasAudioFocus - " + hasAudioFocus);
        if (!hasAudioFocus)
            stopSelf();
        else {
            initMediaPlayer();
        }
    }

    ////////////// Work with Activity ////////////////////

    public int getCurrentSongNum() {
        return currentSongNum;
    }

    public int getCurrentTime() {
        return mediaPlayer.getCurrentPosition();
    }

    public void setCurrentTime(int millis) {
        mediaPlayer.seekTo(millis);
    }

    public void focusPlaylistChanged(boolean onFocus) {
        playlistActFocused = onFocus;
    }

    public void focusSongplayChanged(boolean onFocus) {
        songplayActFocused = onFocus;
    }

    private void sendMetadata() {
        Intent intent = new Intent("metadata-updater");
        intent.putExtra("encrypted_filename", songnames.get(currentSongNum))
                .putExtra("duration", mediaPlayer.getDuration())
                .putExtra("isPlaying", true);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendSongnum() {
        Log.d("MusicService", "Receiving: songnum send - " + currentSongNum);
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("songnum-updater").putExtra("songnum", currentSongNum));
    }

    private void finishActivities() {
        if (songplayActFocused) {
            Log.d("MusicService", "Lifecycle: Send finishing to SongPlayActivity");
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("finishing-songplay"));
        } else if (playlistActFocused) {
            Log.d("MusicService", "Lifecycle: Send finishing to PlaylistActivity");
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("finishing-playlist"));
        }
    }

    ///////////////// Work with songnames //////////////////////////

    public boolean createSongList() {
        File default_dir = new File(V.app_songs_path);
        if (!default_dir.exists()) {
            if (!default_dir.mkdirs())
                Log.e("MusicService", "Default dir was not created");
        }
        File[] files = default_dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return !pathname.isHidden() && !pathname.isDirectory();
            }
        });
        if (files != null && files.length != 0) {
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    return Long.compare(o1.lastModified(), o2.lastModified());
                }
            });
            songnames.clear();
            songnames.ensureCapacity(files.length);
            for (File file :
                    files) {
                songnames.add(file.getName());
            }
        }

        return !songnames.isEmpty();
    }

    public boolean emptySongnames() {
        return songnames.isEmpty();
    }

    public ArrayList<String> getSongnames() {
        return songnames;
    }

    public void removeSongs(ArrayList<Integer> songnumsForDeleting) {
        Log.d("MusicService", "Delete: Call removeSongs");
        if (!songnumsForDeleting.isEmpty()) {
            for (int i = songnumsForDeleting.size() - 1; i >= 0; i--) {
                new File(V.app_songs_path + "/" + songnames.remove((int) songnumsForDeleting.get(i))).delete();
            }
        }
    }

    public String getEncryptedCurrentSongname() {
        return songnames.get(currentSongNum);
    }
}

