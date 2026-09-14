# GitHub-hosted runner가 공인 SSH 포트 없이 인스턴스에 연결하는 터널이다.
resource "aws_ec2_instance_connect_endpoint" "app" {
  subnet_id          = aws_subnet.public.id
  security_group_ids = [aws_security_group.eice.id]
  preserve_client_ip = false

  tags = { Name = "${local.name_prefix}-eice" }
}
