package com.katorabian.service.model

enum class ModelRuntimeState {
    COLD,        // модель не прогрета
    WARMING_UP,  // идёт прогрев
    READY,       // готова к inference
    BUSY,        // выполняется запрос
    ERROR
}