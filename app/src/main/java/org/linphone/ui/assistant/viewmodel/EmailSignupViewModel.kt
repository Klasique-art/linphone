/*
 * Copyright (c) 2010-2023 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.linphone.ui.assistant.viewmodel

import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.linphone.LinphoneApplication.Companion.coreContext
import org.linphone.R
import org.linphone.core.Account
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.Factory
import org.linphone.core.RegistrationState
import org.linphone.core.TransportType
import org.linphone.core.tools.Log
import org.linphone.ui.GenericViewModel
import org.linphone.utils.Event
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class EmailSignupViewModel
    @UiThread
    constructor() : GenericViewModel() {
    companion object {
        private const val TAG = "[Email Signup ViewModel]"

        private const val BACKEND_API_URL = "https://italk.pythonanywhere.com/api/credentials/"
    }

    val email = MutableLiveData<String>()

    val signUpEnabled = MediatorLiveData<Boolean>()

    val signUpInProgress = MutableLiveData<Boolean>()

    val loginInProgress = MutableLiveData<Boolean>()

    val accountCreatedEvent = MutableLiveData<Event<Boolean>>()

    val showUltraSimDialogEvent = MutableLiveData<Event<SipCredentials>>()

    private lateinit var newlyCreatedAccount: Account

    private val coreListener = object : CoreListenerStub() {
        @WorkerThread
        override fun onAccountRegistrationStateChanged(
            core: Core,
            account: Account,
            state: RegistrationState,
            message: String
        ) {
            if (account == newlyCreatedAccount) {
                Log.i("$TAG Account [${account.params.identityAddress?.asStringUriOnly()}] registration state changed: [$state]")

                if (state == RegistrationState.Ok) {
                    Log.i("$TAG Account successfully registered!")
                    loginInProgress.postValue(false)
                    core.removeListener(this)

                    coreContext.postOnMainThread {
                        showGreenToastEvent.value = Event(
                            Pair(R.string.email_signup_success, R.drawable.check)
                        )
                        accountCreatedEvent.value = Event(true)
                    }
                } else if (state == RegistrationState.Failed) {
                    Log.e("$TAG Account registration failed: $message")
                    loginInProgress.postValue(false)
                    core.removeListener(this)

                    coreContext.postOnMainThread {
                        showRedToastEvent.value = Event(
                            Pair(R.string.email_signup_error_server, R.drawable.warning_circle)
                        )
                    }
                }
            }
        }
    }

    init {
        signUpInProgress.value = false
        loginInProgress.value = false

        signUpEnabled.addSource(email) {
            signUpEnabled.value = isSignUpEnabled()
        }
    }

    @UiThread
    fun signUp() {
        val emailValue = email.value.orEmpty().trim()

        if (!isValidEmail(emailValue)) {
            showRedToastEvent.value = Event(
                Pair(R.string.email_signup_error_invalid_email, R.drawable.warning_circle)
            )
            return
        }

        signUpInProgress.value = true

        // Call backend API to get SIP credentials
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sipCredentials = callBackendApi(emailValue)

                if (sipCredentials != null) {
                    // Don't create account yet, show Ultra SIM dialog first!
                    withContext(Dispatchers.Main) {
                        signUpInProgress.value = false
                        showUltraSimDialogEvent.value = Event(sipCredentials)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        signUpInProgress.value = false
                        showRedToastEvent.value = Event(
                            Pair(R.string.email_signup_error_server, R.drawable.warning_circle)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("$TAG Exception during sign up: ${e.message}")
                withContext(Dispatchers.Main) {
                    signUpInProgress.value = false
                    showRedToastEvent.value = Event(
                        Pair(R.string.email_signup_error_network, R.drawable.warning_circle)
                    )
                }
            }
        }
    }

    @UiThread
    fun continueWithLogin(credentials: SipCredentials) {
        loginInProgress.value = true

        // Create SIP account on worker thread
        CoroutineScope(Dispatchers.IO).launch {
            createSipAccount(
                username = credentials.username,
                password = credentials.password,
                domain = credentials.domain,
                displayName = credentials.displayName
            )
        }
    }

    @WorkerThread
    private suspend fun callBackendApi(email: String): SipCredentials? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(BACKEND_API_URL)
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                // Create JSON request body
                val jsonRequest = JSONObject()
                jsonRequest.put("email", email)

                // Send request
                val outputStream: OutputStream = connection.outputStream
                outputStream.write(jsonRequest.toString().toByteArray())
                outputStream.flush()
                outputStream.close()

                val responseCode = connection.responseCode
                Log.i("$TAG Backend API response code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read response
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    // Parse JSON response
                    val jsonResponse = JSONObject(response.toString())
                    Log.i("$TAG Backend API response: $jsonResponse")

                    // Extract SIP credentials from response
                    // TODO: Adjust these field names to match your backend's response format
                    SipCredentials(
                        username = jsonResponse.getString("username"),
                        password = jsonResponse.getString("password"),
                        domain = jsonResponse.getString("domain"),
                        displayName = jsonResponse.optString("display_name", email.substringBefore("@"))
                    )
                } else {
                    Log.e("$TAG Backend API returned error code: $responseCode")
                    null
                }
            } catch (e: Exception) {
                Log.e("$TAG Error calling backend API: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }

    @WorkerThread
    private fun createSipAccount(
        username: String,
        password: String,
        domain: String,
        displayName: String
    ) {
        coreContext.postOnCoreThread { core ->
            Log.i("$TAG Creating SIP account with username=[$username], domain=[$domain]")

            // Create authentication info
            val authInfo = Factory.instance().createAuthInfo(
                username,
                null,
                password,
                null,
                null,
                domain,
                null
            )

            // Create SIP address
            val identity = "sip:$username@$domain"
            val identityAddress = Factory.instance().createAddress(identity)
            identityAddress?.displayName = displayName

            // Create account parameters
            val accountParams = core.createAccountParams()
            accountParams.identityAddress = identityAddress

            // Set server address (proxy)
            val serverAddress = Factory.instance().createAddress("sip:$domain")
            serverAddress?.transport = TransportType.Udp // Always UDP
            accountParams.serverAddress = serverAddress

            // Enable registration
            accountParams.isRegisterEnabled = true

            // Set publish presence
            accountParams.isPublishEnabled = false

            // Create the account
            newlyCreatedAccount = core.createAccount(accountParams)

            // Add authentication info and account to core
            core.addAuthInfo(authInfo)
            core.addAccount(newlyCreatedAccount)

            // Set as default account
            core.defaultAccount = newlyCreatedAccount

            // Listen for registration state changes
            core.addListener(coreListener)

            Log.i("$TAG SIP account created, waiting for registration...")
        }
    }

    @UiThread
    private fun isSignUpEnabled(): Boolean {
        return email.value.orEmpty().isNotEmpty()
    }

    @UiThread
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Data class to hold SIP credentials
    data class SipCredentials(
        val username: String,
        val password: String,
        val domain: String,
        val displayName: String
    )
}
