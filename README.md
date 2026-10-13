# template-pure-java

Java best practices.

&nbsp;

## 1. Local Development Machine Prerequisites

* Install Nix as per <https://nix.dev/install-nix>
  * Nix handles all other requirements so that developer machine setup is as minimal as possible
    * Further, it does not matter what version of Nix you have - it guarantees reproducibility
  * If, after an Apple update, nix is not found on PATH, follow <https://github.com/NixOS/nix/issues/7880> and add the snippet to `~/.zshrc` instead

&nbsp;

## 2. Tips

* Run `echo "alias ub='./universal-build'" >> ~/.zshrc` then `source ~/.zshrc` to make `ub` a quick alias for `./universal-build`
* Add the Nix snippet to your `~/.zshrc` so it is never wiped by system updates (snippet: <https://github.com/NixOS/nix/issues/7880>)

&nbsp;

## 3. Main Build Tasks

### `./universal-build build`

* Compiles the code under `src`
* Runs unit tests
* Runs Jacoco
* Runs Checkstyle
* Runs SpotBugs
* Uses Gradle's Distribution plugin to assemble the application JAR and runtime dependency JARs under `build/release/executable/lib`, without generated launch scripts
* Produces application artifacts without constructing an image

### Fleet artifacts and image packaging

Gradle's `installDist`, included in `build`, assembles the application payload under `build/release/executable`. The CI tar command combines that directory with `Dockerfile`, `flake.nix`, `flake.lock`, and `.dockerignore` from the checkout. No staging script or extra copy is needed.

The separate CD job downloads the complete image-build context archive from the same workflow run and packages it using Docker's official Buildx and build-push actions. The workflow pins the action commits, Buildx version, and BuildKit image digest. The archive contains `Dockerfile`, `flake.nix`, `flake.lock`, `.dockerignore`, and `executable/`. CD extracts these directly into its workspace and uses them as the build context. It exports the Linux/ARM64 image directly to an archive.

Packaging consumes the staged artifact and does not run application compilation or tests. The workflow exports `build/oci-image.tar`.

### `./universal-build clean`

* Removes build output directories

### `./universal-build help`

* Invokes Gradle's help command, showing basic functionality

### `./universal-build tasks`

* Invokes Gradle's tasks command, showing all available tasks

&nbsp;

## Release pipeline

Application compilation and image packaging are separate. `.github/workflows/ci.yml` runs application checks on `ubuntu-24.04-arm`, produces the Linux/ARM64 payload under `build/release`, and uploads an archive that preserves executable permissions. On a push to `main`, a separate **CD image packaging** job downloads that same run's named artifact and assembles the image entirely from its contents; the CD job does not check out the repository. Pull requests run the application build and artifact archiving; they do not construct a release image.

Local build outputs are development outputs, never release inputs. Release jobs use the committed `ubuntu-24.04-arm` runner selection and Linux/ARM64 image configuration. The consuming deployment IaC must select the matching runtime platform; no emulation or cross-compilation is used.

Image-builder setup belongs to the GitHub workflow; the consuming deployment pipeline owns runtime testing. The application flake contains application build and runtime dependencies; it does not provide pipeline tools. Nix is installed on the application-build runner and inside the Dockerfile for the runtime environment, but not on the CD runner.

GitHub's artifact actions provide the same-run handoff: packaging waits for the successful application-build job and downloads `release-artifact-${{ github.sha }}`. GitHub automatically checks the uploaded artifact's digest on download; a mismatch is reported as a warning. See [GitHub artifact documentation](https://docs.github.com/en/actions/tutorials/store-and-share-data#validating-artifacts).

The workflow exports an OCI-layout tar archive (`oci-image.tar`) as a pipeline artifact. The consuming deployment pipeline imports or publishes it with an OCI-compatible tool. Registry publication and environment deployment are integration points for the consuming project's infrastructure pipeline. Publish once and promote the same image digest. Select ARM64 in Lambda or Linux/ARM64 in ECS/CDK; matching runtime libraries remain part of native dependency validation.

### Deployment testing

The consuming application's deployment pipeline must test the actual packaged image with its intended entrypoint and configuration:

* Integration tests exercise application behavior with real dependencies in a pre-production environment.
* Smoke tests confirm that a deployed release starts and successfully handles essential requests; for Lambda, invoke the deployed function and check the result.
* Continuous canary tests exercise important workflows. During gradual production rollout, use their results and release-specific error and latency metrics to stop or roll back unhealthy releases.

Publish each candidate once and deploy the same image digest through these stages. These templates provide application checks and image packaging; they do not configure deployment infrastructure or implement integration, smoke, or canary tests. Successful packaging alone does not establish that the application runs correctly.

The rationale and the rejected alternatives are recorded in [Design Rationale](#design-rationale).

## 4. Workflows

### Adding A Java Dependency

1) Find the coordinates on Maven Central (<https://central.sonatype.com>) or the library’s docs.
2) Add the dependency with an explicit version in the right scope:
   * `implementation("group:artifact:version")`
   * `compileOnly("group:artifact:version")`
   * `annotationProcessor("group:artifact:version")`
   * `runtimeOnly("group:artifact:version")`
   * Tests mirror these scopes: `testImplementation`, `testCompileOnly`, `testAnnotationProcessor`, `testRuntimeOnly`
3) Update the Gradle lockfile using the workflow below
4) Run `./universal-build build` to download and verify it resolves; use an IDE “reload Gradle project” if needed.

