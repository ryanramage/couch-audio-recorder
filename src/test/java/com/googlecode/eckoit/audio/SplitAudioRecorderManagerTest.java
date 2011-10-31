/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.googlecode.eckoit.audio;


import com.github.couchapptakeout.events.ExitApplicationMessage;
import java.io.File;
import org.bushe.swing.event.EventBus;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ryan
 */
public class SplitAudioRecorderManagerTest {

    public SplitAudioRecorderManagerTest() {
    }



    /**
     * Test of startRecording method, of class SplitAudioRecorderManager.
     */
    @Test
    public void testStartRecording() throws Exception {
        System.out.println("startRecording");

        File file = new File("target");
        
        String mixer = "default";
        float gain = 0.0F;

        String ffmpeg = "/Applications/eckoit/lib/ffmpeg";


        EventBus.publish(new ExitApplicationMessage());

    }



}