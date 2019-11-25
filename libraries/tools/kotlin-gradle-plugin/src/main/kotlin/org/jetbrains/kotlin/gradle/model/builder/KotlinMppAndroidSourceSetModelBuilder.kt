/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.model.builder

import com.android.build.gradle.api.AndroidSourceSet
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.model.builder.KotlinMppAndroidSourceSetBuilder.AndroidSourceSetClassifierReplacement.PrefixClassifierReplacement
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.utils.keysToMap

private typealias AndroidSourceSets = NamedDomainObjectContainer<AndroidSourceSet>

private typealias KotlinMppSourceSets = NamedDomainObjectContainer<KotlinSourceSet>

internal object KotlinMppAndroidSourceSetBuilder {
    private val logger = Logging.getLogger(this.javaClass)

    fun configureSourceSets(
        project: Project,
        androidTargetName: String,
        kotlinMppSourceSets: KotlinMppSourceSets,
        androidSourceSets: AndroidSourceSets
    ): Map<AndroidSourceSet, KotlinSourceSet> = androidSourceSets.keysToMap { androidSourceSet ->
        configureSourceSet(
            project = project,
            androidTargetName = androidTargetName,
            kotlinMppSourceSets = kotlinMppSourceSets,
            androidSourceSet = androidSourceSet
        )
    }

    private fun configureSourceSet(
        project: Project,
        androidTargetName: String,
        kotlinMppSourceSets: KotlinMppSourceSets,
        androidSourceSet: AndroidSourceSet
    ): KotlinSourceSet {
        val kotlinMppSourceSet = createKotlinMppSourceSet(androidTargetName, kotlinMppSourceSets, androidSourceSet)
        registerKotlinMppSourceSetInAndroidSourceSet(project, kotlinMppSourceSet, androidSourceSet)
        KotlinMppLegacyAndroidSourceSetSupport.configureLegacySourceSetSupport(project, kotlinMppSourceSet, androidSourceSet)
        KotlinMppLegacyAndroidSourceSetSupport.configureAndroidManifest(project, kotlinMppSourceSet, androidSourceSet)
        configureAndroidResources(project, kotlinMppSourceSet, androidSourceSet)
        return kotlinMppSourceSet
    }

    private fun createKotlinMppSourceSet(
        androidTargetName: String,
        kotlinMppSourceSets: KotlinMppSourceSets,
        androidSourceSet: AndroidSourceSet
    ): KotlinSourceSet {
        val sourceSetType = AndroidMppSourceSetType(androidSourceSet)
        val kotlinMppSourceSetName = sourceSetType.kotlinMppSourceSetName(androidTargetName, androidSourceSet)
        return kotlinMppSourceSets.maybeCreate(kotlinMppSourceSetName).also {
            logger.kotlinDebug("Created kotlin mpp source set $kotlinMppSourceSetName for Android source set ${androidSourceSet.name}")
        }
    }

    private fun registerKotlinMppSourceSetInAndroidSourceSet(
        project: Project,
        kotlinSourceSet: KotlinSourceSet,
        androidSourceSet: AndroidSourceSet
    ) = kotlinSourceSet.kotlin.srcDirs.forEach { kotlinSourceDir ->
        androidSourceSet.java.srcDir(kotlinSourceDir)
        logger.kotlinDebug(
            "Registered kotlin mpp source dir ${kotlinSourceDir.path} in " +
                    "Android source set${project.path}/${androidSourceSet.name}"
        )
    }

    private fun configureAndroidResources(project: Project, kotlinMppSourceSet: KotlinSourceSet, androidSourceSet: AndroidSourceSet) {
        androidSourceSet.resources.srcDir(project.file("src/${kotlinMppSourceSet.name}/resources"))
    }

    private fun AndroidMppSourceSetType(androidSourceSet: AndroidSourceSet): AndroidMppSourceSetType {
        return when {
            androidSourceSet.name.startsWith("test") -> AndroidMppSourceSetType.LocalTest
            androidSourceSet.name.startsWith("androidTest") -> AndroidMppSourceSetType.DeviceTest
            /* Safe to assume: "release"/"debug" source sets can still be considered "main" */
            else -> AndroidMppSourceSetType.Main
        }
    }

    private sealed class AndroidSourceSetClassifierReplacement {
        object None : AndroidSourceSetClassifierReplacement() {
            override fun replaceClassifier(sourceSetClassifier: String): String = sourceSetClassifier
        }

        data class PrefixClassifierReplacement(
            val oldClassifierPrefix: String,
            val newClassifierPrefix: String
        ) : AndroidSourceSetClassifierReplacement() {

            override fun replaceClassifier(sourceSetClassifier: String): String {
                /* Only replace prefix of classifier */
                if (sourceSetClassifier.startsWith(oldClassifierPrefix)) {
                    return sourceSetClassifier.replaceFirst(oldClassifierPrefix, newClassifierPrefix)
                }

                return sourceSetClassifier
            }
        }

        abstract fun replaceClassifier(sourceSetClassifier: String): String
    }

    private enum class AndroidMppSourceSetType(
        private val androidSourceSetClassifierReplacement: AndroidSourceSetClassifierReplacement
    ) {
        Main(AndroidSourceSetClassifierReplacement.None),

        LocalTest(
            PrefixClassifierReplacement(
                oldClassifierPrefix = "test",
                newClassifierPrefix = "localTest"
            )
        ),

        DeviceTest(
            PrefixClassifierReplacement(
                oldClassifierPrefix = "androidTest",
                newClassifierPrefix = "deviceTest"
            )
        );

        fun kotlinMppSourceSetName(
            androidTargetName: String,
            sourceSet: AndroidSourceSet
        ): String = lowerCamelCaseName(
            androidTargetName, androidSourceSetClassifierReplacement.replaceClassifier(sourceSet.name)
        )

    }
}

