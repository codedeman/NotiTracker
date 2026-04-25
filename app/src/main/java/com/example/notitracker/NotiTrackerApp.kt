package com.example.notitracker

import android.app.Application
import com.example.notitracker.data.remote.di.NetworkGraph

class NotiTrackerApp : Application() {

    lateinit var networkGraph: NetworkGraph
        private set

    override fun onCreate() {
        super.onCreate()
        networkGraph = NetworkGraph.create(this)
    }
}
