/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.bither.ui.base;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnShowListener;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.bither.R;
import net.bither.model.Check;
import net.bither.model.Check.CheckListener;
import net.bither.model.Check.ICheckAction;
import net.bither.model.PasswordSeed;
import net.bither.preference.AppSharedPreference;
import net.bither.ui.base.passwordkeyboard.PasswordEntryKeyboardView;
import net.bither.util.CheckUtil;
import net.bither.util.StringUtil;
import net.bither.util.UIUtil;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

public class DialogPassword extends Dialog implements OnDismissListener, TextView.OnEditorActionListener {
    public static interface DialogPasswordListener {
        public void onPasswordEntered(String password);
    }

    private View container;
    private LinearLayout llInput;
    private LinearLayout llChecking;
    private TextView tvTitle;
    private TextView tvError;
    private EditText etPassword;
    private EditText etPasswordConfirm;
    private Button btnOk;
    private Button btnCancel;
    private PasswordEntryKeyboardView kv;
    private PasswordSeed passwordSeed;
    private DialogPasswordListener listener;
    private boolean passwordEntered = false;
    private boolean checkPre = true;
    private boolean cancelable = true;
    private ExecutorService executor;

    public DialogPassword(Context context, DialogPasswordListener listener) {
        super(context, R.style.password_dialog);
        setContentView(R.layout.dialog_password);
        this.listener = listener;
        setOnDismissListener(this);
        passwordSeed = getPasswordSeed();
        initView();
    }

    private void initView() {
        container = findViewById(R.id.fl_container);
        llInput = (LinearLayout) findViewById(R.id.ll_input);
        llChecking = (LinearLayout) findViewById(R.id.ll_checking);
        tvTitle = (TextView) findViewById(R.id.tv_title);
        tvError = (TextView) findViewById(R.id.tv_error);
        etPassword = (EditText) findViewById(R.id.et_password);
        etPasswordConfirm = (EditText) findViewById(R.id.et_password_confirm);
        btnOk = (Button) findViewById(R.id.btn_ok);
        btnCancel = (Button) findViewById(R.id.btn_cancel);
        kv = (PasswordEntryKeyboardView) findViewById(R.id.kv);
        etPassword.addTextChangedListener(passwordWatcher);
        etPasswordConfirm.addTextChangedListener(passwordWatcher);
        etPassword.setOnEditorActionListener(this);
        etPasswordConfirm.setOnEditorActionListener(this);
        configureCheckPre();
        configureEditTextActionId();
        btnOk.setOnClickListener(okClick);
        btnCancel.setOnClickListener(cancelClick);
        btnOk.setEnabled(false);
        passwordCheck.setCheckListener(passwordCheckListener);
        kv.registerEditText(etPassword, etPasswordConfirm);
    }

    private PasswordSeed getPasswordSeed() {
        return AppSharedPreference.getInstance().getPasswordSeed();
    }

