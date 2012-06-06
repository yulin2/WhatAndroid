package what.settings;

import java.net.URISyntaxException;

import what.gui.R;
import what.inbox.ReportActivity;
import what.login.UpdateChecker;
import what.services.AnnouncementService;
import what.services.InboxService;
import what.services.NotificationService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;
import api.son.MySon;
import api.soup.MySoup;

public class SettingsActivity extends PreferenceActivity implements OnPreferenceClickListener {
	private static final int FILE_SELECT_CODE = 0;

	// UI
	private Preference customBackground_preference, quickSearch_preference, spotifyButton_preference, lastfmButton_preference;
	// Services
	private Preference announcementsService_preference, inboxService_preference, notificationsService_preference;
	// Refresh Intervals
	private Preference announcementsService_interval, inboxService_interval, notificationsService_interval;
	// Developer
	private Preference debug_preference;
	// Report
	private Preference report_preference;

	private SharedPreferences sharedPreferences;

	private Intent inboxService;
	private Intent notificationService;
	private Intent annoucementService;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settingsactivity);

		PreferenceScreen prefenceScreen = getPreferenceScreen();
		PreferenceCategory preferenceCategory = new PreferenceCategory(this);
		// TODO fix
		preferenceCategory.setTitle("Version " + getInstalledVersion());
		prefenceScreen.addPreference(preferenceCategory);

		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		customBackground_preference = findPreference("customBackground_preference");
		customBackground_preference.setOnPreferenceClickListener(this);

		quickSearch_preference = findPreference("quickSearch_preference");
		quickSearch_preference.setOnPreferenceClickListener(this);

		spotifyButton_preference = findPreference("spotifyButton_preference");
		spotifyButton_preference.setOnPreferenceClickListener(this);

		lastfmButton_preference = findPreference("lastfmButton_preference");
		lastfmButton_preference.setOnPreferenceClickListener(this);

		announcementsService_preference = findPreference("announcementsService_preference");
		announcementsService_preference.setOnPreferenceClickListener(this);

		inboxService_preference = findPreference("inboxService_preference");
		inboxService_preference.setOnPreferenceClickListener(this);

		notificationsService_preference = findPreference("notificationsService_preference");
		notificationsService_preference.setOnPreferenceClickListener(this);

		annoucementService = new Intent(this, AnnouncementService.class);
		inboxService = new Intent(this, InboxService.class);
		notificationService = new Intent(this, NotificationService.class);

		debug_preference = findPreference("debug_preference");
		debug_preference.setOnPreferenceClickListener(this);

		report_preference = findPreference("report_preference");
		report_preference.setOnPreferenceClickListener(this);
	}

	private double getInstalledVersion() {
		int versionCode;
		String versionName;
		double installedVersion = 0;
		try {
			PackageInfo manager = getPackageManager().getPackageInfo(getPackageName(), 0);
			versionCode = manager.versionCode;
			versionName = manager.versionName;
			installedVersion = versionCode + Double.parseDouble(versionName);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		// TODO remove
		// return installedVersion;
		return UpdateChecker.VERSION;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.preference.Preference.OnPreferenceClickListener#onPreferenceClick(android.preference.Preference)
	 */
	@Override
	public boolean onPreferenceClick(Preference pref) {
		if (pref == customBackground_preference) {
			if (sharedPreferences.getBoolean("customBackground_preference", true)) {
				showFileChooser();
				Toast.makeText(this, "Changes will take effect next time you open a page", Toast.LENGTH_LONG).show();
			}
		}
		if (pref == announcementsService_preference) {
			if (sharedPreferences.getBoolean("announcementsService_preference", true) == true && !AnnouncementService.isRunning()) {
				Toast.makeText(SettingsActivity.this, "Services are very expieremtnal, prepare yourself for bugs",
						Toast.LENGTH_LONG).show();
				try {
					startService(annoucementService);
				} catch (Exception e) {
					Toast.makeText(this, "Could not start service", Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}
			} else {
				try {
					stopService(annoucementService);
				} catch (Exception e) {
					Toast.makeText(this, "Could not stop service", Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}
			}
		}
		if (pref == inboxService_preference) {
			Toast.makeText(SettingsActivity.this, "Services are very expieremtnal, prepare yourself for bugs", Toast.LENGTH_LONG)
					.show();
			if (sharedPreferences.getBoolean("inboxService_preference", true) == true && !InboxService.isRunning()) {
				try {
					startService(inboxService);
					Toast.makeText(this, sharedPreferences.getString("inboxService_interval", "-1"), Toast.LENGTH_SHORT).show();
				} catch (Exception e) {
					Toast.makeText(this, "Could not start service", Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}

			} else {
				try {
					stopService(inboxService);
				} catch (Exception e) {
					Toast.makeText(this, "Could not stop service", Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}
			}
		}
		if (pref == notificationsService_preference) {
			if (sharedPreferences.getBoolean("notificationsService_preference", true) == true && !NotificationService.isRunning()
					&& MySoup.canNotifications()) {
				Toast.makeText(SettingsActivity.this, "Services are very expieremtnal, prepare yourself for bugs",
						Toast.LENGTH_LONG).show();
				try {
					startService(notificationService);
				} catch (Exception e) {
					Toast.makeText(this, "Could not start service", Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}
			} else {
				try {
					stopService(notificationService);
				} catch (Exception e) {
					Toast.makeText(this, "Could not stop service", Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}
			}
		}
		if (pref == debug_preference) {
			if (sharedPreferences.getBoolean("debug_preference", true)) {
				MySon.setDebugEnabled(true);
			} else {
				MySon.setDebugEnabled(false);
			}
		}
		if (pref == report_preference) {
			Intent intent = new Intent(SettingsActivity.this, ReportActivity.class);
			startActivity(intent);
		}
		return false;
	}

	private void showFileChooser() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("*/*");
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		try {
			startActivityForResult(Intent.createChooser(intent, "Select an image as a background"), FILE_SELECT_CODE);
		} catch (android.content.ActivityNotFoundException ex) {
			// Potentially direct the user to the Market with a Dialog
			Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case FILE_SELECT_CODE:
				if (resultCode == RESULT_OK) {
					// Get the Uri of the selected file
					Uri uri = data.getData();
					Log.d("FILE CHOOSER", "File Uri: " + uri.toString());
					// Get the path
					try {
						String path = getPath(this, uri);
						Settings.saveCustomBackgroundPath(path);
						Log.d("FILE CHOOSER", "File Path: " + path);
					} catch (URISyntaxException e) {
						e.printStackTrace();
						Log.d("FILE CHOOSER", "URI SYNTAX EXCEPTION");
					}
					// Get the file instance
					// File file = new File(path);
					// Initiate the upload
				}
				break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	public static String getPath(Context context, Uri uri) throws URISyntaxException {
		if ("content".equalsIgnoreCase(uri.getScheme())) {
			String[] projection = { "_data" };
			Cursor cursor = null;

			try {
				cursor = context.getContentResolver().query(uri, projection, null, null, null);
				int column_index = cursor.getColumnIndexOrThrow("_data");
				if (cursor.moveToFirst()) {
					return cursor.getString(column_index);
				}
			} catch (Exception e) {
			}
		}

		else if ("file".equalsIgnoreCase(uri.getScheme())) {
			return uri.getPath();
		}
		return null;
	}

	@Override
	public void onPause() {
		Toast.makeText(SettingsActivity.this, "Settings saved", Toast.LENGTH_SHORT).show();
		super.onPause();
	}
	/*
	 * @Override public boolean onKeyDown(int keyCode, KeyEvent event) { if (keyCode == KeyEvent.KEYCODE_BACK) { Intent
	 * intent = new Intent(this, what.home.HomeActivity.class); startActivityForResult(intent, 0); return true; } return
	 * super.onKeyDown(keyCode, event); }
	 */

}