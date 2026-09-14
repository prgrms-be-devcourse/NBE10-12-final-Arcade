locals {
  public_ip = aws_eip.app.public_ip
}

output "public_ip" {
  description = "Terraform이 생성해 EC2에 연결한 탄력적 IP 주소."
  value       = local.public_ip
}

output "eip_allocation_id" {
  description = "Terraform이 생성해 EC2에 연결한 탄력적 IP allocation ID."
  value       = aws_eip.app.id
}

output "instance_id" {
  value = aws_instance.app.id
}

output "start_command" {
  description = "중지된 EC2를 운영자 IAM 자격증명으로 시작하는 명령."
  value       = "aws ec2 start-instances --instance-ids ${aws_instance.app.id} --region ${var.region}"
}

output "eice_connect" {
  description = "EIC 임시 키로 EICE를 통해 접속한다. 인터넷에 22번을 열지 않는다."
  value       = "aws ec2-instance-connect ssh --instance-id ${aws_instance.app.id} --connection-type eice --eice-options endpointId=${aws_ec2_instance_connect_endpoint.app.id},maxTunnelDuration=1200 --os-user ec2-user --region ${var.region}"
}

output "ssh_connect" {
  description = "직접 SSH를 허용했을 때 등록된 공개키와 짝이 맞는 개인키로 접속하는 명령."
  value       = length(var.ssh_allowed_cidrs) > 0 ? "ssh ec2-user@${local.public_ip}" : "(ssh_allowed_cidrs 비어 있음 — 직접 SSH 미개방)"
}

output "eice_id" {
  description = "CD와 수동 SSH가 사용할 EC2 Instance Connect Endpoint ID"
  value       = aws_ec2_instance_connect_endpoint.app.id
}

output "app_url" {
  description = "PUBLIC_ORIGIN과 일치해야 하는 실제 서비스 HTTPS 주소."
  value       = "https://${var.domain}"
}

output "bootstrap_url" {
  description = "DNS 연결 전 인프라 확인에만 사용할 탄력적 IP 기반 HTTP 주소."
  value       = "http://${local.public_ip}"
}

output "github_actions_role_arn" {
  description = "배포 워크플로의 AWS_ROLE_ARN Repository Secret에 넣을 값."
  value       = aws_iam_role.github_actions.arn
}

output "uploads_bucket" {
  description = "업로드 버킷 이름. CUSTOM__STORAGE__S3__BUCKET 에 넣는다."
  value       = aws_s3_bucket.uploads.bucket
}

output "uploads_public_url_prefix" {
  description = "CUSTOM__STORAGE__S3__PUBLIC_URL_PREFIX 에 넣는다. 없으면 prod 기동이 실패한다."
  value       = "https://${aws_s3_bucket.uploads.bucket}.s3.${var.region}.amazonaws.com"
}
