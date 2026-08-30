package ir.codecrafter.plasticproducts.data.model

/**
 * Mirrors the query params ProductViewSet.get_queryset() reads for list
 * (backend/products/views.py): search, quality, color (icontains), min_price,
 * max_price, in_stock — all composable together.
 */
data class ProductFilter(
    val search: String? = null,
    val quality: String? = null,
    val color: String? = null,
    val minPrice: String? = null,
    val maxPrice: String? = null,
    val inStock: Boolean? = null,
)
