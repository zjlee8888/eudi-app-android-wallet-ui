/*
 *
 *  * Copyright (c) 2023 European Commission
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package eu.europa.ec.proximityfeature.ui.loading

import androidx.lifecycle.viewModelScope
import eu.europa.ec.commonfeature.config.SuccessUIConfig
import eu.europa.ec.commonfeature.di.getPresentationScope
import eu.europa.ec.commonfeature.ui.loading.Event
import eu.europa.ec.commonfeature.ui.loading.LoadingViewModel
import eu.europa.ec.proximityfeature.interactor.ProximityLoadingCombinedPartialState
import eu.europa.ec.proximityfeature.interactor.ProximityLoadingInteractor
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.ProximityScreens
import eu.europa.ec.uilogic.navigation.Screen
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class ProximityLoadingViewModel constructor(
    private val uiSerializer: UiSerializer,
    private val resourceProvider: ResourceProvider,
    private val interactor: ProximityLoadingInteractor,
) : LoadingViewModel() {

    override fun getTitle(): String {
        return resourceProvider.getString(R.string.proximity_loading_title)
    }

    override fun getSubtitle(): String {
        return resourceProvider.getString(R.string.loading_subtitle)
    }

    override fun getPreviousScreen(): Screen {
        return ProximityScreens.Request
    }

    override fun getCallerScreen(): Screen {
        return ProximityScreens.Loading
    }

    override fun getNextScreen(): String {
        return generateComposableNavigationLink(
            screen = CommonScreens.Success,
            arguments = generateComposableArguments(
                getSuccessConfig()
            )
        )
    }

    override fun doWork() {
        viewModelScope.launch {

            interactor.sendRequestedDocuments().collect {
                when (it) {
                    is ProximityLoadingCombinedPartialState.Failure -> {
                        setState {
                            copy(error = ContentErrorConfig(
                                onRetry = { setEvent(Event.DoWork) },
                                errorSubTitle = it.error,
                                onCancel = { doNavigation(NavigationType.POP) }
                            ))
                        }
                    }

                    is ProximityLoadingCombinedPartialState.Success -> {
                        interactor.stopPresentation()
                        getPresentationScope().close()
                        doNavigation(NavigationType.PUSH)
                    }

                    ProximityLoadingCombinedPartialState.UserAuthenticationRequired -> {
                        // Provide implementation for Biometrics POP
                    }
                }
            }

        }
    }

    private fun getSuccessConfig(): Map<String, String> =
        mapOf(
            SuccessUIConfig.serializedKeyName to uiSerializer.toBase64(
                SuccessUIConfig(
                    header = resourceProvider.getString(R.string.loading_success_config_title),
                    content = resourceProvider.getString(R.string.proximity_loading_success_config_subtitle),
                    imageConfig = SuccessUIConfig.ImageConfig(
                        type = SuccessUIConfig.ImageConfig.Type.DEFAULT
                    ),
                    buttonConfig = listOf(
                        SuccessUIConfig.ButtonConfig(
                            text = resourceProvider.getString(R.string.loading_success_config_primary_button_text),
                            style = SuccessUIConfig.ButtonConfig.Style.PRIMARY,
                            navigation = ConfigNavigation(
                                navigationType = NavigationType.POP,
                                screenToNavigate = DashboardScreens.Dashboard
                            )
                        )
                    ),
                    onBackScreenToNavigate = ConfigNavigation(
                        navigationType = NavigationType.POP,
                        screenToNavigate = DashboardScreens.Dashboard
                    ),
                ),
                SuccessUIConfig.Parser
            ).orEmpty()
        )
}