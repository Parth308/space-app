package sk.kasper.ui_playground

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Bottom
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.Top
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.findNavController
import com.google.accompanist.coil.rememberCoilPainter
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.navigationBarsHeight
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.insets.statusBarsPadding
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import sk.kasper.ui_common.BaseFragment
import sk.kasper.ui_common.settings.SettingsManager
import sk.kasper.ui_common.theme.SpaceTheme
import sk.kasper.ui_common.ui.InsetAwareTopAppBar
import java.util.*
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class ComposePlaygroundFragment : BaseFragment() {

    @Inject
    lateinit var settingsManager: SettingsManager

    enum class PlaygroundTab(val text: String) {
        TYPE("type"),
        COLOR("color"),
        SHAPE("shape"),
        COMPONENTS("components"),
        ANIMATIONS("anim"),
        FILTER("filter"),
    }

    private val defaultTab = PlaygroundTab.FILTER

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SpaceTheme {
                    ProvideWindowInsets {
                        val systemUiController = rememberSystemUiController()
                        SideEffect {
                            systemUiController.setSystemBarsColor(
                                color = Color.Transparent,
                                darkIcons = false
                            )
                        }

                        var showInset by remember { mutableStateOf(false) }
                        if (showInset) {
                            InsetsScreen { showInset = !showInset }
                        } else {
                            Scaffold(topBar = { PlaygroundTopAppBar { showInset = !showInset } }) {
                                PlaygroundTabs { screen ->
                                    when (screen) {
                                        PlaygroundTab.TYPE -> TypeScreen()
                                        PlaygroundTab.COLOR -> ColorsScreen()
                                        PlaygroundTab.COMPONENTS -> ComponentsScreen()
                                        PlaygroundTab.SHAPE -> ShapeScreen()
                                        PlaygroundTab.ANIMATIONS -> AnimationsScreen()
                                        PlaygroundTab.FILTER -> FilterScreen()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    @Preview
    private fun FilterScreenPreview() {
        SpaceTheme {
            FilterScreen()
        }
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    private fun FilterRow(
        map: Map<String, Boolean>,
        foreign: Map<String, Boolean>,
        onClearAllClick: () -> Unit = { },
        onTagSelected: (String, Boolean) -> Unit = { _, _ -> }
    ) {
        Surface {
            Row(modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()) {
                AnimatedVisibility(
                    visible = map.any { it.value },
                    enter = fadeIn() + expandHorizontally()
                ) {
                    val shape = MaterialTheme.shapes.small.copy(all = CornerSize(percent = 50))
                    Box(modifier = Modifier
                        .size(32.dp)
                        .padding(2.dp)
                        .border(
                            2.dp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                            shape = shape
                        )
                        .clip(shape = shape)
                        .clickable { onClearAllClick() }
                        .padding(4.dp)) {
                        Text(
                            text = "X",
                            modifier = Modifier.align(Center),
                            style = MaterialTheme.typography.body2
                        )
                    }
                }

                map.forEach { (name, selected) ->
                    Tag(name, selected = selected, onTagSelected)
                }

                val hasForeign = foreign.isNotEmpty()
//                val animationSpec: AnimationSpec<Float> =
//                    SpringSpec(stiffness = Spring.StiffnessMedium, dampingRatio = 0.8f)
                val animationSpec: AnimationSpec<Float> =
                    tween(easing = LinearOutSlowInEasing)
                val amount by animateFloatAsState(
                    targetValue = if (hasForeign) 0.0f else 1.0f,
                    animationSpec = animationSpec
                )

                if (hasForeign) {
                    Row(modifier = Modifier.placeBehindHorizontal(amount)) {
                        foreign.forEach { (name, selected) ->
                            Tag(name, selected = selected, onTagSelected)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Tag(text: String, selected: Boolean, onTagSelected: (String, Boolean) -> Unit) {
        val shape = MaterialTheme.shapes.small.copy(all = CornerSize(percent = 50))
        val alpha = if (selected) 0.4f else 0.0f
        Text(
            text,
            style = MaterialTheme.typography.body2,
            modifier = Modifier
                .padding(2.dp)
                .border(
                    2.dp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                    shape = shape
                )
                .clip(shape = shape)
                .clickable { onTagSelected(text, !selected) }
                .background(color = MaterialTheme.colors.onSurface.copy(alpha = alpha))
                .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 3.dp)
        )
    }

    private data class FilterState(
        val a: List<String> = listOf("ISS", "Falcon"),
        val aVisible: Boolean = true,
        val b: List<String> = emptyList(),
        val bVisible: Boolean = false,
        val c: List<String> = emptyList(),
        val cVisible: Boolean = false,
        val clearVisible: Boolean = false
    )

    @Composable
    private fun FilterScreen() {
        val topLevel = listOf("ISS", "Falcon")

        val map = remember {
            mutableStateMapOf<String, Boolean>().apply {
                topLevel.forEach {
                    put(it, false)
                }
            }
        }

        val mapToShow = if (map["ISS"] == true) {
            mapOf("ISS" to true)
        } else {
            map
        }

        val foreign = if (map["ISS"] == true) {
            mapOf("Crewd" to false, "Soyuz" to false)
        } else {
            emptyMap()
        }

        FilterRow(mapToShow, foreign, onClearAllClick = {
            map.clear()
            map.apply {
                topLevel.forEach {
                    put(it, false)
                }
            }
        }) { n, s -> map[n] = s }
    }

    enum class AnimState {
        A, B
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    private fun AnimationsScreen() {
        var flag by remember {
            mutableStateOf(true)
        }

        val color by animateColorAsState(if (flag) Color(0xFF03A9F4) else Color(0xFF8BC34A))
        val size by animateDpAsState(targetValue = if (flag) 48.dp else 64.dp)
        var animState by remember {
            mutableStateOf(AnimState.A)
        }

        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = {
                flag = !flag
                animState = if (animState == AnimState.A) AnimState.B else AnimState.A
            }) {
                Text(text = "Toggle")
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color)
            ) {
            }
            Row(modifier = Modifier.height(72.dp)) {
                if (flag) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colors.primary)
                    ) {
                    }
                }
            }

            Row(modifier = Modifier.height(72.dp)) {
                Box(
                    modifier = Modifier
                        .size(size)
                        .background(MaterialTheme.colors.primary)
                ) {
                }
            }

            Row(modifier = Modifier.height(72.dp)) {
                val transition = updateTransition(targetState = animState, label = "aaa")

                val col by transition.animateColor(label = "") {
                    when (it) {
                        AnimState.A -> Color(0xFF9C27B0)
                        AnimState.B -> Color(0xFFFF5722)
                    }
                }
                val siz by transition.animateDp(label = "") {
                    when (it) {
                        AnimState.A -> 32.dp
                        AnimState.B -> 48.dp
                    }
                }

                Box(
                    modifier = Modifier
                        .size(siz)
                        .background(col)
                ) {
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Green)
            ) {
                BoxWithConstraints() {
                    Text("My minWidth is $minWidth while my maxWidth is $maxWidth")
                }
            }
        }
    }

    private fun Modifier.placeBehindHorizontal(amount: Float) = layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)

        layout(constraints.maxWidth, placeable.height) {
            val fl: Float = (constraints.maxWidth) * amount
            placeable.place(fl.roundToInt(), 0)
        }
    }

    @Composable
    private fun PlaygroundTabs(onTabSelected: @Composable (PlaygroundTab) -> Unit) {
        Column {
            val selectedPlaygroundTab: MutableState<PlaygroundTab> =
                remember { mutableStateOf(defaultTab) }
            Surface(
                color = MaterialTheme.colors.primarySurface
            ) {
                ScrollableTabRow(
                    backgroundColor = Color.Transparent,
                    modifier = Modifier.navigationBarsPadding(bottom = false),
                    selectedTabIndex = selectedPlaygroundTab.value.ordinal
                ) {
                    PlaygroundTab.values().forEach { screen ->
                        Tab(
                            modifier = Modifier.height(56.dp),
                            selected = selectedPlaygroundTab.value === screen,
                            onClick = { selectedPlaygroundTab.value = screen },
                        ) {
                            Text(
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.button,
                                text = screen.text.toUpperCase(Locale.getDefault())
                            )
                        }
                    }
                }
            }
            LazyColumn {
                item {
                    onTabSelected(selectedPlaygroundTab.value)
                }
            }
        }
    }

    @Composable
    private fun ComposeView.PlaygroundTopAppBar(onShowInset: () -> Unit = {}) {
        InsetAwareTopAppBar(
            elevation = 0.dp,
            title = {
                Text(
                    text = "Compose playground",
                    maxLines = 1,
                    style = MaterialTheme.typography.h6,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                IconButton(onClick = { findNavController().popBackStack() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = "back",
                    )
                }
            },
            actions = {
                IconButton(onClick = { settingsManager.toggleTheme() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_tonality),
                        contentDescription = "Toggle theme",
                    )
                }

                IconButton(onClick = { onShowInset() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_live_tv),
                        contentDescription = "Insets",
                    )
                }
            }
        )
    }

    @Composable
    fun TypeScreen() {
        listOf(
            "headline1" to MaterialTheme.typography.h1,
            "headline2" to MaterialTheme.typography.h2,
            "headline3" to MaterialTheme.typography.h3,
            "headline4" to MaterialTheme.typography.h4,
            "headline5" to MaterialTheme.typography.h5,
            "headline6" to MaterialTheme.typography.h6,
            "subtitle1" to MaterialTheme.typography.subtitle1,
            "subtitle2" to MaterialTheme.typography.subtitle2,
            "body1" to MaterialTheme.typography.body1,
            "body2" to MaterialTheme.typography.body2,
            "button" to MaterialTheme.typography.button,
            "caption" to MaterialTheme.typography.caption,
            "overline" to MaterialTheme.typography.overline,
        ).forEach { (name, textStyle) ->
            Text(
                text = name.capitalize(Locale.getDefault()),
                style = textStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    @Composable
    fun ColorsScreen() {
        listOf(
            "primary" to MaterialTheme.colors.primary,
            "primaryVariant" to MaterialTheme.colors.primaryVariant,
            "secondary" to MaterialTheme.colors.secondary,
            "secondaryVariant" to MaterialTheme.colors.secondaryVariant,
            "background" to MaterialTheme.colors.background,
            "surface" to MaterialTheme.colors.surface,
            "error" to MaterialTheme.colors.error,
            "onPrimary" to MaterialTheme.colors.onPrimary,
            "onSecondary" to MaterialTheme.colors.onSecondary,
            "onBackground" to MaterialTheme.colors.onBackground,
            "onSurface" to MaterialTheme.colors.onSurface,
            "onError" to MaterialTheme.colors.onError,
        ).forEach { (name, color) ->
            CompositionLocalProvider(LocalContentColor provides Color(0xFF2C9607)) {
                Surface(
                    modifier = Modifier
                        .height(64.dp)
                        .fillMaxWidth(),
                    color = color
                ) {
                    Row(
                        modifier = Modifier
                            .padding(8.dp)
                            .navigationBarsPadding(bottom = false)
                    ) {
                        Text(
                            modifier = Modifier
                                .weight(1f)
                                .align(Top),
                            text = name.capitalize(Locale.getDefault()),
                            style = MaterialTheme.typography.body1
                        )
                        Text(
                            modifier = Modifier.align(Bottom),
                            text = "0x" + color.toArgb().toUInt().toString(16).toUpperCase(
                                Locale.getDefault()
                            ),
                            style = MaterialTheme.typography.body2
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.navigationBarsHeight())
    }

    @Composable
    fun ComponentsScreen() {
        Column(
            Modifier.navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { }) {
                    Text(text = "text".toUpperCase(Locale.getDefault()))
                }
                OutlinedButton(onClick = { }) {
                    Text(text = "outlined".toUpperCase(Locale.getDefault()))
                }
                Button(onClick = { }) {
                    Text(text = "button".toUpperCase(Locale.getDefault()))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                elevation = 4.dp, modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Surface(
                    elevation = 8.dp, modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Surface(
                        elevation = 16.dp, modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(32.dp)
                    ) {

                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ImageBox(
                Modifier.requiredHeight(128.dp),
                contentScale = ContentScale.FillHeight,
                contentDescription = "FillHeight"
            )

            Spacer(modifier = Modifier.height(16.dp))

            ImageBox(
                Modifier
                    .requiredHeight(128.dp)
                    .requiredWidth(128.dp),
                contentScale = ContentScale.Fit,
                contentDescription = "Fit"
            )

            Spacer(modifier = Modifier.height(16.dp))

            ImageBox(
                Modifier
                    .requiredHeight(128.dp)
                    .requiredWidth(128.dp),
                contentScale = ContentScale.Crop,
                contentDescription = "Crop"
            )
        }
    }

    @Composable
    private fun ImageBox(
        modifier: Modifier = Modifier,
        contentScale: ContentScale,
        contentDescription: String
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            modifier = modifier
                .border(2.dp, Color.LightGray)
        ) {
            Box(contentAlignment = BottomCenter) {
                Image(
                    painter = rememberCoilPainter(request = "https://placebear.com/640/420"),
                    contentScale = contentScale,
                    contentDescription = contentDescription
                )
                Text(
                    text = contentDescription,
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = MaterialTheme.colors.onPrimary
                )
            }
        }
    }

    @Composable
    fun ShapeScreen() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ShapeDemoSurface(MaterialTheme.shapes.small, "Shape small")

            ShapeDemoSurface(
                MaterialTheme.shapes.medium,
                "Shape medium",
                width = 200.dp,
                height = 150.dp
            )

            ShapeDemoSurface(
                MaterialTheme.shapes.large,
                "Shape large",
                width = 300.dp,
                height = 200.dp
            )
        }
    }

    @Composable
    private fun ShapeDemoSurface(
        shape: Shape,
        description: String,
        width: Dp = 0.dp,
        height: Dp = 0.dp
    ) {
        Surface(
            shape = shape,
            elevation = 8.dp,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colors.background
        ) {
            Box(
                modifier = Modifier
                    .requiredWidthIn(min = width)
                    .requiredHeightIn(min = height)
                    .padding(16.dp),
                contentAlignment = Center
            ) {
                Text(text = description, style = MaterialTheme.typography.h5)
            }
        }
    }

    @Composable
    private fun InsetsScreen(onClick: () -> Unit) {
        Surface(modifier = Modifier
            .fillMaxSize()
            .clickable { onClick() }) {

            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colors.surface)
                        .border(4.dp, Color.Blue)
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .statusBarsPadding()
                            .navigationBarsPadding(bottom = false)
                            .fillMaxWidth()
                            .requiredHeight(72.dp)
                            .background(Color.Green)
                    ) {
                    }
                }

                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colors.surface)
                        .border(4.dp, Color.Blue)
                        .fillMaxWidth()
                        .align(BottomCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .fillMaxWidth()
                            .requiredHeight(72.dp)
                            .background(Color.Green)
                    ) {
                    }
                }
            }
        }
    }
}