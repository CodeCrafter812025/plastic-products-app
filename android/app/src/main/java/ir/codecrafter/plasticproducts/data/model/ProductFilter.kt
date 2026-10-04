package ir.codecrafter.plasticproducts.data.model

data class ProductFilter(
    val search: String? = null,
    val category: String? = null,
    val brand: String? = null,
    val isBestseller: Boolean? = null,
    val quality: String? = null,
    val color: String? = null,
    val minPrice: String? = null,
    val maxPrice: String? = null,
    val inStock: Boolean? = null,
)
