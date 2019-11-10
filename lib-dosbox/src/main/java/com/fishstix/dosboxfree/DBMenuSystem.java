/*
 *  Copyright (C) 2012 Fishstix (ruebsamen.gene@gmail.com)
 *  
 *  Copyright (C) 2011 Locnet (android.locnet@gmail.com)
 *  
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
/*
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
*/
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingFlowParams;
import com.fishstix.dosboxfree.dosboxprefs.DosBoxPreferences;
import com.fishstix.dosboxfree.dosboxprefs.preference.GamePreference;
import com.fishstix.dosboxfree.touchevent.TouchEventWrapper;
import com.shamanland.adosbox2.DosHDD;
/*
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
*/
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.Toast;

public class DBMenuSystem {
	private static final String mPrefCycleString = "max";	// default slow system
	private static final Uri CONTENT_URI=Uri.parse("content://com.fishstix.dosboxlauncher.files/");
	private final static int JOYSTICK_CENTER_X = 0;
	private final static int JOYSTICK_CENTER_Y = 0;

	public static final int KEYCODE_F1 = 131;
	//public static final int KEYCODE_F12 = 142;

	public final static int CONTEXT_MENU_SPECIAL_KEYS = 1;
	public final static int CONTEXT_MENU_CYCLES = 2;
	public final static int CONTEXT_MENU_FRAMESKIP = 3;
	public final static int CONTEXT_MENU_MEMORY_SIZE = 4;
	public final static int CONTEXT_MENU_TRACKING = 5;
	public final static int CONTEXT_MENU_INPUTMODE = 6;
	 
	private final static int MENU_KEYBOARD_CTRL = 61;
	private final static int MENU_KEYBOARD_ALT = 62;
	private final static int MENU_KEYBOARD_SHIFT = 63;
	
	private final static int MENU_KEYBOARD_ESC = 65;
	private final static int MENU_KEYBOARD_TAB = 66;
	private final static int MENU_KEYBOARD_DEL = 67;
	private final static int MENU_KEYBOARD_INSERT = 68;
	private final static int MENU_KEYBOARD_PAUSE_BREAK = 82;
	private final static int MENU_KEYBOARD_SCROLL_LOCK = 83;

	private final static int MENU_KEYBOARD_F1 = 70;
	private final static int MENU_KEYBOARD_F12 = 81;
	private final static int MENU_KEYBOARD_SWAP_MEDIA = 91;
	private final static int MENU_KEYBOARD_TURBO = 92;
	
	private final static int MENU_CYCLE_AUTO = 150;
	private final static int MENU_CYCLE_55000 = 205;
	
	private final static int MENU_TRACKING_ABS = 220;
	private final static int MENU_TRACKING_REL = 221;
	
	private final static int MENU_FRAMESKIP_0 = 206;
	private final static int MENU_FRAMESKIP_10 = 216;

	private final static String PREF_KEY_FRAMESKIP = "dosframeskip";
	private final static String PREF_KEY_CYCLES = "doscycles";
	//private final static String PREF_KEY_KEY_MAPPING = "pref_key_key_mapping"; 
	
	public final static int INPUT_MOUSE = 0;
	public final static int INPUT_JOYSTICK = 1;
	public final static int INPUT_REAL_MOUSE = 2;
	public final static int INPUT_REAL_JOYSTICK = 3;
	public final static int INPUT_SCROLL = 4;
	
	//following must sync with AndroidOSfunc.cpp
	public final static int DOSBOX_OPTION_ID_SOUND_MODULE_ON = 1;
	public final static int DOSBOX_OPTION_ID_MEMORY_SIZE = 2;
	public final static int DOSBOX_OPTION_ID_CYCLES = 10;
	public final static int DOSBOX_OPTION_ID_FRAMESKIP = 11;
	public final static int DOSBOX_OPTION_ID_REFRESH_HACK_ON = 12;
	public final static int DOSBOX_OPTION_ID_CYCLE_HACK_ON = 13;
	public final static int DOSBOX_OPTION_ID_MIXER_HACK_ON = 14;
	public final static int DOSBOX_OPTION_ID_AUTO_CPU_ON = 15;
	public final static int DOSBOX_OPTION_ID_TURBO_ON = 16;
	public final static int DOSBOX_OPTION_ID_CYCLE_ADJUST = 17;
	public final static int DOSBOX_OPTION_ID_JOYSTICK_ENABLE = 18;
	public final static int DOSBOX_OPTION_ID_GLIDE_ENABLE = 19;
	public final static int DOSBOX_OPTION_ID_SWAP_MEDIA = 21;
	public final static int DOSBOX_OPTION_ID_START_COMMAND = 50;
	
