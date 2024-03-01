/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
 * except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */

package eu.europa.ec.proximityfeature.interactor

import android.content.Context
import androidx.biometric.BiometricPrompt
import eu.europa.ec.businesslogic.controller.biometry.BiometricsAvailability
import eu.europa.ec.businesslogic.controller.biometry.UserAuthenticationResult
import eu.europa.ec.businesslogic.controller.walletcore.WalletCorePartialState
import eu.europa.ec.businesslogic.controller.walletcore.WalletCorePresentationController
import eu.europa.ec.commonfeature.interactor.UserAuthenticationInteractor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

sealed class ProximityLoadingObserveResponsePartialState {
    data class UserAuthenticationRequired(
        val crypto: BiometricPrompt.CryptoObject?,
        val resultHandler: UserAuthenticationResult
    ) : ProximityLoadingObserveResponsePartialState()

    data class Failure(val error: String) : ProximityLoadingObserveResponsePartialState()
    data object Success : ProximityLoadingObserveResponsePartialState()
}

interface ProximityLoadingInteractor {
    val verifierName: String?
    fun stopPresentation()
    fun observeResponse(): Flow<ProximityLoadingObserveResponsePartialState>
    fun handleUserAuthentication(
        context: Context,
        crypto: BiometricPrompt.CryptoObject?,
        resultHandler: UserAuthenticationResult
    )
}

class ProximityLoadingInteractorImpl(
    private val walletCorePresentationController: WalletCorePresentationController,
    private val userAuthenticationInteractor: UserAuthenticationInteractor
) : ProximityLoadingInteractor {

    override val verifierName: String? = walletCorePresentationController.verifierName

    override fun observeResponse(): Flow<ProximityLoadingObserveResponsePartialState> =
        walletCorePresentationController.observeSentDocumentsRequest().mapNotNull { response ->
            when (response) {
                is WalletCorePartialState.Failure -> ProximityLoadingObserveResponsePartialState.Failure(
                    error = response.error
                )

                is WalletCorePartialState.Redirect -> null

                is WalletCorePartialState.Success -> ProximityLoadingObserveResponsePartialState.Success
                is WalletCorePartialState.UserAuthenticationRequired -> {
                    ProximityLoadingObserveResponsePartialState.UserAuthenticationRequired(
                        response.crypto,
                        response.resultHandler
                    )
                }
            }
        }

    override fun handleUserAuthentication(
        context: Context,
        crypto: BiometricPrompt.CryptoObject?,
        resultHandler: UserAuthenticationResult
    ) {
        userAuthenticationInteractor.getBiometricsAvailability {
            when (it) {
                is BiometricsAvailability.CanAuthenticate -> {
                    userAuthenticationInteractor.authenticateWithBiometrics(
                        context = context,
                        crypto = crypto,
                        resultHandler = resultHandler
                    )
                }

                is BiometricsAvailability.NonEnrolled -> {
                    userAuthenticationInteractor.authenticateWithBiometrics(
                        context = context,
                        crypto = crypto,
                        resultHandler = resultHandler
                    )
                }

                is BiometricsAvailability.Failure -> {
                    resultHandler.onAuthenticationFailure()
                }
            }
        }
    }

    override fun stopPresentation() {
        walletCorePresentationController.stopPresentation()
    }
}