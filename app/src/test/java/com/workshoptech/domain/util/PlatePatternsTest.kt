package com.workshoptech.domain.util

import org.junit.Assert.*
import org.junit.Test

class PlatePatternsTest {
    @Test fun libyaPattern_matches6Digits() { assertTrue(Regex(PlatePatterns.patterns["LY"]!![0]).matches("123456")) }
    @Test fun libyaPattern_matches6DigitsWithLetter() { assertTrue(Regex(PlatePatterns.patterns["LY"]!![1]).matches("123456 ب")) }
    @Test fun egyptPattern_matches() { assertTrue(Regex(PlatePatterns.patterns["EG"]!![0]).matches("ب س ر 123")) }
    @Test fun saudiPattern_matches() { assertTrue(Regex(PlatePatterns.patterns["SA"]!![0]).matches("أ ب ج 1234")) }
    @Test fun uaePattern_matches() { assertTrue(Regex(PlatePatterns.patterns["AE"]!![0]).matches("12345 أ 1")) }
    @Test fun kuwaitPattern_matches() { assertTrue(Regex(PlatePatterns.patterns["KW"]!![0]).matches("123 456")) }
    @Test fun qatarPattern_matches() { assertTrue(Regex(PlatePatterns.patterns["QA"]!![0]).matches("123456")) }
    @Test fun bahrainPattern_matches() { assertTrue(Regex(PlatePatterns.patterns["BH"]!![0]).matches("123456")) }
    @Test fun omanPattern_matches() { assertTrue(Regex(PlatePatterns.patterns["OM"]!![0]).matches("12345 أ")) }
    @Test fun jordanPattern_matches() { assertTrue(Regex(PlatePatterns.patterns["JO"]!![0]).matches("12-34567")) }
    @Test fun lebanonPattern_matches() { assertTrue(Regex(PlatePatterns.patterns["LB"]!![0]).matches("ب 123456")) }
    @Test fun iraqPattern_matches() { assertTrue(Regex(PlatePatterns.patterns["IQ"]!![0]).matches("ب 12345")) }
    @Test fun moroccoPattern_matches() { assertTrue(Regex(PlatePatterns.patterns["MA"]!![0]).matches("12345 أ")) }
    @Test fun tunisiaPattern_matches() { assertTrue(Regex(PlatePatterns.patterns["TN"]!![0]).matches("1234 تونس")) }
    @Test fun algeriaPattern_matches() { assertTrue(Regex(PlatePatterns.patterns["DZ"]!![0]).matches("12345 123 12")) }
    @Test fun sudanPattern_matches() { assertTrue(Regex(PlatePatterns.patterns["SD"]!![0]).matches("123456")) }
    @Test fun yemenPattern_matches() { assertTrue(Regex(PlatePatterns.patterns["YE"]!![0]).matches("12345")) }
    @Test fun syriaPattern_matches() { assertTrue(Regex(PlatePatterns.patterns["SY"]!![0]).matches("123456")) }
    @Test fun unknownCountry_returnsFallback() { assertTrue(PlatePatterns.getForCountry("XX").isNotEmpty()) }
    @Test fun allCountries_haveAtLeastOnePattern() { PlatePatterns.patterns.forEach { assertTrue(it.value.isNotEmpty()) } }
    @Test fun ocrEngine_cleanText_removesSpaces() { assertEquals("ABC123", com.workshoptech.ml.OcrEngine.cleanText("ABC 123")) }
    @Test fun ocrEngine_cleanTextKeepSpaces_preservesSpaces() { assertEquals("ABC 123", com.workshoptech.ml.OcrEngine.cleanTextKeepSpaces("ABC 123")) }
}
