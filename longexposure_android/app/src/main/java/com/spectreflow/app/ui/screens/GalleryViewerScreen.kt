package com.spectreflow.app.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectreflow.app.R
import com.spectreflow.app.storage.AppPhoto
import com.spectreflow.app.storage.PhotoRepository
import com.spectreflow.app.ui.theme.AccentEmerald
import com.spectreflow.app.ui.theme.BgObsidian
import com.spectreflow.app.ui.theme.TextPrimary
import com.spectreflow.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full-screen gallery viewer displaying photos captured by SpectreFlow.
 * Shows the latest photo first with horizontal swiping across the collection.
 * Includes Back, Share, and Delete actions with live MediaStore synchronization.
 */
@Composable
fun GalleryViewerScreen(
    photos: List<AppPhoto>,
    photoRepository: PhotoRepository,
    onBack: () -> Unit,
    onSharePhoto: (Uri) -> Unit,
    onDeletePhoto: (AppPhoto) -> Unit,
    modifier: Modifier = Modifier
) {
    if (photos.isEmpty()) {
        LaunchedEffect(Unit) {
            onBack()
        }
        return
    }

    val pagerState = rememberPagerState(initialPage = 0) { photos.size }
    var photoToDelete by remember { mutableStateOf<AppPhoto?>(null) }

    val currentPhoto = photos.getOrNull(pagerState.currentPage) ?: photos.first()
    val formattedDate = remember(currentPhoto.dateAddedSeconds) {
        val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.US)
        sdf.format(Date(currentPhoto.dateAddedSeconds * 1000L))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgObsidian)
    ) {
        // Horizontal Swiping Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val photo = photos[pageIndex]
            var loadedBitmap by remember(photo.id) { mutableStateOf<Bitmap?>(null) }
            var isLoading by remember(photo.id) { mutableStateOf(true) }

            LaunchedEffect(photo.id) {
                isLoading = true
                loadedBitmap = photoRepository.loadFullBitmap(photo.uri)
                isLoading = false
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                if (loadedBitmap != null) {
                    Image(
                        bitmap = loadedBitmap!!.asImageBitmap(),
                        contentDescription = "Photo ${pageIndex + 1} of ${photos.size}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (isLoading) {
                    CircularProgressIndicator(
                        color = AccentEmerald,
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    Text(
                        text = "Unable to load image",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0x991E2228))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = TextPrimary
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${pagerState.currentPage + 1} of ${photos.size}",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = formattedDate,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.size(42.dp))
        }

        // Bottom Action Bar (Share & Delete)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp, start = 32.dp, end = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Share Button
            Button(
                onClick = { onSharePhoto(currentPhoto.uri) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xCC1E2228)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .padding(end = 8.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.action_share), color = TextPrimary, fontWeight = FontWeight.SemiBold)
            }

            // Delete Button
            Button(
                onClick = { photoToDelete = currentPhoto },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xCC2A1616)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .padding(start = 8.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Delete", color = Color(0xFFFF8A80), fontWeight = FontWeight.SemiBold)
            }
        }

        // Delete Confirmation Dialog
        photoToDelete?.let { photo ->
            AlertDialog(
                onDismissRequest = { photoToDelete = null },
                title = {
                    Text(text = "Delete Photo", color = Color.White, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete this long exposure photo? This action cannot be undone.",
                        color = Color(0xFFCFD8DC),
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val target = photoToDelete
                            photoToDelete = null
                            if (target != null) {
                                onDeletePhoto(target)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                    ) {
                        Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { photoToDelete = null }) {
                        Text(stringResource(R.string.action_close), color = Color.White)
                    }
                },
                containerColor = Color(0xFF1E2228),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}
