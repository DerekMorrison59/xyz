package com.example.xyzreader.ui;

import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

/**
 * Created by Derek on 5/29/2016.
 */
public class util {

    public static boolean isLandscape(AppCompatActivity aca) {
        boolean isLand = false;
        Display display =
            ((WindowManager) aca.getSystemService(aca.WINDOW_SERVICE)).getDefaultDisplay();

        int orientation = display.getRotation();

        if (orientation == Surface.ROTATION_90
                || orientation == Surface.ROTATION_270) {
            isLand = true;
        }
        return isLand;
    }
}
