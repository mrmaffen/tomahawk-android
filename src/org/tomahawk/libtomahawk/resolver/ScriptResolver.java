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
package org.tomahawk.libtomahawk.resolver;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tomahawk.libtomahawk.Album;
import org.tomahawk.libtomahawk.Artist;
import org.tomahawk.libtomahawk.Track;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.webkit.WebSettings;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com> Date: 17.01.13
 *
 * This class represents a javascript resolver.
 */
public class ScriptResolver implements Resolver {

    private final static String TAG = ScriptResolver.class.getName();

    private final static String RESOLVER_LEGACY_CODE
            = "var resolver = Tomahawk.resolver.instance ? Tomahawk.resolver.instance : TomahawkResolver;";

    private final static String RESOLVER_LEGACY_CODE2
            = "var resolver = Tomahawk.resolver.instance ? Tomahawk.resolver.instance : window;";

    private final static String SCRIPT_INTERFACE_NAME = "Tomahawk";

    //TEMPORARY WORKAROUND
    private final static String BASEURL_OFFICIALFM = "http://api.official.fm";

    private final static String BASEURL_EXFM = "http://ex.fm";

    private final static String BASEURL_JAMENDO = "http://api.jamendo.com";

    private final static String BASEURL_SOUNDCLOUD = "http://developer.echonest.com";

    private TomahawkApp mTomahawkApp;

    private int mId;

    private ScriptEngine mScriptEngine;

    private String mScriptFilePath;

    private String mName;

    private Drawable mIcon;

    private int mWeight;

    private int mTimeout;

    private JSONObject mConfig;

    private boolean mReady;

    private boolean mStopped;

    public ScriptResolver(int id, TomahawkApp tomahawkApp, String scriptPath) {
        mReady = false;
        mStopped = true;
        mId = id;
        mTomahawkApp = tomahawkApp;
        mScriptEngine = new ScriptEngine(mTomahawkApp);
        WebSettings settings = mScriptEngine.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setDomStorageEnabled(true);
        mScriptEngine.setWebChromeClient(new TomahawkWebChromeClient());
        mScriptEngine.setWebViewClient(new TomahawkWebViewClient(this));
        final ScriptInterface scriptInterface = new ScriptInterface(this);
        mScriptEngine.addJavascriptInterface(scriptInterface, SCRIPT_INTERFACE_NAME);
        String[] tokens = scriptPath.split("/");
        mName = tokens[tokens.length - 1];
        mIcon = mTomahawkApp.getResources().getDrawable(R.drawable.ic_resolver_default);
        mScriptFilePath = scriptPath;

        init();
    }

    /**
     * @return whether or not this scriptresolver is currently resolving
     */
    public boolean isResolving() {
        return mReady && !mStopped;
    }

    /**
     * Reinitialize this ScriptResolver
     */
    public void reload() {
        init();
    }

    /**
     * Initialize this ScriptResolver. Loads the .js script from the given path and sets the
     * appropriate base URL.
     */
    private void init() {
        String baseurl = "http://fake.bla.blu";
        if (getScriptFilePath().contains("officialfm.js")) {
            baseurl = BASEURL_OFFICIALFM;
        } else if (getScriptFilePath().contains("exfm.js")) {
            baseurl = BASEURL_EXFM;
        } else if (getScriptFilePath().contains("jamendo-resolver.js")) {
            baseurl = BASEURL_JAMENDO;
        } else if (getScriptFilePath().contains("soundcloud.js")) {
            baseurl = BASEURL_SOUNDCLOUD;
        }

        mScriptEngine.loadDataWithBaseURL(baseurl, "<!DOCTYPE html>" + "<html>" + "<body>"
                + "<script src=\"file:///android_asset/js/tomahawk_android.js\" type=\"text/javascript\"></script>"
                + "<script src=\"file:///android_asset/js/tomahawk.js        \" type=\"text/javascript\"></script>"
                + "<script src=\"file:///android_asset/" + mScriptFilePath
                + "\" type=\"text/javascript\"></script>" + "</body>" + "</html>", "text/html",
                null, null);
    }

