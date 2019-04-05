/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.implicit

sealed class ImplicitCandidateResolution {
    data class Resolved(val candidate: ImplicitCandidate) : ImplicitCandidateResolution()
    data class Unresolved(val message: String) : ImplicitCandidateResolution()
}
