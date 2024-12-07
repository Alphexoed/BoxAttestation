package dev.alphexo.boxattestation.models

enum class KeyUsageModel {
    DIGITAL_SIGNATURE,
    NON_REPUDIATION,
    KEY_ENCIPHERMENT,
    DATA_ENCIPHERMENT,
    KEY_AGREEMENT,
    KEY_CERT_SIGN,
    CRL_SIGN,
    ENCIPHER_ONLY,
    DECIPHER_ONLY,
    NONE
}

fun keyUsageModeled(keyUsages: BooleanArray): List<KeyUsageModel> {
    val result = mutableListOf<KeyUsageModel>()
    for (i in keyUsages.indices) {
        if (keyUsages[i]) {
            KeyUsageModel.entries.getOrNull(i)?.let { result.add(it) }
        }
    }
    return result
}