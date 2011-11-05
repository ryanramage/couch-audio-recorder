/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.googlecode.eckoit.util;

/**
 *
 * @author ryan
 */
public class Slugger {

    public static String generateSlug(String text) {
       return text.replaceAll("[^a-zA-Z0-9]", "_");
    }

}