	static public void loadPreference(DBMain context, final SharedPreferences prefs) {	
		// gracefully handle upgrade from previous versions, fishstix
		/*if (Integer.valueOf(prefs.getString("confcontroller", "-1")) >= 0) {
			DosBoxPreferences.upgrade(prefs);
		}*/
		Runtime rt = Runtime.getRuntime();
		long maxMemory = rt.maxMemory();
		Log.v("DosBoxTurbo", "maxMemory:" + Long.toString(maxMemory));
		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		int memoryClass = am.getMemoryClass();
		Log.v("DosBoxTurbo", "memoryClass:" + Integer.toString(memoryClass));
		int maxMem = (int) Math.max(maxMemory/1024, memoryClass) * 4;
		if (!prefs.getBoolean("dosmanualconf", false)) {  // only write conf if not in manual config mode
			// Build DosBox config
			// Set Application Prefs
			PrintStream out;
			InputStream myInput; 
			try {
				myInput = context.getAssets().open(DosBoxPreferences.CONFIG_FILE);
				Scanner scanner = new Scanner(myInput);
				out = new PrintStream(new FileOutputStream(context.mConfPath+context.mConfFile));
				// Write text to file
				out.println("[dosbox]");
				if (Integer.valueOf(prefs.getString("dosmemsize", "8")) < maxMem) { 
					out.println("memsize="+prefs.getString("dosmemsize", "8"));
				} else {
					out.println("memsize="+maxMem);
				}
				out.println("vmemsize=4");
				out.println("machine="+prefs.getString("dosmachine", "svga_s3"));
				out.println();
				out.println("[render]");
				out.println("frameskip="+prefs.getString("dosframeskip","2"));
				out.println("scaler=none");
				out.println();
				out.println("[cpu]");
				//if (DBMain.nativeGetCPUFamily() == 3) { // mips cpu - disable dynamic core
				//	out.println("core=normal");					
				//} else {
					out.println("core="+prefs.getString("doscpu", "dynamic"));
				//}
				out.println("cputype="+prefs.getString("doscputype", "auto"));
				if (prefs.getString("doscycles", "-1").contentEquals("-1")) {
					out.println("cycles="+mPrefCycleString);	// auto performance
				} else {
					out.println("cycles="+prefs.getString("doscycles", "3000"));
				}
				out.println("cycleup=500");
				out.println("cycledown=500");
				out.print("isapnpbios=");
				if (prefs.getBoolean("dospnp", true)) {
					out.println("true");
				} else {
					out.println("false");
				}				
				out.println();
				out.println("[sblaster]");
				out.println("sbtype=" + prefs.getString("dossbtype","sb16"));
				out.println("mixer=true");
				out.println("oplmode=auto");
				out.println("oplemu=fast");
				out.println("oplrate=" + prefs.getString("dossbrate", "22050"));
				out.println();
				out.println("[mixer]");
				try {
					out.println("prebuffer=" + prefs.getInt("dosmixerprebuffer", 15));
				} catch (Exception e) {
					out.println("prebuffer=15");
				}
				out.println("rate=" + prefs.getString("dossbrate", "22050"));
				out.println("blocksize=" + prefs.getString("dosmixerblocksize", "1024"));
				out.println();
				out.println("[dos]");
				out.print("xms=");
				if (prefs.getBoolean("dosxms", true)) {
					out.println("true");
				} else {
					out.println("false");
				}
				out.print("ems=");
				if (prefs.getBoolean("dosems", true)) {
					out.println("true");
				} else {
					out.println("false");
				}
				out.print("umb=");
				if (prefs.getBoolean("dosumb", true)) {
					out.println("true");
				} else {
					out.println("false");
				}
				out.println("keyboardlayout="+prefs.getString("doskblayout", "auto"));
				out.println();
				out.println("[ipx]");
				out.print("ipx=");
				if (prefs.getBoolean("dosipx", false)) {
					out.println("true");
				} else {
					out.println("false");
				}
				out.println();
				out.println("[joystick]");
				out.println("joysticktype=2axis");
				out.print("timed=");
				if (prefs.getBoolean("dostimedjoy", false)) {
					out.println("true");
				} else {
					out.println("false");
				}
				out.println();
				out.println("[midi]");
				if (prefs.getBoolean("dosmt32", false)) {
					out.println("mpu401=intelligent");
					out.println("mididevice=mt32");
					out.println("mt32.thread=on");
					out.println("mt32.verbose=off");
				} else {
					out.println("mpu401=none");
					out.println("mididevice=none");					
				}
				out.println();
				out.println("[speaker]");
				out.print("pcspeaker=");
				if (prefs.getBoolean("dospcspeaker", false)) {
					out.println("true");
				} else {
					out.println("false");
				}
				out.println("tandyrate=" + prefs.getString("dossbrate", "22050"));

				// concat dosbox conf
				while (scanner.hasNextLine()){
					out.println(scanner.nextLine());
				}
				// handle autoexec
				//	if (prefs.getString("dosautoexec","-1").contains("-1")) {
					out.println(  "mount c: " + DosHDD.hddLocation() + " -freesize 128\n" +
							"c:\n" +
							"cadet\n");
				/*} else {
					out.println(prefs.getString("dosautoexec",   "mount c: " + DosHDD.hddLocation() + " -freesize 128\n" +
							"c:\n" +
							"cadet\n"));
				}*/
				out.flush();
				out.close();
				myInput.close();
				scanner.close();
				Log.i("DosBoxTurbo","finished writing: "+ context.mConfPath+context.mConfFile);
			} catch (IOException e) {
				e.printStackTrace();
			} 
		}
		// SCALE SCREEN
		context.mSurfaceView.mScale = prefs.getBoolean("confscale", false);
		int spacePercent = prefs.getInt("controlspace", 20);
		context.mSurfaceView.controlSpace = (float) spacePercent / 100.0f;
		context.spaceSeekBar.setProgress(spacePercent);
		
		if (Integer.valueOf(prefs.getString("confscalelocation", "0")) == 0)
			context.mSurfaceView.mScreenTop = false;
		else 
			context.mSurfaceView.mScreenTop = true;
		
		// SCREEN SCALE FACTOR
		context.mPrefScaleFactor = prefs.getInt("confresizefactor", 100);
		
		// SCALE MODE
		if (Integer.valueOf(prefs.getString("confscalemode", "0"))==0) {
			context.mPrefScaleFilterOn = false;
		} else {
			context.mPrefScaleFilterOn = true;
		}
		 
		// ASPECT Ratio 
		context.mSurfaceView.mMaintainAspect = prefs.getBoolean("confkeepaspect", true);
		  
		// SET Cycles
		if (!prefs.getBoolean("dosmanualconf", false)) {
			try {
				DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_CYCLES, Integer.valueOf(prefs.getString("doscycles", "5000")),null, true);
			} catch (NumberFormatException e) {
				// set default to 5000 cycles on exception
				DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_CYCLES, 2000 ,null, true);
			}
		}
		
	/*	if (!DBMain.mLicenseResult || !DBMain.mSignatureResult) {
			prefs.edit().putString("doscycles", "2000").commit();
			DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_CYCLES, 5000 ,null, DBMain.getLicResult());			
		} */
		
		
		// Set Frameskip
		DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_FRAMESKIP, Integer.valueOf(prefs.getString("dosframeskip", "2")),null, true);		
		
		// TURBO CYCLE
		DBMain.nativeSetOption(DOSBOX_OPTION_ID_CYCLE_HACK_ON, prefs.getBoolean("confturbocycle", false)?1:0,null,true);
		// TURBO VGA
		DBMain.nativeSetOption(DOSBOX_OPTION_ID_REFRESH_HACK_ON, prefs.getBoolean("confturbovga", false)?1:0,null,true);
		// TURBO AUDIO
		context.mPrefMixerHackOn = prefs.getBoolean("confturbomixer", true);
		DBMain.nativeSetOption(DOSBOX_OPTION_ID_MIXER_HACK_ON, context.mPrefMixerHackOn?1:0,null,true);
		// 3DFX (GLIDE) EMULATION
		DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_GLIDE_ENABLE, prefs.getBoolean("dosglide", false)?1:0,null, true);
		// SOUND
		context.mPrefSoundModuleOn = prefs.getBoolean("confsound", true);
		DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_SOUND_MODULE_ON, context.mPrefSoundModuleOn?1:0,null,true);
		// AUTO CPU 
		//context.mPrefAutoCPUOn = prefs.getBoolean("dosautocpu", false);  
		//DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_AUTO_CPU_ON, context.mPrefAutoCPUOn?1:0,null,DBMain.getLicResult());
		DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_AUTO_CPU_ON, 0,null,true);

		// INPUT MODE

		
		// Emulate Mouse Click
		//context.mSurfaceView.mEmulateClick = prefs.getBoolean("confmousetapclick", false);
		// VOL BUTTONS
		//context.mPrefHardkeyOn = prefs.getBoolean("confvolbuttons", true);

