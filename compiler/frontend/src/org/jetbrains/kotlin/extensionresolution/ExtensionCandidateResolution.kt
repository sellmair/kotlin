/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.extensionresolution

sealed class ExtensionCandidateResolution {
    data class Resolved(val candidate: ExtensionCandidate) : ExtensionCandidateResolution()
    data class Unresolved(val message: String) : ExtensionCandidateResolution()
}
