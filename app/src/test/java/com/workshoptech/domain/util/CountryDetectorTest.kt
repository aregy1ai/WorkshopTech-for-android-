package com.workshoptech.domain.util

import org.junit.Assert.*
import org.junit.Test

class CountryDetectorTest {
    @Test fun detectLibya_fromPhoneCode() { assertEquals("LY", CountryDetector.detectFromPhoneCode("+218")) }
    @Test fun detectEgypt_fromPhoneCode() { assertEquals("EG", CountryDetector.detectFromPhoneCode("+20")) }
    @Test fun detectSaudi_fromPhoneCode() { assertEquals("SA", CountryDetector.detectFromPhoneCode("+966")) }
    @Test fun detectUAE_fromPhoneCode() { assertEquals("AE", CountryDetector.detectFromPhoneCode("+971")) }
    @Test fun detectKuwait_fromPhoneCode() { assertEquals("KW", CountryDetector.detectFromPhoneCode("+965")) }
    @Test fun detectQatar_fromPhoneCode() { assertEquals("QA", CountryDetector.detectFromPhoneCode("+974")) }
    @Test fun detectBahrain_fromPhoneCode() { assertEquals("BH", CountryDetector.detectFromPhoneCode("+973")) }
    @Test fun detectOman_fromPhoneCode() { assertEquals("OM", CountryDetector.detectFromPhoneCode("+968")) }
    @Test fun detectJordan_fromPhoneCode() { assertEquals("JO", CountryDetector.detectFromPhoneCode("+962")) }
    @Test fun detectLebanon_fromPhoneCode() { assertEquals("LB", CountryDetector.detectFromPhoneCode("+961")) }
    @Test fun detectIraq_fromPhoneCode() { assertEquals("IQ", CountryDetector.detectFromPhoneCode("+964")) }
    @Test fun detectMorocco_fromPhoneCode() { assertEquals("MA", CountryDetector.detectFromPhoneCode("+212")) }
    @Test fun detectTunisia_fromPhoneCode() { assertEquals("TN", CountryDetector.detectFromPhoneCode("+216")) }
    @Test fun detectAlgeria_fromPhoneCode() { assertEquals("DZ", CountryDetector.detectFromPhoneCode("+213")) }
    @Test fun unknownCode_returnsNull() { assertNull(CountryDetector.detectFromPhoneCode("+999")) }
    @Test fun detectFromPlateText_libya() { assertEquals("LY", CountryDetector.detectFromPlateText("123456")) }
    @Test fun detectFromPlateText_egypt() { assertEquals("EG", CountryDetector.detectFromPlateText("ب س ر 123")) }
    @Test fun detectFromPlateText_invalid() { assertNull(CountryDetector.detectFromPlateText("xyz")) }
}