    private void configureCheckPre() {
        if (checkPre) {
            if (passwordSeed != null) {
                etPasswordConfirm.setVisibility(View.GONE);
            } else {
                etPasswordConfirm.setVisibility(View.VISIBLE);
            }
        } else {
            etPasswordConfirm.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (passwordEntered && listener != null) {
            listener.onPasswordEntered(etPassword.getText().toString());
        }
    }

    private void checkValid() {
        btnOk.setEnabled(false);
        String password = etPassword.getText().toString();
        if (password.length() >= 6 && password.length() <= getContext().getResources().getInteger(R.integer.password_length_max)) {
            if (etPasswordConfirm.getVisibility() == View.VISIBLE) {
                String passwordConfirm = etPasswordConfirm.getText().toString();
                if (passwordConfirm.length() >= 6 && passwordConfirm.length() <= getContext().getResources().getInteger(R.integer.password_length_max)) {
                    btnOk.setEnabled(true);
                } else {
                    btnOk.setEnabled(false);
                }
            } else {
                btnOk.setEnabled(true);
            }
        }
    }

    private void shake() {
        Animation shake = AnimationUtils.loadAnimation(getContext(), R.anim.password_wrong_warning);
        container.startAnimation(shake);
    }

    public void setCheckPre(boolean check) {
        checkPre = check;
        configureCheckPre();
    }

    public void show() {
        if (checkPre) {
            if (etPasswordConfirm.getVisibility() != View.VISIBLE) {
                setTitle(R.string.add_address_generate_address_password_label);
            } else {
                setTitle(R.string.add_address_generate_address_password_set_label);
            }
        }
        if (cancelable) {
            btnCancel.setVisibility(View.VISIBLE);
        } else {
            btnCancel.setVisibility(View.GONE);
        }
        super.show();
    }

    public void setTitle(int resource) {
        tvTitle.setText(resource);
    }

    private View.OnClickListener okClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (passwordSeed == null && !StringUtil.compareString(etPassword.getText().toString()
                    , etPasswordConfirm.getText().toString()) && checkPre) {
                tvError.setText(R.string.add_address_generate_address_password_not_same);
                tvError.setVisibility(View.VISIBLE);
                etPasswordConfirm.requestFocus();
                return;
            }
            if (passwordSeed != null && checkPre) {
                ArrayList<Check> checks = new ArrayList<Check>();
                checks.add(passwordCheck);
                executor = CheckUtil.runChecks(checks, 1);
            } else {
                passwordEntered = true;
                dismiss();
            }
        }
    };
    private CheckListener passwordCheckListener = new CheckListener() {

        @Override
        public void onCheckBegin(Check check) {
            llChecking.setVisibility(View.VISIBLE);
            llInput.setVisibility(View.INVISIBLE);
            kv.hideKeyboard();
        }

        @Override
        public void onCheckEnd(Check check, boolean success) {
            if (executor != null) {
                executor.shutdown();
                executor = null;
            }
            if (success) {
                passwordEntered = true;
                dismiss();
            } else {
                llChecking.setVisibility(View.GONE);
                llInput.setVisibility(View.VISIBLE);
                etPassword.setText("");
                checkValid();
                tvError.setText(R.string.password_wrong);
                tvError.setVisibility(View.VISIBLE);
                shake();
                kv.showKeyboard();
            }
        }
    };
    private TextWatcher passwordWatcher = new TextWatcher() {
        private String password;
        private String passwordConfirm;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            password = etPassword.getText().toString();
            passwordConfirm = etPasswordConfirm.getText().toString();
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            tvError.setVisibility(View.GONE);
            String p = etPassword.getText().toString();
            if (p.length() > 0) {
                if (!StringUtil.validPassword(p)) {
                    etPassword.setText(password);
                }
            }
            if (etPasswordConfirm.getVisibility() == View.VISIBLE) {
                String pc = etPasswordConfirm.getText().toString();
                if (pc.length() > 0) {
                    if (!StringUtil.validPassword(pc)) {
                        etPasswordConfirm.setText(passwordConfirm);
                    }
                }
            }
            checkValid();
        }
    };

    @Override
    public void setCancelable(boolean flag) {
        this.cancelable = flag;
        super.setCancelable(flag);
    }

    public void setTitle(String title) {
        tvTitle.setText(title);
    }

    private View.OnClickListener cancelClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            passwordEntered = false;
            dismiss();
        }
    };

    private Check passwordCheck = new Check("", new ICheckAction() {
        @Override
        public boolean check() {
            if (passwordSeed != null) {
                return passwordSeed.checkPassword(etPassword.getText().toString());
            } else {
                return true;
            }
        }
    });

    private void configureEditTextActionId(){
        if(etPasswordConfirm.getVisibility() == View.VISIBLE){
            etPassword.setImeActionLabel(null, EditorInfo.IME_ACTION_NEXT);
        }else{
            etPassword.setImeActionLabel(null, EditorInfo.IME_ACTION_DONE);
        }
        etPasswordConfirm.setImeActionLabel(null, EditorInfo.IME_ACTION_DONE);
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if(v == etPassword){
            if(etPasswordConfirm.getVisibility() == View.VISIBLE){
                return false;
            }else if(btnOk.isEnabled()){
                okClick.onClick(btnOk);
                return true;
            }
        }
        if(v == etPasswordConfirm && btnOk.isEnabled()){
            okClick.onClick(btnOk);
            return true;
        }
        return false;
    }
}
