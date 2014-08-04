/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2013, Enno Gottschalk <mrmaffen@googlemail.com>
 *
 *   Tomahawk is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Tomahawk is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Tomahawk. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tomahawk.libtomahawk.database;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.infosystem.InfoRequestData;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class provides a way of storing user created {@link org.tomahawk.libtomahawk.collection.Playlist}s
 * in the database
 */
public class DatabaseHelper {

    private static DatabaseHelper instance;

    public static final String PLAYLISTSDATASOURCE_RESULTSREPORTED
            = "org.tomahawk.tomahawk_android.playlistsdatasource_resultsreported";

    public static final String PLAYLISTSDATASOURCE_RESULTSREPORTED_PLAYLISTID
            = "org.tomahawk.tomahawk_android.playlistsdatasource_resultsreported_playlistid";

    public static final String CACHED_PLAYLIST_NAME = "Last used playlist";

    public static final String CACHED_PLAYLIST_ID = "cached_playlist_id";

    public static final String LOVEDITEMS_PLAYLIST_NAME = "My loved tracks";

    public static final String LOVEDITEMS_PLAYLIST_ID = "loveditems_playlist_id";

    public static final int FALSE = 0;

    public static final int TRUE = 1;

    public static final int REQUESTTYPE_POST = 0;

    public static final int REQUESTTYPE_PUT = 1;

    public static final int REQUESTTYPE_DELETE = 2;

    private boolean mInitialized;

    // Database fields
    private SQLiteDatabase mDatabase;

    private DatabaseHelper() {
    }

    public static DatabaseHelper getInstance() {
        if (instance == null) {
            synchronized (DatabaseHelper.class) {
                if (instance == null) {
                    instance = new DatabaseHelper();
                }
            }
        }
        return instance;
    }

    public void ensureInit() {
        if (!mInitialized) {
            mInitialized = true;
            TomahawkSQLiteHelper dbHelper = new TomahawkSQLiteHelper(TomahawkApp.getContext());
            dbHelper.close();
            mDatabase = dbHelper.getWritableDatabase();
        }
    }

