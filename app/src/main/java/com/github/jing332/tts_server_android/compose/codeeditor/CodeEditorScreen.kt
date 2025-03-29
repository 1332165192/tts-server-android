package com.github.jing332.tts_server_android.compose.codeeditor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.jing332.common.utils.longToast
import com.github.jing332.compose.ComposeExtensions.clickableRipple
import com.github.jing332.compose.widgets.AppTooltip
import com.github.jing332.compose.widgets.CheckedMenuItem
import com.github.jing332.compose.widgets.LongClickIconButton
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.conf.CodeEditorConfig
import com.github.jing332.tts_server_android.ui.AppActivityResultContracts
import com.github.jing332.tts_server_android.ui.FilePickerActivity
import com.github.jing332.tts_server_android.ui.view.AppDialogs.displayErrorDialog
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentListener
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.launch


fun CodeEditor.string(): String = this.text.toString()

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CodeEditorScreen(
    title: @Composable () -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onLongClickSave: () -> Unit = {},
    onUpdate: (CodeEditor) -> Unit,
    onSaveFile: (() -> Pair<String, ByteArray>)?,

    onDebug: () -> Unit,
    onRemoteAction: (name: String, body: ByteArray?) -> Unit = { _, _ -> },

    vm: CodeEditorViewModel = viewModel(),

    debugIconContent: @Composable () -> Unit = {},
    onLongClickMore: () -> Unit = {},
    onLongClickMoreLabel: String? = null,
    actions: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit = {},
) {
    var codeEditor by remember { mutableStateOf<CodeEditor?>(null) }

    var showThemeDialog by remember { mutableStateOf(false) }
    if (showThemeDialog)
        ThemeSettingsDialog { showThemeDialog = false }

    var showRemoteSyncDialog by remember { mutableStateOf(false) }
    if (showRemoteSyncDialog)
        RemoteSyncSettings { showRemoteSyncDialog = false }

    val fileSaver =
        rememberLauncherForActivityResult(AppActivityResultContracts.filePickerActivity()) {
        }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(vm) {
        if (CodeEditorConfig.isRemoteSyncEnabled.value)
            vm.startSyncServer(
                port = CodeEditorConfig.remoteSyncPort.value,
                onPush = { codeEditor?.setText(it) },
                onPull = { codeEditor?.text.toString() },
                onDebug = onDebug,
                onAction = onRemoteAction
            )

        scope.launch {
            vm.error.collect {
                when (it) {
                    Error.Empty -> {}
                    is Error.Other -> {
                        context.displayErrorDialog(t = it.e)
                    }

                    Error.PortConflict -> {
                        context.longToast("RemoteSync: port conflict!")
                    }

                    is Error.Socket -> {
                        context.longToast("RemoteSync: ${it.message}")
                    }
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0,0,0,0),
        topBar = {
            TopAppBar(title = title, navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.nav_back)
                    )
                }
            },
                actions = {
                    IconButton(onClick = onDebug) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = stringResource(id = R.string.debug)
                        )
                        debugIconContent()
                    }

                    LongClickIconButton(
                        onClick = { codeEditor?.undo() },
                        onLongClickLabel = stringResource(R.string.redo),
                        onLongClick = { codeEditor?.redo() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = stringResource(id = R.string.undo)
                        )
                    }

                    AppTooltip(tooltip = stringResource(R.string.format_code)) {
                        IconButton(onClick = {
                            codeEditor?.let {
                                val newCode = vm.formatCode(it.string())
                                it.setText(newCode)
                            }
                        }) {
                            Icon(Icons.Default.Code, stringResource(id = R.string.format_code))
                        }
                    }
                    LongClickIconButton(onClick = onSave, onLongClick = onLongClickSave) {
                        Icon(
                            Icons.Filled.Save,
                            contentDescription = stringResource(id = R.string.save)
                        )
                    }

                    var showOptions by remember { mutableStateOf(false) }

                    LongClickIconButton(
                        onClick = { showOptions = true },
                        onLongClick = onLongClickMore,
                        onLongClickLabel = onLongClickMoreLabel
                    ) {
                        Icon(Icons.Default.MoreVert, stringResource(id = R.string.more_options))

                        DropdownMenu(
                            expanded = showOptions,
                            onDismissRequest = { showOptions = false }) {
                            if (onSaveFile != null)
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.save_as_file)) },
                                    onClick = {
                                        onSaveFile.invoke().let {
                                            fileSaver.launch(
                                                FilePickerActivity.RequestSaveFile(
                                                    fileName = it.first,
                                                    fileBytes = it.second
                                                )
                                            )
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.AutoMirrored.Filled.InsertDriveFile,
                                            null
                                        )
                                    }
                                )

                            var syncEnabled by remember { CodeEditorConfig.isRemoteSyncEnabled }
                            CheckedMenuItem(
                                text = { Text(stringResource(id = R.string.remote_sync_service)) },
                                checked = syncEnabled,
                                onClick = { showRemoteSyncDialog = true },
                                onClickCheckBox = { syncEnabled = it },
                                leadingIcon = {
                                    Icon(Icons.Default.SettingsRemote, null)
                                }
                            )

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.theme)) },
                                onClick = { showThemeDialog = true },
                                leadingIcon = { Icon(Icons.Default.ColorLens, null) }
                            )

                            var wordWrap by remember { CodeEditorConfig.isWordWrapEnabled }
                            CheckedMenuItem(
                                text = { Text(stringResource(id = R.string.word_wrap)) },
                                checked = wordWrap,
                                onClick = { wordWrap = it },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Default.WrapText, null)
                                }
                            )

                            actions { showOptions = false }
                        }

                    }
                }
            )
        }
    ) { paddingValues ->
        val theme by remember { CodeEditorConfig.theme }
        LaunchedEffect(codeEditor, theme) {
            codeEditor?.helper()?.setTheme(theme)
        }

        val wordWrap by remember { CodeEditorConfig.isWordWrapEnabled }
        LaunchedEffect(codeEditor, wordWrap) {
            codeEditor?.isWordwrap = wordWrap
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CodeEditor(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(), onUpdate = {
                    it.isUndoEnabled = true
                    it.text.addContentListener(object : ContentListener {
                        override fun beforeReplace(content: Content) {

                        }

                        override fun afterInsert(
                            content: Content,
                            startLine: Int,
                            startColumn: Int,
                            endLine: Int,
                            endColumn: Int,
                            insertedContent: CharSequence,
                        ) {
                        }

                        override fun afterDelete(
                            content: Content,
                            startLine: Int,
                            startColumn: Int,
                            endLine: Int,
                            endColumn: Int,
                            deletedContent: CharSequence,
                        ) {
                        }
                    })
                    codeEditor = it

                    onUpdate(it)
                }
            )

            val symbolMap = remember {
                linkedMapOf(
                    "\t" to "TAB",
                    "=" to "=",
                    ">" to ">",
                    "{" to "{",
                    "}" to "}",
                    "(" to "(",
                    ")" to ")",
                    "," to ",",
                    "." to ".",
                    ";" to ";",
                    "'" to "'",
                    "\"" to "\"",
                    "?" to "?",
                    "+" to "+",
                    "-" to "-",
                    "*" to "*",
                    "/" to "/",
                )
            }

            HorizontalDivider(thickness = 1.dp)
            LazyRow(
                Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .imePadding()
                    .then(
                        if (WindowInsets.isImeVisible) Modifier else Modifier.navigationBarsPadding()
                    )
            ) {
                items(symbolMap.toList()) {
                    Box(
                        Modifier
                            .clickableRipple {
                                codeEditor?.let { editor ->
                                    val text = it.first
                                    if (editor.isEditable)
                                        if ("\t" == text && editor.snippetController.isInSnippet())
                                            editor.snippetController.shiftToNextTabStop()
                                        else
                                            editor.insertText(text, 1)
                                }
                            }) {
                        Text(
                            text = it.second,
                            Modifier
                                .minimumInteractiveComponentSize()
                                .align(Alignment.Center)
                        )
                    }
                }

            }
        }
    }
}