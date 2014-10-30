package io.evercam.android.wear;

import android.content.SharedPreferences;

import io.evercam.EvercamException;
import io.evercam.User;

public class PrefsManager
{

	public final static String KEY_USER_NAME = "userName";
	public final static String KEY_EMAIL = "email";
	public final static String KEY_FORENAME = "forename";
	public final static String KEY_LASTNAME = "lastname";
	public final static String KEY_COUNTRY = "country";
	public final static String KEY_USER_API_KEY = "userApiKey";
	public final static String KEY_USER_API_ID = "userApiId";

	public static String getUsername(SharedPreferences sharedPrefs)
	{
		return sharedPrefs.getString(KEY_USER_NAME, null);
	}


	public static void saveAccountDetails(SharedPreferences sharedPrefs, User user) throws EvercamException
	{
		if (user != null)
		{
			SharedPreferences.Editor editor = sharedPrefs.edit();
			editor.putString(KEY_USER_NAME, user.getId());
			editor.putString(KEY_COUNTRY, user.getCountry());
			editor.putString(KEY_EMAIL, user.getEmail());
			editor.putString(KEY_FORENAME, user.getFirstName());
			editor.putString(KEY_LASTNAME, user.getLastName());
			editor.commit();
		}
	}

	public static void saveEvercamUserKeyPair(SharedPreferences sharedPrefs, String apiKey,
			String apiId)
	{
		SharedPreferences.Editor editor = sharedPrefs.edit();
		editor.putString(KEY_USER_API_KEY, apiKey);
		editor.putString(KEY_USER_API_ID, apiId);
		editor.commit();
	}

	public static String getUserApiKey(SharedPreferences sharedPrefs)
	{
		return sharedPrefs.getString(KEY_USER_API_KEY, null);
	}

	public static String getUserApiId(SharedPreferences sharedPrefs)
	{
		return sharedPrefs.getString(KEY_USER_API_ID, null);
	}

	public static void clearAccountDetails(SharedPreferences sharedPrefs)
	{
		SharedPreferences.Editor editor = sharedPrefs.edit();
		editor.putString(KEY_USER_NAME, null);
		editor.putString(KEY_COUNTRY, null);
		editor.putString(KEY_EMAIL, null);
		editor.putString(KEY_FORENAME, null);
		editor.putString(KEY_LASTNAME, null);
		editor.commit();
	}

    public static boolean isUserLogged(SharedPreferences sharedPrefs)
    {
        String savedUsername = sharedPrefs.getString(KEY_USER_NAME, null);
        if (savedUsername != null)
        {
            return true;
        }
        return false;
    }
}
