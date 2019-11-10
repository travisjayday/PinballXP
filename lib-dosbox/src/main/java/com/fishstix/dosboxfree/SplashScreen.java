package com.fishstix.dosboxfree;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.appodeal.ads.Appodeal;
import com.appodeal.ads.InterstitialCallbacks;
import com.appodeal.ads.NonSkippableVideoCallbacks;
import com.fishstix.dosboxfree.DBMain;
import com.fishstix.dosboxfree.R;

/**
 * Created by root on 1/9/18.
 */

public class SplashScreen {
    public DBMain dbMain;
    private LinearLayout titleScrn;
    private static final String TAG = "DosBoxTurbo";
    public  boolean showedControlls = false;

    // show controlls screen (on first time start iff doshdd not exists) (or when help pressed)
    public void showControlls() {
        //Toast.makeText(dbMain, "Showing Game Controlls", Toast.LENGTH_LONG).show();
        LinearLayout title = (LinearLayout) dbMain.findViewById(R.id.title_screen_layout);
        title.setVisibility(View.GONE);
        dbMain.mButtonsView.setVisibility(View.VISIBLE);
        AlphaAnimation alphaAnim = new AlphaAnimation(1f, 0.0f);
        alphaAnim.setDuration (12000);
        dbMain.bButtonA.startAnimation(alphaAnim);  // set alpha on background
        dbMain.bButtonC.startAnimation(alphaAnim);  // set alpha on background
        dbMain.bButtonD.startAnimation(alphaAnim);  // set alpha on background

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Do something after 5s = 5000ms
                showTitle(false);
                dbMain.mButtonsView.setVisibility(View.GONE);

                if (showedControlls) {
                    dbMain.restartApp();
                } else {
                    showedControlls = true;
                }

            }
        }, 10000);
    }

    boolean failedAd = false;
    // show ad
    public void showTitle(boolean firstTime) {
        if (firstTime) {
            showControlls();
            return;
        }

        dbMain.getSupportActionBar().hide();

        LinearLayout title = (LinearLayout) dbMain.findViewById(R.id.title_screen_layout);
        title.setVisibility(View.VISIBLE);
        Toast.makeText(dbMain, "Loading Game in Background...", Toast.LENGTH_LONG).show();

        final Handler adHandler = new Handler();
        adHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                Appodeal.setInterstitialCallbacks(new InterstitialCallbacks() {
                    @Override
                    public void onInterstitialLoaded(boolean b) {
                        if (dbMain.startupAd && !dbMain.mSurfaceView.startPlaying) {
                            dbMain.playingAd = true;
                            Appodeal.show(dbMain, Appodeal.INTERSTITIAL);
                        }
                    }

                    @Override
                    public void onInterstitialFailedToLoad() {
                        Log.e(TAG, "APPODEAL FAILED LOAD");
                        if (!failedAd) {
                            Toast.makeText(dbMain, "Failed to load ad :(", Toast.LENGTH_SHORT).show();
                            failedAd = true;
                        }
                    }

                    @Override
                    public void onInterstitialShown() {

                    }

                    @Override
                    public void onInterstitialClicked() {

                    }

                    @Override
                    public void onInterstitialClosed() {

                    }
                });

                /*Appodeal.setNonSkippableVideoCallbacks(new NonSkippableVideoCallbacks() {
                    @Override
                    public void onNonSkippableVideoLoaded() {
                        if (dbMain.startupAd && !dbMain.mSurfaceView.startPlaying) {
                            dbMain.playingAd = true;
                            Appodeal.show(dbMain, Appodeal.NON_SKIPPABLE_VIDEO);
                        }
                    }

                    @Override
                    public void onNonSkippableVideoFailedToLoad() {
                        Log.e(TAG, "APPODEAL FAILED LOAD");
                        if (!failedAd) {
                            Toast.makeText(dbMain, "Failed to load ad :(", Toast.LENGTH_SHORT).show();
                            failedAd = true;
                        }
                    }

                    @Override
                    public void onNonSkippableVideoShown() {

                    }

                    @Override
                    public void onNonSkippableVideoFinished() {

                    }

                    @Override
                    public void onNonSkippableVideoClosed(boolean b) {

                    }
                });
            }*/
            }
        }, 2000);

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                    }
                    catch (Exception e) {
                            e.printStackTrace();
                        }
                    if (dbMain.mSurfaceView != null) {
                       if (dbMain.mSurfaceView.checkGameLoaded()) {
                            Log.i(dbMain.TAG, "Finished Loading GAME");
                            break;
                       }
                    }
                }


                dbMain.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Do something after 5s = 5000ms

                        if (dbMain.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                            dbMain.enableActionBar(true);
                        else
                            dbMain.enableActionBar(false);
                        LinearLayout title = (LinearLayout) dbMain.findViewById(R.id.title_screen_layout);
                        title.setVisibility(View.GONE);
                    }
                });

            }
      });

      //  dbMain.setContentView(R.layout.main_nomenu);
    }
    // show pinball start screen


}
