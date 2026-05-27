package pe.khipuai.app.ui.screens.maps

import android.webkit.JavascriptInterface

/**
 * Puente seguro JavaScript → Kotlin para comunicar eventos de clic del grafo D3.js
 * (que corre en la WebView) con el ViewModel nativo de Jetpack Compose.
 *
 * Se registra en la WebView mediante `addJavascriptInterface(bridge, "AndroidBridge")`.
 * JavaScript lo invoca con: `window.AndroidBridge.onConceptSelected(conceptId)`
 */
class GraphWebViewBridge(
    private val conceptSelectedCallback: (String) -> Unit
) {
    @JavascriptInterface
    fun onConceptSelected(conceptId: String) {
        conceptSelectedCallback(conceptId)
    }
}
