package nodo.privatemp3player.explorer;

import java.io.File;

class FileInfo {
    private final File file;
    private boolean isChosen = false;

    public FileInfo(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public boolean isChosen() {
        return isChosen;
    }

    public void changeChosen() {
        isChosen = !isChosen;
    }

    public String getName(){
        return file.getName();
    }

    public boolean isFile(){
        return file.isFile();
    }
}