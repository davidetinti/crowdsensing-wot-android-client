package com.example.crowdsensingwotandroidapp.auth;

import android.util.Patterns;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AuthViewModel extends ViewModel {

	private final MutableLiveData<Boolean> emailError = new MutableLiveData<>(false);
	private final MutableLiveData<Boolean> passwordError = new MutableLiveData<>(false);

	private final MutableLiveData<Boolean> isLogin = new MutableLiveData<>(false);

	public MutableLiveData<Boolean> getEmailError() {
		return emailError;
	}

	public MutableLiveData<Boolean> getPasswordError() {
		return passwordError;
	}

	public MutableLiveData<Boolean> getIsLogin(){
		return isLogin;
	}

	public void dataChanged() {
		emailError.setValue(false);
		passwordError.setValue(false);
	}

	public boolean validateData(String email, String password) {
		boolean emailValidation = isEmailValid(email);
		boolean passwordValidation = isPasswordValid(password);
		emailError.setValue(!emailValidation);
		passwordError.setValue(!passwordValidation);
		return emailValidation && passwordValidation;
	}

	// A placeholder email validation check
	private boolean isEmailValid(String email) {
		if (email == null) {
			return false;
		}
		return Patterns.EMAIL_ADDRESS.matcher(email).matches();
	}

	// A placeholder password validation check
	private boolean isPasswordValid(String password) {
		return password != null && password.trim().length() > 5;
	}
}