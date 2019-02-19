workspace(name = "org_dnosproject_dnos_apps")

#### Docker related repos

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# Download the rules_docker repository at release v0.6.0
http_archive(
    name = "io_bazel_rules_docker",
    sha256 = "c0e9d27e6ca307e4ac0122d3dd1df001b9824373fb6fb8627cd2371068e51fef",
    strip_prefix = "rules_docker-0.6.0",
    urls = ["https://github.com/bazelbuild/rules_docker/archive/v0.6.0.tar.gz"],
)

http_archive(
    name = "build_stack_rules_proto",
    sha256 = "5474d1b83e24ec1a6db371033a27ff7aff412f2b23abba86fedd902330b61ee6",
    strip_prefix = "rules_proto-91cbae9bd71a9c51406014b8b3c931652fb6e660",
    urls = ["https://github.com/stackb/rules_proto/archive/91cbae9bd71a9c51406014b8b3c931652fb6e660.tar.gz"],
)

# OPTIONAL: Call this to override the default docker toolchain configuration.
# This call should be placed BEFORE the call to "container_repositories" below
# to actually override the default toolchain configuration.
# Note this is only required if you actually want to call
# docker_toolchain_configure with a custom attr; please read the toolchains
# docs in /toolchains/docker/ before blindly adding this to your WORKSPACE.

load(
    "@io_bazel_rules_docker//toolchains/docker:toolchain.bzl",
    docker_toolchain_configure = "toolchain_configure",
)

docker_toolchain_configure(
    name = "docker_config",
    # OPTIONAL: Path to a directory which has a custom docker client config.json.
    # See https://docs.docker.com/engine/reference/commandline/cli/#configuration-files
    # for more details.
    client_config = "/path/to/docker/client/config",
)

load(
    "@io_bazel_rules_docker//container:container.bzl",
    "container_pull",
    container_repositories = "repositories",
)

# This is NOT needed when going through the language lang_image
# "repositories" function(s).
container_repositories()

container_pull(
    name = "java_base",
    # 'tag' is also supported, but digest is encouraged for reproducibility.
    #digest = "sha256:deadbeef",
    registry = "gcr.io",
    repository = "distroless/java",
)

load(
    "@io_bazel_rules_docker//java:image.bzl",
    _java_image_repos = "repositories",
)

_java_image_repos()

## Py_image

load(
    "@io_bazel_rules_docker//python:image.bzl",
    _py_image_repos = "repositories",
)

_py_image_repos()

#git_repository(
#    name = "dnos_core",
#    commit = "43e58213fadc0197195c99eb418d89b305a543ed",
#    remote = "https://bitbucket.org/dnosproject/dnos-core/src/master",
#)

## Java bazel dependencies for java applications
git_repository(
    name = "dnos_core",
    commit = "24c3fcc632c794ea028bc6cf8f9b564622a36ac3",
    remote = "https://github.com/dnosproject/dnos-core-tools.git"
)

## Java config service and REST API
git_repository(
    name = "dnos_services",
    commit = "913ee5929cab0cd900bc0cfa219d8dd162bfd288",
    remote = "https://github.com/dnosproject/dnos-services",
)
## Java protobuf models
git_repository(
    name = "dnos_core_grpc",
    commit = "182e976ed3ffaecb9ab5304d61ec901afbfe0fb4",
    remote = "https://github.com/dnosproject/dnos-core-grpc",
)
## Python protobuf models
git_repository(
    name = "dnos_core_grpc_python",
    commit = "13e81fa14df060100e0b98ace954d65701f97e61",
    remote = "https://github.com/dnosproject/dnos-core-grpc-python.git",
)

### Java grpc related dependencies

load("@dnos_core//tools/build/bazel:generate_workspace.bzl", "generated_maven_jars")

generated_maven_jars()

load("@dnos_core//tools/build/bazel:grpc_workspace.bzl", "generate_grpc")

generate_grpc()

load("@io_grpc_grpc_java//:repositories.bzl", "grpc_java_repositories")

grpc_java_repositories(
    omit_com_google_api_grpc_google_common_protos = True,
    omit_com_google_auth_google_auth_library_credentials = True,
    omit_com_google_auth_google_auth_library_oauth2_http = True,
    omit_com_google_code_findbugs_jsr305 = True,
    omit_com_google_code_gson = True,
    omit_com_google_errorprone_error_prone_annotations = True,
    omit_com_google_guava = True,
    omit_com_google_j2objc_j2objc_annotations = True,
    omit_com_google_protobuf = True,
    omit_com_google_protobuf_javalite = True,
    omit_com_google_protobuf_nano_protobuf_javanano = True,
    omit_com_google_re2j = True,
    omit_com_google_truth_truth = True,
    omit_com_squareup_okhttp = True,
    omit_com_squareup_okio = True,
    omit_io_netty_buffer = True,
    omit_io_netty_codec = True,
    omit_io_netty_codec_http = True,
    omit_io_netty_codec_http2 = True,
    omit_io_netty_codec_socks = True,
    omit_io_netty_common = True,
    omit_io_netty_handler = True,
    omit_io_netty_handler_proxy = True,
    omit_io_netty_resolver = True,
    omit_io_netty_tcnative_boringssl_static = True,
    omit_io_netty_transport = True,
    omit_io_opencensus_api = True,
    omit_io_opencensus_grpc_metrics = True,
    omit_javax_annotation = False,
    omit_junit_junit = True,
    omit_org_apache_commons_lang3 = True,
)

### Python protobuf related dependencies
load("@build_stack_rules_proto//python:deps.bzl", "python_proto_library")

python_proto_library()

load("@io_bazel_rules_python//python:pip.bzl", "pip_import", "pip_repositories")

pip_repositories()

pip_import(
    name = "protobuf_py_deps",
    requirements = "@build_stack_rules_proto//python/requirements:protobuf.txt",
)

load("@protobuf_py_deps//:requirements.bzl", protobuf_pip_install = "pip_install")

protobuf_pip_install()

### python_grpc_library related loads

load("@build_stack_rules_proto//python:deps.bzl", "python_grpc_library")

python_grpc_library()

load("@com_github_grpc_grpc//bazel:grpc_deps.bzl", "grpc_deps")

grpc_deps()

load("@io_bazel_rules_python//python:pip.bzl", "pip_import", "pip_repositories")

pip_repositories()

pip_import(
    name = "protobuf_py_deps",
    requirements = "@build_stack_rules_proto//python/requirements:protobuf.txt",
)

load("@protobuf_py_deps//:requirements.bzl", protobuf_pip_install = "pip_install")

protobuf_pip_install()

pip_import(
    name = "grpc_py_deps",
    requirements = "@build_stack_rules_proto//python:requirements.txt",
)

load("@grpc_py_deps//:requirements.bzl", grpc_pip_install = "pip_install")

grpc_pip_install()
