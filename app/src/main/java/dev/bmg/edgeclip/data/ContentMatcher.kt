package dev.bmg.edgeclip.data

object ContentMatcher {
    val URL_REGEX = Regex("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]", RegexOption.IGNORE_CASE)
    val OTP_REGEX = Regex("(?<![\\d.])\\b\\d{4,8}\\b(?!\\.[\\d])")
    val PHONE_REGEX = Regex("(?:\\+?\\d{1,3}[- ]?)?\\d{3,5}[- ]?\\d{3,5}(?:[- ]?\\d{1,5})?")
    
    val OTP_KEYWORDS = listOf("otp", "code", "verification", "auth", "login", "pin", "password")

    fun detectSubtype(text: String): String {
        val trimmed = text.trim()
        
        if (URL_REGEX.containsMatchIn(trimmed)) return "URL"
        
        val otpMatch = OTP_REGEX.find(trimmed)
        if (otpMatch != null) {
            val otpValue = otpMatch.value
            val hasKeyword = OTP_KEYWORDS.any { trimmed.contains(it, ignoreCase = true) }
            if (trimmed == otpValue || hasKeyword) return "OTP"
        }

        if (PHONE_REGEX.containsMatchIn(trimmed)) return "PHONE"
        
        return "NONE"
    }

    fun findOtp(text: String): String? {
        return OTP_REGEX.find(text)?.value
    }

    fun findPhone(text: String): String? {
        return PHONE_REGEX.find(text)?.value
    }
}
