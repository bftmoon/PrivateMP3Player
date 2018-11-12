package nodo.privatemp3player.player_main;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nodo.privatemp3player.R;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.SongsViewHolder> implements Filterable {


    private int lastSongNum = -1, currentSongNum = -1;  // all is real nums
    private final ArrayList<String> songnames = new ArrayList<>();
    private ArrayList<String> namesbuffer; // for saving names in a time of searchmode
    private ArrayList<Integer> songnumsForDeleting;  // must be real nums in server (saving nums when search text changed)
    private boolean delMode, searchMode;
    private final OnPlaylistClickListener listener;
    private final View.OnTouchListener touchListener;

    PlaylistAdapter(OnPlaylistClickListener listener, View.OnTouchListener touchListener) {
        this.listener = listener;
        this.touchListener = touchListener;
    }

    class SongsViewHolder extends RecyclerView.ViewHolder {

        final TextView txtSongName;
        final ImageView imgPlaying;

        SongsViewHolder(View itemView) {
            super(itemView);
            this.txtSongName = itemView.findViewById(R.id.txtFileFolder);
            this.imgPlaying = itemView.findViewById(R.id.imgFileFolder);
            itemView.setOnTouchListener(touchListener);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (delMode) {
                        Log.d("PlaylistAdapter.Holder", "Delete: Clicked song for deleting");
                        int pos = getLayoutPosition();
                        int server_pos = getRealNumByNumInList(pos);
                        if (songnumsForDeleting.contains(server_pos)) {
                            songnumsForDeleting.remove(Integer.valueOf(server_pos));
                            notifyItemChanged(pos);
                            Log.d("PlaylistAdapter.Holder", "Delete: Unchoose");

                        } else {
                            songnumsForDeleting.add(server_pos);
                            notifyItemChanged(pos);
                            Log.d("PlaylistAdapter.Holder", "Delete: Choose");
                        }
                        notifyItemChanged(pos);
                    } else {
                        Log.d("PlaylistAdapter.Holder", "Click song for start or stop");
                        lastSongNum = currentSongNum;
                        currentSongNum = getRealNumByNumInList(getLayoutPosition());
                        listener.onSongItemClick(currentSongNum);
                        notifyItemChanged(getLayoutPosition());
                        if (lastSongNum != -1)
                            notifyItemChanged(getNumInListByRealNum(lastSongNum));
                    }
                }
            });
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Log.d("PlaylistAdapter.Holder", "Call onLongClick");
                    if (!delMode) {
                        Log.d("PlaylistAdapter.Holder", "Delete: DeleteMode activated");
                        songnumsForDeleting = new ArrayList<>();
                        songnumsForDeleting.add(getRealNumByNumInList(getLayoutPosition()));
                        listener.onSongItemLongClick();
                        delMode = true;
                        notifyItemChanged(getLayoutPosition());
                    } else {
                        Log.d("PlaylistAdapter.Holder", "Delete: DeleteMode already work");
                    }
                    return false;
                }
            });
        }

        private void bind(int pos) {
            txtSongName.setText(songnames.get(pos));
            imgPlaying.setBackgroundResource(
                    (currentSongNum != -1 && pos == getNumInListByRealNum(currentSongNum)) ?
                            R.mipmap.one_note : R.color.colorNo);
        }

        private void bindInDelMode(int pos) {
            txtSongName.setText(songnames.get(pos));
            imgPlaying.setBackgroundResource(
                    songnumsForDeleting.contains(getRealNumByNumInList(pos)) ?
                            R.mipmap.dagger : R.color.colorNo);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull SongsViewHolder holder, int position) {
        if (delMode) {
            holder.bindInDelMode(position);
        } else {
            holder.bind(position);
        }
    }

    @NonNull
    @Override
    public SongsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SongsViewHolder(LayoutInflater.from(
                parent.getContext()).inflate(R.layout.item_explorer, parent, false));
    }

    @Override
    public int getItemCount() {
        return songnames.size();
    }

    //////////// Songnames work ////////////////////

    void setSongnames(ArrayList<String> songnames) {
        this.songnames.clear();
        this.songnames.addAll(songnames);
        notifyDataSetChanged();
    }

    boolean isEmptySongnames() {
        return songnames.isEmpty();
    }

    /////////// One song work ////////////////////////

    boolean wasSongChosen() {
        return currentSongNum != -1;
    }

    void setSongnum(int songnum) {
        lastSongNum = currentSongNum;
        currentSongNum = songnum;
        notifyItemChanged(lastSongNum);
        notifyItemChanged(currentSongNum);
    }

    /////////////////////////////////////
    ////////////// Search mode ///////////////

    private final Filter filter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (!searchMode) {
                Log.d("PlaylistAdapter", "Search: mode on");
                namesbuffer = new ArrayList<>(songnames);
                searchMode = true;
            }
            List<String> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                Log.d("PlaylistAdapter", "Search: No search string. Add all in buffer.");
                filteredList.addAll(namesbuffer);
            } else {
                String filterStr = constraint.toString().toLowerCase().trim();
                for (String name :
                        namesbuffer) {
                    if (name.toLowerCase().contains(filterStr)) {
                        filteredList.add(name);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            songnames.clear();
            songnames.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };

    @Override
    public Filter getFilter() {
        Log.d("PlaylistAdapter", "Search: Call getFilter");
        return filter;
    }

    void cancelSearchMode() {
        Log.d("PlaylistAdapter", "Search: Cancel mode");
        this.searchMode = false;
        namesbuffer = null;
    }

    boolean isSearchMode() {
        return searchMode;
    }

    private int getRealNumByNumInList(int num_in_list) {
        return searchMode ? namesbuffer.indexOf(songnames.get(num_in_list)) : num_in_list;
    }

    private int getNumInListByRealNum(int real_num) {
        return searchMode ? songnames.indexOf(namesbuffer.get(real_num)) : real_num;
    }

    ///////////////////////////////////////////////////
    ///////////////////// Delete mode /////////////////

    boolean isDelMode() {
        return delMode;
    }

    void cancelDelMode() {
        Log.d("PlaylistAdapter", "Delete: Call cancelDelMode");
        for (int i = songnumsForDeleting.size() - 1; i >= 0; i--) {
            notifyItemChanged(getNumInListByRealNum(songnumsForDeleting.remove(i)));
        }
        delMode = false;
    }

    ArrayList<Integer> removeSongnumsForDeleting() {
        Log.d("PlaylistAdapter", "Delete: Call remove SongnumsForDeleting");
        Collections.sort(songnumsForDeleting);
        for (int i = songnumsForDeleting.size() - 1; i >= 0; i--) {
            int num = songnumsForDeleting.get(i);
            if (num == currentSongNum)
                currentSongNum = -1;
            else if (num == lastSongNum)
                lastSongNum = -1;
            if (searchMode) {
                Log.d("PlaylistAdapter", "Delete, Search: Deleting in searchMode");
                songnames.remove(namesbuffer.remove(num));
            } else {
                Log.d("PlaylistAdapter", "Delete: Deleting not in searchMode");
                songnames.remove(num);
            }
        }

        ArrayList<Integer> delThis = new ArrayList<>(songnumsForDeleting);
        songnumsForDeleting.clear();
        notifyDataSetChanged();
        delMode = false;
        return delThis;
    }
}

