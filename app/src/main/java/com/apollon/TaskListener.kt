package com.apollon

interface TaskListener {

    fun onTaskCompleted(result: TaskResult)
}