/*
 *  Copyright (C) 2012 Fishstix - Based upon DosBox & anDosBox by Locnet
 *  
 *  Copyright (C) 2011 Locnet (android.locnet@gmail.com)
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package com.fishstix.dosboxfree;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.appodeal.ads.Appodeal;
import com.fishstix.dosboxfree.dosboxprefs.DosBoxPreferences;
import com.fishstix.dosboxfree.dosboxprefs.preference.HardCodeWrapper;
import com.fishstix.dosboxfree.joystick.JoystickView;
import com.shamanland.adosbox2.DosHDD;
import com.fishstix.dosboxfree.SplashScreen;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import android.support.v7.app.AppCompatActivity;

import static com.fishstix.dosboxfree.DBMenuSystem.KEYCODE_F1;
import static com.fishstix.dosboxfree.DBMenuSystem.getBooleanPreference;
import static com.fishstix.dosboxfree.DBMenuSystem.saveBooleanPreference;
import static com.fishstix.dosboxfree.DBMenuSystem.savePreference;

public class DBMain extends AppCompatActivity {
    public static String PACKAGE_NAME;
    public static final String TAG = "DosBoxTurbo";
	public static final int SPLASH_TIMEOUT_MESSAGE = -1;
	public static final String START_COMMAND_ID = "start_command";
	public String mConfFile = DosBoxPreferences.CONFIG_FILE;
	//public String mConfPath = DosBoxPreferences.CONFIG_PATH;
	public String mConfPath;
	public static final int HANDLER_DISABLE_GPU = 323;

	public native void nativeInit(Object ctx);
	public static native void nativeShutDown();
	public static native void nativeSetOption(int option, int value, String value2, boolean l);
	public native void nativeStart(Object ctx, Bitmap bitmap, int width, int height, String confPath);
	public static native void nativePause(int state);
	public static native void nativeStop();
	public static native void nativePrefs();
	public static native boolean nativeHasNEON(Object ctx);
	public static native boolean nativeIsARMv7(Object ctx);
	public static native boolean nativeIsARMv15(Object ctx);
	public static native int nativeGetCPUFamily();

	public DBGLSurfaceView mSurfaceView = null;
	public DosBoxAudio mAudioDevice = null;
	public DosBoxThread mDosBoxThread = null;
	public SharedPreferences prefs;
	private static DBMain mDosBoxLauncher = null;
	public FrameLayout mFrameLayout = null;
	SplashScreen mSplashScreen;

	public boolean mPrefScaleFilterOn = false;
	public boolean mPrefSoundModuleOn = true;
	public boolean mPrefMixerHackOn = true;
	public boolean mTurboOn = false;
	public String mPID = null;
	public int mPrefScaleFactor = 100;
	private Context mContext;
	public Button bButtonA,bButtonB,bButtonC,bButtonD, bPortraitScale, bShowKeyboard;
	public SeekBar spaceSeekBar;
	public BillingManager mBillingManager = null;
	public UpdateListener mUpdateListener = null;

	public LinearLayout mButtonsView = null;

	private boolean permissionsOk = false;
	boolean showingKeyboard = false;
	private int locationTries = 3;
	boolean firstTime = false;
	boolean showingControls = false;

	static {
		System.loadLibrary("SDL2");
		System.loadLibrary("dosbox");
        System.loadLibrary("fishstix_util");

	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate()");
		mDosBoxLauncher = this;
		PACKAGE_NAME = getApplicationContext().getPackageName();

		mContext = this;
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		//requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

		super.onCreate(savedInstanceState);

		setContentView(R.layout.main_nomenu);
		initViews();
		evalPermissions();

	}

	void evalPermissions() {
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE))
				showAlertDialog("PinballXP needs permission to save / load game files from the SDCard. It will not access/modify any other data.", Manifest.permission.WRITE_EXTERNAL_STORAGE);
			else
				ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.GET_ACCOUNTS},
					0);
			return;
		}

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		boolean wantsLocation = sharedPreferences.getBoolean("wantsLocation", true);

		if (wantsLocation && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION))
				showAlertDialog("To show relavent & enjoyable ads, Pinball XP needs to access your general location (country) through Wi-Fi.", Manifest.permission.ACCESS_COARSE_LOCATION);
			else
				ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
					0);
			if (locationTries > 0) {
				locationTries--;
				return;
			}
			else {
				sharedPreferences.edit().putBoolean("wantsLocation", false).commit();
			}
		}
		permissionsOk = true;
		startApp();
	}

	private void showAlertDialog(String info, final String permission) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(info).setNeutralButton("Ok!", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ActivityCompat.requestPermissions((DBMain) mContext,
						new String[]{permission},
						0);
			}
		});
		// Create the AlertDialog object and return it
		builder.create();
		builder.show();
	}

	public void startApp() {

		/*if (nativeHasNEON(this)) {
			System.loadLibrary("neonlib");
		}*/

		if (mSplashScreen == null) {
			mSplashScreen = new SplashScreen();
		}

		mSplashScreen.dbMain = this;

        boolean reextract = false;
        int versionCodeReal = -1;

		try {
			PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
			versionCodeReal = pInfo.versionCode;
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		int versionCode = sharedPreferences.getInt("VERSION_CODE", -1);

		if(versionCode != versionCodeReal) {
			reextract = true;
		}
		sharedPreferences.edit().putInt("VERSION_CODE", versionCodeReal).apply();

        //scrn.showControlls();
		// Create HDD from zipped file
		if (!DosHDD.hddExists() || reextract) {
			mSplashScreen.showControlls();
			Toast.makeText(this, "Extracting game files... Do NOT close app!", Toast.LENGTH_SHORT).show();
			Log.i(TAG, "First Time Install or Update!");
		    // first time start presumably
            //Toast.makeText(mContext, "First time start: Please Wait...", Toast.LENGTH_LONG).show();
            // mSplashScreen.showControlls();
		    Log.i(TAG,"Dos HDD not found, creating...");

            Thread t = new Thread(new Runnable() {
                public void run() {
                    if (!DosHDD.createHdd(mContext))
                        Log.i(TAG, "Failed to create Dos HDD");
                    Log.i(TAG, "Stopping Unzip Thread...");
					//DBMain app = (DBMain)mContext;

					//while (app.mButtonsView.getVisibility() != View.GONE);

					//app.runOnUiThread(new Runnable() {
					/*	@Override
						public void run() {
							DBMain app = (DBMain) mContext;
							if (app.mSplashScreen.showedControlls)
								app.recreate();
							else
								app.mSplashScreen.showedControlls = true;
						}
					});*/

					/*Intent mStartActivity = new Intent(mContext, DBMain.class);
					int mPendingIntentId = 123456;
					PendingIntent mPendingIntent = PendingIntent.getActivity(mContext, mPendingIntentId,    mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
					AlarmManager mgr = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
					mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);*/

					mDosBoxLauncher.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(mContext, "Install complete. Restarting App...", Toast.LENGTH_LONG).show();

							if (mSplashScreen.showedControlls) {
								restartApp();
							}
							else {
								mSplashScreen.showedControlls = true;
							}
						}
					});
                }
            });
            t.start();
            return;

        }
		createBilling();

		Log.i(TAG, "Dos HDD found!");
		mSplashScreen.showTitle(firstTime);

		mConfPath = DosBoxPreferences.getExternalDosBoxDir(mContext);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
	    getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
	    getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		if (nativeHasNEON(this)) {
			Log.i(TAG, "NEON DETECTED");
		}
		else {
			Log.i(TAG, "NO NEON SUPPORT");
		}
		registerForContextMenu(mSurfaceView);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		DBMenuSystem.loadPreference(this,prefs);
		//mSurfaceView.mGPURendering = true; //prefs.getBoolean("confgpu", false);

		String cmd =    "mount c: " + DosHDD.hddLocation() + " -freesize 128\n" +
				"c:\n" +
				"cadet\n";

		prefs.edit().putString("dosautoexec",cmd).commit();
		boolean scaleState = DBMenuSystem.getBooleanPreference(mContext, "confscale");
		mSurfaceView.mScale = scaleState;
		if (scaleState)
			bPortraitScale.setText("-");
		else
			bPortraitScale.setText("+");

		initStartDosBox();
	}

	public void createBilling() {
		if (mUpdateListener == null)
			mUpdateListener = new UpdateListener();
		if (mBillingManager == null)
			mBillingManager = new BillingManager(this, mUpdateListener);
	}

	public void restartApp() {

		Intent mStartActivity = new Intent(mContext, DBMain.class);
		int mPendingIntentId = 123456;
		PendingIntent mPendingIntent = PendingIntent.getActivity(mContext, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager mgr = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
		mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);

		System.exit(0);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		Log.i(TAG, "Permission Granted");
		evalPermissions();
	}

 	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == 1) {
			if (resultCode == RESULT_OK) {
				String act = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
				Log.d(TAG, "Selected account " + act);
				String target = "marco.lova13@gmail.com";
				if (act.equals(target)) {
					Log.d(TAG, "account equals special account. Disabling ads...");
					prefs.edit().putBoolean("ads_off", true).commit();
					Toast.makeText(this, "Disabled ads. Please restart the app for changes to take effect.", Toast.LENGTH_LONG).show();
				}
				else {
					Toast.makeText(this, "Failed to Authenticate for disabling ads. Selected: " + act + "; Target: " + target, Toast.LENGTH_LONG).show();
				}
			}
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {
			getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
					| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_FULLSCREEN
					| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
		}
	}

	public class UpdateListener implements BillingManager.BillingUpdatesListener {
		@Override
		public void onBillingClientSetupFinished() {
		//	mActivity.onBillingManagerSetupFinished();
		}

		@Override
		public void onConsumeFinished(String token, @BillingClient.BillingResponse int result) {
		}

		@Override
		public void onPurchasesUpdated(List<Purchase> purchaseList) {

			for (Purchase purchase : purchaseList) {
				if (purchase.getSku().equals("no_ads")) {
					//Toast.makeText(mContext, "You've purchased No Ads! Restart App to instigate changes", Toast.LENGTH_LONG).show();
					//DBMenuSystem.doConfirmQuit((DBMain)mContext);
					Log.i(TAG, "Ad status: DISABLED");
					mBillingManager.boughtNoAds = true;
					return;
				}
			}
			mBillingManager.boughtNoAds = false;


			String appKey = "f8ecea605126edd53010ef78eafcd8dd3c6208802a6f2e7d";

			boolean disabledAds = prefs.getBoolean("ads_off", false);

			if(disabledAds == false) {
				Intent intent = AccountManager.newChooseAccountIntent(null, null, new String[]{"com.google"},null, null, null, null);
				startActivityForResult(intent, 1);
			}
			else {
				Log.d(TAG, "Ads have been disabled due to special account found.");
				Toast.makeText(getApplicationContext(), "Ads disabld for Marco :-)", Toast.LENGTH_SHORT).show();
				return;
			}

				/*Account[] accounts = AccountManager.get(getApplicationContext()).getAccountsByType("com.google");
			for (Account act : accounts) {
				Log.d(TAG, "Found account: " + act.name);
				if (act.name.equals("travisjayday@gmail.com")) {
					Log.d(TAG, "Disabled ads because specified account found");
					Toast.makeText(getApplicationContext(), "Disabled Ads for Marco :-)", Toast.LENGTH_SHORT).show();
					return;
				}
			}*/
			Log.i(TAG, "Ad Status: Enabled");

			Log.d(TAG, "No special accounts found");
			//Appodeal.setTesting(true);
			//Appodeal.setAutoCache(Appodeal.INTERSTITIAL, false);
			Appodeal.initialize((DBMain) mContext, appKey, Appodeal.INTERSTITIAL);
			//Appodeal.cache((DBMain)mContext, Appodeal.INTERSTITIAL);
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	private void initViews() {
		mFrameLayout = (FrameLayout)findViewById(R.id.mFrame);
		mSurfaceView = (DBGLSurfaceView)findViewById(R.id.mSurf);
		//mJoystickView = (JoystickView)findViewById(R.id.mJoystickView);
		mButtonsView = (LinearLayout)findViewById(R.id.ButtonLayout);

		bButtonA = (Button)findViewById(R.id.ButtonA);
		bButtonB = (Button)findViewById(R.id.ButtonB);
		bButtonC = (Button)findViewById(R.id.ButtonC);
		bButtonD = (Button)findViewById(R.id.ButtonD);
		bPortraitScale = (Button) findViewById(R.id.ButtonPortraitScale);
		bShowKeyboard = (Button) findViewById(R.id.ButtonShowKeyboard);

		spaceSeekBar = (SeekBar) findViewById(R.id.seekbar_space);

		hideControls();
		final LinearLayout vButtonHolder = (LinearLayout) findViewById(R.id.vertical_button_holder);
		spaceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				float percent = (float) progress / 100.0f;
				LinearLayout.LayoutParams param1 = (LinearLayout.LayoutParams) vButtonHolder.getLayoutParams();
				param1.weight = 1.0f - percent;
				vButtonHolder.setLayoutParams(param1);
				LinearLayout.LayoutParams param2 = (LinearLayout.LayoutParams) bButtonC.getLayoutParams();
				param2.weight = percent;
				bButtonC.setLayoutParams(param2);
				//Log.d(TAG, "percent: " + percent);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				float percent = (float) seekBar.getProgress() / 100.0f;
				Log.d(TAG, "Saving controlspace percent: " + percent);
				prefs.edit().putInt("controlspace", seekBar.getProgress()).apply();
				mSurfaceView.controlSpace = percent;
			}
		});

		bButtonB.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
            	// press enter and f2 key
				Log.i(TAG, "Restart button pressed (in UI)");
				AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

				//builder.setTitle("Save");
				builder.setMessage("Save Game Score Permanently?");

				builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						if (Appodeal.isLoaded(Appodeal.INTERSTITIAL)) {
							playingAd = true;
							startupAd = true; // dont pause dosbox
							Toast.makeText((DBMain) mContext, "Saving Game in Background...", Toast.LENGTH_SHORT).show();
							Appodeal.show((DBMain) mContext, Appodeal.INTERSTITIAL);
						}
						// Do nothing but close the dialog
						DosBoxControl.nativeTableEvent(0);
						DosBoxControl.nativeTableEvent(2);

						Handler handler = new Handler();
						handler.postDelayed(new Runnable() {
							@Override
							public void run() {
								DosBoxControl.nativeTableEvent(0);
								Handler handler = new Handler();
								handler.postDelayed(new Runnable() {
									@Override
									public void run() {
										DosBoxControl.nativeTableEvent(0);
									}
								}, 500);
							}
						}, 500);
						dialog.dismiss();
					}
				});

				builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (Appodeal.isLoaded(Appodeal.INTERSTITIAL)) {
							playingAd = true;
							startupAd = true; // dont pause dosbox
							Toast.makeText((DBMain) mContext, "Saving Game in Background...", Toast.LENGTH_SHORT).show();
							Appodeal.show((DBMain) mContext, Appodeal.INTERSTITIAL);
						}
						DosBoxControl.nativeTableEvent(0);
						// Do nothing
						dialog.dismiss();
					}
				});

				AlertDialog alert = builder.create();
				alert.show();
            }
        });

		bPortraitScale.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				boolean scaleState = DBMenuSystem.getBooleanPreference(mContext, "confscale");
				scaleState = !scaleState;
				DBMenuSystem.saveBooleanPreference(mContext, "confscale", scaleState);
				mSurfaceView.mScale = scaleState;
				mSurfaceView.setDirty();

				if (scaleState) {
				    Toast.makeText(mContext, "Max scaling: ON", Toast.LENGTH_SHORT).show();
				    bPortraitScale.setText("-");
                }
                else {
					Toast.makeText(mContext, "Max scaling: OFF", Toast.LENGTH_SHORT).show();
					bPortraitScale.setText("+");
				}
              }
		});

		bShowKeyboard.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				if (imm != null) {
					if (!showingKeyboard) {
						imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
						showingKeyboard = true;
					}
					else {
						imm.hideSoftInputFromWindow(mSurfaceView.getWindowToken(), 0);
						showingKeyboard = false;
					}
				}
			}
		});
	}

	public void showControls() {
		spaceSeekBar.setVisibility(View.VISIBLE);
		mButtonsView.setVisibility(View.VISIBLE);
		showingControls = true;
	}

	public void hideControls() {
		spaceSeekBar.setVisibility(View.GONE);
		mButtonsView.setVisibility(View.GONE);
		showingControls = false;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		DosBoxControl.nativeKeyboardStroke(keyCode, 1, 0, 0, keyCode >= 'A' && keyCode <= 'Z'? 1 : 0);
		DosBoxControl.nativeKeyboardStroke(keyCode, 0, 0, 0, keyCode >= 'A' && keyCode <= 'Z'? 1 : 0);
		return false;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			switch (keyCode) {
				case KeyEvent.KEYCODE_BACK:
					if (showingControls) {
						hideControls();
					}
					break;
			}
		}
		return false;
	}

	@Override
	protected void onDestroy() {
		Log.i(TAG, "onDestroy()");
		if (mDosBoxThread != null && mSurfaceView != null) {
			stopDosBox();
			shutDownDosBox();
			mSurfaceView.shutDown();
			mSurfaceView = null;
		}
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		Log.i(TAG,"onPause()");

		if (!permissionsOk) {
            Log.e(TAG, "PERMISSIONS NOT OK!!");
            super.onPause();
			return;
		}

		if (showingKeyboard) {
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			if (imm != null && showingKeyboard) {
				imm.hideSoftInputFromWindow(mSurfaceView.getWindowToken(), 0);
				showingKeyboard = false;
			}
		}

		if (startupAd) {
			pauseDosBox(false);
			startupAd = false;
			Log.i(TAG, "Setting startup ad to false (Not pausing Dosbox)");
		} else {
			pauseDosBox(true);
		}
		super.onPause();
	}

	@Override
	protected void onStop() {
		Log.i(TAG,"onStop()");
		//onPause();
		super.onStop();
	}

	public boolean startupAd = true;
	public boolean playingAd = false;

	@Override
	protected void onResume() {
		if (!permissionsOk) {
		    Log.e(TAG, "PERMISSIONS NOT OK!");
			super.onResume();
			return;
		}

		Log.i(TAG,"onResume()");
		super.onResume();
		pauseDosBox(false);

		if (mSurfaceView != null)
			DBMenuSystem.loadPreference(this,prefs);

		if (Appodeal.isLoaded(Appodeal.INTERSTITIAL) && !playingAd) {
			Log.i(TAG, "Showing interstetial ad in Resume()");
			playingAd = true;
			Appodeal.show(this, Appodeal.INTERSTITIAL);
		}
		else if (playingAd)
			playingAd = false;

		// set rotation
		/*if (Integer.valueOf(prefs.getString("confrotation", "0"))==0) {
			// auto
	        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		} else if (Integer.valueOf(prefs.getString("confrotation", "0"))==1) {
			// portrait
	        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		} else {
	        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}*/

		// check orientation to hide actionbar (in landscape)
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			enableActionBar(false);
		}
		if (mSurfaceView != null)
    		mSurfaceView.mDirty.set(true);
        Log.i(TAG,"onResume");
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    Log.i(TAG,"Config Change");
	 //   Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
	    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) //To fullscreen
		{
			enableActionBar(false);
			if (mSurfaceView != null)
            	mSurfaceView.updateScreenWidth(false);
			if (bPortraitScale != null)
				bPortraitScale.setVisibility(View.GONE);
		}
	    else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT)
	    {
			enableActionBar(true);
			if (mSurfaceView != null)
            	mSurfaceView.updateScreenWidth(true);
			if (bPortraitScale != null)
				bPortraitScale.setVisibility(View.VISIBLE);
	    }
	}

	public void enableActionBar(boolean enabled) {
		if (enabled) {
			if (getSupportActionBar() != null)
				getSupportActionBar().show();
			else if (getActionBar() != null)
				getActionBar().show();
		}
		else {
			if (getSupportActionBar() != null)
				getSupportActionBar().hide();
			else if (getActionBar() != null)
				getActionBar().hide();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//return DBMenuSystem.doCreateOptionsMenu(menu);
	    getMenuInflater().inflate(R.menu.options,  menu);
	    return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu (Menu menu) {
		super.onPrepareOptionsMenu(menu);
		return DBMenuSystem.doPrepareOptionsMenu(this, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)	{
		if (DBMenuSystem.doOptionsItemSelected(this, item))
			return true;
	    return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		  super.onCreateContextMenu(menu, v, menuInfo);
		 // DBMenuSystem.doCreateContextMenu(this, menu, v, menuInfo);
	}


	void pauseDosBox(boolean pause) {
		if (mDosBoxThread == null)
			return;

		if (pause) {
			mDosBoxThread.mDosBoxRunning = false;

			nativePause(1);
			if (mAudioDevice != null)
				mAudioDevice.pause();
		}
		else {
			nativePause(0);
			mDosBoxThread.mDosBoxRunning = true;
			//will auto play audio when have data
			//if (mAudioDevice != null)
			//	mAudioDevice.play();
		}
	}


	public void initStartDosBox() {
		initDosBox();

		(new Handler()).postDelayed(new Runnable() {
			@Override
			public void run() {
				startDosBox();
			}
		}, 2000);

		(new Handler()).postDelayed(new Runnable() {
			@Override
			public void run() {
			}
		}, 2000);
	}

	void initDosBox() {

        mAudioDevice = new DosBoxAudio(this);

		nativeInit(mDosBoxLauncher);

		String argStartCommand = getIntent().getStringExtra(START_COMMAND_ID);

		if (argStartCommand == null) {
			argStartCommand = "";
		}

		String cmd =    "mount c: " + DosHDD.hddLocation() + " -freesize 128\n" +
				"c:\n" +
				"cadet\n";

		nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_START_COMMAND, 0, cmd, true);
		nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_MIXER_HACK_ON, (mPrefMixerHackOn)?1:0,null, true);
		nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_SOUND_MODULE_ON, (mPrefSoundModuleOn)?1:0,null, true);

		mDosBoxThread = new DosBoxThread(this);
	}

	void shutDownDosBox() {
		boolean retry;
		retry = true;
		while (retry) {
			try {
				mDosBoxThread.join();
				retry =	false;
			}
			catch (InterruptedException e) { // try again shutting down the thread
			}
		}
		nativeShutDown();

		if (mAudioDevice != null) {
			mAudioDevice.shutDownAudio();
			mAudioDevice = null;
		}
		mDosBoxThread = null;
	}

	void startDosBox() {

		if (mDosBoxThread != null) {
			mDosBoxThread.setPriority(Thread.MAX_PRIORITY);
			if (mDosBoxThread.getState() == Thread.State.NEW)
				mDosBoxThread.start();
		}

		if ((mSurfaceView != null) && (mSurfaceView.mVideoThread != null)) {
			if (mSurfaceView.mVideoThread.getState() == Thread.State.NEW)
				mSurfaceView.mVideoThread.start();
		}

	}

	void stopDosBox() {
		DosBoxControl.nativeTableEvent(2);

		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				nativePause(0);//it won't die if not running

				//stop audio AFTER above
				if (mAudioDevice != null)
					mAudioDevice.pause();

				if (mSurfaceView != null && mSurfaceView.mVideoThread != null)
					mSurfaceView.mVideoThread.setRunning(false);
				//mSurfaceView.mMouseThread.setRunning(false);

				nativeStop();

				System.exit(0);
			}
		}, 2000);
	}

	public void callbackExit() {
		if (mDosBoxThread != null)
			mDosBoxThread.doExit();
	}

	public void callbackVideoRedraw( int w, int h, int s, int e) {
		if ((mSurfaceView.mSrc_width != w)||(mSurfaceView.mSrc_height != h)) {
			mSurfaceView.bDirtyCoords.set(true);
		}
		mSurfaceView.mSrc_width = w;
		mSurfaceView.mSrc_height = h;
		synchronized (mSurfaceView.mDirty) {
			if (mSurfaceView.mDirty.get()) {
				mSurfaceView.mStartLine = Math.min(mSurfaceView.mStartLine, s);
				mSurfaceView.mEndLine = Math.max(mSurfaceView.mEndLine, e);
			}
			else {
				mSurfaceView.mStartLine = s;
				mSurfaceView.mEndLine = e;
			}
			mSurfaceView.mDirty.set(true);
		}
	}

	public Bitmap callbackVideoSetMode( int w, int h) {
		if ((mSurfaceView.mSrc_width != w)||(mSurfaceView.mSrc_height != h)) {
			mSurfaceView.bDirtyCoords.set(true);
		}
		mSurfaceView.mSrc_width = w;
		mSurfaceView.mSrc_height = h;
		mSurfaceView.resetScreen(false);
		Bitmap newBitmap = null;
		newBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
		//newBitmap = Bitmap.createBitmap(OpenGLRenderer.getNearestPowerOfTwoWithShifts(w), OpenGLRenderer.getNearestPowerOfTwoWithShifts(h), Bitmap.Config.RGB_565);
		if (newBitmap != null) {
			mSurfaceView.mBitmap = null;
			mSurfaceView.mBitmap = newBitmap;

			//locnet, 2011-04-28, support 2.1 or below
			//mSurfaceView.mVideoBuffer = null;
			//mSurfaceView.mVideoBuffer = ByteBuffer.allocateDirect(w * h * DBGLSurfaceView.PIXEL_BYTES);
			//mSurfaceView.mVideoBuffer = ByteBuffer.allocateDirect(OpenGLRenderer.getNearestPowerOfTwoWithShifts(w) * OpenGLRenderer.getNearestPowerOfTwoWithShifts(h) * DBGLSurfaceView.PIXEL_BYTES);
			return mSurfaceView.mBitmap;
		}

		return null;
	}

	//locnet, 2011-04-28, support 2.1 or below
	public Buffer callbackVideoGetBuffer() {
			return null;
	}

	public int callbackAudioInit(int rate, int channels, int encoding, int bufSize) {
		if (mAudioDevice != null)
			return mAudioDevice.initAudio(rate, channels, encoding, bufSize);
		else
			return 0;
	}

	public void callbackAudioShutdown() {
		if (mAudioDevice != null)
			mAudioDevice.shutDownAudio();
	}

	public void callbackAudioWriteBuffer(int size) {
		if (mAudioDevice != null)
			mAudioDevice.AudioWriteBuffer(size);
	}

	public short[] callbackAudioGetBuffer() {
		if (mAudioDevice != null)
			return mAudioDevice.mAudioBuffer;
		else
			return null;
	}

	class DosBoxThread extends Thread {
		DBMain mParent;
		public boolean	mDosBoxRunning = false;

		DosBoxThread(DBMain parent) {
			mParent =  parent;
		}

		public void run() {
			mDosBoxRunning = true;
			Log.i(TAG, "Using DosBox Config: "+mConfPath+mConfFile);
			nativeStart(mDosBoxLauncher, mSurfaceView.mBitmap, mSurfaceView.mBitmap.getWidth(), mSurfaceView.mBitmap.getHeight(), mConfPath+mConfFile);
			//will never return to here;
		}

		public void doExit() {
			if (mSurfaceView != null) {
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				if (imm != null) {
					imm.hideSoftInputFromWindow(mSurfaceView.getWindowToken(), 0);
				}
			}

			mDosBoxRunning = false;
			mParent.finish();
		}
	}
}