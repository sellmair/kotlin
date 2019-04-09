/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Errors.UNABLE_TO_RESOLVE_EXTENSION
import org.jetbrains.kotlin.extensionresolution.ExtensionCandidateResolution
import org.jetbrains.kotlin.extensionresolution.ExtensionResolutionStrategy
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import java.util.*

object ExtensionResolutionCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        resolvedCall.resultingDescriptor.valueParameters.forEach { descriptor: ValueParameterDescriptor ->
            if (descriptor.isExtension && resolvedCall.valueArguments[descriptor] == null) {
                val valueParameters = functionParameters(context)
                val resolution = ExtensionResolutionStrategy
                    .resolve(
                        descriptor,
                        valueParameters,
                        descriptor.original,
                        ArrayList(),
                        false
                    )

                when (resolution) {
                    is ExtensionCandidateResolution.Unresolved -> {
                        val (message) = resolution
                        context.trace.report(
                            UNABLE_TO_RESOLVE_EXTENSION.on(
                                reportOn,
                                message
                            )
                        )
                    }
                    is ExtensionCandidateResolution.Resolved -> {
                        val key = descriptor.returnType.toString()
                        if (context.trace.get(BindingContext.EXTENSION_RESOLUTION_INFO, key) == null) {
                            context.trace.record(
                                BindingContext.EXTENSION_RESOLUTION_INFO,
                                key,
                                resolution.candidate
                            )
                        }
                    }
                }
            }
        }
    }

    private fun functionParameters(context: CallCheckerContext): List<ValueParameterDescriptor> {
        var descriptor: DeclarationDescriptor? = context.scope.ownerDescriptor
        var valueParameters: List<ValueParameterDescriptor> = ArrayList()
        while (descriptor != null) {
            if (descriptor is CallableDescriptor) {
                valueParameters += descriptor.valueParameters
            }
            descriptor = descriptor.containingDeclaration
        }
        return valueParameters
    }
}
