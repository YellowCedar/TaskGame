package com.github.cedaryellow.data.di

import com.github.cedaryellow.data.DefaultUserPointsRepository
import com.github.cedaryellow.data.UserPointsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PointsModule {
    
    @Singleton
    @Binds
    abstract fun bindUserPointsRepository(
        defaultUserPointsRepository: DefaultUserPointsRepository
    ): UserPointsRepository
} 