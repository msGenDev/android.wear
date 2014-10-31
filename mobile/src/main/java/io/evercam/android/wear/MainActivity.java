package io.evercam.android.wear;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;

public class MainActivity extends ActionBarActivity
{

    private SharedPreferences sharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        if(PrefsManager.isUserLogged(sharedPrefs))
        {
            startActivity(new Intent(MainActivity.this, MobileActivity.class));
        }
        else
        {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        }
    }

    @Override
    protected void onStop ()
    {
        super.onStop();
        finish();
    }
}
