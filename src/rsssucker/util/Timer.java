/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package rsssucker.util;

import java.util.Date;

/** Class for timing execution time. */
public class Timer {

    private Date start;
    
    public Timer() {
        start = new Date();
    }
    
    /** reset timer */
    public void restart() {
        start = new Date();
    }
    
    /** get number of miliseconds from start */
    public long milisFromStart() {
        Date now = new Date();
        return now.getTime() - start.getTime();
    }
    
    
    private static double milisPerSecond = 1000;
    private static double milisPerMinute = milisPerSecond * 60;
    /** print time elapsed from start in readable manner */
    public String fromStart() {
        long milis = milisFromStart();
        if (milis < milisPerSecond) { return milis+" miliseconds"; }
        else if (milis < milisPerMinute) { return milis/milisPerSecond + " seconds"; }
        else return milis/milisPerMinute + " minutes";
    }    
    
}
