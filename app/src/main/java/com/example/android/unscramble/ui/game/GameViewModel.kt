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

import android.text.Spannable
import android.text.SpannableString
import android.text.style.TtsSpan
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
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
    var value:T
        get() = state.value
        set(value) {
            savedStateHandle[key] = value
        }

    fun asStateFlow(): StateFlow<T> = state
}

fun <T> SavedStateHandle.getMutableStateFlow(key: String, initialValue: T): SavableMutableStateFlow<T>{
    return SavableMutableStateFlow(this, key, initialValue)
}

class GameViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _score = savedStateHandle.getMutableStateFlow("score", 0)
    val score: StateFlow<Int>
        get() = _score.asStateFlow()

    private val _currentWordCount = savedStateHandle.getMutableStateFlow("currentWordCount", 0)
    val currentWordCount: StateFlow<Int>
        get() = _currentWordCount.asStateFlow()

    private val _currentScrambledWord = savedStateHandle.getMutableStateFlow("currentScrambledWord", "")
    val currentScrambledWord: StateFlow<Spannable> = _currentScrambledWord
        .asStateFlow()
        .map {
            val scrambledWord = it
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
    private var wordsList: MutableList<String> = mutableListOf()
    private lateinit var currentWord: String

    init {
        getNextWord()
    }

    /*
     * Updates currentWord and currentScrambledWord with the next word.
     */
    private fun getNextWord() {
        currentWord = allWordsList.random(Random(Calendar.getInstance().timeInMillis))
        val tempWord = currentWord.toCharArray()
        tempWord.shuffle()

        while (String(tempWord).equals(currentWord, false)) {
            tempWord.shuffle()
        }
        if (wordsList.contains(currentWord)) {
            getNextWord()
        } else {
            Log.d("Unscramble", "currentWord= $currentWord")
            _currentScrambledWord.value = String(tempWord)
            _currentWordCount.value = _currentWordCount.value.inc()
            wordsList.add(currentWord)
        }
    }

    /*
     * Re-initializes the game data to restart the game.
     */
    fun reinitializeData() {
        _score.value = 0
        _currentWordCount.value = 0
        wordsList.clear()
        getNextWord()
    }

    /*
    * Increases the game score if the player’s word is correct.
    */
    private fun increaseScore() {
        _score.value = _score.value?.plus(SCORE_INCREASE)!!
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
            getNextWord()
            true
        } else false
    }
}
