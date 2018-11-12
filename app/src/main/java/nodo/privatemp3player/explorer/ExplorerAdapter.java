package nodo.privatemp3player.explorer;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import nodo.privatemp3player.R;

public class ExplorerAdapter extends RecyclerView.Adapter<ExplorerAdapter.FilesViewHolder> {

    private final HashMap<String, ArrayList<File>> allChosenFiles = new HashMap<>();
    private File path;
    private FileInfo[] fileInfos;
    private final FilenameFilter filter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            File file = new File(dir, name);
            return !file.isHidden();
        }
    };
    private final TextView txtPath;
    private final Context context;

    class FilesViewHolder extends RecyclerView.ViewHolder {
        final TextView txtFileFolder;
        final ImageView imgFileFolder;

        FilesViewHolder(View itemView) {
            super(itemView);
            this.txtFileFolder = itemView.findViewById(R.id.txtFileFolder);
            this.imgFileFolder = itemView.findViewById(R.id.imgFileFolder);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FileInfo chosenFileInfo = fileInfos[getLayoutPosition()];
                    if (chosenFileInfo.isFile()) {
                        if (checkFormat(chosenFileInfo.getFile())) {
                            chosenFileInfo.changeChosen();
                            notifyItemChanged(getLayoutPosition());
                        } else {
                            Toast.makeText(context, "Not mp3 file", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        prepareToLeaveDir();
                        path = chosenFileInfo.getFile();
                        prepareNewDirInfo();
                        notifyDataSetChanged();
                    }
                }
            });
        }

        private void bind(FileInfo fileInfo) {
            txtFileFolder.setText(fileInfo.getName());
            imgFileFolder.setBackgroundResource(
                    fileInfo.isFile() ?
                            R.mipmap.file_icon : R.mipmap.directory_icon);
            imgFileFolder.setImageResource(fileInfo.isChosen() ?
                    R.mipmap.chosen_icon : R.color.colorNo);
        }
    }

    ExplorerAdapter(Context context, TextView txtPath) {
        this.context = context;
        this.path = Environment.getExternalStorageDirectory();
        this.txtPath = txtPath;
        txtPath.setText(path.getAbsolutePath());
        File[] files = this.path.listFiles(filter);
        Arrays.sort(files);
        fileInfos = new FileInfo[files.length];
        for (int i = 0; i < files.length; i++) {
            fileInfos[i] = new FileInfo(files[i]);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull FilesViewHolder holder, int position) {
        holder.bind(fileInfos[position]);
    }

    @NonNull
    @Override
    public FilesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_explorer, parent, false);
        return new FilesViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return fileInfos.length;
    }

    private void prepareToLeaveDir() {
        ArrayList<File> chosenFileNames = new ArrayList<>();
        for (FileInfo fileInfo : fileInfos) {
            if (fileInfo.isChosen()) {
                chosenFileNames.add(fileInfo.getFile());
            }
            if (!chosenFileNames.isEmpty()) {
                allChosenFiles.put(path.toString(), chosenFileNames);
            }
        }
    }

    private void prepareNewDirInfo() {
        txtPath.setText(path.getAbsolutePath());
        File[] files = path.listFiles(filter);
        Arrays.sort(files);
        fileInfos = new FileInfo[files.length];
        ArrayList<File> chsnFlNms;
        if (allChosenFiles.containsKey(path.toString())) {
            chsnFlNms = allChosenFiles.remove(path.toString());
        } else {
            chsnFlNms = new ArrayList<>();
        }
        for (int i = 0; i < files.length; i++) {
            fileInfos[i] = new FileInfo(files[i]);
            if (chsnFlNms.contains(files[i])) {
                fileInfos[i].changeChosen();
            }
        }
    }

    void chooseAllInDir() {
        for (int i = 0; i < fileInfos.length; i++) {
            if (fileInfos[i].isFile()) {
                if (checkFormat(fileInfos[i].getFile())) {
                    fileInfos[i].changeChosen();
                    notifyItemChanged(i);
                }
            }
        }
    }

    boolean undo() {
        if (!path.equals(Environment.getExternalStorageDirectory())) {
            prepareToLeaveDir();
            path = path.getParentFile();
            prepareNewDirInfo();
            notifyDataSetChanged();
            return true;
        }
        return false;
    }

    String[] getChosenFiles() {
        prepareToLeaveDir();
        if (allChosenFiles.size() == 0) {
            return null;
        }
        ArrayList<String> mp3filesPathes = new ArrayList<>();
        for (ArrayList<File> fileArrayList : allChosenFiles.values()) {
            for (File file : fileArrayList) {
                mp3filesPathes.add(file.getAbsolutePath());
            }
        }
        return mp3filesPathes.toArray(new String[mp3filesPathes.size()]);
    }

    private boolean checkFormat(File file) {
        String[] strings = file.getAbsolutePath().split("[.]");
        return (strings[strings.length - 1].equals("mp3"));
    }
}
