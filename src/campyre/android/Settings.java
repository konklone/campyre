package campyre.android;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Settings extends PreferenceActivity {
	
	public static final String KEY_NUMBER_MESSAGES = "number_messages";
	public static final int DEFAULT_NUMBER_MESSAGES = 80;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_titled);
		
		Utils.setTitle(this, R.string.menu_settings);
		
		addPreferencesFromResource(R.xml.settings);
		PreferenceManager.setDefaultValues(this, R.xml.settings, false);
	}
}
