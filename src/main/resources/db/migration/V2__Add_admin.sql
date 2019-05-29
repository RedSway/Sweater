INSERT INTO usr (id, username, password, active)
  VALUES (1, 'admin', '123', TRUE);

INSERT INTO user_role (user_id, roles)
  VALUES (1, 'USER'), (1, 'ADMIN');