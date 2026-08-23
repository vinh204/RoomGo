package com.example.homestay.data.repository;

import com.example.homestay.data.dao.UserDao;
import com.example.homestay.data.entity.User;
import com.example.homestay.utils.PasswordHasher;
import java.util.Locale;

public final class AuthRepository {
  private final UserDao userDao;

  public AuthRepository(UserDao userDao) {
    this.userDao = userDao;
  }

  public OperationResult<AuthData> login(String email, String password) {
    User user = userDao.getUserByEmailForLogin(normalizeEmail(email));
    if (user != null && PasswordHasher.verify(password, user.getPassword()))
      return OperationResult.success(new AuthData(user, String.valueOf(user.getId())));
    return OperationResult.failure(new LoginException("Email hoặc mật khẩu không đúng", null));
  }

  public OperationResult<AuthData> register(
      String email, String phone, String password, String fullName) {
    try {
      String normalized = normalizeEmail(email);
      if (userDao.getUserByEmail(normalized) != null || userDao.getUserByPhone(phone) != null)
        return OperationResult.failure(new Exception("Email hoặc số điện thoại đã tồn tại"));
      User user =
          new User(
              0,
              normalized,
              phone,
              PasswordHasher.hash(password),
              fullName.trim(),
              System.currentTimeMillis());
      long id = userDao.insertUser(user);
      User saved = user.withId(id);
      return OperationResult.success(new AuthData(saved, String.valueOf(id)));
    } catch (Exception error) {
      return OperationResult.failure(error);
    }
  }

  public User getUserByEmail(String email) {
    return userDao.getUserByEmail(normalizeEmail(email));
  }

  public User getUserByPhone(String phone) {
    return userDao.getUserByPhone(phone);
  }

  public OperationResult<String> getMongoUserIdByEmail(String email) {
    User user = getUserByEmail(email);
    return user == null
        ? OperationResult.failure(new Exception("Không tìm thấy tài khoản"))
        : OperationResult.success(String.valueOf(user.getId()));
  }

  public OperationResult<User> updateUser(
      long localUserId, String mongoUserId, String fullName, String newPassword) {
    User current = userDao.getUserById(localUserId);
    if (current == null) return OperationResult.failure(new Exception("Không tìm thấy tài khoản"));
    String password =
        newPassword != null && !newPassword.trim().isEmpty()
            ? PasswordHasher.hash(newPassword)
            : current.getPassword();
    User updated = current.withNamePassword(fullName.trim(), password);
    userDao.updateUser(updated);
    return OperationResult.success(updated);
  }

  public OperationResult<User> syncUserFromBackend(long localUserId, String mongoUserId) {
    User user = userDao.getUserById(localUserId);
    return user == null
        ? OperationResult.failure(new Exception("Không tìm thấy tài khoản"))
        : OperationResult.success(user);
  }

  private static String normalizeEmail(String email) {
    return email.toLowerCase(Locale.ROOT).trim();
  }

  public static final class AuthData {
    private final User user;
    private final String mongoUserId;

    public AuthData(User user, String mongoUserId) {
      this.user = user;
      this.mongoUserId = mongoUserId;
    }

    public User getUser() {
      return user;
    }

    public String getMongoUserId() {
      return mongoUserId;
    }
  }

  public static final class LocalLoginInfo {}

  public static final class LoginException extends Exception {
    private final LocalLoginInfo authResponse;

    public LoginException(String message, LocalLoginInfo authResponse) {
      super(message);
      this.authResponse = authResponse;
    }

    public LocalLoginInfo getAuthResponse() {
      return authResponse;
    }
  }
}