### Updating Gradle Lockfile

1) Run `./universal-build dependencies --write-locks`

### Adding A Toolchain Dependency

1) Search <https://search.nixos.org/packages> for the package name
2) Add the appropriate package to the package list in the `flake.nix` file

### Updating Nix `nixpkgs` Version

1) Modify the version of `inputs.nixpkgs.url` in the `flake.nix` file to the desired version, according to <https://status.nixos.org>
2) Run `./universal-build --update-nix-flake-lockfile` to update the `flake.lock` lockfile
3) Commit both files to version control

### Ensuring Gradle Files Are Up-To-Date

1) Run `./universal-build wrapper` to update the (this is just a pass-through to `./gradlew wrapper`)
   * This connects to the internet to reset the local self-contained Gradle files to the factory default of the given version. Note that this keeps the same Gradle version.
   * This can help if the Gradle files were stale or modified for some reason (for example, IntelliJ's starting Gradle template gives incorrect files in some cases)

### Upgrading Gradle Version

1) Run `./universal-build wrapper --gradle-version x.y`
   * This connects to the internet to reset the local self-contained Gradle files to the factory default of the given version.

&nbsp;

## 5. Shutdown

Shutdown pattern (implemented in `Main`):

1. SIGTERM (or SIGINT/SIGHUP) fires the shutdown hook, which interrupts the main thread and waits up to `SHUTDOWN_GRACE_PERIOD_SECONDS` seconds for main to finish. Keep that grace period below the platform's SIGTERM-to-SIGKILL timeout (ECS and Kubernetes default to 30s)
2. Main clears the interrupt, performs all resource cleanup in its own `finally` block, then releases the hook
3. The JVM exits after the hook thread returns. On a normal exit the hook returns at once. After a signal the process exits with 128 + the signal number (143 for SIGTERM) regardless of main's exit code

Work must respond to that interrupt:

* The interrupt aborts interruptible waits (sleep, queue and latch waits, `Future.get`, SDK retry backoff) by throwing an exception, which should propagate up to `Main`
* Classic `java.net` socket I/O, `synchronized` and `Lock.lock()` ignore the interrupt, so a call already in flight runs until it returns or times out; normal calls finish well within `SHUTDOWN_GRACE_PERIOD_SECONDS`
* Never swallow `InterruptedException`: declare it, or call `Thread.currentThread().interrupt()` and rethrow it wrapped
* Only `Main` calls `System.exit`; elsewhere throw so the exception reaches `Main`
* CPU-bound loops check `Thread.currentThread().isInterrupted()` between iterations
* Work on other threads is not interrupted; main must stop it in its `finally` block

