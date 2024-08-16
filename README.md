<!-- REMEMBER TO UPDATE THE GITHUB BADGE -->

[![Github actions](https://github.com/wxleong/remote-attestation-optiga-tpm/actions/workflows/main.yml/badge.svg)](https://github.com/wxleong/remote-attestation-optiga-tpm/actions)

# Introduction

Remote attestation is a mechanism that allows a remote system (server) to verify the integrity of another system's platform (e.g., a Raspberry Pi®). In a Linux-based system, the Integrity Measurement Architecture (IMA) can be used to capture platform measurements. Combined with a Trusted Platform Module (TPM), which provides hardware-based security and attestation features, this setup can authenticate and protect the IMA measurements, ensuring the integrity of the platform.

Please follow the guide sequentially without skipping any sections unless instructed otherwise. There is no guarantee it will work if any steps are skipped.

The project uses the Yocto framework as the build system.

---

# Table of Contents

- **[Prerequisites](#prerequisites)**
- **[Preparing the Environment](#preparing-the-environment)**
    - **[One-Time Setup](#one-time-setup)**
    - **[Recurring Setup](#recurring-setup)**
- **[Default Yocto Build](#default-yocto-build)**
    - **[Build](#build)**
    - **[QEMU](#qemu)**
- **[Enable IMA](#enable-ima)**
- **[Enable TPM2](#enable-tpm2)**
- **[ARM64](#arm64)**
- **[GitHub Actions](#github-actions)**
- **[License](#license)**

---

<!--section:dummy-->

# Prerequisites

The integration guide has been CI tested for compatibility with the following operating system in a Docker container. Please refer to section [GitHub Actions](#github-actions):

- Operating System: Ubuntu (22.04, 24.04)

---

# Preparing the Environment

## One-Time Setup

Install the dependencies:
```all
$ sudo apt-get update
$ sudo apt-get install -y gawk wget git-core diffstat unzip texinfo gcc-multilib \
     build-essential chrpath socat libsdl1.2-dev xterm zstd liblz4-tool qemu-system \
     cpio file swtpm swtpm-tools
```

Fetch this project for later use:
```all
$ git clone https://github.com/wxleong/remote-attestation-optiga-tpm ~/remote-attestation-optiga-tpm
```

Fetch the poky repository:
```all
$ cd ~
$ git clone http://git.yoctoproject.org/git/poky ~/poky
$ cd ~/poky
$ git checkout scarthgap-5.0.2
```

## Recurring Setup

# Default Yocto Build

Build the Yocto project with the default settings and run the build in an emulator (qemu). Use this as a test to verify that your environment is properly configured and supported before proceeding.

## Build

Set up the build environment:
```all
$ source oe-init-build-env
```

Build the example image `core-image-full-cmdline`:
> core-image-full-cmdline: A console-only image with more full-featured Linux system functionality installed. [More information here](https://docs.yoctoproject.org/ref-manual/images.html#images).
```all
$ bitbake -k core-image-full-cmdline
```

## QEMU

Launch the image in qemu with graphical mode enabled:
```
$ runqemu core-image-full-cmdline serial
```

Launch the qemu in headless mode and run it as a background process:
```all
$ coproc runqemu core-image-full-cmdline slirp nographic > /tmp/log 2>&1
$ pgrep -f runqemu
$ QEMU_PID=`pgrep -f runqemu`
$ sleep 30
```

Ping the qemu:
```all
$ ssh -o StrictHostKeyChecking=no root@127.0.0.1 -p 2222 'echo "Hello, World!"'
$ ssh -o StrictHostKeyChecking=no root@127.0.0.1 -p 2222 'uname -a'
```

Terminate the qemu:
```all
$ sudo kill $QEMU_PID
$ QEMU_PID=''
$ sleep 30
```

---

# Enable IMA

This section provides the detailed steps to enable the Integrity Measurement Architecture (IMA) feature in the kernel.

Fetch the additional meta-layers:
```all
$ git clone https://github.com/openembedded/meta-openembedded -b scarthgap ~/poky/meta-openembedded
$ cd ~/poky/meta-openembedded
$ git checkout 64c481d017c1b5b5eae619a367a5e8fa00f1b156

$ git clone git://git.yoctoproject.org/meta-security -b scarthgap ~/poky/meta-security
$ cd ~/poky/meta-security
$ git checkout 11ea91192d43d7c2b0b95a93aa63ca7e73e38034
```

Edit the `bblayers.conf` and `local.conf`:
```all
$ cd ~/poky/build
$ bitbake-layers add-layer ../meta-openembedded/meta-oe
$ bitbake-layers add-layer ../meta-security
$ bitbake-layers add-layer ../meta-security/meta-integrity

$ echo 'DISTRO_FEATURES:append = " integrity ima"' >> conf/local.conf

# Remove ima_appraise from the default boot parameters,
# focusing on measurement rather than appraisal.
$ echo 'QB_KERNEL_CMDLINE_APPEND:remove:pn-integrity-image-minimal = "ima_appraise=fix"' >> conf/local.conf
```

Build the project using the example image from `meta-security`:
```all
$ bitbake -k integrity-image-minimal
```

Launch the qemu in headless mode and run it as a background process:
```all
$ coproc runqemu integrity-image-minimal slirp nographic > /tmp/log 2>&1
$ pgrep -f runqemu
$ QEMU_PID=`pgrep -f runqemu`
$ sleep 30
```

Check if the IMA is enabled:
```all
$ ssh -o StrictHostKeyChecking=no root@127.0.0.1 -p 2222 \
    'head /sys/kernel/security/ima/ascii_runtime_measurements'
```

Terminate the qemu:
```all
$ sudo kill $QEMU_PID
$ QEMU_PID=''
$ sleep 30
```

---

# Enable TPM2

In addition to the [Enable IMA](#enable-ima) section, this section provides the steps to enable TPM2 in the kernel so that measurements can be extended to the PCR, enabling remote attestation in a later section.

Add TPM meta-layers:
```all
$ bitbake-layers add-layer ../meta-openembedded/meta-python
$ bitbake-layers add-layer ../meta-security/meta-tpm
```

Build the custom image with IMA and TPM2 enabled. This image combines features from the example images integrity-image-minimal (from `meta-security/meta-integrity`) and security-tpm2-image (from `meta-security/meta-tpm`).
```all
$ cp ~/remote-attestation-optiga-tpm/src/images/ima-tpm2-image.bb \
    ~/poky/meta-security/meta-integrity/recipes-core/images/

$ bitbake -k ima-tpm2-image
```

Launch the TPM2 simulator:
- Ubuntu 22.04:
    ```ubuntu:22.04
    $ sudo -u swtpm mkdir /tmp/swtpm
    $ swtpm_setup --version || true
    $ swtpm --version

    # Initialize the swtpm
    $ sudo -u swtpm swtpm_setup \
        --tpmstate /tmp/swtpm \
        --create-ek-cert --create-platform-cert \
        --tpm2 --overwrite

    # Launch the swtpm
    $ coproc sudo -u swtpm swtpm \
        socket --tpm2 \
        --tpmstate dir=/tmp/swtpm \
        --ctrl type=unixio,path=/tmp/swtpm/sock

    $ sleep 5

    # Grant access permissions to all users
    $ sudo chmod a+rw /tmp/swtpm/sock
    ```
- Ubuntu 24.04:
    ```ubuntu:24.04
    $ mkdir /tmp/swtpm
    $ swtpm_setup --version || true
    $ swtpm --version

    # Create configuration files for swtpm_setup:
    # - ~/.config/swtpm_setup.conf
    # - ~/.config/swtpm-localca.conf
    #   This file specifies the location of the CA keys and certificates:
    #   - ~/.config/var/lib/swtpm-localca/*.pem
    # - ~/.config/swtpm-localca.options
    $ swtpm_setup --tpm2 --create-config-files overwrite,root

    # Initialize the swtpm
    $ swtpm_setup --tpm2 --config ~/.config/swtpm_setup.conf \
        --tpm-state /tmp/swtpm --overwrite \
        --create-ek-cert \
        --create-platform-cert \
        --write-ek-cert-files \
        /tmp/swtpm

    # Launch the swtpm
    $ coproc swtpm socket --tpm2 --flags not-need-init \
        --tpmstate dir=/tmp/swtpm \
        --ctrl type=unixio,path=/tmp/swtpm/sock
    $ sleep 5
    ```

Launch QEMU with swtpm as the TPM2 device:
```all
$ coproc runqemu ima-tpm2-image slirp nographic \
    qemuparams="-chardev socket,id=chrtpm,path=/tmp/swtpm/sock \
    -tpmdev emulator,id=tpm0,chardev=chrtpm \
    -device tpm-tis,tpmdev=tpm0" > /tmp/log 2>&1
$ pgrep -f runqemu
$ QEMU_PID=`pgrep -f runqemu`
$ sleep 60
```

Check if the IMA and TPM2 are enabled:
```all
$ ssh -o StrictHostKeyChecking=no root@127.0.0.1 -p 2222 \
    'head /sys/kernel/security/ima/ascii_runtime_measurements'

$ ssh -o StrictHostKeyChecking=no root@127.0.0.1 -p 2222 \
    'ls /dev/tpm*'
```

Read the event log, which is an informational record of measurements made to PCRs by the platform firmware. For detailed information, refer to [TCG PC Client Specific Platform Firmware Profile Specification](https://trustedcomputinggroup.org/resource/pc-client-specific-platform-firmware-profile-specification/):
```all
$ ssh -o StrictHostKeyChecking=no root@127.0.0.1 -p 2222 \
    'tpm2_eventlog /sys/kernel/security/tpm0/binary_bios_measurements'
```

Read the PCRs directly from the TPM. The values should match the event log:
```all
$ ssh -o StrictHostKeyChecking=no root@127.0.0.1 -p 2222 \
    'tpm2_pcrread'
```

To learn which kernel configurations are enabled, dump the `.config` file at runtime by:
```all
$ ssh -o StrictHostKeyChecking=no root@127.0.0.1 -p 2222 \
    'cat /proc/config.gz | gunzip > /tmp/kernel.config'

$ ssh -o StrictHostKeyChecking=no root@127.0.0.1 -p 2222 \
    'cat /tmp/kernel.config'
```

Terminate the qemu:
```all
$ sudo kill $QEMU_PID
$ QEMU_PID=''
$ sleep 60
```

---

# ARM64

All builds up to this section are on the x86_64 platform and run on QEMU with UEFI enabled, allowing for TCG event logs. In this section, the build is configured for the arm64 platform with U-Boot enabled to generate TCG event logs.

---

# GitHub Actions

This section is intended for maintainers only.

GitHub Actions has been set up to automate the testing of this integration guide as part of the CI/CD process. The testing methodology involves extracting command lines from the markdown and running them in a Docker container. Please refer to [script.sh](.github/docker/script.sh) to see the preparation of commands for testing. The process occurs seamlessly in the background, utilizing hidden tags in the README.md (visible in raw mode). This is why `sleep` commands are included in the guide, allowing for readiness before proceeding to the next step.

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
