package com.jaguar.gearbox.ui.tools

import java.nio.ByteBuffer
import java.security.MessageDigest

/** Invalid drafts never become saved-result schemas. A bounded fingerprint still lets the
 * shared hydration guard notice every editor change while its initial disk read is pending.
 * Length-prefixed sections avoid ambiguity from names/amounts containing separators.
 */
internal fun sharedBillDraftInputs(
    participantIds: List<Int>, names: List<String>, expenseIds: List<Int>, amounts: List<String>,
    payers: List<Int>, beneficiaries: List<String>,
): Map<String, String> {
    val digest = MessageDigest.getInstance("SHA-256")
    fun integer(value: Int) { digest.update(ByteBuffer.allocate(4).putInt(value).array()) }
    fun section(values: List<String>) {
        integer(values.size)
        values.forEach { value ->
            // Hash exact UTF-16 code units, including temporarily unpaired editor surrogates.
            integer(value.length)
            value.forEach { char -> digest.update((char.code ushr 8).toByte()); digest.update(char.code.toByte()) }
        }
    }
    section(participantIds.map(Int::toString)); section(names)
    section(expenseIds.map(Int::toString)); section(amounts)
    section(payers.map(Int::toString)); section(beneficiaries)
    val hex = digest.digest().joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
    return mapOf("format" to "shared-draft-v1", "draftFingerprint" to hex)
}
