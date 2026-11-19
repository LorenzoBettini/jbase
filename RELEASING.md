# Releasing Jbase

Run these commands from the repository root. Use Java 21 and the Maven wrapper.
Before starting, check that the version in
[`jbase.parent/ant/increment_versions.ant`](jbase.parent/ant/increment_versions.ant)
matches the version being released. The release build needs SSH push access to
[`jbase-releases`](https://github.com/LorenzoBettini/jbase-releases).
Run the normal verification build first, since `release-composite` skips tests:

```sh
./mvnw -f jbase.releng/pom.xml clean verify
```

## Publish the composite Eclipse update site

```sh
./mvnw -f jbase.releng/pom.xml -Prelease-composite clean deploy
```

The `release-composite` profile builds the p2 repository, clones `jbase-releases`,
copies the repository into its versioned `releases/` directory, and updates the
composite repositories for all releases and the relevant major and minor versions.
It commits and pushes those changes to the `master` branch of `jbase-releases`.
Check the published site at <https://lorenzobettini.github.io/jbase-releases/>.

## Upload Maven artifacts to Central

The composite update site is built from the snapshot/qualifier versions. After
publishing it, remove those qualifiers for the Maven artifacts:

```sh
ant -f jbase.parent/ant/increment_versions.ant set-version-release
```

Review the version changes before uploading. Configure a Central Portal user
token in Maven's `settings.xml` under the server ID `central`, then run:

```sh
./mvnw -f jbase.maven.releng/pom.xml -Psonatype-oss-release clean deploy
```

The Central Publishing Maven Plugin uploads a bundle for validation. It does
not publish the bundle automatically. Review the validated deployment in the
[Central Portal](https://central.sonatype.com/publishing/deployments) and publish
it there when ready.
