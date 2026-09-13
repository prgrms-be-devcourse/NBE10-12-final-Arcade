# 보안 그룹: EC2 애플리케이션과 EICE의 정책을 분리한다.
resource "aws_security_group" "app" {
  name        = "${local.name_prefix}-sg"
  description = "Arcade server"
  vpc_id      = aws_vpc.main.id

  tags = { Name = "${local.name_prefix}-sg" }
}

resource "aws_security_group" "eice" {
  name        = "${local.name_prefix}-eice-sg"
  description = "EC2 Instance Connect Endpoint"
  vpc_id      = aws_vpc.main.id

  tags = { Name = "${local.name_prefix}-eice-sg" }
}

# SSH ingress: 기본은 EICE, 예외적으로 지정한 운영자 IPv4 /32만 직접 허용한다.
resource "aws_vpc_security_group_ingress_rule" "eice_ssh" {
  security_group_id            = aws_security_group.app.id
  description                  = "SSH from EC2 Instance Connect Endpoint"
  referenced_security_group_id = aws_security_group.eice.id
  from_port                    = 22
  to_port                      = 22
  ip_protocol                  = "tcp"
}

resource "aws_vpc_security_group_ingress_rule" "operator_ssh" {
  for_each = toset(var.ssh_allowed_cidrs)

  security_group_id = aws_security_group.app.id
  description       = "Emergency SSH from operator IPv4"
  cidr_ipv4         = each.value
  from_port         = 22
  to_port           = 22
  ip_protocol       = "tcp"
}

# 웹 ingress: app_cidrs에만 HTTP와 HTTPS를 공개한다.
resource "aws_vpc_security_group_ingress_rule" "http" {
  for_each = toset(var.app_cidrs)

  security_group_id = aws_security_group.app.id
  description       = "HTTP entrypoint"
  cidr_ipv4         = each.value
  from_port         = 80
  to_port           = 80
  ip_protocol       = "tcp"
}

resource "aws_vpc_security_group_ingress_rule" "https" {
  for_each = toset(var.app_cidrs)

  security_group_id = aws_security_group.app.id
  description       = "HTTPS entrypoint"
  cidr_ipv4         = each.value
  from_port         = 443
  to_port           = 443
  ip_protocol       = "tcp"
}

# 애플리케이션 내부 포트는 Docker 네트워크에만 둔다.

# EC2 egress: 이미지 pull, AWS API 호출과 패키지 업데이트를 허용한다.
resource "aws_vpc_security_group_egress_rule" "all" {
  security_group_id = aws_security_group.app.id
  description       = "Image pull, AWS API, package update"
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}

# EICE egress: 대상 EC2 보안 그룹의 SSH 포트로만 나간다.
resource "aws_vpc_security_group_egress_rule" "eice_ssh" {
  security_group_id            = aws_security_group.eice.id
  description                  = "SSH to Arcade instance only"
  referenced_security_group_id = aws_security_group.app.id
  from_port                    = 22
  to_port                      = 22
  ip_protocol                  = "tcp"
}
