package com.example.crowdsensingwotandroidapp.auth;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.crowdsensingwotandroidapp.MainViewModel;
import com.example.crowdsensingwotandroidapp.R;
import com.example.crowdsensingwotandroidapp.ServientService;
import com.example.crowdsensingwotandroidapp.databinding.FragmentAuthBinding;
import com.example.crowdsensingwotandroidapp.utils.network.NetworkStateManager;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public class AuthFragment extends Fragment {

	private AuthViewModel authViewModel;
	private FragmentAuthBinding binding;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		binding = FragmentAuthBinding.inflate(inflater, container, false);
		return binding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		// View model setup
		authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
		// Observer setup
		authViewModel.getEmailError()
				.observe(getViewLifecycleOwner(), error -> binding.authEmailLayout.setError(error ? getString(R.string.invalidEmail) : null));
		authViewModel.getPasswordError()
				.observe(getViewLifecycleOwner(), error -> binding.authPasswordLayout.setError(error ? getString(R.string.invalidPassword) : null));
		authViewModel.getIsLogin().observe(getViewLifecycleOwner(), isLogin -> {
			if (isLogin) {
				binding.authTitle.setText(R.string.authTitleLogin);
				binding.authSubtitle.setText(R.string.authSubtitleLogin);
				binding.authRememberMe.setVisibility(View.VISIBLE);
				binding.authPrimaryActionBtn.setText(R.string.loginButtonText);
				binding.authSecondaryText.setText(R.string.authSecondaryTextLogin);
				binding.authSecondaryActionBtn.setText(R.string.registerButtonText);
			} else {
				binding.authTitle.setText(R.string.authTitleRegister);
				binding.authSubtitle.setText(R.string.authSubtitleRegister);
				binding.authRememberMe.setVisibility(View.GONE);
				binding.authPrimaryActionBtn.setText(R.string.registerButtonText);
				binding.authSecondaryText.setText(R.string.authSecondaryTextRegister);
				binding.authSecondaryActionBtn.setText(R.string.loginButtonText);
			}
		});
		MainViewModel.getBearerToken().observe(getViewLifecycleOwner(), bearer -> {
			if (bearer != null) {
				NavController controller = NavHostFragment.findNavController(this);
				controller.navigate(R.id.action_authFragment_to_dashboardFragment);
			}
		});
		// Remember login
		SharedPreferences preferences = requireActivity().getSharedPreferences("loginPreferences", MODE_PRIVATE);
		if (preferences.contains("email")) {
			String email = preferences.getString("email", "");
			String password = preferences.getString("password", "");
			login(email, password, true);
		}
		// View bindings setup
		TextWatcher afterTextChangedListener = new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void afterTextChanged(Editable s) {
				authViewModel.dataChanged();
			}
		};
		binding.authEmailTextInput.addTextChangedListener(afterTextChangedListener);
		binding.authPasswordTextInput.addTextChangedListener(afterTextChangedListener);
		binding.authPasswordTextInput.setOnEditorActionListener((v, actionId, event) -> {
			if (actionId == EditorInfo.IME_ACTION_DONE)
				startAccess();
			return false;
		});
		binding.authPrimaryActionBtn.setOnClickListener(v -> startAccess());
		binding.authSecondaryActionBtn.setOnClickListener(v -> {
			authViewModel.getIsLogin()
					.setValue(Boolean.FALSE.equals(authViewModel.getIsLogin().getValue()));
			binding.authEmailTextInput.setText("");
			binding.authPasswordTextInput.setText("");
			binding.authEmailTextInput.clearFocus();
			binding.authPasswordTextInput.clearFocus();
		});
		// Navigation listener
		OnBackPressedCallback callback = new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {requireActivity().finish();}
		};
		requireActivity().getOnBackPressedDispatcher()
				.addCallback(getViewLifecycleOwner(), callback);
	}

	private void startAccess() {
		String email = Objects.requireNonNull(binding.authEmailTextInput.getText()).toString();
		String password = Objects.requireNonNull(binding.authPasswordTextInput.getText())
				.toString();
		if (authViewModel.validateData(email, password)) {
			if (Boolean.TRUE.equals(authViewModel.getIsLogin().getValue())) {
				login(email, password, binding.authRememberMe.isChecked());
			} else {
				register(email, password);
			}
		}
	}

	private void register(String email, String password) {
		access(email, password, false);
	}

	private void login(String email, String password, Boolean rememberLogin) {
		SharedPreferences preferences = requireActivity().getSharedPreferences("loginPreferences", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		if (rememberLogin) {
			editor.putString("email", email);
			editor.putString("password", password);
			editor.apply();
		} else {
			editor.clear().apply();
		}
		access(email, password, true);
	}

	private void access(String email, String password, Boolean isLogin) {
		if (Boolean.TRUE.equals(NetworkStateManager.getInstance().getNetworkConnectivityStatus()
										.getValue())) {
			MainViewModel.addLoading();
			ServientService.getInstance(requireContext()).thenCompose(servient -> {
						BiFunction<String, String, CompletableFuture<String>> accessFunction = isLogin ? servient::login : servient::register;
						return accessFunction.apply(email, password);
					})
					.thenAccept(bearer -> new Handler(Looper.getMainLooper()).post(() -> MainViewModel.getBearerToken()
							.setValue(bearer))).exceptionally(throwable -> {
						new Handler(Looper.getMainLooper()).post(() -> MainViewModel.getErrorToShow()
								.setValue(throwable));
						return null;
					})
					.thenRun(() -> new Handler(Looper.getMainLooper()).post(MainViewModel::removeLoading));
		} else {
			MainViewModel.getErrorToShow()
					.setValue(new Exception("Impossible to connect. Try again later."));
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}
}