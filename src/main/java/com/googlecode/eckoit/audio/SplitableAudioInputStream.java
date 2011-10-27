/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.googlecode.eckoit.audio;

import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import org.apache.commons.io.input.NullInputStream;

/**
 *
 * @author ryan
 */
public class SplitableAudioInputStream extends AudioInputStream {

    AudioInputStream wrapped;
    public boolean stopped;

    public SplitableAudioInputStream(AudioInputStream wrapped) {
        // this is really just to foil the parent. The parent should never be used.
        super(new NullInputStream(0), new AudioFormat(16000.0F, 16, 1, true, true), 0);
        this.wrapped = wrapped;
        this.stopped = false;
    }


    @Override
    public SplitableAudioInputStream clone() {
        //this is where we stop the one stream and create another
        SplitableAudioInputStream clone = new SplitableAudioInputStream(wrapped);
        this.stopped = true;
        return clone;
    }



    /***********************************************************************/

    /** Defer all the overridden methods to the wrapped object */


    /**
     * Obtains the audio format of the sound data in this audio input stream.
     * @return an audio format object describing this stream's format
     */
    @Override
    public AudioFormat getFormat() {
	return wrapped.getFormat();
    }


    /**
     * Obtains the length of the stream, expressed in sample frames rather than bytes.
     * @return the length in sample frames
     */
    @Override
    public long getFrameLength() {
	return wrapped.getFrameLength();
    }


    /**
     * Reads the next byte of data from the audio input stream.  The audio input
     * stream's frame size must be one byte, or an <code>IOException</code>
     * will be thrown.
     *
     * @return the next byte of data, or -1 if the end of the stream is reached
     * @throws IOException if an input or output error occurs
     * @see #read(byte[], int, int)
     * @see #read(byte[])
     * @see #available
     * <p>
     */
    @Override
    public int read() throws IOException {
        if (!stopped) return wrapped.read();
        else return -1;
    }


    /**
     * Reads some number of bytes from the audio input stream and stores them into
     * the buffer array <code>b</code>. The number of bytes actually read is
     * returned as an integer. This method blocks until input data is
     * available, the end of the stream is detected, or an exception is thrown.
     * <p>This method will always read an integral number of frames.
     * If the length of the array is not an integral number
     * of frames, a maximum of <code>b.length - (b.length % frameSize)
     * </code> bytes will be read.
     *
     * @param b the buffer into which the data is read
     * @return the total number of bytes read into the buffer, or -1 if there
     * is no more data because the end of the stream has been reached
     * @throws IOException if an input or output error occurs
     * @see #read(byte[], int, int)
     * @see #read()
     * @see #available
     */
    @Override
    public int read(byte[] b) throws IOException {
        if (!stopped) return wrapped.read(b);
        else return -1;
    }


    /**
     * Reads up to a specified maximum number of bytes of data from the audio
     * stream, putting them into the given byte array.
     * <p>This method will always read an integral number of frames.
     * If <code>len</code> does not specify an integral number
     * of frames, a maximum of <code>len - (len % frameSize)
     * </code> bytes will be read.
     *
     * @param b the buffer into which the data is read
     * @param off the offset, from the beginning of array <code>b</code>, at which
     * the data will be written
     * @param len the maximum number of bytes to read
     * @return the total number of bytes read into the buffer, or -1 if there
     * is no more data because the end of the stream has been reached
     * @throws IOException if an input or output error occurs
     * @see #read(byte[])
     * @see #read()
     * @see #skip
     * @see #available
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (!stopped) return wrapped.read(b, off, len);
        else return -1;
    }


    /**
     * Skips over and discards a specified number of bytes from this
     * audio input stream.
     * @param n the requested number of bytes to be skipped
     * @return the actual number of bytes skipped
     * @throws IOException if an input or output error occurs
     * @see #read
     * @see #available
     */
    @Override
    public long skip(long n) throws IOException {
        if (!stopped) return wrapped.skip(n);
        else return 0;
    }


    /**
     * Returns the maximum number of bytes that can be read (or skipped over) from this
     * audio input stream without blocking.  This limit applies only to the next invocation of
     * a <code>read</code> or <code>skip</code> method for this audio input stream; the limit
     * can vary each time these methods are invoked.
     * Depending on the underlying stream,an IOException may be thrown if this
     * stream is closed.
     * @return the number of bytes that can be read from this audio input stream without blocking
     * @throws IOException if an input or output error occurs
     * @see #read(byte[], int, int)
     * @see #read(byte[])
     * @see #read()
     * @see #skip
     */
    @Override
    public int available() throws IOException {
        if (!stopped) return wrapped.available();
        else return 0;
    }


    /**
     * Closes this audio input stream and releases any system resources associated
     * with the stream.
     * @throws IOException if an input or output error occurs
     */
    @Override
    public void close() throws IOException {
        if (!stopped) wrapped.close();

    }


    /**
     * Marks the current position in this audio input stream.
     * @param readlimit the maximum number of bytes that can be read before
     * the mark position becomes invalid.
     * @see #reset
     * @see #markSupported
     */

    @Override
    public void mark(int readlimit) {
        if (!stopped)
            wrapped.mark(readlimit);
    }


    /**
     * Repositions this audio input stream to the position it had at the time its
     * <code>mark</code> method was last invoked.
     * @throws IOException if an input or output error occurs.
     * @see #mark
     * @see #markSupported
     */
    @Override
    public void reset() throws IOException {
        if (!stopped)
         wrapped.reset();
    }


    /**
     * Tests whether this audio input stream supports the <code>mark</code> and
     * <code>reset</code> methods.
     * @return <code>true</code> if this stream supports the <code>mark</code>
     * and <code>reset</code> methods; <code>false</code> otherwise
     * @see #mark
     * @see #reset
     */
    @Override
    public boolean markSupported() {

	return wrapped.markSupported();
    }



}
