/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.implicit

import org.jetbrains.kotlin.codegen.implicit.ImplicitResolution.*
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.model.ImplicitValueArgument
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassMemberScope

object ImplicitResolutionStrategy {
    @JvmStatic
    fun resolve(lookingFor: ValueParameterDescriptor,
                parameters: List<ValueParameterDescriptor>,
                argument: ImplicitValueArgument,
                substitutions: List<TypeSubstitution>): ImplicitCandidate? {
        val functionOrder = listOf(FindInLocalFunction,
                                   FindInPackage,
                                   FindInTypeCompanion,
                                   FindInTypeclassCompanion)

        var candidate: ImplicitCandidate? = null
        for (resolution in functionOrder) {
            candidate = resolution.resolve(lookingFor, parameters, argument, substitutions)
            if (candidate != null) {
                break;
            }
        }

        return candidate?.let { candidate ->
            resolveArguments(candidate, parameters, argument, candidate.substitutions)
        }
    }

    private fun resolveArguments(candidate: ImplicitCandidate,
                                 parameters: List<ValueParameterDescriptor>,
                                 argument: ImplicitValueArgument,
                                 substitutions: List<TypeSubstitution>): ImplicitCandidate? {
        return when(candidate) {
            is ImplicitCandidate.FunctionParameter -> candidate
            is ImplicitCandidate.SingleClassCandidate -> {
                val implicitArguments = mutableListOf<ImplicitCandidate>()
                val newSubstitutions = java.util.ArrayList(substitutions)
                val scope = candidate.value.unsubstitutedMemberScope as? LazyClassMemberScope
                scope?.let {
                    val constructor = scope.getPrimaryConstructor()
                    for (parameter in constructor?.valueParameters ?: emptyList()) {
                        if (parameter.isImplicit) {
                            val implicitArgument = ImplicitResolutionStrategy.resolve(parameter, parameters, argument, newSubstitutions)
                            implicitArgument?.let {
                                implicitArguments.add(it)
                                newSubstitutions.addAll(it.substitutions)
                            } ?: throw IllegalStateException("Unable to resolve implicit parameter in constructor " + candidate.value.name.asString())
                        } else {
                            throw IllegalStateException("Found non-implicit parameter in constructor when resolving instance " + candidate.value.name.asString());
                        }
                    }
                }
                if (implicitArguments.size > 0) {
                    ImplicitCandidate.NestedClassCandidate(candidate.value, implicitArguments, newSubstitutions)
                } else {
                    candidate
                }
            }
            is ImplicitCandidate.NestedClassCandidate -> throw IllegalStateException("Shouldn't happen")
        }
    }
}
