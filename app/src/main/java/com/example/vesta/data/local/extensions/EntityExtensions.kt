package com.example.vesta.data.local.extensions

import kotlin.reflect.full.memberProperties

inline fun <reified T : Any> T.toMap(): Map<String, Any?> {
    return T::class.memberProperties
        .associate { it.name to it.get(this) }
}
