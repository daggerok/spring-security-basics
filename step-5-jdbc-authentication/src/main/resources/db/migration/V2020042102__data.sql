insert into sec_users (sec_username, sec_password, sec_enabled)
values ('user', '{bcrypt}$2a$10$OlBp2JOK0/8xDjiVqh4OYOggr3tHTKfBcv82dso4fsnUPo66f5Ury', true)
,      ('admin', '{bcrypt}$2a$10$OKPak8tw3jYSyqil/eNKz.U1nF/HtabOotUqi2ceeLuWdBsejH9yS', true)
;
insert into sec_authorities (sec_username, sec_authority)
values ('user', 'ROLE_USER')
,      ('admin', 'ROLE_ADMIN')
;
