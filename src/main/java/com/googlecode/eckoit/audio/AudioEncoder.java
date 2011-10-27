/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.googlecode.eckoit.audio;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author ryan
 */
public interface AudioEncoder {

    void convert(File wav, long bitrate, long frequency, File outputfile, boolean forceOverwrite) throws InterruptedException, IOException;

}
