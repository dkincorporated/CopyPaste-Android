package dev.dkong.copypaste.composables

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.dkong.copypaste.R

@Composable
fun SettingsInfoText(info: String, horizontalPadding: Dp = 16.dp) {
    Text(
        info,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
    )
}

@Composable
fun SettingsInfoFootnote(info: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Icon(
            Icons.Outlined.Info,
            Icons.Outlined.Info.name,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        Text(
            info,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun HeadlineToggleableSettingsItem(
    name: String,
    checked: MutableState<Boolean>,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(32.dp))
            .clickable {
//                checked.value = !checked.value
                onCheckedChange(!checked.value)
            }
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked.value,
            onCheckedChange = { newChecked ->
//                checked.value = newChecked
                onCheckedChange(newChecked)
            }
        )
    }
}

@Composable
internal fun SettingsItemBase(
    name: String,
    onClick: () -> Unit,
    description: String? = null,
    startContent: @Composable (BoxScope.() -> Unit)? = null,
    endContent: @Composable (BoxScope.() -> Unit)? = null,
    showDivider: Boolean? = null,
    horizontalPadding: Dp = 0.dp,
    isRounded: Boolean = false
) {
    val rowModifier = if (isRounded) {
        Modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = 16.dp)
    } else {
        Modifier
//            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = 16.dp)
    }


    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = rowModifier
    ) {
        startContent?.let {
            Box(content = it, modifier = Modifier.padding(end = horizontalPadding))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        endContent?.let {
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(horizontalPadding / 2),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showDivider == true) VerticalDivider()
                Box(content = it)
            }
        }
    }
}

@Composable
fun SettingsItem(
    name: String,
    onClick: () -> Unit,
    description: String? = null,
    @DrawableRes icon: Int? = null,
    iconDescription: String? = null,
    actionContent: @Composable (BoxScope.() -> Unit)? = null,
    showDivider: Boolean? = null,
    horizontalPadding: Dp = 16.dp,
    iconHasCircle: Boolean = true,
    isRounded: Boolean = false
) {
    SettingsItemBase(
        name = name,
        onClick = onClick,
        description = description,
        startContent = if (icon == null) null else {
            {
                Box(
                    modifier = if (iconHasCircle) {
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    } else {
                        Modifier.size(40.dp)
                    }
                ) {
                    Image(
                        painter = painterResource(id = icon),
                        contentDescription = iconDescription,
                        colorFilter = ColorFilter.tint(
                            if (iconHasCircle) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .size(32.dp)
                            .padding(4.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        },
        endContent = actionContent,
        horizontalPadding = horizontalPadding,
        isRounded = isRounded,
        showDivider = showDivider
    )
}

@Composable
fun RadioButtonSettingsItem(
    name: String,
    id: String,
    onChecked: (String) -> Unit,
    description: String? = null,
    onOptionsClicked: (() -> Unit)? = null,
    horizontalPadding: Dp = 16.dp,
    isRounded: Boolean = false,
    selected: MutableState<String>,
    enabled: (MutableState<Boolean>)? = null
) {
    SettingsItemBase(
        name = name,
        onClick = {
            if (enabled?.value != false) onChecked(id)
        },
        description = description,
        startContent = {
            RadioButton(
                selected = selected.value == id,
                onClick = {
                    selected.value = id
                    onChecked(id)
                },
                enabled = enabled?.value ?: true
            )
        },
        endContent = if (onOptionsClicked == null) null else {
            {
                IconButton(onClick = onOptionsClicked) {
                    Image(
                        painterResource(id = R.drawable.fancy_outlined_settings),
                        contentDescription = "Device settings",
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        },
        showDivider = onOptionsClicked != null,
        horizontalPadding = horizontalPadding,
        isRounded = isRounded
    )
}

@Composable
fun SwitchSettingsItem(
    name: String,
    onClick: (() -> Unit)? = null,
    description: String? = null,
    @DrawableRes icon: Int? = null,
    iconDescription: String? = null,
    selected: MutableState<Boolean>,
    enabled: (MutableState<Boolean>)? = null,
    onSwitchChecked: (() -> Unit)? = null,
    showDivider: Boolean = true,
    horizontalPadding: Dp = 16.dp,
    iconHasCircle: Boolean = true,
    isRounded: Boolean = false
) {
    SettingsItem(
        name = name,
        onClick = if (onClick != null) onClick else {
            {
                selected.value = !selected.value
            }
        },
        description = description,
        icon = icon,
        iconDescription = iconDescription,
        actionContent = {
            Switch(
                checked = selected.value,
                onCheckedChange = { newValue ->
                    selected.value = newValue
                    onSwitchChecked?.let {
                        onSwitchChecked()
                    }
                },
                enabled = enabled?.value != false
            )
        },
        showDivider = showDivider,
        horizontalPadding = horizontalPadding,
        iconHasCircle = iconHasCircle,
        isRounded = isRounded
    )
}