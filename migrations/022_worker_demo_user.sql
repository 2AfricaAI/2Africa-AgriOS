-- Sprint 20.8 - 加一个工人测试账号
-- 账号: worker / Admin@123456 (复用 admin 的 BCrypt 哈希简化 demo)
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `status`) VALUES
  ('worker', '$2b$10$GFHq9PcQS8SvCpf8pDczfuYnJUT0Nf.hBHNA3b6/7z5JPX4VC5srC', 'John Mwangi', 'active')
ON DUPLICATE KEY UPDATE nickname=VALUES(nickname);

-- 绑定 WORKER 角色
INSERT INTO `sys_user_role` (`user_id`, `role_id`)
SELECT u.id, r.id
FROM `sys_user` u, `sys_role` r
WHERE u.username = 'worker' AND r.code = 'WORKER'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);
