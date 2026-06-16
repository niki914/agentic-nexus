package com.niki914.nexus.ipc.store

data class StoreDescriptor(
    val id: String,
    val relativePath: String,
    val defaultJson: String = "{}"
)
