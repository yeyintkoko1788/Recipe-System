package com.yeyint.recipeapp.backend.security

import at.favre.lib.crypto.bcrypt.BCrypt

/** BCrypt password hashing behind an interface so tests can use a fast fake. */
interface PasswordHasher {
    fun hash(raw: String): String
    fun verify(raw: String, hash: String): Boolean
}

class BcryptPasswordHasher(private val cost: Int = 12) : PasswordHasher {
    override fun hash(raw: String): String =
        BCrypt.withDefaults().hashToString(cost, raw.toCharArray())

    override fun verify(raw: String, hash: String): Boolean =
        BCrypt.verifyer().verify(raw.toCharArray(), hash).verified
}
