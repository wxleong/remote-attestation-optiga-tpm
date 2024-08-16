DESCRIPTION = "An image as an exmaple for Ima support with tpm2 enabled"

IMAGE_FEATURES += "ssh-server-openssh"

LICENSE = "MIT"

inherit core-image

IMAGE_INSTALL += "\
    packagegroup-base \
    packagegroup-core-boot \
    packagegroup-ima-evm-utils \
    packagegroup-security-tpm2 \
    os-release"

export IMAGE_BASENAME = "ima-tpm2-image"

INHERIT += "ima-evm-rootfs"

QB_KERNEL_CMDLINE_APPEND:append = " ima_policy=tcb"

