<!-- REMEMBER TO UPDATE THE GITHUB BADGE -->

[![Github actions](https://github.com/wxleong/remote-attestation-optiga-tpm/actions/workflows/main.yml/badge.svg)](https://github.com/wxleong/remote-attestation-optiga-tpm/actions)

# Introduction

Remote attestation is a mechanism that allows a remote system (server) to verify the integrity of another system's platform (e.g., a Raspberry Pi®). In a Linux-based system, the Integrity Measurement Architecture (IMA) can be used to capture platform measurements. Combined with a Trusted Platform Module (TPM), which provides hardware-based security and attestation features, this setup can authenticate and protect the IMA measurements, ensuring the integrity of the platform.

---

# Table of Contents

- **[Prerequisites](#prerequisites)**
- **[Preparing the Environment](#preparing-the-environment)**
    - **[One-Time Setup](#one-time-setup)**
    - **[Recurring Setup](#recurring-setup)**
- **[Default Yocto Build](#default-yocto-build)**
- **[GitHub Actions](#github-actions)**
- **[License](#license)**

---

<!--section:dummy-->

# Prerequisites

The integration guide has been CI tested for compatibility with the following operating system in a Docker container. Please refer to section [GitHub Actions](#github-actions):

- Operating System: Ubuntu (22.04)

---

# Preparing the Environment

## One-Time Setup

Install the dependencies:
```all
$ sudo apt-get update
$ sudo apt-get install -y gawk wget git-core diffstat unzip texinfo gcc-multilib \
     build-essential chrpath socat libsdl1.2-dev xterm zstd liblz4-tool qemu-system \
     cpio file
```

Fetch the poky repository:
```all
$ cd ~
$ git clone http://git.yoctoproject.org/git/poky ~/poky
$ cd ~/poky
$ git checkout yocto-5.0.2
```

## Recurring Setup

# Default Yocto Build

Set up the build environment:
```all
$ source oe-init-build-env
```

Build the `core-image-full-cmdline`:
> core-image-full-cmdline: A console-only image with more full-featured Linux system functionality installed. [More information here](https://docs.yoctoproject.org/ref-manual/images.html#images).
```all
$ bitbake -k core-image-full-cmdline
```

---

# GitHub Actions

This section is intended for maintainers only.

GitHub Actions has been set up to automate the testing of this integration guide as part of the CI/CD process. The testing methodology involves extracting command lines from the markdown and running them in a Docker container. Please refer to [script.sh](.github/docker/script.sh) to see the preparation of commands for testing. The process occurs seamlessly in the background, utilizing hidden tags in the README.md (visible in raw mode).

Commands are divided into sections using the markers `<!--section:xxx-->` and `<!--section-end-->`. This structure allows for the execution of specific sections multiple times (e.g., housekeeping) and allows for the flexibility of reordering or creating new test sequences. The test sequence construction (e.g., section 1 -> section 2 -> section 3 -> ...) is configurable.

To learn about the CI test sequence, refer to the README.md in raw mode and specifically look for `<!--tests:`.

For debugging, it is possible to download the executed test scripts from the Artifacts of a GitHub Actions run.

---

# License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

<!--section-end-->

<!--tests:
dummy;
-->
