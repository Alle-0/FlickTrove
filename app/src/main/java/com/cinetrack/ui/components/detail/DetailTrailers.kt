package com.cinetrack.ui.components.detail

import androidx.compose.ui.res.stringResource
import com.cinetrack.R

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import kotlinx.coroutines.flow.filter
import kotlin.math.abs
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import com.cinetrack.ui.utils.bounceClick
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage

/**
 * DetailTrailers
 * Renders a horizontal list of YouTube trailers.
 * Features a lifecycle-aware WebView with auto-cleanup to prevent "ghost audio".
 */
@Composable
fun DetailTrailers(
    trailers: List<String>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    if (trailers.isEmpty()) return

    var activeVideoKey by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.detail_official_trailers),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp
                ),
                color = Color.White.copy(alpha = 0.5f)
            )

            if (trailers.size > 1) {
                Surface(
                    color = accentColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.detail_swipe),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 8.sp,
                                letterSpacing = 1.2.sp
                            ),
                            color = accentColor
                        )
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        val listState = rememberLazyListState()

        LaunchedEffect(listState) {
            snapshotFlow { listState.isScrollInProgress }
                .filter { !it }
                .collect {
                    val visibleItems = listState.layoutInfo.visibleItemsInfo
                    if (visibleItems.isEmpty()) return@collect
                    val targetItem = visibleItems.minByOrNull { abs(it.offset) } ?: return@collect
                    if (targetItem.index != listState.firstVisibleItemIndex || targetItem.offset != listState.firstVisibleItemScrollOffset) {
                        listState.animateScrollToItem(targetItem.index)
                    }
                }
        }

        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(trailers, key = { it }, contentType = { "trailer" }) { key ->
                val isActive = activeVideoKey == key
                Box(
                    modifier = Modifier
                        .width(340.dp)
                        .aspectRatio(16f / 9f)
                        .then(
                            if (!isActive) {
                                Modifier.bounceClick { activeVideoKey = key }
                            } else {
                                Modifier
                            }
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Black)
                            .border(0.5.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                    ) {
                        if (isActive) {
                            YouTubePlayer(videoId = key)
                        } else {
                            TrailerThumbnail(videoId = key, accentColor = accentColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrailerThumbnail(videoId: String, accentColor: Color) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        // Play Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            // YouTube Red Button
            Box(
                modifier = Modifier
                    .size(width = 68.dp, height = 48.dp)
                    .background(Color(0xFFFF0000).copy(alpha = 0.9f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // "TRAILER UFFICIALE" text
            Text(
                text = stringResource(R.string.detail_official_trailer),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp
                ),
                color = accentColor.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
            )
        }
    }
}

@Composable
fun YouTubePlayer(videoId: String) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false // Cruciale per l'autoplay
                    allowFileAccess = false
                    allowContentAccess = false
                }
                
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?, 
                        request: android.webkit.WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString() ?: ""
                        // Apri esternamente SOLO se il navigatore tenta di uscire dall'iframe (es. click sul logo YouTube)
                        if (request?.isForMainFrame == true && (url.contains("watch?v=") || url.contains("youtu.be"))) {
                            try {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW, 
                                    android.net.Uri.parse(url)
                                )
                                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                ctx.startActivity(intent)
                            } catch (e: Exception) {
                                // Ignora
                            }
                            return true
                        }
                        return super.shouldOverrideUrlLoading(view, request)
                    }
                }
                
                webChromeClient = object : WebChromeClient() {
                    private var customView: android.view.View? = null
                    private var customViewCallback: CustomViewCallback? = null

                    override fun onShowCustomView(view: android.view.View?, callback: CustomViewCallback?) {
                        if (customView != null) {
                            callback?.onCustomViewHidden()
                            return
                        }
                        val activity = ctx.findActivity() ?: return
                        val decorView = activity.window.decorView as android.widget.FrameLayout
                        
                        view?.setBackgroundColor(android.graphics.Color.BLACK)
                        decorView.addView(view, android.widget.FrameLayout.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        ))
                        
                        customView = view
                        customViewCallback = callback
                        
                        // Sblocca la rotazione dello schermo (permette orizzontale e verticale)
                        activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR
                    }

                    override fun onHideCustomView() {
                        if (customView == null) return
                        val activity = ctx.findActivity() ?: return
                        val decorView = activity.window.decorView as android.widget.FrameLayout
                        decorView.removeView(customView)
                        customView = null
                        customViewCallback?.onCustomViewHidden()
                        customViewCallback = null
                        
                        // Blocca di nuovo in verticale
                        activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                }
                setBackgroundColor(android.graphics.Color.BLACK)

                // Use youtube-nocookie.com to avoid some tracking issues and embedding restrictions
                val domain = "https://www.youtube-nocookie.com"
                val html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                        <style>
                            body { margin: 0; padding: 0; background-color: #000000; overflow: hidden; }
                            iframe { width: 100vw; height: 100vh; border: none; }
                        </style>
                    </head>
                    <body>
                        <iframe 
                            src="$domain/embed/$videoId?autoplay=1&playsinline=1&rel=0&modestbranding=1" 
                            allow="autoplay; encrypted-media; picture-in-picture" 
                            allowfullscreen>
                        </iframe>
                    </body>
                    </html>
                """.trimIndent()

                // BaseURL deve combaciare con il dominio dell'iframe per evitare problemi CORS o 152
                loadDataWithBaseURL(domain, html, "text/html", "UTF-8", null)
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { /* Nessun aggiornamento dinamico, niente loop */ },
        onRelease = { webView ->
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
        }
    )
}

private fun android.content.Context.findActivity(): android.app.Activity? {
    var current: android.content.Context = this
    while (current is android.content.ContextWrapper) {
        val wrapper = current as android.content.ContextWrapper
        if (wrapper is android.app.Activity) return wrapper
        current = wrapper.baseContext
    }
    return null
}
