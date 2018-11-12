package nodo.privatemp3player.helpers;

import android.content.Context;
import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Calendar;

public class ExceptionHandler implements Thread.UncaughtExceptionHandler {
    private boolean hasStoragePermission;
    private final Context context;

    public ExceptionHandler(Context context) {
        Thread.setDefaultUncaughtExceptionHandler(this);
        this.context = context;
    }

    @Override
    public void uncaughtException(final Thread thread, final Throwable ex) {
        try {
            File f = hasStoragePermission ?
                    new File(V.app_data_path, V.error_logs_name) :
                    new File(context.getFilesDir(), V.error_logs_name);
            FileUtils.writeStringToFile(f, Calendar.getInstance().getTime() + " " + ex.getClass().getSimpleName() + ": " + ex.getMessage() + "\n");
        } catch (Exception e) {
            Log.e("ExceptionHandler", "Logger failed", e);
        }
    }

    public void gainStoragePerm() {
        hasStoragePermission = true;
        new File(V.app_data_path).mkdir();
        new File(context.getFilesDir(), V.error_logs_name).renameTo(new File(V.app_data_path, V.error_logs_name));
    }

    public boolean askFileToStorage() {
        new File(V.app_data_path).mkdir();
        File file = new File(context.getFilesDir(), V.error_logs_name);
        if (file.exists()) {
            file.renameTo(new File(V.app_data_path, V.asked_error_logs_name));
            return true;
        }
        return false;
    }

    public boolean askFileInStorage() {
        return new File(V.app_data_path, V.error_logs_name).exists();
    }
}