# 공유 데브코스 계정에 이미 등록된 GitHub OIDC provider를 조회한다.
data "aws_iam_openid_connect_provider" "github" {
  url = "https://token.actions.githubusercontent.com"
}

# 2026-07-15 이후 생성된 저장소의 subject에는 변경되지 않는 owner/repository ID가 붙는다.
locals {
  github_oidc_subject = format(
    "repo:%s@%s/%s@%s:environment:%s",
    local.github_repository_owner,
    var.github_repository_owner_id,
    local.github_repository_name,
    var.github_repository_id,
    var.deploy_environment,
  )
}

# environment를 쓰는 job의 subject는 브랜치가 아니라 environment 이름을 포함한다.
resource "aws_iam_role" "github_actions" {
  name = "${local.name_prefix}-github-actions-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Federated = data.aws_iam_openid_connect_provider.github.arn }
      Action    = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
          "token.actions.githubusercontent.com:sub" = local.github_oidc_subject
        }
      }
    }]
  })
}

# EIC 임시 키와 EICE 터널을 대상 인스턴스·Endpoint·22번으로 제한한다.
resource "aws_iam_role_policy" "github_actions_deploy" {
  name = "${local.name_prefix}-github-actions-deploy"
  role = aws_iam_role.github_actions.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid      = "DescribeDeploymentTarget"
        Effect   = "Allow"
        Action   = ["ec2:DescribeInstances", "ec2:DescribeInstanceConnectEndpoints"]
        Resource = "*"
      },
      {
        Sid      = "PushEphemeralKey"
        Effect   = "Allow"
        Action   = ["ec2-instance-connect:SendSSHPublicKey"]
        Resource = aws_instance.app.arn
        Condition = {
          StringEquals = { "ec2:osuser" = "ec2-user" }
        }
      },
      {
        Sid      = "OpenSshTunnel"
        Effect   = "Allow"
        Action   = ["ec2-instance-connect:OpenTunnel"]
        Resource = aws_ec2_instance_connect_endpoint.app.arn
        Condition = {
          NumericEquals = {
            "ec2-instance-connect:remotePort" = 22
          }
          NumericLessThanEquals = {
            "ec2-instance-connect:maxTunnelDuration" = 1200
          }
          IpAddress = {
            "ec2-instance-connect:privateIpAddress" = "${aws_instance.app.private_ip}/32"
          }
        }
      },
    ]
  })
}
