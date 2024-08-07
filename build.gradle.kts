/*
 * Copyright 2019 Scalified
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

allprojects {

	val lombokVersion by extra("1.18.28")
	val javaeeVersion by extra("8.0")
	val commonsLang3Version by extra("3.12.0")
	val axonVersion by extra("4.10.0")

	group = "com.scalified"
	version = axonVersion

	repositories {
		mavenCentral()
	}

	tasks.withType<JavaCompile> {
		sourceCompatibility = "${JavaVersion.VERSION_11}"
		targetCompatibility = "${JavaVersion.VERSION_11}"
		options.encoding = Charsets.UTF_8.name()
	}

}
