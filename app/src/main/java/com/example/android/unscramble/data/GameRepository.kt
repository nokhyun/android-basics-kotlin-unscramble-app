package com.example.android.unscramble.data

import android.app.Application
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GameRepository @Inject constructor(
    application: Application,
    private val dataSource: GameDataSource
) {

    val highScore: Flow<Int> = dataSource.gamePreferencesFlow.map { it.highScore }

    suspend fun updateScore(score: Int) {
        dataSource.updateHighScore(score)
    }
}