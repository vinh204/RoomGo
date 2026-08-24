package com.example.homestay.data.entity;

import androidx.annotation.NonNull;
import androidx.room.*;

@Entity(
    tableName = "users",
    indices = {
      @Index(
          value = {"email"},
          unique = true),
      @Index(
          value = {"phone"},
          unique = true)
    })
public class User {
  @PrimaryKey(autoGenerate = true)
  private final long id;

  @NonNull private final String email;
  @NonNull private final String phone;
  @NonNull private final String password;
  @NonNull private final String fullName;
  private final long createdAt;
  private final boolean locked;
  @NonNull private final String role;

  @Ignore
  public User(
      long id, String email, String phone, String password, String fullName, long createdAt) {
    this(id, email, phone, password, fullName, createdAt, false, "CUSTOMER");
  }

  @Ignore
  public User(
      long id,
      String email,
      String phone,
      String password,
      String fullName,
      long createdAt,
      boolean locked) {
    this(id, email, phone, password, fullName, createdAt, locked, "CUSTOMER");
  }

  public User(
      long id,
      String email,
      String phone,
      String password,
      String fullName,
      long createdAt,
      boolean locked,
      @NonNull String role) {
    this.id = id;
    this.email = email;
    this.phone = phone;
    this.password = password;
    this.fullName = fullName;
    this.createdAt = createdAt;
    this.locked = locked;
    this.role = role;
  }

  public long getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getPhone() {
    return phone;
  }

  public String getPassword() {
    return password;
  }

  public String getFullName() {
    return fullName;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public boolean isLocked() {
    return locked;
  }

  public String getRole() {
    return role;
  }

  public boolean isAdmin() {
    return "ADMIN".equalsIgnoreCase(role);
  }

  public User withId(long v) {
    return new User(v, email, phone, password, fullName, createdAt, locked, role);
  }

  public User updated(String n, String e, String p, String pass) {
    return new User(id, e, p, pass, n, createdAt, locked, role);
  }

  public User withPassword(String pass) {
    return new User(id, email, phone, pass, fullName, createdAt, locked, role);
  }

  public User withNamePassword(String n, String pass) {
    return new User(id, email, phone, pass, n, createdAt, locked, role);
  }

  public User withLocked(boolean value) {
    return new User(id, email, phone, password, fullName, createdAt, value, role);
  }
}
