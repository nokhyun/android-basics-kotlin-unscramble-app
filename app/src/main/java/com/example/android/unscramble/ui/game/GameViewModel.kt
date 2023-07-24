/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.unscramble.ui.game

import android.app.Application
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.TtsSpan
import android.util.Log
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import com.example.android.unscramble.data.GameRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import kotlin.random.Random

/**
 * ViewModel containing the app data and methods to process the data
 */
class SavableMutableStateFlow<T>(
    private val savedStateHandle: SavedStateHandle,
    private val key: String,
    initialValue: T
) {
    private val state: StateFlow<T> = savedStateHandle.getStateFlow(key, initialValue)
    var value: T
        get() = state.value
        set(value) {
            savedStateHandle[key] = value
        }

    fun asStateFlow(): StateFlow<T> = state
}

fun <T> SavedStateHandle.getMutableStateFlow(key: String, initialValue: T): SavableMutableStateFlow<T> {
    return SavableMutableStateFlow(this, key, initialValue)
}

@HiltViewModel
class GameViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _score = savedStateHandle.getMutableStateFlow("score", 0)
    val score: StateFlow<Int>
        get() = _score.asStateFlow()

    val highScore: StateFlow<Int> = gameRepository.highScore
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _currentWordCount = savedStateHandle.getMutableStateFlow("currentWordCount", 0)
    val currentWordCount: StateFlow<Int>
        get() = _currentWordCount.asStateFlow()

    private val _currentScrambledWord = savedStateHandle.getMutableStateFlow("currentScrambledWord", "")
    val currentScrambledWord: StateFlow<Spannable> = _currentScrambledWord
        .asStateFlow()
        .onSubscription {
            if (currentWord.isEmpty()) nextWord()
        }
        .map { scrambledWord ->
            val spannable: Spannable = SpannableString(scrambledWord)
            spannable.setSpan(
                TtsSpan.VerbatimBuilder(scrambledWord).build(),
                0,
                scrambledWord.length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
            spannable
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpannableString(""))

    // List of words used in the game
    private var wordsList: List<String>
        get() = savedStateHandle["wordList"] ?: emptyList()
        set(value) {
            savedStateHandle["wordsList"] = value
        }

    private var currentWord: String
        get() = savedStateHandle["currentWord"] ?: ""
        set(value) {
            val tempWord = value.toCharArray()
            tempWord.shuffle()

            do {
                tempWord.shuffle()
            } while (String(tempWord) == currentWord)
            Log.d("Unscramble", "currentWord= $value")
            _currentScrambledWord.value = String(tempWord)
            _currentWordCount.value += 1
            wordsList = wordsList + currentWord
            savedStateHandle["currentWord"] = value
        }

    init {
//        getNextWord()
    }

    /*
     * Re-initializes the game data to restart the game.
     */
    fun reinitializeData() {
        _score.value = 0
        _currentWordCount.value = 0
        wordsList = emptyList()
        nextWord()
    }

    /*
    * Increases the game score if the player’s word is correct.
    */
    private fun increaseScore() {
        _score.value += SCORE_INCREASE

        viewModelScope.launch {
            gameRepository.updateScore(score.value)
        }
    }

    /*
    * Returns true if the player word is correct.
    * Increases the score accordingly.
    */
    fun isUserWordCorrect(playerWord: String): Boolean {
        if (playerWord.equals(currentWord, true)) {
            increaseScore()
            return true
        }
        return false
    }

    /*
    * Returns true if the current word count is less than MAX_NO_OF_WORDS
    */
    fun nextWord(): Boolean {
        return if (_currentWordCount.value < MAX_NO_OF_WORDS) {
            var nextWord: String
            do {
                nextWord = allWordsList.random(Random(Calendar.getInstance().timeInMillis))
            } while (wordsList.contains(currentWord))
            currentWord = nextWord

            true
        } else false
    }
}

//class GameViewModelFactory(
//    private val application: Application,
//    owner: SavedStateRegistryOwner,
//    defaultArgs: Bundle? = null
//) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
//    override fun <T : ViewModel> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
//        require(modelClass.isAssignableFrom(GameViewModel::class.java)) {
//            "Unknown ViewModel Class"
//        }
//
//        @Suppress("UNCHECKED_CAST")
//        return GameViewModel(
//            savedStateHandle = handle,
//            gameRepository = GameRepository(application),
//        ) as T
//    }
//}
