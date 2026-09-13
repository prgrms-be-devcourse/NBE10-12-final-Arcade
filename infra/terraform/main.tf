data "aws_availability_zones" "available" {
  state = "available"
}

# AL2023 최신 AMI. 하드코딩하지 않는다.
data "aws_ssm_parameter" "al2023" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

locals {
  environment = "dev"
  name_prefix = "arcade"

  compose_version = "5.5.1"
  compose_sha256  = "db1889184726840f75c4f9c001048430d4f25b3be3cb084d3ddd762bc0aed576"

  github_repository_owner = split("/", var.github_repo)[0]
  github_repository_name  = split("/", var.github_repo)[1]
}
