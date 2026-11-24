package com.example.data.backup

import com.squareup.moshi.Moshi

object BackupCodec {

    private val moshi: Moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(MooseBackupPayload::class.java).indent("  ")

    fun encode(payload: MooseBackupPayload): String = adapter.toJson(payload)

    fun decode(rawJson: String): MooseBackupPayload {
        require(rawJson.isNotBlank()) { "Backup JSON is empty" }
        val parsed = adapter.fromJson(rawJson)
            ?: throw IllegalArgumentException("Malformed backup JSON")
        require(parsed.schemaVersion in 1..MooseBackupPayload.CURRENT_SCHEMA_VERSION) {
            "Unsupported backup schemaVersion: ${parsed.schemaVersion}"
        }
        return parsed
    }
}
