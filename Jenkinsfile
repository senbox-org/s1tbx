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
    environment {
        toolName = sh(returnStdout: true, script: "echo ${env.JOB_NAME} | cut -d '/' -f 1").trim()
        branchVersion = sh(returnStdout: true, script: "echo ${env.GIT_BRANCH} | cut -d '/' -f 2").trim()
        toolVersion = ''
        deployDirName = ''
        snapMajorVersion = ''
        sonarOption = ""
        longTestsOption = ""
    }
    agent { label 'snap-test' }
    parameters {
        booleanParam(name: 'launchTests', defaultValue: true, description: 'When true all stages are launched, When false only stages "Package", "Deploy" and "Save installer data" are launched.')
        booleanParam(name: 'runLongUnitTests', defaultValue: true, description: 'When true the option -Denable.long.tests=true is added to maven command so the long unit tests will be executed')
    }
    stages {
        stage('Package and deploy') {
            agent {
                docker {
                    label 'snap-test'
                    image 'snap-build-server.tilaa.cloud/maven:3.6.0-jdk-8'
                    args '-e MAVEN_CONFIG=/var/maven/.m2 -v /data/ssd/testData/:/data/ssd/testData/ -v /opt/maven/.m2/settings.xml:/var/maven/.m2/settings.xml -v docker_local-update-center:/local-update-center'
                }
            }
            steps {
                script {
                    // Get snap version from pom file
                    toolVersion = sh(returnStdout: true, script: "cat pom.xml | grep '<version>' | head -1 | cut -d '>' -f 2 | cut -d '-' -f 1 | cut -d '<' -f 1").trim()
                    snapMajorVersion = sh(returnStdout: true, script: "echo ${toolVersion} | cut -d '.' -f 1").trim()
                    deployDirName = "${toolName}/${branchVersion}-${toolVersion}-${env.GIT_COMMIT}"
                    sonarOption = ""
                    if ("${branchVersion}" == "master") {
                        // Only use sonar on master branch
                        sonarOption = "sonar:sonar"
                    }
                    longTestsOption = ""
                    if("${params.runLongUnitTests}" == "true") {
                        longTestsOption = "-Denable.long.tests=true"
                    }
                }
                echo "Build Job ${env.JOB_NAME} from ${env.GIT_BRANCH} with commit ${env.GIT_COMMIT}"
                sh "mvn -Duser.home=/var/maven -Dsnap.userdir=/home/snap clean package install ${sonarOption} ${longTestsOption} -U -DskipTests=false -Ds1tbx.tests.data.dir=/data/ssd/testData/s1tbx/"
            }
            post {
                always {
                    junit "**/target/surefire-reports/*.xml"
                    jacoco(execPattern: '**/*.exec')
                }
                success {
                    script {
                        if ("${env.GIT_BRANCH}" == 'master' || "${env.GIT_BRANCH}" =~ /\d+\.x/ || "${env.GIT_BRANCH}" =~ /\d+\.\d+\.\d+(-rc\d+)?$/) {
                            echo "Deploy ${env.JOB_NAME} from ${env.GIT_BRANCH} with commit ${env.GIT_COMMIT}"
                            sh "mvn -Duser.home=/var/maven -Dsnap.userdir=/home/snap deploy -DskipTests=true"
                            sh "/opt/scripts/saveToLocalUpdateCenter.sh *-kit/target/netbeans_site/ ${deployDirName} ${branchVersion} ${toolName}"}
                    }
                }
            }
        }
        stage('Save installer data') {
            agent {
                docker {
                    label 'snap-test'
                    image 'snap-build-server.tilaa.cloud/scripts:1.0'
                    args '-v docker_snap-installer:/snap-installer'
                }
            }
            when {
                expression {
                    // We save snap installer data on master branch and branch x.x.x (Ex: 8.0.0) or branch x.x.x-rcx (ex: 8.0.0-rc1) when we want to create a release
                    return ("${env.GIT_BRANCH}" == 'master' || "${env.GIT_BRANCH}" =~ /\d+\.\d+\.\d+(-rc\d+)?$/);
                }
            }
            steps {
                echo "Save data for SNAP Installer ${env.JOB_NAME} from ${env.GIT_BRANCH} with commit ${env.GIT_COMMIT}"
                sh "/opt/scripts/saveInstallData.sh ${toolName} ${env.GIT_BRANCH}"
            }
        }
        stage('Create SNAP Installer') {
            agent { label 'snap-test' }
            when {
                expression {
                    return ("${env.GIT_BRANCH}" == 'master' || "${env.GIT_BRANCH}" =~ /\d+\.\d+\.\d+(-rc\d+)?$/) && "${params.launchTests}" == "true";
                }
            }
            steps {
                echo "Launch snap-installer"
                build job: "snap-installer/${env.GIT_BRANCH}"
            }
        }
    }
    /* disable email send on failure
    post {
        failure {
            step (
                emailext(
                    subject: "[SNAP] JENKINS-NOTIFICATION: ${currentBuild.result ?: 'SUCCESS'} : Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                    body: """Build status : ${currentBuild.result ?: 'SUCCESS'}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':
Check console output at ${env.BUILD_URL}
${env.JOB_NAME} [${env.BUILD_NUMBER}]""",
                    attachLog: true,
                    compressLog: true,
                    recipientProviders: [[$class: 'CulpritsRecipientProvider'], [$class:'DevelopersRecipientProvider']]
                )
            )
        }
    }*/
}
