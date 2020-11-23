#!/usr/bin/env groovy

/**
 * Copyright (C) 2019 CS-SI
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

pipeline {
    agent { label 'snap-test' }
    parameters {
        booleanParam(name: 'launchTests', defaultValue: true, description: 'When true all stages are launched, When false only stages "Package", "Deploy" and "Save installer data" are launched.')
        booleanParam(name: 'runLongUnitTests', defaultValue: true, description: 'When true the option -Denable.long.tests=true is added to maven command so the long unit tests will be executed')
    }
    stages {
        stage('Package and deploy') {
            steps {
                script {
                    def built = build(job: "snap-ci-nightly/master", parameters: [
                        [$class: 'StringParameterValue', name: 'tag', value: "8.x"],
                        [$class: 'StringParameterValue', name: 'docker', value: "snap-ci:master"],
                        [$class: 'StringParameterValue', name: 'engine', value: "8.x"],
                        [$class: 'BooleanParameterValue', name: 'engine_enabled', value: false],
                        [$class: 'StringParameterValue', name: 'desktop', value: "8.x"],
                        [$class: 'BooleanParameterValue', name: 'desktop_enabled', value: false],
                        [$class: 'StringParameterValue', name: 's1tbx', value: "8.x"],
                        [$class: 'BooleanParameterValue', name: 's1tbx_enabled', value: true],
                        [$class: 'StringParameterValue', name: 's2tbx', value: "8.x"],
                        [$class: 'BooleanParameterValue', name: 's2tbx_enabled', value: false],
                        [$class: 'StringParameterValue', name: 's3tbx', value: "8.x"],
                        [$class: 'BooleanParameterValue', name: 's3tbx_enabled', value: false],
                        [$class: 'StringParameterValue', name: 'smos', value: "5.8.x"],
                        [$class: 'BooleanParameterValue', name: 'smos_enabled', value: false],
                        [$class: 'StringParameterValue', name: 'probav', value: "2.2.x"],
                        [$class: 'BooleanParameterValue', name: 'probav_enabled', value: false],
                        [$class: 'StringParameterValue', name: 'installer', value: "8.x"],
                        [$class: 'BooleanParameterValue', name: 'unit_test', value: params.launchTests],
                        [$class: 'BooleanParameterValue', name: 'long_test', value: params.runLongUnitTests],
                    ],
                    quietPeriod: 0,
                    propagate: true,
                    wait: true);
                }
            }
        }
    }
}
