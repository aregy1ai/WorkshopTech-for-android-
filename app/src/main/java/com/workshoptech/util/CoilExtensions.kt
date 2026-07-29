package com.workshoptech.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size

@Composable
fun OptimizedImage(imagePath: String, contentDescription: String?, modifier: Modifier = Modifier, size: Dp = 128.dp, contentScale: ContentScale = ContentScale.Crop) {
    val context = LocalContext.current
    val imageLoader = coil.Coil.imageLoader(context)
    AsyncImage(
        model = ImageRequest.Builder(context).data(imagePath).size(Size(size.value.toInt(), size.value.toInt())).crossfade(true).build(),
        contentDescription = contentDescription, imageLoader = imageLoader, modifier = modifier, contentScale = contentScale
    )
}
