package com.madassignment.myaccomodationapp.di

import com.madassignment.myaccomodationapp.data.repository.FirebaseAuthRepository
import com.madassignment.myaccomodationapp.data.repository.FirestoreChatRepository
import com.madassignment.myaccomodationapp.data.repository.FirestoreListingRepository
import com.madassignment.myaccomodationapp.data.repository.FirestoreReservationRepository
import com.madassignment.myaccomodationapp.data.repository.FirestoreUserRepository
import com.madassignment.myaccomodationapp.domain.repository.AuthRepository
import com.madassignment.myaccomodationapp.domain.repository.ChatRepository
import com.madassignment.myaccomodationapp.domain.repository.ListingRepository
import com.madassignment.myaccomodationapp.domain.repository.ReservationRepository
import com.madassignment.myaccomodationapp.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: FirestoreUserRepository): UserRepository

    @Binds
    @Singleton
    abstract fun bindListingRepository(impl: FirestoreListingRepository): ListingRepository

    @Binds
    @Singleton
    abstract fun bindReservationRepository(impl: FirestoreReservationRepository): ReservationRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: FirestoreChatRepository): ChatRepository
}
