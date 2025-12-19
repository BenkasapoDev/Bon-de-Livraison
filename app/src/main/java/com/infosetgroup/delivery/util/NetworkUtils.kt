package com.infosetgroup.delivery.util

import java.util.Locale

fun mapNetworkErrorToFrench(error: String?): String {
    if (error.isNullOrBlank()) return "Échec de la synchronisation"

    val lower = error.lowercase(Locale.getDefault())

    return when {
        lower.contains("unable to resolve host") || lower.contains("failed to connect") || lower.contains("no address") || lower.contains("unknownhost") || lower.contains("network is unreachable") || lower.contains("enetworkunreach") || lower.contains("network unreachable") ->
            "Pas de connexion"

        lower.contains("timeout") || lower.contains("timed out") || lower.contains("connect timeout") || lower.contains("socket timeout") ->
            "Délai d'attente réseau"

        lower.contains("ssl") || lower.contains("certificate") || lower.contains("handshake") ->
            "Erreur de sécurité réseau"

        lower.startsWith("server returned code") -> {
            val parts = lower.split(" ")
            val code = parts.lastOrNull()?.uppercase(Locale.getDefault()) ?: ""
            "Erreur du serveur${if (code.isNotBlank()) " (code $code)" else ""}"
        }

        else -> "Échec: ${error.trim()}"
    }
}

