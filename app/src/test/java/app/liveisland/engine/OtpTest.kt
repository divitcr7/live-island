package app.liveisland.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OtpTest {
    @Test fun bankOtp() = assertEquals("482913", Otp.find("482913 is your OTP for txn of Rs 1,250 at Amazon. Do not share."))
    @Test fun verificationCode() = assertEquals("7731", Otp.find("Your WhatsApp code: 7731. Don't share this code"))
    @Test fun dashedCode() = assertEquals("123456", Otp.find("G-123-456 is your Google verification code"))
    @Test fun loginCodeAfterText() = assertEquals("90210", Otp.find("Use login code 90210 to sign in"))
    @Test fun hindi() = assertEquals("5521", Otp.find("आपका ओटीपी 5521 है"))
    @Test fun ignoresAmounts() = assertEquals("664120", Otp.find("OTP 664120 for payment of Rs. 4999 to Swiggy"))
    @Test fun ignoresAmountOnly() = assertNull(Otp.find("Your code word: payment of Rs 2500 received"))
    @Test fun noKeyword() = assertNull(Otp.find("Meeting moved to 1430 in room 4021"))
    @Test fun ignoresYear() = assertNull(Otp.find("Your verification is due on 20 Nov 2026"))
}
