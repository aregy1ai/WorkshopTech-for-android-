package com.workshoptech.domain.model

sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val exception: AppException) : AppResult<Nothing>()
    data object Loading : AppResult<Nothing>()

    val isSuccess get() = this is Success
    val isError get() = this is Error
    val isLoading get() = this is Loading

    fun getOrNull(): T? = (this as? Success)?.data
    fun exceptionOrNull(): AppException? = (this as? Error)?.exception
}

sealed class AppException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class DatabaseException(message: String, cause: Throwable? = null) :
        AppException("خطأ في قاعدة البيانات: $message", cause)

    class NetworkException(message: String, cause: Throwable? = null) :
        AppException("خطأ في الشبكة: $message", cause)

    class ImageProcessingException(message: String, cause: Throwable? = null) :
        AppException("خطأ في معالجة الصورة: $message", cause)

    class OcrException(message: String, cause: Throwable? = null) :
        AppException("خطأ في قراءة اللوحة: $message", cause)

    class PermissionException(val permission: String) :
        AppException("الإذن مرفوض: $permission")

    class FileNotFoundException(path: String) :
        AppException("الملف غير موجود: $path")

    class ValidationException(message: String) :
        AppException("خطأ في التحقق: $message")

    class UnknownException(cause: Throwable? = null) :
        AppException("خطأ غير معروف", cause)
}

inline fun <T> runCatchingAppResult(block: () -> T): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (e: AppException) {
        AppResult.Error(e)
    } catch (e: Exception) {
        AppResult.Error(AppException.UnknownException(e))
    }
