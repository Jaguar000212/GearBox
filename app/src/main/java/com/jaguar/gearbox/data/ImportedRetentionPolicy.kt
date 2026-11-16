package com.jaguar.gearbox.data

/** Derived metadata (for example partial translation warnings) is not a user edit. */
fun retentionInputValues(inputs: Map<String, String>): Map<String, String> = inputs.filterKeys { !it.startsWith("_") }

/** Fingerprints only: the suppression gate never holds another saved-state copy of shared text. */
fun suppressImportedObservation(
    importedFingerprint: String?,
    currentFingerprint: String,
    resultRequiresExplicitAction: Boolean,
    hasOutput: Boolean,
): Boolean = importedFingerprint != null && importedFingerprint == currentFingerprint &&
    (!resultRequiresExplicitAction || !hasOutput)