Correctness is guaranteed by idempotency: dying at any point (including SIGKILL, which bypasses hooks entirely) leaves the system in a consistent state. Graceful shutdown only improves efficiency - it is never required for correctness.

&nbsp;

## 6. Output

The application build produces fleet artifacts; a separate packaging stage produces a Linux/ARM64 container image that runs on container runtimes through `Main` and on AWS Lambda through `LambdaHandler`. Both entry points share the execution and business logic. The `Dockerfile` has one `CMD` line per entry point; keep exactly one uncommented. The AWS integrations are examples; the template can also support other cloud providers.

| Runtime | `CMD` |
|---|---|
| Container, ECS task, Kubernetes, EC2 host (default) | `io.template.Main` |
| AWS Lambda | `com.amazonaws.services.lambda.runtime.api.client.AWSLambda io.template.LambdaHandler::handleRequest` |

The image's entrypoint applies the Nix runtime environment, then runs `java -XX:MaxRAMPercentage=75.0` on the jars in the distribution's `lib/`, followed by the `CMD`. The JDK and OS userland that run in production are the ones pinned by `flake.lock` and the `Dockerfile`, on every runtime.

### OCI / Container (`Main`)

* For a container, ECS task, EC2 host or command-line run

For a Lambda-only application, remove:

* `src/main/java/io/template/Main.java` and its JaCoCo exclude in `build.gradle.kts`
* The Shutdown section above and `closeResources` in `composition/AWSClientsModule.java`, including its unused `Injector` import
* The `Main` `CMD` line in the `Dockerfile`, and uncomment the Lambda one

### AWS Lambda (`LambdaHandler`)

* Lambda runs the image's entrypoint and `CMD`, so `java` starts the Lambda runtime interface client (`AWSLambda`), which polls the Lambda Runtime API and calls `LambdaHandler::handleRequest` for each event
* The event JSON reaches `Executor` as its single input argument
* Publish the packaged image to Amazon ECR. In CDK or another infrastructure tool, configure the Lambda function to use that existing image by digest and select ARM64. Promote the same image through deployment environments; infrastructure deployment does not rebuild it.
* Lambda does not patch the JDK or OS inside an image. Patches arrive when `flake.lock` or the `Dockerfile` base image moves and the image is rebuilt
* The Shutdown section does not apply: Lambda freezes and later discards the execution environment without signaling the process, so clients live for the environment's lifetime

To keep only OCI/container deployment, remove:

* `src/main/java/io/template/LambdaHandler.java` and its JaCoCo exclude in `build.gradle.kts`
* The `aws-lambda-java-core` and `aws-lambda-java-runtime-interface-client` dependencies
* The Lambda `CMD` line in the `Dockerfile`

After removing an entry point, update this Output section and regenerate the Gradle lockfile using the workflow above. Keep the shared environment, execution, Guice modules, and DynamoDB sample unless you also choose to remove that sample.

* Reference: <https://docs.aws.amazon.com/lambda/latest/dg/java-image.html>

&nbsp;

## 7. IDE Setup

### IntelliJ IDEA Ultimate

Follow the instructions here: <https://nader-najjar.notion.site/JetBrains-IDE-Setup-Usage-Guide-d9b0a2b78755822f9d03819f5f02feb2?source=copy_link>
    * Use the command `./universal-build --print-java-path-for-ide` to get the local nix installation of the java version specified in the flake

### Visual Studio Code

Follow the instructions here: <https://nader-najjar.notion.site/Visual-Studio-Code-Setup-Usage-Guide-f4e0a2b7875583be9293817d26459034?source=copy_link>
    * Use the command `./universal-build --print-java-path-for-ide` to get the local nix installation of the java version specified in the flake

&nbsp;

## 8. References

### Nix

