/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael K�lling 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package greenfoot.sound;

import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Plays sound from a URL. The sound is loaded into memory the first time it is
 * played.
 * 
 * @author Poul Henriksen
 * 
 */
public class SoundClip extends Sound
{
    /** Name of the file holding the sound data. Used for debugging. */
    private final String name;
    /** URL of the sound data. */
    private final URL url;

    /**
     * The clip that this SoundClip represents. Can be null (when state is
     * CLOSED)
     */
    private Clip soundClip;

    /** The four states a clip can be in. */
    private enum ClipState {
        STOPPED, PLAYING, PAUSED, CLOSED
    };

    private ClipState clipState = ClipState.CLOSED;

    // The following fields are used to determine when to close the clip.
    /** The time at which sound playback was started. In ms. */
    private long playBeginTimestamp;
    /** The time at which the last pause happened. In ms. */
    private long pauseBeginTimestamp;
    /** The total time we have been paused sine we started playback. In ms. */
    private long pausedTime;
    /** Length of this clip in ms. */
    private long clipLength;
    /**
     * Thread that closes this sound clip after a timeout.
     */
    private Thread closeThread;
    /**
     * How long to wait until closing the line after playback has finished. In
     * ms.
     */
    private static final int CLOSE_TIMEOUT = 2000;

    /**
     * Extra delay in ms added to the sleep time before closing the clip. This
     * is jsut an extra buffer of time to make sure we don't close it too soon.
     * Only really needed if CLOSE_TIMEOUT is very low.
     */
    private final static int EXTEA_SLEEP_DELAY = 300;

    /** Listener for state changes. */
    private SoundPlaybackListener playbackListener;

    /**
     * Creates a new sound clip
     */
    public SoundClip(String name, URL url, SoundPlaybackListener listener)
    {
        this.name = name;
        this.url = url;
        playbackListener = listener;
    }

    /**
     * Load the sound file supplied by the parameter into this sound engine.
     * 
     * @throws LineUnavailableException if a matching line is not available due
     *             to resource restrictions
     * @throws IOException if an I/O exception occurs
     * @throws SecurityException if a matching line is not available due to
     *             security restrictions
     * @throws UnsupportedAudioFileException if the URL does not point to valid
     *             audio file data
     * @throws IllegalArgumentException if the system does not support at least
     *             one line matching the specified Line.Info object through any
     *             installed mixer
     */
    private void open()
        throws LineUnavailableException, IOException, UnsupportedAudioFileException, IllegalArgumentException,
        SecurityException
    {
        AudioInputStream stream = AudioSystem.getAudioInputStream(url);
        AudioFormat format = stream.getFormat();
        DataLine.Info info = new DataLine.Info(Clip.class, format);

        // Convert sound formats that are not supported
        // TODO: Check that this works
        if (!AudioSystem.isLineSupported(info)) {
            format = getCompatibleFormat(format);
            stream = AudioSystem.getAudioInputStream(format, stream);
            info = new DataLine.Info(Clip.class, stream.getFormat(), ((int) stream.getFrameLength() * format
                    .getFrameSize()));
        }
        soundClip = (Clip) AudioSystem.getLine(info);
        soundClip.open(stream);
        clipLength = soundClip.getMicrosecondLength() / 1000;
        setState(ClipState.CLOSED);
    }

    /**
     * Play this sound from the beginning of the sound.
     * 
     * @throws LineUnavailableException if a matching line is not available due
     *             to resource restrictions
     * @throws IOException if an I/O exception occurs
     * @throws SecurityException if a matching line is not available due to
     *             security restrictions
     * @throws UnsupportedAudioFileException if the URL does not point to valid
     *             audio file data
     * @throws IllegalArgumentException if the system does not support at least
     *             one line matching the specified Line.Info object through any
     *             installed mixer
     */
    public synchronized void play()
        throws LineUnavailableException, IOException, UnsupportedAudioFileException, IllegalArgumentException,
        SecurityException
    {
        playBeginTimestamp = System.currentTimeMillis();

        if (soundClip == null) {
            open();
        }
        setState(ClipState.PLAYING);
        soundClip.stop();
        soundClip.setMicrosecondPosition(0);
        soundClip.start();
        pausedTime = 0;
        System.out.println("play: " + this);
        startCloseThread();
    }

