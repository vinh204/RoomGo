package com.example.homestay.utils;

public final class AdminAuth {
  public static final String EMAIL = "admin@gmail.com";
  private static final String PASSWORD = "Admin@123";

  private AdminAuth() {}

  public static boolean authenticate(String email, String password) {
    return EMAIL.equalsIgnoreCase(email) && PASSWORD.equals(password);
  }
}