/*
		if (prefs.getBoolean("confbuttonoverlay", false)) {
			context.mSurfaceView.mShowInfo = true;
		} else {
			context.mSurfaceView.mShowInfo = false;
		}
	*/	

		
		// enable/disable genericmotionevent handling for analog sticks
		//context.mSurfaceView.mGenericMotion = prefs.getBoolean("confgenericmotion", false);
	//	context.mSurfaceView.mAnalogStickPref = Short.valueOf(prefs.getString("confanalogsticks", "0"));

		// dpad / trackpad emulation

		
		// OS 2.1 - 2.3 < > key fix
		//context.mSurfaceView.mEnableLTKeyFix = prefs.getBoolean("conffixgingerkey", false);
		
		
		// Add custom mappings to ArrayList 
		//context.mSurfaceView.customMapList.clear();
		/*context.mSurfaceView.customMap.clear();
		for (short i=0;i<DosBoxPreferences.NUM_USB_MAPPINGS;i++) {
			int hardkey = Integer.valueOf(prefs.getString("confmap_custom"+String.valueOf(i)+GamePreference.HARDCODE_KEY, "-1"));
			if ( hardkey > 0) {
				int doskey = Integer.valueOf(prefs.getString("confmap_custom"+String.valueOf(i)+GamePreference.DOSCODE_KEY, "-1"));
				if (doskey > 0) {
					context.mSurfaceView.customMap.put(hardkey,doskey);
				}
			}
		}
		Log.i("DosBoxTurbo","Found " + context.mSurfaceView.customMap.size() + " custom mappings.");*/
		
		// Sliding Menu Style

	/*	// GESTURES
		context.mSurfaceView.mGestureUp = Short.valueOf(prefs.getString("confgesture_swipeup", "0"));
		context.mSurfaceView.mGestureDown = Short.valueOf(prefs.getString("confgesture_swipedown", "0"));

		// TOUCHSCREEN MOUSE
		context.mSurfaceView.mGestureSingleClick = Short.valueOf(prefs.getString("confgesture_singletap", "3"));
		context.mSurfaceView.mGestureDoubleClick = Short.valueOf(prefs.getString("confgesture_doubletap", "5"));
		context.mSurfaceView.mGestureTwoFinger = Short.valueOf(prefs.getString("confgesture_twofinger", "0"));
		context.mSurfaceView.mLongPress = prefs.getBoolean("confgesture_longpress", true);*/

		// FORCE Physical LEFT ALT
		context.mSurfaceView.mUseLeftAltOn = prefs.getBoolean("confaltfix", false);
		
		// SOUND
		DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_SOUND_MODULE_ON, (prefs.getBoolean("confsound", true))?1:0,null,true);
		// DEBUG
		context.mSurfaceView.mDebug = prefs.getBoolean("confdebug", false);
		
		if (context.mSurfaceView.mDebug) {
			// debug mode enabled, show warning
			//Toast.makeText(context, R.string.debug, Toast.LENGTH_LONG).show();
		}
	}
	


	static public void copyConfigFile(DBMain context) {
		try {
		      
			InputStream myInput = new FileInputStream(context.mConfPath + context.mConfFile);
			myInput.close();
			myInput = null;
		}
		catch (FileNotFoundException f) {
			try {
		    	InputStream myInput = context.getAssets().open(context.mConfFile);
		    	OutputStream myOutput = new FileOutputStream(context.mConfPath + context.mConfFile);
		    	byte[] buffer = new byte[1024];
		    	int length;
		    	while ((length = myInput.read(buffer))>0){
		    		myOutput.write(buffer, 0, length);
		    	}
		    	myOutput.flush();
		    	myOutput.close();
		    	myInput.close();
			} catch (IOException e) {
			}
		} catch (IOException e) {
		}
    }	
	
	
	static public void savePreference(DBMain context, String key, String value) {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		if (sharedPrefs != null) {
			SharedPreferences.Editor editor = sharedPrefs.edit();			
			if (editor != null) {		
				//if (PREF_KEY_REFRESH_HACK_ON.equals(key)) {		
				//	editor.putBoolean(PREF_KEY_REFRESH_HACK_ON, context.mPrefRefreshHackOn);
				//}
				editor.putString(key, value);

				editor.commit();
			}
		}		
	} 
	
	static public void saveBooleanPreference(Context context, String key, boolean value) {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		if (sharedPrefs != null) {
			SharedPreferences.Editor editor = sharedPrefs.edit();
		
			if (editor != null) {
				editor.putBoolean(key, value);
				editor.commit();
			}
		}				
	}
	
	static public boolean getBooleanPreference(Context context, String key) {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPrefs.getBoolean(key, false);
	}
	
	/*
	static public boolean doCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_KEYBOARD, 0, "Keyboard").setIcon(R.drawable.ic_menu_keyboard);
		menu.add(Menu.NONE, MENU_INPUT_INPUT_METHOD, 0, "Input Method").setIcon(R.drawable.ic_menu_flag);
		menu.add(Menu.NONE, MENU_KEYBOARD_SPECIAL, 0, "Special Keys").setIcon(R.drawable.ic_menu_flash);
		menu.add(Menu.NONE, MENU_SETTINGS_SCALE, 0, "Scale: Off").setIcon(R.drawable.ic_menu_resize);
		menu.add(Menu.NONE, MENU_PREFS,Menu.NONE,"Config").setIcon(R.drawable.ic_menu_settings);
		menu.add(Menu.NONE, MENU_QUIT, 0, "Exit").setIcon(R.drawable.ic_menu_close_clear_cancel);
		return true;		
	}*/
	
	static public boolean doPrepareOptionsMenu(DBMain context, Menu menu) {
		//menu.findItem(MENU_SETTINGS_SCALE).setTitle((context.mSurfaceView.mScale)?"Scale: On":"Scale: Off");
		//menu.findItem(R.id.menu_scale).setTitle((context.mSurfaceView.mScale)?"Scale: On":"Scale: Off");
		return true;
	}
	
	static public void doShowMenu(DBMain context) {
		context.openOptionsMenu();
	}

	static public void doHideMenu(DBMain context) {
		context.closeOptionsMenu();
	}
	
	static public void doShowKeyboard(DBMain context) {
		/*InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			if (!context.mSurfaceView.hasFocus()){ 
		        context.mSurfaceView.requestFocus();
			}
			imm.showSoftInput(context.mSurfaceView, 0);
			//context.mSurfaceView.mKeyboardVisible = true;
		}*/
	}

	static public void doHideKeyboard(DBMain context) {
	/*	InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.hideSoftInputFromWindow(context.mSurfaceView.getWindowToken(), 0);
			//context.mSurfaceView.mKeyboardVisible = false;
		}*/
	}
	
	static public void doConfirmQuit(final DBMain context) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(context);
    	builder.setTitle(R.string.app_name);
		builder.setMessage("Exit Pinball? High scores will be saved.");
		
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface arg0, int arg1) {
				context.stopDosBox();
			}
		});
		builder.setNegativeButton("Cancel", null);				
		builder.create().show();		
	}
	
	static public void doShowTextDialog(final DBMain context, String message) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(context);
    	builder.setTitle(R.string.app_name);
		builder.setMessage(message);
		
		builder.setPositiveButton("OK", null);

		builder.create().show();		
	}
	/*
	static public void doShowHideInfo(DBMain context, boolean show) {
		context.mSurfaceView.mInfoHide = show;
		context.mSurfaceView.forceRedraw();
	}*/

	private static boolean showingHighscores = false;
	static public boolean doOptionsItemSelected(DBMain context, MenuItem item)
	{
		List<Integer> ids = Arrays.asList(
				R.id.menu_exit,
		//		R.id.menu_inputmethod,
		//		R.id.menu_specialkeys,
		//	R.id.menu_keyboard,
		///		R.id.menu_joystick,
				R.id.menu_settings,
                R.id.menu_modify_controls,
                R.id.menu_remove_ads,
                R.id.menu_boost,
		//		R.id.menu_ingame_sound,
				R.id.menu_ingame_music,
				R.id.menu_highscore
		);

		DBMain dbMain = (DBMain) context;
		switch(ids.indexOf(item.getItemId())){
			case 0:
				doConfirmQuit(context);
			    break;
			case 1:
				Toast.makeText(context,
						"Warning: Changing settings might result in braking the app. Uninstall/Reinstall if something goes wrong.",
						Toast.LENGTH_LONG).show();
				if (context.mPID != null) {
					Intent i = new Intent(context, DosBoxPreferences.class);
					Bundle b = new Bundle();
					b.putString("com.fishstix.dosboxlauncher.pid", context.mPID);
					b.putBoolean("com.fishstix.dosboxlauncher.mlic", true);
					i.putExtras(b);
					context.startActivity(i);
				} else {
					Intent i = new Intent(context, DosBoxPreferences.class);
					Bundle b = new Bundle();
					b.putBoolean("com.fishstix.dosboxlauncher.mlic", true);
					i.putExtras(b);
					context.startActivity(i);
				}
			/*
				InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
				if (imm != null)
					imm.showInputMethodPicker();*/
				break;
			// modify controls
			case 2:
				if (!dbMain.showingControls) {
					dbMain.showControls();
					Toast.makeText(context, "Press the ⏎ (back) button to resume game", Toast.LENGTH_LONG).show();
				}
				else {
					dbMain.hideControls();
				}
				break;
				// remove ads
			case 3:
				if (dbMain.mBillingManager == null) {
					dbMain.createBilling();
				}
				if (!dbMain.mBillingManager.boughtNoAds)
					context.mBillingManager.initiatePurchaseFlow("no_ads", "inapp");
				else
					Toast.makeText(context, "Already purchased 'no-ads'!", Toast.LENGTH_SHORT).show();
				break;
			case 4:
			    boolean sound = !getBooleanPreference(context, "confsound");
                saveBooleanPreference(context, "confsound", sound);
                //context.bScaling.setChecked(!sound);
                if (sound)
					Toast.makeText(context, "Boost: OFF; Restart App to apply changes", Toast.LENGTH_LONG).show();
                else
					Toast.makeText(context, "Boost: ON; Restart App to apply changes", Toast.LENGTH_LONG).show();

				if (context.mSurfaceView != null)
                	context.mSurfaceView.forceRedraw();


                /*
				context.mSurfaceView.mContextMenu = CONTEXT_MENU_SPECIAL_KEYS;				
				context.openContextMenu(context.mSurfaceView);*/
				break;

			// case ingame sound toggled
			case 5:
				// press ALT + O, then M
				DosBoxControl.nativeTableEvent(1);
				break;
			// show highscore
			case 6:
				if (!showingHighscores) {
					if (DBMenuSystem.getBooleanPreference(context, "confscale")) {
						DosBoxControl.nativeTableEvent(5);

						(new Handler()).postDelayed(new Runnable() {
							@Override
							public void run() {
								DosBoxControl.nativeTableEvent(6);
								(new Handler()).postDelayed(new Runnable() {
									@Override
									public void run() {
										DosBoxControl.nativeTableEvent(6);
										(new Handler()).postDelayed(new Runnable() {
											@Override
											public void run() {
												DosBoxControl.nativeTableEvent(6);
												(new Handler()).postDelayed(new Runnable() {
													@Override
													public void run() {
														DosBoxControl.nativeTableEvent(7);
													}
												}, 100);
											}
										}, 100);
									}
								}, 100);
							}
						}, 100);
					}
					else
						DosBoxControl.nativeTableEvent(4);
					showingHighscores = true;
				}
				else {
					// send f4
					DosBoxControl.nativeTableEvent(2);
					// send f3
					DosBoxControl.nativeTableEvent(1);
					showingHighscores = false;
				}
				break;
			case 7:
				if (context.mPID != null) {
					Intent i = new Intent(context, DosBoxPreferences.class);
					Bundle b = new Bundle();
					b.putString("com.fishstix.dosboxlauncher.pid", context.mPID);
					b.putBoolean("com.fishstix.dosboxlauncher.mlic", true);
					i.putExtras(b);
					context.startActivity(i);
				} else {
					Intent i = new Intent(context, DosBoxPreferences.class);
					Bundle b = new Bundle();
					b.putBoolean("com.fishstix.dosboxlauncher.mlic", true);
					i.putExtras(b);
					context.startActivity(i);
				}
				break;
			default:
				break;
		  }
		  return true;
	}

	public static void getData(DBMain context, String pid) {
		try {
			 InputStream is = context.getContentResolver().openInputStream(Uri.parse(CONTENT_URI + pid + ".xml"));
			 FileOutputStream fostream;
			 // Samsung workaround:
			 File file = new File("/dbdata/databases/com.fishstix.dosbox/shared_prefs/");
			 if (file.isDirectory() && file.exists()) {
				 // samsung
				 fostream = new FileOutputStream("/dbdata/databases/com.fishstix.dosbox/shared_prefs/"+pid+".xml");
			 } else {
				 // every one else.
				 fostream = new FileOutputStream(context.getFilesDir()+"/../shared_prefs/"+pid+".xml");
			 }

			 PrintStream out = new PrintStream(fostream);
			 Scanner scanner = new Scanner(is);
			 while (scanner.hasNextLine()){
				out.println(scanner.nextLine());
			 }
			 out.flush();
			 is.close();
			 out.close();
			 scanner.close();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
    public static void CopyAsset(DBMain ctx, String assetfile) {
        AssetManager assetManager = ctx.getAssets();

        InputStream in = null;
        OutputStream out = null;
        try {
        	in = assetManager.open(assetfile);   // if files resides inside the "Files" directory itself
            out = ctx.openFileOutput(assetfile, Context.MODE_PRIVATE);
            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
        } catch(Exception e) {
             Log.e("DosBoxTurbo", e.getMessage());
        }
    }
    
    public static void CopyROM(DBMain ctx, File infile) {
        InputStream in = null;
        OutputStream out = null;
        try {
        	in = new FileInputStream(infile);   // if files resides inside the "Files" directory itself
            out = ctx.openFileOutput(infile.getName().toUpperCase(Locale.US), Context.MODE_PRIVATE);
            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
        } catch(Exception e) {
             Log.e("DosBoxTurbo", e.getMessage());
        }    	
    }
    
    public static boolean MT32_ROM_exists(DBMain ctx) {
    	File ctrlrom = new File(ctx.getFilesDir(), "MT32_CONTROL.ROM");
    	File pcmrom = new File(ctx.getFilesDir(), "MT32_PCM.ROM");
    	
    	if (ctrlrom.exists() && pcmrom.exists()) {
    		return true;
    	}
    	return false;
    }
    
    public static File openFile(String name) {
    	  File origFile = new File(name);
    	  File dir = origFile.getParentFile();
    	  if (dir.listFiles() != null) {
    		  for (File f : dir.listFiles()) {
    			  if (f.getName().equalsIgnoreCase(origFile.getName())) {
    				  return new File(f.getAbsolutePath());
    			  }
    		  }
    	  }  
		return new File(name);
    }
	
    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
          out.write(buffer, 0, read);
        }
    }
}
