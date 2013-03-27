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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com> Date: 06.02.13
 */
public class TomahawkSQLiteHelper extends SQLiteOpenHelper {

    public static final String TAG = TomahawkSQLiteHelper.class.getName();

    public static final String TABLE_USERPLAYLISTS = "userplaylists";

    public static final String USERPLAYLISTS_COLUMN_ID = "id";

    public static final String USERPLAYLISTS_COLUMN_NAME = "name";

    public static final String USERPLAYLISTS_COLUMN_CURRENTTRACKINDEX = "currenttrackindex";

    public static final String TABLE_TRACKS = "tracks";

    public static final String TRACKS_COLUMN_ID = "id";

    public static final String TRACKS_COLUMN_IDUSERPLAYLISTS = "id_userplaylists";

    public static final String TRACKS_COLUMN_TRACKNAME = "trackname";

    public static final String TRACKS_COLUMN_IDALBUMS = "id_albums";

    public static final String TRACKS_COLUMN_ARTISTNAME = "artistname";

    public static final String TRACKS_COLUMN_PATH = "path";

    public static final String TRACKS_COLUMN_BITRATE = "bitrate";

    public static final String TRACKS_COLUMN_DURATION = "duration";

    public static final String TRACKS_COLUMN_SIZE = "size";

    public static final String TRACKS_COLUMN_TRACKNUMBER = "tracknumber";

    public static final String TRACKS_COLUMN_YEAR = "year";

    public static final String TRACKS_COLUMN_RESOLVERID = "resolverid";

    public static final String TRACKS_COLUMN_LINKURL = "linkurl";

    public static final String TRACKS_COLUMN_PURCHASEURL = "purchaseurl";

    public static final String TRACKS_COLUMN_SCORE = "score";

    public static final String TABLE_ALBUMS = "albums";

    public static final String ALBUMS_COLUMN_ID = "id";

    public static final String ALBUMS_COLUMN_NAME = "name";

    public static final String ALBUMS_COLUMN_ALBUMART = "albumart";

    public static final String ALBUMS_COLUMN_FIRSTYEAR = "firstyear";

    public static final String ALBUMS_COLUMN_LASTYEAR = "lastyear";

    private static final String DATABASE_NAME = "userplaylists.db";

    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String CREATE_TABLE_USERPLAYLISTS = "CREATE TABLE `" + TABLE_USERPLAYLISTS
            + "` (  `" + USERPLAYLISTS_COLUMN_ID + "` INTEGER PRIMARY KEY AUTOINCREMENT ,  `"
            + USERPLAYLISTS_COLUMN_NAME + "` TEXT , `" + USERPLAYLISTS_COLUMN_CURRENTTRACKINDEX
            + "` INTEGER );";

    private static final String CREATE_TABLE_ALBUMS = "CREATE TABLE `" + TABLE_ALBUMS + "` (  `"
            + ALBUMS_COLUMN_ID + "` INTEGER PRIMARY KEY AUTOINCREMENT ,  `" + ALBUMS_COLUMN_NAME
            + "` TEXT , `" + ALBUMS_COLUMN_ALBUMART + "` TEXT , `" + ALBUMS_COLUMN_FIRSTYEAR
            + "` INTEGER, `" + ALBUMS_COLUMN_LASTYEAR + "` INTEGER );";

    private static final String CREATE_TABLE_TRACKS = "CREATE TABLE `" + TABLE_TRACKS + "` (  `"
            + TRACKS_COLUMN_ID + "` INTEGER PRIMARY KEY AUTOINCREMENT , `"
            + TRACKS_COLUMN_IDUSERPLAYLISTS + "` INTEGER ,  `" + TRACKS_COLUMN_TRACKNAME
            + "` TEXT ,  `" + TRACKS_COLUMN_IDALBUMS + "` INTEGER ,  `" + TRACKS_COLUMN_ARTISTNAME
            + "` TEXT ,   `" + TRACKS_COLUMN_PATH + "` TEXT , `" + TRACKS_COLUMN_BITRATE
            + "` INTEGER , `" + TRACKS_COLUMN_DURATION + "` INTEGER, `" + TRACKS_COLUMN_SIZE
            + "` INTEGER, `" + TRACKS_COLUMN_TRACKNUMBER + "` INTEGER, `" + TRACKS_COLUMN_YEAR
            + "` INTEGER, `" + TRACKS_COLUMN_RESOLVERID + "` INTEGER, `" + TRACKS_COLUMN_LINKURL
            + "` TEXT, `" + TRACKS_COLUMN_PURCHASEURL + "` TEXT, `" + TRACKS_COLUMN_SCORE
            + "` REAL, FOREIGN KEY (`" + TRACKS_COLUMN_IDUSERPLAYLISTS + "`) REFERENCES `"
            + TABLE_USERPLAYLISTS + "` (`" + USERPLAYLISTS_COLUMN_ID + "`), FOREIGN KEY (`"
            + TRACKS_COLUMN_IDALBUMS + "`) REFERENCES `" + TABLE_ALBUMS + "` (`" + ALBUMS_COLUMN_ID
            + "`));";

    public TomahawkSQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(CREATE_TABLE_USERPLAYLISTS);
        database.execSQL(CREATE_TABLE_ALBUMS);
        database.execSQL(CREATE_TABLE_TRACKS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
                + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS `" + TABLE_TRACKS + "`;");
        db.execSQL("DROP TABLE IF EXISTS `" + TABLE_ALBUMS + "`;");
        db.execSQL("DROP TABLE IF EXISTS `" + TABLE_USERPLAYLISTS + "`;");
        onCreate(db);
    }

}
