package com.example.homestay.utils

object AdminAuth {
    const val EMAIL = "admin@gmail.com"
    private const val PASSWORD = "Admin@123"

    fun authenticate(email: String, password: String): Boolean =
        email.equals(EMAIL, ignoreCase = true) && password == PASSWORD
}
