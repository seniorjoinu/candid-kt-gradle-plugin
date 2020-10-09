package com.github.seniorjoinu.candid.plugin

import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Plugin
import org.gradle.api.Project

const val CANDIDKT_EXTENSION_NAME = "candid"
const val CANDIDKT_GROUP_NAME = "candid"
const val CANDIDKT_TASK_NAME = "generateCandidKt"
const val CANDIDKT_TASK_DESTINATION_PREFIX = "generated/sources/candid/kotlin"

/**
 * The Candid to Kotlin plugin.
 */
abstract class CandidKtPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Add the extension object
        project.extensions.create(CANDIDKT_EXTENSION_NAME, CandidKtExtension::class.java, project)
        project.candidExtension.apply {
            fun candidSourceSetContainer(factory: NamedDomainObjectFactory<CandidSourceSet>) = project.container(CandidSourceSet::class.java, factory)

            sourceSets = candidSourceSetContainer(candidSourceSetFactory(project))
        }

        // Add the 'main' candid source set
        project.candidExtension.sourceSets.maybeCreate(CandidSourceSet.SOURCE_SET_NAME_MAIN)

        // Add a task for each source set using the configuration from the extension object
        project.candidExtension.sourceSets.all {
            registerSourceTask(project, it.name)
        }
    }

    private fun candidSourceSetFactory(project: Project): NamedDomainObjectFactory<CandidSourceSet> = DefaultCandidSourceSetFactory(project)

    private fun registerSourceTask(project: Project, sourceSetName: String) = with(project.candidExtension) {
        val sourceSet = project.candidExtension.sourceSets.getByName(sourceSetName)
        val taskName = if (sourceSetName == CandidSourceSet.SOURCE_SET_NAME_MAIN) CANDIDKT_TASK_NAME else CANDIDKT_TASK_NAME.replace("generate", "generate${sourceSetName.capitalize()}")
        project.tasks.register(taskName, CandidKtTask::class.java) { candidKtTask ->
            candidKtTask.description = "Generates Kotlin sources from Candid language files resolved from the '$sourceSetName' Candid source set."
            candidKtTask.group = CANDIDKT_GROUP_NAME
            candidKtTask.sourceSetName = sourceSetName
            candidKtTask.genPackage.set(genPackage)
            candidKtTask.sourceSet.takeIf { it != sourceSet }?.dependsOn(sourceSet)
            candidKtTask.source(sourceSet.candid)
            candidKtTask.didFiles += sourceSet.candid
        }
    }
}