* Installation: <https://nix.dev/install-nix>
* Nix Versions: <https://status.nixos.org>
* Nix Package Search: <https://search.nixos.org/packages>

### Maven

* Maven Central Search: <https://central.sonatype.com>

### Jackson

* Jackson Databind Javadoc: <https://javadoc.io/doc/com.fasterxml.jackson.core/jackson-databind/latest/index.html>
* Jackson Databind GitHub README Tutorial: <https://github.com/FasterXML/jackson-databind>
* Jackson Databind Wiki (Including Databind-Specific Annotations): <https://github.com/FasterXML/jackson-databind/wiki>
* Jackson Annotations Javadoc: <https://javadoc.io/doc/com.fasterxml.jackson.core/jackson-annotations/latest/com.fasterxml.jackson.annotation/com/fasterxml/jackson/annotation/package-summary.html>
* Jackson Annotations GitHub README Tutorial: <https://github.com/FasterXML/jackson-annotations>
* Jackson Annotations Wiki: <https://github.com/FasterXML/jackson-annotations/wiki>

### Hibernate

* Main Documentation Page: <https://docs.hibernate.org/validator/9.1/reference/en-US/html_single/>
* Anchor To Available Annotations: <https://docs.hibernate.org/validator/9.1/reference/en-US/html_single/#section-builtin-constraints>

## Design Rationale

Date: 2026-10-11

Status: Accepted; application-build and image-packaging jobs implemented.

### Scope

This record describes the ideal release architecture, including Bazel as the intended build system. This repository is the legacy demonstration: it uses Gradle and Nix, with the custom `universal-build` interface, and does not use Bazel. It demonstrates the same CI/CD separation through a self-replicating, pinned Nix environment for local builds, fleet CI, and the runtime JDK. The goal is hermetic builds; the implemented legacy flow uses Gradle for the Java payload and GitHub Actions for artifact bundling and OCI image packaging. Bazel-specific flows below describe the ideal case rather than this repository's implementation.

### Decision

Separate the application build from release image packaging:

1. Fleet CI builds and tests application artifacts on native Linux runners for the chosen runtime CPU architecture.
2. A separate packaging step consumes those release artifacts and assembles the OCI image.
3. The consuming deployment pipeline publishes the candidate, tests it in deployment environments, and promotes the same image digest through a monitored rollout.

Bazel owns compilation and packaging definitions. The pipeline orchestrates when to invoke them. OCI packaging uses Bazel image rules and does not require Docker or a Dockerfile; runtime testing belongs to the consuming deployment pipeline.

Local builds provide development feedback. Local outputs do not become release inputs. A local macOS build and a Linux release build may produce different platform-specific artifacts without violating hermeticity.

### Constraints

- The standard release target is Linux/ARM64. Release builds use native execution. Emulation and cross-compilation are excluded from the standard path.
- Fleet build runners, release artifacts, OCI images, and deployment targets agree on Linux and CPU architecture. Native toolchains and system libraries must also be compatible with the runtime image.
- Runner OS family/release and CPU architecture, Bazel version, toolchains, dependencies, base-image digests, image platforms, and deployment architecture are declared in committed configuration.
- GitHub-hosted runner labels select OS release and architecture, not an immutable snapshot of every installed package. Build tools and dependencies are controlled through Bazel rather than assumed from the runner's installed software.
- Hermeticity requires pinned, declared inputs and isolation from undeclared host state. Bazel does not guarantee this merely by being present; the build rules must satisfy it.

### Goal and the three options

The goal is one reusable setup that introduces no OS/CPU compatibility failure for any supported application type, including applications with native executables, native libraries, and native package dependencies. Developers may use different workstation OS families and CPU architectures. Emulation and cross-compilation are excluded. A design that works only because today's dependencies happen to be portable is insufficient.

This is a platform-compatibility requirement, not a claim that builds can never fail because of code defects, unavailable infrastructure, or incompatible undeclared dependencies.

#### Option 1: Application build in CI; image packaging in a separate CD stage — selected

