package ir.codecrafter.plasticproducts.ui.admin

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.ProductUpdateBody
import ir.codecrafter.plasticproducts.data.model.ProductWriteBody
import ir.codecrafter.plasticproducts.data.model.StockChangeReason
import ir.codecrafter.plasticproducts.data.network.ErrorMessage
import ir.codecrafter.plasticproducts.data.repository.AdminProductRepository
import ir.codecrafter.plasticproducts.data.repository.AuthResult
import ir.codecrafter.plasticproducts.data.repository.ProductRepository
import ir.codecrafter.plasticproducts.ui.navigation.AdminProductRoutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject

data class AdminProductFormUiState(
    val isEditMode: Boolean = false,
    val title: String = "",
    val price: String = "",
    val weight: String = "1",
    val color: String = "",
    val quality: String? = "\u0627\u0648\u0644\u06cc\u0647",
    val description: String = "",
    val stock: String = "",
    val isActive: Boolean = true,
    val category: String = "",
    val subCategory: String = "",
    val brand: String = "",
    val unitLabel: String = "",
    val packagingInfo: String = "",
    val isBestseller: Boolean = false,
    val imageUrls: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isUploadingImage: Boolean = false,
    val isUpdatingPrice: Boolean = false,
    val isUpdatingStock: Boolean = false,
    val errorMessage: String? = null,
)

sealed class AdminProductFormEvent {
    data class ActionFailed(val message: String) : AdminProductFormEvent()
    data object Saved : AdminProductFormEvent()
}