    /**
     * This method is being called, when the ScriptEngine has completely loaded the given .js
     * script.
     */
    public void onScriptEngineReady() {
        resolverInit();
        resolverUserConfig();
        mReady = true;
    }

    /**
     * This method calls the js function resolver.init().
     */
    private void resolverInit() {
        mScriptEngine.loadUrl("javascript:" + RESOLVER_LEGACY_CODE + makeJSFunctionCallbackJava(
                R.id.scriptresolver_resolver_init, "resolver.init()", false));
    }

    /**
     * This method tries to get the resolver's settings.
     */
    private void resolverSettings() {
        mScriptEngine.loadUrl("javascript:" + RESOLVER_LEGACY_CODE + makeJSFunctionCallbackJava(
                R.id.scriptresolver_resolver_settings,
                "resolver.settings ? resolver.settings : getSettings() ", true));
    }

    /**
     * This method tries to get the resolver's UserConfig.
     */
    private void resolverUserConfig() {
        mScriptEngine.loadUrl("javascript:" + RESOLVER_LEGACY_CODE + makeJSFunctionCallbackJava(
                R.id.scriptresolver_resolver_userconfig, "resolver.getUserConfig()", true));
    }

    /**
     * Every callback from a function inside the javascript should first call the method
     * callbackToJava, which is exposed to javascript within the ScriptInterface. And after that
     * this callback will be handled here.
     *
     * @param id  used to identify which function did the callback
     * @param obj the JSONObject containing the result information. Can be null
     */
    public void handleCallbackToJava(int id, JSONObject obj) {
        String s = "null";
        if (obj != null) {
            s = obj.toString();
        }
        Log.d(TAG,
                "handleCallbackToJava: id='" + mTomahawkApp.getResources().getResourceEntryName(id)
                        + "(" + id + ")" + "', result='" + s + "'");
        try {
            if (id == R.id.scriptresolver_resolver_settings) {
                mName = obj.getString("name");
                mWeight = obj.getInt("weight");
                mTimeout = obj.getInt("timeout") * 1000;
                String[] tokens = getScriptFilePath().split("/");
                String basepath = "";
                for (int i = 0; i < tokens.length - 1; i++) {
                    basepath += tokens[i];
                    basepath += "/";
                }
                mIcon = Drawable.createFromStream(
                        mTomahawkApp.getAssets().open(basepath + obj.getString("icon")), null);
            } else if (id == R.id.scriptresolver_resolver_userconfig) {
            } else if (id == R.id.scriptresolver_resolver_init) {
                resolverSettings();
            } else if (id == R.id.scriptresolver_add_track_results_string) {
                String qid = obj.get("qid").toString();
                JSONArray resultList = obj.getJSONArray("results");
                mTomahawkApp.getPipeLine().reportResults(qid, parseResultList(resultList));
                mStopped = true;
            }
        } catch (JSONException e) {
            Log.e(TAG, "handleCallbackToJava: " + e.getClass() + ": " + e.getLocalizedMessage());
        } catch (IOException e) {
            Log.e(TAG, "handleCallbackToJava: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    /**
     * Invoke the javascript to resolve the given Query.
     *
     * @param query the query which should be resolved
     */
    public void resolve(Query query) {
        mStopped = false;
        if (!query.isFullTextQuery()) {
            mScriptEngine.loadUrl(
                    "javascript:" + RESOLVER_LEGACY_CODE2 + makeJSFunctionCallbackJava(
                            R.id.scriptresolver_resolve,
                            "resolver.resolve( '" + query.getQid() + "', '" + query.getArtistName()
                                    + "', '" + query.getAlbumName() + "', '" + query.getTrackName()
                                    + "' )", false));
        } else {
            mScriptEngine.loadUrl("javascript:" + RESOLVER_LEGACY_CODE + makeJSFunctionCallbackJava(
                    R.id.scriptresolver_resolve,
                    "(Tomahawk.resolver.instance !== undefined) ?resolver.search( '" + query
                            .getQid() + "', '" + query.getFullTextQuery() + "' ):resolve( '" + query
                            .getQid() + "', '', '', '" + query.getFullTextQuery() + "' )", false));
        }
    }

    /**
     * Parses the given JSONArray into a ArrayList<Result>.
     *
     * @param resList JSONArray containing the raw result information
     * @return a ArrayList<Result> containing the parsed data
     */
    private ArrayList<Result> parseResultList(final JSONArray resList) {
        ArrayList<Result> resultList = new ArrayList<Result>();
        for (int i = 0; i < resList.length(); i++) {
            try {
                JSONObject obj = resList.getJSONObject(i);
                Result result = new Result();
                Artist artist = new Artist(mTomahawkApp.getUniqueArtistId());
                Album album = new Album(mTomahawkApp.getUniqueAlbumId());
                Track track = new Track(mTomahawkApp.getUniqueTrackId());
                if (obj.has("url")) {
                    track.setPath(obj.get("url").toString());
                    track.setLocal(false);
                    if (obj.has("artist")) {
                        artist.setName(obj.get("artist").toString());
                        track.setArtist(artist);
                    }
                    if (obj.has("album")) {
                        album.setName(obj.get("album").toString());
                        track.setAlbum(album);
                    }
                    if (obj.has("track")) {
                        track.setName(obj.get("track").toString());
                    }
                    if (obj.has("albumpos")) {
                        track.setTrackNumber(Integer.valueOf(obj.get("albumpos").toString()));
                    }
                    if (obj.has("bitrate")) {
                        track.setBitrate(Integer.valueOf(obj.get("bitrate").toString()));
                    }
                    if (obj.has("size")) {
                        track.setSize(Integer.valueOf(obj.get("size").toString()));
                    }
                    if (obj.has("purchaseUrl")) {
                        track.setPurchaseUrl(obj.get("purchaseUrl").toString());
                    }
                    if (obj.has("linkUrl")) {
                        track.setLinkUrl(obj.get("linkUrl").toString());
                    }
                    if (obj.has("score")) {
                        result.setTrackScore(Float.valueOf(obj.get("score").toString()));
                    }
                    if (obj.has("discnumber")) {
                        track.setTrackNumber(Integer.valueOf(obj.get("discnumber").toString()));
                    }
                    //                    if (obj.has("year") && obj.get("year") != null)
                    //                        track.setYear(Integer.valueOf(obj.get("year").toString()));
                    if (obj.has("duration")) {
                        track.setDuration(
                                Math.round(Float.valueOf(obj.get("duration").toString()) * 1000));
                    }
                    track.setResolver(this);
                    result.setResolver(this);
                    result.setArtist(artist);
                    result.setAlbum(album);
                    result.setTrack(track);
                    resultList.add(result);
                }
            } catch (JSONException e) {
                Log.e(TAG, "parseResultList: " + e.getClass() + ": " + e.getLocalizedMessage());
            }
        }
        return resultList;
    }

    /**
     * Wraps the given js call into the necessary functions to make sure, that the javascript
     * function will callback the exposed java method callbackToJava in the ScriptInterface
     *
     * @param id                 used to later identify the callback
     * @param string             the string which should be surrounded. Usually a simple js function
     *                           call.
     * @param shouldReturnResult whether or not this js function call will return with a JSONObject
     *                           as a result
     * @return the computed String
     */
    private String makeJSFunctionCallbackJava(int id, String string, boolean shouldReturnResult) {
        return SCRIPT_INTERFACE_NAME + ".callbackToJava(" + id + ",JSON.stringify(" + string + "),"
                + shouldReturnResult + ");";
    }

    public int getId() {
        return mId;
    }

    /**
     * @return the absolute filepath (without file://android_asset) of the corresponding script
     */
    public String getScriptFilePath() {
        return mScriptFilePath;
    }

    /**
     * @return the JSONObject containing the Config information, which was returned by the
     *         corresponding script
     */
    public JSONObject getConfig() {
        return mConfig;
    }

    /**
     * @return the Drawable which has been created by loading the image the js function attribute
     *         "icon" pointed at
     */
    public Drawable getIcon() {
        return mIcon;
    }

    public int getWeight() {
        return mWeight;
    }

}
