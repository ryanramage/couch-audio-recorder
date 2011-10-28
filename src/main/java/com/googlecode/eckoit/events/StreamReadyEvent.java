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
public class StreamReadyEvent extends ConversionFinishedEvent {


    private String contentType;

    public StreamReadyEvent(File finishedFile, String contentType) {
        super(finishedFile);
        this.contentType = contentType;
    }
}
