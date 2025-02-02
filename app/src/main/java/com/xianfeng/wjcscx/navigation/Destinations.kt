package com.xianfeng.wjcscx.navigation

interface Destinations {
    val route: String
}

object MainScreen: Destinations {
    override val route = "MainScreen"
}

object SendScreen: Destinations {
    override val route = "SendScreen"
}

object ReceiveScreen: Destinations {
    override val route = "ReceiveScreen"
}

object FileScreen: Destinations {
    override val route = "FileScreen"
}