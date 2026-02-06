package com.katorabian.service.gatekeeper

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ExecutionTarget {
    @SerialName("LOCAL") LOCAL,
    @SerialName("REMOTE") REMOTE
}
