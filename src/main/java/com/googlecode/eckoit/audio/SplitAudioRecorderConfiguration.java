/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.googlecode.eckoit.audio;

/**
 *
 * @author ryan
 */
public class SplitAudioRecorderConfiguration {


    private boolean stream = false;

    private float wavSampleRate = 16000.0F;
    private int wavSampleSize = 16;

    private long mp3Bitrate = 24000L;
    private long mp3Frequency = 16000L;

    private long oggBitrate = 24000L;
    private long oggFrequency = 22050L;

    /**
     * @return the wavSampleRate
     */
    public float getWavSampleRate() {
        return wavSampleRate;
    }

    /**
     * @param wavSampleRate the wavSampleRate to set
     */
    public void setWavSampleRate(float wavSampleRate) {
        this.wavSampleRate = wavSampleRate;
    }

    /**
     * @return the wavSampleSize
     */
    public int getWavSampleSize() {
        return wavSampleSize;
    }

    /**
     * @param wavSampleSize the wavSampleSize to set
     */
    public void setWavSampleSize(int wavSampleSize) {
        this.wavSampleSize = wavSampleSize;
    }

    /**
     * @return the mp3Bitrate
     */
    public long getMp3Bitrate() {
        return mp3Bitrate;
    }

    /**
     * @param mp3Bitrate the mp3Bitrate to set
     */
    public void setMp3Bitrate(long mp3Bitrate) {
        this.mp3Bitrate = mp3Bitrate;
    }

    /**
     * @return the mp3Frequency
     */
    public long getMp3Frequency() {
        return mp3Frequency;
    }

    /**
     * @param mp3Frequency the mp3Frequency to set
     */
    public void setMp3Frequency(long mp3Frequency) {
        this.mp3Frequency = mp3Frequency;
    }

    /**
     * @return the oggBitrate
     */
    public long getOggBitrate() {
        return oggBitrate;
    }

    /**
     * @param oggBitrate the oggBitrate to set
     */
    public void setOggBitrate(long oggBitrate) {
        this.oggBitrate = oggBitrate;
    }

    /**
     * @return the offFrequency
     */
    public long getOggFrequency() {
        return oggFrequency;
    }

    /**
     * @param offFrequency the offFrequency to set
     */
    public void setOggFrequency(long oggFrequency) {
        this.oggFrequency = oggFrequency;
    }

    /**
     * @return the stream
     */
    public boolean isStream() {
        return stream;
    }

    /**
     * @param stream the stream to set
     */
    public void setStream(boolean stream) {
        this.stream = stream;
    }

    

}
