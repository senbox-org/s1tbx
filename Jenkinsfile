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
    }
    agent { label 'snap-test' }
    parameters {
        booleanParam(name: 'launchTests', defaultValue: true, description: 'When true all stages are launched, When false only stages "Package", "Deploy" and "Save installer data" are launched.')
    }
    stages {
        stage('Package') {
            agent {
                docker {
                    label 'snap-test'
                    image 'snap-build-server.tilaa.cloud/maven:3.6.0-jdk-8'
                    args '-e MAVEN_CONFIG=/var/maven/.m2 -v /data/ssd/testData/:/data/ssd/testData/ -v /opt/maven/.m2/settings.xml:/home/snap/.m2/settings.xml'
                }
            }
            steps {
                script {
                    // Get snap version from pom file
                    toolVersion = sh(returnStdout: true, script: "cat pom.xml | grep '<version>' | head -1 | cut -d '>' -f 2 | cut -d '-' -f 1").trim()
                    snapMajorVersion = sh(returnStdout: true, script: "echo ${toolVersion} | cut -d '.' -f 1").trim()
                    deployDirName = "${toolName}/${branchVersion}-${toolVersion}-${env.GIT_COMMIT}"
                }
                echo "Build Job ${env.JOB_NAME} from ${env.GIT_BRANCH} with commit ${env.GIT_COMMIT}"
                sh "mvn -Dm2repo=/var/tmp/repository/ -Duser.home=/home/snap -Dsnap.userdir=/home/snap clean package install -U -DskipTests=false"
            }
        }
        stage('Deploy') {
            agent {
                docker {
                    label 'snap-test'
                    image 'snap-build-server.tilaa.cloud/maven:3.6.0-jdk-8'
                    args '-e MAVEN_CONFIG=/var/maven/.m2 -v /opt/maven/.m2/settings.xml:/home/snap/.m2/settings.xml -v docker_local-update-center:/local-update-center'
                }
            }
            when {
                expression {
                    return "${env.GIT_BRANCH}" == 'master' || "${env.GIT_BRANCH}" =~ /\d+\.x/ || "${env.GIT_BRANCH}" =~ /\d+\.\d+\.\d+(-rc\d+)?$/);
                }
            }
            steps {
                echo "Deploy ${env.JOB_NAME} from ${env.GIT_BRANCH} with commit ${env.GIT_COMMIT}"
                sh "mvn -Dm2repo=/var/tmp/repository/ -Duser.home=/home/snap -Dsnap.userdir=/home/snap deploy -U -DskipTests=true"
                sh "/opt/scripts/saveToLocalUpdateCenter.sh . ${deployDirName} ${branchVersion} ${toolName}"
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
                sh "/opt/scripts/saveInstallData.sh ${toolName}"
            }
        }
        stage('Launch SNAP Desktop build') {
            agent { label 'snap-test' }
            when {
                expression {
                    return ("${env.GIT_BRANCH}" == 'master' || "${env.GIT_BRANCH}" =~ /\d+\.\d+\.\d+(-rc\d+)?$/);
                }
            }
            steps {
                echo "Launch snap-desktop build"
                build job: "snap-desktop/${env.GIT_BRANCH}", parameters: [
                    [$class: 'BooleanParameterValue', name: 'launchTests', value: Boolean.valueOf("${params.launchTests}") ]
                ],
                quietPeriod: 0,
                propagate: true,
                wait: true
            }
        }
        stage('Create docker image') {
            agent {
                docker {
                    label 'snap-test'
                    image 'snap-build-server.tilaa.cloud/scripts:1.0'
                    // We add the docker group from host (i.e. 999)
                    args ' --group-add 999 -v /var/run/docker.sock:/var/run/docker.sock -v /usr/bin/docker:/bin/docker -v /usr/lib/x86_64-linux-gnu/libltdl.so.7:/usr/lib/x86_64-linux-gnu/libltdl.so.7 -v docker_local-update-center:/local-update-center -v /opt/maven/.docker:/home/snap/.docker -v docker_snap-installer:/snap-installer'
                }
            }
            when {
                expression {
                    return "${env.GIT_BRANCH}" =~ /\d+\.x/;
                }
            }
            steps {
                echo "Create docker image of ${env.JOB_NAME} from ${env.GIT_BRANCH} using commit ${env.GIT_COMMIT}"
                script {
                    dockerName = "${toolName}:${branchVersion}-${toolVersion}-${env.GIT_COMMIT}"
                }
                // Launch deploy script
                sh "/opt/scripts/deploy.sh ${snapMajorVersion} ${deployDirName} ${branchVersion} ${dockerName} ${toolName}"
            }
        }
        stage ('Starting Tests') {
            parallel {
                stage ('Starting GPT Tests') {
                    agent { label 'snap-test' }
                    when {
                        expression {
                            return "${env.GIT_BRANCH}" =~ /\d+\.x/;
                        }
                    }
                    steps {
                        echo "Launch snap-gpt-tests using docker image snap:${branchVersion} and scope REGULAR"
                        build job: "snap-gpt-tests/${branchVersion}", parameters: [
                            [$class: 'StringParameterValue', name: 'dockerTagName', value: "snap:${branchVersion}"],
                            [$class: 'StringParameterValue', name: 'testScope', value: "REGULAR"]
                        ]
                    }
                }
                stage ('Starting GUI Tests') {
                    agent { label 'snap-test' }
                    when {
                        expression {
                            return "${env.GIT_BRANCH}" =~ /\d+\.x/;
                        }
                    }
                    steps {
                        echo "Launch snap-gui-tests using docker image snap:${branchVersion}"
                        build job: "snap-gui-tests/${branchVersion}", parameters: [
                            [$class: 'StringParameterValue', name: 'dockerTagName', value: "snap:${branchVersion}"],
                            [$class: 'StringParameterValue', name: 'testFileList', value: "qftests.lst"]
                        ]
                    }
                }
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
