/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.googlecode.eckoit.screengrab;




import com.github.couchapptakeout.events.ExitApplicationMessage;
import com.googlecode.eckoit.events.ScreenGrabFinishEvent;
import com.googlecode.eckoit.events.ScreenGrabStartEvent;
import com.googlecode.eckoit.events.TargetClickedEvent;
import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.EventSubscriber;

/**
 *
 * @author ryan
 */
public class ScreenGrabber {

    private File currentDir;
    private Timer timer;
    private boolean inPresentationMode = false;

    public ScreenGrabber() {

        EventBus.subscribeStrongly(TargetClickedEvent.class, new EventSubscriber<TargetClickedEvent>() {
            @Override
            public void onEvent(TargetClickedEvent t) {
                if (inPresentationMode) {
                    Robot robot;
                    try {
                        robot = new Robot();
                        Point last = MouseInfo.getPointerInfo().getLocation();
                        robot.mouseMove(10, 10);
                        robot.mousePress(InputEvent.BUTTON1_MASK);
                        robot.mouseRelease(InputEvent.BUTTON1_MASK);
                        robot.mouseMove(last.x, last.y);
                        System.out.println("Taking picture");
                        // wait to take the picture
                        Thread.sleep(300);
                        capture();

                    } catch (Exception ex) {
                        Logger.getLogger(ScreenGrabber.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }
            }

        });


        EventBus.subscribeStrongly(ScreenGrabStartEvent.class, new EventSubscriber<ScreenGrabStartEvent>() {
            @Override
            public void onEvent(ScreenGrabStartEvent t) {
                currentDir = t.getStorageDir();
                
                if (t.getPresentationMode()) {
                    inPresentationMode = true;
                    capture();
                } else {
                    if (timer != null) {
                        timer.cancel();
                    }
                    timer = new Timer();
                    
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            capture();
                        }
                    }, t.getIntervalInMilli(), t.getIntervalInMilli());
                }
            }
        });
        EventBus.subscribeStrongly(ScreenGrabFinishEvent.class, new EventSubscriber<ScreenGrabFinishEvent>() {
            @Override
            public void onEvent(ScreenGrabFinishEvent t) {
                if (timer != null) {
                    timer.cancel();
                }
                inPresentationMode = false;
            }
        });
        EventBus.subscribeStrongly(ExitApplicationMessage.class,  new EventSubscriber<ExitApplicationMessage>() {
            @Override
            public void onEvent(ExitApplicationMessage t) {
                if (timer != null) {
                    timer.cancel();
                }
            }
        });
    }


    protected void capture() {
        try {
            Date tstamp = new Date();
            File newImageFile = new File(currentDir, tstamp.getTime() + ".png");
            saveToFile(captureScreen(), "png", newImageFile);
        } catch (Exception ex) {
            Logger.getLogger(ScreenGrabber.class.getName()).log(Level.INFO, null, ex);
        }
    }


    public void saveToFile(BufferedImage image, String type, File file) throws IOException {
        // we may want to scale the image first.
        // see http://helpdesk.objects.com.au/java/how-do-i-scale-a-bufferedimage
        ImageIO.write(image, type, file);
    }

    public BufferedImage captureScreen() throws AWTException {
        return new Robot().createScreenCapture(
           new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()) );
    }

}
