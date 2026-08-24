package com.example.homestay.ui.auth;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.example.homestay.data.entity.User;
import com.example.homestay.data.repository.AuthRepository;
import com.example.homestay.data.repository.OperationResult;
import com.example.homestay.utils.InputValidator;
import com.example.homestay.utils.RateLimiter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AuthViewModel extends ViewModel {
  private final AuthRepository repository;
  private final Context context;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final MutableLiveData<AuthResult> loginResult = new MutableLiveData<>();
  private final MutableLiveData<AuthResult> registerResult = new MutableLiveData<>();

  public AuthViewModel(AuthRepository repository, Context context) {
    this.repository = repository;
    this.context = context == null ? null : context.getApplicationContext();
  }

  public LiveData<AuthResult> getLoginResult() {
    return loginResult;
  }

  public LiveData<AuthResult> getRegisterResult() {
    return registerResult;
  }

  public void login(String email, String password) {
    if (!InputValidator.validateEmail(email)) {
      loginResult.setValue(AuthResult.error("Email không hợp lệ"));
      return;
    }
    if (context != null) {
      RateLimiter.AttemptStatus status = RateLimiter.canAttemptLogin(context, email);
      if (!status.allowed && status.lockedUntil != null) {
        long seconds = RateLimiter.getLockedSecondsRemaining(context, email);
        boolean adminLocked = seconds > 365L * 24 * 60 * 60;
        String time = seconds < 60 ? seconds + " giây" : (seconds / 60) + " phút";
        loginResult.setValue(
            new AuthResult(
                false,
                null,
                null,
                adminLocked
                    ? "Tài khoản đã bị quản trị viên khóa. Vui lòng liên hệ RoomGo để được hỗ trợ."
                    : "Tài khoản tạm khóa. Vui lòng thử lại sau " + time + ".",
                null,
                status.lockedUntil));
        return;
      }
    }
    executor.execute(
        () -> {
          OperationResult<AuthRepository.AuthData> result = repository.login(email, password);
          if (result.isSuccess()) {
            if (context != null) RateLimiter.recordSuccess(context, email);
            AuthRepository.AuthData data = result.getValue();
            loginResult.postValue(
                new AuthResult(true, data.getUser(), data.getMongoUserId(), null, null, null));
          } else {
            String message =
                result.getError() == null ? "Đăng nhập thất bại" : result.getError().getMessage();
            Integer remaining = null;
            Long lockedUntil = null;
            if (context != null) {
              RateLimiter.FailureStatus status = RateLimiter.recordFailure(context, email);
              remaining = status.remainingAttempts;
              lockedUntil = status.lockedUntil;
              message =
                  remaining > 0
                      ? message + ". Còn " + remaining + " lần thử."
                      : "Đã vượt quá số lần đăng nhập sai. Tài khoản đã bị khóa.";
            }
            loginResult.postValue(
                new AuthResult(false, null, null, message, remaining, lockedUntil));
          }
        });
  }

  public void register(String fullName, String email, String phone, String password) {
    if (!InputValidator.validateFullName(fullName)) {
      registerResult.setValue(AuthResult.error("Họ và tên không hợp lệ (2-50 ký tự)"));
      return;
    }
    if (!InputValidator.validateEmail(email)) {
      registerResult.setValue(AuthResult.error("Email không hợp lệ"));
      return;
    }
    if (!InputValidator.validatePhoneNumber(phone)) {
      registerResult.setValue(AuthResult.error("Số điện thoại không hợp lệ"));
      return;
    }
    if (!InputValidator.isPasswordValid(password)) {
      registerResult.setValue(AuthResult.error(InputValidator.getPasswordErrorMessage(password)));
      return;
    }
    String normalizedPhone = InputValidator.normalizePhoneNumber(phone);
    String name = InputValidator.sanitizeInput(fullName);
    executor.execute(
        () -> {
          OperationResult<AuthRepository.AuthData> result =
              repository.register(email, normalizedPhone, password, name);
          if (result.isSuccess()) {
            AuthRepository.AuthData data = result.getValue();
            registerResult.postValue(
                new AuthResult(
                    true, data.getUser(), data.getMongoUserId(), "Đăng ký thành công", null, null));
          } else
            registerResult.postValue(
                AuthResult.error(
                    result.getError() == null
                        ? "Đăng ký thất bại"
                        : result.getError().getMessage()));
        });
  }

  @Override
  protected void onCleared() {
    executor.shutdownNow();
  }

  public static final class AuthResult {
    public final boolean success;
    public final User user;
    public final String mongoUserId;
    public final String message;
    public final Integer remainingAttempts;
    public final Long lockedUntil;

    public AuthResult(
        boolean success,
        User user,
        String mongoUserId,
        String message,
        Integer remainingAttempts,
        Long lockedUntil) {
      this.success = success;
      this.user = user;
      this.mongoUserId = mongoUserId;
      this.message = message;
      this.remainingAttempts = remainingAttempts;
      this.lockedUntil = lockedUntil;
    }

    static AuthResult error(String message) {
      return new AuthResult(false, null, null, message, null, null);
    }
  }

  public static final class Factory implements ViewModelProvider.Factory {
    private final AuthRepository repository;
    private final Context context;

    public Factory(AuthRepository repository, Context context) {
      this.repository = repository;
      this.context = context;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      if (modelClass.isAssignableFrom(AuthViewModel.class))
        return modelClass.cast(new AuthViewModel(repository, context));
      throw new IllegalArgumentException("Unknown ViewModel class");
    }
  }
}
