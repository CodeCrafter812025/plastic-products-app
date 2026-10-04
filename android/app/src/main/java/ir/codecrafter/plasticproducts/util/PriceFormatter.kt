package ir.codecrafter.plasticproducts.util

import ir.codecrafter.plasticproducts.data.model.Product
import ir.codecrafter.plasticproducts.data.model.ProductCategory
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object PriceFormatter {
    private val formatter = DecimalFormat("#,###", DecimalFormatSymbols(Locale.US))

    fun format(rawPrice: String?): String {
        if (rawPrice.isNullOrBlank()) return "0"
        val number = rawPrice.trim().toBigDecimalOrNull() ?: return rawPrice
        return formatter.format(number.toBigInteger())
    }

    fun formatQuantity(rawQty: String?): String {
        if (rawQty.isNullOrBlank()) return "0"
        val bd = rawQty.trim().toBigDecimalOrNull() ?: return rawQty
        return bd.stripTrailingZeros().toPlainString()
    }

    /**
     * مبدل هوشمند واحد شمارش برای نمایش معادل دقیق عددی/بسته‌ای/کیسه‌ای در پیش‌فاکتور زنده
     */
    fun buildUnitConversionHint(product: Product, qty: BigDecimal): String? {
        val intQty = qty.toInt()
        if (intQty <= 0) return null
        val unit = product.unitLabel.orEmpty()

        return when {
            product.category == ProductCategory.CUP -> {
                val totalCups = formatter.format(intQty * 500L)
                val cartons = intQty / 10
                val remPacks = intQty % 10
                val cartonText = when {
                    cartons > 0 && remPacks == 0 -> "$cartons \u06a9\u0627\u0631\u062a\u0646 \u06a9\u0627\u0645\u0644"
                    cartons > 0 -> "$cartons \u06a9\u0627\u0631\u062a\u0646 \u0648$remPacks \u067e\u06a9"
                    else -> "$intQty \u067e\u06a9"
                }
                "\u0645\u0639\u0627\u062f\u0644: $cartonText (\u0645\u062c\u0645\u0648\u0639\u0627\u064b$totalCups \u0639\u062f\u062f \u0644\u06cc\u0648\u0627\u0646)"
            }
            product.category == ProductCategory.NAYLEX -> {
                val bags = intQty / 25
                val remKg = intQty % 25
                when {
                    bags > 0 && remKg == 0 -> "\u0645\u0639\u0627\u062f\u0644: $bags \u06a9\u06cc\u0633\u0647 25 \u06a9\u06cc\u0644\u0648\u06cc\u06cc \u06a9\u0627\u0645\u0644"
                    bags > 0 -> "\u0645\u0639\u0627\u062f\u0644: $bags \u06a9\u06cc\u0633\u0647 25 \u06a9\u06cc\u0644\u0648\u06cc\u06cc \u0648$remKg \u06a9\u06cc\u0644\u0648\u06af\u0631\u0645"
                    else -> null
                }
            }
            unit.contains("10 \u0639\u062f\u062f\u06cc") -> {
                val totalPieces = formatter.format(intQty * 10L)
                "\u0645\u0639\u0627\u062f\u0644: \u0645\u062c\u0645\u0648\u0639\u0627\u064b $totalPieces \u0639\u062f\u062f \u0632\u06cc\u067e\u200c\u06a9\u06cc\u067e"
            }
            unit.contains("100 \u0639\u062f\u062f\u06cc") -> {
                val totalPieces = formatter.format(intQty * 100L)
                "\u0645\u0639\u0627\u062f\u0644: \u0645\u062c\u0645\u0648\u0639\u0627\u064b $totalPieces \u0628\u0631\u06af \u0641\u0631\u06cc\u0632\u0631"
            }
            unit.contains("250 \u0639\u062f\u062f\u06cc") -> {
                val totalPieces = formatter.format(intQty * 250L)
                "\u0645\u0639\u0627\u062f\u0644: \u0645\u062c\u0645\u0648\u0639\u0627\u064b $totalPieces \u0628\u0631\u06af \u0641\u0631\u06cc\u0632\u0631"
            }
            unit.contains("500 \u0639\u062f\u062f\u06cc") -> {
                val totalPieces = formatter.format(intQty * 500L)
                "\u0645\u0639\u0627\u062f\u0644: \u0645\u062c\u0645\u0648\u0639\u0627\u064b $totalPieces \u0628\u0631\u06af"
            }
            unit.contains("2 \u0639\u062f\u062f\u06cc") -> {
                val totalPieces = formatter.format(intQty * 2L)
                "\u0645\u0639\u0627\u062f\u0644: \u0645\u062c\u0645\u0648\u0639\u0627\u064b $totalPieces \u0631\u0648\u0644"
            }
            unit.contains("250 \u06af\u0631\u0645\u06cc") -> {
                val totalGrams = intQty * 250
                if (totalGrams >= 1000) {
                    val kg = totalGrams / 1000.0
                    "\u0645\u0639\u0627\u062f\u0644 \u0648\u0632\u0646\u06cc: $kg \u06a9\u06cc\u0644\u0648\u06af\u0631\u0645"
                } else {
                    "\u0645\u0639\u0627\u062f\u0644 \u0648\u0632\u0646\u06cc: $totalGrams \u06af\u0631\u0645"
                }
            }
            else -> null
        }
    }
}
