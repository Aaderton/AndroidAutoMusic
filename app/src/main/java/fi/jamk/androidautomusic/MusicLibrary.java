package fi.jamk.androidautomusic;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;


class MusicLibrary {

    // hard coding some songs with information and album art
    private static final TreeMap<String, MediaMetadata> music = new TreeMap<>();
    private static final HashMap<String, Integer> albumRes = new HashMap<>();
    private static final HashMap<String, Integer> musicRes = new HashMap<>();
    static {
        createMediaMetadata("Time", "Time",
                "Jungle", "Jungle", "Funk", 214,
                R.raw.time, R.drawable.jungle_album, "jungle_album");
        createMediaMetadata("Cocaine",
                "Cocaine", "Eric Clapton", "Slowhand", "Rock", 212,
                R.raw.cocaine, R.drawable.clapton_album,
                "clapton_album");
        createMediaMetadata("Scar_Tissue",
                "Scar Tissue", "Red Hot Chili Peppers", "Californication", "Rock", 218,
                R.raw.scar_tissue, R.drawable.rhcp_album,
                "rhcp_album");
        createMediaMetadata("Clint_Eastwood",
                "Clint Eastwood", "Gorillaz", "Demon Days", "Hip Hop", 274,
                R.raw.clint_eastwood, R.drawable.gorillaz_album,
                "gorillaz_album");
    }

    public static String getRoot() {
        return "root";
    }

    public static String getSongUri(String mediaId) {
        return "android.resource://" + BuildConfig.APPLICATION_ID + "/" + getMusicRes(mediaId);
    }

    private static String getAlbumArtUri(String albumArtResName) {
        return "android.resource://" + BuildConfig.APPLICATION_ID + "/drawable/" + albumArtResName;
    }

    private static int getMusicRes(String mediaId) {
        return musicRes.containsKey(mediaId) ? musicRes.get(mediaId) : 0;
    }

    private static int getAlbumRes(String mediaId) {
        return albumRes.containsKey(mediaId) ? albumRes.get(mediaId) : 0;
    }

    public static Bitmap getAlbumBitmap(Context ctx, String mediaId) {
        return BitmapFactory.decodeResource(ctx.getResources(), MusicLibrary.getAlbumRes(mediaId));
    }

    public static List<MediaBrowser.MediaItem> getMediaItems() {
        List<MediaBrowser.MediaItem> result = new ArrayList<>();
        for (MediaMetadata metadata: music.values()) {
            result.add(new MediaBrowser.MediaItem(metadata.getDescription(),
                    MediaBrowser.MediaItem.FLAG_PLAYABLE));
        }
        return result;
    }

    public static String getPreviousSong(String currentMediaId) {
        String prevMediaId = music.lowerKey(currentMediaId);
        if (prevMediaId == null) {
            prevMediaId = music.firstKey();
        }
        return prevMediaId;
    }

    public static String getNextSong(String currentMediaId) {
        String nextMediaId = music.higherKey(currentMediaId);
        if (nextMediaId == null) {
            nextMediaId = music.firstKey();
        }
        return nextMediaId;
    }

    public static MediaMetadata getMetadata(Context ctx, String mediaId) {
        MediaMetadata metadataWithoutBitmap = music.get(mediaId);
        Bitmap albumArt = getAlbumBitmap(ctx, mediaId);

        // Since MediaMetadata is immutable, we need to create a copy to set the album art
        // We don't set it initially on all items so that they don't take unnecessary memory
        MediaMetadata.Builder builder = new MediaMetadata.Builder();
        for (String key: new String[]{MediaMetadata.METADATA_KEY_MEDIA_ID,
                MediaMetadata.METADATA_KEY_ALBUM, MediaMetadata.METADATA_KEY_ARTIST,
                MediaMetadata.METADATA_KEY_GENRE, MediaMetadata.METADATA_KEY_TITLE}) {
            builder.putString(key, metadataWithoutBitmap.getString(key));
        }
        builder.putLong(MediaMetadata.METADATA_KEY_DURATION,
                metadataWithoutBitmap.getLong(MediaMetadata.METADATA_KEY_DURATION));
        builder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, albumArt);
        return builder.build();
    }

    private static void createMediaMetadata(String mediaId, String title, String artist,
                String album, String genre, long duration, int musicResId, int albumArtResId,
                String albumArtResName) {
        music.put(mediaId,
                new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, mediaId)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, album)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, duration * 1000)
                .putString(MediaMetadata.METADATA_KEY_GENRE, genre)
                .putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, getAlbumArtUri(albumArtResName))
                .putString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI, getAlbumArtUri(albumArtResName))
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .build());
        albumRes.put(mediaId, albumArtResId);
        musicRes.put(mediaId, musicResId);
    }
}
