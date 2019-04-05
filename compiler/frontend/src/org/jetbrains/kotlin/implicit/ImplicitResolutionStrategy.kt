/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.implicit

import org.jetbrains.kotlin.implicit.ImplicitCandidateResolution.Resolved
import org.jetbrains.kotlin.implicit.ImplicitCandidateResolution.Unresolved
import org.jetbrains.kotlin.implicit.ImplicitResolution.*
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

object ImplicitResolutionStrategy {
    @JvmStatic
    fun resolve(
        lookingFor: ValueParameterDescriptor,
        parameters: List<ValueParameterDescriptor>,
        argumentParameterDescriptor: ValueParameterDescriptor,
        substitutions: List<TypeSubstitution>,
        lookInSupertypes: Boolean = false
    ): ImplicitCandidateResolution {

        val functionOrder = listOf(
            FindInLocalFunction,
            FindInPackage,
            FindInTypeCompanion,
            FindInTypeclassCompanion,
            FindInTypeSubpackages,
            FindInTypeclassSubpackages
        )
        val candidates = functionOrder.fold(emptyList<Resolved>()) { acc, resolution ->
            val resolved = resolution.resolve(lookingFor, parameters, argumentParameterDescriptor, substitutions, lookInSupertypes)
            when (resolved) {
                is Resolved -> acc + listOf(resolved)
                else -> acc
            }
        }

        return if (candidates.isEmpty() && !lookInSupertypes) {
            resolve(lookingFor, parameters, argumentParameterDescriptor, substitutions, true)
        } else if (candidates.size == 1) {
            resolveArguments(
                candidates.first(),
                parameters,
                argumentParameterDescriptor,
                candidates.first().candidate.substitutions
            )
        } else if (candidates.size > 1) {
            val candidatesList = candidates.map { "- ${it.candidate.value.fqNameSafe}" }.joinToString("\n")
            return Unresolved(
                "Unable to resolve parameter ${argumentParameterDescriptor.name} : ${argumentParameterDescriptor.returnType}" +
                        "\nExtension resolution implements coherence: There must be a single extension in scope to satisfy the required " +
                        "type constraints. Found conflicting candidates:\n${candidatesList}"
            )
        } else {
            Unresolved(
                "Unable to resolve parameter (" +
                        "${argumentParameterDescriptor.name} : ${argumentParameterDescriptor.returnType})."
            )
        }
    }

    private fun resolveArguments(
        resolvedCandidate: Resolved,
        parameters: List<ValueParameterDescriptor>,
        argumentParameterDescriptor: ValueParameterDescriptor,
        substitutions: List<TypeSubstitution>
    ): ImplicitCandidateResolution {
        return when (val candidate = resolvedCandidate.candidate) {
            is ExtensionCandidate.FunctionParameter -> Resolved(candidate)
            is ExtensionCandidate.SingleClassCandidate -> {
                val implicitArguments = mutableListOf<ExtensionCandidate>()
                val newSubstitutions = java.util.ArrayList(substitutions)
                val scope = candidate.value.unsubstitutedPrimaryConstructor
                scope?.let {
                    for (parameter in scope.valueParameters) {
                        if (parameter.isExtension) {
                            val implicitArgument = resolve(
                                parameter,
                                parameters,
                                argumentParameterDescriptor,
                                newSubstitutions
                            )
                            if (implicitArgument is Resolved) {
                                implicitArguments.add(implicitArgument.candidate)
                                newSubstitutions.addAll(implicitArgument.candidate.substitutions)
                            } else {
                                return Unresolved("Unable to resolve parameter (${parameter.name} : ${parameter.returnType}) in constructor for ${candidate.value.name.asString()}")
                            }
                        } else {
                            return Unresolved("Found non-extension parameter in (${parameter.name} : ${parameter.returnType}) constructor when resolving instance for ${candidate.value.name.asString()}. Add the ''with'' keyword to let the compiler resolve it.")
                        }
                    }
                }
                if (implicitArguments.size > 0) {
                    Resolved(
                        ExtensionCandidate.NestedClassCandidate(
                            candidate.value,
                            implicitArguments,
                            newSubstitutions
                        )
                    )
                } else {
                    Resolved(candidate)
                }
            }
            is ExtensionCandidate.NestedClassCandidate -> Unresolved("Shouldn't happen.")
        }
    }
}