Release flow:

```text
Committed source and pinned build configuration
    -> native fleet CI runner matching the target Linux/CPU platform
    -> Bazel compilation, tests, and checks
    -> versioned release artifacts
    -> separate CD packaging stage assembles the OCI image
    -> image publication
    -> pre-production deployment and integration/smoke testing
    -> monitored production rollout of the same image digest to the matching runtime platform
```

Local development flow:

```text
Committed source and pinned build configuration
    -> local Bazel compilation, tests, and checks for the workstation platform
    -> local development outputs, never used as release packaging inputs
```

Required alignment: the fleet application's build platform, release artifacts, image, and runtime agree on target OS family and CPU architecture. Native toolchains and runtime libraries are compatible. The workstation need not match the release platform because its artifacts do not enter the release path.

This avoids both counterexamples below. Release-platform responsibility is enforced by committed fleet, build, packaging, and deployment configuration. Local checks remain development feedback; they do not replace fleet checks or deployment testing of the release image.

#### Option 2: Docker wraps compilation and image packaging — rejected for this goal

Flow, used both locally and in fleet CI:

```text
Committed source
    -> Docker build targeting the runtime OS/CPU
    -> compilation, tests, and checks inside a builder stage
    -> compiled artifacts copied into the runtime-image stage
    -> image publication
    -> pre-production deployment and integration/smoke testing
    -> deployment
```

Required alignment: the builder stage and runtime image target the runtime OS/CPU. With native execution only, the machine executing the builder must also support that target CPU architecture. A Linux VM on macOS can supply Linux, but does not change the host CPU architecture.

**Counterexample:** a developer has an ARM64 Mac while the production target is Linux/x86_64. The Docker build contains a native compilation step targeting x86_64. The ARM64 workstation cannot execute that x86_64 builder natively. Allowing the build to default to ARM64 instead produces artifacts for the wrong release CPU. Emulation, cross-compilation, or moving execution to a matching remote builder would be required; the first two are excluded and the third does not provide the promised complete local build.

**Reason for rejection:** Docker removes the workstation OS-userland mismatch but cannot guarantee a complete native local release build on every workstation CPU. It therefore fails the universal local/fleet goal under the stated constraints.

#### Option 3: Host compilation followed by image construction in the same local/CI build — rejected for this goal

Flow, used both locally and in fleet CI:

```text
Committed source
    -> compilation, tests, and checks directly on the build host
    -> host-built application artifacts
    -> Docker build or OCI assembly copies artifacts into a Linux runtime image
    -> image publication
    -> pre-production deployment and integration/smoke testing
    -> deployment
```

Required alignment: any native host-built artifacts must match the runtime image's OS/CPU and compatible runtime libraries. Merely selecting a Linux image does not convert copied artifacts to Linux.

**Counterexample:** a developer compiles a native Node.js addon on macOS/ARM64, then the image build copies it into a Linux/ARM64 image. The CPU architecture matches, but the macOS native binary cannot be loaded as a Linux shared library. Targeting a Linux/x86_64 image would add a CPU mismatch as well. Pure Java bytecode or JavaScript may work, but that does not make the structure safe for every application type.

**Reason for rejection:** the same build interface accepts platform-specific workstation artifacts as inputs to a different runtime platform. A universal template cannot assume all current and future dependencies are portable.

#### Why Bazel favors the selected option

Bazel supports explicit application-artifact and image-packaging targets, declared toolchains, action caching, and OCI layer assembly. The separate packaging stage can use Bazel OCI rules without Docker or a Dockerfile. The pipeline orchestrates the stages while the repository defines how to produce their outputs.

Options 2 and 3 are rejected for this goal and its constraints, not declared invalid for projects with narrower requirements. The decision follows from the concrete counterexamples, not a claim that companies never use those approaches.

### Artifact boundary

The packaging step must identify and consume the exact versioned fleet artifacts, together with the pinned packaging inputs. It must not substitute local outputs or silently recompile the application under a different environment. Artifact identities must remain traceable to the tested source revision.

