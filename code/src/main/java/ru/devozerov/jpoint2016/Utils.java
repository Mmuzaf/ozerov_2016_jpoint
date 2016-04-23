/*
 *  ########  ######## ##     ##  ######   #######     ###     ######  ##     ##     ########  ##     ##
 *  ##     ## ##       ##     ## ##    ## ##     ##   ## ##   ##    ## ##     ##     ##     ## ##     ##
 *  ##     ## ##       ##     ## ##       ##     ##  ##   ##  ##       ##     ##     ##     ## ##     ##
 *  ##     ## ######   ##     ## ##       ##     ## ##     ## ##       #########     ########  ##     ##
 *  ##     ## ##        ##   ##  ##       ##     ## ######### ##       ##     ##     ##   ##   ##     ##
 *  ##     ## ##         ## ##   ##    ## ##     ## ##     ## ##    ## ##     ## ### ##    ##  ##     ##
 *  ########  ########    ###     ######   #######  ##     ##  ######  ##     ## ### ##     ##  #######
 */

package ru.devozerov.jpoint2016;

/**
 * Utility methods.
 */
public class Utils {
    /**
     * Print a message with current thread's name.
     *
     * @param msg Message.
     */
    public static void printWithThreadName(Object msg) {
        System.out.println(Thread.currentThread().getName() + ": " + msg);
    }

    /**
     * Private constructor.
     */
    private Utils() {
        // No-op.
    }
}
