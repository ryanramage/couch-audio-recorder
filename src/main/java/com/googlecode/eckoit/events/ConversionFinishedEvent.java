/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.googlecode.eckoit.events;

import java.io.File;

/**
 *
 * @author ryan
 */
public class ConversionFinishedEvent {
    private File finishedFile;


    public ConversionFinishedEvent(File finishedFile) {
        this.finishedFile = finishedFile;
    }

    public File getFinishedFile() {
        return finishedFile;
    }


}
