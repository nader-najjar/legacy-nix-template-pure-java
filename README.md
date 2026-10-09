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
* Builds a container image with Podman, set to run the code entrypoint (tagged `<project-name>:latest`)
* Saves the image as `build/container-image.tar`
* Builds a Lambda deployment package as `build/lambda-deployment-package.zip`

### `./universal-build clean`

* Removes build output directories

### `./universal-build help`

* Invokes Gradle's help command, showing basic functionality

### `./universal-build tasks`

* Invokes Gradle's tasks command, showing all available tasks

&nbsp;

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

## 6. Outputs

The template currently supports two deployment paths: an OCI/container image through `Main`, and AWS Lambda through `LambdaHandler`. Keep only the path your application uses and remove the other using the instructions below. Both paths share the execution and business logic. The AWS integrations are examples; the template can also support other cloud providers.

### OCI / Container (`Main`)

* For a container, ECS task, EC2 host or command-line run; the container image runs it

To keep only Lambda, remove:

* `src/main/java/io/template/Main.java` and its JaCoCo exclude in `build.gradle.kts`
* The Shutdown section above and `closeResources` in `composition/AWSClientsModule.java`, including its unused `Injector` import
* `containerImageName`, `podmanBuildImage`, `podmanSaveImageTar`, and `executeContainerImageSmokeTest` in `build.gradle.kts`; remove only the `podmanSaveImageTar` dependency from `build`, keeping the Lambda package dependency
* The `application` plugin declaration and `application { ... }` configuration in `build.gradle.kts`; keep the `java` plugin for compilation and JAR packaging
* `Dockerfile`, `.dockerignore`, and the container output bullets under Main Build Tasks

### AWS Lambda (`LambdaHandler`)

* Handler: `io.template.LambdaHandler::handleRequest`, on the `java25` runtime
* The event JSON reaches `Executor` as its single input argument
* `buildLambdaDeploymentPackage` (run by `build`) zips the application jar and every runtime classpath jar into the zip's `lib/`, which the Lambda Java runtime puts on the classpath
* Deploy the zip with the infrastructure tool of your choice, for example CDK's `lambda.Code.fromAsset("build/lambda-deployment-package.zip")`
* The Shutdown section does not apply: Lambda freezes and later discards the execution environment without signaling the process, so clients live for the environment's lifetime

To keep only OCI/container deployment, remove:

* `src/main/java/io/template/LambdaHandler.java` and its JaCoCo exclude in `build.gradle.kts`
* `buildLambdaDeploymentPackage` and only its dependency in the `build` task, keeping the container build dependency
* The `aws-lambda-java-core` dependency and the Lambda output bullet under Main Build Tasks

After removing a deployment path, update this Outputs section and regenerate the Gradle lockfile using the workflow above. Keep the shared environment, execution, Guice modules, and DynamoDB sample unless you also choose to remove that sample.

* Reference: <https://docs.aws.amazon.com/lambda/latest/dg/java-package.html>

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
