FROM ubuntu:22.04@sha256:104ae83764a5119017b8e8d6218fa0832b09df65aae7d5a6de29a85d813da2fb

#
# 1. OS packages, filesystem layout, and user creation
#     - Within reason, the versions of these apt dependencies do not matter
#     - Creates a non-root user for both required Nix compatibility, and for security best practices
#     - Creates the directory that will hold our software; it is owned by the non-root user
#

ENV OS_USER=customuser
ENV OS_USER_GROUP=custom
ENV SOFTWARE_DIRECTORY=/custom-software

RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y \
      ca-certificates \
      curl \
      xz-utils \
      bash \
      coreutils \
      sudo && \
    rm -rf /var/lib/apt/lists/*

RUN mkdir -p "${SOFTWARE_DIRECTORY}"

RUN groupadd -r "${OS_USER_GROUP}" && useradd -m -r -g "${OS_USER_GROUP}" "${OS_USER}" && \
    echo "${OS_USER} ALL=(root) NOPASSWD: ALL" > "/etc/sudoers.d/${OS_USER}" && chmod 440 "/etc/sudoers.d/${OS_USER}" && \
    chown "${OS_USER}":"${OS_USER_GROUP}" "${SOFTWARE_DIRECTORY}"

USER ${OS_USER}

#
# 2. Nix installation
#     - The Nix version only matters in one aspect: it must be new enough to support new features
#     - Aside from that, version does not matter as it is declarative based on the lockfile and flake
#

RUN curl -L https://nixos.org/nix/install | sh -s -- --no-daemon
ENV PATH=/home/${OS_USER}/.nix-profile/bin:$PATH
ENV NIX_CONFIG="experimental-features = nix-command flakes"

#
# 3. Snapshot the runtime environment from the flake
#     - We snapshot the runtime dev shell environment once at image build time, then reuse it at runtime without invoking Nix again
#     - This is for security reasons and for minimizing runtime complexity
#

COPY flake.nix flake.lock ${SOFTWARE_DIRECTORY}/buildtime-workspace/

RUN mkdir -p ${SOFTWARE_DIRECTORY}/runtime-workspace && \
    nix print-dev-env ${SOFTWARE_DIRECTORY}/buildtime-workspace/#runtime > ${SOFTWARE_DIRECTORY}/runtime-workspace/nix-env.sh

#
# 4. Application bits and entrypoint
#     - Copies the Gradle application distribution produced by installDist
#     - Creates an entrypoint runtime wrapper that:
#         - Applies the flake-defined runtime dev shell environment recorded at image build time
#         - Delegates to the Gradle-generated launch script
#

COPY build/install/template-pure-java ${SOFTWARE_DIRECTORY}/runtime-workspace/executable

RUN printf '%s\n' \
  '#!/usr/bin/env bash' \
  'set -euo pipefail' \
  'set -a' \
  ". ${SOFTWARE_DIRECTORY}/runtime-workspace/nix-env.sh" \
  'set +a' \
  "exec ${SOFTWARE_DIRECTORY}/runtime-workspace/executable/bin/template-pure-java \"\$@\"" \
  > ${SOFTWARE_DIRECTORY}/runtime-workspace/run-application && \
  chmod +x ${SOFTWARE_DIRECTORY}/runtime-workspace/run-application

ENTRYPOINT ["sh", "-c", "exec \"${SOFTWARE_DIRECTORY}/runtime-workspace/run-application\" \"$@\"", "unusedAndUnpassedArgToAccountForShCOffset"]
CMD []
