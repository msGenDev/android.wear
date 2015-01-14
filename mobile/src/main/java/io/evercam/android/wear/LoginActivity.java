package io.evercam.android.wear;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import io.evercam.API;
import io.evercam.ApiKeyPair;
import io.evercam.EvercamException;
import io.evercam.User;

public class LoginActivity extends Activity
{
    private final String TAG = "evercamwear-LoginActivity";
    private UserLoginTask userLoginTask = null;

    private String username;
    private String password;
    private SharedPreferences sharedPrefs;

    private EditText usernameView;
    private EditText passwordView;
    private View loginFormView;
    private View loginStatusView;
    private TextView loginStatusMessageView;
    private TextView signUpLink;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_login);
        setUnderLine();

        /**
         * FIXME: Developer key and id has been removed from Evercam, I am keeping it here only
         * because it hasn't been updated in Evercam Java wrapper
         **/
        API.setDeveloperKeyPair("8f0edbf9b3c69cc38dad662d2aa82d33", "16486214");

        usernameView = (EditText) findViewById(R.id.email);
        passwordView = (EditText) findViewById(R.id.password);
        passwordView.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent)
            {
                if(id == R.id.login || id == EditorInfo.IME_NULL)
                {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        loginFormView = findViewById(R.id.login_form);
        loginStatusView = findViewById(R.id.login_status);
        loginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

        findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                attemptLogin();
            }
        });

        signUpLink.setVisibility(View.GONE);
        //TODO: Sign up
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
    }

    public void attemptLogin()
    {
        if(userLoginTask != null)
        {
            return;
        }

        usernameView.setError(null);
        passwordView.setError(null);

        username = usernameView.getText().toString();
        password = passwordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if(TextUtils.isEmpty(password))
        {
            passwordView.setError(getString(R.string.error_password_required));
            focusView = passwordView;
            cancel = true;
        }
        else if(password.contains(" "))
        {
            passwordView.setError(getString(R.string.error_invalid_password));
            focusView = passwordView;
            cancel = true;
        }

        if(TextUtils.isEmpty(username))
        {
            usernameView.setError(getString(R.string.error_username_required));
            focusView = usernameView;
            cancel = true;
        }
        else if(username.contains(" "))
        {
            usernameView.setError(getString(R.string.error_invalid_username));
            focusView = usernameView;
            cancel = true;
        }

        if(cancel)
        {
            focusView.requestFocus();
        }
        else
        {
            loginStatusMessageView.setText(R.string.login_progress_signing_in);
            showProgress(true);
            userLoginTask = new UserLoginTask();
            userLoginTask.execute((Void) null);
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show)
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2)
        {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            loginStatusView.setVisibility(View.VISIBLE);
            loginStatusView.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener
                    (new AnimatorListenerAdapter()
            {
                @Override
                public void onAnimationEnd(Animator animation)
                {
                    loginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });

            loginFormView.setVisibility(View.VISIBLE);
            loginFormView.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener
                    (new AnimatorListenerAdapter()
            {
                @Override
                public void onAnimationEnd(Animator animation)
                {
                    loginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });
        }
        else
        {
            loginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
            loginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    public class UserLoginTask extends AsyncTask<Void, Void, Boolean>
    {
        String error = "Error";

        @Override
        protected Boolean doInBackground(Void... params)
        {
            try
            {
                ApiKeyPair userKeyPair = API.requestUserKeyPairFromEvercam(username, password);
                String userApiKey = userKeyPair.getApiKey();
                String userApiId = userKeyPair.getApiId();
                PrefsManager.saveEvercamUserKeyPair(sharedPrefs, userApiKey, userApiId);
                API.setUserKeyPair(userApiKey, userApiId);
                User evercamUser = new User(username);
                PrefsManager.saveAccountDetails(sharedPrefs, evercamUser);
                return true;
            }
            catch(EvercamException e)
            {
                error = e.getMessage();
                Log.e(TAG, error);
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success)
        {
            userLoginTask = null;
            showProgress(false);

            if(success)
            {
                startCapture();
            }
            else
            {
                Toast toast = Toast.makeText(getApplicationContext(), error, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                passwordView.setText(null);
            }
        }

        @Override
        protected void onCancelled()
        {
            userLoginTask = null;
            showProgress(false);
        }
    }

    private void startCapture()
    {
        Intent intentMain = new Intent();
        intentMain.setClass(LoginActivity.this, MainActivity.class);
        startActivity(intentMain);
    }

    @Override
    public void onBackPressed()
    {

    }

    private void setUnderLine()
    {
        signUpLink = (TextView) findViewById(R.id.signupLink);
        SpannableString spanString = new SpannableString(this.getResources().getString(R.string
                .create_account));
        spanString.setSpan(new UnderlineSpan(), 0, spanString.length(), 0);
        signUpLink.setText(spanString);
    }
}
