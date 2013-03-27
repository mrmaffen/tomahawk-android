/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
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
package org.tomahawk.libtomahawk.playlist;

import org.tomahawk.libtomahawk.Track;

import java.util.Collection;

/**
 * A simple interface to represent a collection of 0 or more Tracks.
 */
public interface Playable {

    /**
     * Set the tracks for this PlayableInterface.
     */
    public void setTracks(Collection<Track> tracks);

    /**
     * Set the current Track to track. If Track cannot be found the current Track stays the same.
     */
    public void setCurrentTrack(Track track);

    /**
     * Return the current Track for this PlayableInterface.
     */
    public Track getCurrentTrack();

    /**
     * Return the next Track for this PlayableInterface.
     */
    public Track getNextTrack();

    /**
     * Return the previous Track for this PlayableInterface.
     */
    public Track getPreviousTrack();

    /**
     * Return the Track at pos i.
     */
    public Track getTrackAtPos(int i);

    /**
     * Return the first Track in this PlayableInterface.
     */
    public Track getFirstTrack();

    /**
     * Return the last Track in this PlayableInterface.
     */
    public Track getLastTrack();

    /**
     * Returns true if the PlayableInterface has a next Track.
     */
    public boolean hasNextTrack();

    /**
     * Returns true if the PlayableInterface has a previous Track.
     */
    public boolean hasPreviousTrack();
}
