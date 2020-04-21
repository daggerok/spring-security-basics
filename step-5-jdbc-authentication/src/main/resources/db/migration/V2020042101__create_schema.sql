create schema "public"
;
create table sec_users (
  sec_username varchar(255) not null primary key,
  sec_password varchar(1024) not null,
  sec_enabled boolean not null
)
;
create table sec_authorities (
  sec_username varchar(255) not null,
  sec_authority varchar(255) not null,
  constraint sec_authorities_fk
    foreign key (sec_username)
      references sec_users (sec_username)
)
;
create unique index sec_authorities_idx
  on sec_authorities (sec_username, sec_authority)
;
