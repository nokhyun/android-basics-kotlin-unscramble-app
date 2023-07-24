package com.example.android.unscramble.di

import androidx.lifecycle.ViewModel
import com.example.android.unscramble.ui.game.GameViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module(subcomponents = [GameComponent::class])
interface GameModule {
    @Binds
    @IntoMap
    @ViewModelKey(GameViewModel::class)
    fun bindGameViewModel(viewModel: GameViewModel): ViewModel
}