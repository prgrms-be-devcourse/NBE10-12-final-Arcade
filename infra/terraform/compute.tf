resource "aws_instance" "app" {
  ami           = data.aws_ssm_parameter.al2023.value
  instance_type = var.instance_type

  subnet_id              = aws_subnet.public.id
  vpc_security_group_ids = [aws_security_group.app.id]
  iam_instance_profile   = aws_iam_instance_profile.instance.name

  # CPU 크레딧 추가 과금을 막는다.
  credit_specification {
    cpu_credits = "standard"
  }

  root_block_device {
    volume_type = "gp3"
    volume_size = var.root_volume_size
    encrypted   = true

    tags = { Name = "${local.name_prefix}-ec2-root" }
  }

  # IMDSv2만 허용한다.
  metadata_options {
    http_tokens                 = "required"
    http_endpoint               = "enabled"
    http_put_response_hop_limit = 2 # 컨테이너 안에서도 메타데이터를 읽어야 한다
  }

  lifecycle {
    # 최신 AMI 조회값이 바뀌어도 일반 apply에서 운영 EC2를 자동 교체하지 않는다.
    ignore_changes = [ami]
  }

  user_data_replace_on_change = true
  user_data = templatefile("${path.module}/user_data.cloud-config.tftpl", {
    swap_size_bytes         = var.swap_size_mb * 1024 * 1024
    compose_version         = local.compose_version
    compose_sha256          = local.compose_sha256
    operator_ssh_public_key = trimspace(var.operator_ssh_public_key)
  })

  tags = { Name = "${local.name_prefix}-ec2" }
}

# 서비스의 고정 공개 IP를 생성해 EC2에 연결한다.
resource "aws_eip" "app" {
  domain = "vpc"

  tags = { Name = "${local.name_prefix}-eip" }

  depends_on = [aws_internet_gateway.main]
}

resource "aws_eip_association" "app" {
  instance_id   = aws_instance.app.id
  allocation_id = aws_eip.app.id

  depends_on = [aws_internet_gateway.main]
}
