package com.example.android.unscramble.data

import android.app.Application
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameRepository(
    application: Application,
    private val dataSource: GameDataSource = GameDataSource(application)
) {

    val highScore: Flow<Int> = dataSource.gamePreferencesFlow.map { it.highScore }

    suspend fun updateScore(score: Int) {
        dataSource.updateHighScore(score)
    }
}