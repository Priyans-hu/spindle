package com.markfoundry.spindle

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.markfoundry.spindle.data.SettingsStore
import com.markfoundry.spindle.media.MediaSessionRepository
import com.markfoundry.spindle.media.NotificationAccess
import com.markfoundry.spindle.player.PlayerControls
import com.markfoundry.spindle.skin.defaultSkin
import com.markfoundry.spindle.widget.SpindleWidget
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = MediaSessionRepository(app)
    private val settings = SettingsStore(app)

    val state = repository.state
    val controls: PlayerControls = repository

    val skinId: StateFlow<String> = settings.skinId
        .map { it ?: defaultSkin().id }
        .stateIn(viewModelScope, SharingStarted.Eagerly, defaultSkin().id)

    private var running = false
    private var ticker: Job? = null

    fun isAccessGranted(): Boolean = NotificationAccess.isGranted(getApplication())

    fun selectSkin(id: String) {
        viewModelScope.launch {
            settings.setSkinId(id)
            SpindleWidget.updateAll(getApplication(), id)
        }
    }

    /** Bind to the media session and start ticking progress. Safe to call repeatedly. */
    fun onResume() {
        if (running || !isAccessGranted()) return
        running = true
        repository.start()
        ticker = viewModelScope.launch {
            while (isActive) {
                repository.tick()
                delay(1000)
            }
        }
    }

    fun onPause() {
        running = false
        ticker?.cancel()
        ticker = null
        repository.stop()
    }

    override fun onCleared() {
        ticker?.cancel()
        repository.stop()
    }
}
