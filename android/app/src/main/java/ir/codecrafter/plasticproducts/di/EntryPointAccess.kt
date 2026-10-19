package ir.codecrafter.plasticproducts.di

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import ir.codecrafter.plasticproducts.data.repository.ProfileRepository

object EntryPointAccess {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface RepositoryEntryPoint {
        fun profileRepository(): ProfileRepository
    }

    fun getProfileRepository(context: Context): ProfileRepository? {
        return try {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                RepositoryEntryPoint::class.java
            ).profileRepository()
        } catch (e: Exception) {
            null
        }
    }
}