node {
   def mavenProfiles = ""
   def mavenArguments = "clean verify"
   def hasToDeploye = false
   def ideTests = false
   def isSnapshot = false
   if (env.JOB_NAME.endsWith("release")) {
     mavenProfiles = "-Prelease-composite"
     hasToDeploye = true
     mavenArguments = "clean deploy"
   } else if (env.JOB_NAME.endsWith("release-snapshot")) {
     mavenProfiles = "-Prelease-composite,release-snapshots"
     hasToDeploye = true
     isSnapshot = true
     mavenArguments = "clean deploy"
   } else {
     mavenProfiles = ""
     ideTests = true
   }
   properties([
     [$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', numToKeepStr: '30']]
   ])
   stage('Preparation') { // for display purposes
      checkout scm
   }
   if (!hasToDeploye) {
      stage('Build and Test') {
         wrap([$class: 'Xvfb', autoDisplayName: true]) {
           if (ideTests) {
             sh "mutter --replace --sm-disable 2> mutter.err &"
           }
           // Run the maven build
           // don't make the build fail in case of test failures...
           sh (script:
             "./mvnw -f jbase.releng/pom.xml -Dmaven.test.failure.ignore=true -fae ${mavenProfiles} ${mavenArguments}",
           )
         }
      }
      stage('JUnit Results') {
         // ... JUnit archiver will set the build as UNSTABLE in case of test failures
         junit '**/target/surefire-reports/TEST-*.xml'
         archive '**/target/repository/'
      }
   } else {
      stage('Build and Deploy P2 Artifacts') {
         sh (script:
           "./mvnw -f jbase.releng/pom.xml ${mavenProfiles} ${mavenArguments}",
         )
      }
      if (!isSnapshot) {
         stage('Remove SNAPSHOT') {
            sh (script:
              "ant -f jbase.parent/ant/increment_versions.ant set-version-release",
            )
         }
      }
      stage('Build and Deploy Maven Artifacts') {
         sh (script:
           "./mvnw -f jbase.maven.releng/pom.xml -Dmaven.repo.local='${env.WORKSPACE}'/.repository ${mavenOnlyProfile} -Psonatype-oss-release clean deploy",
         )
      }
   }
}
