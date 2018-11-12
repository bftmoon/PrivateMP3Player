package nodo.privatemp3player.helpers;

import android.os.Environment;

public final class V {
    public static final int PLAYLIST = 1;
    public static final int SONGPLAY = 2;
    public static final int YOUR_PAIN = 3;
    public static final int KEYWORD = 4;
    public static final int LOADING = 5;
    public static final int PREFERENCE = 6;
    public static final int SECURITY_PREF = 7;
    public static final int EXPLORER = 8;
    public static final int INFO = 9;

    public static final String app_data_path = Environment.getExternalStorageDirectory() + "/privatemp3player";
    public static final String app_songs_path = app_data_path + "/data";
    public static final String decrypted_folder_path = app_data_path + "/decrypted";
    public static final String error_logs_name = "errors.txt";
    public static final String asked_error_logs_name = "asked_errors.txt";


    public final class Prefs {
        public static final String common_prefs = "common_prefs";
        public static final String isFullEncrypt = "isFullEncrypt";
        public static final String keyword = "keyword";
        public static final String need_update = "need_update";
    }

    public class Extras {
        public static final String change_pin = "change_pin";
        public static final String songpathes = "songpathes";
        public static final String decrypt_all = "decrypt_all";
        public static final String need_reencrypt = "need_reencrypt";
    }
}