GitHub's artifact actions scope the download to the current workflow run. CI checks out the run's source revision and packages all image-build inputs with the compiled payload. CD uses only that archive; the artifact name includes `${{ github.sha }}`. The upload action creates an immutable artifact; download automatically checks its digest, reporting a warning if it differs. This pipeline uses that managed handoff instead of maintaining its own revision stamps and checksum manifests. See [GitHub artifact documentation](https://docs.github.com/en/actions/tutorials/store-and-share-data).

Image construction occurs once per release candidate before runtime validation. Environment promotion changes deployment configuration, not the image contents. Base-image or toolchain updates create new release candidates and go through validation again.

For multiple runtime architectures, produce and validate a release candidate for each architecture. Lambda deployment selects one architecture's image rather than the combined OCI index.

### Pipeline tooling boundary

The committed runner and image configuration select Linux/ARM64; the consuming deployment IaC must select the same runtime platform. Pipeline scripts do not repeat this configuration as runtime assertions or compare a self-assigned platform marker with a constant. The same-run GitHub artifact handoff replaces custom revision markers and checksum manifests.

Application manifests, dependency locks, and Nix flakes define application build and runtime dependencies. Image-builder and container-runtime setup belongs to the pipeline configuration under `.github/`; application build commands do not invoke those pipeline tools.

The Dockerfile templates use Docker's official actions in their CD jobs, with pinned action commits, a pinned Buildx version, and a BuildKit image digest. Nix remains in application CI and inside the Dockerfile for the runtime environment; the CD runner does not install Nix. Bazel continues to assemble images with OCI rules. Packaging does not execute the application image; the Bazel packaging job needs no Docker daemon.

### Deployment testing

The consuming application's deployment pipeline must test the actual packaged image with its intended entrypoint and configuration:

* Integration tests exercise application behavior with real dependencies in a pre-production environment.
* Smoke tests confirm that a deployed release starts and successfully handles essential requests; for Lambda, invoke the deployed function and check the result.
* Continuous canary tests exercise important workflows. During gradual production rollout, use their results and release-specific error and latency metrics to stop or roll back unhealthy releases.

Publish each candidate once and deploy the same image digest through these stages. These templates provide application checks and image packaging; they do not configure deployment infrastructure or implement integration, smoke, or canary tests. Successful packaging alone does not establish that the application runs correctly.

### Implementation state

CD jobs do not check out the source repository. The Dockerfile templates' archives contain the complete Docker build context. The Bazel archive contains a standalone packaging workspace with `image/BUILD.bazel`, the compiled JAR, and the pinned module/tool configuration; its OCI rules are separate from source-compilation definitions. Packaging tools may fetch declared base images and pinned toolchain dependencies.

The Gradle template's `build` includes `installDist`, which assembles application and dependency JARs under `build/release/executable/lib`. The CI tar command bundles that payload with the Dockerfile, runtime Nix files, and ignore file from the checkout. Gradle owns application packaging; the workflow owns the CD artifact bundle, transfer, and image construction.

The templates provide separate application-build and image-packaging jobs on native Linux/ARM64 runners. Packaging depends on the successful application-build job, downloads that workflow run's named artifact, and exports the image archive. Bazel OCI assembly has no application-compilation dependency. The legacy JavaScript fleet build compiles the Lambda client's native addon before artifact transfer.

The workflow exports an OCI-layout tar archive named `oci-image.tar`, using the shared OCI image format. The Dockerfile workflows select the OCI exporter; Bazel archives its OCI layout directly without a Docker-format conversion or container-loader target. Registry publication and environment deployment belong to the consuming project's infrastructure pipeline and are not configured here. The packaging workflow does not establish runtime correctness; deployment tests must do so.

The release path needs successful fleet execution. This legacy Java change does not require regenerating its existing dependency lockfiles. This record does not claim that the existing local builds have been audited for full hermeticity.