    /**
     * Store the given {@link Playlist}
     *
     * @param playlist the given {@link Playlist}
     */
    public void storePlaylist(final Playlist playlist) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    ContentValues values = new ContentValues();
                    values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_NAME, playlist.getName());
                    values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_CURRENTTRACKINDEX,
                            playlist.getCurrentQueryIndex());
                    String insertId = playlist.getId();
                    values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_CURRENTREVISION,
                            playlist.getCurrentRevision());
                    values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID, insertId);
                    mDatabase.beginTransaction();
                    mDatabase.insertWithOnConflict(TomahawkSQLiteHelper.TABLE_PLAYLISTS, null,
                            values,
                            SQLiteDatabase.CONFLICT_REPLACE);
                    // Delete every already associated Track entry
                    mDatabase.delete(TomahawkSQLiteHelper.TABLE_TRACKS,
                            TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTID + " = \"" + insertId
                                    + "\"",
                            null
                    );
                    // Store every single Track in the database and store the relationship
                    // by storing the playlists's id with it
                    for (Query query : playlist.getQueries()) {
                        values.clear();
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTID, insertId);
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_TRACKNAME,
                                query.getBasicTrack().getName());
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_ARTISTNAME,
                                query.getBasicTrack().getArtist().getName());
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_ALBUMNAME,
                                query.getBasicTrack().getAlbum().getName());
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_RESULTHINT,
                                query.getTopTrackResultKey());
                        if (query.isFetchedViaHatchet()) {
                            values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_ISFETCHEDVIAHATCHET,
                                    TRUE);
                        } else {
                            values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_ISFETCHEDVIAHATCHET,
                                    FALSE);
                        }
                        mDatabase.insert(TomahawkSQLiteHelper.TABLE_TRACKS, null, values);
                    }
                    mDatabase.setTransactionSuccessful();
                    mDatabase.endTransaction();
                    sendReportResultsBroadcast(playlist.getId());
                }
            }
        }).start();
    }

    /**
     * Rename the given {@link Playlist}
     *
     * @param playlist the given {@link Playlist}
     * @param newName  the new playlist name
     */
    public void renamePlaylist(final Playlist playlist, final String newName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    ContentValues values = new ContentValues();
                    values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_NAME, newName);
                    values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_CURRENTTRACKINDEX,
                            playlist.getCurrentQueryIndex());
                    String insertId = playlist.getId();
                    values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_CURRENTREVISION,
                            playlist.getCurrentRevision());
                    values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID, insertId);
                    mDatabase.beginTransaction();
                    mDatabase.insertWithOnConflict(TomahawkSQLiteHelper.TABLE_PLAYLISTS, null,
                            values, SQLiteDatabase.CONFLICT_REPLACE);
                    mDatabase.setTransactionSuccessful();
                    mDatabase.endTransaction();
                    sendReportResultsBroadcast(playlist.getId());
                }
            }
        }).start();
    }

    /**
     * @return the stored {@link org.tomahawk.libtomahawk.collection.Playlist} with
     * CACHED_PLAYLIST_ID as its id
     */
    public Playlist getCachedPlaylist() {
        return getPlaylist(CACHED_PLAYLIST_ID);
    }

    /**
     * @return the stored {@link org.tomahawk.libtomahawk.collection.Playlist} with
     * LOVEDITEMS_PLAYLIST_ID as its id
     */
    public Playlist getLovedItemsPlaylist() {
        return getPlaylist(LOVEDITEMS_PLAYLIST_ID);
    }

    /**
     * @return every stored {@link org.tomahawk.libtomahawk.collection.Playlist} in the database
     */
    public ArrayList<Playlist> getPlaylists() {
        ArrayList<Playlist> playListList = new ArrayList<Playlist>();
        String[] columns = new String[]{TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID};
        Cursor playlistsCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_PLAYLISTS,
                columns, TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID + " != ? AND "
                        + TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID + " != ?",
                new String[]{CACHED_PLAYLIST_ID, LOVEDITEMS_PLAYLIST_ID}, null, null, null
        );
        playlistsCursor.moveToFirst();
        while (!playlistsCursor.isAfterLast()) {
            Playlist playlist = getEmptyPlaylist(playlistsCursor.getString(0));
            playListList.add(playlist);
            playlistsCursor.moveToNext();
        }
        playlistsCursor.close();
        return playListList;
    }

    /**
     * @param playlistId the id by which to get the correct {@link org.tomahawk.libtomahawk.collection.Playlist}
     * @return the stored {@link org.tomahawk.libtomahawk.collection.Playlist} with playlistId as
     * its id
     */
    public Playlist getEmptyPlaylist(String playlistId) {
        String[] columns = new String[]{TomahawkSQLiteHelper.PLAYLISTS_COLUMN_NAME,
                TomahawkSQLiteHelper.PLAYLISTS_COLUMN_CURRENTREVISION};
        Cursor playlistsCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_PLAYLISTS,
                columns, TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID + " = ?",
                new String[]{playlistId}, null, null, null);
        if (playlistsCursor.moveToFirst()) {
            Playlist playlist = Playlist.get(playlistId,
                    playlistsCursor.getString(0), playlistsCursor.getString(1));
            playlistsCursor.close();
            return playlist;
        }
        playlistsCursor.close();
        return null;
    }

    /**
     * @param playlistId the id by which to get the correct {@link org.tomahawk.libtomahawk.collection.Playlist}
     * @return the stored {@link org.tomahawk.libtomahawk.collection.Playlist} with playlistId as
     * its id
     */
    public Playlist getPlaylist(String playlistId) {
        ArrayList<Query> queries;
        String[] columns = new String[]{TomahawkSQLiteHelper.PLAYLISTS_COLUMN_NAME,
                TomahawkSQLiteHelper.PLAYLISTS_COLUMN_CURRENTREVISION,
                TomahawkSQLiteHelper.PLAYLISTS_COLUMN_CURRENTTRACKINDEX};
        Cursor playlistsCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_PLAYLISTS,
                columns, TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID + " = ?",
                new String[]{playlistId}, null, null, null);
        if (playlistsCursor.moveToFirst()) {
            ConcurrentHashMap<Integer, Long> queryIdMap = new ConcurrentHashMap<Integer, Long>();
            columns = new String[]{TomahawkSQLiteHelper.TRACKS_COLUMN_TRACKNAME,
                    TomahawkSQLiteHelper.TRACKS_COLUMN_ARTISTNAME,
                    TomahawkSQLiteHelper.TRACKS_COLUMN_ALBUMNAME,
                    TomahawkSQLiteHelper.TRACKS_COLUMN_RESULTHINT,
                    TomahawkSQLiteHelper.TRACKS_COLUMN_ISFETCHEDVIAHATCHET,
                    TomahawkSQLiteHelper.TRACKS_COLUMN_ID};
            Cursor tracksCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_TRACKS, columns,
                    TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTID + " = ?",
                    new String[]{playlistId}, null, null, null);
            queries = new ArrayList<Query>();
            tracksCursor.moveToFirst();
            int positionCounter = 0;
            while (!tracksCursor.isAfterLast()) {
                String trackName = tracksCursor.getString(0);
                String artistName = tracksCursor.getString(1);
                String albumName = tracksCursor.getString(2);
                String resultHint = tracksCursor.getString(3);
                Query query = Query.get(trackName, albumName, artistName, resultHint, false,
                        tracksCursor.getInt(4) == TRUE);
                queryIdMap.put(positionCounter++, tracksCursor.getLong(5));
                queries.add(query);
                tracksCursor.moveToNext();
            }
            Playlist playlist = Playlist.fromQueryList(playlistId, playlistsCursor.getString(0),
                    playlistsCursor.getString(1), queries, playlistsCursor.getInt(2));
            playlist.setFilled(true);
            tracksCursor.close();
            playlistsCursor.close();
            return playlist;
        }
        playlistsCursor.close();
        return null;
    }

    /**
     * @param playlistId the id by which to get the correct {@link org.tomahawk.libtomahawk.collection.Playlist}
     * @return the stored {@link org.tomahawk.libtomahawk.collection.Playlist} with playlistId as
     * its id
     */
    public int getPlaylistTrackCount(String playlistId) {
        int count = 0;
        String[] columns = new String[]{TomahawkSQLiteHelper.PLAYLISTS_COLUMN_NAME,
                TomahawkSQLiteHelper.PLAYLISTS_COLUMN_CURRENTREVISION,
                TomahawkSQLiteHelper.PLAYLISTS_COLUMN_CURRENTTRACKINDEX};
        Cursor playlistsCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_PLAYLISTS,
                columns, TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID + " = ?",
                new String[]{playlistId}, null, null, null);
        if (playlistsCursor.moveToFirst()) {
            columns = new String[]{TomahawkSQLiteHelper.TRACKS_COLUMN_ID};
            Cursor tracksCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_TRACKS, columns,
                    TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTID + " = ?",
                    new String[]{playlistId}, null, null, null);
            count = tracksCursor.getCount();
            tracksCursor.close();
        }
        playlistsCursor.close();
        return count;
    }

    /**
     * Delete the {@link org.tomahawk.libtomahawk.collection.Playlist} with the given id
     *
     * @param playlistId String containing the id of the {@link org.tomahawk.libtomahawk.collection.Playlist}
     *                   to be deleted
     */
    public void deletePlaylist(final String playlistId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    mDatabase.beginTransaction();
                    mDatabase.delete(TomahawkSQLiteHelper.TABLE_TRACKS,
                            TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTID + " = \""
                                    + playlistId + "\"", null
                    );
                    mDatabase.delete(TomahawkSQLiteHelper.TABLE_PLAYLISTS,
                            TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID + " = \"" + playlistId
                                    + "\"", null
                    );
                    mDatabase.setTransactionSuccessful();
                    mDatabase.endTransaction();
                    sendReportResultsBroadcast(playlistId);
                }
            }
        }).start();
    }

    /**
     * Delete the {@link org.tomahawk.libtomahawk.collection.PlaylistEntry} with the given key in
     * the {@link org.tomahawk.libtomahawk.collection.Playlist} with the given playlistId
     */
    public void deleteEntryInPlaylist(final String playlistId, final String entryId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    mDatabase.beginTransaction();
                    mDatabase.delete(TomahawkSQLiteHelper.TABLE_TRACKS,
                            TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTID
                                    + " = \"" + playlistId + "\"" + " and "
                                    + TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTENTRYID
                                    + " = " + entryId, null);
                    mDatabase.setTransactionSuccessful();
                    mDatabase.endTransaction();
                    sendReportResultsBroadcast(playlistId);
                }
            }
        }).start();
    }

    /**
     * Add the given {@link ArrayList} of {@link Track}s to the {@link
     * org.tomahawk.libtomahawk.collection.Playlist} with the given playlistId
     */
    public void addQueriesToPlaylist(final String playlistId, final ArrayList<Query> queries) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    mDatabase.beginTransaction();
                    // Store every single Track in the database and store the relationship
                    // by storing the playlists's id with it
                    for (Query query : queries) {
                        ContentValues values = new ContentValues();
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTID,
                                playlistId);
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_TRACKNAME,
                                query.getBasicTrack().getName());
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_ARTISTNAME,
                                query.getBasicTrack().getArtist().getName());
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_ALBUMNAME,
                                query.getBasicTrack().getAlbum().getName());
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_RESULTHINT,
                                query.getTopTrackResultKey());
                        if (query.isFetchedViaHatchet()) {
                            values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_ISFETCHEDVIAHATCHET,
                                    TRUE);
                        } else {
                            values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_ISFETCHEDVIAHATCHET,
                                    FALSE);
                        }
                        mDatabase.insert(TomahawkSQLiteHelper.TABLE_TRACKS, null, values);
                    }
                    mDatabase.setTransactionSuccessful();
                    mDatabase.endTransaction();
                    sendReportResultsBroadcast(playlistId);
                }
            }
        }).start();
    }

    /**
     * Checks if a query with the same track/artistName as the given query is included in the
     * lovedItems Playlist
     *
     * @return whether or not the given query is loved
     */
    public boolean isItemLoved(Query query) {
        String[] columns = new String[]{TomahawkSQLiteHelper.TRACKS_COLUMN_TRACKNAME,
                TomahawkSQLiteHelper.TRACKS_COLUMN_ARTISTNAME};
        Cursor tracksCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_TRACKS, columns,
                TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTID + " = ?",
                new String[]{LOVEDITEMS_PLAYLIST_ID}, null, null, null
        );
        tracksCursor.moveToFirst();
        while (!tracksCursor.isAfterLast()) {
            String trackName = tracksCursor.getString(0);
            String artistName = tracksCursor.getString(1);
            if (query.getName().equalsIgnoreCase(trackName)
                    && query.getArtist().getName().equalsIgnoreCase(artistName)) {
                tracksCursor.close();
                return true;
            }
            tracksCursor.moveToNext();
        }
        tracksCursor.close();
        return false;
    }

    /**
     * Checks if an artist with the same artistName as the given artist is loved
     *
     * @return whether or not the given artist is loved
     */
    public boolean isItemLoved(Artist artist) {
        boolean isLoved = false;
        String[] columns = new String[]{TomahawkSQLiteHelper.LOVED_ARTISTS_COLUMN_ARTISTNAME};
        Cursor artistsCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_LOVED_ARTISTS, columns,
                TomahawkSQLiteHelper.LOVED_ARTISTS_COLUMN_ARTISTNAME + " = ?",
                new String[]{artist.getName()}, null, null, null);
        if (artistsCursor.getCount() > 0) {
            isLoved = true;
        }
        artistsCursor.close();
        return isLoved;
    }

    /**
     * Checks if an album with the same albumName as the given album is loved
     *
     * @return whether or not the given album is loved
     */
    public boolean isItemLoved(Album album) {
        boolean isLoved = false;
        String[] columns = new String[]{TomahawkSQLiteHelper.LOVED_ALBUMS_COLUMN_ALBUMNAME,
                TomahawkSQLiteHelper.LOVED_ALBUMS_COLUMN_ARTISTNAME};
        Cursor albumsCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_LOVED_ALBUMS, columns,
                TomahawkSQLiteHelper.LOVED_ALBUMS_COLUMN_ALBUMNAME + " = ? AND "
                        + TomahawkSQLiteHelper.LOVED_ALBUMS_COLUMN_ARTISTNAME + " = ?",
                new String[]{album.getName(), album.getArtist().getName()}, null, null, null
        );
        if (albumsCursor.getCount() > 0) {
            isLoved = true;
        }
        albumsCursor.close();
        return isLoved;
    }

    /**
     * Store the given query as a lovedItem, if isLoved is true. Otherwise remove(unlove) the
     * query.
     */
    public void setLovedItem(Query query, boolean isLoved) {
        if (isLoved) {
            ArrayList<Query> queries = new ArrayList<Query>();
            queries.add(query);
            addQueriesToPlaylist(DatabaseHelper.LOVEDITEMS_PLAYLIST_ID, queries);
        } else {
            mDatabase.beginTransaction();
            mDatabase.delete(TomahawkSQLiteHelper.TABLE_TRACKS,
                    TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTID
                            + " = \"" + LOVEDITEMS_PLAYLIST_ID + "\""
                            + " AND " + TomahawkSQLiteHelper.TRACKS_COLUMN_TRACKNAME
                            + " = \"" + query.getName() + "\""
                            + " AND " + TomahawkSQLiteHelper.TRACKS_COLUMN_ARTISTNAME
                            + " = \"" + query.getArtist().getName() + "\"", null
            );
            mDatabase.setTransactionSuccessful();
            mDatabase.endTransaction();
        }
    }

    /**
     * Store the given artist as a lovedItem, if isLoved is true. Otherwise remove(unlove) it.
     */
    public void setLovedItem(final Artist artist, final boolean isLoved) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    mDatabase.beginTransaction();
                    if (isLoved) {
                        ContentValues values = new ContentValues();
                        values.put(TomahawkSQLiteHelper.LOVED_ARTISTS_COLUMN_ARTISTNAME,
                                artist.getName());
                        mDatabase.insert(TomahawkSQLiteHelper.TABLE_LOVED_ARTISTS, null, values);
                    } else {
                        mDatabase.delete(TomahawkSQLiteHelper.TABLE_LOVED_ARTISTS,
                                TomahawkSQLiteHelper.LOVED_ARTISTS_COLUMN_ARTISTNAME
                                        + " = \"" + artist.getName() + "\"", null
                        );
                    }
                    mDatabase.setTransactionSuccessful();
                    mDatabase.endTransaction();
                }
            }
        }).start();
    }

    /**
     * Store the given album as a lovedItem, if isLoved is true. Otherwise remove(unlove) it.
     */
    public void setLovedItem(final Album album, final boolean isLoved) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    mDatabase.beginTransaction();
                    if (isLoved) {
                        ContentValues values = new ContentValues();
                        values.put(TomahawkSQLiteHelper.LOVED_ALBUMS_COLUMN_ALBUMNAME,
                                album.getName());
                        values.put(TomahawkSQLiteHelper.LOVED_ALBUMS_COLUMN_ARTISTNAME,
                                album.getArtist().getName());
                        mDatabase.insert(TomahawkSQLiteHelper.TABLE_LOVED_ALBUMS, null, values);
                    } else {
                        mDatabase.delete(TomahawkSQLiteHelper.TABLE_LOVED_ALBUMS,
                                TomahawkSQLiteHelper.LOVED_ALBUMS_COLUMN_ALBUMNAME
                                        + " = \"" + album.getName() + "\""
                                        + " AND "
                                        + TomahawkSQLiteHelper.LOVED_ALBUMS_COLUMN_ARTISTNAME
                                        + " = \"" + album.getArtist().getName() + "\"", null
                        );
                    }
                    mDatabase.setTransactionSuccessful();
                    mDatabase.endTransaction();
                }
            }
        }).start();
    }

    public void storeStarredArtists(final List<Artist> artists) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    mDatabase.beginTransaction();
                    mDatabase.delete(TomahawkSQLiteHelper.TABLE_LOVED_ARTISTS, null, null);
                    for (Artist artist : artists) {
                        ContentValues values = new ContentValues();
                        values.put(TomahawkSQLiteHelper.LOVED_ARTISTS_COLUMN_ARTISTNAME,
                                artist.getName());
                        mDatabase.insert(TomahawkSQLiteHelper.TABLE_LOVED_ARTISTS, null, values);
                    }
                    mDatabase.setTransactionSuccessful();
                    mDatabase.endTransaction();
                    sendReportResultsBroadcast(null);
                }
            }
        }).start();
    }

    public void storeStarredAlbums(final List<Album> albums) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    mDatabase.beginTransaction();
                    mDatabase.delete(TomahawkSQLiteHelper.TABLE_LOVED_ALBUMS, null, null);
                    for (Album album : albums) {
                        ContentValues values = new ContentValues();
                        values.put(TomahawkSQLiteHelper.LOVED_ALBUMS_COLUMN_ALBUMNAME,
                                album.getName());
                        values.put(TomahawkSQLiteHelper.LOVED_ALBUMS_COLUMN_ARTISTNAME,
                                album.getArtist().getName());
                        mDatabase.insert(TomahawkSQLiteHelper.TABLE_LOVED_ALBUMS, null, values);
                    }
                    mDatabase.setTransactionSuccessful();
                    mDatabase.endTransaction();
                    sendReportResultsBroadcast(null);
                }
            }
        }).start();
    }

    public ArrayList<Artist> getStarredArtists() {
        ArrayList<Artist> starredArtists = new ArrayList<Artist>();
        String[] columns = new String[]{TomahawkSQLiteHelper.LOVED_ARTISTS_COLUMN_ARTISTNAME};
        Cursor artistsCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_LOVED_ARTISTS, columns,
                null, null, null, null, null);
        artistsCursor.moveToFirst();
        while (!artistsCursor.isAfterLast()) {
            String artistName = artistsCursor.getString(0);
            starredArtists.add(Artist.get(artistName));
            artistsCursor.moveToNext();
        }
        artistsCursor.close();
        return starredArtists;
    }

    public ArrayList<Album> getStarredAlbums() {
        ArrayList<Album> starredAlbums = new ArrayList<Album>();
        String[] columns = new String[]{TomahawkSQLiteHelper.LOVED_ALBUMS_COLUMN_ARTISTNAME,
                TomahawkSQLiteHelper.LOVED_ALBUMS_COLUMN_ALBUMNAME};
        Cursor albumsCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_LOVED_ALBUMS,
                columns, null, null, null, null, null);
        albumsCursor.moveToFirst();
        while (!albumsCursor.isAfterLast()) {
            String artistName = albumsCursor.getString(0);
            String albumName = albumsCursor.getString(1);
            starredAlbums.add(Album.get(albumName, Artist.get(artistName)));
            albumsCursor.moveToNext();
        }
        albumsCursor.close();
        return starredAlbums;
    }

    public Cursor getSearchHistoryCursor(String entry) {
        return mDatabase.query(TomahawkSQLiteHelper.TABLE_SEARCHHISTORY, null,
                TomahawkSQLiteHelper.SEARCHHISTORY_COLUMN_ENTRY + " LIKE ?",
                new String[]{entry + "%"}, null, null,
                TomahawkSQLiteHelper.SEARCHHISTORY_COLUMN_ID + " DESC");
    }

    public void addEntryToSearchHistory(String entry) {
        ContentValues values = new ContentValues();
        mDatabase.beginTransaction();
        values.put(TomahawkSQLiteHelper.SEARCHHISTORY_COLUMN_ENTRY, entry.trim());
        mDatabase.insert(TomahawkSQLiteHelper.TABLE_SEARCHHISTORY, null, values);
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
    }

    /**
     * Add an operation to the log. This operation log is being used to store pending operations, so
     * that this operation can be executed, if we have the opportunity to do so.
     *
     * @param opToLog     InfoRequestData object containing the type of the operation, which
     *                    determines where and how to send the data to the API. Contains also the
     *                    JSON-String which contains the data to send.
     * @param timeStamp   a timestamp indicating when this operation has been added to the oplog
     * @param requestType the type of the request (post, put, delete)
     */
    public void addOpToInfoSystemOpLog(InfoRequestData opToLog, int timeStamp, int requestType) {
        ContentValues values = new ContentValues();
        mDatabase.beginTransaction();
        values.put(TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_TYPE, opToLog.getType());
        values.put(TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_REQUESTTYPE, requestType);
        values.put(TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_JSONSTRING,
                opToLog.getJsonStringToSend());
        values.put(TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_TIMESTAMP, timeStamp);
        mDatabase.insert(TomahawkSQLiteHelper.TABLE_INFOSYSTEMOPLOG, null, values);
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
    }

    /**
     * Remove the operation with the given id from the InfoSystem-OpLog table
     *
     * @param loggedOps a list of all the operations to remove from the InfoSystem-OpLog table
     */
    public void removeOpsFromInfoSystemOpLog(List<InfoRequestData> loggedOps) {
        mDatabase.beginTransaction();
        for (InfoRequestData loggedOp : loggedOps) {
            mDatabase.delete(TomahawkSQLiteHelper.TABLE_INFOSYSTEMOPLOG,
                    TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_ID + " = " + loggedOp.getOpLogId(),
                    null);
        }
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
    }

    /**
     * Remove the operation with the given id from the InfoSystem-OpLog table
     *
     * @param opLogId the id of the operation to remove from the InfoSystem-OpLog table
     */
    public void removeOpFromInfoSystemOpLog(int opLogId) {
        mDatabase.beginTransaction();
        mDatabase.delete(TomahawkSQLiteHelper.TABLE_INFOSYSTEMOPLOG,
                TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_ID + " = " + opLogId, null);
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
    }

    /**
     * @return an InfoRequestData object that contains all data that should be delivered to the API
     */
    public List<InfoRequestData> getLoggedOps() {
        List<InfoRequestData> loggedOps = new ArrayList<InfoRequestData>();
        String[] columns = new String[]{TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_ID,
                TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_TYPE,
                TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_JSONSTRING};
        Cursor opLogCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_INFOSYSTEMOPLOG,
                columns, null, null, null, null,
                TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_TIMESTAMP + " DESC");
        opLogCursor.moveToFirst();
        while (!opLogCursor.isAfterLast()) {
            String requestId = TomahawkMainActivity.getSessionUniqueStringId();
            InfoRequestData infoRequestData = new InfoRequestData(requestId, opLogCursor.getInt(0),
                    opLogCursor.getInt(1), opLogCursor.getString(2));
            loggedOps.add(infoRequestData);
            opLogCursor.moveToNext();
        }
        opLogCursor.close();
        return loggedOps;
    }

    /**
     * @return the count of all logged ops that should be delivered to the API
     */
    public int getLoggedOpsCount() {
        String[] columns = new String[]{TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_ID};
        Cursor opLogCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_INFOSYSTEMOPLOG,
                columns, null, null, null, null, null);
        int count = opLogCursor.getCount();
        opLogCursor.close();
        return count;
    }

    /**
     * Send a broadcast indicating that playlists have been changed in the database and should be
     * refetched
     */
    private void sendReportResultsBroadcast(String playlistId) {
        Intent reportIntent = new Intent(PLAYLISTSDATASOURCE_RESULTSREPORTED);
        if (playlistId != null) {
            reportIntent.putExtra(PLAYLISTSDATASOURCE_RESULTSREPORTED_PLAYLISTID, playlistId);
        }
        TomahawkApp.getContext().sendBroadcast(reportIntent);
    }
}
