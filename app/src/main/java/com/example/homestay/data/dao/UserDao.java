package com.example.homestay.data.dao;

import androidx.room.*;
import com.example.homestay.data.entity.User;
import java.util.List;

@Dao
public interface UserDao {
  @Query("SELECT * FROM users WHERE email=:e LIMIT 1")
  User getUserByEmailForLogin(String e);

  @Query("SELECT * FROM users WHERE email=:e LIMIT 1")
  User getUserByEmail(String e);

  @Query("SELECT * FROM users WHERE phone=:p LIMIT 1")
  User getUserByPhone(String p);

  @Query("SELECT * FROM users WHERE id=:id LIMIT 1")
  User getUserById(long id);

  @Insert(onConflict = OnConflictStrategy.ABORT)
  long insertUser(User u);

  @Update
  void updateUser(User u);

  @Delete
  void deleteUser(User u);

  @Query("SELECT * FROM users ORDER BY createdAt DESC")
  List<User> getAllUsersNow();
}