    /**
     * Stop this sound.
     * 
     */
    public synchronized void stop()
    {
        if (soundClip == null || isStopped()) {
            return;
        }
        setState(ClipState.STOPPED);
        soundClip.stop();
        soundClip.setMicrosecondPosition(0);
        System.out.println("Stop: " + this);
    }

    /**
     * Pause the clip. Paused sounds can be resumed.
     * 
     */
    public synchronized void pause()
    {
        pauseBeginTimestamp = System.currentTimeMillis();
        if (soundClip == null || isPaused()) {
            return;
        }
        setState(ClipState.PAUSED);
        soundClip.stop();
        System.out.println("Pause: " + this);
    }

    /**
     * Resume a paused clip. If the clip is not currently paused, this call will
     * do nothing
     * 
     */
    public synchronized void resume()
    {
        if (soundClip == null || !isPaused()) {
            return;
        }
        pausedTime += System.currentTimeMillis() - pauseBeginTimestamp;
        System.out.println("Pausedtime: " + pausedTime);
        soundClip.start();
        setState(ClipState.PLAYING);
        System.out.println("Resume: " + this);
    }

    private void setState(ClipState newState)
    {
        if (clipState != newState) {
            System.out.println("Setting state to: " + newState);
            clipState = newState;
            switch(clipState) {
                case PLAYING :
                    playbackListener.playbackStarted(this);
                    break;
                case STOPPED :
                    playbackListener.playbackStopped(this);
                    break;
                case PAUSED :
                    playbackListener.playbackPaused(this);
                    break;
            }
        }

        // The close thread might be waiting, so we wake it up.
        this.notifyAll();
    }

    /**
     * Get a name for this sound. The name should uniquely identify the sound
     * clip.
     */
    public String getName()
    {
        return name;
    }

    /**
     * True if the sound is currently playing.
     */
    public synchronized boolean isPlaying()
    {
        return clipState == ClipState.PLAYING;
    }

    /**
     * True if the sound is currently playing.
     */
    public synchronized boolean isPaused()
    {
        return clipState == ClipState.PAUSED;
    }

    /**
     * True if the sound is currently playing.
     */
    public synchronized boolean isStopped()
    {
        return clipState == ClipState.STOPPED;
    }

    /**
     * Close the clip when it should have finished playing. This will be done
     * asynchronously.
     * 
     * The reason we are using this is instead of listening for LineEvent.STOP
     * is that on some linux systems the LineEvent for stop is send before
     * playback has actually stopped.
     * 
     * @param sleepTime Minimum time to wait before closing the stream.
     */
    private synchronized void startCloseThread()
    {
        if (closeThread == null) {
            closeThread = new Thread("SoundClipCloseThread") {
                public void run()
                {
                    SoundClip thisClip = SoundClip.this;
                    while (thisClip.soundClip != null) {
                        synchronized (thisClip) {
                            long playTime = (System.currentTimeMillis() - playBeginTimestamp) - pausedTime;
                            long timeLeftOfPlayback = clipLength - playTime + EXTEA_SLEEP_DELAY;
                            long timeLeftToClose = timeLeftOfPlayback + CLOSE_TIMEOUT;
                            if (isPaused()) {
                                try {
                                    thisClip.wait();
                                }
                                catch (InterruptedException e) {
                                    // TODO Handle this!
                                    e.printStackTrace();
                                }
                            }
                            else if (timeLeftToClose > 0) {
                                System.out.println("Waiting to close");
                                try {
                                    thisClip.wait(timeLeftToClose);
                                }
                                catch (InterruptedException e) {
                                    // TODO Handle this!
                                    e.printStackTrace();
                                }
                            }
                            else {
                                System.out.println("Closing clip: " + thisClip.name);
                                thisClip.soundClip.close();
                                thisClip.soundClip = null;
                                thisClip.closeThread = null;
                                setState(ClipState.CLOSED);
                            }
                        }
                    }
                }
            };
            closeThread.start();
        }
    }

    public String toString()
    {
        return url + " " + super.toString();
    }
}