@file:OptIn(ExperimentalMaterial3Api::class)

package dev.dkong.copypaste.screens.home

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.dkong.copypaste.composables.SectionHeading
import dev.dkong.copypaste.screens.RootScreen
import dev.dkong.copypaste.utils.ConnectionManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.TextUnit
import dev.dkong.copypaste.objects.Sequence
import dev.dkong.copypaste.objects.Action
import dev.dkong.copypaste.objects.Position
import dev.dkong.copypaste.utils.ActionManager
import dev.dkong.copypaste.utils.ExecutionManager
import kotlinx.coroutines.launch

@Composable
fun DashboardHomeScreen(
    navHostController: NavHostController,
    selectedPage: MutableState<HomeScreenItem>,
    homeNavHostController: NavHostController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isConnected by remember { mutableStateOf(false) }
    var sequences = remember { mutableStateListOf<Sequence>() }

    LaunchedEffect(Unit) {
        ActionManager.listen(context) { s ->
            sequences.clear()
            sequences.addAll(s)
            Log.d("FETCHING SAVED", sequences.toString())
            Log.d("FETCHING SAVED", sequences.size.toString())
        }
    }

    LazyColumn(
        modifier = Modifier.padding(16.dp)
    ) {
        item {
            // Card for no connection to server
            ConnectionManager.checkConnection { connected ->
                isConnected = connected
            }
            if (!isConnected) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Couldn't connect to the server",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Text(
                            text = "Please check the connection and the server settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Button(
                            onClick = {
                                homeNavHostController.navigate(HomeScreenItem.Settings.route)
                                selectedPage.value = HomeScreenItem.Settings
                            },
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 8.dp)
                        ) {
                            Text(text = "Server settings")
                        }
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            Icons.Default.Check.name,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp),
                        )
                        Text(
                            text = "Connected to server",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
        }
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                SectionHeading(heading = "Actions", includeHorizontalPadding = false)
                Button(
                    onClick = {
                        navHostController.navigate(RootScreen.Upload.route)
                    },
                    enabled = isConnected
                ) {
                    Text(text = "New")
                }
            }
        }
        itemsIndexed(sequences) { _, item ->
            SequenceCard(sequence = item, navHostController = navHostController)
        }
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = {
                    scope.launch {
                        ActionManager.addSequence(
                            context,
                            lockScreenSettings
                            )
                    }
                }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Add test data")
                }
            }
        }
    }
}

// TEST DATA

private val flashNotifications = Sequence(
    id = System.currentTimeMillis() / 1000,
    creationTime = System.currentTimeMillis() / 1000,
    name = "Toggle flash notifications",
    status = "SUCCESS",
    dimensions = Position(1080f, 2340f),
    result = arrayOf(
        Action(
            actType = Action.ActionType.Swipe,
            firstFrame = -1,
            resultingScreenOcr = "SettingsSearch settingsNetwork & internetMobile, Wi‑Fi, hotspotConnected devicesBluetooth, pairingAppsAssistant, recent apps, default appsNotificationsNotification history, conversationsBattery100%Storage48% used - 4.20 GB freeSound & vibrationVolume, haptics, Do Not DisturbDisplayDark theme, font size, brightnessWallpaper & styleColors, themed icons, app gridAccessibilityDisplay, interaction, audioSecurity & privacyApp security, device lock, permissionsLocationOn - 4 apps have access to locationSafety & emergencyEmergency SOS, medical info, alerts",
            taps = arrayOf(
                Position(540f, 1800f),
                Position(540f, 1400f)
            ),
            dimensions = Position(1080f, 2340f)
        ),
        Action(
            actType = Action.ActionType.Tap,
            firstFrame = -1,
            resultingScreenOcr = "SettingsSearch settingsNetwork & internetMobile, Wi‑Fi, hotspotConnected devicesBluetooth, pairingAppsAssistant, recent apps, default appsNotificationsNotification history, conversationsBattery100%Storage48% used - 4.20 GB freeSound & vibrationVolume, haptics, Do Not DisturbDisplayDark theme, font size, brightnessWallpaper & styleColors, themed icons, app gridAccessibilityDisplay, interaction, audioSecurity & privacyApp security, device lock, permissionsLocationOn - 4 apps have access to locationSafety & emergencyEmergency SOS, medical info, alerts",
            taps = arrayOf(
                Position(540f, 950f)
            ),
            dimensions = Position(1080f, 2340f)
        ),
        Action(
            actType = Action.ActionType.Swipe,
            firstFrame = -1,
            resultingScreenOcr = "NotificationsNotification historyShow recent and snoozed notificationsConversationConversationsNo priority conversationsBubblesOn / Conversations can appear as floating iconsPrivacyDevice & app notificationsControl which apps and devices can read notificationsNotifications on lock screenHide silent conversations and notificationsGeneralDo Not DisturbOffFlash notificationsOn / Screen flashWireless emergency alertsHide silent notifications in status barAllow notification snoozing",
            taps = arrayOf(
                Position(540f, 2100f),
                Position(540f, 500f),
            ),
            dimensions = Position(1080f, 2340f)
        ),
        Action(
            actType = Action.ActionType.Tap,
            firstFrame = -1,
            resultingScreenOcr = "NotificationsBubblesOn / Conversations can appear as floating iconsPrivacyDevice & app notificationsControl which apps and devices can read notificationsNotifications on lock screenHide silent conversations and notificationsGeneralDo Not DisturbOffFlash notificationsOn / Screen flashWireless emergency alertsHide silent notifications in status barAllow notification snoozingNotification dot on app iconEnhanced notificationsGet suggested actions, replies, and more",
            taps = arrayOf(
                Position(540f, 1350f)
            ),
            dimensions = Position(1080f, 2340f)
        ),
        Action(
            actType = Action.ActionType.Tap,
            firstFrame = -1,
            resultingScreenOcr = "Flash notificationsFlash the screen when you receive notifications or when alarms soundScreen flashYellowPreviewUse flash notifications with caution if you're light sensitive",
            taps = arrayOf(
                Position(969f, 1520f)
            ),
            dimensions = Position(1080f, 2340f)
        )
    )
)

