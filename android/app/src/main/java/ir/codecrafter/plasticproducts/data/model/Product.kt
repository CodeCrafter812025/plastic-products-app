package ir.codecrafter.plasticproducts.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: Int,
    val title: String,
    val price: String,
    val weight: String,
    val color: String? = null,
    val quality: String,
    val description: String = "",
    @SerialName("image_urls") val imageUrls: List<String> = emptyList(),
    val stock: String,
    @SerialName("is_active") val isActive: Boolean,
    val category: String? = null,
    @SerialName("sub_category") val subCategory: String? = null,
    val brand: String? = null,
    @SerialName("unit_label") val unitLabel: String? = null,
    @SerialName("packaging_info") val packagingInfo: String? = null,
    @SerialName("is_bestseller") val isBestseller: Boolean = false,
    @SerialName("created_by") val createdBy: Int,
    @SerialName("created_by_name") val createdByName: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

object ProductQuality {
    const val PRIMARY = "\u0627\u0648\u0644\u06cc\u0647"
    const val RECYCLED = "\u0628\u0627\u0632\u06cc\u0627\u0641\u062a\u06cc"
}

object ProductCategory {
    const val NAYLEX = "\u0646\u0627\u06cc\u0644\u06a9\u0633 \u062f\u0633\u062a\u0647\u200c\u062f\u0627\u0631 \u0634\u0641\u0627\u0641"
    const val TISSUE = "\u062f\u0633\u062a\u0645\u0627\u0644 \u06a9\u0627\u063a\u0630\u06cc"
    const val CUP = "\u0644\u06cc\u0648\u0627\u0646 \u06cc\u06a9\u0628\u0627\u0631 \u0645\u0635\u0631\u0641"
    const val TRASH_BAG = "\u06a9\u06cc\u0633\u0647 \u0632\u0628\u0627\u0644\u0647"
    const val SOFREH = "\u0633\u0641\u0631\u0647 \u06cc\u06a9\u0628\u0627\u0631 \u0645\u0635\u0631\u0641"
    const val ZIPLOCK = "\u067e\u0644\u0627\u0633\u062a\u06cc\u06a9 \u0632\u06cc\u067e\u200c\u062f\u0627\u0631"
    const val FREEZER = "\u067e\u0644\u0627\u0633\u062a\u06cc\u06a9 \u0641\u0631\u06cc\u0632\u0631"

    val ALL = listOf(NAYLEX, TISSUE, CUP, TRASH_BAG, SOFREH, ZIPLOCK, FREEZER)
}
