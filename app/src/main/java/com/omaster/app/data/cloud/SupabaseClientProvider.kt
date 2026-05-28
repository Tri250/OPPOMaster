package com.omaster.app.data.cloud

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseClientProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var _client: SupabaseClient? = null

    val client: SupabaseClient
        get() = _client ?: createClient().also { _client = it }

    private fun createClient(): SupabaseClient {
        return try {
            createSupabaseClient(
                supabaseUrl = SupabaseConfig.SUPABASE_URL,
                supabaseKey = SupabaseConfig.SUPABASE_ANON_KEY
            ) {
                install(Auth)
                install(Postgrest)
            }.also {
                Timber.d("Supabase client created successfully")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to create Supabase client")
            throw e
        }
    }

    fun reset() {
        _client = null
        Timber.d("Supabase client reset")
    }
}