private val lockScreenSettings = Sequence(
    id = System.currentTimeMillis() / 1000,
    creationTime = System.currentTimeMillis() / 1000,
    name = "Lock screen settings",
    status = "SUCCESS",
    dimensions = Position(1080f, 2340f),
    result = arrayOf(
        Action(
            actType = Action.ActionType.Tap,
            actionHint = "Settings",
            firstFrame = 5,
            resultingScreenOcr = "Settings",
            taps = arrayOf(
                Position(1000f, 2203f)
            ),
            dimensions = Position(1080f, 2340f)
        ),
        Action(
            actType = Action.ActionType.Tap,
            actionHint = "Lock screen",
            firstFrame = 34,
            resultingScreenOcr = "LockscreenWhattoshowPrivacyShowsensitivecontentonlywhenunlockedAddtextonlockscreenNoneShowwalletAllowaccesstowalletfromlockscreenShowdevicecontrolsShowcontrolsforexternaldevicesfromthelockscreenControlfromlockeddeviceControlexternaldeviceswithoutunlockingyourphoneortabletifallowedbythedevicecontrolsappDoublelineclockShowdoublelineclockwhenavailableWhentoshowAlwaysshowtimeandinfoIncreasedbatteryusage",
            taps = arrayOf(
                Position(540f, 1236f)
            ),
            dimensions = Position(1080f, 2340f)
        ),
        Action(
            actType = Action.ActionType.Tap,
            actionHint = "Double",
            firstFrame = 106,
            resultingScreenOcr = "LockscreenWhattoshowPrivacyShowsensitivecontentonlywhenunlockedAddtextonlockscreenNoneShowwalletAllowaccesstowalletfromlockscreenShowdevicecontrolsShowcontrolsforexternaldevicesfromthelockscreenControlfromlockeddeviceControlexternaldeviceswithoutunlockingyourphoneortabletifallowedbythedevicecontrolsappDoublelineclockShowdoublelineclockwhenavailableWhentoshowAlwaysshowtimeandinfoIncreasedbatteryusage",
            taps = arrayOf(
                Position(163f, 1912f)
            ),
            dimensions = Position(1080f, 2340f)
        ),
        Action(
            actType = Action.ActionType.Tap,
            actionHint = "Control",
            firstFrame = 110,
            resultingScreenOcr = "LockscreenWhattoshowPrivacyShowsensitivecontentonlywhenunlockedAddtextonlockscreenNoneShowwalletAllowaccesstowalletfromlockscreenShowdevicecontrolsShowcontrolsforexternaldevicesfromthelockscreenControlfromlockeddeviceControlexternaldeviceswithoutunlockingyourphoneortabletifallowedbythedevicecontrolsappDoublelineclockShowdoublelineclockwhenavailableWhentoshowAlwaysshowtimeandinfoIncreasedbatteryusageTaptocheckphoneOnLifttocheckphoneOnA",
            taps = arrayOf(
                Position(163f, 1912f)
            ),
            dimensions = Position(1080f, 2340f)
        ),
        Action(
            actType = Action.ActionType.Tap,
            actionHint = "Control",
            firstFrame = 115,
            resultingScreenOcr = "TaptocheckphoneTaptocheckphoneTochecktimenotificationsandotherinfotapyourscreen",
            taps = arrayOf(
                Position(156f, 1140f),
                Position(150f, 1071f),
                Position(143f, 1001f),
                Position(137f, 918f),
                Position(130f, 840f),
                Position(124f, 783f),
                Position(112f, 732f),
                Position(109f, 711f),
                Position(114f, 703f),
                Position(116f, 701f),
                Position(120f, 703f),
                Position(119f, 705f),
                Position(120f, 709f)
            ),
            dimensions = Position(1080f, 2340f)
        ),
        Action(
            actType = Action.ActionType.Tap,
            actionHint = "notifications",
            firstFrame = 176,
            resultingScreenOcr = "TaptocheckphoneTaptocheckphoneTochecktimenotificationsandotherinfotapyourscreen",
            taps = arrayOf(
                Position(443f, 737f)
            ),
            dimensions = Position(1080f, 2340f)
        ),
        Action(
            actType = Action.ActionType.Tap,
            actionHint = "and",
            firstFrame = 198,
            resultingScreenOcr = "AFlashnotificationsFlashthescreenwhenyoureceivenotificationsorwhenalarmssoundScreenflashYellowPreviewUseflashnotificationswithcautionifyourelightsensitive",
            taps = arrayOf(
                Position(540f, 1236f)
            ),
            dimensions = Position(1080f, 2340f)
        )
    )
)

