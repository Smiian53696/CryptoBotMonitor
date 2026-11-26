
package com.example.cryptobotmonitor.fake

import com.example.cryptobotmonitor.data.auth.CurrentUser
import com.example.cryptobotmonitor.data.auth.CurrentUserProvider

// Dane użytkownika używane podczas testów
class FakeCurrentUserProvider : CurrentUserProvider {

    var user: CurrentUser? = CurrentUser(
        uid = "test-user",
        email = "user@gmail.com"
    )

    var getUserCalls = 0
        private set

    // Symulacja pobierania użytkownika
    override fun getCurrentUser(): CurrentUser? {

        getUserCalls++

        return user
    }
}
