package kr.ac.kaist.lockscreen;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;

public class SignInActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        init();
    }

    // region Variables
    private EditText userEmail;
    private EditText userPassword;

    static SharedPreferences loginPrefs = null;
    static final String email = "email", password = "password";
    // endregion

    private void init() {
        // region Initialize UI Variables
        userEmail = findViewById(R.id.txt_email);
        userPassword = findViewById(R.id.txt_password);
        // endregion

        if (loginPrefs == null)
            loginPrefs = getSharedPreferences("UserLogin", 0);

        if (loginPrefs.contains(SignInActivity.email) && loginPrefs.contains(SignInActivity.password)) {
            signIn(loginPrefs.getString(SignInActivity.email, null), loginPrefs.getString(SignInActivity.password, null));
        } else Toast.makeText(this, "No log in yet", Toast.LENGTH_SHORT).show();
    }

    public void signInClick(View view) {
        signIn(userEmail.getText().toString(), userPassword.getText().toString());
    }

    public void signUpClick(View view) {
        Intent intent = new Intent(this, SignUpActivity.class);
        startActivity(intent);
    }

    public void signIn(String email, String password) {

        if (Tools.isNetworkAvailable(this))
            Tools.execute(new MyRunnable(
                    this,
                    getString(R.string.url_server, getString(R.string.server_ip)),
                    email,
                    password
            ) {
                @Override
                public void run() {
                    String url = (String) args[0];
                    String email = (String) args[1];
                    String password = (String) args[2];

                    PHPRequest request;
                    try {
                        request = new PHPRequest(url);
                        String result = request.PhPtest(PHPRequest.SERV_CODE_SIGN_IN, email, password, null, null, null, null, null, null, null, null);

                        switch (result) {
                            case Tools.RES_OK:
                                runOnUiThread(new MyRunnable(activity, args) {
                                    @Override
                                    public void run() {
                                        String email = (String) args[1];
                                        String password = (String) args[2];

                                        SharedPreferences.Editor editor = SignInActivity.loginPrefs.edit();
                                        editor.putString(SignInActivity.email, email);
                                        editor.apply();
                                        editor.apply();
                                        editor.putString(SignInActivity.password, password);
                                        editor.apply();

                                        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }
                                });
                                break;
                            case Tools.RES_FAIL:
                                Thread.sleep(2000);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(SignInActivity.this, "Failed to sign in.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                break;
                            case Tools.RES_SRV_ERR:
                                Thread.sleep(2000);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(SignInActivity.this, "Failed to sign in. (SERVER SIDE ERROR)", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                break;
                            default:
                                break;
                        }
                    } catch (InterruptedException | IOException e) {
                        e.printStackTrace();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(SignInActivity.this, "Failed to sign in.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    enableTouch();
                }
            });
        else if (loginPrefs.getString(SignInActivity.email, null) != null && loginPrefs.getString(SignInActivity.password, null) != null) {
            Intent intent = new Intent(SignInActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Internet is not available", Toast.LENGTH_SHORT).show();
        }
    }
}
