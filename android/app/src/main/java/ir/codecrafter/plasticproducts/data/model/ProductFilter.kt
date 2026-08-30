package ir.codecrafter.plasticproducts.data.model

/**
 * Mirrors the query params ProductViewSet.get_queryset() actually reads for
 * list (backend/products/views.py): quality, color (icontains), min_price,
 * max_price, in_stock. There is no free-text "search" param in the real code.
 */
data class ProductFilter(
    val quality: String? = null,
    val color: String? = null,
    val minPrice: String? = null,
    val maxPrice: String? = null,
    val inStock: Boolean? = null,
)
