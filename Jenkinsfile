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
    agent any
    stages {
        stage('Package') {
            agent {
                docker {
                    image 'snap-build-server.tilaa.cloud/maven:3.6.0-jdk-8'
                    // We add the docker group from host (i.e. 999)
                    args ' --group-add 999 -e MAVEN_CONFIG=/var/maven/.m2 -v /var/run/docker.sock:/var/run/docker.sock -v /usr/bin/docker:/bin/docker -v /opt/maven/.m2/settings.xml:/var/maven/.m2/settings.xml -v docker_local-update-center:/local-update-center'
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
                // sh 'mvn -Duser.home=/var/maven -Dsnap.userdir=/home/snap clean package install -U -Dsnap.reader.tests.data.dir=/data/ssd/s2tbx/ -Dsnap.reader.tests.execute=false -DskipTests=${launchUnitTests}'
                sh "/opt/scripts/saveToLocalUpdateCenter.sh *-kit/target/netbeans_site/ ${deployDirName}"
            }
        }
        stage('Deploy') {
            agent {
                docker {
                    image 'snap-build-server.tilaa.cloud/scripts:1.0'
                    // We add the docker group from host (i.e. 999)
                    args ' --group-add 999 -v /var/run/docker.sock:/var/run/docker.sock -v /usr/bin/docker:/bin/docker -v docker_local-update-center:/local-update-center -v /home/obarrilero/.docker:/home/snap/.docker'
                }
            }
            steps {
                echo "Deploy ${env.JOB_NAME} from ${env.GIT_BRANCH} using commit ${env.GIT_COMMIT}"
                script {
                    dockerName = "${toolName}:${branchVersion}-${toolVersion}-${env.GIT_COMMIT}"
                }
                // Launch deploy script
                sh "/opt/scripts/deploy.sh ${snapMajorVersion} ${deployDirName} ${branchVersion} ${dockerName} ${toolName}"
            }
        }
        stage('Pre-release') {
            agent {
                docker {
                    image 'snap-build-server.tilaa.cloud/scripts:1.0'
                    // We add the docker group from host (i.e. 999)
                    args ' --group-add 999 -v /var/run/docker.sock:/var/run/docker.sock -v /usr/bin/docker:/bin/docker -v docker_local-update-center:/local-update-center -v /home/obarrilero/.docker:/home/snap/.docker'
                }
            }
            when {
                expression {
                    return "${env.GIT_BRANCH}" == 'master' || "${env.GIT_BRANCH}" =~ "/.\\.x/" || "${env.GIT_BRANCH}" == "testJenkins_validation";
                }
            }
            steps {
                script {
                    dockerName = "${toolName}:${branchVersion}"
                    deployDirName = "${toolName}/${branchVersion}"
                }
                echo "Pre release from ${env.GIT_BRANCH} using commit ${env.GIT_COMMIT}"
                sh "/opt/scripts/deploy.sh ${snapMajorVersion} ${deployDirName} ${branchVersion} ${dockerName} ${toolName}"
            }
        }
        stage ('Starting Tests') {
            agent any
            when {
                expression {
                    return "${env.GIT_BRANCH}" == 'master' || "${env.GIT_BRANCH}" =~ "/.\\.x/" || "${env.GIT_BRANCH}" == "testJenkins_validation";
                }
            }
            steps {
                build job: 'snap-gpt-tests/master', parameters: [[$class: 'StringParameterValue', name: 'commitHash', value: "${env.GIT_COMMIT}"],[$class: 'StringParameterValue', name: 'toolVersion', value: "${toolVersion}"]]
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
