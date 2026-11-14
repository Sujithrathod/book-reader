package com.bookreader.app.ui.library

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bookreader.app.BookFormat
import com.bookreader.app.data.BookEntity
import com.bookreader.app.data.LibrarySort
import com.bookreader.app.reader.ReaderActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The library: a "Continue reading" card for the book in progress, format filters, and the
 * books as a grid of covers. Books slide into place when the list changes, covers react to
 * a press, and books being imported show as shimmering placeholders until they are ready.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(vm: LibraryViewModel = viewModel()) {
    val context = LocalContext.current
    val books by vm.books.collectAsState()
    val total by vm.totalBooks.collectAsState()
    val search by vm.search.collectAsState()
    val sort by vm.sort.collectAsState()
    val format by vm.format.collectAsState()
    val importing by vm.importing.collectAsState()
    val continueBook by vm.continueBook.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var searching by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf<BookEntity?>(null) }
    var toDelete by remember { mutableStateOf<BookEntity?>(null) }
    val grid = rememberLazyGridState()
    val atTop by remember { derivedStateOf { grid.firstVisibleItemIndex == 0 } }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        vm.import(uris)
    }
    val pickBooks = { picker.launch(BookFormat.pickerMimeTypes) }

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(Unit) { vm.openReader.collect { id -> context.startActivity(ReaderActivity.intent(context, id)) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searching) {
                        OutlinedTextField(
                            value = search,
                            onValueChange = { vm.search.value = it },
                            placeholder = { Text("Search title or author") },
                            singleLine = true,
                            shape = RoundedCornerShape(28.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Column {
                            Text("Library", fontWeight = FontWeight.Bold)
                            if (total > 0) {
                                Text(
                                    if (total == 1) "1 book" else "$total books",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (searching) {
                        IconButton(onClick = { searching = false; vm.search.value = "" }) { Icon(Icons.Rounded.Close, "Close search") }
                    } else {
                        IconButton(onClick = { searching = true }) { Icon(Icons.Rounded.Search, "Search") }
                    }
                    Box {
                        IconButton(onClick = { sortMenu = true }) { Icon(Icons.AutoMirrored.Rounded.Sort, "Sort") }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            LibrarySort.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(if (option == sort) "✓  ${option.label}" else "    ${option.label}") },
                                    onClick = { vm.sort.value = option; sortMenu = false },
                                )
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (total > 0) {
                ExtendedFloatingActionButton(
                    onClick = pickBooks,
                    icon = { Icon(Icons.Rounded.Add, null) },
                    text = { Text("Import") },
                    expanded = atTop, // shrinks to a round button while scrolling
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val list = books
        when {
            list == null -> Unit
            total == 0 && importing == 0 -> EmptyLibrary(Modifier.padding(padding), onImport = pickBooks)
            else -> LazyVerticalGrid(
                state = grid,
                columns = GridCells.Adaptive(minSize = 112.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(padding).fillMaxSize(),
            ) {
                val resume = continueBook
                if (resume != null && search.isBlank() && format == null) {
                    item(key = "continue", span = { GridItemSpan(maxLineSpan) }) {
                        ContinueCard(resume, Modifier.animateItem()) { vm.open(resume) }
                    }
                }
                item(key = "filters", span = { GridItemSpan(maxLineSpan) }) {
                    Row(Modifier.animateItem(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = format == null, onClick = { vm.format.value = null }, label = { Text("All") })
                        BookFormat.entries.forEach { f ->
                            FilterChip(selected = format == f.name, onClick = { vm.format.value = if (format == f.name) null else f.name }, label = { Text(f.name) })
                        }
                    }
                }
                items(importing, key = { "importing-$it" }) { ImportingCard(Modifier.animateItem()) }
                if (list.isEmpty() && importing == 0) {
                    item(key = "none", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            if (search.isNotBlank()) "No books match \"$search\"." else "No ${format ?: ""} books yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.animateItem().padding(vertical = 24.dp),
                        )
                    }
                }
                itemsIndexed(list, key = { _, book -> book.id }) { index, book ->
                    BookCard(book, index, Modifier.animateItem(), onOpen = { vm.open(book) }, onLongPress = { details = book })
                }
            }
        }
    }

    details?.let { book ->
        ModalBottomSheet(onDismissRequest = { details = null }) {
            BookDetails(
                book,
                onOpen = { details = null; vm.open(book) },
                onRemove = { details = null; toDelete = book },
            )
        }
    }

    toDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Remove this book?") },
            text = { Text("\"${book.title}\", its reading position and bookmarks will be removed from this phone. The original file you imported is not touched.") },
            confirmButton = { TextButton(onClick = { vm.delete(book); toDelete = null }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun EmptyLibrary(modifier: Modifier, onImport: () -> Unit) {
    // The book icon floats gently up and down.
    val float by rememberInfiniteTransition(label = "empty").animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float",
    )
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.graphicsLayer { translationY = float.dp.toPx() }.size(120.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Outlined.MenuBook, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Your library is empty", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Add DRM-free EPUB, PDF or TXT books from your phone.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onImport) {
            Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
            Text("Import books", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun rememberCover(book: BookEntity): ImageBitmap? {
    val cover by produceState<ImageBitmap?>(null, book.coverPath) {
        value = book.coverPath?.let { path ->
            withContext(Dispatchers.IO) { BitmapFactory.decodeFile(path)?.asImageBitmap() }
        }
    }
    return cover
}

/** A book's cover, or its title on the app's green when it has none. The picture fades in. */
@Composable
private fun Cover(book: BookEntity, modifier: Modifier, titleLines: Int = 5) {
    val cover = rememberCover(book)
    val shown by animateFloatAsState(if (cover != null) 1f else 0f, tween(300), label = "cover")
    Box(modifier.background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
        Text(
            book.title,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            maxLines = titleLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(10.dp),
        )
        if (cover != null) {
            Image(cover, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().graphicsLayer { alpha = shown })
        }
    }
}

/** A bar that slides to its value when the book's progress changes. */
@Composable
private fun ProgressBar(progress: Double, modifier: Modifier = Modifier) {
    val shown by animateFloatAsState(progress.toFloat().coerceIn(0f, 1f), tween(700, easing = FastOutSlowInEasing), label = "progress")
    Box(modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))) {
        Box(Modifier.fillMaxWidth(shown).height(5.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.primary))
    }
}

@Composable
private fun ContinueCard(book: BookEntity, modifier: Modifier, onOpen: () -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
            .clickable(onClickLabel = "Continue reading ${book.title}", onClick = onOpen)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(book, Modifier.width(76.dp).aspectRatio(2f / 3f).shadow(6.dp, RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp)), titleLines = 3)
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Text("Continue reading", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            book.author?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                ProgressBar(book.progress, Modifier.weight(1f))
                Text("${(book.progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 10.dp))
            }
            FilledTonalButton(onClick = onOpen, modifier = Modifier.padding(top = 8.dp)) {
                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                Text("Continue", modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

/** Stands in for a book while it is being imported: a cover-shaped box with a moving shine. */
@Composable
private fun ImportingCard(modifier: Modifier) {
    val shift by rememberInfiniteTransition(label = "import").animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1200)),
        label = "shine",
    )
    val base = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val shine = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(base, shine, base), start = Offset(shift * 400f, 0f), end = Offset(shift * 400f + 300f, 300f))),
            contentAlignment = Alignment.Center,
        ) {
            Text("Importing…", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth(0.8f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(base))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookCard(book: BookEntity, index: Int, modifier: Modifier, onOpen: () -> Unit, onLongPress: () -> Unit) {
    // Each card fades and rises into place, one after another, the first time it is shown.
    val appear = remember(book.id) { Animatable(0f) }
    LaunchedEffect(book.id) { appear.animateTo(1f, tween(380, delayMillis = index.coerceAtMost(8) * 45, easing = FastOutSlowInEasing)) }
    // The card shrinks a little under the finger and springs back.
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, spring(dampingRatio = 0.55f, stiffness = 500f), label = "press")

    Column(
        modifier
            .graphicsLayer {
                alpha = appear.value
                translationY = (1f - appear.value) * 28.dp.toPx()
                scaleX = scale
                scaleY = scale
            }
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onOpen,
                onLongClick = onLongPress,
                onLongClickLabel = "Book details",
            ),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).shadow(if (pressed) 2.dp else 6.dp, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp))) {
            Cover(book, Modifier.fillMaxSize())
            Text(
                book.format,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(book.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        book.author?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(6.dp))
        if (book.progress > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressBar(book.progress, Modifier.weight(1f))
                Text(
                    "${(book.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        } else {
            Text(
                "New",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

/** Shown on a long-press: the book's details with Open and Remove. */
@Composable
private fun BookDetails(book: BookEntity, onOpen: () -> Unit, onRemove: () -> Unit) {
    val dates = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Cover(book, Modifier.width(88.dp).aspectRatio(2f / 3f).shadow(6.dp, RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp)), titleLines = 3)
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                book.author?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Text(
                    listOfNotNull(
                        book.format,
                        "Added ${dates.format(Date(book.addedAt))}",
                        book.lastOpenedAt?.let { "Last read ${dates.format(Date(it))}" },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        Row(Modifier.padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            ProgressBar(book.progress, Modifier.weight(1f))
            Text(if (book.progress > 0) "${(book.progress * 100).toInt()}% read" else "Not started", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 12.dp))
        }
        Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onRemove, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.DeleteOutline, null, modifier = Modifier.size(18.dp))
                Text("Remove", modifier = Modifier.padding(start = 6.dp))
            }
            Button(onClick = onOpen, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                Text(if (book.progress > 0) "Continue" else "Read", modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}
