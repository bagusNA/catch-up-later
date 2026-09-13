package com.bagusna.catchuplater.features.setup.dto

/**
 * Tells the client whether first-run bootstrap is still required, i.e. whether
 * the instance has no accounts yet.
 */
data class SetupStatusResponse(
    val required: Boolean,
)
