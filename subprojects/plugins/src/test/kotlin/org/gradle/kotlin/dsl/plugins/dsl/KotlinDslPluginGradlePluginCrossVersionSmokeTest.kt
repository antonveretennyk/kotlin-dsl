/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.kotlin.dsl.plugins.dsl

import org.gradle.kotlin.dsl.embeddedKotlinVersion
import org.gradle.kotlin.dsl.fixtures.AbstractPluginTest

import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized


@RunWith(Parameterized::class)
class KotlinDslPluginGradlePluginCrossVersionSmokeTest(

    private
    val kotlinVersion: String

) : AbstractPluginTest() {

    companion object {

        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun testedKotlinVersions() = listOf(
            "1.3.10",
            embeddedKotlinVersion,
            "1.2.20"
        )
    }

    @Test
    fun `kotlin-dsl plugin in buildSrc and production code using kotlin-gradle-plugin `() {

        withDefaultSettingsIn("buildSrc")
        withBuildScriptIn("buildSrc", """
            import org.jetbrains.kotlin.config.KotlinCompilerVersion

            plugins {
                `kotlin-dsl`
            }

            repositories {
                jcenter()
            }

            dependencies {
                implementation(kotlin("gradle-plugin", "$kotlinVersion"))
            }

            println("buildSrc build script classpath kotlin compiler version " + KotlinCompilerVersion.VERSION)
        """)
        withFile("buildSrc/src/main/kotlin/my-plugin.gradle.kts", """
            apply<org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJvmPlugin>()
        """)

        withDefaultSettings()
        withBuildScript("""
            import org.jetbrains.kotlin.config.KotlinCompilerVersion

            plugins {
                `my-plugin`
            }

            repositories {
                jcenter()
            }

            dependencies {
                implementation(kotlin("stdlib"))
            }

            println("root build script classpath kotlin compiler version " + KotlinCompilerVersion.VERSION)
        """)
        withFile("src/main/kotlin/SomeSource.kt", "fun main(args: Array<String>) {}")

        buildWithPlugin("classes").apply {
            assertThat(
                output,
                allOf(
                    containsString("buildSrc build script classpath kotlin compiler version $embeddedKotlinVersion"),
                    containsString("root build script classpath kotlin compiler version $kotlinVersion")
                )
            )
        }
    }
}