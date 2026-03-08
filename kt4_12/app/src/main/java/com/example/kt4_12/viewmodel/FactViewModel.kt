package com.example.kt4_12.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kt4_12.data.AnimalFacts
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlin.random.Random

class FactViewModel : ViewModel() {

    private var currentFact: String = ""

    fun getRandomFact(): Flow<String> = flow {
        val delayTime = Random.nextLong(1500, 3000)
        delay(delayTime)

        val randomIndex = Random.nextInt(AnimalFacts.factsList.size)
        currentFact = AnimalFacts.factsList[randomIndex]
        emit(currentFact)
    }.onStart {
        emit("")
    }.catch { exception ->
        emit("Ошибка загрузки: ${exception.message}")
    }

    fun generateNewFact() {
        viewModelScope.launch {
            getRandomFact().collect { fact ->
            }
        }
    }
}