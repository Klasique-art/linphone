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
package org.linphone.ui.assistant.fragment

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.navigation.navGraphViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.linphone.R
import org.linphone.core.tools.Log
import org.linphone.databinding.AssistantEmailSignupFragmentBinding
import org.linphone.ui.GenericFragment
import org.linphone.ui.assistant.viewmodel.EmailSignupViewModel

@UiThread
class EmailSignupFragment : GenericFragment() {
    companion object {
        private const val TAG = "[Email Signup Fragment]"
    }

    private lateinit var binding: AssistantEmailSignupFragmentBinding

    private val viewModel: EmailSignupViewModel by navGraphViewModels(
        R.id.assistant_nav_graph
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = AssistantEmailSignupFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        observeToastEvents(viewModel)

        binding.setBackClickListener {
            requireActivity().finish()
        }

        // Show Ultra SIM dialog when credentials are received
        viewModel.showUltraSimDialogEvent.observe(viewLifecycleOwner) {
            it.consume { credentials ->
                showUltraSimDialog(credentials)
            }
        }

        // Close assistant when account is successfully created
        viewModel.accountCreatedEvent.observe(viewLifecycleOwner) {
            it.consume { success ->
                if (success) {
                    Log.i("$TAG Account created successfully, finishing assistant")
                    requireActivity().finish()
                }
            }
        }
    }

    private fun showUltraSimDialog(credentials: EmailSignupViewModel.SipCredentials) {
        val message = getString(R.string.ultra_sim_dialog_message, credentials.username)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.ultra_sim_dialog_title)
            .setMessage(Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY))
            .setPositiveButton(R.string.ultra_sim_dialog_continue) { dialog, _ ->
                Log.i("$TAG User clicked Continue, proceeding with login")
                viewModel.continueWithLogin(credentials)
                dialog.dismiss()
            }
            .setCancelable(false) // User must click Continue
            .show()
    }
}
