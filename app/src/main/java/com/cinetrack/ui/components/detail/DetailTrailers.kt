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
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.shadow
import androidx.activity.compose.BackHandler
import kotlin.math.absoluteValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import com.cinetrack.ui.utils.bounceClick
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage

import com.cinetrack.data.api.Video
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.EaseOutCirc
import androidx.compose.animation.core.EaseInCirc
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.HazeState
import com.cinetrack.ui.components.glass.hazeGlass
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.geometry.Rect

data class TrailersMenuModalData(
    val triggerBounds: Rect,
    val availableCategories: List<String>,
    val categoryCounts: Map<String, Int>,
    val selectedCategory: String,
    val onSelectCategory: (String) -> Unit
)

@Composable
fun DetailTrailersMenuModal(
    data: TrailersMenuModalData?,
    accentColor: Color,
    hazeState: HazeState?,
    onDismiss: () -> Unit
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val config = androidx.compose.ui.platform.LocalConfiguration.current

    var activeData by remember { mutableStateOf<TrailersMenuModalData?>(null) }
    if (data != null) {
        activeData = data
    }
    val targetData = activeData ?: return
    val isVisible = data != null

    val transition = androidx.compose.animation.core.updateTransition(
        targetState = isVisible,
        label = "TrailersMenuTransition"
    )

    if (transition.currentState || transition.targetState) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(99999f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .bounceClick(scaleDown = 1f, onClick = onDismiss)
            )

            var actualMenuHeightPx by remember { mutableStateOf(0f) }
            val triggerRect = targetData.triggerBounds
            val menuWidth = 210.dp
            val menuWidthPx = with(density) { menuWidth.toPx() }
            val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
            val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }
            val sideSafetyPx = with(density) { 16.dp.toPx() }
            val topSafetyPx = with(density) { 96.dp.toPx() }
            val bottomSafetyPx = with(density) { 100.dp.toPx() }
            val spacingPx = with(density) { 8.dp.toPx() }

            val menuHeightPx = if (actualMenuHeightPx > 0f) {
                actualMenuHeightPx
            } else {
                with(density) { (targetData.availableCategories.size * 38 + 12).dp.toPx() }
            }

            val minX = sideSafetyPx
            val maxX = screenWidthPx - sideSafetyPx - menuWidthPx
            val xPos = (triggerRect.right - menuWidthPx).coerceIn(minX, maxX.coerceAtLeast(minX))

            val openDownY = triggerRect.bottom + spacingPx
            val openUpY = triggerRect.top - spacingPx - menuHeightPx

            val minY = topSafetyPx
            val maxY = screenHeightPx - bottomSafetyPx - menuHeightPx
            val yPos = if (openDownY + menuHeightPx <= screenHeightPx - bottomSafetyPx) {
                openDownY
            } else if (openUpY >= topSafetyPx) {
                openUpY
            } else {
                openDownY.coerceIn(minY, maxY.coerceAtLeast(minY))
            }

            val isOpenUp = (yPos == openUpY)

            val blurAlpha by transition.animateFloat(
                transitionSpec = { tween(200) },
                label = "blurAlpha"
            ) { if (it) 0.85f else 0f }

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(200)) + expandVertically(
                    expandFrom = if (isOpenUp) Alignment.Bottom else Alignment.Top,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
                exit = fadeOut(animationSpec = tween(200)),
                modifier = Modifier
                    .offset { androidx.compose.ui.unit.IntOffset(xPos.toInt(), yPos.toInt()) }
                    .zIndex(100f)
            ) {
                Column(
                    modifier = Modifier
                        .onGloballyPositioned { coords ->
                            actualMenuHeightPx = coords.size.height.toFloat()
                        }
                        .width(menuWidth)
                        .clip(RoundedCornerShape(20.dp))
                        .then(
                            if (hazeState != null) {
                                Modifier.hazeGlass(
                                    state = hazeState,
                                    alpha = blurAlpha,
                                    shape = RoundedCornerShape(20.dp)
                                )
                            } else {
                                Modifier.background(Color(0xFF141414).copy(alpha = 0.95f))
                            }
                        )
                        .padding(vertical = 6.dp)
                ) {
                    targetData.availableCategories.forEach { cat ->
                        val count = targetData.categoryCounts[cat] ?: 0
                        val isSelected = targetData.selectedCategory == cat

                        val catLabel = when (cat) {
                            "Trailer" -> stringResource(R.string.detail_video_menu_trailer)
                            "Teaser" -> stringResource(R.string.detail_video_menu_teaser)
                            "Clip" -> stringResource(R.string.detail_video_menu_clip)
                            "Featurette" -> stringResource(R.string.detail_video_menu_featurette)
                            "Behind the Scenes" -> stringResource(R.string.detail_video_menu_bts)
                            "Bloopers" -> stringResource(R.string.detail_video_menu_bloopers)
                            "Opening Credits" -> stringResource(R.string.detail_video_menu_credits)
                            "Recap" -> stringResource(R.string.detail_video_menu_recap)
                            else -> cat
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick {
                                    targetData.onSelectCategory(cat)
                                    onDismiss()
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = catLabel,
                                color = if (isSelected) accentColor else Color.White,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            if (count > 0) {
                                Spacer(Modifier.width(10.dp))
                                Surface(
                                    color = if (isSelected) accentColor else Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "$count",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * DetailTrailers
 * Renders a horizontal list of YouTube trailers and video extras.
 * Features category filtering (Trailers, Bloopers, Featurettes, etc.),
 * and a lifecycle-aware WebView with auto-cleanup to prevent "ghost audio".
 */
@Composable
fun DetailTrailers(
    videos: List<Video> = emptyList(),
    trailers: List<String> = emptyList(),
    accentColor: Color,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier,
    isMenuOpen: Boolean = false,
    onMenuOpenChange: (Boolean) -> Unit = {},
    onOpenCategoryMenu: (TrailersMenuModalData) -> Unit = {}
) {
    val effectiveVideos = remember(videos, trailers) {
        if (videos.isNotEmpty()) {
            videos.filter { it.site == "YouTube" || it.site == "Vimeo" }
        } else {
            trailers.map { Video(key = it, site = "YouTube", type = "Trailer", name = "Trailer") }
        }
    }

    if (effectiveVideos.isEmpty()) return

    val categoryOrder = listOf("Trailer", "Teaser", "Clip", "Featurette", "Behind the Scenes", "Bloopers", "Opening Credits", "Recap")
    val availableCategories = remember(effectiveVideos) {
        effectiveVideos.map { it.type }.distinct().filter { it.isNotBlank() }
            .sortedBy { categoryOrder.indexOf(it).takeIf { idx -> idx != -1 } ?: 99 }
    }
    var selectedCategory by remember(availableCategories) {
        mutableStateOf(availableCategories.firstOrNull() ?: "Trailer")
    }
    val currentVideos = remember(effectiveVideos, selectedCategory) {
        effectiveVideos.filter { it.type == selectedCategory }.distinctBy { it.key }
    }

    var activeVideoKey by remember { mutableStateOf<String?>(null) }
    var lastDismissTime by remember { mutableStateOf(0L) }
    var pillButtonCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    LaunchedEffect(isMenuOpen) {
        if (!isMenuOpen) {
            lastDismissTime = System.currentTimeMillis()
        }
    }

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
            val titleText = when (selectedCategory) {
                "Trailer" -> stringResource(R.string.detail_official_trailers)
                "Teaser" -> stringResource(R.string.detail_video_title_teaser)
                "Clip" -> stringResource(R.string.detail_video_title_clip)
                "Featurette" -> stringResource(R.string.detail_video_title_featurette)
                "Behind the Scenes" -> stringResource(R.string.detail_video_title_bts)
                "Bloopers" -> stringResource(R.string.detail_video_title_bloopers)
                "Opening Credits" -> stringResource(R.string.detail_video_title_credits)
                "Recap" -> stringResource(R.string.detail_video_title_recap)
                else -> selectedCategory.uppercase()
            }

            Text(
                text = titleText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.5.sp
                ),
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.weight(1f, fill = false)
            )

            if (availableCategories.size > 1) {
                Box {
                    Surface(
                        color = accentColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                pillButtonCoords = coords
                            }
                            .bounceClick {
                                val now = System.currentTimeMillis()
                                val liveBounds = pillButtonCoords?.boundsInRoot()
                                if (now - lastDismissTime > 300L && liveBounds != null) {
                                    val counts = availableCategories.associateWith { cat ->
                                        effectiveVideos.count { it.type == cat && (it.site == "YouTube" || it.site == "Vimeo") }
                                    }
                                    onOpenCategoryMenu(
                                        TrailersMenuModalData(
                                            triggerBounds = liveBounds,
                                            availableCategories = availableCategories,
                                            categoryCounts = counts,
                                            selectedCategory = selectedCategory,
                                            onSelectCategory = { newCat ->
                                                selectedCategory = newCat
                                                activeVideoKey = null
                                            }
                                        )
                                    )
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = when (selectedCategory) {
                                    "Trailer" -> stringResource(R.string.detail_video_pill_trailer)
                                    "Teaser" -> stringResource(R.string.detail_video_pill_teaser)
                                    "Clip" -> stringResource(R.string.detail_video_pill_clip)
                                    "Featurette" -> stringResource(R.string.detail_video_pill_featurette)
                                    "Behind the Scenes" -> stringResource(R.string.detail_video_pill_bts)
                                    "Bloopers" -> stringResource(R.string.detail_video_pill_bloopers)
                                    "Opening Credits" -> stringResource(R.string.detail_video_pill_credits)
                                    "Recap" -> stringResource(R.string.detail_video_pill_recap)
                                    else -> selectedCategory.uppercase()
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp,
                                    letterSpacing = 1.2.sp
                                ),
                                color = accentColor
                            )
                            val arrowRotation by animateFloatAsState(
                                targetValue = if (isMenuOpen) -90f else 90f,
                                label = "arrowRotation"
                            )
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier
                                    .size(12.dp)
                                    .graphicsLayer {
                                        rotationZ = arrowRotation
                                    }
                            )
                        }
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
            userScrollEnabled = !isMenuOpen,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(currentVideos, key = { it.key }, contentType = { "trailer" }) { video ->
                val isActive = activeVideoKey == video.key
                Box(
                    modifier = Modifier
                        .width(340.dp)
                        .aspectRatio(16f / 9f)
                        .then(
                            if (!isActive && !isMenuOpen) {
                                Modifier.bounceClick { activeVideoKey = video.key }
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
                            YouTubePlayer(videoId = video.key)
                        } else {
                            TrailerThumbnail(video = video, accentColor = accentColor)
                        }
                    }
                }
            }
        }
    }
}

private fun getSimulatedDuration(video: Video): String {
    val hash = video.key.hashCode().absoluteValue
    val (minSeconds, maxSeconds) = when (video.type) {
        "Teaser" -> 30 to 75
        "Clip" -> 45 to 180
        "Featurette", "Behind the Scenes" -> 150 to 360
        "Bloopers" -> 120 to 300
        else -> 90 to 160
    }
    val totalSeconds = minSeconds + (hash % (maxSeconds - minSeconds + 1))
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
fun TrailerThumbnail(video: Video, accentColor: Color) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = "https://img.youtube.com/vi/${video.key}/hqdefault.jpg",
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        // Play Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f)),
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
        }

        // Bottom black gradient overlay for readable text
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(110.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        // Video Name text (or fallback to type/default)
        Text(
            text = video.name.ifBlank { video.type.ifBlank { stringResource(R.string.detail_official_trailer) } },
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                letterSpacing = 0.2.sp
            ),
            color = accentColor.copy(alpha = 0.95f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 18.dp, end = 64.dp, bottom = 14.dp)
        )

        // Duration Badge on bottom right
        val durationText = remember(video.key, video.type) {
            getSimulatedDuration(video)
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 14.dp)
                .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(
                text = durationText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = Color.White
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
