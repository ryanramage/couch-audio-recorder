/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.googlecode.eckoit.audio;

import java.io.IOException;
import org.codehaus.jackson.map.ObjectMapper;
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
public class SplitAudioRecorderConfigurationTest {

    public SplitAudioRecorderConfigurationTest() {
    }



    /**
     * Test of getWavSampleRate method, of class SplitAudioRecorderConfiguration.
     */
    @Test
    public void testJsonSerialization() throws IOException {
        ObjectMapper mapper = new ObjectMapper();


        SplitAudioRecorderConfiguration config = new SplitAudioRecorderConfiguration();
        String result = mapper.writeValueAsString(config);

        System.out.println(result);
        assertTrue(result.contains("wavSampleRate"));



    }


    @Test
    public void testJsonReading() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String value = "{\"wavSampleRate\":16000.0,\"wavSampleSize\":16,\"mp3Bitrate\":28000,\"mp3Frequency\":16000,\"oggBitrate\":24000,\"oggFrequency\":22050,\"stream\":false}";
        SplitAudioRecorderConfiguration readValue = mapper.readValue(value, SplitAudioRecorderConfiguration.class);

        assertEquals(28000, readValue.getMp3Bitrate());



    }


}