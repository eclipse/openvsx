-- Assign admin role to 'super_user'
UPDATE user_data SET role='admin' WHERE user_data.login_name='super_user';