private val setTimer = Sequence(
    id = System.currentTimeMillis() / 1000,
    creationTime = System.currentTimeMillis() / 1000,
    name = "Set 5-minute timer",
    status = "SUCCESS",
    dimensions = Position(1080f, 2340f),
    result = arrayOf(
        Action(
            actType = Action.ActionType.Tap,
            actionHint = "Timer",
            firstFrame = 5,
            resultingScreenOcr = "TimerAlarmhmsClockTimerStopwatchBedtime",
            taps = arrayOf(
                Position(565f, 2203f)
            ),
            dimensions = Position(1080f, 2340f)
        ),
        Action(
            actType = Action.ActionType.Tap,
            actionHint = "5",
            firstFrame = 34,
            resultingScreenOcr = "TimerAlarmhmsClockLOTimerStopwatchBedtime",
            taps = arrayOf(
                Position(558f, 1097f)
            ),
            dimensions = Position(1080f, 2340f)
        ),
        Action(
            actType = Action.ActionType.Tap,
            actionHint = "00",
            firstFrame = 54,
            resultingScreenOcr = "TimerAlarmhmsClockTimerStopwatchBedtime",
            taps = arrayOf(
                Position(557f, 1663f)
            ),
            dimensions = Position(1080f, 2340f)
        ),
        Action(
            actType = Action.ActionType.Tap,
            actionHint = "2",
            firstFrame = 80,
            resultingScreenOcr = "TimermsTimerAlarmClockTimerStopwatchBedtime",
            taps = arrayOf(
                Position(268f, 851f)
            ),
            dimensions = Position(1080f, 2340f)
        ),
        Action(
            actType = Action.ActionType.Tap,
            actionHint = "5:00",
            firstFrame = 195,
            resultingScreenOcr = "TimerAlarmhmsClockTimerStopwatchBedtime",
            taps = arrayOf(
                Position(539f, 1970f)
            ),
            dimensions = Position(1080f, 2340f)
        )
    )
)

// END TEST DATA

@Composable
fun SequenceCard(sequence: Sequence, navHostController: NavHostController) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = {
            navHostController.navigate("seq_info?id=${sequence.id}")
            // TODO: Launch the sequence detail page
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column {
                sequence.name?.let { name ->
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "${sequence.result?.size ?: "No"} actions | ${sequence.id}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            FilledTonalIconButton(onClick = {
                ExecutionManager.setUpSequence(sequence)
                ExecutionManager.start()
            }) {
                Icon(Icons.Default.PlayArrow, Icons.Default.PlayArrow.name)
            }
        }
    }
}