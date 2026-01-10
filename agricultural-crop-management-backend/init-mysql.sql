-- Grant privileges to springuser from any host
GRANT ALL PRIVILEGES ON quanlymuavu.* TO 'springuser'@'%' IDENTIFIED BY 'springpass';
FLUSH PRIVILEGES;


