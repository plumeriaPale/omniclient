package com.example.omniclient.viewmodels

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.omniclient.data.db.AppDatabase
import com.example.omniclient.data.db.DatabaseProvider
import com.example.omniclient.data.db.UserDao
import com.example.omniclient.data.db.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserViewModel(private val userDao: UserDao) : ViewModel() {
    private val _users = MutableStateFlow<List<UserEntity>>(emptyList())
    val users: StateFlow<List<UserEntity>> = _users

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            _users.value = userDao.getAllUsers()
        }
    }

    fun addUser(username: String, password: String) {
        viewModelScope.launch {
            userDao.insertUser(UserEntity(username, password))
            loadUsers()
        }
    }

    fun deleteUser(user: UserEntity) {
        viewModelScope.launch {
            userDao.deleteUser(user)
            loadUsers()
        }
    }
}

class UserViewModelFactory(private val userDao: UserDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserViewModel(userDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}