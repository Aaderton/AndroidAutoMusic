package fi.jamk.androidautomusic;

import android.app.Activity;
import android.content.ComponentName;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private BrowseAdapter mBrowserAdapter;
    private ImageButton mPlayPause;
    private ImageButton mPlayPrevious;
    private ImageButton mPlayNext;
    private TextView mTitle;
    private TextView mSubtitle;
    private ImageView mAlbumArt;
    //private ListView bgImage;
    private ViewGroup mPlaybackControls;

    private MediaMetadata mCurrentMetadata;
    private PlaybackState mCurrentState;

    private MediaBrowser mMediaBrowser;

    //MediaBrowser to allow a client to browse media content
    private final MediaBrowser.ConnectionCallback mConnectionCallback =
            new MediaBrowser.ConnectionCallback() {
                @Override
                public void onConnected() {
                    mMediaBrowser.subscribe(mMediaBrowser.getRoot(), mSubscriptionCallback);
                    MediaController mediaController = new MediaController(
                            MainActivity.this, mMediaBrowser.getSessionToken());
                    updatePlaybackState(mediaController.getPlaybackState());
                    updateMetadata(mediaController.getMetadata());
                    mediaController.registerCallback(mMediaControllerCallback);
                    setMediaController(mediaController);
                }
            };

    //receive callbacks from the MediaController and update state like which queue
    // is being shown, title, description and the PlaybackState
    private final MediaController.Callback mMediaControllerCallback = new MediaController.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            updateMetadata(metadata);
            mBrowserAdapter.notifyDataSetChanged();
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            updatePlaybackState(state);
            mBrowserAdapter.notifyDataSetChanged();
        }

        @Override
        public void onSessionDestroyed() {
            updatePlaybackState(null);
            mBrowserAdapter.notifyDataSetChanged();
        }
    };

    private final MediaBrowser.SubscriptionCallback mSubscriptionCallback =
        new MediaBrowser.SubscriptionCallback() {
            @Override
            public void onChildrenLoaded(String parentId, List<MediaBrowser.MediaItem> children) {
                onMediaLoaded(children);
            }
        };

    private void onMediaLoaded(List<MediaBrowser.MediaItem> media) {
        mBrowserAdapter.clear();
        mBrowserAdapter.addAll(media);
        mBrowserAdapter.notifyDataSetChanged();
    }

    private void onMediaItemSelected(MediaBrowser.MediaItem item) {
        if (item.isPlayable()) {
            getMediaController().getTransportControls().playFromMediaId(item.getMediaId(), null);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        setTitle(getString(R.string.app_name));
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));


        mBrowserAdapter = new BrowseAdapter(this);

        //create the ListView for songs
        ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setAdapter(mBrowserAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MediaBrowser.MediaItem item = mBrowserAdapter.getItem(position);
                onMediaItemSelected(item);
            }
        });

        //user input controls
        mPlaybackControls = (ViewGroup) findViewById(R.id.playback_controls);
        mPlayPause = (ImageButton) findViewById(R.id.play_pause);
        mPlayPause.setEnabled(true);
        mPlayPause.setOnClickListener(playPauseListener);

        mPlayPrevious = (ImageButton) findViewById(R.id.play_previous);
        mPlayPrevious.setOnClickListener(playPreviousListener);

        mPlayNext = (ImageButton) findViewById(R.id.play_next);
        mPlayNext.setOnClickListener(playNextListener);

        mTitle = (TextView) findViewById(R.id.title);
        mSubtitle = (TextView) findViewById(R.id.artist);
        mAlbumArt = (ImageView) findViewById(R.id.album_art);

        //bgImage = (ListView) findViewById(R.id.list_view);
    }

    @Override
    public void onStart() {
        super.onStart();

        mMediaBrowser = new MediaBrowser(this,
               new ComponentName(this, MusicService.class), mConnectionCallback, null);
        mMediaBrowser.connect();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (getMediaController() != null) {
            getMediaController().unregisterCallback(mMediaControllerCallback);
        }
        if (mMediaBrowser != null && mMediaBrowser.isConnected()) {
            mMediaBrowser.unsubscribe(mCurrentMetadata.getDescription().getMediaId());
            mMediaBrowser.disconnect();
        }
    }
    //update state and icons
    private void updatePlaybackState(PlaybackState state) {
        mCurrentState = state;
        if (state == null || state.getState() == PlaybackState.STATE_PAUSED ||
                state.getState() == PlaybackState.STATE_STOPPED) {
            mPlayPause.setImageDrawable(getDrawable(R.drawable.ic_play_circle_filled_black_48dp));
        } else {
            //bgImage.setBackground(mAlbumArt.getDrawable());
            mPlayPause.setImageDrawable(getDrawable(R.drawable.ic_pause_circle_filled_black_48dp));
        }
        mPlaybackControls.setVisibility(state == null ? View.GONE : View.VISIBLE);
    }

    private void updateMetadata(MediaMetadata metadata) {
        mCurrentMetadata = metadata;
        mTitle.setText(metadata == null ? "" : metadata.getDescription().getTitle());
        mSubtitle.setText(metadata == null ? "" : metadata.getDescription().getSubtitle());
        mAlbumArt.setImageBitmap(metadata == null ? null : fi.jamk.androidautomusic.MusicLibrary.getAlbumBitmap(this,
                metadata.getDescription().getMediaId()));
        mBrowserAdapter.notifyDataSetChanged();
    }

    //adapter for showing the list of browsed MediaItems
    private class BrowseAdapter extends ArrayAdapter<MediaBrowser.MediaItem> {

        public BrowseAdapter(Activity context) {
            super(context, R.layout.media_list_item, new ArrayList<MediaBrowser.MediaItem>());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            MediaBrowser.MediaItem item = getItem(position);
            int itemState = fi.jamk.androidautomusic.MediaItemViewHolder.STATE_NONE;
            if (item.isPlayable()) {
                String itemMediaId = item.getDescription().getMediaId();
                int playbackState = PlaybackState.STATE_NONE;
                if (mCurrentState != null) {
                    playbackState = mCurrentState.getState();
                }
                if (mCurrentMetadata != null &&
                        itemMediaId.equals(mCurrentMetadata.getDescription().getMediaId())) {
                    if (playbackState == PlaybackState.STATE_PLAYING ||
                            playbackState == PlaybackState.STATE_BUFFERING) {
                        itemState = fi.jamk.androidautomusic.MediaItemViewHolder.STATE_PLAYING;
                    } else if (playbackState != PlaybackState.STATE_ERROR) {
                        itemState = fi.jamk.androidautomusic.MediaItemViewHolder.STATE_PAUSED;
                    }
                }
            }
            return fi.jamk.androidautomusic.MediaItemViewHolder.setupView((Activity) getContext(), convertView, parent,
                    item.getDescription(), itemState);
        }
    }
    //previous song button functionality
    private final View.OnClickListener playPreviousListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            getMediaController().getTransportControls().skipToPrevious();
        }
    };
    //next song button functionality
    private final View.OnClickListener playNextListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            getMediaController().getTransportControls().skipToNext();
        }
    };
    //play/pause functionality
    private final View.OnClickListener playPauseListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final int state = mCurrentState == null ?
                    PlaybackState.STATE_NONE : mCurrentState.getState();
            if (state == PlaybackState.STATE_PAUSED ||
                    state == PlaybackState.STATE_STOPPED ||
                    state == PlaybackState.STATE_NONE) {

                if (mCurrentMetadata == null) {
                    mCurrentMetadata = fi.jamk.androidautomusic.MusicLibrary.getMetadata(MainActivity.this,
                            fi.jamk.androidautomusic.MusicLibrary.getMediaItems().get(0).getMediaId());
                    updateMetadata(mCurrentMetadata);
                }
                getMediaController().getTransportControls().playFromMediaId(
                        mCurrentMetadata.getDescription().getMediaId(), null);

            } else {
                getMediaController().getTransportControls().pause();
            }
        }
    };

}
