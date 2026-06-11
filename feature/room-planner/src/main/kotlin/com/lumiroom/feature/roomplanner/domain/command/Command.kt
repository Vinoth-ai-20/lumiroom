package com.lumiroom.feature.roomplanner.domain.command

interface Command {
    fun execute()
    fun undo()
}
