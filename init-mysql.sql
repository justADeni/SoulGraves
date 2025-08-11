-- init-mysql.sql
CREATE USER IF NOT EXISTS 'username'@'%' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON minecraft.* TO 'username'@'%';
FLUSH PRIVILEGES;