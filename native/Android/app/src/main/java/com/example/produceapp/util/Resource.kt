package com.example.produceapp.util

// =============================================================================
// Resource.kt — 非同步資料狀態的密封類別 (Sealed Class)
//
// 設計說明：
//   Resource<T> 是一個「三態狀態機」，用於封裝任何非同步資料請求的生命週期：
//
//   Loading → 請求中 → Success 或 Error
//
//   在 Android MVVM 架構中的角色：
//   - ViewModel 發出請求時，先 emit Resource.Loading()
//   - 請求成功，emit Resource.Success(data)
//   - 請求失敗，emit Resource.Error(message)
//   - UI (Composable) 透過 collectAsState() 觀察 StateFlow<Resource<T>>，
//     根據不同狀態顯示：骨架屏 / 資料列表 / 錯誤 + 重試按鈕
//
// 對應關係：
//   iOS:     enum Resource<T> { case loading, success(T), failure(Error) }
//   Android: sealed class Resource<T>(val data, val message)
//   兩者語義完全相同，只是語言語法不同。
//
// data 參數設計：
//   Error 和 Loading 狀態也帶有 data? 參數，
//   允許「加載中同時顯示舊資料」或「失敗但保留上次成功的資料」的 UI 體驗。
//   例：下拉重新整理時，Loading 狀態仍可顯示之前的資料，不清空列表。
// =============================================================================

/**
 * 非同步資料狀態包裝器
 * @param T 資料類型
 * @param data 攜帶的資料（可為 null）
 * @param message 錯誤訊息（僅 Error 狀態使用）
 */
sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null
) {
    /**
     * 請求成功，包含回傳的資料
     * UI 行為：隱藏 Skeleton Loader，顯示實際資料列表
     */
    class Success<T>(data: T) : Resource<T>(data)

    /**
     * 請求失敗，包含錯誤訊息
     * UI 行為：顯示錯誤訊息 + 「重試」按鈕
     * @param data 可選：保留上次成功的資料（用於「錯誤但仍顯示舊資料」的 UX 模式）
     */
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)

    /**
     * 請求進行中
     * UI 行為：顯示骨架屏 (Skeleton Loader) 或進度指示器
     * @param data 可選：下拉刷新時保留舊資料，不清空列表
     */
    class Loading<T>(data: T? = null) : Resource<T>(data)
}
