/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.googlecode.eckoit.audio;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 *
 * @author ryan
 */
public class FFMpegConverter implements AudioEncoder {

    public static final String ENCODER_MP3 = "mp3";
    public static final String ENCODER_VORBIS = "libvorbis";

    String ffmpegFullCommand;
    String encoder;
    //ffmpeg.exe -i out.wav -f mp3  -ab 48000 -ar 44100 test3.mp3

    public FFMpegConverter( String ffmpegcmd, String encoder) {
        this.ffmpegFullCommand =  ffmpegcmd;
        this.encoder = encoder;
    }

    public void convert(File wav, long bitrate, long frequency, File outputfile, boolean forceOverwrite) throws InterruptedException, IOException {
        if (outputfile.exists() && !forceOverwrite) {
            throw new IllegalArgumentException("The output file exists, and force overwriting is not true");
        }
        ProcessBuilder pb = null;
        if (ENCODER_MP3.equals(encoder)) {
            pb = new ProcessBuilder(new String[] {ffmpegFullCommand, "-i", wav.getAbsolutePath(), "-y", "-f", encoder, "-ab", bitrate + "", "-ar", frequency + "", outputfile.getAbsolutePath()} );
        } else {

            pb = new ProcessBuilder(new String[] {ffmpegFullCommand, "-i", wav.getAbsolutePath(), "-y", "-acodec", "libvorbis", "-ab", bitrate + "", outputfile.getAbsolutePath()} );
            
        }
        pb.redirectErrorStream(true);
        Process p = pb.start();
        InputStream stream = p.getInputStream();
        //BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        //String line = br.readLine();
        int chr = stream.read();
        //while(br != null) {
        while (chr != -1) {
            chr = stream.read();
        }
    }


    public void makeTS(File mp3, File outputfile) throws InterruptedException, IOException {
        ProcessBuilder pb = new ProcessBuilder(new String[] {ffmpegFullCommand, "-i", mp3.getAbsolutePath(), "-y", "-acodec", "copy", "-f", "mpegts", outputfile.getAbsolutePath()} );
        pb.redirectErrorStream(true);
        Process p = pb.start();
        InputStream stream = p.getInputStream();
        int chr = stream.read();
        while (chr != -1) {
            chr = stream.read();
        }
    }

    public void fixMP3(File mp3, File outputfile) throws InterruptedException, IOException {
        ProcessBuilder pb = new ProcessBuilder(new String[] {ffmpegFullCommand, "-i", mp3.getAbsolutePath(), "-y", "-acodec", "copy", outputfile.getAbsolutePath()} );
        pb.redirectErrorStream(true);
        Process p = pb.start();
        InputStream stream = p.getInputStream();
        int chr = stream.read();
        while (chr != -1) {
            chr = stream.read();
        }
    }

    public void convertURL(String url, long bitrate, long frequency, File outputfile, boolean forceOverwrite) throws InterruptedException, IOException {
        if (outputfile.exists() && !forceOverwrite) {
            throw new IllegalArgumentException("The output file exists, and force overwriting is not true");
        }
        ProcessBuilder pb = null;
        if (ENCODER_MP3.equals(encoder)) {
            pb = new ProcessBuilder(new String[] {ffmpegFullCommand, "-i", url, "-y", "-f", encoder, "-ab", bitrate + "", "-ar", frequency + "", outputfile.getAbsolutePath()} );
        } else {

            pb = new ProcessBuilder(new String[] {ffmpegFullCommand, "-i", url, "-y", "-acodec", "libvorbis", "-ab", bitrate + "", outputfile.getAbsolutePath()} );

        }
        pb.redirectErrorStream(true);
        Process p = pb.start();
        InputStream stream = p.getInputStream();
        //BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        //String line = br.readLine();
        int chr = stream.read();
        //while(br != null) {
        while (chr != -1) {
            chr = stream.read();
        }
    }




    public static final void main(String[] args) {
        File wav = new File("C:\\rtemp\\out.wav");
        long bitrate = 24000L;
        long frequency = 22050L;
        File outputfile = new File("C:\\Program Files\\FFmpeg for Audacity\\green.mp3");
        boolean forceOverwrite = true;
        FFMpegConverter instance = new FFMpegConverter("C:\\Program Files\\FFmpeg for Audacity\\ffmpeg.exe", ENCODER_MP3);
        try {
            instance.convert(wav, bitrate, frequency, outputfile, forceOverwrite);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }


}
