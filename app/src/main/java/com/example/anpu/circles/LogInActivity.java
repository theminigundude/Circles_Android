package com.example.anpu.circles;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.transition.Explode;
import android.transition.Fade;
import android.transition.Slide;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.anpu.circles.model.UserData;
import com.example.anpu.circles.utilities.JellyInterpolator;
import com.example.anpu.circles.utilities.MD5Util;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import com.example.anpu.circles.model.User;
import com.example.anpu.circles.model.UserResponseStatus;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LogInActivity extends AppCompatActivity {

    @BindView(R.id.main_btn_login) TextView mBtnLogin;
    @BindView(R.id.layout_progress_login) View progress;
    @BindView(R.id.input_layout_login) View mInputLayout;
    @BindView(R.id.login_layout_email) LinearLayout mEmail;
    @BindView(R.id.login_layout_pwd) LinearLayout mPwd;
    @BindView(R.id.login_avatar) ImageView loginAvatar;

    private float mWidth, mHeight;

    private ActionBar bar;

    private String email;
    private String nickname;
    private String pwd;
    private String urlLogin = "http://steins.xin:8001/auth/login";
    private String urlForget = "http://steins.xin:8001/auth/forgetpwd";

    SharedPreferences sprefLogin;
    SharedPreferences.Editor editorLogin;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // new
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);  // bind emailEditText and pwdEditText

        // hide the bar at the top
//        getSupportActionBar().hide();
        bar = getSupportActionBar();
        if (bar != null) {
            bar.hide();
        }

        Fade fade = new Fade();
        fade.setDuration(1000);

        Explode explode = new Explode();
        explode.setDuration(1000);

        Slide slide = new Slide();
        slide.setDuration(1000);

