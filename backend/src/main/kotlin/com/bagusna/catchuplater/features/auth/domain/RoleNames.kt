package com.bagusna.catchuplater.features.auth.domain

/**
 * Well-known authorities. Role names are stored in the database and are always
 * prefixed with `ROLE_` so that they work with `hasRole(...)` expressions.
 */
object RoleNames {
    const val USER = "ROLE_USER"
    const val ADMIN = "ROLE_ADMIN"
}
