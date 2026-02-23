package com.katorabian.domain

import java.util.UUID

object UserContext {

    @Deprecated("Временное решение до внедрения авторизации")
    val SINGLE_USER_ID: UUID =
        UUID.fromString("00000000-0000-0000-0000-000000000001")
}