package com.alorma.gitskariostest;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.alorma.github.sdk.bean.dto.request.CreateAuthorization;
import com.alorma.github.sdk.bean.dto.response.GithubAuthorization;
import com.alorma.github.sdk.bean.dto.response.User;
import com.alorma.github.sdk.services.login.CreateAuthorizationClient;
import com.alorma.github.sdk.services.user.GetAuthUserClient;
import com.alorma.github.sdk.services.user.TwoFactorAuthException;
import com.alorma.github.sdk.services.user.UnauthorizedException;
import com.alorma.gitskarios.core.client.TokenProvider;
import com.alorma.gitskarios.core.client.TokenProviderInterface;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

  private EditText userText;
  private EditText passText;
  private EditText otpText;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    userText = (EditText) findViewById(R.id.user);
    passText = (EditText) findViewById(R.id.passwd);
    otpText = (EditText) findViewById(R.id.otpCode);

    findViewById(R.id.login).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        String user = userText.getText().toString();
        String password = passText.getText().toString();
        String otp = otpText.getText().toString();

        if (TextUtils.isEmpty(otp)) {
          loginSimple(user, password);
        } else {
          manageTwoFactor();
        }
      }
    });
  }

  private void loginSimple(String user, String password) {
    new GetAuthUserClient(user, password).observable()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.newThread())
        .subscribe(new Action1<User>() {
          @Override
          public void call(User user) {
            Toast.makeText(MainActivity.this, "user: " + user.toString(), Toast.LENGTH_SHORT)
                .show();
          }
        }, new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {
            if (throwable instanceof UnauthorizedException) {
              Toast.makeText(MainActivity.this, "incorrect credentials", Toast.LENGTH_SHORT).show();
            } else if (throwable instanceof TwoFactorAuthException) {
              manageTwoFactor();
            } else {
              Toast.makeText(MainActivity.this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
            }
          }
        });
  }

  private void manageTwoFactor() {
    String user = userText.getText().toString();
    String password = passText.getText().toString();
    String otp = otpText.getText().toString();

    CreateAuthorization createAuth = new CreateAuthorization();
    createAuth.scopes = new String[] { "repo", "user" };
    createAuth.note = "TestAppGitskariosSdk " + System.currentTimeMillis();
    CreateAuthorizationClient client = new CreateAuthorizationClient(user, password, createAuth);

    if (!TextUtils.isEmpty(otp)) {
      client.setOtpCode(otp);
    }

    client.observable()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.newThread())
        .subscribe(new Action1<GithubAuthorization>() {
          @Override
          public void call(GithubAuthorization githubAuthorization) {
            Toast.makeText(MainActivity.this, "Authorization: " + githubAuthorization.toString(),
                Toast.LENGTH_SHORT).show();

            onToken(githubAuthorization.token);
          }
        }, new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {
            if (throwable instanceof TwoFactorAuthException) {
              otpText.setEnabled(true);

              Toast.makeText(MainActivity.this, "provide otp code sent by sms", Toast.LENGTH_SHORT).show();
            }
          }
        });
  }

  private void onToken(final String token) {
    TokenProvider.setTokenProviderInstance(new TokenProviderInterface() {
      @Override
      public String getToken() {
        return token;
      }
    });

    new GetAuthUserClient(token).observable()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.newThread())
        .subscribe(new Action1<User>() {
          @Override
          public void call(User user) {
            Toast.makeText(MainActivity.this, "user: " + user.toString(), Toast.LENGTH_SHORT)
                .show();
          }
        }, new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {

          }
        });
  }
}
