package nodo.privatemp3player.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import nodo.privatemp3player.player_main.SongMetadata;

public class Encryptor {
    private static char[] key_chars, old_key_chars;
    private static boolean isfullFileEncrypt, isfullFileEncrypt_old;
    private final char[] badSymbols = {'\\', '/', ':', '*', '?', '\"', '<', '>', '|', ' '};

    public Encryptor(Context context) {
        SharedPreferences pref = context.getSharedPreferences(V.Prefs.common_prefs, Context.MODE_PRIVATE);
        String keyword = pref.getString(V.Prefs.keyword, "");
        isfullFileEncrypt = pref.getBoolean(V.Prefs.isFullEncrypt, false);
        key_chars = keyword.toCharArray();
    }


    private String encryptFileName(String filename) {
        char[] name = filename.toCharArray();
        StringBuilder new_name = new StringBuilder();
        char buffer_char;
        for (int i = 0; i < name.length; i++) {
            buffer_char = (char) (name[i] + key_chars[i % key_chars.length]);
            for (int j = 0; j < badSymbols.length; j++) {
                if (buffer_char == badSymbols[j]) {
                    new_name.append(' ');
                    buffer_char = (char) ('a' + j);
                    break;
                }
            }
            new_name.append(buffer_char);
        }
        return new_name.toString();
    }

    public String decryptFileName(String name) {
        char[] encr_name = name.toCharArray();
        String new_name = "";
        char buffer_char;

        for (int i = 0, key_j = 0; i < encr_name.length; i++) {
            if (encr_name[i] == ' ') {
                i++;
                buffer_char = (char) (badSymbols[encr_name[i] - 'a'] - key_chars[key_j]);
            } else {
                buffer_char = (char) (encr_name[i] - key_chars[key_j]);
            }
            key_j++;
            if (key_j >= key_chars.length)
                key_j = 0;
            new_name += buffer_char;
        }
        return new_name;
    }

    public void reencryptFile(String filename) {
        String filepath_old = V.app_songs_path + "/" + filename;
        if (!isfullFileEncrypt_old && !isfullFileEncrypt) {
            String newname = encryptFileName(decryptFileNameByOldKey(filename));
            new File(filepath_old).renameTo(new File(V.app_songs_path + "/" + newname));
        } else {
            try {
                BufferedInputStream encryptedStream = new BufferedInputStream(
                        new FileInputStream(filepath_old));
                BufferedOutputStream reencryptedStream = new BufferedOutputStream(
                        new FileOutputStream(V.app_songs_path + "/" + encryptFileName(decryptFileNameByOldKey(filename))));

                byte[] buffer = new byte[1024];
                int lengthRead, k = 0, l = 0;

                while ((lengthRead = encryptedStream.read(buffer)) > 0) {
                    for (int i = 0; i < buffer.length; i += 15, k++, l++) {
                        if (isfullFileEncrypt_old) {
                            if (k == old_key_chars.length)
                                k = 0;
                            buffer[i] ^= old_key_chars[k];
                        }
                        if (isfullFileEncrypt) {
                            if (l == key_chars.length)
                                l = 0;
                            buffer[i] ^= key_chars[l];
                        }
                    }
                    reencryptedStream.write(buffer, 0, lengthRead);
                    reencryptedStream.flush();
                }
                encryptedStream.close();
                reencryptedStream.close();
                new File(filepath_old).delete();

            } catch (IOException e) {
                Log.e("Encryptor", "Not found file in reencryptFile: " + e);
            }
        }
    }

    private String decryptFileNameByOldKey(String name) {
        char[] encr_name = name.toCharArray();
        StringBuilder new_name = new StringBuilder();
        char buffer_char;

        for (int i = 0, key_j = 0; i < encr_name.length; i++) {
            if (encr_name[i] == ' ') {
                i++;
                buffer_char = (char) (badSymbols[encr_name[i] - 'a'] - old_key_chars[key_j]);
            } else {
                buffer_char = (char) (encr_name[i] - old_key_chars[key_j]);
            }
            key_j++;
            if (key_j >= old_key_chars.length)
                key_j = 0;
            new_name.append(buffer_char);
        }
        return new_name.toString();
    }

    public void encryptFile(String filepath) throws IOException {
        BufferedInputStream instream = new BufferedInputStream(new FileInputStream(filepath));
        BufferedOutputStream outstream = new BufferedOutputStream(new FileOutputStream(V.app_songs_path + "/" + encryptFileName(new File(filepath).getName())));

        byte[] buffer = new byte[1024];
        int lengthRead, k = 0;

        while ((lengthRead = instream.read(buffer)) > 0) {
            if (isfullFileEncrypt) {
                for (int i = 0; i < buffer.length; i += 15, k++) {
                    if (k == key_chars.length)
                        k = 0;
                    buffer[i] ^= key_chars[k];
                }
            }
            outstream.write(buffer, 0, lengthRead);
            outstream.flush();
        }
        instream.close();
        outstream.close();

    }

    public SongMetadata decryptMetadata(String filepath) {

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(filepath);
        byte[] buffer = retriever.getEmbeddedPicture();

        if (buffer == null)
            return new SongMetadata(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                    null);
        return new SongMetadata(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                BitmapFactory.decodeByteArray(buffer, 0, buffer.length));
    }

    public String decryptFile(String filepath) {
        if (isfullFileEncrypt) {
            File tempfile = new File(V.app_data_path + "/.temp.mp3");
            try {
                BufferedInputStream encryptedStream = new BufferedInputStream(new FileInputStream(filepath));
                BufferedOutputStream decryptedStream = new BufferedOutputStream(new FileOutputStream(tempfile));

                byte[] buffer = new byte[1024];
                int lengthRead, k = 0;

                while ((lengthRead = encryptedStream.read(buffer)) > 0) {
                    if (isfullFileEncrypt) {
                        for (int i = 0; i < buffer.length; i += 15, k++) {
                            if (k == key_chars.length)
                                k = 0;
                            buffer[i] ^= key_chars[k];
                        }
                    }
                    decryptedStream.write(buffer, 0, lengthRead);
                    decryptedStream.flush();
                }
                encryptedStream.close();
                decryptedStream.close();

                return tempfile.getAbsolutePath();
            } catch (IOException e) {
                Log.e("Encryptor", "Not found file in decryptFile: " + e.getMessage());
            }
        }
        return filepath;
    }

    public ArrayList<String> decryptAllNames(ArrayList<String> songnames) {
        ArrayList<String> decrypted_names = new ArrayList<>(songnames.size());
        for (String songname : songnames) {
            decrypted_names.add(decryptFileName(songname));
        }
        return decrypted_names;
    }

    public void prepareOldKeyDecryptingHelpers(String old_key, boolean isFullFileEncr_old) {
        old_key_chars = old_key.toCharArray();
        isfullFileEncrypt_old = isFullFileEncr_old;
    }

    public void removeOldKeyDecryptingHelpers() {
        old_key_chars = null;
    }
}