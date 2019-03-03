package kr.ac.kaist.lockscreen;

import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class SignUpActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        init();
    }

    // region Variables
    private EditText email;
    private EditText password;
    private EditText confPassword;
    // endregion

    private void init() {
        // region Initialize UI Variables
        email = findViewById(R.id.txt_email);
        password = findViewById(R.id.txt_password);
        confPassword = findViewById(R.id.txt_conf_password);
        // endregion
    }

    public void userRegister(String email, String password) {
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

                PHPRequest request = null;
                try {
                    request = new PHPRequest(url);
                    String result = request.PhPtest(PHPRequest.SERV_CODE_SIGN_UP, email, password, null, null, null, null, null, null, null, null);

                    switch (result) {
                        case Tools.RES_OK:
                            runOnUiThread(new MyRunnable(activity, args) {
                                @Override
                                public void run() {
                                    Toast.makeText(SignUpActivity.this, "Successfully signed up. You can sign in now!", Toast.LENGTH_SHORT).show();
                                    onBackPressed();
                                }
                            });
                            break;
                        case Tools.RES_FAIL:
                            Thread.sleep(2000);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(SignUpActivity.this, "Email already exists, please try another email!", Toast.LENGTH_SHORT).show();
                                }
                            });
                            break;
                        case Tools.RES_SRV_ERR:
                            Thread.sleep(2000);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(SignUpActivity.this, "Failed to sign up. (SERVER SIDE ERROR)", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(SignUpActivity.this, "Failed to sign up.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                enableTouch();
            }
        });
    }

    public void registerClick(View view) {
        String usrEmail = email.getText().toString();
        String usrPassword = password.getText().toString();
        String usrConfirmPass = confPassword.getText().toString();

        if (isRegistrationValid(usrEmail, usrPassword, usrConfirmPass))
            userRegister(usrEmail, usrPassword);
        else
            Toast.makeText(this, "Invalid input. Please recheck inputs and try again!", Toast.LENGTH_SHORT).show();
    }

    public boolean isRegistrationValid(String email, String password, String confirmPass) {
        return email != null &&
                password != null &&
                password.length() >= 6 &&
                password.length() <= 16 &&
                password.equals(confirmPass);
    }
}
