create table sec_users (
  sec_username varchar(255) not null primary key,
  sec_password varchar(1024) not null,
  sec_enabled boolean not null,
  sec_authority varchar(255) not null
)
;
create unique index sec_users_authorities_idx
  on sec_users (sec_username, sec_authority)
;
