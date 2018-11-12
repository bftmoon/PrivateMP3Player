package nodo.privatemp3player.player_main;

import android.graphics.Bitmap;

public class SongMetadata {

    private String title;
    private String artist;
    private final Bitmap picture;

    public SongMetadata(String title, String artist, Bitmap picture) {
        this.title = title;
        this.artist = artist;
        this.picture = picture;
    }

    public String getTitleAndArtist(){
        if (title == null)
            title = "Unknown";
        if (artist == null)
            artist = "Unknown";
        return title + " - " + artist;
    }

    public Bitmap getPicture() {
        return picture;
    }

    public boolean hasPicture(){
        return picture != null;
    }
}
