# Releasing

Every push to `master` runs `maven_release.yml`, which builds, signs with the
organisation key and publishes to Maven Central through the `deploy`
environment. While the version in `pom.xml` ends in `-SNAPSHOT` that publishes a
snapshot; a release is the same push with a release version. So a release is
three pull requests and one tag, the way 0.2.11 was cut in #168 and 0.2.12 in
#194.

## Before

- `master` is green on JDK 17, 21 and 25, and the last `maven_release.yml` run
  on `master` succeeded.
- Every change meant for the release is merged. Nothing merges on its author's
  approval alone: one approval from another collaborator, or no objection after
  a week (issue #71).
- The release notes are drafted from the merged pull requests, with authors
  credited by handle.

## Steps

1. Open a pull request titled `0.2.N release` that changes the version and the
   scm `<tag>` in `pom.xml` from `0.2.N-SNAPSHOT` to `0.2.N`. Get it approved
   like any other change, then merge it. The push to `master` publishes `0.2.N`
   to Maven Central; wait for the workflow to finish and for the version to
   appear at
   https://repo1.maven.org/maven2/org/openapitools/jackson-databind-nullable/.
2. Tag the merge commit `v0.2.N` and push the tag. Create the GitHub release
   from the tag with the drafted notes.
3. Open and merge a pull request `prepare 0.2.N+1 snapshot` that sets the
   version back to `0.2.N+1-SNAPSHOT` (and the scm `<tag>` with it). Nothing
   else merges between steps 1 and 3: every push to `master` runs the deploy
   again, and a second publish of `0.2.N` fails.
4. Open the version bump in OpenAPITools/openapi-generator, where the Java
   templates pin `jackson-databind-nullable-version`, as #24501 did for 0.2.11.

## If something goes wrong

A failed `maven_release.yml` run on the release commit publishes nothing;
fix `master` and push again, the workflow reruns on every push. A version that
reached Maven Central cannot be removed; ship `0.2.N+1` with the fix instead.