@HiltViewModel
class AdminProductFormViewModel @Inject constructor(
    private val adminProductRepository: AdminProductRepository,
    private val productRepository: ProductRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val productId: Int? = savedStateHandle[AdminProductRoutes.PRODUCT_ID_ARG]

    private val _uiState = MutableStateFlow(AdminProductFormUiState(isEditMode = productId != null))
    val uiState: StateFlow<AdminProductFormUiState> = _uiState.asStateFlow()

    private val _events = Channel<AdminProductFormEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        productId?.let(::loadProduct)
    }

    fun retryLoad() {
        productId?.let(::loadProduct)
    }

    private fun loadProduct(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = productRepository.getProductDetail(id)) {
                is AuthResult.Success -> {
                    val product = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            title = product.title,
                            price = product.price,
                            weight = product.weight,
                            color = product.color.orEmpty(),
                            quality = product.quality,
                            description = product.description,
                            stock = product.stock,
                            isActive = product.isActive,
                            category = product.category.orEmpty(),
                            subCategory = product.subCategory.orEmpty(),
                            brand = product.brand.orEmpty(),
                            unitLabel = product.unitLabel.orEmpty(),
                            packagingInfo = product.packagingInfo.orEmpty(),
                            isBestseller = product.isBestseller,
                            imageUrls = product.imageUrls,
                        )
                    }
                }
                else -> _uiState.update { it.copy(isLoading = false, errorMessage = describeFailure(result)) }
            }
        }
    }

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value) }
    fun onPriceChange(value: String) = _uiState.update { it.copy(price = value) }
    fun onWeightChange(value: String) = _uiState.update { it.copy(weight = value) }
    fun onColorChange(value: String) = _uiState.update { it.copy(color = value) }
    fun onQualityChange(value: String) = _uiState.update { it.copy(quality = value) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }
    fun onStockChange(value: String) = _uiState.update { it.copy(stock = value) }
    fun onIsActiveChange(value: Boolean) = _uiState.update { it.copy(isActive = value) }
    fun onCategoryChange(value: String) = _uiState.update { it.copy(category = value) }
    fun onSubCategoryChange(value: String) = _uiState.update { it.copy(subCategory = value) }
    fun onBrandChange(value: String) = _uiState.update { it.copy(brand = value) }
    fun onUnitLabelChange(value: String) = _uiState.update { it.copy(unitLabel = value) }
    fun onPackagingInfoChange(value: String) = _uiState.update { it.copy(packagingInfo = value) }
    fun onIsBestsellerChange(value: Boolean) = _uiState.update { it.copy(isBestseller = value) }

    fun save() {
        val state = _uiState.value
        val quality = state.quality ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = if (productId != null) {
                adminProductRepository.updateProduct(
                    productId,
                    ProductUpdateBody(
                        title = state.title.trim(),
                        weight = state.weight.trim(),
                        color = state.color.trim().ifBlank { null },
                        quality = quality,
                        description = state.description.trim(),
                        isActive = state.isActive,
                        category = state.category.trim().ifBlank { null },
                        subCategory = state.subCategory.trim().ifBlank { null },
                        brand = state.brand.trim().ifBlank { null },
                        unitLabel = state.unitLabel.trim().ifBlank { null },
                        packagingInfo = state.packagingInfo.trim().ifBlank { null },
                        isBestseller = state.isBestseller,
                    ),
                )
            } else {
                adminProductRepository.createProduct(
                    ProductWriteBody(
                        title = state.title.trim(),
                        price = state.price.trim(),
                        weight = state.weight.trim(),
                        color = state.color.trim().ifBlank { null },
                        quality = quality,
                        description = state.description.trim(),
                        stock = state.stock.trim(),
                        isActive = true,
                        category = state.category.trim().ifBlank { null },
                        subCategory = state.subCategory.trim().ifBlank { null },
                        brand = state.brand.trim().ifBlank { null },
                        unitLabel = state.unitLabel.trim().ifBlank { null },
                        packagingInfo = state.packagingInfo.trim().ifBlank { null },
                        isBestseller = state.isBestseller,
                    ),
                )
            }
            when (result) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(AdminProductFormEvent.Saved)
                }
                else -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(AdminProductFormEvent.ActionFailed(describeFailure(result)))
                }
            }
        }
    }

    fun updatePrice(newPrice: String) {
        val id = productId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingPrice = true) }
            when (val result = adminProductRepository.updatePrice(id, newPrice.trim())) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isUpdatingPrice = false) }
                    loadProduct(id)
                }
                else -> {
                    _uiState.update { it.copy(isUpdatingPrice = false) }
                    _events.send(AdminProductFormEvent.ActionFailed(describeFailure(result)))
                }
            }
        }
    }

    fun updateStock(newStock: String, reason: StockChangeReason) {
        val id = productId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingStock = true) }
            when (val result = adminProductRepository.updateStock(id, newStock.trim(), reason)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isUpdatingStock = false) }
                    loadProduct(id)
                }
                else -> {
                    _uiState.update { it.copy(isUpdatingStock = false) }
                    _events.send(AdminProductFormEvent.ActionFailed(describeFailure(result)))
                }
            }
        }
    }

    fun uploadImage(uri: Uri) {
        val id = productId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingImage = true) }
            val part = withContext(Dispatchers.IO) { buildImagePart(uri) }
            if (part == null) {
                _uiState.update { it.copy(isUploadingImage = false) }
                _events.send(AdminProductFormEvent.ActionFailed(context.getString(R.string.error_generic)))
                return@launch
            }
            when (val result = adminProductRepository.uploadImage(id, part)) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(isUploadingImage = false, imageUrls = result.data.imageUrls)
                }
                else -> {
                    _uiState.update { it.copy(isUploadingImage = false) }
                    _events.send(AdminProductFormEvent.ActionFailed(describeFailure(result)))
                }
            }
        }
    }

    private fun buildImagePart(uri: Uri): MultipartBody.Part? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
            val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            MultipartBody.Part.createFormData("image", "upload.$extension", requestBody)
        } catch (e: IOException) {
            null
        }
    }

    private fun describeFailure(result: AuthResult<*>): String = when (result) {
        is AuthResult.RateLimited -> result.message ?: context.getString(R.string.error_rate_limited)
        is AuthResult.Error -> when (val message = result.message) {
            is ErrorMessage.StringMessage -> message.value
            is ErrorMessage.FieldErrors -> message.fields.values.flatten().firstOrNull()
                ?: context.getString(R.string.error_generic)
            null -> context.getString(R.string.error_generic)
        }
        AuthResult.NetworkError -> context.getString(R.string.error_network)
        is AuthResult.Success -> ""
    }
}