//        getWindow().setEnterTransition(fade);
        getWindow().setExitTransition(slide);

        // set sharedpreference
        sprefLogin = PreferenceManager.getDefaultSharedPreferences(this);
        editorLogin = sprefLogin.edit();
        editorLogin.apply();


        Glide.with(getApplicationContext()).load(R.drawable.paw_code)
                .into(loginAvatar);
    }

    @OnTextChanged(R.id.edit_email_login)
    void nameChanged(CharSequence s, int start, int before, int count) {
        email = s.toString();
    }

    @OnTextChanged(R.id.edit_pwd_login)
    void pwdChanged(CharSequence s, int start, int before, int count) {
        pwd = s.toString();
    }

    @OnClick(R.id.main_btn_login)
    void loginClicked() {

        if (email == null || email.equals("")) {
            Toast.makeText(this, "Email is empty", Toast.LENGTH_SHORT).show();
        }
        else if (pwd == null || pwd.equals("")) {
            Toast.makeText(this, "Password is empty", Toast.LENGTH_SHORT).show();
        }
        else {
            // Using regex to check if it's nyu email
            checkEmail();
        }


    }

    @OnClick(R.id.signup_textview)
    void signupTextClicked() {
        Intent intent = new Intent(LogInActivity.this, SignUpActivity.class);
        intent.putExtra("transition", "slide");
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
    }


    @OnClick(R.id.forgetpwd_textview)
    void forgetpwdTextClicked() {
        if (email != null && !email.isEmpty()) {
            final Gson gsonForget = new GsonBuilder().enableComplexMapKeySerialization().create();
            HashMap<String, String> forget = new HashMap<>();
            forget.put("email", email);


            OkHttpClient client = new OkHttpClient();
            RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8")
                    , gsonForget.toJson(forget));
            final Request request = new Request.Builder()
                    .post(body)
                    .url(urlForget)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(LogInActivity.this, "Failure to connect to the server", Toast.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final Gson resp = new GsonBuilder().enableComplexMapKeySerialization().create();
                    Type type = new TypeToken<HashMap<String, String>>() {}.getType();
                    HashMap<String, String> resHash = resp.fromJson(response.body().string(), type);
                    if (resHash.get("status").equals("1")) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(LogInActivity.this, "An email has sent to reset the password.", Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(LogInActivity.this, "Please check your email it doesn't seem to be right.", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            });
        }
    }

    private void checkEmail() {
        String pattern = "@nyu.edu";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(email);
        if (! m.find()) {
            Toast.makeText(this, "Email should end with @nyu.edu", Toast.LENGTH_LONG).show();
        }
        else {
            mWidth = mBtnLogin.getMeasuredWidth();
            mHeight = mBtnLogin.getMeasuredHeight();

            mEmail.setVisibility(View.INVISIBLE);
            mPwd.setVisibility(View.INVISIBLE);

            inputAnimator(mInputLayout, mWidth, mHeight);
        }
    }

    private void inputAnimator(final View view, float w, float h) {

        AnimatorSet set = new AnimatorSet();

        ValueAnimator animator = ValueAnimator.ofFloat(0, w);
        animator.addUpdateListener(new AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (Float) animation.getAnimatedValue();
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
                params.leftMargin = (int) value;
                params.rightMargin = (int) value;
                view.setLayoutParams(params);
            }
        });

        ObjectAnimator animator2 = ObjectAnimator.ofFloat(mInputLayout, "scaleX", 1f, 0.5f);
        set.setDuration(500);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.playTogether(animator, animator2);
        set.start();
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {

                progress.setVisibility(View.VISIBLE);
                progressAnimator(progress);
                mInputLayout.setVisibility(View.INVISIBLE);

            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
    }

    private void progressAnimator(final View view) {
        PropertyValuesHolder animator = PropertyValuesHolder.ofFloat("scaleX", 0.5f, 1f);
        PropertyValuesHolder animator2 = PropertyValuesHolder.ofFloat("scaleY", 0.5f, 1f);
        ObjectAnimator animator3 = ObjectAnimator.ofPropertyValuesHolder(view,
                animator, animator2);
        animator3.setDuration(1000);
        animator3.setInterpolator(new JellyInterpolator());
//        animator3.setInterpolator(new AccelerateDecelerateInterpolator());
        animator3.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                // check if the account is valid
                // generate json
                validLogin();

            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        animator3.start();
    }

    private void recovery() {
        progress.setVisibility(View.GONE);
        mInputLayout.setVisibility(View.VISIBLE);
        mEmail.setVisibility(View.VISIBLE);
        mPwd.setVisibility(View.VISIBLE);

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mInputLayout.getLayoutParams();
        params.leftMargin = 0;
        params.rightMargin = 0;
        mInputLayout.setLayoutParams(params);

        ObjectAnimator animator2 = ObjectAnimator.ofFloat(mInputLayout, "scaleX", 0.5f, 1f);
        animator2.setDuration(500);
        animator2.setInterpolator(new AccelerateDecelerateInterpolator());
        animator2.start();
    }

    private void validLogin() {
        final Gson gson = new Gson();
        User user = new User(email, MD5Util.getMD5Str(pwd));
        String jsonUser = gson.toJson(user);
        // post to the server
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonUser);
        Request request = new Request.Builder()
                .post(body)
                .url(urlLogin)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(LogInActivity.this, "Failure to connect to the server", Toast.LENGTH_LONG).show();
                        recovery();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
//                String ans = response.body().string();
//                Log.d("Test", ans);
//                System.out.print(ans);
                UserResponseStatus userResponseStatus = gson.fromJson(response.body().string(), UserResponseStatus.class);
                // failure
                if (userResponseStatus.getStatus() == 0) {
                    if (userResponseStatus.getType() == 0) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(LogInActivity.this, "Account is not activated.", Toast.LENGTH_LONG).show();
                                recovery();
                            }
                        });
                    }
                    else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(LogInActivity.this, "Account does not exist.", Toast.LENGTH_LONG).show();
                                recovery();
                            }
                        });
                    }
                }
                // success
                else {
                    UserData.setEmail(MD5Util.getMD5(email));
                    UserData.setNickname(userResponseStatus.getNickname());
                    UserData.setAvatar(userResponseStatus.getAvatar());
                    UserData.setUncypheredEmail(email);

                    editorLogin.putString("email", email);
                    editorLogin.commit();

//                    Intent intent = new Intent(LogInActivity.this, HomePage1.class);
                    Intent intent = new Intent(LogInActivity.this, HomePageFragmentActivity.class);
                    startActivity(intent);
                    LogInActivity.this.finish();
                }
            }
        });
    }
}
