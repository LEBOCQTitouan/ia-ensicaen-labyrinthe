package edu.ensicaen.util;

import java.util.logging.Logger;

/**
 * Utility class having various miscellaneous functions.
 */
public class Utility {
    public static boolean DEBUG = true;
    public static final Logger LOGGER =
            Logger.getLogger(Utility.class.getPackage().getName());

    public static double dec3(double a) {
        a = a * 100;
        a = Math.round(a);
        a = a / 100;
        return a;
    }

    public static void delay(int t) {
        try {
            Thread.sleep(t);
        } catch (Exception e) {

        }
    }
}
