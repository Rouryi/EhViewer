/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.ui.scene;

import android.content.Context;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.UrlOpener;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhRequest;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.parser.ProfileParser;
import com.hippo.ehviewer.ui.MainActivity;
import com.hippo.rippleold.RippleSalon;
import com.hippo.scene.Announcer;
import com.hippo.scene.SceneFragment;
import com.hippo.scene.StageActivity;
import com.hippo.util.ActivityHelper;
import com.hippo.util.ExceptionUtils;
import com.hippo.yorozuya.AssertUtils;
import com.hippo.yorozuya.ViewUtils;

public final class SignInScene extends BaseScene implements EditText.OnEditorActionListener,
        View.OnClickListener {

    private static final String KEY_REQUEST_ID = "request_id";

    private static final int REQUEST_CODE_WEBVIEW = 0;

    /*---------------
     View life cycle
     ---------------*/
    @Nullable
    private View mProgress;
    @Nullable
    private TextInputLayout mUsernameLayout;
    @Nullable
    private TextInputLayout mPasswordLayout;
    @Nullable
    private EditText mUsername;
    @Nullable
    private EditText mPassword;
    @Nullable
    private View mRegister;
    @Nullable
    private View mSignIn;
    @Nullable
    private TextView mSignInViaWebView;
    @Nullable
    private TextView mSignInViaCookies;
    @Nullable
    private TextView mSkipSigningIn;

    private int mRequestId;

    @Override
    public boolean needShowLeftDrawer() {
        return false;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            onInit();
        } else {
            onRestore(savedInstanceState);
        }
    }

    private void onInit() {
    }

    private void onRestore(Bundle savedInstanceState) {
        mRequestId = savedInstanceState.getInt(KEY_REQUEST_ID);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_REQUEST_ID, mRequestId);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scene_login, container, false);

        View loginForm = ViewUtils.$$(view, R.id.login_form);
        mProgress = ViewUtils.$$(view, R.id.progress);
        mUsernameLayout = (TextInputLayout) ViewUtils.$$(loginForm, R.id.username_layout);
        mUsername = mUsernameLayout.getEditText();
        AssertUtils.assertNotNull(mUsername);
        mPasswordLayout = (TextInputLayout) ViewUtils.$$(loginForm, R.id.password_layout);
        mPassword = mPasswordLayout.getEditText();
        AssertUtils.assertNotNull(mPassword);
        mRegister = ViewUtils.$$(loginForm, R.id.register);
        mSignIn = ViewUtils.$$(loginForm, R.id.sign_in);
        mSignInViaWebView = (TextView) ViewUtils.$$(loginForm, R.id.sign_in_via_webview);
        mSignInViaCookies = (TextView) ViewUtils.$$(loginForm, R.id.sign_in_via_cookies);
        mSkipSigningIn = (TextView) ViewUtils.$$(loginForm, R.id.skip_signing_in);

        mSignInViaWebView.setPaintFlags(mSignInViaWebView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        mSignInViaCookies.setPaintFlags(mSignInViaCookies.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        mSkipSigningIn.setPaintFlags(mSignInViaCookies.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        mPassword.setOnEditorActionListener(this);

        mRegister.setOnClickListener(this);
        mSignIn.setOnClickListener(this);
        mSignInViaWebView.setOnClickListener(this);
        mSignInViaCookies.setOnClickListener(this);
        mSkipSigningIn.setOnClickListener(this);

        RippleSalon.addRipple(mRegister, true);
        RippleSalon.addRipple(mSignIn, true);

        EhApplication application = (EhApplication) getContext().getApplicationContext();
        if (application.containGlobalStuff(mRequestId)) {
            // request exist
            showProgress(false);
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Show IME
        if (mProgress != null && View.INVISIBLE != mProgress.getVisibility()) {
            ActivityHelper.showSoftInput(getActivity(), mUsername);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mProgress = null;
        mUsernameLayout = null;
        mPasswordLayout = null;
        mUsername = null;
        mPassword = null;
        mRegister = null;
        mSignIn = null;
        mSignInViaWebView = null;
        mSignInViaCookies = null;
        mSkipSigningIn = null;
    }

    private void showProgress(boolean animation) {
        if (null != mProgress && View.VISIBLE != mProgress.getVisibility()) {
            if (animation) {
                mProgress.setAlpha(0.0f);
                mProgress.setVisibility(View.VISIBLE);
                mProgress.animate().alpha(1.0f).setDuration(500).start();
            } else {
                mProgress.setAlpha(1.0f);
                mProgress.setVisibility(View.VISIBLE);
            }
        }
    }

    private void hideProgress() {
        if (null != mProgress) {
            mProgress.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onSceneResult(int requestCode, int resultCode, Bundle data) {
        if (REQUEST_CODE_WEBVIEW == requestCode) {
            if (RESULT_OK == resultCode) {
                getProfile();
            }
        } else {
            super.onSceneResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onClick(View v) {
        if (mRegister == v) {
            UrlOpener.openUrl(getActivity(), EhUrl.URL_REGISTER, false, true);
        } else if (mSignIn == v) {
            signIn();
        } else if (mSignInViaWebView == v) {
            startScene(new Announcer(WebViewLoginScene.class).setRequestCode(this, REQUEST_CODE_WEBVIEW));
        } else if (mSignInViaCookies == v) {

        } else if (mSkipSigningIn == v) {
            redirectTo();
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (v == mPassword) {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                signIn();
                return true;
            }
        }

        return false;
    }

    private void signIn() {
        if (null == mUsername || null == mPassword || null == mUsernameLayout ||
                null == mPasswordLayout) {
            return;
        }

        String username = mUsername.getText().toString();
        String password = mPassword.getText().toString();

        if (username.isEmpty()) {
            mUsernameLayout.setError(getString(R.string.error_username_cannot_empty));
            return;
        } else {
            mUsernameLayout.setError(null);
        }

        if (password.isEmpty()) {
            mPasswordLayout.setError(getString(R.string.error_password_cannot_empty));
            return;
        } else {
            mPasswordLayout.setError(null);
        }

        ActivityHelper.hideSoftInput(getActivity());
        showProgress(true);

        // Clean up for sign in
        EhApplication.getEhCookieStore(getContext()).cleanUpForSignIn();

        EhCallback callback = new SignInListener(getContext(),
                ((StageActivity) getActivity()).getStageId(), getTag());
        mRequestId = ((EhApplication) getContext().getApplicationContext()).putGlobalStuff(callback);
        EhRequest request = new EhRequest()
                .setMethod(EhClient.METHOD_SIGN_IN)
                .setArgs(username, password)
                .setCallback(callback);
        EhApplication.getEhClient(getContext()).execute(request);
    }

    private void getProfile() {
        ActivityHelper.hideSoftInput(getActivity());
        showProgress(true);

        EhCallback callback = new GetProfileListener(getContext(),
                ((StageActivity) getActivity()).getStageId(), getTag());
        mRequestId = ((EhApplication) getContext().getApplicationContext()).putGlobalStuff(callback);
        EhRequest request = new EhRequest()
                .setMethod(EhClient.METHOD_GET_PROFILE)
                .setCallback(callback);
        EhApplication.getEhClient(getContext()).execute(request);
    }

    private void redirectTo() {
        ((MainActivity) getActivity()).startSceneForCheckStep(
                MainActivity.CHECK_STEP_SIGN_IN, getArguments());
        finish();
    }

    private void whetherToSkip() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.skip_signing_in)
                .setMessage(R.string.skip_signing_in_plain)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.get_it, null)
                .show();
    }

    public void onSignInSuccess() {
        if (EhApplication.getEhCookieStore(getContext()).hasSignedIn()) {
            getProfile();
        } else {
            hideProgress();
            whetherToSkip();
        }
    }

    public void onSignInFailure(Exception e) {
        Toast.makeText(getContext(), ExceptionUtils.getReadableString(getContext(), e),
                Toast.LENGTH_SHORT).show();
        hideProgress();
        whetherToSkip();
    }

    public void onGetProfileSuccess() {
        redirectTo();
    }

    public void onGetProfileFailure(Exception e) {
        Toast.makeText(getContext(), ExceptionUtils.getReadableString(getContext(), e),
                Toast.LENGTH_SHORT).show();
        hideProgress();
        whetherToSkip();
    }

    private class SignInListener extends EhCallback<SignInScene, String> {

        public SignInListener(Context context, int stageId, String sceneTag) {
            super(context, stageId, sceneTag);
        }

        @Override
        public void onSuccess(String result) {
            getApplication().removeGlobalStuff(this);
            Settings.putDisplayName(result);

            SignInScene scene = getScene();
            if (scene != null) {
                scene.onSignInSuccess();
            }
        }

        @Override
        public void onFailure(Exception e) {
            getApplication().removeGlobalStuff(this);
            e.printStackTrace();

            SignInScene scene = getScene();
            if (scene != null) {
                scene.onSignInFailure(e);
            }
        }

        @Override
        public void onCancel() {
            getApplication().removeGlobalStuff(this);
        }

        @Override
        public boolean isInstance(SceneFragment scene) {
            return scene instanceof SignInScene;
        }
    }

    private class GetProfileListener extends EhCallback<SignInScene, ProfileParser.Result> {

        public GetProfileListener(Context context, int stageId, String sceneTag) {
            super(context, stageId, sceneTag);
        }

        @Override
        public void onSuccess(ProfileParser.Result result) {
            getApplication().removeGlobalStuff(this);
            Settings.putDisplayName(result.displayName);
            Settings.putAvatar(result.avatar);

            SignInScene scene = getScene();
            if (scene != null) {
                scene.onGetProfileSuccess();
            }
        }

        @Override
        public void onFailure(Exception e) {
            getApplication().removeGlobalStuff(this);
            e.printStackTrace();

            SignInScene scene = getScene();
            if (scene != null) {
                scene.onGetProfileFailure(e);
            }
        }

        @Override
        public void onCancel() {
            getApplication().removeGlobalStuff(this);
        }

        @Override
        public boolean isInstance(SceneFragment scene) {
            return scene instanceof SignInScene;
        }
    }
}
