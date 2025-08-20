package com.app.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.app.data.security.SecureStorage
import com.app.domain.repository.SecureRepository

@RequiresApi(Build.VERSION_CODES.GINGERBREAD)
class SecureRepositoryImpl(private val secureStorage: SecureStorage) : SecureRepository {

    override suspend fun setWelcomeNotificationShown(isShown: Boolean){
      val response = secureStorage.setWelcomeNotificationShown(isShown)
        return response
    }


}